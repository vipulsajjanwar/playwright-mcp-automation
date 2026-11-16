/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Playwright element interaction helpers: safe click, hover, scroll-into-view, and visibility wait wrappers.
 * Created: 2026-08-15
 */
package framework.utils;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.SelectOption;
import framework.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class ElementUtils {

    private static final Logger logger = LoggerFactory.getLogger(ElementUtils.class);

    private ElementUtils() {}

    public static void click(Locator locator, String elementName) {
        try {
            logger.info("Clicking element: {}", elementName);
            locator.click();
        } catch (Exception e) {
            throw new FrameworkException("Failed to click element: " + elementName, e);
        }
    }

    public static void fill(Locator locator, String value, String elementName) {
        try {
            logger.info("Filling field [{}] with value: {}", elementName, value);
            locator.fill(value);
        } catch (Exception e) {
            throw new FrameworkException("Failed to fill field: " + elementName, e);
        }
    }

    public static void clear(Locator locator, String elementName) {
        try {
            logger.info("Clearing field: {}", elementName);
            locator.clear();
        } catch (Exception e) {
            throw new FrameworkException("Failed to clear element: " + elementName, e);
        }
    }

    public static String getText(Locator locator, String elementName) {
        try {
            String text = locator.innerText().trim();
            logger.debug("Got text '{}' from element: {}", text, elementName);
            return text;
        } catch (Exception e) {
            throw new FrameworkException("Failed to get text from element: " + elementName, e);
        }
    }

    public static String getInputValue(Locator locator, String elementName) {
        try {
            String value = locator.inputValue();
            logger.debug("Got input value '{}' from element: {}", value, elementName);
            return value;
        } catch (Exception e) {
            throw new FrameworkException("Failed to get input value from element: " + elementName, e);
        }
    }

    public static void selectByValue(Locator locator, String value, String elementName) {
        try {
            logger.info("Selecting option [{}] in dropdown: {}", value, elementName);
            locator.selectOption(new SelectOption().setValue(value));
        } catch (Exception e) {
            throw new FrameworkException("Failed to select option by value in dropdown: " + elementName, e);
        }
    }

    public static void selectByLabel(Locator locator, String label, String elementName) {
        try {
            logger.info("Selecting label [{}] in dropdown: {}", label, elementName);
            locator.selectOption(new SelectOption().setLabel(label));
        } catch (Exception e) {
            throw new FrameworkException("Failed to select option by label in dropdown: " + elementName, e);
        }
    }

    public static void check(Locator locator, String elementName) {
        try {
            if (!locator.isChecked()) {
                logger.info("Checking checkbox: {}", elementName);
                locator.check();
            }
        } catch (Exception e) {
            throw new FrameworkException("Failed to check element: " + elementName, e);
        }
    }

    public static void uncheck(Locator locator, String elementName) {
        try {
            if (locator.isChecked()) {
                logger.info("Unchecking checkbox: {}", elementName);
                locator.uncheck();
            }
        } catch (Exception e) {
            throw new FrameworkException("Failed to uncheck element: " + elementName, e);
        }
    }

    public static void setFileInput(Locator locator, String filePath, String elementName) {
        try {
            logger.info("Uploading file [{}] to element: {}", filePath, elementName);
            locator.setInputFiles(Paths.get(filePath));
        } catch (Exception e) {
            throw new FrameworkException("Failed to upload file to element: " + elementName, e);
        }
    }

    public static boolean isVisible(Locator locator) {
        try {
            return locator.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isEnabled(Locator locator) {
        try {
            return locator.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isChecked(Locator locator) {
        try {
            return locator.isChecked();
        } catch (Exception e) {
            return false;
        }
    }
}
