package framework.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * MCP Protocol Client & JSON-RPC dispatcher.
 * Compliant with the Model Context Protocol (MCP) specification (initialize, tools/list, tools/call).
 */
public class McpClient {

    private static final Logger logger = LoggerFactory.getLogger(McpClient.class);
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private McpClient() {}

    public static String handleJsonRpcRequest(String jsonRpcPayload) {
        logger.info("Processing MCP JSON-RPC Payload: {}", jsonRpcPayload);
        try {
            JsonNode root = JsonUtils.parseJsonNode(jsonRpcPayload);
            String id = root.has("id") ? root.get("id").asText() : "1";
            String method = root.has("method") ? root.get("method").asText() : "";

            // 1. initialize
            if ("initialize".equalsIgnoreCase(method)) {
                return handleInitialize(id);
            }

            // 2. notifications/initialized or notifications
            if (method.startsWith("notifications/")) {
                logger.info("Received notification: {}", method);
                return ""; // Notifications do not receive responses in JSON-RPC
            }

            // 3. tools/list
            if ("tools/list".equalsIgnoreCase(method)) {
                return handleToolsList(id);
            }

            // 4. tools/call
            if ("tools/call".equalsIgnoreCase(method)) {
                return handleToolsCall(id, root.path("params"));
            }

            // 5. Direct legacy tool name dispatch
            if (!method.isEmpty() && !method.contains("/")) {
                Map<String, Object> arguments = extractArguments(root.path("params"));
                String result = McpToolManager.executeTool(method, arguments);
                return createResponse(id, result);
            }

            return createErrorResponse(id, -32601, "Method not found: " + method);
        } catch (Exception e) {
            logger.error("Failed to process MCP JSON-RPC request: {}", e.getMessage(), e);
            return createErrorResponse("1", -32700, "Parse error: " + e.getMessage());
        }
    }

    private static String handleInitialize(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
        result.put("serverInfo", Map.of(
                "name", "playwright-mcp-automation",
                "version", "1.0.0"
        ));
        return createRawResponse(id, result);
    }

    private static String handleToolsList(String id) {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(createToolDef(
                "run_prompt_file",
                "Executes a single prompt file (.prompt.txt) using deterministic closed-action vocabulary.",
                Map.of(
                        "path", Map.of("type", "string", "description", "Path to prompt file"),
                        "dryRun", Map.of("type", "boolean", "description", "Validate and plan without browser execution"),
                        "replan", Map.of("type", "boolean", "description", "Force LLM re-planning bypassing cache")
                ),
                List.of("path")
        ));

        tools.add(createToolDef(
                "run_prompt_suite",
                "Discovers and executes a suite of prompt files in parallel.",
                Map.of(
                        "dir", Map.of("type", "string", "description", "Prompts directory"),
                        "include", Map.of("type", "string", "description", "File include pattern"),
                        "tags", Map.of("type", "string", "description", "Tag filter"),
                        "threads", Map.of("type", "integer", "description", "Concurrency thread count")
                ),
                List.of()
        ));

        tools.add(createToolDef(
                "get_prompt_run_status",
                "Gets the real status and step metrics from a prompt run.",
                Map.of("runId", Map.of("type", "string", "description", "Run ID")),
                List.of()
        ));

        tools.add(createToolDef(
                "get_prompt_run_report",
                "Returns the report JSON and HTML artifact paths for a prompt run.",
                Map.of("runId", Map.of("type", "string", "description", "Run ID")),
                List.of()
        ));

        tools.add(createToolDef(
                "get_failed_tests",
                "Retrieves real test and step failures recorded from the latest run.",
                Collections.emptyMap(),
                List.of()
        ));

        tools.add(createToolDef(
                "run_test",
                "Executes a TestNG test class programmatically.",
                Map.of("testClass", Map.of("type", "string", "description", "Fully-qualified TestNG test class name")),
                List.of("testClass")
        ));

        tools.add(createToolDef(
                "run_test_suite",
                "Executes a TestNG XML suite with path traversal protection.",
                Map.of("suite", Map.of("type", "string", "description", "Suite name or path (e.g. smoke, icedq/admin/smoke)")),
                List.of("suite")
        ));

        tools.add(createToolDef(
                "generate_prompt_file",
                "Generates a new prompt file from requirements. Never overwrites existing files.",
                Map.of(
                        "requirement", Map.of("type", "string", "description", "Test requirement text"),
                        "fileName", Map.of("type", "string", "description", "Filename ending in .prompt.txt")
                ),
                List.of("requirement")
        ));

        tools.add(createToolDef(
                "analyze_failure",
                "Classifies failure root cause, timeouts, and locator issues using AI analyzers.",
                Map.of(
                        "testName", Map.of("type", "string", "description", "Name of the failing test"),
                        "errorMessage", Map.of("type", "string", "description", "Error message")
                ),
                List.of("testName", "errorMessage")
        ));

        tools.add(createToolDef(
                "generate_test_data",
                "Generates realistic synthetic user test data.",
                Map.of("count", Map.of("type", "integer", "description", "Record count")),
                List.of()
        ));

        // Security check: Only expose database query execution if allowed
        if (McpConfig.isDbExecutionAllowed()) {
            tools.add(createToolDef(
                    "execute_database_query",
                    "Executes a SQL query against the configured test database.",
                    Map.of("query", Map.of("type", "string", "description", "SQL query string")),
                    List.of("query")
            ));
        }

        Map<String, Object> result = Map.of("tools", tools);
        return createRawResponse(id, result);
    }

    private static Map<String, Object> createToolDef(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return Map.of(
                "name", name,
                "description", description,
                "inputSchema", schema
        );
    }

    private static String handleToolsCall(String id, JsonNode paramsNode) {
        String toolName = paramsNode.path("name").asText();
        if (toolName.isBlank()) {
            return createErrorResponse(id, -32602, "Missing tool name in params");
        }

        Map<String, Object> arguments = extractArguments(paramsNode.path("arguments"));
        String toolOutputJson = McpToolManager.executeTool(toolName, arguments);

        // Standard MCP tool response formatting: content array of text items
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of(Map.of(
                "type", "text",
                "text", toolOutputJson
        )));
        result.put("isError", toolOutputJson.contains("\"error\":"));
        return createRawResponse(id, result);
    }

    private static Map<String, Object> extractArguments(JsonNode argsNode) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (argsNode != null && argsNode.isObject()) {
            argsNode.fields().forEachRemaining(entry -> {
                JsonNode v = entry.getValue();
                if (v.isBoolean()) map.put(entry.getKey(), v.asBoolean());
                else if (v.isInt() || v.isLong()) map.put(entry.getKey(), v.asLong());
                else if (v.isDouble()) map.put(entry.getKey(), v.asDouble());
                else map.put(entry.getKey(), v.asText());
            });
        }
        return map;
    }

    private static String createRawResponse(String id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return JsonUtils.toJson(response);
    }

    private static String createResponse(String id, String contentJson) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        try {
            response.put("result", JsonUtils.parseJsonNode(contentJson));
        } catch (Exception e) {
            response.put("result", contentJson);
        }
        return JsonUtils.toJson(response);
    }

    private static String createErrorResponse(String id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return JsonUtils.toJson(response);
    }
}
