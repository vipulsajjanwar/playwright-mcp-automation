package framework.reporting;

import com.microsoft.playwright.Page;
import framework.context.TestContext;
import framework.context.TestContextManager;
import framework.reporting.artifacts.ScreenshotManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for reporting actions, backward-compatible with legacy tests.
 * Delegates directly to {@link FrameworkLogger} and {@link ReportingManager}
 * to prevent duplicate reporting calls.
 */
public final class ReportUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReportUtils.class);

    private ReportUtils() {}

    public static void logInfo(String message) {
        FrameworkLogger.info(message);
    }

    public static void logPass(String message) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            ReportingManager.getInstance().onStep(context, "[PASS] " + message);
        } else {
            logger.info("[PASS] {}", message);
        }
    }

    public static void logFail(String message, Throwable throwable) {
        FrameworkLogger.error(message, throwable);
    }

    public static void logFail(String message, String reason, Page page) {
        TestContext context = TestContextManager.getContext();
        if (page != null) {
            ScreenshotManager.captureScreenshot(page, "ValidationFailure", context);
        }
        FrameworkLogger.error(message + " - " + reason, null);
    }

    public static void logError(Exception e, String message) {
        FrameworkLogger.error(message, e);
    }

    public static void logScreenshot(String screenshotPath, String title) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            context.addScreenshot(screenshotPath, null);
            ReportingManager.getInstance().onStep(context, "[SCREENSHOT] " + title + ": " + screenshotPath);
        }
    }

    public static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "unknown";
        String safe = name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return safe.length() > 80 ? safe.substring(0, 80) : safe;
    }

    public static void reportErrorLog(Exception e, String message) {
        FrameworkLogger.error(message, e);
    }
}
