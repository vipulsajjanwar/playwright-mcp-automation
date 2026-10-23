package framework.reporting;

import com.microsoft.playwright.Page;
import framework.context.TestContext;
import framework.context.TestContextManager;
import framework.reporting.artifacts.ScreenshotManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unified logging abstraction for tests.
 * Consolidates console logging, steps, and attaches information to all active reporters
 * without generating duplicate console logs.
 */
public final class FrameworkLogger {

    private static final Logger logger = LoggerFactory.getLogger(FrameworkLogger.class);

    private FrameworkLogger() {}

    public static void info(String message) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            ReportingManager.getInstance().onStep(context, message);
        } else {
            logger.info("[INFO] {}", message);
        }
    }

    public static void step(String stepDescription) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            ReportingManager.getInstance().onStep(context, stepDescription);
        } else {
            logger.info("[STEP] {}", stepDescription);
        }
    }

    public static void warn(String message) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            ReportingManager.getInstance().onStep(context, "[WARN] " + message);
        } else {
            logger.warn("[WARN] {}", message);
        }
    }

    /**
     * Logs an HTML table as a step in the test report.
     *
     * @param title     descriptive title for the table
     * @param htmlTable the pre-built HTML table markup
     */
    public static void table(String title, String htmlTable) {
        TestContext context = TestContextManager.getContext();
        String payload = "[TABLE] " + title + "\n" + htmlTable;
        if (context != null) {
            ReportingManager.getInstance().onStep(context, payload);
        } else {
            logger.info("[TABLE] {}", title);
        }
    }

    public static void error(String message, Throwable throwable) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            ReportingManager.getInstance().onStep(context, "[ERROR] " + message + (throwable != null ? " (" + throwable.getMessage() + ")" : ""));
        }
        logger.error("[ERROR] {}", message, throwable);
    }

    public static void screenshot(Page page, String title) {
        TestContext context = TestContextManager.getContext();
        if (page != null) {
            ScreenshotManager.captureScreenshot(page, title, context);
        }
    }
}
