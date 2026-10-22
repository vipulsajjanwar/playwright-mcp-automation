package framework.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import framework.ai.AiConfig;
import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API client supporting Claude 3.5 Sonnet / Haiku with structured output.
 */
public class AnthropicLlmClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(AnthropicLlmClient.class);
    private static final String DEFAULT_ENDPOINT = "https://api.anthropic.com/v1/messages";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    public AnthropicLlmClient() {
        this(AiConfig.getApiKey(), AiConfig.getModel(), AiConfig.getTimeoutSeconds());
    }

    public AnthropicLlmClient(String apiKey, String model, int timeoutSeconds) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "claude-3-5-sonnet-20241022";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 60;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .build();
    }

    @Override
    public String generatePlan(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Anthropic API key is not configured. Set environment variable "
                    + AiConfig.getApiKeyEnvName() + " or define in secrets.properties.");
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 4096,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        String jsonPayload = JsonUtils.toJson(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DEFAULT_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        int maxRetries = 3;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            LlmRateLimiter.acquire();
            try {
                logger.info("Calling Anthropic API [model={}, attempt={}/{}]", model, attempt, maxRetries);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    JsonNode root = JsonUtils.parseJsonNode(response.body());
                    JsonNode contentArray = root.path("content");
                    if (contentArray.isArray() && contentArray.size() > 0) {
                        String rawText = contentArray.get(0).path("text").asText();
                        return cleanJsonResponse(rawText);
                    }
                    throw new RuntimeException("Empty content received from Anthropic API: " + response.body());
                } else if (statusCode == 429 || statusCode >= 500) {
                    logger.warn("Anthropic API returned status {}: {}. Retrying in {}ms...", statusCode, response.body(), backoffMs);
                    if (attempt == maxRetries) {
                        throw new RuntimeException("Anthropic API call failed after " + maxRetries + " attempts. Status: " + statusCode + ", Body: " + response.body());
                    }
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    throw new RuntimeException("Anthropic API error [status=" + statusCode + "]: " + response.body());
                }
            } catch (IOException | InterruptedException e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Anthropic API call failed after " + maxRetries + " attempts", e);
                }
                logger.warn("Network error during Anthropic API call: {}. Retrying in {}ms...", e.getMessage(), backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry backoff", ie);
                }
                backoffMs *= 2;
            } finally {
                LlmRateLimiter.release();
            }
        }
        throw new RuntimeException("Anthropic API call failed unexpectedly");
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
