package framework.base;

import com.microsoft.playwright.Page;
import framework.browser.BrowserFactory;
import framework.browser.BrowserManager;
import framework.browser.PlaywrightManager;
import framework.config.ConfigManager;
import framework.utils.PageUtils;
import framework.utils.TraceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

public abstract class BaseTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        logger.info("================ Starting Test: {} ================", method.getName());
        BrowserManager.initThreadSession();
        TraceUtils.startTracing(BrowserManager.getContext());

        String baseUrl = ConfigManager.get("baseUrl");
        if (baseUrl != null && !baseUrl.isEmpty() && shouldNavigateBaseUrl()) {
            PageUtils.navigate(page(), baseUrl);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        boolean isPassed = result.getStatus() == ITestResult.SUCCESS;

        try {
            TraceUtils.stopTracing(BrowserManager.getContext(), testName, isPassed);
        } catch (Exception e) {
            logger.warn("Failed to finalize tracing in tearDown: {}", e.getMessage());
        } finally {
            BrowserManager.closeThreadSession();
            logger.info("================ Finished Test: {} [Status: {}] ================",
                    testName, isPassed ? "PASSED" : "FAILED");
        }
    }

    @AfterSuite(alwaysRun = true)
    public void globalTearDown() {
        logger.info("Closing global browser and playwright instances...");
        BrowserFactory.closeBrowser();
        PlaywrightManager.closePlaywright();
    }

    protected Page page() {
        return BrowserManager.getPage();
    }

    protected boolean shouldNavigateBaseUrl() {
        return true;
    }
}
