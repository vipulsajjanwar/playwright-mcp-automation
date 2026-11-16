/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Runtime exception wrapping API client errors including HTTP failures, timeout, and response validation issues.
 * Created: 2026-08-15
 */
package framework.exceptions;

public class ApiException extends FrameworkException {
    public ApiException(String message) {
        super(message);
    }
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
