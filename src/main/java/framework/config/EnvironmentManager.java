/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Manages active test environment selection, resolution, and switching based on system properties and config files.
 * Created: 2026-08-15
 */
package framework.config;

import framework.constants.FrameworkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentManager {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentManager.class);

    public static String getActiveEnvironment() {
        String env = System.getProperty("env");
        if (env == null || env.trim().isEmpty()) {
            env = System.getenv("ENV");
        }
        if (env == null || env.trim().isEmpty()) {
            env = FrameworkConstants.DEFAULT_ENV;
        }
        logger.info("Active execution environment: {}", env);
        return env.toLowerCase().trim();
    }
}
