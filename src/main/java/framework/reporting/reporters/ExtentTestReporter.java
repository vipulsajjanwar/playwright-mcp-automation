package framework.reporting.reporters;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import framework.annotations.TestMetadata;
import framework.config.ReportingConfig;
import framework.context.TestContext;
import framework.reporting.ReportManager;
import framework.reporting.TestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;

import java.io.File;
import java.util.Base64;

/**
 * ExtentReports implementation of {@link TestReporter}.
 * Automatically configures categories, authors, device info, steps, and screenshots.
 */
public class ExtentTestReporter implements TestReporter {

    private static final Logger logger = LoggerFactory.getLogger(ExtentTestReporter.class);
    private static final String EXTENT_TEST_KEY = "extentTest";

    @Override
    public String getReporterName() {
        return "ExtentReports";
    }

    @Override
    public boolean isEnabled() {
        return ReportingConfig.isExtentEnabled();
    }

    @Override
    public void onSuiteStart(ISuite suite) {
        if (!isEnabled()) return;
        try {
         //   String suiteName = suite != null ? suite.getName() : "Automation Suite";
          //  ReportManager.getExtentReports(suiteName);
        } catch (Exception e) {
            logger.warn("Could not pre-initialize ExtentReports onSuiteStart: {}", e.getMessage());
        }
    }

    @Override
    public void onTestStart(TestContext context) {
        if (!isEnabled() || context == null) return;

        try {
            TestMetadata metadata = context.getMetadata();
            String testDisplayName = context.getTestName() != null && !context.getTestName().isBlank()
                    ? context.getTestName()
                    : "Test";
            if (context.isRetry()) {
                testDisplayName += " [RETRY #" + context.getRetryCount() + "]";
            }

            ExtentReports reports = ReportManager.getExtentReports(context.getSuiteName());
            String desc = metadata != null ? metadata.getDescription() : "";
            ExtentTest extentTest = reports.createTest(testDisplayName, desc != null ? desc : "");

            // Assign metadata
            if (metadata != null) {
                if (!metadata.getCategory().isBlank()) {
                    extentTest.assignCategory(metadata.getCategory());
                }
                for (String tag : metadata.getTags()) {
                    if (!tag.isBlank()) {
                        extentTest.assignCategory(tag);
                    }
                }
                if (!metadata.getEffectiveAuthor().isBlank()) {
                    extentTest.assignAuthor(metadata.getEffectiveAuthor());
                }
                if (!metadata.getPriority().isBlank()) {
                    extentTest.assignCategory("Priority:" + metadata.getPriority());
                }
            }

            if (context.getBrowser() != null && !context.getBrowser().isBlank()) {
                extentTest.assignDevice(context.getBrowser());
            }

            context.setAttribute(EXTENT_TEST_KEY, extentTest);
        } catch (Exception e) {
            logger.warn("Failed to create ExtentTest node: {}", e.getMessage());
        }
    }

    @Override
    public void onStep(TestContext context, String message) {
        if (!isEnabled() || context == null || message == null) return;

        ExtentTest test = getExtentTest(context);
        if (test == null) return;

        String msg = message.trim();

        if (msg.startsWith("[TABLE]")) {
            // Extract HTML table content after the title line
            int newlineIdx = msg.indexOf('\n');
            String htmlContent = (newlineIdx > 0) ? msg.substring(newlineIdx + 1) : msg;
            test.log(Status.INFO, htmlContent);
        } else if (msg.startsWith("[PASS]")) {
            test.log(Status.PASS, MarkupHelper.createLabel(msg, ExtentColor.GREEN));
        } else if (msg.startsWith("[FAIL]")) {
            test.log(Status.FAIL, MarkupHelper.createLabel(msg, ExtentColor.RED));
        } else if (msg.startsWith("[ERROR]")) {
            test.log(Status.FAIL, MarkupHelper.createLabel(msg, ExtentColor.RED));
        } else if (msg.startsWith("[WARN]")) {
            test.log(Status.WARNING, MarkupHelper.createLabel(msg, ExtentColor.ORANGE));
        } else if (msg.startsWith("[SKIP]")) {
            test.log(Status.SKIP, MarkupHelper.createLabel(msg, ExtentColor.AMBER));
        } else {
            test.log(Status.INFO, MarkupHelper.createLabel(msg, ExtentColor.TRANSPARENT));
        }
    }

    @Override
    public void onTestPass(TestContext context) {
        if (!isEnabled() || context == null) return;
        ExtentTest test = getExtentTest(context);
        if (test != null) {
            test.log(Status.PASS, MarkupHelper.createLabel("Test Passed Successfully (Duration: "
                    + context.getDurationMs() + "ms)", ExtentColor.GREEN));
        }
    }

    @Override
    public void onTestFail(TestContext context, Throwable throwable) {
        if (!isEnabled() || context == null) return;

        ExtentTest test = getExtentTest(context);
        if (test != null) {
            String msg = throwable != null ? throwable.getMessage() : "Test failed";

            byte[] bytes = context.getLatestScreenshotBytes();
            if (bytes != null && bytes.length > 0) {
                String base64 = Base64.getEncoder().encodeToString(bytes);
                test.fail("Test Failed: " + msg,
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
            } else {
                test.fail("Test Failed: " + msg);
            }

            if (throwable != null) {
                test.fail(throwable);
            }
        }
    }

    @Override
    public void onTestSkip(TestContext context, String reason) {
        if (!isEnabled() || context == null) return;
        ExtentTest test = getExtentTest(context);
        if (test != null) {
            test.log(Status.SKIP, MarkupHelper.createLabel("Test Skipped: "
                    + (reason != null ? reason : "Condition not met"), ExtentColor.ORANGE));
        }
    }

    @Override
    public void onTestFinish(TestContext context) {
        // Individual test completion
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (!isEnabled()) return;
        try {
            ReportManager.flushReports();
            logger.info("ExtentReports flushed successfully.");
        } catch (Exception e) {
            logger.warn("Failed to flush ExtentReports on suite finish: {}", e.getMessage());
        }
    }

    private ExtentTest getExtentTest(TestContext context) {
        ExtentTest test = context.getAttribute(EXTENT_TEST_KEY);
        if (test == null) {
            test = ReportManager.getTest();
        }
        return test;
    }
}
