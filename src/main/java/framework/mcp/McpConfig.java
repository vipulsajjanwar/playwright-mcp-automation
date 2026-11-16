/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Configuration class for MCP server connection details: transport type, host, port, and authentication tokens.
 * Created: 2026-08-15
 */
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
