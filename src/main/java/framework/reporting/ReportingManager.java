package framework.reporting;

import framework.context.TestContext;
import framework.reporting.reporters.AllureTestReporter;
import framework.reporting.reporters.ChainTestReporter;
import framework.reporting.reporters.ConsoleTestReporter;
import framework.reporting.reporters.DatabaseTestReporter;
import framework.reporting.reporters.ELKTestReporter;
import framework.reporting.reporters.ExtentTestReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central orchestrator for dispatching test lifecycle events to all registered
 * {@link TestReporter} implementations.
 *
 * <p>Protects test execution by isolating each reporter in fail-safe try-catch blocks.
 * A failure in any one reporting integration will never fail a test or disrupt other reporters.
 */
public final class ReportingManager {

    private static final Logger logger = LoggerFactory.getLogger(ReportingManager.class);
    private static final ReportingManager INSTANCE = new ReportingManager();

    private final List<TestReporter> reporters = new CopyOnWriteArrayList<>();

    private ReportingManager() {
        // Register default reporters
        reporters.add(new ConsoleTestReporter());
        reporters.add(new ExtentTestReporter());
        reporters.add(new AllureTestReporter());
        reporters.add(new ChainTestReporter());
        reporters.add(new ELKTestReporter());
        reporters.add(new DatabaseTestReporter());
    }

    public static ReportingManager getInstance() {
        return INSTANCE;
    }

    public void registerReporter(TestReporter reporter) {
        if (reporter != null && !reporters.contains(reporter)) {
            reporters.add(reporter);
            logger.info("Registered custom reporter: {}", reporter.getReporterName());
        }
    }

    public List<TestReporter> getRegisteredReporters() {
        return List.copyOf(reporters);
    }

    private final java.util.Set<String> startedSuites = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> finishedSuites = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public void onSuiteStart(ISuite suite) {
        String suiteKey = suite != null ? suite.getName() : "default_suite";
        if (!startedSuites.add(suiteKey)) {
            return;
        }
        for (TestReporter reporter : reporters) {
            if (reporter.isEnabled()) {
                try {
                    reporter.onSuiteStart(suite);
                } catch (Throwable t) {
                    logger.warn("Reporter [{}] failed onSuiteStart: {}", reporter.getReporterName(), t.getMessage());
                }
            }
        }
    }

    public void onTestStart(TestContext context) {
        if (context == null) return;
        for (TestReporter reporter : reporters) {
            if (reporter.isEnabled()) {
                try {
                    reporter.onTestStart(context);
                } catch (Throwable t) {
                    logger.warn("Reporter [{}] failed onTestStart: {}", reporter.getReporterName(), t.getMessage());
                }
            }
        }
    }

    public void onStep(TestContext context, String message) {
        if (context != null) {
            context.addStepLog(message);
        }
        for (TestReporter reporter : reporters) {
            if (reporter.isEnabled()) {
                try {
                    reporter.onStep(context, message);
                } catch (Throwable t) {
                    logger.warn("Reporter [{}] failed onStep: {}", reporter.getReporterName(), t.getMessage());
                }
            }
        }
    }

    public void onTestPass(TestContext context) {
        if (context == null) return;
        context.markPassed();
        for (TestReporter reporter : reporters) {
            if (reporter.isEnabled()) {
                try {
                    reporter.onTestPass(context);
                } catch (Throwable t) {
                    logger.warn("Reporter [{}] failed onTestPass: {}", reporter.getReporterName(), t.getMessage());
                }
            }
        }
    }

    public void onTestFail(TestContext context, Throwable throwable) {
        if (context == null) return;
        context.markFailed(throwable);
        for (TestReporter reporter : reporters) {
            if (reporter.isEnabled()) {
                try {
                    reporter.onTestFail(context, throwable);
                } catch (Throwable t) {
                    logger.warn("Reporter [{}] failed onTestFail: {}", reporter.getReporterName(), t.getMessage());
                }
            }
        }
    }

    public void onTestSkip(TestContext context, String reason) {
        if (context == null) return;
        context.markSkipped(reason);
        for (TestReporter reporter : reporters) {
            if (reporter.isEnabled()) {
                try {
                    reporter.onTestSkip(context, reason);
                } catch (Throwable t) {
                    logger.warn("Reporter [{}] failed onTestSkip: {}", reporter.getReporterName(), t.getMessage());
                }
            }
        }
    }

    public void onTestFinish(TestContext context) {
        if (context == null) return;
        for (TestReporter reporter : reporters) {
            if (reporter.isEnabled()) {
                try {
                    reporter.onTestFinish(context);
                } catch (Throwable t) {
                    logger.warn("Reporter [{}] failed onTestFinish: {}", reporter.getReporterName(), t.getMessage());
                }
            }
        }
    }

    public void onSuiteFinish(ISuite suite) {
        String suiteKey = suite != null ? suite.getName() : "default_suite";
        if (!finishedSuites.add(suiteKey)) {
            return;
        }
        for (TestReporter reporter : reporters) {
            if (reporter.isEnabled()) {
                try {
                    reporter.onSuiteFinish(suite);
                } catch (Throwable t) {
                    logger.warn("Reporter [{}] failed onSuiteFinish: {}", reporter.getReporterName(), t.getMessage());
                }
            }
        }
    }
}
