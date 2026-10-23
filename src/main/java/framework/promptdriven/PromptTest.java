package framework.promptdriven;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import framework.annotations.CustomTest;
import framework.base.BaseTest;
import framework.browser.BrowserManager;
import framework.promptdriven.report.TestRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TestNG test instance representing a single prompt file.
 * Implements ITest for human-readable test names in reports and logs.
 */
@CustomTest(category = "AI-Prompt", testType = "UI", module = "PromptDriven")
public class PromptTest extends BaseTest implements ITest {

    private static final Logger logger = LoggerFactory.getLogger(PromptTest.class);
    private static final ConcurrentHashMap<String, ReentrantLock> RESOURCE_LOCKS = new ConcurrentHashMap<>();

    private final PromptFile promptFile;

    public PromptTest(PromptFile promptFile) {
        this.promptFile = promptFile;
    }

    @Override
    public String getTestName() {
        return promptFile != null ? promptFile.getName() : "UnknownPrompt";
    }

    public PromptFile getPromptFile() {
        return promptFile;
    }

    @Override
    protected boolean shouldNavigateBaseUrl() {
        return false;
    }

    @Override
    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser"})
    public void setUp(Method method, @Optional("") String browser) {
        if ("api".equalsIgnoreCase(promptFile.getType())) {
            logger.info("Skipping browser initialization for pure API prompt test: {}", promptFile.getName());
            return;
        }
        super.setUp(method, browser);
    }

    @Test
    public void runPrompt() {
        String lockName = promptFile.getLock();
        ReentrantLock lock = null;
        if (lockName != null && !lockName.isBlank()) {
            lock = RESOURCE_LOCKS.computeIfAbsent(lockName.trim(), k -> new ReentrantLock(true));
            logger.info("Acquiring concurrency lock '{}' for prompt '{}'", lockName, promptFile.getName());
            lock.lock();
        }

        try {
            Page page = null;
            BrowserContext context = null;
            if (!"api".equalsIgnoreCase(promptFile.getType())) {
                try {
                    page = page();
                    context = BrowserManager.getContext();
                } catch (Exception e) {
                    logger.debug("Browser context not available for {}: {}", promptFile.getName(), e.getMessage());
                }
            }

            TestRunResult result = PromptTestRunner.run(promptFile, page, context);
            logger.info("Prompt test completed: {} -> Status: {}", promptFile.getName(), result.getStatus());
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.info("Released concurrency lock '{}' for prompt '{}'", lockName, promptFile.getName());
            }
        }
    }
}
