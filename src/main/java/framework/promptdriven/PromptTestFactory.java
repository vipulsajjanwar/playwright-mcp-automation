/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Factory that dynamically constructs TestNG ITestNGMethod instances from discovered prompt test definitions.
 * Created: 2026-10-22
 */
package framework.promptdriven;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Factory;

import java.util.List;

/**
 * TestNG Factory dynamically discovering all prompt files and instantiating
 * an isolated {@link PromptTest} per file for sequential or parallel execution.
 */
public class PromptTestFactory {

    private static final Logger logger = LoggerFactory.getLogger(PromptTestFactory.class);

    @Factory
    public Object[] createInstances() {
        List<PromptFile> promptFiles = PromptDiscovery.discover();
        logger.info("PromptTestFactory discovered {} prompt file(s) for execution.", promptFiles.size());

        if (promptFiles.isEmpty()) {
            logger.warn("No prompt files matched discovery criteria. Factory returning 0 test instances.");
            return new Object[0];
        }

        return promptFiles.stream()
                .map(PromptTest::new)
                .toArray();
    }
}
