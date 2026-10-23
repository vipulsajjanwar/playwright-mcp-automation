package framework.reporting.reporters;

import framework.annotations.TestMetadata;
import framework.config.ReportingConfig;
import framework.constants.FrameworkConstants;
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

        return ChainTestUtil.getReportName(FrameworkConstants.REPORT_NAME);
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
            String displayName = context.getTestName();
            if (context.getBrowser() != null && !context.getBrowser().isBlank()) {
                displayName = displayName + " [" + context.getBrowser() + "]";
            }

            ChainTestListener.startTest(
                    displayName,
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
        if (!isEnabled() || message == null) return;

        String msg = message.trim();

        if (msg.startsWith("[TABLE]")) {
            int newlineIdx = msg.indexOf('\n');
            String title = (newlineIdx > 0)
                    ? msg.substring("[TABLE] ".length(), newlineIdx).trim()
                    : "Table";
            String htmlContent = (newlineIdx > 0) ? msg.substring(newlineIdx + 1) : "";
            ChainTestListener.attachHtml(title, htmlContent);
        } else if (msg.startsWith("[PASS]")) {
            ChainTestListener.passStep(msg);
        } else if (msg.startsWith("[FAIL]") || msg.startsWith("[ERROR]")) {
            ChainTestListener.fail(msg);
        } else if (msg.startsWith("[WARN]")) {
            ChainTestListener.warn(msg);
        } else if (msg.startsWith("[SKIP]")) {
            ChainTestListener.skip(msg);
        } else {
            ChainTestListener.info(msg);
        }
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
