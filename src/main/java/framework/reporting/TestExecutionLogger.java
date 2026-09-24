package framework.reporting;

import framework.constants.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.List;

public class TestExecutionLogger {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionLogger.class);

    private TestExecutionLogger() {}

    public static void logStep(String stepDescription) {
        String logMsg = String.format("[STEP] [Thread-%d] %s", Thread.currentThread().getId(), stepDescription);
        logger.info(logMsg);
        ReportUtils.logInfo(logMsg);
    }

    public static void finalizeTest(
            boolean loginFlag,
            Runnable clickTabAction,
            List<String> testResults,
            String testcaseId,
            String testcaseName) {

        // Check if the test should be skipped
        if (!loginFlag) {
            throw new SkipException("Test skipped : " + testcaseId);
        }

        if(clickTabAction != null) {
            // Click the appropriate tab
            clickTabAction.run();
        }


        // Check for skipped tests
        if (TestConstants.NUMBEROFSKIPS > 0) {
            throw new SkipException("Test skipped : " + testcaseId);
        }

        // Check for failed tests
        if (TestConstants.NUMBEROFFAILURES > 0) {
             throw new AssertionError(testcaseId+" : " + testcaseName + " failed. Please check the report for details.");
        }
    }
}
