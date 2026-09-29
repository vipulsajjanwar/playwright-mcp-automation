package framework.listeners;

import com.microsoft.playwright.Page;
import framework.browser.BrowserManager;
import framework.config.ConfigManager;
import framework.constants.TestConstants;
import framework.reporting.ReportManager;
import framework.reporting.ReportUtils;
import framework.utils.CustomTimeUtil;
import framework.utils.ScreenshotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    private static final Logger logger = LoggerFactory.getLogger(TestListener.class);
    String timeFormat = ConfigManager.get("time.format", "yyyy-MM-dd HH:mm:ss");
    String timezone = ConfigManager.get("time.zone", "Asia/Kolkata");
    String tStartTime;
    String tendTime;
    String timeLog = "Time taken by test script :: ";
    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        logger.info("Test started: {}", testName);
        tStartTime = CustomTimeUtil.getCurrentTime(timeFormat, timezone);
       // ReportManager.createTest(testName, description != null ? description : testName);
        TestConstants.testCount++;

        String className = result.getTestClass().getRealClass().getSimpleName();
        try {
            ChainTestListener.startTest(
                    testName,
                    className,
                    description,
                    result.getMethod().getGroups()
            );

            ChainTestListener.addBrowser(ConfigManager.get("browser"));
            ChainTestListener.addEnvironment(ConfigManager.get("env"));
            ChainTestListener.addTestClass(className);
        } catch (Exception reportingException) {
            logger.warn("Failed to report test start to ChainTest: {}", reportingException.getMessage());
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        logger.info("Test passed: {}", testName);
        ReportUtils.logPass("Test passed successfully: " + testName);
        tendTime = CustomTimeUtil.getCurrentTime(timeFormat, timezone);
        ReportUtils.logInfo(timeLog + CustomTimeUtil.getTimeDifference(tStartTime, tendTime, timeFormat));
        TestConstants.passedTC++;
        try {
            ChainTestListener.passTest();
        } catch (Exception reportingException) {
            logger.warn("Failed to report test success to ChainTest: {}", reportingException.getMessage());
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        logger.error("Test failed: {}", testName, result.getThrowable());
        ReportUtils.logFail("Test failed: " + testName, result.getThrowable());
        tendTime = CustomTimeUtil.getCurrentTime(timeFormat, timezone);
        ReportUtils.logInfo(timeLog + CustomTimeUtil.getTimeDifference(tStartTime, tendTime, timeFormat));
        TestConstants.failedTC++;
        Page page = null;
        try {
            page = BrowserManager.getPage();
        } catch (Exception ignored) {
            // Non-UI tests do not have a Playwright Page initialized
        }

        try {
            ChainTestListener.failTest(result.getThrowable(), page);
        } catch (Exception reportingException) {
            logger.warn("Failed to report test failure to ChainTest: {}", reportingException.getMessage());
        }

        try {
            if (page != null && !page.isClosed()) {
                String screenshotPath = ScreenshotUtils.captureScreenshot(page, testName);
                if (screenshotPath != null && !screenshotPath.isEmpty()) {
                    ReportUtils.logScreenshot(screenshotPath, "Failure Screenshot");
                }
            }
        } catch (Exception e) {
            logger.warn("Could not capture screenshot on failure for test {}: {}", testName, e.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        logger.warn("Test skipped: {}", testName);
        ReportUtils.logInfo("Test skipped: " + testName);
        tendTime = CustomTimeUtil.getCurrentTime(timeFormat, timezone);
        ReportUtils.logInfo(timeLog + CustomTimeUtil.getTimeDifference(tStartTime, tendTime, timeFormat));
        TestConstants.skippedTC++;
        String skipReason = result.getThrowable() != null
                ? (result.getThrowable().getMessage() != null ? result.getThrowable().getMessage() : result.getThrowable().toString())
                : "Test skipped";
        try {
            ChainTestListener.skipTest(skipReason);
        } catch (Exception reportingException) {
            logger.warn("Failed to report test skip to ChainTest: {}", reportingException.getMessage());
        }
    }

    @Override
    public void onStart(ITestContext context) {
        logger.info("Starting Context/Test Tag: {}", context.getName());
        TestConstants.ReportName = ReportUtils.sanitizeFilename(context.getCurrentXmlTest().getSuite().getName());
        TestConstants.startTime = CustomTimeUtil.getCurrentTime(timeFormat, timezone);
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("Finished Context/Test Tag: {}", context.getName());
        TestConstants.endTime = CustomTimeUtil.getCurrentTime(timeFormat, timezone);
        try {
            ChainTestListener.flush();
        } catch (Exception reportingException) {
            logger.warn("Failed to flush ChainTest: {}", reportingException.getMessage());
        }
    }
}
