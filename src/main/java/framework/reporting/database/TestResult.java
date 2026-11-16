/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Database entity model representing a single test execution result row stored in the reporting schema.
 * Created: 2026-10-01
 */
package framework.reporting.database;

import framework.context.TestContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * Domain entity representing a test execution record for database persistence.
 */
public class TestResult {

    private String executionId;
    private String testId;
    private String testName;
    private String module;
    private String feature;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private long duration;
    private String environment;
    private String browser;
    private int retryCount;
    private String errorMessage;
    private String stackTrace;
    private String buildNumber;
    private String branch;
    private String commit;
    private Instant createdAt;

    public TestResult() {}

    public static TestResult fromContext(TestContext context) {
        TestResult entity = new TestResult();
        entity.executionId = context.getExecutionId();
        entity.testId = context.getMetadata() != null && !context.getMetadata().getTestId().isBlank()
                ? context.getMetadata().getTestId()
                : context.getMethodName();
        entity.testName = context.getTestName();
        entity.module = context.getMetadata() != null ? context.getMetadata().getModule() : "General";
        entity.feature = context.getMetadata() != null ? context.getMetadata().getFeature() : "General";
        entity.status = context.getStatus() != null ? context.getStatus().name() : "UNKNOWN";
        entity.startTime = context.getStartTime();
        entity.endTime = context.getEndTime();
        entity.duration = context.getDurationMs();
        entity.environment = context.getEnvironment();
        entity.browser = context.getBrowser();
        entity.retryCount = context.getRetryCount();

        if (context.getThrowable() != null) {
            entity.errorMessage = context.getThrowable().getMessage();
            StringWriter sw = new StringWriter();
            context.getThrowable().printStackTrace(new PrintWriter(sw));
            entity.stackTrace = sw.toString();
        } else if (context.getFailureReason() != null) {
            entity.errorMessage = context.getFailureReason();
            entity.stackTrace = "";
        }

        entity.buildNumber = context.getJenkinsBuildNumber();
        entity.branch = context.getGitBranch();
        entity.commit = context.getGitCommit();
        entity.createdAt = Instant.now();
        return entity;
    }

    // Getters and Setters
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = testId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    public String getBuildNumber() { return buildNumber; }
    public void setBuildNumber(String buildNumber) { this.buildNumber = buildNumber; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getCommit() { return commit; }
    public void setCommit(String commit) { this.commit = commit; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
