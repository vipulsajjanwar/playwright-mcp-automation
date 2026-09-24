package framework.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.microsoft.playwright.Page;
import framework.constants.TestConstants;
import framework.exceptions.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Reporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class ReportUtils {

    private static final Logger logger = LoggerFactory.getLogger(ReportUtils.class);

    private ReportUtils() {}

    public static void logInfo(String message) {
        logger.info("[INFO] {}",message);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.INFO, MarkupHelper.createLabel(message, ExtentColor.TRANSPARENT));
        }
    }

    public static void logPass(String message) {
        logger.info("[PASS] {}", message);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.PASS, MarkupHelper.createLabel(message, ExtentColor.GREEN));
        }
    }

    public static void logFail(String message, Throwable throwable) {
        logger.error("[FAIL] {}", message, throwable);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.FAIL, MarkupHelper.createLabel(message, ExtentColor.RED));
        }
    }

    public static void logFail(String message, String throwable, Page page) {
        logger.error("[FAIL] {} {}", message, throwable);
        try {
            ExtentTest test = ReportManager.getTest();
            if (test != null) {
                String screenshotPath = takeScreenshotForValidationFailure(sanitizeFilename(throwable), page);
                test.log(Status.FAIL,  message+ "::::::::",
                        MediaEntityBuilder.createScreenCaptureFromBase64String(screenshotPath).build());
            }
        } catch (IOException e) {
            logger.error("Failed to capture screenshot for validation failure: {}", e.getMessage(), e);
        }
    }

    public static void logError(Exception e, String message) {
        logger.error("[ERROR] {}", message, e);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {

            test.log(Status.FAIL, message);
            if (e != null) {
                test.log(Status.FAIL, e);
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

    public static String takeScreenshotForValidationFailure(
            String methodName,
            Page page) throws IOException {

        String timestamp = new SimpleDateFormat("HH.mm.ss-MM-dd-yyyy")
                .format(new Date());

        Path screenshotDir = Paths.get(
                "reports",
                "Screenshots",
                methodName
        );

        Files.createDirectories(screenshotDir);

        Path screenshotPath = screenshotDir.resolve(
                methodName + "-" + timestamp + ".png"
        );

        byte[] screenshot = page.screenshot(
                new Page.ScreenshotOptions()
                        .setPath(screenshotPath)
                        .setFullPage(true)
        );

        return Base64.getEncoder().encodeToString(screenshot);
    }

    public static String sanitizeFilename(String name) {
        if (name == null) return "unknown";
        String safe = name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return safe.length() > 80 ? safe.substring(0, 80) : safe;
    }

    public static void reportErrorLog(Exception e, String message) {
        try {
            ExtentTest test = ReportManager.getTest();
            String br = "<br>";

            // Find actual failure location (skip framework methods)
            StackTraceElement failurePoint = null;
            if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
                for (StackTraceElement el : e.getStackTrace()) {
                    if (!el.getClassName().contains("BaseSuite")) {
                        failurePoint = el;
                        break;
                    }
                }
                if (failurePoint == null) {
                    failurePoint = e.getStackTrace()[0];
                }
            }

            String className = failurePoint != null ? failurePoint.getClassName() : "unknown";
            String methodName = failurePoint != null ? failurePoint.getMethodName() : "unknown";
            int lineNumber = failurePoint != null ? failurePoint.getLineNumber() : -1;

            String exceptionLocation =
                    "Class: " + className +
                            " | Method: " + methodName +
                            " | Line: " + lineNumber;

            // ---- EXTENT REPORT ----
            if (test != null) {
                test.log(Status.WARNING, MarkupHelper.createLabel(
                        "<b>Message:</b> " + message + br +
                                "<b>Exception:</b> " + e.getClass().getSimpleName() + br +
                                "<b>Exception Message:</b> " + e.getMessage() + br +
                                "<b>Location:</b> " + exceptionLocation + br +
                                "<b>Possible Cause:</b> " + FrameworkException.getExceptionCause(e),
                        ExtentColor.GREY
                ));
            }

            // ---- LOG FILE ----
            logger.error("Test Failure: " + message);

            TestConstants.NUMBEROFFAILURES++;
            Reporter.log(message + " | " + exceptionLocation);

        } catch (Exception ex) {
            logger.error("reportErrorLog(Exception) itself failed: " + ex.getMessage());
        }
    }
}
