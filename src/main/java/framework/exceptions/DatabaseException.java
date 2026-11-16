/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Runtime exception wrapping JDBC and database connectivity or query execution errors.
 * Created: 2026-08-15
 */
package framework.exceptions;

public class DatabaseException extends FrameworkException {
    public DatabaseException(String message) {
        super(message);
    }
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
