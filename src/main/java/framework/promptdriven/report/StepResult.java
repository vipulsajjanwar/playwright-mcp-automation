/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Value object capturing the outcome (pass/fail/skip), duration, and error details of a single executed step.
 * Created: 2026-10-22
 */
package framework.promptdriven.report;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Result data model for a single executed plan step.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepResult {

    private int stepId;
    private String action;
    private String source;
    private String status; // PASS, FAIL, HEALED, SKIPPED
    private long durationMs;
    private String expected;
    private String actual;
    private String target;
    private String screenshotPath;
    private String error;
    private String oldTarget;
    private String newTarget;
    private boolean critical;
    private String apiDetails;

    public StepResult() {}

    public StepResult(int stepId, String action, String source, String status) {
        this.stepId = stepId;
        this.action = action;
        this.source = source;
        this.status = status;
    }

    public int getStepId() { return stepId; }
    public void setStepId(int stepId) { this.stepId = stepId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getScreenshotPath() { return screenshotPath; }
    public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getOldTarget() { return oldTarget; }
    public void setOldTarget(String oldTarget) { this.oldTarget = oldTarget; }

    public String getNewTarget() { return newTarget; }
    public void setNewTarget(String newTarget) { this.newTarget = newTarget; }

    public boolean isCritical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }

    public String getApiDetails() { return apiDetails; }
    public void setApiDetails(String apiDetails) { this.apiDetails = apiDetails; }

    public boolean isPassed() {
        return "PASS".equalsIgnoreCase(status) || "HEALED".equalsIgnoreCase(status);
    }
}
