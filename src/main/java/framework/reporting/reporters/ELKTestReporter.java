package framework.reporting.reporters;

import framework.config.ReportingConfig;
import framework.context.TestContext;
import framework.reporting.TestReporter;
import framework.reporting.elk.ELKStackLogger;
import framework.reporting.elk.ELKTestResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;

/**
 * Elasticsearch / ELK Stack implementation of {@link TestReporter}.
 * Serializes {@link TestContext} via {@link ELKTestResultBuilder} and pushes
 * documents asynchronously via {@link ELKStackLogger}.
 */
public class ELKTestReporter implements TestReporter {

    private static final Logger logger = LoggerFactory.getLogger(ELKTestReporter.class);

    @Override
    public String getReporterName() {
        return "ELKStack";
    }

    @Override
    public boolean isEnabled() {
        return ReportingConfig.isElkEnabled();
    }

    @Override
    public void onSuiteStart(ISuite suite) {
        // No-op
    }

    @Override
    public void onTestStart(TestContext context) {
        // No-op
    }

    @Override
    public void onStep(TestContext context, String message) {
        // ELK tracks summary test documents upon completion
    }

    @Override
    public void onTestPass(TestContext context) {
        dispatch(context);
    }

    @Override
    public void onTestFail(TestContext context, Throwable throwable) {
        dispatch(context);
    }

    @Override
    public void onTestSkip(TestContext context, String reason) {
        dispatch(context);
    }

    @Override
    public void onTestFinish(TestContext context) {
        // Dispatched during pass/fail/skip
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (!isEnabled()) return;
        ELKStackLogger.shutdown();
    }

    private void dispatch(TestContext context) {
        if (!isEnabled() || context == null) return;
        try {
            String jsonPayload = ELKTestResultBuilder.buildJsonPayload(context);
            ELKStackLogger.sendResult(jsonPayload);
        } catch (Exception e) {
            logger.warn("ELK dispatch failed: {}", e.getMessage());
        }
    }
}
