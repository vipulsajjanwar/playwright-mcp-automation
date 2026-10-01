package framework.reporting;

import framework.constants.TestConstants;
import framework.context.TestContext;
import framework.context.TestContextManager;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.List;

/**
 * Legacy execution logger preserved for backwards compatibility.
 * Delegates log steps and assertions directly to {@link FrameworkLogger} and {@link ReportingManager}.
 */
public final class TestExecutionLogger {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionLogger.class);

    private TestExecutionLogger() {}

    public static void logStep(String stepDescription) {
        FrameworkLogger.step(stepDescription);
    }

    public static void OnStartTest(String testcaseId, String testcaseName) {
        TestContext context = TestContextManager.getContext();
        if (context != null) {
            FrameworkLogger.info(String.format("Starting Test [%s] - %s", testcaseId, testcaseName));
        } else {
            logger.info("[START TEST] Testcase ID: {}, Name: {}", testcaseId, testcaseName);
        }
    }

    public static void finalizeTest(
            boolean loginFlag,
            Runnable clickTabAction,
            List<String> testResults,
            String testcaseId,
            String testcaseName) {

        if (!loginFlag) {
            Allure.step("Test skipped: " + testcaseId, () -> {
                throw new SkipException("Pre-condition not met, skipping: " + testcaseId);
            });
            throw new SkipException("Pre-condition not met, skipping: " + testcaseId);
        }

        if (clickTabAction != null) {
            try {
                clickTabAction.run();
            } catch (Exception e) {
                logger.warn("clickTabAction failed: {}", e.getMessage());
            }
        }

        if (TestConstants.NUMBEROFSKIPS > 0) {
            throw new SkipException("Test skipped: " + testcaseId);
        }

        if (TestConstants.NUMBEROFFAILURES > 0) {
            throw new AssertionError(testcaseId + " (" + testcaseName + ") encountered failures.");
        }
    }
}
