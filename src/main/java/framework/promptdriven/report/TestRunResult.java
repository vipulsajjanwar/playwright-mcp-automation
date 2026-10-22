package framework.promptdriven.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Result data model for a complete prompt test file execution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestRunResult {

    private String runId;
    private String promptName;
    private String testName;
    private String status; // PASSED, FAILED
    private long startTime;
    private long endTime;
    private long durationMs;
    private int totalSteps;
    private int passedSteps;
    private int failedSteps;
    private int healedSteps;
    private String errorMessage;
    private List<StepResult> steps = new ArrayList<>();
    private String planJson;
    private String outputDir;
    private String tracePath;
    private String reportHtmlPath;
    private String reportJsonPath;

    public TestRunResult() {}

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }

    public String getPromptName() { return promptName; }
    public void setPromptName(String promptName) { this.promptName = promptName; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public int getPassedSteps() { return passedSteps; }
    public void setPassedSteps(int passedSteps) { this.passedSteps = passedSteps; }

    public int getFailedSteps() { return failedSteps; }
    public void setFailedSteps(int failedSteps) { this.failedSteps = failedSteps; }

    public int getHealedSteps() { return healedSteps; }
    public void setHealedSteps(int healedSteps) { this.healedSteps = healedSteps; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<StepResult> getSteps() { return steps != null ? steps : new ArrayList<>(); }
    public void setSteps(List<StepResult> steps) { this.steps = steps; }

    public String getPlanJson() { return planJson; }
    public void setPlanJson(String planJson) { this.planJson = planJson; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public String getTracePath() { return tracePath; }
    public void setTracePath(String tracePath) { this.tracePath = tracePath; }

    public String getReportHtmlPath() { return reportHtmlPath; }
    public void setReportHtmlPath(String reportHtmlPath) { this.reportHtmlPath = reportHtmlPath; }

    public String getReportJsonPath() { return reportJsonPath; }
    public void setReportJsonPath(String reportJsonPath) { this.reportJsonPath = reportJsonPath; }

    public boolean isPassed() {
        return "PASSED".equalsIgnoreCase(status);
    }
}
