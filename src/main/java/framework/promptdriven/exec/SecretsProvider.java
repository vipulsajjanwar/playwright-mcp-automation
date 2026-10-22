package framework.promptdriven.exec;

import framework.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages secret resolution and masking.
 * Never leaks literal secret values to logs, traces, reports, or LLM prompts.
 */
public class SecretsProvider {

    private static final Logger logger = LoggerFactory.getLogger(SecretsProvider.class);
    private final Properties secretsProperties = new Properties();
    private final Set<String> secretValues = ConcurrentHashMap.newKeySet();

    public SecretsProvider() {
        loadSecrets();
    }

    private void loadSecrets() {
        List<String> candidatePaths = List.of("secrets.properties", "src/test/resources/config/secrets.properties");
        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    secretsProperties.load(fis);
                    for (String key : secretsProperties.stringPropertyNames()) {
                        String val = secretsProperties.getProperty(key);
                        if (val != null && val.length() >= 3) {
                            secretValues.add(val.trim());
                        }
                    }
                    logger.debug("Loaded secrets from {}", path);
                    break;
                } catch (IOException e) {
                    logger.warn("Could not read secrets from {}: {}", path, e.getMessage());
                }
            }
        }
    }

    public String resolveSecret(String name) {
        if (name == null || name.isBlank()) return "";

        // 1. Environment variable
        String envVal = System.getenv(name);
        if (envVal == null) {
            envVal = System.getenv(name.toUpperCase(Locale.ROOT).replace('.', '_'));
        }
        if (envVal != null && !envVal.isBlank()) {
            registerSecretValue(envVal);
            return envVal.trim();
        }

        // 2. secrets.properties
        if (secretsProperties.containsKey(name)) {
            String val = secretsProperties.getProperty(name);
            if (val != null && !val.isBlank()) {
                registerSecretValue(val);
                return val.trim();
            }
        }

        // 3. System property
        String sysVal = System.getProperty(name);
        if (sysVal != null && !sysVal.isBlank()) {
            registerSecretValue(sysVal);
            return sysVal.trim();
        }

        // 4. ConfigManager fallback
        String confVal = ConfigManager.get(name);
        if (confVal != null && !confVal.isBlank()) {
            registerSecretValue(confVal);
            return confVal.trim();
        }

        return "";
    }

    public void registerSecretValue(String val) {
        if (val != null && val.length() >= 3) {
            secretValues.add(val.trim());
        }
    }

    public String mask(String input) {
        if (input == null || input.isEmpty()) return input;
        String result = input;
        for (String secret : secretValues) {
            if (secret != null && !secret.isBlank() && result.contains(secret)) {
                result = result.replace(secret, "********");
            }
        }
        return result;
    }

    public Set<String> getRegisteredSecrets() {
        return Collections.unmodifiableSet(secretValues);
    }
}
