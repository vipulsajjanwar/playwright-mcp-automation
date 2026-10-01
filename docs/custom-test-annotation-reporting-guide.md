# Custom Test Annotation & Central Reporting Integration Guide

## 1. Overview & Architecture

The `@CustomTest` annotation serves as the **single source of truth** for all test-related metadata, reporting, execution tracking, logging, and observability across the framework. It eliminates boilerplate code in test classes and dynamically bridges metadata into multiple reporting engines and data sinks.

### Integrated Subsystems
- **TestNG**: Suite, test, and method lifecycle hooks via `TestListener` and `SuiteListener`.
- **ExtentReports**: Dynamic test nodes, categories, authors, device info, steps, and screenshots.
- **Allure Reports**: Dynamic runtime mappings for `@Epic`, `@Feature`, `@Story`, `@Severity`, `@Owner`, `@Issue`, `@TmsLink`, tags, and attachments.
- **ChainTest**: Hierarchical test tree (Suite > Class > Method), execution metadata, and screenshots.
- **ELK Stack / Elasticsearch**: Non-blocking asynchronous JSON payload transmission to Elasticsearch indices.
- **Relational Database**: Relational schema persistence (`test_results` table) using JDBC connection pooling.
- **Console / File Logging**: Structured, unified logging via SLF4J / Logback without log duplication.
- **Playwright Artifacts**: Automatic failure capture for Screenshots, Traces, Videos, Page HTML source, and Page URL.
- **CI/CD Environments**: Automatic ingestion of Jenkins (`JOB_NAME`, `BUILD_NUMBER`, `BUILD_URL`) and Git metadata (`GIT_BRANCH`, `GIT_COMMIT`).

---

## 2. Architecture Diagram

```
                  ┌──────────────────────────────┐
                  │      @CustomTest (Method)    │
                  │      @CustomTest (Class)     │
                  └──────────────┬───────────────┘
                                 │
                                 ▼
                     TestMetadataExtractor
                                 │
                                 ▼
                     TestContextFactory
                                 │
                                 ▼
              ThreadLocal<TestContext> (Isolated State)
                                 │
                                 ▼
                     TestListener (TestNG)
                                 │
                                 ▼
                         ReportingManager
                                 │
     ┌───────────────┬───────────┼───────────┬──────────────┬──────────────┐
     ▼               ▼           ▼           ▼              ▼              ▼
ExtentReporter AllureReporter ChainTest   ELKReporter   DBReporter    Console
     │               │           │           │              │
Extent Reports  Allure XML   ChainReport  ELKBuilder    JDBC Repo
(HTML Dashboard)(Attachments)(Test Tree)     │              │
                                      ELKStackLogger    Database
                                      (Elasticsearch)  (H2 / Postgres)
```

---

## 3. Package Structure

```text
src/main/java/framework/
├── annotations/
│   ├── CustomTest.java                 # Central test metadata annotation
│   ├── TestMetadata.java               # Immutable value object for parsed metadata
│   ├── TestMetadataExtractor.java      # Resolver merging method, class & TestNG annotations
│   ├── TestInfo.java                   # Legacy annotation (backward-compatible)
│   └── TestInfoResolver.java           # Legacy resolver (backward-compatible)
├── context/
│   ├── TestContext.java                # Central execution context domain model
│   ├── TestContextFactory.java         # Factory creating TestContext from ITestResult & CI vars
│   └── TestContextManager.java         # ThreadLocal context holder
├── config/
│   ├── ReportingConfig.java            # Unified reporting configuration provider
│   └── ConfigManager.java              # Configuration loader
├── reporting/
│   ├── TestReporter.java               # Common reporter interface
│   ├── ReportingManager.java           # Central reporter registry & event dispatcher
│   ├── FrameworkLogger.java            # Unified logging facade
│   ├── TestStatus.java                 # Execution status enum (NOT_RUN, STARTED, RUNNING, PASS, FAIL, SKIP)
│   ├── ReportUtils.java                # Backwards-compatible utility delegating to FrameworkLogger
│   ├── TestExecutionLogger.java        # Backwards-compatible legacy logger
│   ├── artifacts/
│   │   ├── ScreenshotManager.java      # Deduplicated screenshot capture and attachment
│   │   └── ArtifactManager.java        # Playwright trace, video, HTML, URL artifact capture
│   ├── elk/
│   │   ├── ELKTestResultBuilder.java   # Transforms TestContext to Elasticsearch JSON payload
│   │   └── ELKStackLogger.java         # Asynchronous HTTP sender to Elasticsearch
│   ├── database/
│   │   ├── TestResult.java             # Database entity model
│   │   ├── TestResultRepository.java   # Repository interface
│   │   └── JdbcTestResultRepository.java # JDBC implementation with auto-schema creation
│   └── reporters/
│       ├── ExtentTestReporter.java     # ExtentReports provider
│       ├── AllureTestReporter.java     # Dynamic Allure provider
│       ├── ChainTestReporter.java      # ChainTest provider
│       ├── ELKTestReporter.java        # Elasticsearch provider
│       ├── DatabaseTestReporter.java   # Database persistence provider
│       └── ConsoleTestReporter.java    # SLF4J formatted console & file provider
└── listeners/
    ├── TestListener.java               # Primary unified TestNG listener
    ├── SuiteListener.java              # Suite-level lifecycle listener
    └── AnnotationTransformer.java      # TestNG retry transformer
```

---

## 4. Custom Annotation Reference

### Attributes & Defaults

| Attribute | Type | Default | Description |
|---|---|---|---|
| `testId` | `String` | `""` | Unique test ID (e.g. `"ADMIN_USER_001"`) |
| `name` | `String` | `""` | Human-readable test name |
| `description` | `String` | `""` | Description of test objective |
| `module` | `String` | `""` | High-level module (maps to Allure `@Epic`) |
| `feature` | `String` | `""` | Feature area (maps to Allure `@Feature`) |
| `subFeature` | `String` | `""` | Sub-feature (maps to Allure `@Story`) |
| `testType` | `String` | `"UI"` | `"UI"`, `"API"`, `"DATABASE"`, `"INTEGRATION"` |
| `priority` | `String` | `"MEDIUM"` | `"LOW"`, `"MEDIUM"`, `"HIGH"`, `"CRITICAL"` |
| `severity` | `String` | `"NORMAL"` | `"BLOCKER"`, `"CRITICAL"`, `"NORMAL"`, `"MINOR"`, `"TRIVIAL"` |
| `category` | `String` | `"Regression"` | Categorization label |
| `tags` | `String[]` | `{}` | Search and filter tags |
| `owner` | `String` | `""` | Responsible team (maps to Allure `@Owner`) |
| `author` | `String` | `""` | Author / tester name |
| `requirementId`| `String` | `""` | Business requirement ID (maps to Allure `@TmsLink`) |
| `jiraId` | `String` | `""` | Jira ticket key (maps to Allure `@Issue`) |
| `storyId` | `String` | `""` | User story ID |
| `environment` | `String` | `""` | Target environment override |
| `browser` | `String` | `""` | Target browser override |
| `platform` | `String` | `""` | Target OS / platform |
| `service` | `String` | `""` | Backend microservice under test |
| `component` | `String` | `""` | Architecture component |
| `dataSet` | `String` | `""` | Data variant identifier |
| `executionType`| `String` | `"AUTOMATED"` | Execution type |
| `enabled` | `boolean` | `true` | Execution enable flag |

---

## 5. Usage Example

### Writing a Clean Test Class

```java
package tests.ui;

import framework.annotations.CustomTest;
import framework.base.BaseTest;
import framework.reporting.FrameworkLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

@CustomTest(
    module = "Administration",
    feature = "User Management",
    owner = "QA Core Team",
    category = "Regression"
)
public class UserCreationTest extends BaseTest {

    @CustomTest(
        testId = "ADMIN_USER_001",
        name = "Create Admin User",
        description = "Verify that an administrator can create a new user with admin privileges",
        subFeature = "User Creation",
        priority = "HIGH",
        severity = "CRITICAL",
        tags = {"Sanity", "Users", "RBAC"},
        requirementId = "REQ-USER-102",
        jiraId = "JIRA-7890",
        storyId = "STORY-456"
    )
    @Test(groups = {"smoke", "regression"})
    public void testCreateAdminUser() {
        FrameworkLogger.step("Step 1: Navigating to User Management page");
        Assert.assertNotNull(page(), "Playwright page instance should be active");

        FrameworkLogger.step("Step 2: Submitting user details");
        // Page actions here...

        FrameworkLogger.step("Step 3: Validating user table record");
        Assert.assertTrue(page().url().contains("http"));
    }
}
```

---

## 6. Central Reporting Configuration

Configure options in `src/test/resources/config/config.properties`:

```properties
# Central Reporting & Observability Configuration
reporting.extent=true
reporting.allure=true
reporting.chaintest=true
reporting.elk=false
reporting.database=false
reporting.console=true

reporting.screenshot.on.failure=true
reporting.video.on.failure=true
reporting.trace.on.failure=true
reporting.async.elk=true
reporting.async.database=true

# Elasticsearch Configuration (active when reporting.elk=true)
elk.host=192.168.102.37
elk.port=9200
elk.username=elastic
elk.password=elastic
elk.index=test-automation

# Database Configuration (active when reporting.database=true)
db.driver=org.h2.Driver
db.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
db.username=sa
db.password=
```

---

## 7. ELK JSON Payload Schema

When a test completes and ELK reporting is active, `ELKTestResultBuilder` serializes the `TestContext` to the following format:

```json
{
  "@timestamp": "2026-10-01T13:15:08.016Z",
  "testId": "ADMIN_USER_001",
  "testName": "Create Admin User",
  "module": "Administration",
  "feature": "User Management",
  "status": "PASS",
  "severity": "CRITICAL",
  "priority": "HIGH",
  "executionType": "AUTOMATED",
  "environment": "qa",
  "browser": "chromium",
  "suite": "Full Regression Suite",
  "className": "UserCreationTest",
  "methodName": "testCreateAdminUser",
  "threadId": "24",
  "executionId": "8b51d102-1845-429d-9cf3-0e86b2ffb384",
  "startTime": "2026-10-01T13:15:00.000Z",
  "endTime": "2026-10-01T13:15:08.016Z",
  "duration": 8016,
  "retryCount": 0,
  "error": {
    "message": "",
    "type": "",
    "stackTrace": ""
  },
  "jenkins": {
    "jobName": "Pipeline-Nightly",
    "buildNumber": "152",
    "buildUrl": "http://jenkins:8080/job/Pipeline-Nightly/152/",
    "branch": "main",
    "commit": "9f2b8a4c"
  },
  "artifacts": {
    "screenshot": "reports/Full_Regression/screenshots/testCreateAdminUser_20261001.png",
    "video": "",
    "trace": ""
  }
}
```

---

## 8. Database Schema (`test_results`)

Auto-created by `JdbcTestResultRepository`:

```sql
CREATE TABLE IF NOT EXISTS test_results (
    execution_id VARCHAR(100),
    test_id VARCHAR(100),
    test_name VARCHAR(255),
    module VARCHAR(100),
    feature VARCHAR(100),
    status VARCHAR(50),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration BIGINT,
    environment VARCHAR(50),
    browser VARCHAR(50),
    retry_count INT,
    error_message VARCHAR(2000),
    stack_trace CLOB,
    build_number VARCHAR(100),
    branch VARCHAR(100),
    commit_hash VARCHAR(100),
    created_at TIMESTAMP
);
```

---

## 9. Parallel Execution & Thread Safety

- **Thread-Local Storage**: Each test execution is isolated in `ThreadLocal<TestContext>`.
- **Deduplicated Artifacts**: Screenshot capture is triggered once and referenced across all reporting sinks.
- **Thread ID Namespacing**: Artifact filenames incorporate thread IDs and high-resolution timestamps to prevent race conditions during concurrent runs.
- **Fail-Safe Dispatching**: Every reporter invocation is isolated in individual try-catch blocks so reporting errors never alter test outcomes.
