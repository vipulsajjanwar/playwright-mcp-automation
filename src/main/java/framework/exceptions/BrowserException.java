package framework.exceptions;

public class BrowserException extends FrameworkException {
    public BrowserException(String message) {
        super(message);
    }
    public BrowserException(String message, Throwable cause) {
        super(message, cause);
    }
}
