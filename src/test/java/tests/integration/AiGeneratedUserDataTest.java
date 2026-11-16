/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Integration test validating AI-generated test data quality and format for use in user management workflows.
 * Created: 2026-08-15
 */
package tests.integration;

import framework.ai.AiTestDataGenerator;
import framework.base.BaseTest;
import framework.pages.UserManagementPage;
import framework.reporting.TestExecutionLogger;
import framework.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AiGeneratedUserDataTest extends BaseTest {

    @Test(groups = {"integration", "ai"}, description = "Create user using AI generated test data")
    public void testUserCreationWithAiData() {
        TestExecutionLogger.logStep("Requesting 3 AI generated user data records");
        String json = AiTestDataGenerator.generateUserDataJson(3);
        JsonNode users = JsonUtils.parseJsonNode(json);

        Assert.assertTrue(users.isArray() && users.size() == 3, "AI should return 3 user records");

        JsonNode user1 = users.get(0);
        TestExecutionLogger.logStep("Filling user form with AI user: " + user1.get("email").asText());

        UserManagementPage userPage = new UserManagementPage(page());
        userPage.createUser(
                user1.get("firstName").asText(),
                user1.get("lastName").asText(),
                user1.get("email").asText(),
                user1.get("phone").asText(),
                user1.get("role").asText()
        );
    }
}
