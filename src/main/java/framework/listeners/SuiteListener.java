/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: TestNG ISuiteListener implementation handling suite-level setup, teardown, and cross-suite reporting aggregation.
 * Created: 2026-08-15
 */
package framework.listeners;

import framework.constants.TestConstants;
import framework.reporting.ReportUtils;
import framework.reporting.ReportingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Suite-level TestNG listener that delegates suite start and finish events
 * to {@link ReportingManager}.
 */
public class SuiteListener implements ISuiteListener {

    private static final Logger logger = LoggerFactory.getLogger(SuiteListener.class);

    @Override
    public void onStart(ISuite suite) {
        String suiteName = suite != null ? suite.getName() : "Playwright Automation Suite";
        TestConstants.SuiteName = ReportUtils.sanitizeFilename(suiteName);
        logger.info("================ Starting Test Suite: {} ================", suiteName);
        ReportingManager.getInstance().onSuiteStart(suite);
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info("================ Finishing Test Suite: {} ================", suite != null ? suite.getName() : "");
        ReportingManager.getInstance().onSuiteFinish(suite);
    }
}
