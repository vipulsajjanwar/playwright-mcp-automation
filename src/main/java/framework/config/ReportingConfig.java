package framework.config;

/**
 * Central configuration holder for all reporting and observability integrations.
 * Provides backwards compatibility with existing configuration keys while offering
 * standardized reporting.* settings.
 */
public final class ReportingConfig {

    private ReportingConfig() {}

    public static boolean isExtentEnabled() {
        return ConfigManager.getBoolean("reporting.extent", true);
    }

    public static boolean isAllureEnabled() {
        if (ConfigManager.get("reporting.allure") != null) {
            return ConfigManager.getBoolean("reporting.allure", true);
        }
        return ConfigManager.getBoolean("allure.enabled", true);
    }

    public static boolean isAllureHistoryEnabled() {
        if (ConfigManager.get("allure.history.enabled") != null) {
            return ConfigManager.getBoolean("allure.history.enabled", true);
        }
        return ConfigManager.getBoolean("reporting.allure.history", true);
    }

    public static String getAllureBasePath() {
        String path = ConfigManager.get("allure.base.path");
        if (path != null && !path.isBlank()) {
            return path.trim();
        }
        path = ConfigManager.get("reporting.allure.base.path");
        return (path != null && !path.isBlank()) ? path.trim() : "reports/allure";
    }

    public static boolean isAllureAutoGenerateEnabled() {
        if (ConfigManager.get("allure.auto.generate") != null) {
            return ConfigManager.getBoolean("allure.auto.generate", true);
        }
        return ConfigManager.getBoolean("reporting.allure.auto.generate", true);
    }

    public static boolean isChainTestEnabled() {
        if (ConfigManager.get("reporting.chaintest") != null) {
            return ConfigManager.getBoolean("reporting.chaintest", true);
        }
        return ConfigManager.getBoolean("chaintest.enabled", true);
    }

    public static boolean isElkEnabled() {
        if (ConfigManager.get("reporting.elk") != null) {
            return ConfigManager.getBoolean("reporting.elk", false);
        }
        return ConfigManager.getBoolean("elk.enabled", false);
    }

    public static boolean isDatabaseEnabled() {
        return ConfigManager.getBoolean("reporting.database", false);
    }

    public static boolean isConsoleEnabled() {
        return ConfigManager.getBoolean("reporting.console", true);
    }

    public static boolean isScreenshotOnFailureEnabled() {
        if (ConfigManager.get("reporting.screenshot.on.failure") != null) {
            return ConfigManager.getBoolean("reporting.screenshot.on.failure", true);
        }
        return ConfigManager.getBoolean("screenshot.onFailure", true);
    }

    public static boolean isVideoOnFailureEnabled() {
        if (ConfigManager.get("reporting.video.on.failure") != null) {
            return ConfigManager.getBoolean("reporting.video.on.failure", true);
        }
        return ConfigManager.getBoolean("video.onFailure", true);
    }

    public static boolean isTraceOnFailureEnabled() {
        if (ConfigManager.get("reporting.trace.on.failure") != null) {
            return ConfigManager.getBoolean("reporting.trace.on.failure", true);
        }
        return ConfigManager.getBoolean("trace.onFailure", true);
    }

    public static boolean isAsyncElkEnabled() {
        return ConfigManager.getBoolean("reporting.async.elk", true);
    }

    public static boolean isAsyncDatabaseEnabled() {
        return ConfigManager.getBoolean("reporting.async.database", true);
    }
}
