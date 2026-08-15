package framework.execution;

import framework.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(RetryAnalyzer.class);
    private int count = 0;

    @Override
    public boolean retry(ITestResult result) {
        boolean retryEnabled = ConfigManager.getBoolean("retry.enabled", true);
        int maxRetryCount = ConfigManager.getInt("retry.count", 1);

        if (retryEnabled && count < maxRetryCount) {
            count++;
            logger.warn("Retrying test: {} for retry count: {} / {}", result.getName(), count, maxRetryCount);
            return true;
        }
        return false;
    }
}
