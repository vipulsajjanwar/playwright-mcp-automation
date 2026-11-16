/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Configuration holder for AI integration settings including API keys, model selection, temperature, and retry behaviour.
 * Created: 2026-08-15
 */
package framework.ai;

import framework.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AiConfig {

    private static final Logger logger = LoggerFactory.getLogger(AiConfig.class);
    private static final Properties secretsProps = new Properties();

    static {
        loadSecretsProperties();
    }

    private AiConfig() {}

    private static void loadSecretsProperties() {
        List<String> candidatePaths = List.of("secrets.properties", "src/test/resources/config/secrets.properties");
        for (String path : candidatePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    secretsProps.load(fis);
                    logger.debug("Loaded secrets from: {}", path);
                    break;
                } catch (IOException e) {
                    logger.warn("Could not read secrets file: {}", path, e);
                }
            }
        }
    }

    public static String getProvider() {
        return ConfigManager.get("ai.provider", "mock").trim().toLowerCase();
    }

    public static String getModel() {
        String model = ConfigManager.get("ai.model", "");
        if (model != null && !model.isBlank()) {
            return model.trim();
        }
        String provider = getProvider();
        if ("anthropic".equalsIgnoreCase(provider)) {
            return "claude-3-5-sonnet-20241022";
        } else if ("openai".equalsIgnoreCase(provider)) {
            return "gpt-4o";
        }
        return "mock-model";
    }

    public static String getApiKeyEnvName() {
        return ConfigManager.get("ai.apiKeyEnv", "AI_API_KEY");
    }

    public static String getApiKey() {
        // 1. System property
        String key = System.getProperty("ai.apiKey");
        if (key != null && !key.isBlank()) return key.trim();

        // 2. Env var specified by ai.apiKeyEnv
        String envName = getApiKeyEnvName();
        if (envName != null && !envName.isBlank()) {
            String val = System.getenv(envName);
            if (val != null && !val.isBlank()) return val.trim();
        }

        // 3. Direct AI_API_KEY env var
        String directEnv = System.getenv("AI_API_KEY");
        if (directEnv != null && !directEnv.isBlank()) return directEnv.trim();

        // 4. secrets.properties
        if (secretsProps.containsKey("ai.apiKey")) {
            return secretsProps.getProperty("ai.apiKey").trim();
        }
        if (envName != null && secretsProps.containsKey(envName)) {
            return secretsProps.getProperty(envName).trim();
        }

        // 5. ConfigManager fallback
        return ConfigManager.get("ai.apiKey", "");
    }

    public static int getTimeoutSeconds() {
        return ConfigManager.getInt("ai.timeoutSeconds", 60);
    }

    public static int getMaxConcurrentRequests() {
        return ConfigManager.getInt("ai.maxConcurrentRequests", 2);
    }

    public static boolean isReplan() {
        return ConfigManager.getBoolean("ai.replan", false);
    }

    public static boolean isHealWriteBack() {
        return ConfigManager.getBoolean("ai.healWriteBack", false);
    }

    public static List<String> getAllowedHosts() {
        String raw = ConfigManager.get("ai.allowedHosts", "");
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static boolean isHostAllowed(String host) {
        if (host == null || host.isBlank()) return false;
        List<String> allowed = getAllowedHosts();
        if (allowed.isEmpty()) return true; // If empty, don't restrict or allow all
        String cleanHost = host.trim().toLowerCase();
        for (String a : allowed) {
            String cleanAllowed = a.trim().toLowerCase();
            if (cleanHost.equals(cleanAllowed) || cleanHost.endsWith("." + cleanAllowed)) {
                return true;
            }
        }
        return false;
    }

    public static String getPromptsDir() {
        return ConfigManager.get("prompts.dir", "src/test/resources/prompts");
    }

    public static int getPromptsThreads() {
        return ConfigManager.getInt("prompts.threads", 1);
    }

    public static boolean isPromptsStrict() {
        return ConfigManager.getBoolean("prompts.strict", false);
    }

    public static String getScreenshotsMode() {
        return ConfigManager.get("prompts.screenshots", "failures").trim().toLowerCase();
    }

    public static int getMaxSteps() {
        return ConfigManager.getInt("prompts.maxSteps", 60);
    }

    public static int getStepTimeoutMs() {
        return ConfigManager.getInt("prompts.stepTimeoutMs", 30000);
    }

    public static int getTestTimeoutMinutes() {
        return ConfigManager.getInt("prompts.testTimeoutMinutes", 10);
    }

    public static boolean isLocatorAnalyzerEnabled() {
        return ConfigManager.getBoolean("ai.locatorAnalyzer.enabled", true);
    }

    public static boolean isFailureAnalyzerEnabled() {
        return ConfigManager.getBoolean("ai.failureAnalyzer.enabled", true);
    }
}
