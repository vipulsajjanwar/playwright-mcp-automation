package framework.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import framework.ai.AiConfig;
import framework.config.ConfigManager;
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
 * OpenAI-compatible Chat Completions client supporting GPT-4o / Azure / local vLLM / Ollama.
 */
public class OpenAiCompatLlmClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCompatLlmClient.class);
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final int timeoutSeconds;

    public OpenAiCompatLlmClient() {
        this(AiConfig.getApiKey(), AiConfig.getModel(), ConfigManager.get("ai.endpoint", DEFAULT_ENDPOINT), AiConfig.getTimeoutSeconds());
    }

    public OpenAiCompatLlmClient(String apiKey, String model, String endpoint, int timeoutSeconds) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gpt-4o";
        this.endpoint = (endpoint != null && !endpoint.isBlank()) ? endpoint.trim() : DEFAULT_ENDPOINT;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 60;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.timeoutSeconds))
                .build();
    }

    @Override
    public String generatePlan(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured. Set environment variable "
                    + AiConfig.getApiKeyEnvName() + " or define in secrets.properties.");
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        String jsonPayload = JsonUtils.toJson(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        int maxRetries = 3;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            LlmRateLimiter.acquire();
            try {
                logger.info("Calling OpenAI API [endpoint={}, model={}, attempt={}/{}]", endpoint, model, attempt, maxRetries);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    JsonNode root = JsonUtils.parseJsonNode(response.body());
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        String rawContent = choices.get(0).path("message").path("content").asText();
                        return cleanJsonResponse(rawContent);
                    }
                    throw new RuntimeException("Empty choice content received from OpenAI API: " + response.body());
                } else if (statusCode == 429 || statusCode >= 500) {
                    logger.warn("OpenAI API returned status {}: {}. Retrying in {}ms...", statusCode, response.body(), backoffMs);
                    if (attempt == maxRetries) {
                        throw new RuntimeException("OpenAI API call failed after " + maxRetries + " attempts. Status: " + statusCode + ", Body: " + response.body());
                    }
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                    backoffMs *= 2;
                } else {
                    throw new RuntimeException("OpenAI API error [status=" + statusCode + "]: " + response.body());
                }
            } catch (IOException | InterruptedException e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("OpenAI API call failed after " + maxRetries + " attempts", e);
                }
                logger.warn("Network error during OpenAI API call: {}. Retrying in {}ms...", e.getMessage(), backoffMs);
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
        throw new RuntimeException("OpenAI API call failed unexpectedly");
    }

    @Override
    public String getProviderName() {
        return "openai";
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
