package framework.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import framework.annotations.TestInfoResolver;
import framework.config.ConfigManager;
import framework.constants.TestConstants;
import framework.reporting.ELKStackReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ELKTestListener implements ITestListener, ISuiteListener {

    private static final Logger log = LoggerFactory.getLogger(ELKTestListener.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_TRACE_CHARS = 4000;

    @Override
    public void onTestSuccess(ITestResult result) {
        send(result, "PASSED");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        send(result, "FAILED");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        send(result, "SKIPPED");
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        send(result, "FAILED_WITHIN_SUCCESS_PERCENTAGE");
    }

    @Override
    public void onFinish(ISuite suite) {
        // Suite listeners run after @AfterSuite, so Playwright may already be closed.
        try {
            ELKStackReport.close();
        } catch (Exception e) {
            log.warn("ELK context cleanup failed: {}", e.getMessage());
        }
    }

    private void send(ITestResult result, String status) {
        try {
            String json = MAPPER.writeValueAsString(buildDocument(result, status));
            ELKStackReport.logToELKStack(json);
        } catch (Exception e) {
            // Never let reporting fail or alter the test outcome.
            log.warn("Could not build/send ELK document for {}: {}",
                    result.getName(), e.getMessage());
        }
    }

    private Map<String, Object> buildDocument(ITestResult result, String status) {
        Map<String, Object> doc = new LinkedHashMap<>();

        // ISO-8601 UTC so Kibana maps it as a date without timezone guessing
        doc.put("@timestamp", Instant.now().toString());
        doc.put("suite", result.getTestContext().getSuite().getName());
        doc.put("test", result.getTestContext().getName());
        doc.put("class", result.getTestClass().getName());
        doc.put("method", result.getMethod().getMethodName());
        doc.put("description", result.getMethod().getDescription());
        doc.put("status", status);
        doc.put("startTime", Instant.ofEpochMilli(result.getStartMillis()).toString());
        doc.put("endTime", Instant.ofEpochMilli(result.getEndMillis()).toString());
        doc.put("durationMs", result.getEndMillis() - result.getStartMillis());
        doc.put("thread", Thread.currentThread().getName());
        doc.put("environment", ConfigManager.get("env", "unknown"));
        doc.put("browser", ConfigManager.get("browser", "unknown"));
        doc.put("groups", result.getMethod().getGroups());
        TestInfoResolver.Resolved info = TestInfoResolver.resolve(result);
        doc.put("author", info.author());
        doc.put("severity", info.severity());

        Throwable t = result.getThrowable();
        if (t != null) {
            doc.put("errorType", t.getClass().getName());
            doc.put("errorMessage", truncate(t.getMessage(), 1000));
            doc.put("stackTrace", truncate(stackTraceOf(t), MAX_TRACE_CHARS));
        }
        return doc;
    }

    private static String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...[truncated]";
    }
}

