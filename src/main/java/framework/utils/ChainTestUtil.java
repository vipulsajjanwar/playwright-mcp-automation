/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Utility methods for ChainTest integration: node creation, step logging, and media attachment helpers.
 * Created: 2026-09-29
 */
package framework.utils;

import com.microsoft.playwright.Page;
import framework.config.ConfigManager;
import framework.listeners.ChainTestListener;
import framework.reporting.ChainTestLogger;
import framework.reporting.LogLevel;
import org.testng.Reporter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChainTestUtil {

    private ChainTestUtil() {
        // Utility class
    }

    // =========================================================
    // ENABLE / DISABLE
    // =========================================================

    public static boolean isEnabled() {
        return Boolean.parseBoolean(ConfigManager.get("chaintest.enabled", "false"));
    }

    // =========================================================
    // INFO
    // =========================================================

    public static void info(String message) {
        if (!isEnabled()) {
            return;
        }
        ChainTestLogger.info(message);
    }

    // =========================================================
    // PASS
    // =========================================================

    public static void pass(String message) {
        if (!isEnabled()) {
            return;
        }
        ChainTestLogger.pass(message);
    }

    // =========================================================
    // FAIL
    // =========================================================

    public static void fail(String message) {
        if (!isEnabled()) {
            return;
        }
        ChainTestLogger.fail(message);
    }

    public static void fail(String message, Throwable throwable) {
        if (!isEnabled()) {
            return;
        }
        ChainTestLogger.fail(message);
        exception("Exception", throwable);
    }

    // =========================================================
    // WARN
    // =========================================================

    public static void warn(String message) {
        if (!isEnabled()) {
            return;
        }
        ChainTestLogger.warn(message);
    }

    // =========================================================
    // ERROR
    // =========================================================

    public static void error(String message) {
        if (!isEnabled()) {
            return;
        }
        ChainTestLogger.error(message);
    }

    public static void error(String message, Throwable throwable) {
        if (!isEnabled()) {
            return;
        }
        ChainTestLogger.error(message);
        exception("Exception", throwable);
    }

    // =========================================================
    // DEBUG
    // =========================================================

    public static void debug(String message) {
        if (!isEnabled()) {
            return;
        }
        ChainTestLogger.debug(message);
    }

    // =========================================================
    // EXCEPTION
    // =========================================================

    public static void exception(String message, Throwable throwable) {
        if (!isEnabled()) {
            return;
        }

        if (throwable == null) {
            ChainTestLogger.error(message);
            return;
        }

        String details = message + " | " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        ChainTestLogger.error(details);
    }

    // =========================================================
    // SCREENSHOT
    // =========================================================

    public static void screenshot(Page page, String name) {
        if (!isEnabled()) {
            return;
        }

        if (page == null) {
            warn("Screenshot skipped because Page is null");
            return;
        }

        try {
            ChainTestListener.attachScreenshot(name, page);
        } catch (Exception e) {
            warn("Unable to capture screenshot: " + e.getMessage());
        }
    }

    // =========================================================
    // IMAGE / SCREENSHOT ATTACHMENT
    // =========================================================

    public static void attachImage(String name, byte[] imageBytes) {
        if (!isEnabled() || imageBytes == null || imageBytes.length == 0) {
            return;
        }
        ChainTestListener.attachScreenshotBytes(name, imageBytes);
    }

    public static void attachScreenshot(String screenshotPath, String title) {
        if (!isEnabled() || screenshotPath == null || screenshotPath.isEmpty()) {
            return;
        }
        ChainTestListener.attachScreenshotPath(title, Paths.get(screenshotPath));
    }

    // =========================================================
    // TEXT
    // =========================================================

    public static void attachText(String name, String content) {
        if (!isEnabled()) {
            return;
        }
        ChainTestListener.attachText(name, content);
    }

    // =========================================================
    // JSON
    // =========================================================

    public static void attachJson(String name, String json) {
        if (!isEnabled()) {
            return;
        }
        ChainTestListener.attachJson(name, json);
    }

    // =========================================================
    // HTML
    // =========================================================

    public static void attachHtml(String name, String html) {
        if (!isEnabled()) {
            return;
        }
        ChainTestListener.attachHtml(name, html);
    }

    // =========================================================
    // INTERNAL LOG
    // =========================================================

    private static void log(String level, String message) {
        if (!isEnabled()) {
            return;
        }

        LogLevel logLevel;
        try {
            logLevel = LogLevel.valueOf(level.toUpperCase());
        } catch (Exception e) {
            logLevel = LogLevel.INFO;
        }
        ChainTestLogger.log(logLevel, message);
    }

    public static String getReportName(String suiteName) {

        return suiteName;
    }

    public static String getReportDirectory(String suiteName) {

        return "reports/ChainTest/" + suiteName;
    }
}
