/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Programmatic TestNG test runner enabling dynamic suite construction and execution from code or config.
 * Created: 2026-08-15
 */
package framework.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.TestNG;

import java.util.ArrayList;
import java.util.List;

public class TestNGExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TestNGExecutor.class);

    public static void executeSuites(List<String> suiteXmlPaths) {
        logger.info("Executing TestNG Suites: {}", suiteXmlPaths);
        TestNG testng = new TestNG();
        testng.setTestSuites(suiteXmlPaths);
        testng.run();
    }

    public static void executeSuite(String suiteXmlPath) {
        List<String> suites = new ArrayList<>();
        suites.add(suiteXmlPath);
        executeSuites(suites);
    }
}
