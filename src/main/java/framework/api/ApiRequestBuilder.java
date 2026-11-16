/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Fluent builder for constructing REST API requests with headers, query parameters, path variables, and request bodies.
 * Created: 2026-08-15
 */
package framework.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

public class ApiRequestBuilder {

    private final RequestSpecification requestSpec;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Object> queryParams = new HashMap<>();
    private final Map<String, Object> pathParams = new HashMap<>();

    public ApiRequestBuilder() {
        this.requestSpec = RestAssured.given().contentType(ContentType.JSON);
    }

    public ApiRequestBuilder header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public ApiRequestBuilder bearerToken(String token) {
        headers.put("Authorization", "Bearer " + token);
        return this;
    }

    public ApiRequestBuilder queryParam(String key, Object value) {
        queryParams.put(key, value);
        return this;
    }

    public ApiRequestBuilder pathParam(String key, Object value) {
        pathParams.put(key, value);
        return this;
    }

    public ApiRequestBuilder body(Object body) {
        requestSpec.body(body);
        return this;
    }

    public RequestSpecification build() {
        if (!headers.isEmpty()) requestSpec.headers(headers);
        if (!queryParams.isEmpty()) requestSpec.queryParams(queryParams);
        if (!pathParams.isEmpty()) requestSpec.pathParams(pathParams);
        return requestSpec;
    }
}
