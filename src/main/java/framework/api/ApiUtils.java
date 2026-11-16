/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Helper utilities for common API testing tasks: JSON parsing, response extraction, request logging, and payload construction.
 * Created: 2026-08-15
 */
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
