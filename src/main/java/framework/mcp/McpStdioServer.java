/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: STDIO transport implementation of an MCP server for local agent integration via standard input/output streams.
 * Created: 2026-10-22
 */
package framework.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Standard I/O MCP transport server.
 * Reads JSON-RPC requests from standard input and sends responses to standard output.
 */
public class McpStdioServer {

    private static final Logger logger = LoggerFactory.getLogger(McpStdioServer.class);
    private volatile boolean running = false;

    public static void main(String[] args) {
        logger.info("Starting McpStdioServer on standard I/O streams...");
        new McpStdioServer().start(System.in, System.out);
    }

    public void start(InputStream in, PrintStream out) {
        running = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String response = McpClient.handleJsonRpcRequest(line);
                if (response != null && !response.isBlank()) {
                    out.println(response);
                    out.flush();
                }
            }
        } catch (IOException e) {
            if (running) {
                logger.error("I/O error in McpStdioServer: {}", e.getMessage(), e);
            }
        } finally {
            running = false;
        }
    }

    public void stop() {
        running = false;
    }
}
