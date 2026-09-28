package framework.utils;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import framework.config.ConfigManager;
import framework.constants.FrameworkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TraceUtils {

    private static final Logger logger = LoggerFactory.getLogger(TraceUtils.class);

    private TraceUtils() {}

    public static void startTracing(BrowserContext context) {
        boolean traceEnabled = ConfigManager.getBoolean("trace.enabled", true);
        if (traceEnabled) {
            logger.info("Starting Playwright trace recording [Thread: {}]", Thread.currentThread().getName());
            context.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));
        }
    }

    public static String stopTracing(BrowserContext context, String testName, boolean isTestPassed) {
        boolean traceEnabled = ConfigManager.getBoolean("trace.enabled", true);
        boolean keepOnPass = ConfigManager.getBoolean("trace.keepOnPass", false);
        boolean onFailureOnly = ConfigManager.getBoolean("trace.onFailure", true);

        if (!traceEnabled) {
            return null;
        }

        try {
            boolean shouldSave = (!isTestPassed && onFailureOnly) || (isTestPassed && keepOnPass);
            if (shouldSave) {
                Path targetDir = Paths.get(FrameworkConstants.TRACE_DIR);
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(ConfigManager.get("timestamp.format")));
                String fileName = String.format("%s_%s_%d.zip", sanitizeName(testName), timestamp, Thread.currentThread().getId());
                Path tracePath = targetDir.resolve(fileName);

                context.tracing().stop(new Tracing.StopOptions().setPath(tracePath));
                logger.info("Saved trace file: {}", tracePath.toAbsolutePath());
                return tracePath.toAbsolutePath().toString();
            } else {
                logger.info("Discarding trace for passed test: {}", testName);
                context.tracing().stop();
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to process trace for test: {}", testName, e);
            return null;
        }
    }

    private static String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
