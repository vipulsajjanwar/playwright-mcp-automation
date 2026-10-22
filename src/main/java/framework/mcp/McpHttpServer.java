package framework.mcp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Localhost-only HTTP server transport for MCP JSON-RPC requests.
 * Binds strictly to 127.0.0.1 on the configured mcp.serverPort.
 */
public class McpHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(McpHttpServer.class);

    private final int port;
    private HttpServer server;

    public McpHttpServer() {
        this(McpConfig.getServerPort());
    }

    public McpHttpServer(int port) {
        this.port = port > 0 ? port : 8080;
    }

    public static void main(String[] args) throws IOException {
        McpHttpServer httpServer = new McpHttpServer();
        httpServer.start();
        logger.info("McpHttpServer running on http://127.0.0.1:{}", httpServer.getPort());
    }

    public synchronized void start() throws IOException {
        if (server != null) return;

        // Strictly bind to 127.0.0.1 for local isolation and security
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
        server = HttpServer.create(address, 0);

        HttpHandler rpcHandler = new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                String jsonRpcResponse = McpClient.handleJsonRpcRequest(body);
                byte[] responseBytes = jsonRpcResponse.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        };

        server.createContext("/", rpcHandler);
        server.createContext("/mcp", rpcHandler);
        server.setExecutor(null);
        server.start();

        logger.info("McpHttpServer successfully started on http://127.0.0.1:{}", port);
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            logger.info("McpHttpServer stopped.");
        }
    }

    public int getPort() {
        return port;
    }
}
