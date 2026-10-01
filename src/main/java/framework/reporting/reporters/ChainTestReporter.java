package framework.reporting.reporters;

import framework.annotations.TestMetadata;
import framework.config.ReportingConfig;
import framework.context.TestContext;
import framework.listeners.ChainTestListener;
import framework.reporting.TestReporter;
import framework.utils.ChainTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;

import java.util.ArrayList;
import java.util.List;

/**
 * ChainTest implementation of {@link TestReporter}.
 * Bridges the test lifecycle and metadata directly to ChainTest.
 */
public class ChainTestReporter implements TestReporter {

    private static final Logger logger = LoggerFactory.getLogger(ChainTestReporter.class);

    @Override
    public String getReporterName() {
        return "ChainTest";
    }

    @Override
    public boolean isEnabled() {
        return ReportingConfig.isChainTestEnabled() && ChainTestUtil.isEnabled();
    }

    @Override
    public void onSuiteStart(ISuite suite) {
        // ChainTest initialization handled in ChainTestListener
    }

    @Override
    public void onTestStart(TestContext context) {
        if (!isEnabled() || context == null) return;

        try {
            TestMetadata metadata = context.getMetadata();
            List<String> tags = new ArrayList<>();
            if (metadata != null) {
                tags.addAll(metadata.getTags());
                if (!metadata.getCategory().isBlank()) {
                    tags.add(metadata.getCategory());
                }
            }

            ChainTestListener.startTest(
                    context.getTestName(),
                    context.getClassName(),
                    metadata != null ? metadata.getDescription() : "",
                    tags.toArray(new String[0])
            );

            if (context.getBrowser() != null) {
                ChainTestListener.addBrowser(context.getBrowser());
            }
            if (context.getEnvironment() != null) {
                ChainTestListener.addEnvironment(context.getEnvironment());
            }
            ChainTestListener.addTestClass(context.getClassName());
        } catch (Exception e) {
            logger.warn("Failed to report test start to ChainTest: {}", e.getMessage());
        }
    }

    @Override
    public void onStep(TestContext context, String message) {
        if (!isEnabled()) return;
        ChainTestListener.info(message);
    }

    @Override
    public void onTestPass(TestContext context) {
        if (!isEnabled()) return;
        try {
            ChainTestListener.passTest();
        } catch (Exception e) {
            logger.warn("Failed to report test pass to ChainTest: {}", e.getMessage());
        }
    }

    @Override
    public void onTestFail(TestContext context, Throwable throwable) {
        if (!isEnabled()) return;
        try {
            ChainTestListener.failTest(throwable, null);

            // Attach failure screenshot if captured
            if (context != null && !context.getScreenshotPaths().isEmpty()) {
                String path = context.getScreenshotPaths().get(context.getScreenshotPaths().size() - 1);
                ChainTestUtil.attachScreenshot(path, "Failure Screenshot");
            }
        } catch (Exception e) {
            logger.warn("Failed to report test failure to ChainTest: {}", e.getMessage());
        }
    }

    @Override
    public void onTestSkip(TestContext context, String reason) {
        if (!isEnabled()) return;
        try {
            ChainTestListener.skipTest(reason);
        } catch (Exception e) {
            logger.warn("Failed to report test skip to ChainTest: {}", e.getMessage());
        }
    }

    @Override
    public void onTestFinish(TestContext context) {
        // Individual test completion
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (!isEnabled()) return;
        try {
            ChainTestListener.flush();
        } catch (Exception e) {
            logger.warn("Failed to flush ChainTest: {}", e.getMessage());
        }
    }
}
