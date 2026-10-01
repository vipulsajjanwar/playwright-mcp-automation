package framework.listeners;

import com.microsoft.playwright.Page;
import framework.browser.BrowserManager;
import framework.constants.TestConstants;
import framework.context.TestContext;
import framework.context.TestContextFactory;
import framework.context.TestContextManager;
import framework.reporting.ReportUtils;
import framework.reporting.ReportingManager;
import framework.reporting.artifacts.ArtifactManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Primary TestNG listener acting as the central entry point for test execution lifecycle.
 * Automatically resolves {@link framework.annotations.CustomTest} metadata, initializes
 * the thread-safe {@link TestContext}, captures Playwright artifacts, and dispatches events
 * to {@link ReportingManager}.
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger logger = LoggerFactory.getLogger(TestListener.class);

    @Override
    public void onStart(ISuite suite) {
        String suiteName = suite != null ? suite.getName() : "Playwright Automation Suite";
        TestConstants.SuiteName = ReportUtils.sanitizeFilename(suiteName);
        logger.info("Initializing Test Suite: {}", suiteName);
        ReportingManager.getInstance().onSuiteStart(suite);
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info("Finalizing Test Suite: {}", suite != null ? suite.getName() : "");
        ReportingManager.getInstance().onSuiteFinish(suite);
    }

    @Override
    public void onStart(ITestContext context) {
        logger.info("Starting Test Tag: {}", context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        logger.info("Finished Test Tag: {}", context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        TestContext context = TestContextFactory.createTestContext(result);
        TestContextManager.setContext(context);

        synchronized (TestConstants.class) {
            TestConstants.testCount++;
        }

        ReportingManager.getInstance().onTestStart(context);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        TestContext context = getOrCreateContext(result);

        synchronized (TestConstants.class) {
            TestConstants.passedTC++;
            TestConstants.NUMBEROFPASSES++;
        }

        try {
            ReportingManager.getInstance().onTestPass(context);
            ReportingManager.getInstance().onTestFinish(context);
        } finally {
            TestContextManager.clearContext();
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        TestContext context = getOrCreateContext(result);

        synchronized (TestConstants.class) {
            TestConstants.failedTC++;
            TestConstants.NUMBEROFFAILURES++;
        }

        // Automatically capture Playwright failure artifacts before browser is closed
        Page page = null;
        try {
            page = BrowserManager.getPage();
        } catch (Exception ignored) {
            // Non-UI tests do not have a Playwright Page initialized
        }

        if (page != null) {
            ArtifactManager.captureFailureArtifacts(page, context);
        }

        try {
            ReportingManager.getInstance().onTestFail(context, result.getThrowable());
            ReportingManager.getInstance().onTestFinish(context);
        } finally {
            TestContextManager.clearContext();
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        TestContext context = getOrCreateContext(result);

        synchronized (TestConstants.class) {
            TestConstants.skippedTC++;
            TestConstants.NUMBEROFSKIPS++;
        }

        String skipReason = result.getThrowable() != null
                ? (result.getThrowable().getMessage() != null ? result.getThrowable().getMessage() : result.getThrowable().toString())
                : "Test skipped";

        try {
            ReportingManager.getInstance().onTestSkip(context, skipReason);
            ReportingManager.getInstance().onTestFinish(context);
        } finally {
            TestContextManager.clearContext();
        }
    }

    private TestContext getOrCreateContext(ITestResult result) {
        TestContext context = TestContextManager.getContext();
        if (context == null) {
            context = TestContextFactory.createTestContext(result);
            TestContextManager.setContext(context);
        }
        return context;
    }
}
