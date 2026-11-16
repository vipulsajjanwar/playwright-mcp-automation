/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Context holder for ChainTest reporter integration, storing project key, run ID, and thread-local test node references.
 * Created: 2026-09-29
 */
package framework.reporting;

import framework.listeners.ChainTestListener;

import com.aventstack.chaintest.domain.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChainTestContext {
    private final String testName;

    private final long threadId;

    private TestStatus status =
            TestStatus.RUNNING;

    private int logCount;

    private final List<Path> attachments =
            new ArrayList<>();

    private final Map<String, String> metadata =
            new LinkedHashMap<>();

    private Test chainTestNode;

    private String className;


    public ChainTestContext(
            String testName,
            long threadId) {

        this.testName = testName;
        this.threadId = threadId;
    }

    public String getTestName() {
        return testName;
    }

    public long getThreadId() {
        return threadId;
    }

    public TestStatus getStatus() {
        return status;
    }

    public void setStatus(
            TestStatus status) {

        this.status = status;
    }

    public void incrementLogCount() {
        logCount++;
    }

    public int getLogCount() {
        return logCount;
    }

    public void addAttachment(Path path) {

        if (path != null) {
            attachments.add(path);
        }
    }

    public List<Path> getAttachments() {
        return attachments;
    }

    public void addMetadata(
            String key,
            String value) {

        metadata.put(
                key,
                value
        );
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Test getChainTestNode() {
        return chainTestNode;
    }

    public void setChainTestNode(Test chainTestNode) {
        this.chainTestNode = chainTestNode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
