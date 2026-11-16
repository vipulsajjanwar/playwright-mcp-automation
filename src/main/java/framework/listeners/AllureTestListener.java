/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: TestNG listener that integrates Allure reporting hooks for test start, finish, failure, and step events.
 * Created: 2026-09-28
 */
package framework.listeners;

import ch.qos.logback.core.status.Status;
import com.microsoft.playwright.Page;
import framework.browser.BrowserManager;
import framework.config.ConfigManager;
import framework.utils.AllureUtils;
import io.qameta.allure.Allure;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Map;

public class AllureTestListener implements ITestListener, ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        if (!AllureUtils.isEnabled()) {
            return;
        }
        String suiteName = suite != null ? suite.getName() : "Playwright Automation Suite";
        framework.reporting.allure.ReportPathManager.ExecutionPaths paths = framework.reporting.allure.ReportPathManager.initializeExecutionPaths(suiteName);
        framework.reporting.allure.AllureHistoryManager.preserveHistory(paths.suiteName(), paths.resultsDir(), paths.executionDir());

        // Once per run: populates the Environment widget and defect categories.
        AllureUtils.writeEnvironment(Map.of(
                "Base URL", ConfigManager.get("baseUrl") != null ? ConfigManager.get("baseUrl") : "http://localhost",
                "Browser", ConfigManager.get("browser") != null ? ConfigManager.get("browser") : "chromium",
                "Java", System.getProperty("java.version") != null ? System.getProperty("java.version") : "unknown",
                "OS", System.getProperty("os.name") != null ? System.getProperty("os.name") : "unknown"
        ));
        AllureUtils.writeDefaultCategories();
    }

    @Override
    public void onFinish(ISuite suite) {
        if (!AllureUtils.isEnabled()) {
            return;
        }
        String suiteName = suite != null ? suite.getName() : "Playwright Automation Suite";
        framework.reporting.allure.AllureReportManager.onSuiteFinish(suiteName);
    }

    @Override
    public void onTestStart(ITestResult result) {

        AllureUtils.step("Test Started", () -> {
            AllureUtils.attachText("Test Name", result.getMethod().getMethodName());
            AllureUtils.attachText("Description", result.getMethod().getDescription());
        });

    }

    @Override
    public void onTestSuccess(ITestResult result) {

       AllureUtils.step("Test Passed", () -> {
            AllureUtils.attachText("Test Success", "Test passed: " + result.getMethod().getMethodName());
        });
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (!AllureUtils.isEnabled()) {
            return; // don't even capture the screenshot / page source
        }
        AllureUtils.attachStackTrace("Stack trace", result.getThrowable());

        Page page = BrowserManager.getPage();
        if (page != null) {
            try {
                AllureUtils.attachScreenshot("Screenshot on failure",
                        page.screenshot(new Page.ScreenshotOptions().setFullPage(true)));
                AllureUtils.attachText("Page URL", page.url());
                AllureUtils.attachHtml("Page HTML", "<pre>" + AllureUtils.escape(page.content()) + "</pre>");
            } catch (RuntimeException e) {
                AllureUtils.attachText("Page capture failed", e.toString());
            }
        }

        AllureUtils.step("Test Failed", () -> {
            AllureUtils.attachText("Test Failure", "Test failed: " + result.getMethod().getMethodName());
        });


    }


}
