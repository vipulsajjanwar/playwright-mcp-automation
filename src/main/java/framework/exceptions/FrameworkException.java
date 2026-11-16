/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Root runtime exception for the automation framework, providing a common base for all framework-specific errors.
 * Created: 2026-08-15
 */
package framework.exceptions;

import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.impl.TargetClosedError;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final Map<Class<? extends Exception>, String> exceptionMessages = createExceptionMessages();

    private static Map<Class<? extends Exception>, String> createExceptionMessages() {
        Map<Class<? extends Exception>, String> map = new LinkedHashMap<>();

        // Playwright-specific exceptions
        map.put(TimeoutError.class,
                "The Playwright operation timed out. Verify the locator, page state, network conditions, or increase the configured timeout.");

        map.put(TargetClosedError.class,
                "The browser, page, or browser context has already been closed. Verify the browser/page lifecycle and test cleanup.");

        map.put(PlaywrightException.class,
                "Playwright encountered an error while interacting with the browser. Check the locator, page state, browser session, and stack trace.");

        // General Java exceptions
        map.put(NullPointerException.class,
                "Null reference encountered. Make sure the object is initialized before use.");

        map.put(IllegalArgumentException.class,
                "Invalid argument passed to a method. Check parameter values.");

        map.put(IllegalStateException.class,
                "Method was invoked at an illegal or inappropriate time. Check the current object state.");

        map.put(IOException.class,
                "An I/O error occurred. Check file paths, permissions, or network resources.");

        map.put(FileNotFoundException.class,
                "File not found. Verify the file path and that the file exists.");

        map.put(ArithmeticException.class,
                "Illegal arithmetic operation, such as division by zero. Check your calculations.");

        map.put(ArrayIndexOutOfBoundsException.class,
                "Array index is out of range. Check the array size and accessed index.");

        map.put(ClassCastException.class,
                "Invalid type casting. Ensure the object is of the expected type.");

        map.put(NumberFormatException.class,
                "Invalid number format. Ensure the string can be parsed as a number.");

        return map;
    }

    public static String getExceptionCause(Exception e) {
        if (e == null) {
            return "Unknown error occurred. Exception was null.";
        }

        for (Map.Entry<Class<? extends Exception>, String> entry : exceptionMessages.entrySet()) {
            if (entry.getKey().isInstance(e)) {
                return entry.getValue();
            }
        }

        return "Unknown error occurred. Check the stack trace for more details.";
    }
}
