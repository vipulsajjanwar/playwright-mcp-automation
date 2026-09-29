package framework.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.microsoft.playwright.Page;
import framework.config.ConfigManager;
import framework.constants.FrameworkConstants;
import framework.constants.TestConstants;
import framework.exceptions.FrameworkException;
import framework.utils.AllureUtils;
import framework.utils.ChainTestUtil;
import io.qameta.allure.Allure;
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

    private ReportUtils() {
    }

    public static void logInfo(String message) {
        logger.info("[INFO] {}", message);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.INFO, MarkupHelper.createLabel(message, ExtentColor.TRANSPARENT));
        }
        if (AllureUtils.isEnabled()) {
            AllureUtils.attachLog("INFO", message);
        }
        if (ChainTestUtil.isEnabled()) {
            ChainTestUtil.info(message);
        }
    }

    public static void logPass(String message) {
        logger.info("[PASS] {}", message);
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.PASS, MarkupHelper.createLabel(message, ExtentColor.GREEN));
        }
        if (AllureUtils.isEnabled()) {
            AllureUtils.attachLog("PASS", message);
        }
        if (ChainTestUtil.isEnabled()) {
            ChainTestUtil.pass(message);
        }
    }

    public static void logFail(String message, Throwable throwable) {
        logger.error("[FAIL] {}", message, throwable);
        TestConstants.NUMBEROFFAILURES++;
        ExtentTest test = ReportManager.getTest();
        if (test != null) {
            test.log(Status.FAIL, MarkupHelper.createLabel(message, ExtentColor.RED));
        }
        if (AllureUtils.isEnabled()) {
            AllureUtils.attachLog("FAIL", message);
            AllureUtils.attachStackTrace("Stack trace", throwable);
            Allure.getLifecycle().updateStep(s -> s.setStatus(io.qameta.allure.model.Status.FAILED));
        }
        if (ChainTestUtil.isEnabled()) {
            ChainTestUtil.fail(message, throwable);
        }
    }

    public static void logFail(String message, String throwable, Page page) {
        logger.error("[FAIL] {} {}", message, throwable);
        try {
            String screenshotPath = null;
            ExtentTest test = ReportManager.getTest();
            if (test != null) {
                screenshotPath = takeScreenshotForValidationFailure(sanitizeFilename(throwable), page);
                test.log(Status.FAIL, message + "::::::::", MediaEntityBuilder.createScreenCaptureFromBase64String(screenshotPath).build());
            }
            TestConstants.NUMBEROFFAILURES++;
            if (AllureUtils.isEnabled()) {
                AllureUtils.attachLog("FAIL", message);
                AllureUtils.attachStackTrace("Stack trace", new FrameworkException(throwable));
                if (page != null) {
                    AllureUtils.attachScreenshot("Screenshot on failure",
                            page.screenshot(new Page.ScreenshotOptions().setFullPage(true)));
                    AllureUtils.attachLog("Page URL", page.url());
                    AllureUtils.attachLog("Page HTML", "<pre>" + AllureUtils.escape(page.content()) + "</pre>");
                }
            }

            if (ChainTestUtil.isEnabled()) {
                ChainTestUtil.fail(message, new FrameworkException(throwable));
                Path screenshotDir = Paths.get(FrameworkConstants.SCREENSHOT_DIR, throwable);
                Files.createDirectories(screenshotDir);
                ChainTestUtil.attachImage("Screenshot on failure", page.screenshot(
                        new Page.ScreenshotOptions()
                                .setPath(screenshotDir)
                                .setFullPage(true)
                ));
            }

        } catch (IOException e) {
            logger.error("Failed to capture screenshot for validation failure: {}", e.getMessage(), e);
        }
    }

    public static void logError(Exception e, String message) {
        logger.error("[ERROR] {}", message, e);
        TestConstants.NUMBEROFSKIPS++;
        ExtentTest test = ReportManager.getTest();
        if (test != null) {

            test.log(Status.FAIL, message);
            if (e != null) {
                test.log(Status.FAIL, e);
            }
        }
        if (AllureUtils.isEnabled()) {
            AllureUtils.attachLog("ERROR", message);
            if (e != null) {
                AllureUtils.attachStackTrace("Stack trace", e);
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
        if (AllureUtils.isEnabled() && screenshotPath != null && !screenshotPath.isEmpty()) {
            try {
                byte[] screenshotBytes = Files.readAllBytes(Paths.get(screenshotPath));
                AllureUtils.attachScreenshot(title, screenshotBytes);
            } catch (IOException e) {
                logger.error("Failed to attach screenshot to Allure report", e);
            }
        }
    }

    public static String takeScreenshotForValidationFailure(
            String methodName,
            Page page) throws IOException {

        String timestamp = new SimpleDateFormat(ConfigManager.get("timestamp.format"))
                .format(new Date());

        Path screenshotDir = Paths.get(FrameworkConstants.SCREENSHOT_DIR, methodName);

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
