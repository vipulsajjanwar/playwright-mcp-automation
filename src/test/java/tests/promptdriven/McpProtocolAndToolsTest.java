package tests.promptdriven;

import com.fasterxml.jackson.databind.JsonNode;
import framework.mcp.McpClient;
import framework.mcp.McpStdioServer;
import framework.mcp.McpToolManager;
import framework.utils.JsonUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class McpProtocolAndToolsTest {

    @Test
    public void testMcpInitialize() {
        String initRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": "init-1",
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {"name": "test-client", "version": "1.0.0"}
                  }
                }
                """;

        String response = McpClient.handleJsonRpcRequest(initRequest);
        JsonNode root = JsonUtils.parseJsonNode(response);

        Assert.assertEquals(root.path("id").asText(), "init-1");
        Assert.assertEquals(root.path("result").path("protocolVersion").asText(), "2024-11-05");
        Assert.assertEquals(root.path("result").path("serverInfo").path("name").asText(), "playwright-mcp-automation");
    }

    @Test
    public void testMcpToolsList() {
        String listRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": "list-1",
                  "method": "tools/list",
                  "params": {}
                }
                """;

        String response = McpClient.handleJsonRpcRequest(listRequest);
        JsonNode root = JsonUtils.parseJsonNode(response);
        JsonNode tools = root.path("result").path("tools");

        Assert.assertTrue(tools.isArray());
        Assert.assertTrue(tools.size() >= 8);

        boolean foundRunPromptFile = false;
        boolean foundGeneratePrompt = false;
        for (JsonNode t : tools) {
            String name = t.path("name").asText();
            if ("run_prompt_file".equals(name)) foundRunPromptFile = true;
            if ("generate_prompt_file".equals(name)) foundGeneratePrompt = true;
            Assert.assertTrue(t.has("inputSchema"), "Tool " + name + " missing inputSchema");
        }
        Assert.assertTrue(foundRunPromptFile);
        Assert.assertTrue(foundGeneratePrompt);
    }

    @Test
    public void testMcpToolsCallRunPromptFileDryRun() {
        String callRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": "call-1",
                  "method": "tools/call",
                  "params": {
                    "name": "run_prompt_file",
                    "arguments": {
                      "path": "src/test/resources/prompts/api_users_crud.prompt.txt",
                      "dryRun": true
                    }
                  }
                }
                """;

        String response = McpClient.handleJsonRpcRequest(callRequest);
        JsonNode root = JsonUtils.parseJsonNode(response);

        Assert.assertEquals(root.path("id").asText(), "call-1");
        JsonNode content = root.path("result").path("content");
        Assert.assertTrue(content.isArray() && content.size() > 0);
        String text = content.get(0).path("text").asText();
        JsonNode toolResultNode = JsonUtils.parseJsonNode(text);
        Assert.assertEquals(toolResultNode.path("status").asText(), "PASSED");
    }

    @Test
    public void testMcpPathTraversalRejection() {
        String traversalRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": "sec-1",
                  "method": "tools/call",
                  "params": {
                    "name": "run_test_suite",
                    "arguments": {
                      "suite": "../../etc/passwd"
                    }
                  }
                }
                """;

        String response = McpClient.handleJsonRpcRequest(traversalRequest);
        JsonNode root = JsonUtils.parseJsonNode(response);
        String text = root.path("result").path("content").get(0).path("text").asText();

        Assert.assertTrue(text.contains("forbidden") || text.contains("Path traversal"));
    }

    @Test
    public void testMcpStdioServerCommunication() {
        String inputJson = "{\"jsonrpc\":\"2.0\",\"id\":\"stdio-1\",\"method\":\"get_test_status\",\"params\":{}}\n";
        ByteArrayInputStream in = new ByteArrayInputStream(inputJson.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);

        McpStdioServer server = new McpStdioServer();
        server.start(in, ps);

        String output = out.toString(StandardCharsets.UTF_8);
        Assert.assertTrue(output.contains("stdio-1"));
        Assert.assertTrue(output.contains("lastExecutionStatus"));
    }
}
