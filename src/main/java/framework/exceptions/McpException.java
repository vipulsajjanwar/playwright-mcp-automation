package framework.exceptions;

public class McpException extends FrameworkException {
    public McpException(String message) {
        super(message);
    }
    public McpException(String message, Throwable cause) {
        super(message, cause);
    }
}
