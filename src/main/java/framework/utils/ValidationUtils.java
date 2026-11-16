/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Assertion helper utilities providing human-readable soft and hard assertion wrappers for framework tests.
 * Created: 2026-08-15
 */
package framework.utils;

import framework.exceptions.FrameworkException;
import org.testng.Assert;

public class ValidationUtils {

    private ValidationUtils() {}

    public static void assertEquals(Object actual, Object expected, String message) {
        try {
            Assert.assertEquals(actual, expected, message);
        } catch (AssertionError e) {
            throw new FrameworkException("Assertion Failed: " + message + " [Expected: " + expected + ", Actual: " + actual + "]", e);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        try {
            Assert.assertTrue(condition, message);
        } catch (AssertionError e) {
            throw new FrameworkException("Assertion Failed: " + message + " [Condition was FALSE]", e);
        }
    }

    public static void assertNotNull(Object object, String message) {
        try {
            Assert.assertNotNull(object, message);
        } catch (AssertionError e) {
            throw new FrameworkException("Assertion Failed: " + message + " [Object was NULL]", e);
        }
    }
}
