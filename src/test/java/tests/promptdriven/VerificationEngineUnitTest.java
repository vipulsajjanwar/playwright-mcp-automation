/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Unit tests for VerificationEngine asserting correct evaluation of text, attribute, and visibility conditions.
 * Created: 2026-10-22
 */
package tests.promptdriven;

import framework.promptdriven.exec.ExecutionContext;
import framework.promptdriven.exec.VerificationEngine;
import framework.promptdriven.plan.PlanStep;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class VerificationEngineUnitTest {

    @Test
    public void testVerifyStatusSuccessAndFailure() {
        ExecutionContext context = new ExecutionContext("run1", "testPrompt", Paths.get("target/test-out"), false, false);
        context.setLastApiResponse(new ExecutionContext.ApiResponseData(200, "{\"ok\":true}", Map.of(), 50));

        // Expected 200 -> PASS
        PlanStep passStep = new PlanStep(1, "VERIFY_STATUS", Map.of("expected", 200), "verify status 200", false);
        VerificationEngine.VerifyResult passResult = VerificationEngine.verify(passStep, context, null);
        Assert.assertTrue(passResult.passed());
        Assert.assertEquals(passResult.actual(), "200");

        // Expected 404 -> MUST FAIL!
        PlanStep failStep = new PlanStep(2, "VERIFY_STATUS", Map.of("expected", 404), "verify status 404", false);
        VerificationEngine.VerifyResult failResult = VerificationEngine.verify(failStep, context, null);
        Assert.assertFalse(failResult.passed(), "Wrong expectation must fail, never pass silently!");
        Assert.assertEquals(failResult.expected(), "404");
        Assert.assertEquals(failResult.actual(), "200");
    }

    @Test
    public void testVerifyJsonSuccessAndFailure() {
        ExecutionContext context = new ExecutionContext("run1", "testPrompt", Paths.get("target/test-out"), false, false);
        context.setLastApiResponse(new ExecutionContext.ApiResponseData(200, "{\"user\":{\"id\":42,\"role\":\"ADMIN\"}}", Map.of(), 50));

        // Matching path
        PlanStep passStep = new PlanStep(1, "VERIFY_JSON", Map.of("jsonPath", "user.role", "expected", "ADMIN"), "check role", false);
        VerificationEngine.VerifyResult passResult = VerificationEngine.verify(passStep, context, null);
        Assert.assertTrue(passResult.passed());

        // Mismatched value -> FAILS!
        PlanStep failStep = new PlanStep(2, "VERIFY_JSON", Map.of("jsonPath", "user.role", "expected", "GUEST"), "check guest", false);
        VerificationEngine.VerifyResult failResult = VerificationEngine.verify(failStep, context, null);
        Assert.assertFalse(failResult.passed());
    }

    @Test
    public void testVerifyEveryItemHas() {
        ExecutionContext context = new ExecutionContext("run1", "testPrompt", Paths.get("target/test-out"), false, false);
        context.setLastApiResponse(new ExecutionContext.ApiResponseData(200, "[{\"id\":1,\"email\":\"a@b.com\"},{\"id\":2,\"email\":\"c@d.com\"}]", Map.of(), 50));

        // All items have id, email -> PASS
        PlanStep passStep = new PlanStep(1, "VERIFY_EVERY_ITEM_HAS", Map.of("fields", List.of("id", "email")), "check items", false);
        VerificationEngine.VerifyResult passResult = VerificationEngine.verify(passStep, context, null);
        Assert.assertTrue(passResult.passed());

        // Missing field -> FAILS
        PlanStep failStep = new PlanStep(2, "VERIFY_EVERY_ITEM_HAS", Map.of("fields", List.of("id", "missingField")), "check items", false);
        VerificationEngine.VerifyResult failResult = VerificationEngine.verify(failStep, context, null);
        Assert.assertFalse(failResult.passed());
    }
}
