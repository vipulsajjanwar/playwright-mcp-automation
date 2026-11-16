/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Top-level orchestrator that drives the full prompt-driven test lifecycle: discovery, planning, execution, and reporting.
 * Created: 2026-10-22
 */
package framework.promptdriven;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import framework.ai.AiConfig;
import framework.config.ConfigManager;
import framework.constants.BrowserConstants;
import framework.mcp.McpToolManager;
import framework.promptdriven.exec.ExecutionContext;
import framework.promptdriven.exec.StepExecutor;
import framework.promptdriven.plan.PlanService;
import framework.promptdriven.plan.TestPlan;
import framework.promptdriven.report.ReportWriter;
import framework.promptdriven.report.StepResult;
import framework.promptdriven.report.TestRunResult;
import framework.utils.CustomTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Orchestrator executing a single PromptFile end-to-end within an isolated ExecutionContext.
 */
public class PromptTestRunner {

    private static final Logger logger = LoggerFactory.getLogger(PromptTestRunner.class);

    public static TestRunResult run(PromptFile promptFile, Page page, BrowserContext context) {
        String runId = System.getProperty("prompts.runId");
        if (runId == null || runId.isBlank()) {
            runId = "run_" + CustomTimeUtil.getCurrentTime("yyyyMMdd_HHmmss", "UTC") + "_" + UUID.randomUUID().toString().substring(0, 6);
        }

        boolean dryRun = ConfigManager.getBoolean("prompts.dryRun", false);
        boolean failFast = promptFile.isFailFast();
        String safeName = promptFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path outputDir = Paths.get("target", "prompt-runs", runId, safeName);

        ExecutionContext execContext = new ExecutionContext(runId, promptFile.getName(), outputDir, failFast, dryRun);
        execContext.setPage(page);
        execContext.setBrowserContext(context);

        TestRunResult runResult = new TestRunResult();
        runResult.setRunId(runId);
        runResult.setPromptName(promptFile.getName());
        runResult.setTestName(promptFile.getTestName());
        runResult.setStartTime(System.currentTimeMillis());

        long start = System.currentTimeMillis();
        Throwable executionFailure = null;
        TestPlan plan = null;

        try {
            logger.info("Starting execution of prompt '{}' [type={}, runId={}]", promptFile.getName(), promptFile.getType(), runId);

            // Grounding snapshot if UI page is present
            String compactSnapshot = "";
            if (page != null && !promptFile.getType().equalsIgnoreCase("api")) {
                try {
                    compactSnapshot = page.content();
                    if (compactSnapshot.length() > 2000) {
                        compactSnapshot = compactSnapshot.substring(0, 2000) + "... [truncated]";
                    }
                } catch (Exception ignored) {}
            }

            PlanService planService = new PlanService();
            plan = planService.getOrGeneratePlan(promptFile, compactSnapshot);

            StepExecutor stepExecutor = new StepExecutor(execContext);
            stepExecutor.executePlan(plan);

        } catch (Throwable t) {
            executionFailure = t;
            logger.error("Prompt test '{}' failed: {}", promptFile.getName(), t.getMessage());
        } finally {
            long end = System.currentTimeMillis();
            runResult.setEndTime(end);
            runResult.setDurationMs(end - start);
            runResult.setSteps(execContext.getStepResults());
            runResult.setTotalSteps(execContext.getStepResults().size());

            int passed = 0;
            int failed = 0;
            int healed = 0;
            for (StepResult sr : execContext.getStepResults()) {
                if ("PASS".equalsIgnoreCase(sr.getStatus())) passed++;
                else if ("HEALED".equalsIgnoreCase(sr.getStatus())) {
                    passed++;
                    healed++;
                } else failed++;
            }

            runResult.setPassedSteps(passed);
            runResult.setFailedSteps(failed);
            runResult.setHealedSteps(healed);

            boolean isOverallPass = (executionFailure == null && failed == 0);
            runResult.setStatus(isOverallPass ? "PASSED" : "FAILED");
            if (executionFailure != null) {
                runResult.setErrorMessage(executionFailure.getMessage());
            }

            // Write JSON and HTML reports
            ReportWriter.writeReports(runResult, plan != null ? plan : new TestPlan(), execContext);

            // Register in McpToolManager run store
            McpToolManager.recordRunResult(runResult);
        }

        if (executionFailure != null) {
            if (executionFailure instanceof AssertionError ae) throw ae;
            throw new RuntimeException("Prompt test execution failed: " + executionFailure.getMessage(), executionFailure);
        }

        return runResult;
    }
}
