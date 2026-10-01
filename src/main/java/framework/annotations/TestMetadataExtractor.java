package framework.annotations;

import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Extracts and merges test metadata from {@link CustomTest}, legacy {@link TestInfo},
 * and TestNG {@link Test} annotations.
 */
public final class TestMetadataExtractor {

    private TestMetadataExtractor() {}

    public static TestMetadata extract(ITestResult result) {
        if (result == null) {
            return TestMetadata.builder().build();
        }
        ITestNGMethod testNGMethod = result.getMethod();
        Method method = testNGMethod.getConstructorOrMethod().getMethod();
        Class<?> clazz = testNGMethod.getRealClass();

        return extract(method, clazz, testNGMethod);
    }

    public static TestMetadata extract(Method method, Class<?> clazz, ITestNGMethod testNGMethod) {
        TestMetadata.Builder builder = TestMetadata.builder();

        CustomTest methodAnnotation = method != null ? method.getAnnotation(CustomTest.class) : null;
        CustomTest classAnnotation = clazz != null ? clazz.getAnnotation(CustomTest.class) : null;

        TestInfo legacyMethodInfo = method != null ? method.getAnnotation(TestInfo.class) : null;
        TestInfo legacyClassInfo = clazz != null ? clazz.getAnnotation(TestInfo.class) : null;

        Test testngAnnotation = method != null ? method.getAnnotation(Test.class) : null;

        boolean isAnnotated = methodAnnotation != null || classAnnotation != null
                || legacyMethodInfo != null || legacyClassInfo != null;
        builder.annotated(isAnnotated);

        // Class-level CustomTest defaults
        if (classAnnotation != null) {
            applyCustomTest(builder, classAnnotation);
        }

        // Method-level CustomTest overrides
        if (methodAnnotation != null) {
            applyCustomTest(builder, methodAnnotation);
        }

        // Legacy TestInfo fallbacks if CustomTest was not specified for these fields
        if (legacyClassInfo != null) {
            applyLegacyTestInfo(builder, legacyClassInfo);
        }
        if (legacyMethodInfo != null) {
            applyLegacyTestInfo(builder, legacyMethodInfo);
        }

        // TestNG groups and description fallbacks
        if (testNGMethod != null) {
            if (testNGMethod.getGroups() != null && testNGMethod.getGroups().length > 0) {
                builder.addTags(testNGMethod.getGroups());
            }
            if (builder.build().getDescription().isBlank() && testNGMethod.getDescription() != null) {
                builder.description(testNGMethod.getDescription());
            }
        } else if (testngAnnotation != null) {
            if (testngAnnotation.groups().length > 0) {
                builder.addTags(testngAnnotation.groups());
            }
            if (builder.build().getDescription().isBlank() && !testngAnnotation.description().isBlank()) {
                builder.description(testngAnnotation.description());
            }
        }

        TestMetadata current = builder.build();

        // Fallbacks for testId, name, module, feature
        String methodName = method != null ? method.getName() : "testMethod";
        String className = clazz != null ? clazz.getSimpleName() : "TestClass";

        if (current.getTestId().isBlank()) {
            builder.testId(className + "#" + methodName);
        }
        if (current.getName().isBlank()) {
            builder.name(!current.getDescription().isBlank() ? current.getDescription() : methodName);
        }
        if (current.getDescription().isBlank()) {
            builder.description("Test execution for " + methodName);
        }
        if (current.getModule().isBlank() || "General".equals(current.getModule())) {
            builder.module(className.replaceAll("Test$", ""));
        }
        if (current.getFeature().isBlank() || "General".equals(current.getFeature())) {
            builder.feature(methodName);
        }

        return builder.build();
    }

    private static void applyCustomTest(TestMetadata.Builder builder, CustomTest annotation) {
        if (!annotation.testId().isBlank()) builder.testId(annotation.testId());
        if (!annotation.name().isBlank()) builder.name(annotation.name());
        if (!annotation.description().isBlank()) builder.description(annotation.description());
        if (!annotation.module().isBlank()) builder.module(annotation.module());
        if (!annotation.feature().isBlank()) builder.feature(annotation.feature());
        if (!annotation.subFeature().isBlank()) builder.subFeature(annotation.subFeature());
        if (!annotation.testType().isBlank()) builder.testType(annotation.testType());
        if (!annotation.priority().isBlank()) builder.priority(annotation.priority());
        if (!annotation.severity().isBlank()) builder.severity(annotation.severity());
        if (!annotation.category().isBlank()) builder.category(annotation.category());
        if (annotation.tags().length > 0) builder.addTags(annotation.tags());
        if (!annotation.owner().isBlank()) builder.owner(annotation.owner());
        if (!annotation.author().isBlank()) builder.author(annotation.author());
        if (!annotation.requirementId().isBlank()) builder.requirementId(annotation.requirementId());
        if (!annotation.jiraId().isBlank()) builder.jiraId(annotation.jiraId());
        if (!annotation.storyId().isBlank()) builder.storyId(annotation.storyId());
        if (!annotation.environment().isBlank()) builder.environment(annotation.environment());
        if (!annotation.browser().isBlank()) builder.browser(annotation.browser());
        if (!annotation.platform().isBlank()) builder.platform(annotation.platform());
        if (!annotation.service().isBlank()) builder.service(annotation.service());
        if (!annotation.component().isBlank()) builder.component(annotation.component());
        if (!annotation.dataSet().isBlank()) builder.dataSet(annotation.dataSet());
        if (!annotation.executionType().isBlank()) builder.executionType(annotation.executionType());
        builder.enabled(annotation.enabled());
    }

    private static void applyLegacyTestInfo(TestMetadata.Builder builder, TestInfo legacy) {
        if (!legacy.id().isBlank() && builder.build().getTestId().isBlank()) {
            builder.testId(legacy.id());
        }
        if (!legacy.author().isBlank() && builder.build().getAuthor().isBlank()) {
            builder.author(legacy.author());
        }
        if (!legacy.description().isBlank() && builder.build().getDescription().isBlank()) {
            builder.description(legacy.description());
        }
        if (!legacy.module().isBlank() && builder.build().getModule().isBlank()) {
            builder.module(legacy.module());
        }
        if (legacy.severity() != TestInfo.Severity.UNSET && "NORMAL".equals(builder.build().getSeverity())) {
            builder.severity(legacy.severity().name());
        }
        if (legacy.tags().length > 0) {
            builder.addTags(legacy.tags());
        }
        if (legacy.links().length > 0) {
            for (String link : legacy.links()) {
                if (builder.build().getJiraId().isBlank()) {
                    builder.jiraId(link);
                }
            }
        }
    }
}
