package framework.promptdriven.plan;

import com.fasterxml.jackson.databind.JsonNode;
import framework.ai.AiConfig;
import framework.ai.llm.LlmClient;
import framework.ai.llm.LlmClientFactory;
import framework.config.ConfigManager;
import framework.promptdriven.PromptFile;
import framework.utils.CustomTimeUtil;
import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for synthesizing, validating, retrying, and caching execution plans.
 */
public class PlanService {

    private static final Logger logger = LoggerFactory.getLogger(PlanService.class);

    private final LlmClient llmClient;
    private final PlanCache planCache;

    public PlanService() {
        this(LlmClientFactory.getClient(), new PlanCache());
    }

    public PlanService(LlmClient llmClient, PlanCache planCache) {
        this.llmClient = llmClient != null ? llmClient : LlmClientFactory.getClient();
        this.planCache = planCache != null ? planCache : new PlanCache();
    }

    public TestPlan getOrGeneratePlan(PromptFile promptFile, String compactAriaSnapshot) {
        String promptHash = PlanCache.computeHash(promptFile.getRawContent());

        // 1. Check cache first
        Optional<TestPlan> cached = planCache.get(promptHash);
        if (cached.isPresent()) {
            TestPlan plan = cached.get();
            PlanValidator.ValidationResult valResult = PlanValidator.validate(plan);
            if (valResult.isValid()) {
                logger.info("Using cached plan for prompt '{}' (hash: {})", promptFile.getName(), promptHash);
                return plan;
            } else {
                logger.warn("Cached plan for '{}' failed validation: {}. Re-planning...",
                        promptFile.getName(), valResult.getErrorMessage());
            }
        }

        // 2. Generate plan via LLM
        TestPlan generatedPlan = generatePlanWithRetry(promptFile, compactAriaSnapshot);
        generatedPlan.setPromptHash(promptHash);
        generatedPlan.setGeneratedAt(CustomTimeUtil.getCurrentTime("yyyy-MM-dd HH:mm:ss", "UTC"));
        generatedPlan.setProvider(llmClient.getProviderName());
        generatedPlan.setModel(AiConfig.getModel());

        // 3. Save to cache
        planCache.put(promptHash, generatedPlan);
        return generatedPlan;
    }

    private TestPlan generatePlanWithRetry(PromptFile promptFile, String snapshot) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(promptFile, snapshot);

        logger.info("Generating execution plan via LLM [{}] for '{}'", llmClient.getProviderName(), promptFile.getName());
        String rawResponse = llmClient.generatePlan(systemPrompt, userPrompt);
        TestPlan plan = parsePlanJson(rawResponse);

        PlanValidator.ValidationResult valResult = PlanValidator.validate(plan);
        if (valResult.isValid()) {
            return plan;
        }

        logger.warn("Initial plan validation failed for '{}': {}. Retrying once with error feedback...",
                promptFile.getName(), valResult.getErrorMessage());

        // Retry once with error feedback
        String errorFeedbackPrompt = userPrompt + "\n\nCRITICAL FIX NEEDED: The previous plan was rejected with errors:\n"
                + valResult.getErrorMessage() + "\nPlease fix these errors and return valid JSON conforming to the schema and closed vocabulary.";

        String retryResponse = llmClient.generatePlan(systemPrompt, errorFeedbackPrompt);
        TestPlan retryPlan = parsePlanJson(retryResponse);

        PlanValidator.ValidationResult retryResult = PlanValidator.validate(retryPlan);
        if (!retryResult.isValid()) {
            throw new IllegalStateException("Failed to generate a valid plan for prompt '" + promptFile.getName()
                    + "' after retry. Errors: " + retryResult.getErrorMessage());
        }

        return retryPlan;
    }

    private TestPlan parsePlanJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("LLM returned empty or null plan response");
        }
        try {
            JsonNode root = JsonUtils.parseJsonNode(rawJson);
            if (root.has("steps") && root.get("steps").isArray()) {
                return JsonUtils.fromJson(rawJson, TestPlan.class);
            } else if (root.isArray()) {
                List<PlanStep> stepList = new ArrayList<>();
                for (JsonNode sn : root) {
                    stepList.add(JsonUtils.fromJson(sn.toString(), PlanStep.class));
                }
                return new TestPlan(stepList);
            }
            throw new IllegalArgumentException("Unexpected plan JSON format (must be object with 'steps' array or array of steps)");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse plan JSON: " + e.getMessage() + "\nRaw response:\n" + rawJson, e);
        }
    }

    private String buildSystemPrompt() {
        return """
                You are an expert test automation architect that converts plain-English test descriptions into a deterministic JSON execution plan.
                
                RULES:
                1. Output strictly valid JSON matching the schema below. No Markdown wrapping, no conversational text.
                2. Use ONLY actions from the CLOSED ACTION VOCABULARY. Reject unknown actions.
                3. If a step line is ambiguous or cannot be confidently mapped, emit:
                   {"id": <n>, "action": "UNRESOLVED", "args": {"reason": "<explanation>"}, "source": "<line>", "critical": true}
                   DO NOT GUESS.
                4. NEVER invent URLs, credentials, selectors, or expected values not present in the prompt file or page snapshot.
                5. Use ${VARIABLE_NAME} placeholders for all credentials and dynamic data. Never resolve secret values.
                6. UI Targets must use semantic attributes in priority: role -> label -> placeholder -> text -> testId -> css (css only if explicitly provided).
                   Example target: {"role": "button", "name": "Login"} or {"label": "Email"} or {"placeholder": "Search"} or {"text": "Submit"}.
                
                CLOSED ACTION VOCABULARY:
                - UI Actions: NAVIGATE{url}, CLICK{target}, FILL{target, value}, SELECT{target, option/value/label}, CHECK{target}, UNCHECK{target}, PRESS{key}, HOVER{target}, WAIT_FOR{target/text/url, state}, UPLOAD{target, file}, SWITCH_TAB{index/title}, STORE_TEXT{target, as}, SCREENSHOT{name}
                - UI Verifications: VERIFY_VISIBLE{target}, VERIFY_HIDDEN{target}, VERIFY_TEXT{target, expected, match=exact|contains|regex}, VERIFY_URL{expected, match}, VERIFY_TITLE{expected}, VERIFY_COUNT{target, expected, op}, VERIFY_TABLE{target, columns[], rows?}, VERIFY_ENABLED{target}, VERIFY_DISABLED{target}, VERIFY_COMPONENTS{items[{name, role, text?}]}
                - API Actions: API_BASE{url}, API_REQUEST{method, path, headers, query, body}, API_STORE{jsonPath/header, as}
                - API Verifications: VERIFY_STATUS{expected}, VERIFY_JSON{jsonPath, expected, op}, VERIFY_FIELD_EXISTS{jsonPath}, VERIFY_EVERY_ITEM_HAS{arrayPath, fields[]}, VERIFY_HEADER{name, expected}, VERIFY_RESPONSE_TIME{maxMs}, VERIFY_SCHEMA{schemaFile}
                - Data: GENERATE_DATA{type, count, as}, SET_VAR{name, value}
                
                JSON OUTPUT SCHEMA:
                {
                  "version": "1.0",
                  "steps": [
                    {
                      "id": 1,
                      "action": "ACTION_NAME",
                      "args": { ... },
                      "source": "exact original step line",
                      "critical": true
                    }
                  ]
                }
                """;
    }

    private String buildUserPrompt(PromptFile promptFile, String snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("Test file: ").append(promptFile.getName()).append("\n");
        sb.append("Test name: ").append(promptFile.getTestName()).append("\n");
        sb.append("Test type: ").append(promptFile.getType()).append("\n");
        sb.append("Base URL: ").append(ConfigManager.get("baseUrl", "")).append("\n");
        sb.append("Variables available: ").append(promptFile.getVariableNames()).append("\n");
        sb.append("Mode: ").append(promptFile.getMode()).append("\n");

        if (snapshot != null && !snapshot.isBlank()) {
            sb.append("\nCURRENT PAGE ARIA SNAPSHOT GROUNDING:\n");
            sb.append(snapshot).append("\n");
        }

        sb.append("\nSTEP LINES TO PLAN:\n");
        for (int i = 0; i < promptFile.getRawStepLines().size(); i++) {
            sb.append(i + 1).append(". ").append(promptFile.getRawStepLines().get(i)).append("\n");
        }

        return sb.toString();
    }
}
