package tests.promptdriven;

import framework.promptdriven.plan.ActionType;
import framework.promptdriven.plan.PlanStep;
import framework.promptdriven.plan.PlanValidator;
import framework.promptdriven.plan.TestPlan;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanValidatorUnitTest {

    @Test
    public void testValidPlanPasses() {
        List<PlanStep> steps = List.of(
                new PlanStep(1, "NAVIGATE", Map.of("url", "https://app.example.com"), "go to url", true),
                new PlanStep(2, "CLICK", Map.of("target", Map.of("role", "button", "name", "Login")), "click Login", true),
                new PlanStep(3, "VERIFY_STATUS", Map.of("expected", 200), "verify status 200", false)
        );
        TestPlan plan = new TestPlan(steps);

        PlanValidator.ValidationResult result = PlanValidator.validate(plan);
        Assert.assertTrue(result.isValid(), "Expected valid plan to pass validation");
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testRejectUnknownAction() {
        List<PlanStep> steps = List.of(
                new PlanStep(1, "HACK_THE_MAINFRAME", Map.of("target", "system"), "hack it", true)
        );
        TestPlan plan = new TestPlan(steps);

        PlanValidator.ValidationResult result = PlanValidator.validate(plan);
        Assert.assertFalse(result.isValid(), "Expected unknown action to be rejected");
        Assert.assertTrue(result.getErrorMessage().contains("not in the closed action vocabulary"));
    }

    @Test
    public void testRejectUnresolvedStep() {
        List<PlanStep> steps = List.of(
                new PlanStep(1, "UNRESOLVED", Map.of("reason", "Cannot understand prompt text: 'do magic'"), "do magic", true)
        );
        TestPlan plan = new TestPlan(steps);

        PlanValidator.ValidationResult result = PlanValidator.validate(plan);
        Assert.assertFalse(result.isValid(), "Expected UNRESOLVED action to fail validation");
        Assert.assertTrue(result.getErrorMessage().contains("UNRESOLVED"));
        Assert.assertTrue(result.getErrorMessage().contains("Cannot understand prompt text"));
    }

    @Test
    public void testRejectMissingRequiredArgs() {
        List<PlanStep> steps = List.of(
                new PlanStep(1, "NAVIGATE", Map.of(), "go to", true),
                new PlanStep(2, "FILL", Map.of("target", Map.of("role", "textbox")), "fill without value", true)
        );
        TestPlan plan = new TestPlan(steps);

        PlanValidator.ValidationResult result = PlanValidator.validate(plan);
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrorMessage().contains("requires 'url'"));
        Assert.assertTrue(result.getErrorMessage().contains("requires 'value'"));
    }

    @Test
    public void testExceedingMaxSteps() {
        System.setProperty("prompts.maxSteps", "5");
        try {
            List<PlanStep> steps = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                steps.add(new PlanStep(i, "NAVIGATE", Map.of("url", "https://app.example.com"), "step " + i, true));
            }
            TestPlan plan = new TestPlan(steps);

            PlanValidator.ValidationResult result = PlanValidator.validate(plan);
            Assert.assertFalse(result.isValid());
            Assert.assertTrue(result.getErrorMessage().contains("exceeds maximum allowed steps"));
        } finally {
            System.clearProperty("prompts.maxSteps");
        }
    }
}
