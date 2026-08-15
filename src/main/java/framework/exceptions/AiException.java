package framework.exceptions;

public class AiException extends FrameworkException {
    public AiException(String message) {
        super(message);
    }
    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
