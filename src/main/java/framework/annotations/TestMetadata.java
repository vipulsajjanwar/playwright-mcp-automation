/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: Rich metadata annotation providing test classification fields including owner, priority, component, Jira ID, and requirement mappings.
 * Created: 2026-10-01
 */
package framework.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable domain representation of test metadata resolved from
 * {@link CustomTest} and legacy {@link TestInfo} annotations.
 */
public final class TestMetadata {

    private final String testId;
    private final String name;
    private final String description;
    private final String module;
    private final String feature;
    private final String subFeature;
    private final String testType;
    private final String priority;
    private final String severity;
    private final String category;
    private final List<String> tags;
    private final String owner;
    private final String author;
    private final String requirementId;
    private final String jiraId;
    private final String storyId;
    private final String environment;
    private final String browser;
    private final String platform;
    private final String service;
    private final String component;
    private final String dataSet;
    private final String executionType;
    private final boolean enabled;
    private final boolean annotated;

    private TestMetadata(Builder builder) {
        this.testId = builder.testId;
        this.name = builder.name;
        this.description = builder.description;
        this.module = builder.module;
        this.feature = builder.feature;
        this.subFeature = builder.subFeature;
        this.testType = builder.testType;
        this.priority = builder.priority;
        this.severity = builder.severity;
        this.category = builder.category;
        this.tags = Collections.unmodifiableList(new ArrayList<>(builder.tags));
        this.owner = builder.owner;
        this.author = builder.author;
        this.requirementId = builder.requirementId;
        this.jiraId = builder.jiraId;
        this.storyId = builder.storyId;
        this.environment = builder.environment;
        this.browser = builder.browser;
        this.platform = builder.platform;
        this.service = builder.service;
        this.component = builder.component;
        this.dataSet = builder.dataSet;
        this.executionType = builder.executionType;
        this.enabled = builder.enabled;
        this.annotated = builder.annotated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTestId() { return testId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getModule() { return module; }
    public String getFeature() { return feature; }
    public String getSubFeature() { return subFeature; }
    public String getTestType() { return testType; }
    public String getPriority() { return priority; }
    public String getSeverity() { return severity; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public String getOwner() { return owner; }
    public String getAuthor() { return author; }
    public String getRequirementId() { return requirementId; }
    public String getJiraId() { return jiraId; }
    public String getStoryId() { return storyId; }
    public String getEnvironment() { return environment; }
    public String getBrowser() { return browser; }
    public String getPlatform() { return platform; }
    public String getService() { return service; }
    public String getComponent() { return component; }
    public String getDataSet() { return dataSet; }
    public String getExecutionType() { return executionType; }
    public boolean isEnabled() { return enabled; }
    public boolean isAnnotated() { return annotated; }

    /**
     * Resolves author or owner if one is blank.
     */
    public String getEffectiveAuthor() {
        if (author != null && !author.isBlank()) {
            return author;
        }
        if (owner != null && !owner.isBlank()) {
            return owner;
        }
        return "QA";
    }

    public static final class Builder {
        private String testId = "";
        private String name = "";
        private String description = "";
        private String module = "General";
        private String feature = "General";
        private String subFeature = "";
        private String testType = "UI";
        private String priority = "MEDIUM";
        private String severity = "NORMAL";
        private String category = "Regression";
        private final Set<String> tags = new LinkedHashSet<>();
        private String owner = "";
        private String author = "";
        private String requirementId = "";
        private String jiraId = "";
        private String storyId = "";
        private String environment = "";
        private String browser = "";
        private String platform = "";
        private String service = "";
        private String component = "";
        private String dataSet = "";
        private String executionType = "AUTOMATED";
        private boolean enabled = true;
        private boolean annotated = false;

        public Builder testId(String testId) { this.testId = testId != null ? testId : ""; return this; }
        public Builder name(String name) { this.name = name != null ? name : ""; return this; }
        public Builder description(String description) { this.description = description != null ? description : ""; return this; }
        public Builder module(String module) { this.module = module != null ? module : ""; return this; }
        public Builder feature(String feature) { this.feature = feature != null ? feature : ""; return this; }
        public Builder subFeature(String subFeature) { this.subFeature = subFeature != null ? subFeature : ""; return this; }
        public Builder testType(String testType) { this.testType = testType != null ? testType : "UI"; return this; }
        public Builder priority(String priority) { this.priority = priority != null ? priority : "MEDIUM"; return this; }
        public Builder severity(String severity) { this.severity = severity != null ? severity : "NORMAL"; return this; }
        public Builder category(String category) { this.category = category != null ? category : "Regression"; return this; }
        public Builder addTags(String... tags) {
            if (tags != null) {
                for (String tag : tags) {
                    if (tag != null && !tag.isBlank()) {
                        this.tags.add(tag.trim());
                    }
                }
            }
            return this;
        }
        public Builder addTags(List<String> tags) {
            if (tags != null) {
                for (String tag : tags) {
                    if (tag != null && !tag.isBlank()) {
                        this.tags.add(tag.trim());
                    }
                }
            }
            return this;
        }
        public Builder owner(String owner) { this.owner = owner != null ? owner : ""; return this; }
        public Builder author(String author) { this.author = author != null ? author : ""; return this; }
        public Builder requirementId(String requirementId) { this.requirementId = requirementId != null ? requirementId : ""; return this; }
        public Builder jiraId(String jiraId) { this.jiraId = jiraId != null ? jiraId : ""; return this; }
        public Builder storyId(String storyId) { this.storyId = storyId != null ? storyId : ""; return this; }
        public Builder environment(String environment) { this.environment = environment != null ? environment : ""; return this; }
        public Builder browser(String browser) { this.browser = browser != null ? browser : ""; return this; }
        public Builder platform(String platform) { this.platform = platform != null ? platform : ""; return this; }
        public Builder service(String service) { this.service = service != null ? service : ""; return this; }
        public Builder component(String component) { this.component = component != null ? component : ""; return this; }
        public Builder dataSet(String dataSet) { this.dataSet = dataSet != null ? dataSet : ""; return this; }
        public Builder executionType(String executionType) { this.executionType = executionType != null ? executionType : "AUTOMATED"; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder annotated(boolean annotated) { this.annotated = annotated; return this; }

        public TestMetadata build() {
            return new TestMetadata(this);
        }
    }
}
