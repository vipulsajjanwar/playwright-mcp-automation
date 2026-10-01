package framework.reporting.reporters;

import framework.annotations.TestMetadata;
import framework.config.ReportingConfig;
import framework.context.TestContext;
import framework.reporting.TestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;

/**
 * Console and log file reporter using SLF4J / Logback.
 * Outputs formatted, unified logs without duplication.
 */
public class ConsoleTestReporter implements TestReporter {

    private static final Logger logger = LoggerFactory.getLogger("TestExecution");

    @Override
    public String getReporterName() {
        return "Console";
    }

    @Override
    public boolean isEnabled() {
        return ReportingConfig.isConsoleEnabled();
    }

    @Override
    public void onSuiteStart(ISuite suite) {
        if (!isEnabled()) return;
        logger.info("================================================================================");
        logger.info("STARTING SUITE: {}", suite != null ? suite.getName() : "Automation Suite");
        logger.info("================================================================================");
    }

    @Override
    public void onTestStart(TestContext context) {
        if (!isEnabled() || context == null) return;
        TestMetadata meta = context.getMetadata();
        String testId = meta != null && !meta.getTestId().isBlank() ? meta.getTestId() : context.getMethodName();
        String module = meta != null ? meta.getModule() : "General";
        String priority = meta != null ? meta.getPriority() : "MEDIUM";

        logger.info("[TEST START] [{}] - '{}' | Module: {} | Priority: {} | Thread: {}",
                testId, context.getTestName(), module, priority, context.getThreadId());
    }

    @Override
    public void onStep(TestContext context, String message) {
        if (!isEnabled()) return;
        long threadId = context != null ? context.getThreadId() : Thread.currentThread().getId();
        logger.info("[STEP] [Thread: {}] {}", threadId, message);
    }

    @Override
    public void onTestPass(TestContext context) {
        if (!isEnabled() || context == null) return;
        logger.info("[TEST PASS] '{}' completed in {} ms [Thread: {}]",
                context.getTestName(), context.getDurationMs(), context.getThreadId());
    }

    @Override
    public void onTestFail(TestContext context, Throwable throwable) {
        if (!isEnabled() || context == null) return;
        String reason = throwable != null ? throwable.getMessage() : context.getFailureReason();
        logger.error("[TEST FAIL] '{}' failed after {} ms [Thread: {}] - Reason: {}",
                context.getTestName(), context.getDurationMs(), context.getThreadId(), reason);
        if (throwable != null) {
            logger.error("Exception details: ", throwable);
        }
    }

    @Override
    public void onTestSkip(TestContext context, String reason) {
        if (!isEnabled() || context == null) return;
        logger.warn("[TEST SKIP] '{}' skipped [Thread: {}] - Reason: {}",
                context.getTestName(), context.getThreadId(), reason);
    }

    @Override
    public void onTestFinish(TestContext context) {
        // Individual test completion
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (!isEnabled()) return;
        logger.info("================================================================================");
        logger.info("FINISHED SUITE: {}", suite != null ? suite.getName() : "Automation Suite");
        logger.info("================================================================================");
    }
}
