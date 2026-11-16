/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Per-test execution context holding the active Page, BrowserContext, test metadata, and reporter references.
 * Created: 2026-10-01
 */
package framework.context;

import framework.annotations.TestMetadata;
import framework.reporting.TestStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe central test context collecting metadata, execution environment,
 * TestNG information, artifacts, metrics, and state for a single test execution.
 */
public class TestContext {

    private final String executionId;
    private final long threadId;
    private final String threadName;

    // TestNG and Method Metadata
    private final String suiteName;
    private final String testName;
    private final String className;
    private final String methodName;
    private final String[] groups;
    private final TestMetadata metadata;

    // Environment and CI/CD Info
    private final String browser;
    private final String browserVersion;
    private final String os;
    private final String environment;
    private final String jenkinsJobName;
    private final String jenkinsBuildNumber;
    private final String jenkinsBuildUrl;
    private final String gitBranch;
    private final String gitCommit;

    // Execution Timing & Status
    private Instant startTime;
    private Instant endTime;
    private long durationMs;
    private volatile TestStatus status = TestStatus.NOT_RUN;
    private volatile Throwable throwable;
    private volatile String failureReason;

    // Retry Tracking
    private volatile int retryCount = 0;
    private volatile int attemptNumber = 1;
    private volatile boolean isRetry = false;

    // Playwright Artifacts & Details
    private volatile String pageUrl;
    private volatile String pageTitle;
    private volatile String pageSourceHtml;
    private volatile String videoPath;
    private volatile String tracePath;
    private final List<String> screenshotPaths = new CopyOnWriteArrayList<>();
    private volatile byte[] latestScreenshotBytes;

    // Logs and Errors
    private final List<String> consoleErrors = new CopyOnWriteArrayList<>();
    private final List<String> networkErrors = new CopyOnWriteArrayList<>();
    private final List<String> stepLogs = new CopyOnWriteArrayList<>();

    // Test Data and API Information
    private final Map<String, Object> testData = new ConcurrentHashMap<>();
    private final Map<String, Object> apiInfo = new ConcurrentHashMap<>();

    // Custom Extension Attributes
    private final Map<String, Object> customAttributes = new ConcurrentHashMap<>();

    public TestContext(Builder builder) {
        this.executionId = builder.executionId;
        this.threadId = builder.threadId;
        this.threadName = builder.threadName;
        this.suiteName = builder.suiteName;
        this.testName = builder.testName;
        this.className = builder.className;
        this.methodName = builder.methodName;
        this.groups = builder.groups != null ? builder.groups : new String[0];
        this.metadata = builder.metadata != null ? builder.metadata : TestMetadata.builder().build();

        this.browser = builder.browser;
        this.browserVersion = builder.browserVersion;
        this.os = builder.os;
        this.environment = builder.environment;
        this.jenkinsJobName = builder.jenkinsJobName;
        this.jenkinsBuildNumber = builder.jenkinsBuildNumber;
        this.jenkinsBuildUrl = builder.jenkinsBuildUrl;
        this.gitBranch = builder.gitBranch;
        this.gitCommit = builder.gitCommit;

        this.startTime = builder.startTime != null ? builder.startTime : Instant.now();
        this.status = TestStatus.STARTED;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getExecutionId() { return executionId; }
    public long getThreadId() { return threadId; }
    public String getThreadName() { return threadName; }
    public String getSuiteName() { return suiteName; }
    public String getTestName() { return testName; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public String[] getGroups() { return groups; }
    public TestMetadata getMetadata() { return metadata; }

    public String getBrowser() { return browser; }
    public String getBrowserVersion() { return browserVersion; }
    public String getOs() { return os; }
    public String getEnvironment() { return environment; }
    public String getJenkinsJobName() { return jenkinsJobName; }
    public String getJenkinsBuildNumber() { return jenkinsBuildNumber; }
    public String getJenkinsBuildUrl() { return jenkinsBuildUrl; }
    public String getGitBranch() { return gitBranch; }
    public String getGitCommit() { return gitCommit; }

    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public long getDurationMs() { return durationMs; }
    public TestStatus getStatus() { return status; }
    public Throwable getThrowable() { return throwable; }
    public String getFailureReason() { return failureReason; }

    public int getRetryCount() { return retryCount; }
    public int getAttemptNumber() { return attemptNumber; }
    public boolean isRetry() { return isRetry; }

    public String getPageUrl() { return pageUrl; }
    public String getPageTitle() { return pageTitle; }
    public String getPageSourceHtml() { return pageSourceHtml; }
    public String getVideoPath() { return videoPath; }
    public String getTracePath() { return tracePath; }
    public List<String> getScreenshotPaths() { return Collections.unmodifiableList(screenshotPaths); }
    public byte[] getLatestScreenshotBytes() { return latestScreenshotBytes; }

    public List<String> getConsoleErrors() { return Collections.unmodifiableList(consoleErrors); }
    public List<String> getNetworkErrors() { return Collections.unmodifiableList(networkErrors); }
    public List<String> getStepLogs() { return Collections.unmodifiableList(stepLogs); }
    public Map<String, Object> getTestData() { return testData; }
    public Map<String, Object> getApiInfo() { return apiInfo; }
    public Map<String, Object> getCustomAttributes() { return customAttributes; }

    // Mutators
    public void markPassed() {
        this.status = TestStatus.PASS;
        finishTiming();
    }

    public void markFailed(Throwable throwable) {
        this.status = TestStatus.FAIL;
        this.throwable = throwable;
        this.failureReason = throwable != null ? throwable.getMessage() : "Unknown Failure";
        finishTiming();
    }

    public void markSkipped(String reason) {
        this.status = TestStatus.SKIP;
        this.failureReason = reason;
        finishTiming();
    }

    private void finishTiming() {
        this.endTime = Instant.now();
        if (this.startTime != null) {
            this.durationMs = Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    public void setRetryInfo(int retryCount, int attemptNumber, boolean isRetry) {
        this.retryCount = retryCount;
        this.attemptNumber = attemptNumber;
        this.isRetry = isRetry;
    }

    public void addScreenshot(String path, byte[] bytes) {
        if (path != null && !path.isBlank()) {
            this.screenshotPaths.add(path);
        }
        if (bytes != null && bytes.length > 0) {
            this.latestScreenshotBytes = bytes;
        }
    }

    public void setPlaywrightArtifacts(String pageUrl, String pageTitle, String pageSourceHtml,
                                       String videoPath, String tracePath) {
        if (pageUrl != null) this.pageUrl = pageUrl;
        if (pageTitle != null) this.pageTitle = pageTitle;
        if (pageSourceHtml != null) this.pageSourceHtml = pageSourceHtml;
        if (videoPath != null) this.videoPath = videoPath;
        if (tracePath != null) this.tracePath = tracePath;
    }

    public void addStepLog(String message) {
        if (message != null) {
            this.stepLogs.add(message);
        }
    }

    public void addConsoleError(String error) {
        if (error != null) {
            this.consoleErrors.add(error);
        }
    }

    public void addNetworkError(String error) {
        if (error != null) {
            this.networkErrors.add(error);
        }
    }

    public void setAttribute(String key, Object value) {
        if (key != null && value != null) {
            this.customAttributes.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) this.customAttributes.get(key);
    }

    public static final class Builder {
        private String executionId;
        private long threadId;
        private String threadName;
        private String suiteName;
        private String testName;
        private String className;
        private String methodName;
        private String[] groups;
        private TestMetadata metadata;
        private String browser;
        private String browserVersion;
        private String os;
        private String environment;
        private String jenkinsJobName;
        private String jenkinsBuildNumber;
        private String jenkinsBuildUrl;
        private String gitBranch;
        private String gitCommit;
        private Instant startTime;

        public Builder executionId(String executionId) { this.executionId = executionId; return this; }
        public Builder threadId(long threadId) { this.threadId = threadId; return this; }
        public Builder threadName(String threadName) { this.threadName = threadName; return this; }
        public Builder suiteName(String suiteName) { this.suiteName = suiteName; return this; }
        public Builder testName(String testName) { this.testName = testName; return this; }
        public Builder className(String className) { this.className = className; return this; }
        public Builder methodName(String methodName) { this.methodName = methodName; return this; }
        public Builder groups(String[] groups) { this.groups = groups; return this; }
        public Builder metadata(TestMetadata metadata) { this.metadata = metadata; return this; }
        public Builder browser(String browser) { this.browser = browser; return this; }
        public Builder browserVersion(String browserVersion) { this.browserVersion = browserVersion; return this; }
        public Builder os(String os) { this.os = os; return this; }
        public Builder environment(String environment) { this.environment = environment; return this; }
        public Builder jenkinsJobName(String jenkinsJobName) { this.jenkinsJobName = jenkinsJobName; return this; }
        public Builder jenkinsBuildNumber(String jenkinsBuildNumber) { this.jenkinsBuildNumber = jenkinsBuildNumber; return this; }
        public Builder jenkinsBuildUrl(String jenkinsBuildUrl) { this.jenkinsBuildUrl = jenkinsBuildUrl; return this; }
        public Builder gitBranch(String gitBranch) { this.gitBranch = gitBranch; return this; }
        public Builder gitCommit(String gitCommit) { this.gitCommit = gitCommit; return this; }
        public Builder startTime(Instant startTime) { this.startTime = startTime; return this; }

        public TestContext build() {
            return new TestContext(this);
        }
    }
}
