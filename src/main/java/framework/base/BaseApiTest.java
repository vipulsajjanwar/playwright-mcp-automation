/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Abstract base class for API tests, handling RestAssured configuration, auth token lifecycle, and shared request specifications.
 * Created: 2026-08-15
 */
package framework.base;

import framework.config.ConfigManager;
import io.restassured.RestAssured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;

public abstract class BaseApiTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeClass(alwaysRun = true)
    public void setupApiConfig() {
        String baseUrl = ConfigManager.get("api.baseUrl", "https://reqres.in/api");
        logger.info("Initializing API Test Suite with Base URL: {}", baseUrl);
        RestAssured.baseURI = baseUrl;
    }
}
