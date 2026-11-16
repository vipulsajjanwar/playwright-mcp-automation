/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Runtime exception for browser and Playwright errors: launch failures, navigation timeouts, and element interaction errors.
 * Created: 2026-08-15
 */
package framework.exceptions;

public class BrowserException extends FrameworkException {
    public BrowserException(String message) {
        super(message);
    }
    public BrowserException(String message, Throwable cause) {
        super(message, cause);
    }
}
