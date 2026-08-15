package tests.api;

import framework.api.ApiClient;
import framework.api.ApiResponseValidator;
import framework.base.BaseApiTest;
import framework.reporting.TestExecutionLogger;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

public class UpdateUserTest extends BaseApiTest {

    @Test(groups = {"api", "regression"}, description = "PUT /users/1 - Update User Record")
    public void testUpdateUserApi() {
        TestExecutionLogger.logStep("Executing PUT /users/1 request");
        Map<String, String> payload = Map.of("name", "morpheus", "job", "zion resident");
        Response response = ApiClient.put("/users/1", payload);

        ApiResponseValidator.validateStatusCode(response, 200);
        ApiResponseValidator.validateJsonPathValue(response, "job", "zion resident");
    }
}
