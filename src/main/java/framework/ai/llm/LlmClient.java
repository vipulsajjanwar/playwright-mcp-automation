package framework.ai.llm;

/**
 * Common interface for LLM providers used for plan generation.
 */
public interface LlmClient {

    /**
     * Sends a system prompt and user input to generate a JSON execution plan.
     *
     * @param systemPrompt Instructions, JSON schema, and action vocabulary.
     * @param userPrompt The prompt file content, grounding accessibility snapshot, and test context.
     * @return The raw JSON string returned by the LLM.
     */
    String generatePlan(String systemPrompt, String userPrompt);

    /**
     * Returns the provider identifier (e.g. anthropic, openai, mock).
     */
    String getProviderName();
}
