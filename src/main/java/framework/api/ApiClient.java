package framework.api;

import framework.exceptions.ApiException;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);

    private ApiClient() {}

    public static Response get(String endpoint) {
        return get(new ApiRequestBuilder().build(), endpoint);
    }

    public static Response get(RequestSpecification spec, String endpoint) {
        try {
            logger.info("Executing GET request to: {}", endpoint);
            Response response = spec.get(endpoint);
            logger.debug("GET Response status: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to execute GET request: " + endpoint, e);
        }
    }

    public static Response post(String endpoint, Object body) {
        return post(new ApiRequestBuilder().body(body).build(), endpoint);
    }

    public static Response post(RequestSpecification spec, String endpoint) {
        try {
            logger.info("Executing POST request to: {}", endpoint);
            Response response = spec.post(endpoint);
            logger.debug("POST Response status: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to execute POST request: " + endpoint, e);
        }
    }

    public static Response put(String endpoint, Object body) {
        return put(new ApiRequestBuilder().body(body).build(), endpoint);
    }

    public static Response put(RequestSpecification spec, String endpoint) {
        try {
            logger.info("Executing PUT request to: {}", endpoint);
            Response response = spec.put(endpoint);
            logger.debug("PUT Response status: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to execute PUT request: " + endpoint, e);
        }
    }

    public static Response delete(String endpoint) {
        return delete(new ApiRequestBuilder().build(), endpoint);
    }

    public static Response delete(RequestSpecification spec, String endpoint) {
        try {
            logger.info("Executing DELETE request to: {}", endpoint);
            Response response = spec.delete(endpoint);
            logger.debug("DELETE Response status: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            throw new ApiException("Failed to execute DELETE request: " + endpoint, e);
        }
    }
}
