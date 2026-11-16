/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: TestReporter implementation writing colour-coded test progress and results to standard console output.
 * Created: 2026-10-01
 */
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
        String msg = message != null ? message.trim() : "";

        if (msg.startsWith("[TABLE]")) {
            // Tables don't render in console — log just the title
            int newlineIdx = msg.indexOf('\n');
            String title = (newlineIdx > 0) ? msg.substring(0, newlineIdx) : msg;
            logger.info("[STEP] [Thread: {}] {}", threadId, title);
        } else if (msg.startsWith("[PASS]")) {
            logger.info("[PASS] [Thread: {}] {}", threadId, msg);
        } else if (msg.startsWith("[FAIL]") || msg.startsWith("[ERROR]")) {
            logger.error("[STEP] [Thread: {}] {}", threadId, msg);
        } else if (msg.startsWith("[WARN]")) {
            logger.warn("[STEP] [Thread: {}] {}", threadId, msg);
        } else if (msg.startsWith("[SKIP]")) {
            logger.warn("[SKIP] [Thread: {}] {}", threadId, msg);
        } else {
            logger.info("[STEP] [Thread: {}] {}", threadId, msg);
        }
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
