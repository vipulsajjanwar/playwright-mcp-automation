package framework.api;

import framework.exceptions.ApiException;
import io.restassured.response.Response;
import org.testng.Assert;

public class ApiResponseValidator {

    private ApiResponseValidator() {}

    public static void validateStatusCode(Response response, int expectedStatusCode) {
        if (response.getStatusCode() != expectedStatusCode) {
            throw new ApiException(String.format("Expected status code %d, but got %d. Body: %s",
                    expectedStatusCode, response.getStatusCode(), response.getBody().asString()));
        }
    }

    public static void validateResponseTime(Response response, long maxTimeMs) {
        if (response.getTime() > maxTimeMs) {
            throw new ApiException(String.format("Response time %d ms exceeded max allowed time %d ms",
                    response.getTime(), maxTimeMs));
        }
    }

    public static void validateJsonPathValue(Response response, String jsonPath, Object expectedValue) {
        Object actualValue = response.jsonPath().get(jsonPath);
        Assert.assertEquals(actualValue, expectedValue, "JSONPath mismatch for: " + jsonPath);
    }
}
