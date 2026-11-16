/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Integration test demonstrating AI-powered failure analysis on injected test failures to validate LLM suggestions.
 * Created: 2026-08-15
 */
package tests.integration;

import framework.ai.AiFailureAnalyzer;
import framework.reporting.TestExecutionLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AiFailureAnalysisExampleTest {

    @Test(groups = {"integration", "ai"}, description = "Demonstrate AI failure analysis diagnostics")
    public void testAiFailureAnalysis() {
        TestExecutionLogger.logStep("Simulating failure and invoking AI Failure Analyzer");
        String resultJson = AiFailureAnalyzer.analyzeFailure(
                "SampleTimeoutTest",
                "Timeout 30000ms exceeded while waiting for element locator('#submit-btn')",
                "com.microsoft.playwright.TimeoutError",
                "https://demo.example.com/checkout",
                "#submit-btn"
        );
        Assert.assertTrue(resultJson.contains("ELEMENT_TIMEOUT"), "Analysis should categorize as ELEMENT_TIMEOUT");
    }
}
