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
