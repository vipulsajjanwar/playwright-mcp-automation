/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Composite meta-annotation aggregating TestNG @Test configuration and custom framework metadata for a unified test declaration.
 * Created: 2026-10-01
 */
package framework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Central custom test annotation for test metadata, reporting, logging,
 * execution tracking, and observability integration across TestNG, Extent,
 * Allure, ChainTest, ELK, Database, and CI/CD pipelines.
 *
 * <p>Can be applied at both class level (for common suite/module metadata)
 * and method level (for test-specific metadata). Method-level attributes
 * override class-level defaults.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface CustomTest {

    /**
     * Unique identifier for the test case (e.g., "ADMIN_USER_001", "TC_LOGIN_05").
     */
    String testId() default "";

    /**
     * Human-readable name of the test. If empty, falls back to method name or description.
     */
    String name() default "";

    /**
     * Comprehensive description of what the test verifies.
     */
    String description() default "";

    /**
     * High-level functional module (e.g., "Administration", "Billing", "Authentication").
     */
    String module() default "";

    /**
     * Functional feature under test (e.g., "Users", "Roles", "SSO Login").
     */
    String feature() default "";

    /**
     * Sub-feature or granular component (e.g., "User Profile", "Password Policy").
     */
    String subFeature() default "";

    /**
     * Type of test (e.g., "UI", "API", "DATABASE", "INTEGRATION", "E2E").
     */
    String testType() default "UI";

    /**
     * Test execution priority: "LOW", "MEDIUM", "HIGH", "CRITICAL".
     */
    String priority() default "MEDIUM";

    /**
     * Defect or business severity: "BLOCKER", "CRITICAL", "NORMAL", "MINOR", "TRIVIAL".
     */
    String severity() default "NORMAL";

    /**
     * Test classification category (e.g., "Regression", "Smoke", "Sanity", "Security").
     */
    String category() default "Regression";

    /**
     * Free-form tags and labels for grouping and filtering.
     */
    String[] tags() default {};

    /**
     * Team or organizational owner (e.g., "Core QA", "SecOps", "Identity Team").
     */
    String owner() default "";

    /**
     * Primary test author/engineer name.
     */
    String author() default "";

    /**
     * Business requirement ID (e.g., "REQ-8941").
     */
    String requirementId() default "";

    /**
     * Jira issue / ticket ID (e.g., "ICEDQ-12345").
     */
    String jiraId() default "";

    /**
     * User story / Agile story identifier (e.g., "STORY-567").
     */
    String storyId() default "";

    /**
     * Target environment override (e.g., "qa", "staging", "prod").
     */
    String environment() default "";

    /**
     * Target browser requirement override (e.g., "chromium", "firefox", "webkit").
     */
    String browser() default "";

    /**
     * Target OS / platform requirement (e.g., "WINDOWS", "LINUX", "MAC").
     */
    String platform() default "";

    /**
     * Backend service under test (e.g., "user-service", "auth-service").
     */
    String service() default "";

    /**
     * Frontend or backend architectural component.
     */
    String component() default "";

    /**
     * Test data set or variant identifier (e.g., "AdminUser_DataVariant_1").
     */
    String dataSet() default "";

    /**
     * Execution mode / type (e.g., "AUTOMATED", "MANUAL", "HYBRID").
     */
    String executionType() default "AUTOMATED";

    /**
     * Whether the test is active and enabled for execution.
     */
    boolean enabled() default true;
}
