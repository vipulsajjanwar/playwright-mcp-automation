/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Coordinates test suite execution, thread pool management, and pre/post-execution hooks for the framework.
 * Created: 2026-08-15
 */
package framework.execution;

import framework.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionManager {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionManager.class);

    public static void main(String[] args) {
        String suiteName = ConfigManager.get("suite", "smoke");
        String suitePath = "src/test/resources/testng/" + suiteName + ".xml";
        logger.info("Starting execution for suiteName: {} [Path: {}]", suiteName, suitePath);
        TestNGExecutor.executeSuite(suitePath);
    }
}
