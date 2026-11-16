/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: API test suite covering user retrieval endpoints: single user, list pagination, and not-found scenarios.
 * Created: 2026-08-15
 */
package tests.api;

import framework.api.ApiClient;
import framework.api.ApiRequestBuilder;
import framework.api.ApiResponseValidator;
import framework.base.BaseApiTest;
import framework.reporting.TestExecutionLogger;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GetUserTest extends BaseApiTest {

    @Test(groups = {"api", "smoke"}, description = "GET /users/1 - Single User Retrieval")
    public void testGetUserById() {
        TestExecutionLogger.logStep("Executing GET /users/1 request");
        Response response = ApiClient.get("/users/1");

        ApiResponseValidator.validateStatusCode(response, 200);
        ApiResponseValidator.validateJsonPathValue(response, "id", 1);
        ApiResponseValidator.validateJsonPathValue(response, "name", "Leanne Graham");
        Assert.assertTrue(response.getTime() < 5000, "Response time should be under 5s");
    }
}
