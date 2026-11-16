# =============================================================================
# add_author_headers.ps1
# Adds standardized author headers to all Java files in the project.
# Author: Vipul Sajjanwar – Senior Test Automation Engineer
# =============================================================================

param (
    [switch]$DryRun = $false
)

Set-Location $PSScriptRoot\..

# ------------------------------------------------------------------
# Description map – derived from class responsibilities
# ------------------------------------------------------------------
$descriptions = @{
    # com.icedq
    "com/icedq/admin/AppLoginPage.java"                                       = "Page Object representing the IcedQ Admin application login screen, encapsulating locators and actions for credential entry and form submission."
    # com.securebank
    "com/securebank/pages/AccountsPage.java"                                  = "Page Object for the SecureBank Accounts page, providing actions and verifications for account listing, details, and navigation."
    "com/securebank/pages/DashboardPage.java"                                 = "Page Object for the SecureBank Dashboard, encapsulating navigation, widget interactions, and dashboard-level assertions."
    "com/securebank/pages/SecureBankLoginPage.java"                           = "Page Object for the SecureBank login screen, handling credential input, login submission, and authentication state verification."
    # framework.ai
    "framework/ai/AiConfig.java"                                              = "Configuration holder for AI integration settings including API keys, model selection, temperature, and retry behaviour."
    "framework/ai/AiFailureAnalyzer.java"                                     = "AI-powered test failure analysis engine that interprets stack traces and screenshots to generate actionable root-cause suggestions."
    "framework/ai/AiLocatorAnalyzer.java"                                     = "AI-driven locator self-healing service that proposes alternative element selectors when standard locators fail at runtime."
    "framework/ai/AiTestDataGenerator.java"                                   = "AI-based test data generator that creates realistic, context-aware input data sets for parameterised and data-driven tests."
    "framework/ai/AiTestGenerator.java"                                       = "AI-powered test generation utility that scaffolds new test cases from natural-language feature descriptions."
    "framework/ai/llm/AnthropicLlmClient.java"                               = "LLM client implementation targeting the Anthropic Claude API for AI-assisted framework capabilities."
    "framework/ai/llm/LlmClient.java"                                        = "Contract interface defining the uniform API for interacting with any supported Large Language Model provider."
    "framework/ai/llm/LlmClientFactory.java"                                 = "Factory class responsible for instantiating and caching the appropriate LlmClient based on configuration."
    "framework/ai/llm/LlmRateLimiter.java"                                   = "Thread-safe rate limiter for LLM API calls, preventing quota exhaustion and enforcing configurable request throttling."
    "framework/ai/llm/MockLlmClient.java"                                    = "Deterministic mock implementation of LlmClient for offline unit tests and CI environments without real AI endpoints."
    "framework/ai/llm/OpenAiCompatLlmClient.java"                            = "LLM client implementation compatible with OpenAI-spec APIs (OpenAI, Ollama, Azure OpenAI, local models)."
    # framework.annotations
    "framework/annotations/CustomTest.java"                                   = "Composite meta-annotation aggregating TestNG @Test configuration and custom framework metadata for a unified test declaration."
    "framework/annotations/TestInfo.java"                                     = "Annotation interface for attaching descriptive test metadata such as ID, name, severity, and feature tags to test methods."
    "framework/annotations/TestInfoResolver.java"                             = "Utility class that extracts and resolves TestInfo annotation data at runtime for use by reporters and listeners."
    "framework/annotations/TestMetadata.java"                                 = "Rich metadata annotation providing test classification fields including owner, priority, component, Jira ID, and requirement mappings."
    "framework/annotations/TestMetadataExtractor.java"                       = "Extracts and aggregates TestMetadata annotation values from test methods and classes for downstream reporting."
    # framework.api
    "framework/api/ApiClient.java"                                            = "Core REST API client built on RestAssured, providing centralised request execution with authentication, logging, and timeout handling."
    "framework/api/ApiRequestBuilder.java"                                    = "Fluent builder for constructing REST API requests with headers, query parameters, path variables, and request bodies."
    "framework/api/ApiResponseValidator.java"                                 = "Assertion utility for HTTP responses, validating status codes, JSON fields, schema compliance, and response time SLAs."
    "framework/api/ApiUtils.java"                                             = "Helper utilities for common API testing tasks: JSON parsing, response extraction, request logging, and payload construction."
    # framework.base
    "framework/base/BaseApiTest.java"                                         = "Abstract base class for API tests, handling RestAssured configuration, auth token lifecycle, and shared request specifications."
    "framework/base/BasePage.java"                                            = "Abstract Playwright Page Object base providing core interaction primitives: click, fill, navigate, wait, and screenshot capture."
    "framework/base/BaseTest.java"                                            = "Abstract base class for all UI tests, orchestrating browser and page lifecycle, TestNG hooks, and reporter integration."
    # framework.browser
    "framework/browser/BrowserContextManager.java"                           = "Manages isolated Playwright BrowserContext instances per test, enabling cookie isolation and parallel test execution."
    "framework/browser/BrowserFactory.java"                                   = "Factory class that instantiates Playwright Browser objects for Chromium, Firefox, and WebKit based on configuration."
    "framework/browser/BrowserManager.java"                                   = "Thread-local browser lifecycle manager ensuring each test thread has a dedicated, clean Browser instance."
    "framework/browser/PlaywrightManager.java"                               = "Central Playwright instance manager handling initialisation, configuration, and graceful shutdown of the Playwright engine."
    # framework.components
    "framework/components/DataTable.java"                                     = "Reusable Page Component encapsulating interactions with HTML data tables: row reading, column extraction, and cell assertions."
    "framework/components/Dropdown.java"                                      = "Reusable Page Component for dropdown and select controls, providing select-by-text, value, and index with assertion support."
    "framework/components/Header.java"                                        = "Reusable Page Component representing the application header bar with navigation, user menu, and notification area."
    "framework/components/Modal.java"                                         = "Reusable Page Component encapsulating modal dialog interactions: open, close, confirm, dismiss, and content verification."
    "framework/components/Pagination.java"                                    = "Reusable Page Component for paginated lists, providing navigation to first, last, next, previous, and specific pages."
    "framework/components/Sidebar.java"                                       = "Reusable Page Component for sidebar navigation menus, handling expand, collapse, and menu item selection."
    # framework.config
    "framework/config/ChainTestConfig.java"                                   = "Configuration class for ChainTest reporting integration, supplying project credentials, run identifiers, and dashboard URLs."
    "framework/config/ConfigManager.java"                                     = "Singleton configuration manager providing unified access to all framework property sources with environment override support."
    "framework/config/EnvConfig.java"                                         = "Environment-specific configuration holder mapping base URLs, credentials, and service endpoints per deployment environment."
    "framework/config/EnvironmentManager.java"                                = "Manages active test environment selection, resolution, and switching based on system properties and config files."
    "framework/config/PropertiesReader.java"                                  = "Utility for reading Java .properties files from classpath or file system with caching and type-safe accessors."
    "framework/config/PropertiesWriter.java"                                  = "Utility for writing or updating Java .properties files at runtime for dynamic configuration persistence."
    "framework/config/ReportingConfig.java"                                   = "Configuration class centralising reporting preferences: enabled reporters, output paths, and notification settings."
    # framework.constants
    "framework/constants/ApiConstants.java"                                   = "Constants for API testing: endpoint paths, HTTP status codes, default headers, and timeout values."
    "framework/constants/BrowserConstants.java"                               = "Constants for browser configuration: supported browser names, default viewport dimensions, and launch option keys."
    "framework/constants/DatabaseConstants.java"                              = "Constants for database connectivity: driver class names, connection timeout defaults, and SQL query templates."
    "framework/constants/FileConstants.java"                                  = "Constants for file system paths used by the framework: report directories, screenshot folders, and resource locations."
    "framework/constants/FrameworkConstants.java"                             = "Core framework-wide constants including version identifiers, default timeouts, and global configuration keys."
    "framework/constants/TestConstants.java"                                  = "Common test constants shared across test classes: data file names, test user roles, and environment identifiers."
    # framework.context
    "framework/context/TestContext.java"                                      = "Per-test execution context holding the active Page, BrowserContext, test metadata, and reporter references."
    "framework/context/TestContextFactory.java"                               = "Factory for creating and initialising TestContext instances with the correct browser, page, and metadata for each test."
    "framework/context/TestContextManager.java"                               = "Thread-local registry for TestContext instances, ensuring each parallel test thread accesses its own isolated context."
    # framework.database
    "framework/database/DatabaseManager.java"                                 = "Manages JDBC connections and connection pools for database validation and test data setup/teardown."
    "framework/database/DatabaseValidator.java"                               = "Provides assertion-style methods for verifying database state against expected values after test actions."
    "framework/database/JdbcUtils.java"                                       = "JDBC utility class offering low-level query execution, result-set mapping, and connection lifecycle helpers."
    "framework/database/QueryExecutor.java"                                   = "High-level query executor wrapping JDBC for SELECT, INSERT, UPDATE, and DELETE operations with retry and logging."
    # framework.excel
    "framework/excel/ExcelReader.java"                                        = "Apache POI-based Excel reader for extracting test data from .xlsx workbooks into structured collections."
    "framework/excel/ExcelUtils.java"                                         = "Utility class for Excel file manipulation: cell type handling, sheet navigation, and data normalisation."
    "framework/excel/ExcelWriter.java"                                        = "Apache POI-based Excel writer for recording test results and generated data back to .xlsx workbooks."
    # framework.exceptions
    "framework/exceptions/AiException.java"                                   = "Checked exception signalling failures during AI service calls, prompt parsing, or LLM response processing."
    "framework/exceptions/ApiException.java"                                  = "Runtime exception wrapping API client errors including HTTP failures, timeout, and response validation issues."
    "framework/exceptions/BrowserException.java"                              = "Runtime exception for browser and Playwright errors: launch failures, navigation timeouts, and element interaction errors."
    "framework/exceptions/ConfigurationException.java"                        = "Runtime exception indicating a missing, invalid, or conflicting framework configuration value."
    "framework/exceptions/DatabaseException.java"                             = "Runtime exception wrapping JDBC and database connectivity or query execution errors."
    "framework/exceptions/ExcelException.java"                                = "Runtime exception for Excel file read/write failures including format errors and missing sheets."
    "framework/exceptions/FrameworkException.java"                            = "Root runtime exception for the automation framework, providing a common base for all framework-specific errors."
    "framework/exceptions/McpException.java"                                  = "Runtime exception for Model Context Protocol (MCP) communication and tool invocation failures."
    # framework.execution
    "framework/execution/ExecutionManager.java"                               = "Coordinates test suite execution, thread pool management, and pre/post-execution hooks for the framework."
    "framework/execution/RetryAnalyzer.java"                                  = "TestNG IRetryAnalyzer implementation controlling automatic test retry logic based on configurable attempt counts."
    "framework/execution/TestNGExecutor.java"                                 = "Programmatic TestNG test runner enabling dynamic suite construction and execution from code or config."
    # framework.listeners
    "framework/listeners/AllureTestListener.java"                             = "TestNG listener that integrates Allure reporting hooks for test start, finish, failure, and step events."
    "framework/listeners/AnnotationTransformer.java"                          = "TestNG IAnnotationTransformer implementation dynamically modifying annotations for retry, groups, and timeout at runtime."
    "framework/listeners/ChainTestListener.java"                              = "TestNG listener bridging framework events to ChainTest real-time reporting dashboard."
    "framework/listeners/ELKTestListener.java"                                = "TestNG listener that ships test execution events and results to the ELK Stack (Elasticsearch/Logstash) pipeline."
    "framework/listeners/SuiteListener.java"                                  = "TestNG ISuiteListener implementation handling suite-level setup, teardown, and cross-suite reporting aggregation."
    "framework/listeners/TestListener.java"                                   = "Primary TestNG ITestListener dispatching test lifecycle events to all registered reporter implementations."
    # framework.mcp
    "framework/mcp/McpClient.java"                                            = "Client for communicating with MCP (Model Context Protocol) servers, sending tool call requests and processing responses."
    "framework/mcp/McpConfig.java"                                            = "Configuration class for MCP server connection details: transport type, host, port, and authentication tokens."
    "framework/mcp/McpHttpServer.java"                                        = "HTTP transport implementation of an MCP server, exposing framework tools over a REST-like SSE endpoint."
    "framework/mcp/McpStdioServer.java"                                       = "STDIO transport implementation of an MCP server for local agent integration via standard input/output streams."
    "framework/mcp/McpToolManager.java"                                       = "Registry and dispatcher for MCP tools exposed by the framework, mapping tool names to execution handlers."
    # framework.pages
    "framework/pages/ComponentsPage.java"                                     = "Page Object for a components showcase page, providing interaction methods for UI component testing scenarios."
    "framework/pages/FileUploadPage.java"                                     = "Page Object for file upload functionality, encapsulating input element targeting, file attachment, and upload verification."
    "framework/pages/LoginPage.java"                                          = "Generic login Page Object providing username/password input, form submission, and post-login state assertions."
    "framework/pages/UserManagementPage.java"                                 = "Page Object for user management screens, covering user creation, search, role assignment, and deletion workflows."
    # framework.promptdriven
    "framework/promptdriven/PromptDiscovery.java"                             = "Service that discovers and loads prompt test definition files from the configured resource directories."
    "framework/promptdriven/PromptFile.java"                                  = "Value object representing a parsed prompt test file including metadata, variables, and ordered step definitions."
    "framework/promptdriven/PromptParser.java"                                = "Parser that converts YAML/Markdown prompt test files into structured PromptFile and TestPlan objects."
    "framework/promptdriven/PromptSuiteListener.java"                         = "TestNG listener tailored for prompt-driven test suites, managing plan caching, variable scoping, and result collection."
    "framework/promptdriven/PromptTest.java"                                  = "TestNG test class that executes prompt-driven test plans discovered and compiled at runtime by the framework."
    "framework/promptdriven/PromptTestFactory.java"                           = "Factory that dynamically constructs TestNG ITestNGMethod instances from discovered prompt test definitions."
    "framework/promptdriven/PromptTestRunner.java"                            = "Top-level orchestrator that drives the full prompt-driven test lifecycle: discovery, planning, execution, and reporting."
    "framework/promptdriven/exec/ApiActionExecutor.java"                      = "Executes API action steps defined in prompt-driven test plans using the framework REST client."
    "framework/promptdriven/exec/ExecutionContext.java"                        = "Mutable execution state for a prompt-driven test run, holding variables, secrets, and step outcomes."
    "framework/promptdriven/exec/SecretsProvider.java"                        = "Secure accessor for sensitive values referenced in prompt tests, resolving secrets from environment or vault."
    "framework/promptdriven/exec/StepExecutor.java"                           = "Central dispatcher routing individual plan steps to the appropriate UI or API action executor."
    "framework/promptdriven/exec/UiActionExecutor.java"                       = "Executes UI action steps (click, fill, navigate, assert) defined in prompt-driven test plans via Playwright."
    "framework/promptdriven/exec/VariableResolver.java"                       = "Resolves variable references and expressions within prompt test step definitions using runtime context values."
    "framework/promptdriven/exec/VerificationEngine.java"                     = "Evaluates verification assertions from test plan steps, mapping conditions to Playwright and Java assertions."
    "framework/promptdriven/plan/ActionType.java"                             = "Enumeration of supported action types (CLICK, FILL, NAVIGATE, ASSERT, API_CALL, etc.) in prompt-driven test plans."
    "framework/promptdriven/plan/PlanCache.java"                              = "Thread-safe cache storing compiled TestPlan objects to avoid redundant parsing during parallel test execution."
    "framework/promptdriven/plan/PlanService.java"                            = "Service coordinating AI plan generation, caching, and retrieval for prompt-driven tests."
    "framework/promptdriven/plan/PlanStep.java"                               = "Immutable value object representing a single executable step in a compiled test plan."
    "framework/promptdriven/plan/PlanValidator.java"                          = "Validates compiled TestPlan objects for required fields, action type compatibility, and logical consistency."
    "framework/promptdriven/plan/TestPlan.java"                               = "Compiled, executable representation of a prompt-driven test, containing ordered PlanStep instances and metadata."
    "framework/promptdriven/report/ReportWriter.java"                         = "Serialises prompt-driven test run results to JSON and HTML summary reports for archival and review."
    "framework/promptdriven/report/StepResult.java"                           = "Value object capturing the outcome (pass/fail/skip), duration, and error details of a single executed step."
    "framework/promptdriven/report/TestRunResult.java"                        = "Aggregate result object for a complete prompt-driven test run, collecting all step results and overall status."
    # framework.reporting
    "framework/reporting/ChainTestContext.java"                               = "Context holder for ChainTest reporter integration, storing project key, run ID, and thread-local test node references."
    "framework/reporting/ChainTestLogger.java"                                = "Logging bridge that routes framework log messages to the ChainTest real-time reporting pipeline."
    "framework/reporting/ELKStackReport.java"                                 = "Value object representing a structured test execution report payload for ingestion by the ELK Stack."
    "framework/reporting/EmailNotification.java"                              = "Email notification service sending HTML test execution summaries on suite completion or failure thresholds."
    "framework/reporting/FrameworkLogger.java"                                = "Unified logging abstraction consolidating console output, step logging, and reporter attachment without duplicate entries."
    "framework/reporting/LogLevel.java"                                       = "Enumeration of supported log severity levels (DEBUG, INFO, WARN, ERROR) used throughout the framework logging layer."
    "framework/reporting/ReportManager.java"                                  = "Central report manager coordinating HTML, Allure, and third-party report generation post-suite execution."
    "framework/reporting/ReportUtils.java"                                    = "Utility methods for report file management, path construction, timestamp formatting, and report artifact cleanup."
    "framework/reporting/ReportingManager.java"                               = "Singleton orchestrator managing all registered TestReporter implementations and dispatching lifecycle events."
    "framework/reporting/TestExecutionLogger.java"                            = "Structured test execution event logger writing detailed step-level logs for diagnostics and audit trails."
    "framework/reporting/TestReporter.java"                                   = "Reporter interface contract defining lifecycle callbacks implemented by all reporting integrations."
    "framework/reporting/TestStatus.java"                                     = "Enumeration of test execution statuses (PASSED, FAILED, SKIPPED, BROKEN) used across all reporter implementations."
    "framework/reporting/allure/AllureHistoryManager.java"                    = "Manages Allure history trend data: copying previous results, pruning old runs, and linking history for trend charts."
    "framework/reporting/allure/AllureReportManager.java"                     = "Coordinates Allure report generation, including serving, opening, and archiving generated reports."
    "framework/reporting/allure/ReportPathManager.java"                       = "Resolves and manages file system paths for Allure results, history, and generated report output directories."
    "framework/reporting/artifacts/ArtifactManager.java"                      = "Manages test artifact lifecycle: attaching screenshots, videos, and traces to the active reporter context."
    "framework/reporting/artifacts/ScreenshotManager.java"                   = "Captures and stores Playwright page screenshots on test failure or on-demand, with Allure attachment support."
    "framework/reporting/database/JdbcTestResultRepository.java"             = "JDBC-backed implementation of TestResultRepository for persisting test results to a relational database."
    "framework/reporting/database/TestResult.java"                            = "Database entity model representing a single test execution result row stored in the reporting schema."
    "framework/reporting/database/TestResultRepository.java"                  = "Repository interface defining CRUD operations for persisting and querying test results in the database."
    "framework/reporting/elk/ELKStackLogger.java"                             = "Ships structured log events to Elasticsearch via Logstash HTTP endpoint for real-time test observability."
    "framework/reporting/elk/ELKTestResultBuilder.java"                       = "Builder class constructing ELK-compatible JSON documents from test result data for Elasticsearch indexing."
    "framework/reporting/reporters/AllureTestReporter.java"                   = "TestReporter implementation delegating test events to the Allure framework for rich HTML report generation."
    "framework/reporting/reporters/ChainTestReporter.java"                    = "TestReporter implementation streaming live test results to the ChainTest SaaS reporting dashboard."
    "framework/reporting/reporters/ConsoleTestReporter.java"                  = "TestReporter implementation writing colour-coded test progress and results to standard console output."
    "framework/reporting/reporters/DatabaseTestReporter.java"                 = "TestReporter implementation persisting test execution results to a configured relational database."
    "framework/reporting/reporters/ELKTestReporter.java"                      = "TestReporter implementation forwarding test events and results to the ELK Stack logging pipeline."
    "framework/reporting/reporters/ExtentTestReporter.java"                   = "TestReporter implementation generating interactive ExtentReports HTML reports with screenshots and step logs."
    # framework.utils
    "framework/utils/AllureUtils.java"                                        = "Utility methods for programmatic Allure annotations: attaching files, adding steps, and setting categories."
    "framework/utils/ChainTestUtil.java"                                      = "Utility methods for ChainTest integration: node creation, step logging, and media attachment helpers."
    "framework/utils/CustomTimeUtil.java"                                     = "Date and time formatting utilities tailored to framework reporting, log stamping, and timestamp generation."
    "framework/utils/DateUtils.java"                                          = "General-purpose date/time utilities for parsing, formatting, comparison, and offset calculations."
    "framework/utils/ElementUtils.java"                                       = "Playwright element interaction helpers: safe click, hover, scroll-into-view, and visibility wait wrappers."
    "framework/utils/FileUtils.java"                                          = "File system utility methods: directory creation, file copying, content reading, and cleanup helpers."
    "framework/utils/JsonUtils.java"                                          = "JSON serialisation/deserialisation utilities wrapping Jackson ObjectMapper with error handling and type mapping."
    "framework/utils/PageUtils.java"                                          = "Playwright page-level utilities: URL assertion, title verification, cookie management, and scroll helpers."
    "framework/utils/PlaywrightFileActions.java"                              = "Playwright-specific file action helpers for download interception, upload triggering, and file path resolution."
    "framework/utils/RandomDataUtils.java"                                    = "Random test data generators for names, emails, phone numbers, dates, and UUIDs using Faker."
    "framework/utils/ScreenshotUtils.java"                                    = "Screenshot capture utilities providing full-page, element-scoped, and annotated screenshot methods for reporting."
    "framework/utils/StringUtils.java"                                        = "String manipulation utilities: trimming, normalisation, truncation, regex helpers, and camelCase conversion."
    "framework/utils/TraceUtils.java"                                         = "Playwright trace lifecycle utilities: starting, stopping, and exporting traces for failed test diagnostics."
    "framework/utils/ValidationUtils.java"                                    = "Assertion helper utilities providing human-readable soft and hard assertion wrappers for framework tests."
    "framework/utils/WaitUtils.java"                                          = "Playwright wait strategy helpers: element visibility, network idle, custom condition polling, and sleep wrappers."
    # tests.api
    "tests/api/CreateUserTest.java"                                           = "API test suite validating user creation endpoints: request construction, response assertions, and schema verification."
    "tests/api/DeleteUserTest.java"                                           = "API test suite verifying user deletion endpoints: successful deletion, idempotency, and 404 response handling."
    "tests/api/GetUserTest.java"                                              = "API test suite covering user retrieval endpoints: single user, list pagination, and not-found scenarios."
    "tests/api/UpdateUserTest.java"                                           = "API test suite for user update (PUT/PATCH) endpoints, covering full updates, partial updates, and conflict cases."
    # tests.database
    "tests/database/UserDatabaseValidationTest.java"                          = "Database integration test validating that user CRUD API operations correctly persist and modify database state."
    # tests.icedq
    "tests/icedq/admin/LoginTest.java"                                        = "UI test suite for the IcedQ Admin login flow, covering valid credentials, invalid credentials, and session handling."
    # tests.integration
    "tests/integration/AiFailureAnalysisExampleTest.java"                     = "Integration test demonstrating AI-powered failure analysis on injected test failures to validate LLM suggestions."
    "tests/integration/AiGeneratedUserDataTest.java"                          = "Integration test validating AI-generated test data quality and format for use in user management workflows."
    "tests/integration/ExcelDataDrivenLoginTest.java"                         = "Data-driven login test reading credential sets from Excel to validate multiple user scenarios in one suite."
    # tests.promptdriven
    "tests/promptdriven/McpProtocolAndToolsTest.java"                         = "Unit and integration tests verifying MCP protocol handling, tool registration, and request/response correctness."
    "tests/promptdriven/ParallelIsolationTest.java"                           = "Tests verifying thread isolation of prompt-driven test execution contexts during parallel test runs."
    "tests/promptdriven/PlanCacheUnitTest.java"                               = "Unit tests for PlanCache verifying thread safety, eviction behaviour, and cache hit/miss correctness."
    "tests/promptdriven/PlanValidatorUnitTest.java"                           = "Unit tests for PlanValidator covering valid plan acceptance, missing field detection, and type mismatch errors."
    "tests/promptdriven/PromptDiscoveryUnitTest.java"                         = "Unit tests for PromptDiscovery validating file scanning, filter patterns, and prompt file loading correctness."
    "tests/promptdriven/PromptExecutionAndHealingTest.java"                   = "Integration tests for end-to-end prompt-driven test execution including locator healing and step retry logic."
    "tests/promptdriven/PromptParserUnitTest.java"                            = "Unit tests for PromptParser covering YAML syntax, variable substitution, and malformed input error handling."
    "tests/promptdriven/VariableResolverAndSecretsTest.java"                  = "Unit tests for VariableResolver and SecretsProvider validating expression resolution and secure value masking."
    "tests/promptdriven/VerificationEngineUnitTest.java"                      = "Unit tests for VerificationEngine asserting correct evaluation of text, attribute, and visibility conditions."
    # tests.securebank
    "tests/securebank/SecureBankDashboardTest.java"                           = "UI test suite for the SecureBank Dashboard: navigation assertions, widget loading, and account summary display."
    "tests/securebank/SecureBankLoginTest.java"                               = "UI test suite for the SecureBank login screen covering valid login, invalid credentials, and session timeout."
    # tests.ui
    "tests/ui/DropdownTest.java"                                              = "UI test suite exercising the Dropdown component across select-by-text, select-by-value, and multi-select scenarios."
    "tests/ui/FileUploadTest.java"                                            = "UI test suite validating file upload workflows including single upload, multiple files, and invalid format rejection."
    "tests/ui/TableTest.java"                                                 = "UI test suite for DataTable component interactions: sorting, filtering, pagination, and cell content assertion."
    "tests/ui/UserCreationTest.java"                                          = "End-to-end UI test suite for user creation workflows covering form validation, success paths, and duplicate detection."
}

# ------------------------------------------------------------------
# Header template
# ------------------------------------------------------------------
function Get-Header {
    param([string]$description, [string]$created)
    return @"
/**
 * Project: Playwright Java Automation Framework
 * Author: Vipul Sajjanwar
 * Role: Senior Test Automation Engineer
 * Description: $description
 * Created: $created
 */
"@
}

# ------------------------------------------------------------------
# Main loop
# ------------------------------------------------------------------
$files = Get-ChildItem -Path . -Filter *.java -Recurse | Where-Object { $_.FullName -notmatch '\\target\\' }

$updated = [System.Collections.Generic.List[string]]::new()
$skipped = [System.Collections.Generic.List[string]]::new()
$alreadyHasHeader = [System.Collections.Generic.List[string]]::new()

foreach ($file in $files) {
    $relativePath = $file.FullName.Replace((Get-Location).Path + '\', '').Replace('\', '/')
    
    # Read raw bytes to detect CRLF line endings
    $rawBytes = [IO.File]::ReadAllBytes($file.FullName)
    $hasCRLF = $false
    for ($i = 0; $i -lt $rawBytes.Length - 1; $i++) {
        if ($rawBytes[$i] -eq 13 -and $rawBytes[$i+1] -eq 10) { $hasCRLF = $true; break }
    }
    # Strip BOM if present, read as string
    $rawStr = [System.IO.File]::ReadAllText($file.FullName, (New-Object System.Text.UTF8Encoding $false))
    # Remove any BOM that may have been embedded as first char
    if ($rawStr.Length -gt 0 -and [int]$rawStr[0] -eq 65279) { $rawStr = $rawStr.Substring(1) }
    
    # Normalize to LF internally using stripped rawStr
    $contentNorm = $rawStr -replace "`r`n", "`n" -replace "`r", "`n"
    
    # Already has our header?
    if ($contentNorm -match '(?s)/\*\*[\s\S]*?Project:\s*Playwright Java Automation Framework[\s\S]*?\*/') {
        $alreadyHasHeader.Add($relativePath)
        Write-Host "[SKIP-EXISTS] $relativePath" -ForegroundColor Yellow
        continue
    }
    
    # Determine creation date from git
    $created = (git log --diff-filter=A --format="%as" -- $relativePath 2>$null) | Select-Object -Last 1
    if (-not $created) { $created = "2026-11-16" }
    
    # Look up description
    # Match by partial path key ending
    $descKey = $relativePath -replace '^src/(main|test)/java/', ''
    $description = $descriptions[$descKey]
    if (-not $description) { $description = "Automation framework component for $([IO.Path]::GetFileNameWithoutExtension($file.Name))." }
    
    # Build header (always LF internally)
    $header = "/**`n * Project: Playwright Java Automation Framework`n * Author: Vipul Sajjanwar`n * Role: Senior Test Automation Engineer`n * Description: $description`n * Created: $created`n */`n"
    
    # Prepend header
    $newContent = $header + $contentNorm
    
    if ($DryRun) {
        Write-Host "[DRY-RUN] Would update: $relativePath" -ForegroundColor Cyan
        $updated.Add($relativePath)
        continue
    }
    
    # Restore original line endings if file had CRLF
    if ($hasCRLF) {
        $newContent = $newContent -replace "`n", "`r`n"
    }
    
    [IO.File]::WriteAllText($file.FullName, $newContent, (New-Object System.Text.UTF8Encoding $false))
    $updated.Add($relativePath)
    Write-Host "[UPDATED]  $relativePath" -ForegroundColor Green
}

Write-Host ""
Write-Host "===== SUMMARY =====" -ForegroundColor Magenta
Write-Host "Total files scanned : $($files.Count)" -ForegroundColor White
Write-Host "Files updated       : $($updated.Count)" -ForegroundColor Green
Write-Host "Already had header  : $($alreadyHasHeader.Count)" -ForegroundColor Yellow
Write-Host "Files skipped       : $($skipped.Count)" -ForegroundColor Red
