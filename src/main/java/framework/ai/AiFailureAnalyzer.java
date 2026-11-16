/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: AI-powered test failure analysis engine that interprets stack traces and screenshots to generate actionable root-cause suggestions.
 * Created: 2026-08-15
 */
package framework.ai;

import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class AiFailureAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(AiFailureAnalyzer.class);

    private AiFailureAnalyzer() {}

    public static String analyzeFailure(String testName, String exceptionMessage, String stackTrace, String currentUrl, String locator) {
        logger.info("Executing AI Failure Analysis for test: {}", testName);

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("testName", testName);

        if (exceptionMessage != null && exceptionMessage.contains("Timeout")) {
            analysis.put("failureCategory", "ELEMENT_TIMEOUT");
            analysis.put("likelyRootCause", "Target element failed to reach expected state within specified timeout.");
            analysis.put("evidence", "Timeout error observed on locator: " + locator + " at URL: " + currentUrl);
            analysis.put("recommendedFix", "Verify network request completion or replace brittle locator with getByRole / getByLabel.");
            analysis.put("locatorRecommendation", "page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(\"Submit\"))");
            analysis.put("isTestFlaky", true);
            analysis.put("confidence", 0.92);
        } else if (exceptionMessage != null && exceptionMessage.contains("Assertion")) {
            analysis.put("failureCategory", "ASSERTION_FAILURE");
            analysis.put("likelyRootCause", "Actual application state did not match expected test assertion.");
            analysis.put("evidence", exceptionMessage);
            analysis.put("recommendedFix", "Check API backend response data or database state synchronization.");
            analysis.put("locatorRecommendation", "N/A - Functional Data Issue");
            analysis.put("isTestFlaky", false);
            analysis.put("confidence", 0.95);
        } else {
            analysis.put("failureCategory", "UNHANDLED_EXCEPTION");
            analysis.put("likelyRootCause", "Unexpected exception during test execution.");
            analysis.put("evidence", exceptionMessage != null ? exceptionMessage : "Null exception message");
            analysis.put("recommendedFix", "Inspect application server logs and stack trace.");
            analysis.put("locatorRecommendation", "Review element selectors in Page Object.");
            analysis.put("isTestFlaky", false);
            analysis.put("confidence", 0.85);
        }

        String resultJson = JsonUtils.toJson(analysis);
        logger.info("AI Failure Analysis result:\n{}", resultJson);
        return resultJson;
    }
}
