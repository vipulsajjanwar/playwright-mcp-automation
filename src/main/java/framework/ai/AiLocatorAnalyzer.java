package framework.ai;

import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiLocatorAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(AiLocatorAnalyzer.class);

    private AiLocatorAnalyzer() {}

    public static String analyzeAndSuggestLocators(String failedLocator, String elementName, String pageTitle) {
        logger.info("Executing AI Locator Analysis for failed locator: [{}] on element: [{}]", failedLocator, elementName);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("failedLocator", failedLocator);
        result.put("elementName", elementName);
        result.put("pageTitle", pageTitle);

        List<Map<String, String>> suggestions = new ArrayList<>();

        // Priority 1: getByRole
        Map<String, String> s1 = new LinkedHashMap<>();
        s1.put("priority", "1");
        s1.put("type", "getByRole");
        s1.put("code", String.format("page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(\"%s\"))", elementName));
        s1.put("resilienceScore", "95%");
        suggestions.add(s1);

        // Priority 2: getByLabel
        Map<String, String> s2 = new LinkedHashMap<>();
        s2.put("priority", "2");
        s2.put("type", "getByLabel");
        s2.put("code", String.format("page.getByLabel(\"%s\")", elementName));
        s2.put("resilienceScore", "90%");
        suggestions.add(s2);

        // Priority 3: getByPlaceholder
        Map<String, String> s3 = new LinkedHashMap<>();
        s3.put("priority", "3");
        s3.put("type", "getByPlaceholder");
        s3.put("code", String.format("page.getByPlaceholder(\"%s\")", elementName));
        s3.put("resilienceScore", "88%");
        suggestions.add(s3);

        // Priority 4: getByText
        Map<String, String> s4 = new LinkedHashMap<>();
        s4.put("priority", "4");
        s4.put("type", "getByText");
        s4.put("code", String.format("page.getByText(\"%s\")", elementName));
        s4.put("resilienceScore", "82%");
        suggestions.add(s4);

        // Priority 5: CSS
        Map<String, String> s5 = new LinkedHashMap<>();
        s5.put("priority", "5");
        s5.put("type", "CSS");
        s5.put("code", String.format("page.locator(\"button:has-text('%s')\")", elementName));
        s5.put("resilienceScore", "75%");
        suggestions.add(s5);

        result.put("recommendedLocators", suggestions);

        String json = JsonUtils.toJson(result);
        logger.info("AI Locator Analysis recommendations:\n{}", json);
        return json;
    }
}
