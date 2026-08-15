package framework.mcp;

import framework.config.ConfigManager;

public class McpConfig {

    private McpConfig() {}

    public static boolean isMcpEnabled() {
        return ConfigManager.getBoolean("mcp.enabled", true);
    }

    public static int getServerPort() {
        return ConfigManager.getInt("mcp.serverPort", 8080);
    }

    public static boolean isDbExecutionAllowed() {
        return ConfigManager.getBoolean("mcp.allowDbExecution", true);
    }
}
