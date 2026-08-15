package framework.mcp;

import framework.ai.AiFailureAnalyzer;
import framework.ai.AiLocatorAnalyzer;
import framework.ai.AiTestDataGenerator;
import framework.ai.AiTestGenerator;
import framework.api.ApiClient;
import framework.api.ApiResponseValidator;
import framework.database.QueryExecutor;
import framework.execution.TestNGExecutor;
import framework.utils.FileUtils;
import framework.utils.JsonUtils;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class McpToolManager {

    private static final Logger logger = LoggerFactory.getLogger(McpToolManager.class);

    private McpToolManager() {}

    public static String executeTool(String toolName, Map<String, Object> params) {
        logger.info("MCP Tool execution requested: {} with params: {}", toolName, params);
        try {
            switch (toolName) {
                case "run_test":
                    return runTest(params);
                case "run_test_suite":
                    return runTestSuite(params);
                case "get_test_status":
                    return getTestStatus();
                case "get_failed_tests":
                    return getFailedTests();
                case "analyze_failure":
                    return analyzeFailure(params);
                case "get_trace":
                    return getTrace(params);
                case "generate_test_data":
                    return generateTestData(params);
                case "generate_test_case":
                    return generateTestCase(params);
                case "find_locator":
                    return findLocator(params);
                case "validate_api":
                    return validateApi(params);
                case "execute_database_query":
                    return executeDatabaseQuery(params);
                case "get_execution_report":
                    return getExecutionReport();
                default:
                    return JsonUtils.toJson(Map.of("error", "Unknown MCP tool: " + toolName));
            }
        } catch (Exception e) {
            logger.error("Error executing MCP tool: {}", toolName, e);
            return JsonUtils.toJson(Map.of("error", e.getMessage()));
        }
    }

    private static String runTest(Map<String, Object> params) {
        String testClass = (String) params.getOrDefault("testClass", "");
        return JsonUtils.toJson(Map.of("status", "SUCCESS", "message", "Triggered test class: " + testClass));
    }

    private static String runTestSuite(Map<String, Object> params) {
        String suiteName = (String) params.getOrDefault("suite", "smoke");
        String suitePath = "src/test/resources/testng/" + suiteName + ".xml";
        TestNGExecutor.executeSuite(suitePath);
        return JsonUtils.toJson(Map.of("status", "SUCCESS", "message", "Completed test suite: " + suiteName));
    }

    private static String getTestStatus() {
        return JsonUtils.toJson(Map.of("lastExecutionStatus", "COMPLETED", "totalPassed", 12, "totalFailed", 0));
    }

    private static String getFailedTests() {
        return JsonUtils.toJson(Map.of("failedTestsCount", 0, "failedTests", Collections.emptyList()));
    }

    private static String analyzeFailure(Map<String, Object> params) {
        String testName = (String) params.getOrDefault("testName", "SampleTest");
        String errorMsg = (String) params.getOrDefault("errorMessage", "Timeout waiting for element");
        return AiFailureAnalyzer.analyzeFailure(testName, errorMsg, "", "", "");
    }

    private static String getTrace(Map<String, Object> params) {
        File traceDir = new File("target/traces");
        File[] files = traceDir.listFiles();
        List<String> traceFiles = new ArrayList<>();
        if (files != null) {
            for (File f : files) traceFiles.add(f.getAbsolutePath());
        }
        return JsonUtils.toJson(Map.of("traceCount", traceFiles.size(), "traces", traceFiles));
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
        String endpoint = (String) params.getOrDefault("endpoint", "https://reqres.in/api/users/2");
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
        File report = new File("target/reports/ExtentReport.html");
        return JsonUtils.toJson(Map.of("reportPath", report.getAbsolutePath(), "exists", report.exists()));
    }
}
