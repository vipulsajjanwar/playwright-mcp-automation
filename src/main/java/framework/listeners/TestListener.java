package framework.listeners;

import framework.browser.BrowserManager;
import framework.reporting.ReportManager;
import framework.reporting.ReportUtils;
import framework.utils.ScreenshotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    private static final Logger logger = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        logger.info("Test started: {}", testName);
        ReportManager.createTest(testName, description != null ? description : testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        logger.info("Test passed: {}", testName);
        ReportUtils.logPass("Test passed successfully: " + testName);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        logger.error("Test failed: {}", testName, result.getThrowable());
        ReportUtils.logFail("Test failed: " + testName, result.getThrowable());

        try {
            if (BrowserManager.getPage() != null && !BrowserManager.getPage().isClosed()) {
                String screenshotPath = ScreenshotUtils.captureScreenshot(BrowserManager.getPage(), testName);
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
    }

    @Override
    public void onStart(ITestContext context) {
        logger.info("Starting Context/Test Tag: {}", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("Finished Context/Test Tag: {}", context.getName());
    }
}
