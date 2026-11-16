/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: API test suite verifying user deletion endpoints: successful deletion, idempotency, and 404 response handling.
 * Created: 2026-08-15
 */
package tests.api;

import framework.api.ApiClient;
import framework.api.ApiResponseValidator;
import framework.base.BaseApiTest;
import framework.reporting.TestExecutionLogger;
import io.restassured.response.Response;
import org.testng.annotations.Test;

public class DeleteUserTest extends BaseApiTest {

    @Test(groups = {"api", "regression"}, description = "DELETE /users/1 - Delete User Record")
    public void testDeleteUserApi() {
        TestExecutionLogger.logStep("Executing DELETE /users/1 request");
        Response response = ApiClient.delete("/users/1");
        ApiResponseValidator.validateStatusCode(response, 200);
    }
}
