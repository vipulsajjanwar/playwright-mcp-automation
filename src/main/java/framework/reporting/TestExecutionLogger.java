package framework.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestExecutionLogger {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionLogger.class);

    private TestExecutionLogger() {}

    public static void logStep(String stepDescription) {
        String logMsg = String.format("[STEP] [Thread-%d] %s", Thread.currentThread().getId(), stepDescription);
        logger.info(logMsg);
        ReportUtils.logInfo(logMsg);
    }
}
