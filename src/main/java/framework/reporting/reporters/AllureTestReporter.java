package framework.reporting.reporters;

import framework.annotations.TestMetadata;
import framework.config.ConfigManager;
import framework.config.ReportingConfig;
import framework.context.TestContext;
import framework.reporting.TestReporter;
import framework.utils.AllureUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.SeverityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Allure implementation of {@link TestReporter}.
 * Dynamically binds {@link framework.annotations.CustomTest} metadata into Allure Epics,
 * Features, Stories, Severities, Links, and Tags without requiring boilerplate Allure annotations.
 */
public class AllureTestReporter implements TestReporter {

    private static final Logger logger = LoggerFactory.getLogger(AllureTestReporter.class);

    @Override
    public String getReporterName() {
        return "Allure";
    }

    @Override
    public boolean isEnabled() {
        return ReportingConfig.isAllureEnabled();
    }

    @Override
    public void onSuiteStart(ISuite suite) {
        if (!isEnabled()) return;
        try {
            AllureUtils.writeEnvironment(Map.of(
                    "Base URL", ConfigManager.get("baseUrl", "http://localhost"),
                    "Browser", ConfigManager.get("browser", "chromium"),
                    "Environment", ConfigManager.get("env", "qa"),
                    "Java", System.getProperty("java.version"),
                    "OS", System.getProperty("os.name")
            ));
            AllureUtils.writeDefaultCategories();
        } catch (Exception e) {
            logger.warn("Failed to populate Allure environment metadata: {}", e.getMessage());
        }
    }

    @Override
    public void onTestStart(TestContext context) {
        if (!isEnabled() || context == null) return;

        try {
            TestMetadata metadata = context.getMetadata();

            // Dynamic test name & description
            AllureUtils.testName(context.getTestName());
            if (metadata != null && !metadata.getDescription().isBlank()) {
                AllureUtils.description(metadata.getDescription());
            }

            if (metadata != null) {
                // Epic & Feature
                if (!metadata.getModule().isBlank()) {
                    AllureUtils.epic(metadata.getModule());
                } else if (!metadata.getService().isBlank()) {
                    AllureUtils.epic(metadata.getService());
                }

                if (!metadata.getFeature().isBlank()) {
                    AllureUtils.feature(metadata.getFeature());
                }

                // Story
                if (!metadata.getStoryId().isBlank()) {
                    AllureUtils.story(metadata.getStoryId());
                } else if (!metadata.getSubFeature().isBlank()) {
                    AllureUtils.story(metadata.getSubFeature());
                }

                // Severity
                AllureUtils.severity(mapSeverity(metadata.getSeverity()));

                // Owner / Author
                if (!metadata.getEffectiveAuthor().isBlank()) {
                    AllureUtils.owner(metadata.getEffectiveAuthor());
                }

                // Links
                if (!metadata.getJiraId().isBlank()) {
                    AllureUtils.issue(metadata.getJiraId(), metadata.getJiraId());
                }
                if (!metadata.getRequirementId().isBlank()) {
                    AllureUtils.tms(metadata.getRequirementId(), metadata.getRequirementId());
                }

                // Tags
                if (!metadata.getCategory().isBlank()) {
                    AllureUtils.tag(metadata.getCategory());
                }
                if (!metadata.getTestType().isBlank()) {
                    AllureUtils.tag(metadata.getTestType());
                }
                if (!metadata.getPriority().isBlank()) {
                    AllureUtils.tag("Priority:" + metadata.getPriority());
                }
                for (String t : metadata.getTags()) {
                    if (!t.isBlank()) {
                        AllureUtils.tag(t);
                    }
                }
            }

            // Environment / Browser parameters
            if (context.getBrowser() != null) {
                AllureUtils.parameter("Browser", context.getBrowser());
            }
            if (context.getEnvironment() != null) {
                AllureUtils.parameter("Environment", context.getEnvironment());
            }
        } catch (Exception e) {
            logger.warn("Failed to apply Allure metadata: {}", e.getMessage());
        }
    }

    @Override
    public void onStep(TestContext context, String message) {
        if (!isEnabled()) return;
        AllureUtils.logStep(message);
    }

    @Override
    public void onTestPass(TestContext context) {
        if (!isEnabled() || context == null) return;
        AllureUtils.step("Test Completed Successfully", () -> {
            AllureUtils.attachText("Duration", context.getDurationMs() + " ms");
        });
    }

    @Override
    public void onTestFail(TestContext context, Throwable throwable) {
        if (!isEnabled() || context == null) return;

        try {
            // Attach exception stack trace
            if (throwable != null) {
                AllureUtils.attachStackTrace("Stack Trace", throwable);
            } else if (context.getFailureReason() != null) {
                AllureUtils.attachText("Failure Reason", context.getFailureReason());
            }

            // Attach screenshot
            byte[] screenshotBytes = context.getLatestScreenshotBytes();
            if (screenshotBytes != null && screenshotBytes.length > 0) {
                AllureUtils.attachScreenshot("Screenshot on Failure", screenshotBytes);
            } else if (!context.getScreenshotPaths().isEmpty()) {
                String path = context.getScreenshotPaths().get(context.getScreenshotPaths().size() - 1);
                try {
                    byte[] bytes = Files.readAllBytes(Paths.get(path));
                    AllureUtils.attachScreenshot("Screenshot on Failure", bytes);
                } catch (Exception ignored) {}
            }

            // Attach Page URL & HTML
            if (context.getPageUrl() != null && !context.getPageUrl().isBlank()) {
                AllureUtils.attachText("Page URL", context.getPageUrl());
            }
            if (context.getPageSourceHtml() != null && !context.getPageSourceHtml().isBlank()) {
                AllureUtils.attachHtml("Page HTML Source", "<pre>" + AllureUtils.escape(context.getPageSourceHtml()) + "</pre>");
            }

            // Attach Trace & Video references
            if (context.getTracePath() != null && !context.getTracePath().isBlank()) {
                AllureUtils.attachText("Playwright Trace Path", context.getTracePath());
            }
            if (context.getVideoPath() != null && !context.getVideoPath().isBlank()) {
                AllureUtils.attachText("Playwright Video Path", context.getVideoPath());
            }
        } catch (Exception e) {
            logger.warn("Error capturing Allure failure attachments: {}", e.getMessage());
        }
    }

    @Override
    public void onTestSkip(TestContext context, String reason) {
        if (!isEnabled()) return;
        AllureUtils.attachText("Skip Reason", reason != null ? reason : "Skipped");
    }

    @Override
    public void onTestFinish(TestContext context) {
        // Test complete
    }

    private SeverityLevel mapSeverity(String severity) {
        if (severity == null) return SeverityLevel.NORMAL;
        return switch (severity.trim().toUpperCase()) {
            case "BLOCKER" -> SeverityLevel.BLOCKER;
            case "CRITICAL" -> SeverityLevel.CRITICAL;
            case "MINOR" -> SeverityLevel.MINOR;
            case "TRIVIAL" -> SeverityLevel.TRIVIAL;
            default -> SeverityLevel.NORMAL;
        };
    }
}
