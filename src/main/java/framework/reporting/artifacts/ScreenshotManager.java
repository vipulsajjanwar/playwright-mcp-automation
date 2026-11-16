/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Captures and stores Playwright page screenshots on test failure or on-demand, with Allure attachment support.
 * Created: 2026-10-01
 */
package framework.reporting.artifacts;

import com.microsoft.playwright.Page;
import framework.config.ConfigManager;
import framework.config.ReportingConfig;
import framework.constants.FrameworkConstants;
import framework.context.TestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized screenshot manager supporting failure capture, on-demand screenshots,
 * deduplication, and attachment to all active reporting integrations.
 */
public final class ScreenshotManager {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotManager.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern(ConfigManager.get("timestamp.format", "yyyyMMdd_HHmmss_SSS"));

    private ScreenshotManager() {}

    /**
     * Captures a failure screenshot and associates it with the TestContext.
     * Reuses already captured screenshot if one was already taken during this test failure event.
     */
    public static String captureFailureScreenshot(Page page, TestContext context) {
        if (!ReportingConfig.isScreenshotOnFailureEnabled()) {
            return null;
        }

        if (context != null && !context.getScreenshotPaths().isEmpty()) {
            // Already captured for this context
            return context.getScreenshotPaths().get(context.getScreenshotPaths().size() - 1);
        }

        return captureScreenshot(page, context != null ? context.getTestName() : "failure", context);
    }

    /**
     * Captures a full-page screenshot and saves it to the reports screenshot directory.
     */
    public static String captureScreenshot(Page page, String name, TestContext context) {
        if (page == null) {
            return null;
        }

        try {
            if (page.isClosed()) {
                return null;
            }

            Path targetDir = Paths.get(FrameworkConstants.SCREENSHOT_DIR);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String safeName = sanitize(name);
            String fileName = String.format("%s_%s_%d.png", safeName, timestamp, Thread.currentThread().getId());
            Path screenshotPath = targetDir.resolve(fileName);

            byte[] bytes = page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath).setFullPage(true));
            String absolutePath = screenshotPath.toAbsolutePath().toString();

            if (context != null) {
                context.addScreenshot(absolutePath, bytes);
            }

            logger.info("Captured screenshot successfully: {}", absolutePath);
            return absolutePath;
        } catch (Exception e) {
            logger.warn("Could not capture screenshot for '{}': {}", name, e.getMessage());
            return null;
        }
    }

    public static byte[] captureScreenshotBytes(Page page) {
        if (page == null) {
            return new byte[0];
        }
        try {
            if (page.isClosed()) {
                return new byte[0];
            }
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (Exception e) {
            logger.warn("Could not capture screenshot bytes: {}", e.getMessage());
            return new byte[0];
        }
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "screenshot";
        }
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
