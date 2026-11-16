/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Builder class constructing ELK-compatible JSON documents from test result data for Elasticsearch indexing.
 * Created: 2026-10-01
 */
package framework.reporting.elk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import framework.annotations.TestMetadata;
import framework.context.TestContext;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Builds standard Elasticsearch JSON payloads from {@link TestContext}.
 * Follows the prescribed schema for ELK index ingestion.
 */
public final class ELKTestResultBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_STACK_TRACE_CHARS = 4000;

    private ELKTestResultBuilder() {}

    public static String buildJsonPayload(TestContext context) {
        ObjectNode root = buildDocument(context);
        try {
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"error\":\"Failed to serialize ELK document: " + e.getMessage() + "\"}";
        }
    }

    public static ObjectNode buildDocument(TestContext context) {
        ObjectNode root = MAPPER.createObjectNode();
        TestMetadata meta = context.getMetadata();

        root.put("@timestamp", context.getEndTime() != null ? context.getEndTime().toString() : context.getStartTime().toString());
        root.put("testId", meta != null && !meta.getTestId().isBlank() ? meta.getTestId() : context.getMethodName());
        root.put("testName", context.getTestName());
        root.put("module", meta != null ? meta.getModule() : "General");
        root.put("feature", meta != null ? meta.getFeature() : "General");
        root.put("status", context.getStatus() != null ? context.getStatus().name() : "UNKNOWN");
        root.put("severity", meta != null ? meta.getSeverity() : "NORMAL");
        root.put("priority", meta != null ? meta.getPriority() : "MEDIUM");
        root.put("executionType", meta != null ? meta.getExecutionType() : "AUTOMATED");
        root.put("environment", context.getEnvironment() != null ? context.getEnvironment() : "unknown");
        root.put("browser", context.getBrowser() != null ? context.getBrowser() : "unknown");
        root.put("suite", context.getSuiteName() != null ? context.getSuiteName() : "");
        root.put("className", context.getClassName() != null ? context.getClassName() : "");
        root.put("methodName", context.getMethodName() != null ? context.getMethodName() : "");
        root.put("threadId", String.valueOf(context.getThreadId()));
        root.put("executionId", context.getExecutionId() != null ? context.getExecutionId() : "");
        root.put("startTime", context.getStartTime() != null ? context.getStartTime().toString() : "");
        root.put("endTime", context.getEndTime() != null ? context.getEndTime().toString() : "");
        root.put("duration", context.getDurationMs());
        root.put("retryCount", context.getRetryCount());

        // Error Object
        ObjectNode errorNode = root.putObject("error");
        Throwable t = context.getThrowable();
        if (t != null) {
            errorNode.put("message", truncate(t.getMessage(), 1000));
            errorNode.put("type", t.getClass().getName());
            errorNode.put("stackTrace", truncate(stackTraceOf(t), MAX_STACK_TRACE_CHARS));
        } else if (context.getFailureReason() != null) {
            errorNode.put("message", context.getFailureReason());
            errorNode.put("type", "Assertion/Execution Failure");
            errorNode.put("stackTrace", "");
        } else {
            errorNode.put("message", "");
            errorNode.put("type", "");
            errorNode.put("stackTrace", "");
        }

        // Jenkins / CI Object
        ObjectNode jenkinsNode = root.putObject("jenkins");
        jenkinsNode.put("jobName", context.getJenkinsJobName() != null ? context.getJenkinsJobName() : "");
        jenkinsNode.put("buildNumber", context.getJenkinsBuildNumber() != null ? context.getJenkinsBuildNumber() : "");
        jenkinsNode.put("buildUrl", context.getJenkinsBuildUrl() != null ? context.getJenkinsBuildUrl() : "");
        jenkinsNode.put("branch", context.getGitBranch() != null ? context.getGitBranch() : "");
        jenkinsNode.put("commit", context.getGitCommit() != null ? context.getGitCommit() : "");

        // Artifacts Object
        ObjectNode artifactsNode = root.putObject("artifacts");
        String screenshotPath = !context.getScreenshotPaths().isEmpty()
                ? context.getScreenshotPaths().get(context.getScreenshotPaths().size() - 1)
                : "";
        artifactsNode.put("screenshot", screenshotPath);
        artifactsNode.put("video", context.getVideoPath() != null ? context.getVideoPath() : "");
        artifactsNode.put("trace", context.getTracePath() != null ? context.getTracePath() : "");

        return root;
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...[truncated]";
    }
}
