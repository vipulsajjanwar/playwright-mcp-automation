/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Manages test artifact lifecycle: attaching screenshots, videos, and traces to the active reporter context.
 * Created: 2026-10-01
 */
package framework.reporting.artifacts;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import framework.browser.BrowserManager;
import framework.config.ReportingConfig;
import framework.context.TestContext;
import framework.utils.TraceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Manages Playwright artifacts including Traces, Videos, Screenshots,
 * HTML source, and Page URL tracking for the active {@link TestContext}.
 */
public final class ArtifactManager {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactManager.class);
    private static final int MAX_HTML_CHARS = 100_000;

    private ArtifactManager() {}

    /**
     * Captures browser artifacts (URL, Title, HTML source, and failure screenshot)
     * into the current TestContext.
     */
    public static void captureFailureArtifacts(Page page, TestContext context) {
        if (page == null || context == null) {
            return;
        }

        try {
            if (!page.isClosed()) {
                String url = page.url();
                String title = page.title();
                String content = page.content();
                if (content != null && content.length() > MAX_HTML_CHARS) {
                    content = content.substring(0, MAX_HTML_CHARS) + "\n<!-- TRUNCATED -->";
                }

                // Capture failure screenshot
                String screenshotPath = ScreenshotManager.captureFailureScreenshot(page, context);

                // Video path if available
                String videoPath = null;
                try {
                    if (page.video() != null) {
                        Path vPath = page.video().path();
                        if (vPath != null) {
                            videoPath = vPath.toAbsolutePath().toString();
                        }
                    }
                } catch (Exception ignored) {
                    // Video might still be recording or not configured
                }

                context.setPlaywrightArtifacts(url, title, content, videoPath, null);
            }
        } catch (Exception e) {
            logger.warn("Failed to capture Playwright failure artifacts for test {}: {}",
                    context.getTestName(), e.getMessage());
        }
    }

    /**
     * Finalizes and records Playwright trace if tracing was active.
     */
    public static void finalizeTracing(BrowserContext browserContext, TestContext context, boolean isPassed) {
        if (browserContext == null || context == null) {
            return;
        }

        try {
            String tracePath = TraceUtils.stopTracing(browserContext, context.getTestName(), isPassed);
            if (tracePath != null) {
                context.setPlaywrightArtifacts(context.getPageUrl(), context.getPageTitle(),
                        context.getPageSourceHtml(), context.getVideoPath(), tracePath);
            }
        } catch (Exception e) {
            logger.warn("Failed to finalize tracing: {}", e.getMessage());
        }
    }
}
