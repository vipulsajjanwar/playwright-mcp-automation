/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Registry and dispatcher for MCP tools exposed by the framework, mapping tool names to execution handlers.
 * Created: 2026-08-15
 */
package framework.mcp;

import framework.ai.AiConfig;
import framework.ai.AiFailureAnalyzer;
import framework.ai.AiLocatorAnalyzer;
import framework.ai.AiTestDataGenerator;
import framework.ai.AiTestGenerator;
import framework.api.ApiClient;
import framework.api.ApiResponseValidator;
import framework.database.QueryExecutor;
import framework.execution.TestNGExecutor;
import framework.promptdriven.PromptDiscovery;
import framework.promptdriven.PromptFile;
import framework.promptdriven.PromptParser;
import framework.promptdriven.PromptTestRunner;
import framework.promptdriven.report.StepResult;
import framework.promptdriven.report.TestRunResult;
import framework.utils.JsonUtils;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestNG;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * Central MCP Tool Execution Manager.
 * Exposes test execution, prompt execution, diagnostics, reports, and AI generation tools.
 */
public class McpToolManager {

    private static final Logger logger = LoggerFactory.getLogger(McpToolManager.class);

    private static final Map<String, TestRunResult> RUN_RESULTS = new ConcurrentHashMap<>();
    private static volatile TestRunResult LAST_RUN_RESULT = null;
    private static final List<String> LAST_FAILED_TESTS = Collections.synchronizedList(new ArrayList<>());

    private McpToolManager() {}

    public static void recordRunResult(TestRunResult result) {
        if (result != null) {
            LAST_RUN_RESULT = result;
            if (result.getRunId() != null) {
                RUN_RESULTS.put(result.getRunId(), result);
            }
            if (!result.isPassed()) {
                LAST_FAILED_TESTS.add(result.getPromptName() + " (" + result.getTestName() + ")");
            }
        }
    }

    public static String executeTool(String toolName, Map<String, Object> params) {
        logger.info("Executing MCP Tool: {} with params: {}", toolName, params);
        try {
            return switch (toolName) {
                case "run_prompt_file" -> runPromptFile(params);
                case "run_prompt_suite" -> runPromptSuite(params);
                case "get_prompt_run_status" -> getPromptRunStatus(params);
                case "get_prompt_run_report" -> getPromptRunReport(params);
                case "get_failed_tests" -> getFailedTests();
                case "run_test" -> runTest(params);
                case "run_test_suite" -> runTestSuite(params);
                case "generate_prompt_file" -> generatePromptFile(params);
                case "get_test_status" -> getTestStatus();
                case "analyze_failure" -> analyzeFailure(params);
                case "get_trace" -> getTrace(params);
                case "generate_test_data" -> generateTestData(params);
                case "generate_test_case" -> generateTestCase(params);
                case "find_locator" -> findLocator(params);
                case "validate_api" -> validateApi(params);
                case "execute_database_query" -> executeDatabaseQuery(params);
                case "get_execution_report" -> getExecutionReport();
                default -> JsonUtils.toJson(Map.of("error", "Unknown MCP tool: " + toolName));
            };
        } catch (Exception e) {
            logger.error("Error executing MCP tool '{}': {}", toolName, e.getMessage(), e);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }

    private static String runPromptFile(Map<String, Object> params) throws Exception {
        String pathStr = (String) params.get("path");
        if (pathStr == null || pathStr.isBlank()) {
            return JsonUtils.toJson(Map.of("error", "Missing required argument 'path'"));
        }

        Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            // Check under prompts dir
            path = Paths.get(AiConfig.getPromptsDir(), pathStr);
        }
        if (!Files.exists(path)) {
            return JsonUtils.toJson(Map.of("error", "Prompt file not found: " + pathStr));
        }

        boolean dryRun = Boolean.parseBoolean(String.valueOf(params.getOrDefault("dryRun", "false")));
        boolean replan = Boolean.parseBoolean(String.valueOf(params.getOrDefault("replan", "false")));

        String oldDryRun = System.getProperty("prompts.dryRun");
        String oldReplan = System.getProperty("ai.replan");
        try {
            if (dryRun) System.setProperty("prompts.dryRun", "true");
            if (replan) System.setProperty("ai.replan", "true");

            PromptFile promptFile = PromptParser.parse(path);
            TestRunResult result = PromptTestRunner.run(promptFile, null, null);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("runId", result.getRunId());
            resp.put("promptName", result.getPromptName());
            resp.put("status", result.getStatus());
            resp.put("totalSteps", result.getTotalSteps());
            resp.put("passedSteps", result.getPassedSteps());
            resp.put("failedSteps", result.getFailedSteps());
            resp.put("healedSteps", result.getHealedSteps());
            resp.put("durationMs", result.getDurationMs());
            resp.put("reportHtml", result.getReportHtmlPath());
            resp.put("reportJson", result.getReportJsonPath());
            return JsonUtils.toJson(resp);
        } finally {
            if (oldDryRun != null) System.setProperty("prompts.dryRun", oldDryRun); else System.clearProperty("prompts.dryRun");
            if (oldReplan != null) System.setProperty("ai.replan", oldReplan); else System.clearProperty("ai.replan");
        }
    }

    private static String runPromptSuite(Map<String, Object> params) {
        String dir = (String) params.getOrDefault("dir", AiConfig.getPromptsDir());
        String include = (String) params.getOrDefault("include", "");
        String tags = (String) params.getOrDefault("tags", "");
        int threads = Integer.parseInt(params.getOrDefault("threads", "1").toString());

        List<PromptFile> files = PromptDiscovery.discover(dir, include, "", tags);
        if (files.isEmpty()) {
            return JsonUtils.toJson(Map.of("message", "No prompt files discovered", "totalRun", 0));
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, threads));
        List<Future<TestRunResult>> futures = new ArrayList<>();

        for (PromptFile pf : files) {
            futures.add(executor.submit(() -> PromptTestRunner.run(pf, null, null)));
        }

        executor.shutdown();
        List<Map<String, Object>> summaries = new ArrayList<>();
        int passed = 0;
        int failed = 0;

        for (Future<TestRunResult> f : futures) {
            try {
                TestRunResult r = f.get(10, TimeUnit.MINUTES);
                if (r.isPassed()) passed++; else failed++;
                summaries.add(Map.of(
                        "promptName", r.getPromptName(),
                        "status", r.getStatus(),
                        "durationMs", r.getDurationMs(),
                        "reportHtml", r.getReportHtmlPath() != null ? r.getReportHtmlPath() : ""
                ));
            } catch (Exception e) {
                failed++;
                summaries.add(Map.of("error", e.getMessage(), "status", "FAILED"));
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("totalFiles", files.size());
        resp.put("passed", passed);
        resp.put("failed", failed);
        resp.put("results", summaries);
        return JsonUtils.toJson(resp);
    }

    private static String getPromptRunStatus(Map<String, Object> params) {
        String runId = (String) params.get("runId");
        TestRunResult target = null;
        if (runId != null && !runId.isBlank()) {
            target = RUN_RESULTS.get(runId.trim());
        } else {
            target = LAST_RUN_RESULT;
        }

        if (target == null) {
            return JsonUtils.toJson(Map.of("status", "NO_RUNS_RECORDED"));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("runId", target.getRunId());
        resp.put("promptName", target.getPromptName());
        resp.put("status", target.getStatus());
        resp.put("totalSteps", target.getTotalSteps());
        resp.put("passedSteps", target.getPassedSteps());
        resp.put("failedSteps", target.getFailedSteps());
        resp.put("healedSteps", target.getHealedSteps());
        resp.put("durationMs", target.getDurationMs());
        resp.put("errorMessage", target.getErrorMessage() != null ? target.getErrorMessage() : "");
        return JsonUtils.toJson(resp);
    }

    private static String getPromptRunReport(Map<String, Object> params) {
        String runId = (String) params.get("runId");
        TestRunResult target = (runId != null && !runId.isBlank()) ? RUN_RESULTS.get(runId.trim()) : LAST_RUN_RESULT;

        if (target == null) {
            return JsonUtils.toJson(Map.of("error", "No execution report found"));
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("runId", target.getRunId());
        resp.put("reportHtmlPath", target.getReportHtmlPath());
        resp.put("reportJsonPath", target.getReportJsonPath());
        resp.put("reportData", target);
        return JsonUtils.toJson(resp);
    }

    private static String getFailedTests() {
        if (LAST_RUN_RESULT != null && !LAST_RUN_RESULT.isPassed()) {
            List<Map<String, Object>> failedSteps = new ArrayList<>();
            for (StepResult s : LAST_RUN_RESULT.getSteps()) {
                if (!s.isPassed()) {
                    failedSteps.add(Map.of(
                            "stepId", s.getStepId(),
                            "action", s.getAction(),
                            "source", s.getSource(),
                            "error", s.getError() != null ? s.getError() : "Failed"
                    ));
                }
            }
            return JsonUtils.toJson(Map.of(
                    "failedTestsCount", LAST_FAILED_TESTS.size(),
                    "failedTests", LAST_FAILED_TESTS,
                    "lastRunFailedSteps", failedSteps
            ));
        }
        return JsonUtils.toJson(Map.of(
                "failedTestsCount", LAST_FAILED_TESTS.size(),
                "failedTests", LAST_FAILED_TESTS
        ));
    }

    private static String runTest(Map<String, Object> params) {
        String testClass = (String) params.get("testClass");
        if (testClass == null || testClass.isBlank()) {
            return JsonUtils.toJson(Map.of("error", "Missing required 'testClass' parameter"));
        }

        try {
            Class<?> clazz = Class.forName(testClass);
            TestNG testng = new TestNG();
            testng.setTestClasses(new Class<?>[]{clazz});
            testng.setUseDefaultListeners(false);
            testng.run();

            boolean hasFailure = testng.hasFailure();
            if (hasFailure) {
                LAST_FAILED_TESTS.add(testClass);
            }
            return JsonUtils.toJson(Map.of(
                    "status", hasFailure ? "FAILED" : "SUCCESS",
                    "testClass", testClass,
                    "hasFailures", hasFailure
            ));
        } catch (ClassNotFoundException e) {
            return JsonUtils.toJson(Map.of("error", "Class not found: " + testClass));
        } catch (Exception e) {
            return JsonUtils.toJson(Map.of("error", "Execution failed: " + e.getMessage()));
        }
    }

    private static String runTestSuite(Map<String, Object> params) {
        String suite = (String) params.getOrDefault("suite", "smoke");

        // Path traversal rejection
        if (suite.contains("..") || suite.startsWith("/") || suite.startsWith("\\")) {
            return JsonUtils.toJson(Map.of("error", "Invalid suite name: Path traversal is forbidden."));
        }

        String suitePath = suite.endsWith(".xml") ? suite : suite + ".xml";
        if (!suitePath.startsWith("src/")) {
            suitePath = "src/test/resources/testng/" + suitePath;
        }

        File f = new File(suitePath);
        if (!f.exists()) {
            return JsonUtils.toJson(Map.of("error", "Suite file does not exist: " + suitePath));
        }

        TestNGExecutor.executeSuite(suitePath);
        return JsonUtils.toJson(Map.of("status", "SUCCESS", "message", "Executed suite: " + suitePath));
    }

    private static String generatePromptFile(Map<String, Object> params) {
        String requirement = (String) params.get("requirement");
        if (requirement == null || requirement.isBlank()) {
            return JsonUtils.toJson(Map.of("error", "Missing required 'requirement' parameter"));
        }

        String fileName = (String) params.getOrDefault("fileName", "generated_test_" + System.currentTimeMillis() % 10000 + ".prompt.txt");
        if (!fileName.endsWith(".prompt.txt") && !fileName.endsWith(".prompt")) {
            fileName += ".prompt.txt";
        }

        Path promptsDir = Paths.get(AiConfig.getPromptsDir());
        Path targetPath = promptsDir.resolve(fileName);

        // Security / safety: new file only, never overwrite!
        if (Files.exists(targetPath)) {
            return JsonUtils.toJson(Map.of("error", "File already exists: " + targetPath + ". Overwriting is strictly forbidden."));
        }

        String draftContent = String.format("""
                # test: %s
                # type: ui
                # mode: soft
                // Generated by MCP from requirement: %s
                go to https://app.example.com
                login with username ${USER_ADMIN} and password ${PASS_ADMIN}
                verify all UI components: header logo, search box, profile menu
                """, fileName.replace(".prompt.txt", ""), requirement);

        try {
            Files.createDirectories(promptsDir);
            Files.writeString(targetPath, draftContent);
            return JsonUtils.toJson(Map.of(
                    "status", "SUCCESS",
                    "createdPath", targetPath.toString(),
                    "content", draftContent
            ));
        } catch (IOException e) {
            return JsonUtils.toJson(Map.of("error", "Failed to write prompt file: " + e.getMessage()));
        }
    }

    private static String getTestStatus() {
        if (LAST_RUN_RESULT != null) {
            return JsonUtils.toJson(Map.of(
                    "lastExecutionStatus", LAST_RUN_RESULT.getStatus(),
                    "totalPassed", LAST_RUN_RESULT.getPassedSteps(),
                    "totalFailed", LAST_RUN_RESULT.getFailedSteps(),
                    "runId", LAST_RUN_RESULT.getRunId()
            ));
        }
        return JsonUtils.toJson(Map.of(
                "lastExecutionStatus", "NO_EXECUTIONS",
                "totalPassed", 0,
                "totalFailed", 0
        ));
    }

    private static String analyzeFailure(Map<String, Object> params) {
        String testName = (String) params.getOrDefault("testName", "SampleTest");
        String errorMsg = (String) params.getOrDefault("errorMessage", "Timeout waiting for element");
        return AiFailureAnalyzer.analyzeFailure(testName, errorMsg, "", "", "");
    }

    private static String getTrace(Map<String, Object> params) {
        File traceDir = new File("reports");
        List<String> traceFiles = new ArrayList<>();
        findTracesRecursive(traceDir, traceFiles);
        return JsonUtils.toJson(Map.of("traceCount", traceFiles.size(), "traces", traceFiles));
    }

    private static void findTracesRecursive(File dir, List<String> list) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) findTracesRecursive(f, list);
                    else if (f.getName().endsWith(".zip")) list.add(f.getAbsolutePath());
                }
            }
        }
    }

    private static String generateTestData(Map<String, Object> params) {
        int count = Integer.parseInt(params.getOrDefault("count", "5").toString());
        return AiTestDataGenerator.generateUserDataJson(count);
    }

    private static String generateTestCase(Map<String, Object> params) {
        String req = (String) params.getOrDefault("requirement", "User login with MFA");
        return AiTestGenerator.generateTestSkeleton(req);
    }

    private static String findLocator(Map<String, Object> params) {
        String locator = (String) params.getOrDefault("failedLocator", "#btn-submit");
        String name = (String) params.getOrDefault("elementName", "Submit");
        return AiLocatorAnalyzer.analyzeAndSuggestLocators(locator, name, "Application Login");
    }

    private static String validateApi(Map<String, Object> params) {
        String endpoint = (String) params.getOrDefault("endpoint", "https://jsonplaceholder.typicode.com/todos/1");
        Response response = ApiClient.get(endpoint);
        ApiResponseValidator.validateStatusCode(response, 200);
        return JsonUtils.toJson(Map.of("statusCode", response.getStatusCode(), "responseTimeMs", response.getTime()));
    }

    private static String executeDatabaseQuery(Map<String, Object> params) {
        if (!McpConfig.isDbExecutionAllowed()) {
            return JsonUtils.toJson(Map.of("error", "Database query execution is disabled in MCP security config"));
        }
        String sql = (String) params.getOrDefault("query", "SELECT 1");
        List<Map<String, Object>> result = QueryExecutor.executeQuery(sql);
        return JsonUtils.toJson(result);
    }

    private static String getExecutionReport() {
        File report = new File("reports");
        return JsonUtils.toJson(Map.of("reportDirectory", report.getAbsolutePath(), "exists", report.exists()));
    }
}
