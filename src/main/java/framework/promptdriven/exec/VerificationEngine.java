/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Evaluates verification assertions from test plan steps, mapping conditions to Playwright and Java assertions.
 * Created: 2026-10-22
 */
package framework.promptdriven.exec;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import framework.promptdriven.plan.ActionType;
import framework.promptdriven.plan.PlanStep;
import framework.promptdriven.report.StepResult;
import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Deterministic assertion and verification engine.
 * Ensures every check produces explicit expected, actual, and PASS/FAIL results.
 * Never passes silently.
 */
public class VerificationEngine {

    private static final Logger logger = LoggerFactory.getLogger(VerificationEngine.class);

    public record VerifyResult(boolean passed, String expected, String actual, String error) {}

    public static VerifyResult verify(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor) {
        ActionType type = step.getActionType();
        if (type == null) {
            return new VerifyResult(false, "valid action", "null", "Unknown verification action: " + step.getAction());
        }

        VariableResolver resolver = context.getVariableResolver();

        try {
            return switch (type) {
                case VERIFY_VISIBLE -> verifyVisible(step, context, uiExecutor);
                case VERIFY_HIDDEN -> verifyHidden(step, context, uiExecutor);
                case VERIFY_TEXT -> verifyText(step, context, uiExecutor, resolver);
                case VERIFY_URL -> verifyUrl(step, context, resolver);
                case VERIFY_TITLE -> verifyTitle(step, context, resolver);
                case VERIFY_COUNT -> verifyCount(step, context, uiExecutor, resolver);
                case VERIFY_TABLE -> verifyTable(step, context, uiExecutor, resolver);
                case VERIFY_ENABLED -> verifyEnabled(step, context, uiExecutor);
                case VERIFY_DISABLED -> verifyDisabled(step, context, uiExecutor);
                case VERIFY_COMPONENTS -> verifyComponents(step, context, uiExecutor, resolver);
                case VERIFY_STATUS -> verifyStatus(step, context, resolver);
                case VERIFY_JSON -> verifyJson(step, context, resolver);
                case VERIFY_FIELD_EXISTS -> verifyFieldExists(step, context, resolver);
                case VERIFY_EVERY_ITEM_HAS -> verifyEveryItemHas(step, context, resolver);
                case VERIFY_HEADER -> verifyHeader(step, context, resolver);
                case VERIFY_RESPONSE_TIME -> verifyResponseTime(step, context, resolver);
                default -> new VerifyResult(false, "Supported verification", type.name(), "Action is not a verification: " + type);
            };
        } catch (Exception e) {
            logger.error("Verification execution threw exception for step {}: {}", step.getId(), e.getMessage(), e);
            return new VerifyResult(false, step.getStringArg("expected"), "EXCEPTION", e.getMessage());
        }
    }

    private static VerifyResult verifyVisible(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor) {
        Locator loc = uiExecutor.resolveLocator(step.getTarget());
        boolean visible = loc.isVisible();
        return new VerifyResult(visible, "visible", visible ? "visible" : "hidden", visible ? null : "Element was not visible");
    }

    private static VerifyResult verifyHidden(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor) {
        Locator loc = uiExecutor.resolveLocator(step.getTarget());
        boolean hidden = loc.isHidden();
        return new VerifyResult(hidden, "hidden", hidden ? "hidden" : "visible", hidden ? null : "Element was not hidden");
    }

    private static VerifyResult verifyText(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor, VariableResolver resolver) {
        Locator loc = uiExecutor.resolveLocator(step.getTarget());
        String expectedRaw = step.getStringArg("expected");
        String expected = resolver.resolve(expectedRaw);
        String matchType = step.getArgs().getOrDefault("match", "contains").toString().toLowerCase(Locale.ROOT);

        String actual = loc.innerText().trim();
        boolean pass = switch (matchType) {
            case "exact" -> actual.equals(expected);
            case "regex" -> Pattern.compile(expected).matcher(actual).find();
            default -> actual.contains(expected);
        };

        return new VerifyResult(pass, expected, actual, pass ? null : String.format("Text mismatch (match=%s): expected '%s', got '%s'", matchType, expected, actual));
    }

    private static VerifyResult verifyUrl(PlanStep step, ExecutionContext context, VariableResolver resolver) {
        String expected = resolver.resolve(step.getStringArg("expected"));
        String matchType = step.getArgs().getOrDefault("match", "contains").toString().toLowerCase(Locale.ROOT);
        String actual = context.getPage().url();

        boolean pass = "exact".equals(matchType) ? actual.equals(expected) : actual.contains(expected);
        return new VerifyResult(pass, expected, actual, pass ? null : "URL mismatch: expected " + expected + ", got " + actual);
    }

    private static VerifyResult verifyTitle(PlanStep step, ExecutionContext context, VariableResolver resolver) {
        String expected = resolver.resolve(step.getStringArg("expected"));
        String actual = context.getPage().title();
        boolean pass = actual.contains(expected);
        return new VerifyResult(pass, expected, actual, pass ? null : "Title mismatch: expected " + expected + ", got " + actual);
    }

    private static VerifyResult verifyCount(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor, VariableResolver resolver) {
        Locator loc = uiExecutor.resolveLocator(step.getTarget());
        int expected = Integer.parseInt(resolver.resolve(step.getStringArg("expected")));
        String op = step.getArgs().getOrDefault("op", "==").toString();
        int actual = loc.count();

        boolean pass = switch (op) {
            case ">" -> actual > expected;
            case "<" -> actual < expected;
            case ">=" -> actual >= expected;
            case "<=" -> actual <= expected;
            default -> actual == expected;
        };
        return new VerifyResult(pass, op + " " + expected, String.valueOf(actual), pass ? null : "Count mismatch: expected " + op + " " + expected + ", got " + actual);
    }

    @SuppressWarnings("unchecked")
    private static VerifyResult verifyTable(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor, VariableResolver resolver) {
        Page page = context.getPage();
        Object colsObj = step.getArg("columns");
        List<String> expectedColumns = new ArrayList<>();
        if (colsObj instanceof List) {
            for (Object col : (List<?>) colsObj) {
                expectedColumns.add(resolver.resolve(String.valueOf(col)));
            }
        }

        List<String> missingCols = new ArrayList<>();
        for (String col : expectedColumns) {
            Locator th = page.locator("th, [role='columnheader']").filter(new Locator.FilterOptions().setHasText(col));
            if (th.count() == 0 && !page.content().contains(col)) {
                missingCols.add(col);
            }
        }

        boolean pass = missingCols.isEmpty();
        return new VerifyResult(pass, expectedColumns.toString(), pass ? expectedColumns.toString() : "Missing: " + missingCols,
                pass ? null : "Table missing columns: " + missingCols);
    }

    private static VerifyResult verifyEnabled(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor) {
        Locator loc = uiExecutor.resolveLocator(step.getTarget());
        boolean enabled = loc.isEnabled();
        return new VerifyResult(enabled, "enabled", enabled ? "enabled" : "disabled", enabled ? null : "Element was disabled");
    }

    private static VerifyResult verifyDisabled(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor) {
        Locator loc = uiExecutor.resolveLocator(step.getTarget());
        boolean disabled = loc.isDisabled();
        return new VerifyResult(disabled, "disabled", disabled ? "disabled" : "enabled", disabled ? null : "Element was enabled");
    }

    @SuppressWarnings("unchecked")
    private static VerifyResult verifyComponents(PlanStep step, ExecutionContext context, UiActionExecutor uiExecutor, VariableResolver resolver) {
        Page page = context.getPage();
        Object itemsObj = step.getArg("items");
        if (!(itemsObj instanceof List)) {
            return new VerifyResult(false, "list of items", "null", "Invalid items argument for VERIFY_COMPONENTS");
        }

        List<?> items = (List<?>) itemsObj;
        List<String> verified = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (Object itm : items) {
            String name = "";
            String role = "any";
            if (itm instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) itm;
                name = resolver.resolve(String.valueOf(map.getOrDefault("name", "")));
                role = String.valueOf(map.getOrDefault("role", "any"));
            } else {
                name = resolver.resolve(String.valueOf(itm));
            }

            if (name.isBlank()) continue;

            boolean found = false;
            // 1. Try text or role locator
            try {
                if (page.getByText(name).first().isVisible()) {
                    found = true;
                }
            } catch (Exception ignored) {}

            if (!found) {
                // Fallback check in page content
                found = page.content().contains(name);
            }

            if (found) {
                verified.add(name);
            } else {
                missing.add(name);
            }
        }

        boolean pass = missing.isEmpty();
        return new VerifyResult(pass, "All components visible: " + items, "Verified: " + verified + (missing.isEmpty() ? "" : ", Missing: " + missing),
                pass ? null : "Missing components: " + missing);
    }

    private static VerifyResult verifyStatus(PlanStep step, ExecutionContext context, VariableResolver resolver) {
        ExecutionContext.ApiResponseData response = context.getLastApiResponse();
        if (response == null) {
            return new VerifyResult(false, step.getStringArg("expected"), "null", "No prior API response recorded in ExecutionContext");
        }

        int expected = Integer.parseInt(resolver.resolve(step.getStringArg("expected")));
        int actual = response.statusCode();
        boolean pass = (actual == expected);
        return new VerifyResult(pass, String.valueOf(expected), String.valueOf(actual),
                pass ? null : "Status code mismatch: expected " + expected + ", got " + actual);
    }

    private static VerifyResult verifyJson(PlanStep step, ExecutionContext context, VariableResolver resolver) {
        ExecutionContext.ApiResponseData response = context.getLastApiResponse();
        if (response == null) {
            return new VerifyResult(false, step.getStringArg("expected"), "null", "No prior API response recorded");
        }

        String path = step.getStringArg("jsonPath");
        String expected = resolver.resolve(step.getStringArg("expected"));

        try {
            JsonNode root = JsonUtils.parseJsonNode(response.body());
            JsonNode targetNode = extractJsonPath(root, path);
            String actual = targetNode.isMissingNode() ? "MISSING" : targetNode.asText();
            boolean pass = actual.equals(expected);
            return new VerifyResult(pass, expected, actual, pass ? null : "JSON path '" + path + "' mismatch: expected '" + expected + "', got '" + actual + "'");
        } catch (Exception e) {
            return new VerifyResult(false, expected, "ERROR", "Failed to parse JSON response: " + e.getMessage());
        }
    }

    private static VerifyResult verifyFieldExists(PlanStep step, ExecutionContext context, VariableResolver resolver) {
        ExecutionContext.ApiResponseData response = context.getLastApiResponse();
        if (response == null) {
            return new VerifyResult(false, "field exists", "null", "No prior API response recorded");
        }

        String path = step.getStringArg("jsonPath");
        try {
            JsonNode root = JsonUtils.parseJsonNode(response.body());
            JsonNode targetNode = extractJsonPath(root, path);
            boolean exists = !targetNode.isMissingNode() && !targetNode.isNull();
            return new VerifyResult(exists, "field '" + path + "' exists", exists ? "present" : "missing",
                    exists ? null : "Field '" + path + "' does not exist in response");
        } catch (Exception e) {
            return new VerifyResult(false, "field exists", "ERROR", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static VerifyResult verifyEveryItemHas(PlanStep step, ExecutionContext context, VariableResolver resolver) {
        ExecutionContext.ApiResponseData response = context.getLastApiResponse();
        if (response == null) {
            return new VerifyResult(false, "every item has fields", "null", "No prior API response recorded");
        }

        String arrayPath = step.getStringArg("arrayPath");
        Object fieldsObj = step.getArg("fields");
        List<String> fields = new ArrayList<>();
        if (fieldsObj instanceof List) {
            for (Object f : (List<?>) fieldsObj) fields.add(resolver.resolve(String.valueOf(f)));
        }

        try {
            JsonNode root = JsonUtils.parseJsonNode(response.body());
            JsonNode arrayNode = (arrayPath == null || arrayPath.equals("$")) ? root : extractJsonPath(root, arrayPath);

            if (!arrayNode.isArray() || arrayNode.isEmpty()) {
                return new VerifyResult(false, "non-empty array with fields " + fields, "not an array or empty", "Target node is not an array");
            }

            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode item = arrayNode.get(i);
                for (String field : fields) {
                    if (item.path(field).isMissingNode()) {
                        return new VerifyResult(false, "all items have " + fields, "item " + i + " missing " + field,
                                "Item at index " + i + " is missing field '" + field + "'");
                    }
                }
            }
            return new VerifyResult(true, "all items have " + fields, "verified on " + arrayNode.size() + " items", null);
        } catch (Exception e) {
            return new VerifyResult(false, "every item has fields", "ERROR", e.getMessage());
        }
    }

    private static VerifyResult verifyHeader(PlanStep step, ExecutionContext context, VariableResolver resolver) {
        ExecutionContext.ApiResponseData response = context.getLastApiResponse();
        if (response == null) return new VerifyResult(false, "header", "null", "No prior API response");

        String name = step.getStringArg("name");
        String expected = resolver.resolve(step.getStringArg("expected"));
        String actual = response.headers().get(name);

        boolean pass = actual != null && (actual.equals(expected) || actual.contains(expected));
        return new VerifyResult(pass, expected, String.valueOf(actual), pass ? null : "Header mismatch for '" + name + "'");
    }

    private static VerifyResult verifyResponseTime(PlanStep step, ExecutionContext context, VariableResolver resolver) {
        ExecutionContext.ApiResponseData response = context.getLastApiResponse();
        if (response == null) return new VerifyResult(false, "response time", "null", "No prior API response");

        long maxMs = Long.parseLong(resolver.resolve(step.getStringArg("maxMs")));
        long actual = response.durationMs();
        boolean pass = actual <= maxMs;
        return new VerifyResult(pass, "<= " + maxMs + "ms", actual + "ms", pass ? null : "Response time exceeded max: " + actual + "ms > " + maxMs + "ms");
    }

    private static JsonNode extractJsonPath(JsonNode root, String path) {
        if (path == null || path.isBlank() || path.equals("$")) return root;
        String clean = path.replaceFirst("^\\$\\.?", "");
        String[] tokens = clean.split("\\.");
        JsonNode curr = root;
        for (String t : tokens) {
            if (t.contains("[") && t.endsWith("]")) {
                String name = t.substring(0, t.indexOf('['));
                int idx = Integer.parseInt(t.substring(t.indexOf('[') + 1, t.length() - 1));
                curr = name.isEmpty() ? curr.path(idx) : curr.path(name).path(idx);
            } else {
                curr = curr.path(t);
            }
        }
        return curr;
    }
}
