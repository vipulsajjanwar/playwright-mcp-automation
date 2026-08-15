package framework.mcp;

import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class McpClient {

    private static final Logger logger = LoggerFactory.getLogger(McpClient.class);

    private McpClient() {}

    public static String handleJsonRpcRequest(String jsonRpcPayload) {
        logger.info("Processing MCP JSON-RPC Payload: {}", jsonRpcPayload);
        try {
            var node = JsonUtils.parseJsonNode(jsonRpcPayload);
            String id = node.has("id") ? node.get("id").asText() : "1";
            String method = node.has("method") ? node.get("method").asText() : "";

            if (!"tools/call".equals(method) && !method.isEmpty()) {
                return createResponse(id, McpToolManager.executeTool(method, new LinkedHashMap<>()));
            }

            if (node.has("params") && node.get("params").has("name")) {
                String toolName = node.get("params").get("name").asText();
                Map<String, Object> arguments = new LinkedHashMap<>();
                if (node.get("params").has("arguments")) {
                    node.get("params").get("arguments").fields().forEachRemaining(entry ->
                            arguments.put(entry.getKey(), entry.getValue().asText())
                    );
                }
                String result = McpToolManager.executeTool(toolName, arguments);
                return createResponse(id, result);
            }

            return createResponse(id, JsonUtils.toJson(Map.of("error", "Invalid JSON-RPC request structure")));
        } catch (Exception e) {
            logger.error("Failed to parse MCP JSON-RPC request", e);
            return createResponse("1", JsonUtils.toJson(Map.of("error", e.getMessage())));
        }
    }

    private static String createResponse(String id, String contentJson) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", JsonUtils.parseJsonNode(contentJson));
        return JsonUtils.toJson(response);
    }
}
