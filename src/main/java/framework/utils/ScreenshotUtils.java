package framework.utils;

import com.microsoft.playwright.Page;
import framework.constants.FrameworkConstants;
import framework.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtils {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotUtils.class);

    private ScreenshotUtils() {}

    public static String captureScreenshot(Page page, String testName) {
        try {
            Path targetDir = Paths.get(FrameworkConstants.SCREENSHOT_DIR);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String fileName = String.format("%s_%s_%d.png", sanitizeName(testName), timestamp, Thread.currentThread().getId());
            Path screenshotPath = targetDir.resolve(fileName);

            page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath).setFullPage(true));
            logger.info("Captured screenshot: {}", screenshotPath.toAbsolutePath());
            return screenshotPath.toAbsolutePath().toString();
        } catch (Exception e) {
            logger.error("Failed to capture screenshot for test: {}", testName, e);
            return "";
        }
    }

    public static byte[] captureScreenshotAsBytes(Page page) {
        try {
            return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        } catch (Exception e) {
            logger.error("Failed to capture screenshot bytes", e);
            return new byte[0];
        }
    }

    private static String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
