package framework.ai.llm;

import framework.ai.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating configured LLM provider clients.
 */
public final class LlmClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(LlmClientFactory.class);

    private LlmClientFactory() {}

    public static LlmClient getClient() {
        return getClient(AiConfig.getProvider());
    }

    public static LlmClient getClient(String providerName) {
        String provider = (providerName != null && !providerName.isBlank())
                ? providerName.trim().toLowerCase()
                : "mock";

        logger.info("Initializing LlmClient for provider: {}", provider);

        return switch (provider) {
            case "anthropic" -> new AnthropicLlmClient();
            case "openai" -> new OpenAiCompatLlmClient();
            case "mock" -> new MockLlmClient();
            default -> {
                logger.warn("Unknown AI provider '{}'. Falling back to MockLlmClient.", provider);
                yield new MockLlmClient();
            }
        };
    }
}
