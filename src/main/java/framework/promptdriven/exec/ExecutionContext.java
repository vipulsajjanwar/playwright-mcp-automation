/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Mutable execution state for a prompt-driven test run, holding variables, secrets, and step outcomes.
 * Created: 2026-10-22
 */
package framework.promptdriven.exec;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import framework.ai.AiConfig;
import framework.promptdriven.report.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolated execution context for a single prompt-driven test.
 * Contains Playwright page and API context, variable map, evidence directories,
 * last API response, and security host validation.
 */
public class ExecutionContext {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionContext.class);

    public record ApiResponseData(int statusCode, String body, Map<String, String> headers, long durationMs) {}

    private final String runId;
    private final String promptName;
    private final boolean failFast;
    private final boolean dryRun;
    private final Path outputDir;
    private final Path screenshotsDir;
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    private final SecretsProvider secretsProvider;
    private final VariableResolver variableResolver;
    private final List<StepResult> stepResults = Collections.synchronizedList(new ArrayList<>());

    private Page page;
    private BrowserContext browserContext;
    private APIRequestContext apiRequestContext;
    private String apiBaseUrl = "";
    private ApiResponseData lastApiResponse;
    private volatile boolean hasFailures = false;

    public ExecutionContext(String runId, String promptName, Path outputDir, boolean failFast, boolean dryRun) {
        this.runId = runId;
        this.promptName = promptName;
        this.failFast = failFast;
        this.dryRun = dryRun;
        this.outputDir = outputDir;
        this.screenshotsDir = outputDir.resolve("screenshots");
        this.secretsProvider = new SecretsProvider();
        this.variableResolver = new VariableResolver(this.variables, this.secretsProvider);

        try {
            Files.createDirectories(this.screenshotsDir);
        } catch (IOException e) {
            logger.warn("Could not create directories for {}: {}", outputDir, e.getMessage());
        }
    }

    public String getRunId() { return runId; }
    public String getPromptName() { return promptName; }
    public boolean isFailFast() { return failFast; }
    public boolean isDryRun() { return dryRun; }
    public Path getOutputDir() { return outputDir; }
    public Path getScreenshotsDir() { return screenshotsDir; }
    public Map<String, Object> getVariables() { return variables; }
    public SecretsProvider getSecretsProvider() { return secretsProvider; }
    public VariableResolver getVariableResolver() { return variableResolver; }
    public List<StepResult> getStepResults() { return stepResults; }

    public Page getPage() { return page; }
    public void setPage(Page page) { this.page = page; }

    public BrowserContext getBrowserContext() { return browserContext; }
    public void setBrowserContext(BrowserContext browserContext) { this.browserContext = browserContext; }

    public APIRequestContext getApiRequestContext() { return apiRequestContext; }
    public void setApiRequestContext(APIRequestContext apiRequestContext) { this.apiRequestContext = apiRequestContext; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public ApiResponseData getLastApiResponse() { return lastApiResponse; }
    public void setLastApiResponse(ApiResponseData lastApiResponse) { this.lastApiResponse = lastApiResponse; }

    public boolean hasFailures() { return hasFailures; }
    public void markFailure() { this.hasFailures = true; }

    public void addStepResult(StepResult result) {
        stepResults.add(result);
        if (!result.isPassed()) {
            hasFailures = true;
        }
    }

    public void setVariable(String name, Object value) {
        if (name != null && value != null) {
            variables.put(name, value);
        }
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    /**
     * Validates that target URL host is within ai.allowedHosts.
     * Throws SecurityException if host is disallowed.
     */
    public void validateHostAllowed(String url) {
        if (url == null || url.isBlank()) return;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && !host.isBlank()) {
                if (!AiConfig.isHostAllowed(host)) {
                    throw new SecurityException(String.format(
                            "Access to host '%s' is blocked by security policy (ai.allowedHosts: %s)",
                            host, AiConfig.getAllowedHosts()));
                }
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Could not parse URI '{}' for host validation: {}", url, e.getMessage());
        }
    }
}
