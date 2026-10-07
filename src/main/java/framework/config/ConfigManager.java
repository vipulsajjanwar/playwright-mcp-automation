package framework.config;

import framework.constants.FrameworkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Properties defaultConfigProps;
    private static final Properties envConfigProps;

    static {
        defaultConfigProps = PropertiesReader.loadProperties(FrameworkConstants.DEFAULT_CONFIG_PATH);
        String env = EnvironmentManager.getActiveEnvironment();
        String envConfigPath = FrameworkConstants.CONFIG_DIR + env + ".properties";
        envConfigProps = PropertiesReader.loadProperties(envConfigPath);
    }

    private ConfigManager() {}

    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String defaultValue) {
        // 1. System property
        String val = System.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 2. Environment variable
        val = System.getenv(key);
        if (val == null) {
            val = System.getenv(key.toUpperCase().replace('.', '_'));
        }
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 3. Environment specific properties file
        val = envConfigProps.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        // 4. Default property
        val = defaultConfigProps.getProperty(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        // 5. EnvConfig (from .env files)
        val = EnvConfig.get(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }

        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer config for key {}: {}. Defaulting to {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }
}
