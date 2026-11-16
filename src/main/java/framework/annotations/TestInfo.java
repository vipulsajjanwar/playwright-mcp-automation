/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Annotation interface for attaching descriptive test metadata such as ID, name, severity, and feature tags to test methods.
 * Created: 2026-10-01
 */
package framework.annotations;

public @interface TestInfo {

    /** Unique test case id, e.g. "TC-LOGIN-001". Method-level only. */
    String id() default "";

    String author() default "";

    String description() default "";

    Severity severity() default Severity.UNSET;

    /** Feature / module under test, e.g. "Login". */
    String module() default "";

    /** Jira / requirement / defect links or keys. */
    String[] links() default {};

    /** Free-form labels. Class-level and method-level tags are merged. */
    String[] tags() default {};

    enum Severity { UNSET, BLOCKER, CRITICAL, NORMAL, MINOR, TRIVIAL }
}
