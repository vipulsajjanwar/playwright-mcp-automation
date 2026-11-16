/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Runtime exception indicating a missing, invalid, or conflicting framework configuration value.
 * Created: 2026-08-15
 */
package framework.exceptions;

public class ConfigurationException extends FrameworkException {
    public ConfigurationException(String message) {
        super(message);
    }
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
