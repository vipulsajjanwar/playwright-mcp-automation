package tests.api;

import framework.api.ApiClient;
import framework.api.ApiResponseValidator;
import framework.base.BaseApiTest;
import framework.reporting.TestExecutionLogger;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

public class CreateUserTest extends BaseApiTest {

    @Test(groups = {"api", "regression"}, description = "POST /users - Create User Record")
    public void testCreateUserApi() {
        TestExecutionLogger.logStep("Executing POST /users request");
        Map<String, String> payload = Map.of("name", "morpheus", "job", "leader");
        Response response = ApiClient.post("/users", payload);

        ApiResponseValidator.validateStatusCode(response, 201);
        ApiResponseValidator.validateJsonPathValue(response, "name", "morpheus");
        ApiResponseValidator.validateJsonPathValue(response, "job", "leader");
    }
}
