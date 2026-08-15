package framework.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReportUtils.class);

    private ReportUtils() {}

    public static void logInfo(String message) {
        logger.info(message);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.INFO, message);
        }
    }

    public static void logPass(String message) {
        logger.info("[PASS] {}", message);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.PASS, message);
        }
    }

    public static void logFail(String message, Throwable throwable) {
        logger.error("[FAIL] {}", message, throwable);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.FAIL, message);
            if (throwable != null) {
                test.log(Status.FAIL, throwable);
            }
        }
    }

    public static void logScreenshot(String screenshotPath, String title) {
        ExtentTest test = ReportManager.getTest();
        if (test != null && screenshotPath != null && !screenshotPath.isEmpty()) {
            try {
                test.info(title, MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
            } catch (Exception e) {
                logger.error("Failed to attach screenshot to report", e);
            }
        }
    }
}
