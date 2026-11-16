/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Executes API action steps defined in prompt-driven test plans using the framework REST client.
 * Created: 2026-10-22
 */
package framework.promptdriven.exec;

import com.fasterxml.jackson.databind.JsonNode;
import framework.promptdriven.plan.ActionType;
import framework.promptdriven.plan.PlanStep;
import framework.promptdriven.report.StepResult;
import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Executes API actions, records response data, validates hosts, and extracts variables.
 */
public class ApiActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ApiActionExecutor.class);
    private final ExecutionContext context;
    private final HttpClient httpClient;

    public ApiActionExecutor(ExecutionContext context) {
        this.context = context;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public StepResult execute(PlanStep step) {
        ActionType type = step.getActionType();
        long startTime = System.currentTimeMillis();
        VariableResolver resolver = context.getVariableResolver();

        StepResult result = new StepResult(step.getId(), step.getAction(), step.getSource(), "PASS");
        result.setCritical(step.isCritical());

        try {
            switch (type) {
                case API_BASE -> {
                    String rawUrl = step.getStringArg("url");
                    String url = resolver.resolve(rawUrl);
                    context.validateHostAllowed(url);
                    context.setApiBaseUrl(url);
                    result.setExpected("Set API Base URL: " + url);
                    result.setActual("API Base URL active");
                }
                case API_REQUEST -> {
                    executeApiRequest(step, resolver, result);
                }
                case API_STORE -> {
                    executeApiStore(step, resolver, result);
                }
                default -> throw new UnsupportedOperationException("Unsupported API action: " + type);
            }
        } catch (Exception e) {
            result.setStatus("FAIL");
            result.setError(e.getMessage());
            logger.error("Failed API step {}: {}", step.getId(), e.getMessage());
        } finally {
            result.setDurationMs(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void executeApiRequest(PlanStep step, VariableResolver resolver, StepResult result) throws Exception {
        String method = step.getStringArg("method").toUpperCase(Locale.ROOT);
        String path = resolver.resolve(step.getStringArg("path"));

        String fullUrl;
        if (path.startsWith("http://") || path.startsWith("https://")) {
            fullUrl = path;
        } else {
            String base = context.getApiBaseUrl();
            if (base.endsWith("/") && path.startsWith("/")) {
                fullUrl = base + path.substring(1);
            } else if (!base.endsWith("/") && !path.startsWith("/")) {
                fullUrl = base + "/" + path;
            } else {
                fullUrl = base + path;
            }
        }

        context.validateHostAllowed(fullUrl);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(30));

        // Headers
        Object headersObj = step.getArg("headers");
        Map<String, String> resolvedHeaders = new LinkedHashMap<>();
        if (headersObj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) headersObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String hKey = String.valueOf(entry.getKey());
                String hVal = resolver.resolve(String.valueOf(entry.getValue()));
                resolvedHeaders.put(hKey, hVal);
                reqBuilder.header(hKey, hVal);
            }
        }
        if (!resolvedHeaders.containsKey("Content-Type") && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
            reqBuilder.header("Content-Type", "application/json");
            resolvedHeaders.put("Content-Type", "application/json");
        }

        // Body
        String resolvedBody = "";
        Object bodyObj = step.getArg("body");
        if (bodyObj != null) {
            if (bodyObj instanceof String strBody) {
                resolvedBody = resolver.resolve(strBody);
            } else {
                resolvedBody = resolver.resolve(JsonUtils.toJson(bodyObj));
            }
        }

        HttpRequest.BodyPublisher bodyPublisher = resolvedBody.isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(resolvedBody);

        reqBuilder.method(method, bodyPublisher);

        long reqStart = System.currentTimeMillis();
        logger.info("Executing API request: {} {}", method, fullUrl);
        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        long reqDuration = System.currentTimeMillis() - reqStart;

        Map<String, String> respHeaders = new LinkedHashMap<>();
        response.headers().map().forEach((k, v) -> respHeaders.put(k, String.join(", ", v)));

        ExecutionContext.ApiResponseData responseData = new ExecutionContext.ApiResponseData(
                response.statusCode(),
                response.body(),
                respHeaders,
                reqDuration
        );
        context.setLastApiResponse(responseData);

        String maskedBody = resolver.mask(response.body());
        if (maskedBody.length() > 500) {
            maskedBody = maskedBody.substring(0, 500) + "... [truncated]";
        }

        result.setApiDetails(String.format("%s %s -> Status: %d (%dms)", method, fullUrl, response.statusCode(), reqDuration));
        result.setActual("Status " + response.statusCode() + ", Body: " + maskedBody);
    }

    private void executeApiStore(PlanStep step, VariableResolver resolver, StepResult result) {
        ExecutionContext.ApiResponseData response = context.getLastApiResponse();
        if (response == null) {
            throw new IllegalStateException("Cannot store from API response: no API request has run yet.");
        }

        String asVar = step.getStringArg("as");
        String jsonPath = step.getStringArg("jsonPath");
        String headerName = step.getStringArg("header");

        if (jsonPath != null && !jsonPath.isBlank()) {
            JsonNode root = JsonUtils.parseJsonNode(response.body());
            JsonNode node = extractJsonPath(root, jsonPath);
            if (node.isMissingNode() || node.isNull()) {
                throw new IllegalArgumentException("JSON path '" + jsonPath + "' not found in API response");
            }
            String value = node.isContainerNode() ? node.toString() : node.asText();
            context.setVariable(asVar, value);
            result.setActual("Stored JSON path '" + jsonPath + "' value '" + resolver.mask(value) + "' into " + asVar);
        } else if (headerName != null && !headerName.isBlank()) {
            String value = response.headers().get(headerName);
            if (value == null) {
                throw new IllegalArgumentException("Header '" + headerName + "' not found in API response");
            }
            context.setVariable(asVar, value);
            result.setActual("Stored header '" + headerName + "' value into " + asVar);
        } else {
            throw new IllegalArgumentException("API_STORE requires either 'jsonPath' or 'header'");
        }
    }

    private JsonNode extractJsonPath(JsonNode root, String path) {
        if (path == null || path.isBlank() || path.equals("$")) return root;
        String clean = path.replaceFirst("^\\$\\.?", "");
        String[] tokens = clean.split("\\.");
        JsonNode curr = root;
        for (String t : tokens) {
            if (t.contains("[") && t.endsWith("]")) {
                String name = t.substring(0, t.indexOf('['));
                int idx = Integer.parseInt(t.substring(t.indexOf('[') + 1, t.length() - 1));
                curr = name.isEmpty() ? curr.path(idx) : curr.path(name).path(idx);
            } else {
                curr = curr.path(t);
            }
        }
        return curr;
    }
}
