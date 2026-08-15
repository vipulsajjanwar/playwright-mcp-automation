package framework.exceptions;

public class ExcelException extends FrameworkException {
    public ExcelException(String message) {
        super(message);
    }
    public ExcelException(String message, Throwable cause) {
        super(message, cause);
    }
}
