package framework.listeners;

import framework.constants.TestConstants;
import framework.reporting.ReportManager;
import framework.reporting.ReportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;

public class SuiteListener implements ISuiteListener {

    private static final Logger logger = LoggerFactory.getLogger(SuiteListener.class);

    @Override
    public void onStart(ISuite suite) {
        logger.info("================ Starting Test Suite: {} ================", suite.getName());
        TestConstants.SuiteName = ReportUtils.sanitizeFilename(suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        logger.info("================ Finishing Test Suite: {} ================", suite.getName());
        ReportManager.flushReports();
        ChainTestListener.flush();
    }
}
