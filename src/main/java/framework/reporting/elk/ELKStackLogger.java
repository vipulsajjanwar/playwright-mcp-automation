/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Ships structured log events to Elasticsearch via Logstash HTTP endpoint for real-time test observability.
 * Created: 2026-10-01
 */
package framework.reporting.elk;

import framework.config.ConfigManager;
import framework.config.ReportingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated logger responsible solely for transmitting serialized test results
 * to Elasticsearch. Supports non-blocking asynchronous execution.
 */
public final class ELKStackLogger {

    private static final Logger logger = LoggerFactory.getLogger(ELKStackLogger.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final ExecutorService ASYNC_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r, "elk-reporter-worker");
        thread.setDaemon(true);
        return thread;
    });

    private ELKStackLogger() {}

    public static void sendResult(String jsonPayload) {
        if (!ReportingConfig.isElkEnabled()) {
            return;
        }

        if (ReportingConfig.isAsyncElkEnabled()) {
            ASYNC_EXECUTOR.submit(() -> postDocument(jsonPayload));
        } else {
            postDocument(jsonPayload);
        }
    }

    private static void postDocument(String jsonPayload) {
        try {
            String host = ConfigManager.get("elk.host", "localhost");
            String port = ConfigManager.get("elk.port", "9200");
            String index = ConfigManager.get("elk.index", "test-automation");
            String user = ConfigManager.get("elk.username", "");
            String pass = ConfigManager.get("elk.password", "");

            if (host.endsWith("/")) {
                host = host.substring(0, host.length() - 1);
            }

            String url = String.format("http://%s:%s/%s/_doc", host, port, index);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8));

            if (!user.isBlank()) {
                String auth = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
                requestBuilder.header("Authorization", "Basic " + auth);
            }

            HttpResponse<String> response = HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Successfully posted test result to ELK [status: {}]", response.statusCode());
            } else {
                logger.warn("ELK server returned non-success response [status: {}]: {}",
                        response.statusCode(), response.body());
            }
        } catch (Exception e) {
            // Reporting failures must never fail the test
            logger.warn("ELK logging skipped due to connection failure: {}", e.getMessage());
        }
    }

    public static void shutdown() {
        try {
            ASYNC_EXECUTOR.shutdown();
            if (!ASYNC_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                ASYNC_EXECUTOR.shutdownNow();
            }
        } catch (Exception ignored) {
            ASYNC_EXECUTOR.shutdownNow();
        }
    }
}
