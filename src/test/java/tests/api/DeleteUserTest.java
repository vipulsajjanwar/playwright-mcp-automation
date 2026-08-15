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
