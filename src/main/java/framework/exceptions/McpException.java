/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Runtime exception for Model Context Protocol (MCP) communication and tool invocation failures.
 * Created: 2026-08-15
 */
package framework.exceptions;

public class McpException extends FrameworkException {
    public McpException(String message) {
        super(message);
    }
    public McpException(String message, Throwable cause) {
        super(message, cause);
    }
}
