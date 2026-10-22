package framework.promptdriven.exec;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.Page;
import framework.ai.AiConfig;
import framework.ai.AiTestDataGenerator;
import framework.promptdriven.plan.ActionType;
import framework.promptdriven.plan.PlanStep;
import framework.promptdriven.plan.TestPlan;
import framework.promptdriven.report.StepResult;
import framework.reporting.FrameworkLogger;
import framework.utils.JsonUtils;
import framework.utils.RandomDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

/**
 * Sequential step execution engine.
 * Dispatches to UI / API / Data / Verification executors, captures evidence,
 * and enforces critical vs soft verification policies.
 */
public class StepExecutor {

    private static final Logger logger = LoggerFactory.getLogger(StepExecutor.class);

    private final ExecutionContext context;
    private final UiActionExecutor uiExecutor;
    private final ApiActionExecutor apiExecutor;

    public StepExecutor(ExecutionContext context) {
        this.context = context;
        this.uiExecutor = new UiActionExecutor(context);
        this.apiExecutor = new ApiActionExecutor(context);
    }

    public void executePlan(TestPlan plan) {
        if (context.isDryRun()) {
            logger.info("DRY RUN MODE: Skipping execution. Plan contains {} step(s).", plan.getSteps().size());
            for (PlanStep step : plan.getSteps()) {
                StepResult dryStep = new StepResult(step.getId(), step.getAction(), step.getSource(), "PASS");
                dryStep.setActual("Dry run simulated pass");
                context.addStepResult(dryStep);
            }
            return;
        }

        List<PlanStep> steps = plan.getSteps();
        for (PlanStep step : steps) {
            StepResult result = executeStep(step);
            context.addStepResult(result);

            // Screenshot policy
            String screenshotMode = AiConfig.getScreenshotsMode();
            if (context.getPage() != null) {
                if ("all".equalsIgnoreCase(screenshotMode) || (!result.isPassed() && !"none".equalsIgnoreCase(screenshotMode))) {
                    try {
                        String imgName = "step_" + step.getId() + "_" + (result.isPassed() ? "pass" : "fail") + ".png";
                        Path dest = context.getScreenshotsDir().resolve(imgName);
                        context.getPage().screenshot(new Page.ScreenshotOptions().setPath(dest));
                        result.setScreenshotPath(dest.toString());
                    } catch (Exception e) {
                        logger.warn("Could not capture screenshot for step {}: {}", step.getId(), e.getMessage());
                    }
                }
            }

            // Abort policy on failure
            if (!result.isPassed()) {
                context.markFailure();
                String failMsg = String.format("Step %d (%s) FAILED: %s [source: '%s']",
                        step.getId(), step.getAction(), result.getError(), step.getSource());
                FrameworkLogger.error(failMsg, null);

                if (step.isCritical() || context.isFailFast()) {
                    logger.error("Aborting test due to critical/fail-fast step failure: {}", failMsg);
                    throw new AssertionError(failMsg);
                }
            } else {
                FrameworkLogger.info(String.format("Step %d (%s) PASSED [source: '%s']",
                        step.getId(), step.getAction(), step.getSource()));
            }
        }

        // Post-execution evaluation for soft assertions
        if (context.hasFailures()) {
            List<String> failedStepMessages = new ArrayList<>();
            for (StepResult res : context.getStepResults()) {
                if (!res.isPassed()) {
                    failedStepMessages.add(String.format("Step %d (%s): expected '%s', actual '%s' [error: %s]",
                            res.getStepId(), res.getAction(), res.getExpected(), res.getActual(), res.getError()));
                }
            }
            throw new AssertionError("Prompt test completed with verification failures:\n- "
                    + String.join("\n- ", failedStepMessages));
        }
    }

    private StepResult executeStep(PlanStep step) {
        ActionType type = step.getActionType();
        long startTime = System.currentTimeMillis();
        VariableResolver resolver = context.getVariableResolver();

        if (type == null || type == ActionType.UNRESOLVED) {
            StepResult res = new StepResult(step.getId(), "UNRESOLVED", step.getSource(), "FAIL");
            String reason = step.getStringArg("reason");
            res.setError("UNRESOLVED STEP: " + (reason != null ? reason : "Ambiguous instruction in prompt"));
            res.setDurationMs(System.currentTimeMillis() - startTime);
            return res;
        }

        // 1. Data actions
        if (type == ActionType.GENERATE_DATA) {
            return executeGenerateData(step, resolver, startTime);
        } else if (type == ActionType.SET_VAR) {
            String name = resolver.resolve(step.getStringArg("name"));
            String val = resolver.resolve(step.getStringArg("value"));
            context.setVariable(name, val);
            StepResult res = new StepResult(step.getId(), "SET_VAR", step.getSource(), "PASS");
            res.setActual("Set variable " + name + " = " + resolver.mask(val));
            res.setDurationMs(System.currentTimeMillis() - startTime);
            return res;
        }

        // 2. Verifications (UI or API)
        if (type.isVerification()) {
            VerificationEngine.VerifyResult vr = VerificationEngine.verify(step, context, uiExecutor);
            StepResult res = new StepResult(step.getId(), step.getAction(), step.getSource(), vr.passed() ? "PASS" : "FAIL");
            res.setExpected(vr.expected());
            res.setActual(vr.actual());
            res.setError(vr.error());
            res.setCritical(step.isCritical());
            res.setDurationMs(System.currentTimeMillis() - startTime);
            return res;
        }

        // 3. UI Actions
        if (type.isUiAction()) {
            return uiExecutor.execute(step);
        }

        // 4. API Actions
        if (type.isApiAction()) {
            return apiExecutor.execute(step);
        }

        StepResult unsupported = new StepResult(step.getId(), step.getAction(), step.getSource(), "FAIL");
        unsupported.setError("Unsupported step type: " + type);
        unsupported.setDurationMs(System.currentTimeMillis() - startTime);
        return unsupported;
    }

    private StepResult executeGenerateData(PlanStep step, VariableResolver resolver, long startTime) {
        String asVar = step.getStringArg("as");
        String type = step.getStringArg("type");
        int count = Integer.parseInt(step.getArgs().getOrDefault("count", "1").toString());

        // Concurrency guard: append test thread id and timestamp to generated fields
        String suffix = "_" + Thread.currentThread().getId() + "_" + System.currentTimeMillis() % 10000;
        String firstName = RandomDataUtils.generateRandomString(5) + suffix;
        String lastName = RandomDataUtils.generateRandomString(6);
        String email = (firstName + "." + lastName + "@example.com").toLowerCase(Locale.ROOT);
        String username = "user" + suffix;

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", email);
        userData.put("username", username);
        userData.put("role", "ADMIN");

        context.setVariable(asVar, userData);
        context.setVariable(asVar + ".firstName", firstName);
        context.setVariable(asVar + ".lastName", lastName);
        context.setVariable(asVar + ".email", email);
        context.setVariable(asVar + ".username", username);

        StepResult res = new StepResult(step.getId(), "GENERATE_DATA", step.getSource(), "PASS");
        res.setActual("Generated " + type + " data stored in " + asVar + " (" + email + ")");
        res.setDurationMs(System.currentTimeMillis() - startTime);
        return res;
    }
}
