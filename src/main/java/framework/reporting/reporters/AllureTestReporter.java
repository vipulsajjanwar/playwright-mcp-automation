package framework.reporting.reporters;

import framework.annotations.TestMetadata;
import framework.config.ConfigManager;
import framework.config.ReportingConfig;
import framework.context.TestContext;
import framework.reporting.TestReporter;
import framework.utils.AllureUtils;
import framework.reporting.allure.AllureHistoryManager;
import framework.reporting.allure.AllureReportManager;
import framework.reporting.allure.ReportPathManager;
import io.qameta.allure.SeverityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Allure implementation of {@link TestReporter}.
 * Dynamically binds framework runtime metadata into Allure without requiring
 * per-test boilerplate annotations.
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
        if (!isEnabled()) {
            return;
        }

        try {
            String suiteName = suite != null ? suite.getName() : "Playwright Automation Suite";
            ReportPathManager.ExecutionPaths paths = ReportPathManager.initializeExecutionPaths(suiteName);
            AllureHistoryManager.preserveHistory(paths.suiteName(), paths.resultsDir(), paths.executionDir());

            AllureUtils.writeEnvironment(buildEnvironmentDetails());
            AllureUtils.writeDefaultCategories();
            writeExecutorDetails(suite);
        } catch (Exception e) {
            logger.warn("Failed to populate Allure suite metadata: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onTestStart(TestContext context) {
        if (!isEnabled() || context == null) {
            return;
        }

        try {
            TestMetadata metadata = context.getMetadata();

            // Dynamic test name
            String displayName = context.getTestName();
            if (context.getBrowser() != null && !context.getBrowser().isBlank()) {
                displayName = displayName + " [" + context.getBrowser() + "]";
            }
            AllureUtils.testName(displayName);

            // Description
            if (metadata != null && metadata.getDescription() != null && !metadata.getDescription().isBlank()) {
                AllureUtils.description(metadata.getDescription());
            }

            if (metadata != null) {
                // Epic / Feature / Story
                if (notBlank(metadata.getModule())) {
                    AllureUtils.epic(metadata.getModule());
                } else if (notBlank(metadata.getService())) {
                    AllureUtils.epic(metadata.getService());
                }

                if (notBlank(metadata.getFeature())) {
                    AllureUtils.feature(metadata.getFeature());
                }

                if (notBlank(metadata.getStoryId())) {
                    AllureUtils.story(metadata.getStoryId());
                } else if (notBlank(metadata.getSubFeature())) {
                    AllureUtils.story(metadata.getSubFeature());
                }

                // Severity
                AllureUtils.severity(mapSeverity(metadata.getSeverity()));

                // Owner / Author
                if (notBlank(metadata.getEffectiveAuthor())) {
                    AllureUtils.owner(metadata.getEffectiveAuthor());
                }

                // Links
                if (notBlank(metadata.getJiraId())) {
                    AllureUtils.issue(metadata.getJiraId(), metadata.getJiraId());
                }

                if (notBlank(metadata.getRequirementId())) {
                    AllureUtils.tms(metadata.getRequirementId(), metadata.getRequirementId());
                }

                // Tags
                if (notBlank(metadata.getCategory())) {
                    AllureUtils.tag(metadata.getCategory());
                }

                if (notBlank(metadata.getTestType())) {
                    AllureUtils.tag(metadata.getTestType());
                }

                if (notBlank(metadata.getPriority())) {
                    AllureUtils.tag("Priority:" + metadata.getPriority());
                }

                for (String tag : metadata.getTags()) {
                    if (notBlank(tag)) {
                        AllureUtils.tag(tag);
                    }
                }
            }

            // Runtime parameters
            if (notBlank(context.getBrowser())) {
                AllureUtils.parameter("Browser", context.getBrowser());
            }

            if (notBlank(context.getEnvironment())) {
                AllureUtils.parameter("Environment", context.getEnvironment());
            }

            if (notBlank(context.getSuiteName())) {
                AllureUtils.parameter("Suite", context.getSuiteName());
            }

            if (notBlank(context.getClassName())) {
                AllureUtils.parameter("Class", context.getClassName());
            }

            if (notBlank(context.getMethodName())) {
                AllureUtils.parameter("Method", context.getMethodName());
            }

            if (notBlank(context.getExecutionId())) {
                AllureUtils.parameter("Execution ID", context.getExecutionId());
            }
        } catch (Exception e) {
            logger.warn("Failed to apply Allure metadata: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onStep(TestContext context, String message) {
        if (!isEnabled() || message == null || message.isBlank()) {
            return;
        }

        AllureUtils.logStep(message.trim());
    }

    @Override
    public void onTestPass(TestContext context) {
        if (!isEnabled() || context == null) {
            return;
        }

        AllureUtils.step("Test Completed Successfully", () -> {
            AllureUtils.attachText("Duration", context.getDurationMs() + " ms");
        });
    }

    @Override
    public void onTestFail(TestContext context, Throwable throwable) {
        if (!isEnabled() || context == null) {
            return;
        }

        try {
            if (throwable != null) {
                AllureUtils.attachStackTrace("Stack Trace", throwable);
            } else {
                attachText("Failure Reason", context.getFailureReason());
            }

            resolveScreenshot(context)
                    .ifPresent(bytes -> AllureUtils.attachScreenshot("Screenshot on Failure", bytes));

            attachText("Page URL", context.getPageUrl());
            attachText("Playwright Trace Path", context.getTracePath());
            attachText("Playwright Video Path", context.getVideoPath());

            String html = context.getPageSourceHtml();
            if (notBlank(html)) {
                AllureUtils.attachHtml("Page HTML Source", "<pre>" + AllureUtils.escape(html) + "</pre>");
            }
        } catch (Exception e) {
            logger.warn("Error capturing Allure failure attachments: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onTestSkip(TestContext context, String reason) {
        if (!isEnabled()) {
            return;
        }

        AllureUtils.attachText("Skip Reason", notBlank(reason) ? reason : "Skipped");
    }

    @Override
    public void onTestFinish(TestContext context) {
        // no-op
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        if (!isEnabled()) {
            return;
        }

        try {
            String suiteName = suite != null ? suite.getName() : "Playwright Automation Suite";
            AllureReportManager.onSuiteFinish(suiteName);
        } catch (Exception e) {
            logger.warn("Failed to complete Allure suite finish actions: {}", e.getMessage(), e);
        }
    }

    private static void attachText(String name, String value) {
        if (notBlank(value)) {
            AllureUtils.attachText(name, value);
        }
    }

    private Optional<byte[]> resolveScreenshot(TestContext context) {
        byte[] bytes = context.getLatestScreenshotBytes();
        if (bytes != null && bytes.length > 0) {
            return Optional.of(bytes);
        }

        List<String> paths = context.getScreenshotPaths();
        if (paths == null || paths.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Files.readAllBytes(Paths.get(paths.get(paths.size() - 1))));
        } catch (IOException e) {
            logger.debug("Could not read screenshot file: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, String> buildEnvironmentDetails() {
        Map<String, String> env = new LinkedHashMap<>();

        env.put("Base URL", defaultIfBlank(ConfigManager.get("baseUrl"), "http://localhost"));
        env.put("Browser", defaultIfBlank(ConfigManager.get("browser"), "chromium"));
        env.put("Environment", defaultIfBlank(ConfigManager.get("env"), "qa"));
        env.put("Java", defaultIfBlank(System.getProperty("java.version"), "unknown"));
        env.put("OS", defaultIfBlank(System.getProperty("os.name"), "unknown"));
        env.put("OS Version", defaultIfBlank(System.getProperty("os.version"), "unknown"));

        return env;
    }

    private void writeExecutorDetails(ISuite suite) {
        String buildUrl = firstNonBlank(
                System.getenv("BUILD_URL"),
                System.getProperty("BUILD_URL"),
                ""
        );

        String jobName = firstNonBlank(
                System.getenv("JOB_NAME"),
                System.getenv("BUILD_TAG"),
                suite != null ? suite.getName() : "Local Run"
        );

        String executorName;
        String executorType;
        String executorUrl;

        if (notBlank(System.getenv("JENKINS_URL")) || notBlank(System.getenv("BUILD_URL"))) {
            executorName = "Jenkins";
            executorType = "jenkins";
            executorUrl = firstNonBlank(System.getenv("JENKINS_URL"), "");
        } else if (notBlank(System.getenv("GITHUB_ACTIONS"))) {
            executorName = "GitHub Actions";
            executorType = "github";
            executorUrl = "";
        } else {
            executorName = "Local Execution";
            executorType = "local";
            executorUrl = "";
        }

        AllureUtils.writeExecutor(
                executorName,
                executorType,
                executorUrl,
                jobName,
                buildUrl
        );
    }

    private SeverityLevel mapSeverity(String severity) {
        if (severity == null) {
            return SeverityLevel.NORMAL;
        }

        return switch (severity.trim().toUpperCase()) {
            case "BLOCKER" -> SeverityLevel.BLOCKER;
            case "CRITICAL" -> SeverityLevel.CRITICAL;
            case "MINOR" -> SeverityLevel.MINOR;
            case "TRIVIAL" -> SeverityLevel.TRIVIAL;
            default -> SeverityLevel.NORMAL;
        };
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return notBlank(value) ? value : fallback;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (notBlank(value)) {
                return value;
            }
        }
        return "";
    }
}
