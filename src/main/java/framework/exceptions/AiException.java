/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Checked exception signalling failures during AI service calls, prompt parsing, or LLM response processing.
 * Created: 2026-08-15
 */
package framework.exceptions;

public class AiException extends FrameworkException {
    public AiException(String message) {
        super(message);
    }
    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
