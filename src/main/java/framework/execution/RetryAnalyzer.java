package framework.execution;

import framework.config.ConfigManager;
import framework.context.TestContext;
import framework.context.TestContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe TestNG {@link IRetryAnalyzer} integrating with {@link TestContext}.
 * Tracks retry counts and updates the active context so all reporters know the attempt status.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(RetryAnalyzer.class);
    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public boolean retry(ITestResult result) {
        boolean retryEnabled = ConfigManager.getBoolean("retry.enabled", true);
        int maxRetryCount = ConfigManager.getInt("retry.count", 1);

        if (retryEnabled && count.get() < maxRetryCount) {
            int currentRetry = count.incrementAndGet();
            logger.warn("Retrying test: {} [Attempt: {} / Max: {}]",
                    result.getName(), currentRetry + 1, maxRetryCount + 1);

            TestContext context = TestContextManager.getContext();
            if (context != null) {
                context.setRetryInfo(currentRetry, currentRetry + 1, true);
            }
            return true;
        }
        return false;
    }

    public int getRetryCount() {
        return count.get();
    }
}
