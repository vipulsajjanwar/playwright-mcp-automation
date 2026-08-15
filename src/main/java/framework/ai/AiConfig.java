package framework.ai;

import framework.config.ConfigManager;

public class AiConfig {

    private AiConfig() {}

    public static String getProvider() {
        return ConfigManager.get("ai.provider", "mock").toLowerCase();
    }

    public static String getModel() {
        return ConfigManager.get("ai.model", "gpt-4o-mini");
    }

    public static String getApiKey() {
        return ConfigManager.get("ai.apiKey", "");
    }

    public static boolean isLocatorAnalyzerEnabled() {
        return ConfigManager.getBoolean("ai.locatorAnalyzer.enabled", true);
    }

    public static boolean isFailureAnalyzerEnabled() {
        return ConfigManager.getBoolean("ai.failureAnalyzer.enabled", true);
    }
}
