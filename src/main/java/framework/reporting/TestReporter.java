package framework.reporting;

import framework.context.TestContext;
import org.testng.ISuite;

/**
 * Common abstraction for all reporting and observability providers.
 * Implementations integrate with Extent, Allure, ChainTest, ELK, Database, Console, etc.
 */
public interface TestReporter {

    /**
     * Unique human-readable name of the reporter.
     */
    String getReporterName();

    /**
     * Whether this reporter is currently enabled.
     */
    boolean isEnabled();

    /**
     * Called when the test suite execution begins.
     */
    default void onSuiteStart(ISuite suite) {}

    /**
     * Called when a test case begins execution.
     */
    void onTestStart(TestContext context);

    /**
     * Called when a test step is logged.
     */
    void onStep(TestContext context, String message);

    /**
     * Called when a test passes.
     */
    void onTestPass(TestContext context);

    /**
     * Called when a test fails.
     */
    void onTestFail(TestContext context, Throwable throwable);

    /**
     * Called when a test is skipped.
     */
    void onTestSkip(TestContext context, String reason);

    /**
     * Called when a test completes execution (pass, fail, or skip).
     */
    void onTestFinish(TestContext context);

    /**
     * Called when the test suite execution finishes.
     */
    default void onSuiteFinish(ISuite suite) {}
}
