/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Resolves variable references and expressions within prompt test step definitions using runtime context values.
 * Created: 2026-10-22
 */
package framework.promptdriven.exec;

import framework.utils.JsonUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves runtime variable placeholders (${NAME}, ${env.VAR}, ${stored.KEY}, ${user.email})
 * and masks credentials in outputs.
 */
public class VariableResolver {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_.-]+)\\}");

    private final Map<String, Object> variables;
    private final SecretsProvider secretsProvider;

    public VariableResolver(Map<String, Object> variables, SecretsProvider secretsProvider) {
        this.variables = variables;
        this.secretsProvider = secretsProvider != null ? secretsProvider : new SecretsProvider();
    }

    public String resolve(String template) {
        if (template == null || !template.contains("${")) {
            return template;
        }

        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varExpr = matcher.group(1);
            String resolvedValue = resolveVariable(varExpr);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(resolvedValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public String resolveVariable(String key) {
        if (key == null || key.isBlank()) return "";

        // 1. Direct env variable: ${env.VAR}
        if (key.startsWith("env.")) {
            String envKey = key.substring(4);
            String envVal = System.getenv(envKey);
            return envVal != null ? envVal : "";
        }

        // 2. Explicit stored variable prefix: ${stored.KEY}
        if (key.startsWith("stored.")) {
            String storedKey = key.substring(7);
            Object val = variables.get(storedKey);
            return val != null ? String.valueOf(val) : "";
        }

        // 3. Explicit generated variable prefix: ${generated.KEY}
        if (key.startsWith("generated.")) {
            String genKey = key.substring(10);
            return resolveFromVariablesOrDotNotation(genKey);
        }

        // 4. Check per-test variables store (including dot notation e.g. newUser.email)
        String varResult = resolveFromVariablesOrDotNotation(key);
        if (!varResult.isEmpty()) {
            return varResult;
        }

        // 5. SecretsProvider resolution for credentials or configuration
        String secretVal = secretsProvider.resolveSecret(key);
        if (!secretVal.isEmpty()) {
            return secretVal;
        }

        // If unresolved, retain placeholder or empty string
        return "${" + key + "}";
    }

    @SuppressWarnings("unchecked")
    private String resolveFromVariablesOrDotNotation(String key) {
        if (variables.containsKey(key)) {
            Object val = variables.get(key);
            return val != null ? String.valueOf(val) : "";
        }

        if (key.contains(".")) {
            String[] parts = key.split("\\.", 2);
            Object root = variables.get(parts[0]);
            if (root instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) root;
                Object subVal = map.get(parts[1]);
                if (subVal != null) return String.valueOf(subVal);
            } else if (root instanceof String str && (str.startsWith("{") || str.startsWith("["))) {
                try {
                    var node = JsonUtils.parseJsonNode(str);
                    var child = node.path(parts[1]);
                    if (!child.isMissingNode()) {
                        return child.asText();
                    }
                } catch (Exception ignored) {}
            }
        }
        return "";
    }

    public String mask(String text) {
        return secretsProvider.mask(text);
    }

    public SecretsProvider getSecretsProvider() {
        return secretsProvider;
    }
}
