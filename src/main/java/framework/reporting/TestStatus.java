/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Enumeration of test execution statuses (PASSED, FAILED, SKIPPED, BROKEN) used across all reporter implementations.
 * Created: 2026-09-29
 */
package framework.reporting;

public enum TestStatus {
    NOT_RUN,
    STARTED,
    RUNNING,
    PASS,
    FAIL,
    SKIP
}
