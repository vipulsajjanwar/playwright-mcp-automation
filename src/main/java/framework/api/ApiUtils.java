package framework.api;

import io.restassured.response.Response;

public class ApiUtils {

    private ApiUtils() {}

    public static <T> T extractPojo(Response response, Class<T> clazz) {
        return response.as(clazz);
    }

    public static <T> T extractJsonPath(Response response, String jsonPath) {
        return response.jsonPath().get(jsonPath);
    }
}
