package framework.promptdriven;

import framework.ai.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

import java.util.List;

/**
 * TestNG listener dynamically adjusting suite parallel execution mode and thread count
 * based on -Dprompts.threads / ai configuration.
 */
public class PromptSuiteListener implements IAlterSuiteListener {

    private static final Logger logger = LoggerFactory.getLogger(PromptSuiteListener.class);

    @Override
    public void alter(List<XmlSuite> suites) {
        int threads = AiConfig.getPromptsThreads();
        for (XmlSuite suite : suites) {
            if ("PromptDrivenAutomationSuite".equalsIgnoreCase(suite.getName()) || suite.getName().contains("Prompt")) {
                logger.info("Configuring Prompt Driven Suite '{}' with thread count: {}", suite.getName(), threads);
                if (threads > 1) {
                    suite.setParallel(XmlSuite.ParallelMode.INSTANCES);
                    suite.setThreadCount(threads);
                } else {
                    suite.setParallel(XmlSuite.ParallelMode.NONE);
                    suite.setThreadCount(1);
                }
            }
        }
    }
}
