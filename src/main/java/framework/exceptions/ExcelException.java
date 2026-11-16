/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Runtime exception for Excel file read/write failures including format errors and missing sheets.
 * Created: 2026-08-15
 */
package framework.exceptions;

public class ExcelException extends FrameworkException {
    public ExcelException(String message) {
        super(message);
    }
    public ExcelException(String message, Throwable cause) {
        super(message, cause);
    }
}
