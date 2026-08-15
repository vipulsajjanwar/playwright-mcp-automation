package framework.ai;

import framework.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AiTestGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AiTestGenerator.class);

    private AiTestGenerator() {}

    public static String generateTestSkeleton(String requirement) {
        logger.info("AI Test Skeleton Generator received requirement: {}", requirement);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requirement", requirement);
        result.put("scenarios", Arrays.asList(
                "Positive Case: Successfully create item with valid inputs",
                "Negative Case: Attempt creation with empty mandatory fields",
                "Boundary Case: Attempt creation with maximum allowed character length"
        ));

        result.put("pageObjectSkeleton",
                "public class GeneratedFeaturePage extends BasePage {\n" +
                "    private final Locator inputField;\n" +
                "    private final Locator submitBtn;\n\n" +
                "    public GeneratedFeaturePage(Page page) {\n" +
                "        super(page);\n" +
                "        this.inputField = page.getByLabel(\"Item Name\");\n" +
                "        this.submitBtn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(\"Submit\"));\n" +
                "    }\n" +
                "}"
        );

        result.put("testngTestSkeleton",
                "public class GeneratedFeatureTest extends BaseTest {\n" +
                "    @Test\n" +
                "    public void testFeatureCreation() {\n" +
                "        GeneratedFeaturePage pageObj = new GeneratedFeaturePage(page());\n" +
                "        // Add test logic\n" +
                "    }\n" +
                "}"
        );

        String json = JsonUtils.toJson(result);
        logger.info("AI Test Skeleton Generated:\n{}", json);
        return json;
    }
}
