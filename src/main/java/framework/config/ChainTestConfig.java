/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Configuration class for ChainTest reporting integration, supplying project credentials, run identifiers, and dashboard URLs.
 * Created: 2026-09-29
 */
package framework.config;

import framework.constants.FrameworkConstants;

import java.io.InputStream;
import java.util.Properties;

public class ChainTestConfig {

    private static final Properties PROPERTIES ;

    static {
        PROPERTIES = PropertiesReader.loadProperties(FrameworkConstants.CHAINTEST_CONFIG_PATH);
    }

    private ChainTestConfig() {
    }

    public static String get(String key, String defaultValue) {

        return PROPERTIES.getProperty(
                key,
                defaultValue
        );
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
