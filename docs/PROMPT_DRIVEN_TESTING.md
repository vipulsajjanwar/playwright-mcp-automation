# Prompt-Driven Test Automation Guide

## 1. Overview

Prompt-Driven Testing allows QA engineers, developers, and product owners to write automated tests in plain English text files (`*.prompt.txt`). The framework plans the test into a deterministic, validated JSON plan using an LLM (Anthropic Claude, OpenAI, or an offline Mock provider), caches the plan for fast and cost-free replay, and executes it deterministically via Playwright and REST clients.

---

## 2. Architecture & Lifecycle

```
prompts/*.prompt.txt
      │
      ▼
PromptDiscovery ──► PromptTestFactory (@Factory)
                          │
                          ▼ (parallel thread pool, isolated contexts)
                    PromptTest (extends BaseTest)
                          │
                    PromptTestRunner
   ┌────────────────┬─────┴───────────────┬────────────────┐
   ▼                ▼                      ▼                ▼
PromptParser     PlanService            StepExecutor    ReportWriter
(header + steps) (LLM -> JSON plan,     (UI / API       (JSON + HTML +
                 cache, validate)       dispatch)        screenshots/traces)
                                           │
                                ┌──────────┴──────────┐
                                ▼                     ▼
                         UiActionExecutor      ApiActionExecutor
                         (Playwright + heal)   (HttpClient)
```

---

## 3. Prompt File Syntax

A prompt file consists of optional header comments (`# key: value`) followed by step instructions.

### Example UI Test (`ui_login_smoke.prompt.txt`):
```text
# test: Login Smoke Test
# type: ui
# mode: soft
# tags: smoke, login
go to https://app.example.com
login with username ${USER_ADMIN} and password ${PASS_ADMIN}
verify all UI components: header logo, left nav (Dashboard, Users, Reports), search box, profile menu
click Users
verify table has columns Name, Email, Role
create a user using generated test data
verify the new user appears in the table
```

### Example API Test (`api_users_crud.prompt.txt`):
```text
# test: API Users CRUD
# type: api
# mode: soft
# tags: smoke, api
base url https://jsonplaceholder.typicode.com
POST /users with body {"name":"John Doe","username":"${USER_ADMIN}","email":"john@example.com"}
verify status 201
GET /users/1
verify status 200 and response has field email
store email as userEmail
GET /users
verify status 200 and every item has id, email
```

### Example Mixed Test (`mixed_create_user_api_verify_ui.prompt.txt`):
```text
# test: Mixed API Create and UI Verify
# type: mixed
# mode: soft
# tags: integration, mixed
base url https://jsonplaceholder.typicode.com
POST /users with body {"username":"${USER_ADMIN}","role":"ADMIN"}
verify status 201
store id as createdUserId
go to https://app.example.com
login with username ${USER_ADMIN} and password ${PASS_ADMIN}
verify all UI components: header logo, left nav
```

### Supported Headers
| Header | Description | Default |
|---|---|---|
| `# test: <name>` | Descriptive test name for reports and TestNG | File name |
| `# type: <ui\|api\|mixed>` | Test category | `ui` |
| `# mode: <soft\|fail-fast>` | Assertion mode (`soft` continues on assertion failures; `fail-fast` aborts immediately) | `soft` |
| `# lock: <resourceName>` | Serialization mutex preventing concurrent execution of conflicting tests | None |
| `# tags: <tag1, tag2>` | Tags for filtering | None |

### Formatting Rules
- Blank lines and `//` comments are ignored.
- Numbered (`1. step`) or bulleted (`- step`, `* step`) prefixes are automatically stripped.
- Indented lines (2+ spaces or tabs) are treated as multi-line continuations of the preceding step.
- Variables and secrets must use `${NAME}` syntax. Literal passwords are automatically detected, warned, and rejected in strict mode (`-Dprompts.strict=true`).

---

## 4. Closed Action Vocabulary

The framework validates every generated plan step against a closed action vocabulary:

### UI Actions
- `NAVIGATE{url}`: Navigates page to URL (checked against `ai.allowedHosts`).
- `CLICK{target}`: Clicks element.
- `FILL{target, value}`: Types text into input field.
- `SELECT{target, option/value/label}`: Selects dropdown option.
- `CHECK{target}` / `UNCHECK{target}`: Checks or unchecks checkboxes.
- `PRESS{key}`: Presses keyboard key.
- `HOVER{target}`: Moves mouse over element.
- `WAIT_FOR{target|text|url, state}`: Waits for element, text, or URL.
- `UPLOAD{target, file}`: Sets input files for file upload.
- `SWITCH_TAB{index|title}`: Switches active browser tab.
- `STORE_TEXT{target, as}`: Extracts element text and stores in test variables.
- `SCREENSHOT{name}`: Takes a named screenshot.

### UI Verifications
- `VERIFY_VISIBLE{target}`: Element is visible.
- `VERIFY_HIDDEN{target}`: Element is hidden.
- `VERIFY_TEXT{target, expected, match=exact|contains|regex}`: Text matches expected.
- `VERIFY_URL{expected, match}`: Current page URL matches expected.
- `VERIFY_TITLE{expected}`: Page title matches expected.
- `VERIFY_COUNT{target, expected, op}`: Matches element count.
- `VERIFY_TABLE{target, columns[], rows?}`: Verifies table header columns.
- `VERIFY_ENABLED{target}` / `VERIFY_DISABLED{target}`: Element enabled or disabled.
- `VERIFY_COMPONENTS{items[{name, role}]}`: Expands each component and verifies visibility.

### API Actions
- `API_BASE{url}`: Sets active API base URL (checked against `ai.allowedHosts`).
- `API_REQUEST{method, path, headers, query, body}`: Sends HTTP request.
- `API_STORE{jsonPath|header, as}`: Stores JSON response property or header into variable store.

### API Verifications
- `VERIFY_STATUS{expected}`: Verifies HTTP response status code.
- `VERIFY_JSON{jsonPath, expected, op}`: Verifies JSON response field value.
- `VERIFY_FIELD_EXISTS{jsonPath}`: Verifies JSON field exists and is non-null.
- `VERIFY_EVERY_ITEM_HAS{arrayPath, fields[]}`: Verifies array elements contain expected fields.
- `VERIFY_HEADER{name, expected}`: Verifies HTTP response header.
- `VERIFY_RESPONSE_TIME{maxMs}`: Verifies response time in milliseconds.

### Data Actions
- `GENERATE_DATA{type, count, as}`: Generates synthetic test data with concurrency-safe suffixes.
- `SET_VAR{name, value}`: Sets a runtime variable.

### Target Locators
Targets follow semantic priority: `role` &rarr; `label` &rarr; `placeholder` &rarr; `text` &rarr; `testId` &rarr; `css`. CSS selectors are only accepted if explicitly specified in the prompt file.

---

## 5. Configuration

Add or override configuration properties in `config.properties` or via `-D` flags:

```properties
ai.provider=mock                 # mock | anthropic | openai
ai.model=                        # optional override (defaults: claude-3-5-sonnet-20241022 or gpt-4o)
ai.apiKeyEnv=AI_API_KEY          # name of environment variable containing API key
ai.timeoutSeconds=60             # LLM call timeout
ai.maxConcurrentRequests=2       # LLM concurrency limiter with rate-limit backoff
ai.replan=false                  # bypass cached plan and force LLM re-planning
ai.healWriteBack=false           # write healed locators back to cached plan
ai.allowedHosts=app.example.com,api.example.com,jsonplaceholder.typicode.com
prompts.dir=src/test/resources/prompts
prompts.threads=1                # thread count for parallel execution
prompts.strict=false             # fail immediately on suspected literal passwords
prompts.screenshots=failures     # failures | all | none
prompts.maxSteps=60              # plan safety limit
prompts.stepTimeoutMs=30000
prompts.testTimeoutMinutes=10
```

---

## 6. Execution Commands

### Sequential execution of all prompt files:
```powershell
mvn test -DsuiteXmlFile=src/test/resources/testng/prompts.xml
```

### Parallel execution (e.g. 4 threads):
```powershell
mvn test -DsuiteXmlFile=src/test/resources/testng/prompts.xml -Dprompts.threads=4
```

### Run a single prompt file:
```powershell
mvn test -DsuiteXmlFile=src/test/resources/testng/prompts.xml -Dprompts.include=ui_login_smoke.prompt.txt
```

### Filter by tag, headless, qa environment:
```powershell
mvn test -DsuiteXmlFile=src/test/resources/testng/prompts.xml -Dprompts.tags=smoke -Denv=qa -Dheadless=true
```

### Dry run (plan only, no execution):
```powershell
mvn test -DsuiteXmlFile=src/test/resources/testng/prompts.xml -Dprompts.dryRun=true
```

### Force re-plan with LLM:
```powershell
mvn test -DsuiteXmlFile=src/test/resources/testng/prompts.xml -Dai.replan=true
```

---

## 7. Plan Caching & Determinism

When a prompt file is first executed, the framework computes its SHA-256 hash and caches the validated plan under `target/ai-plans/<promptHash>.json`. Subsequent runs replay the cached plan with **zero LLM calls**, guaranteeing:
- Deterministic execution without LLM variance.
- High speed and zero API token cost.
- Immediate offline unit and regression testability.

---

## 8. Bounded Self-Healing

When a UI action fails due to locator timeout or element not found:
1. `AiFailureAnalyzer` classifies the error (timeout, locator change, unhandled exception).
2. The framework generates up to **2** resilient alternative locators (e.g. `getByText` or aria attributes).
3. The step is retried with the alternative target.
4. If successful, the step is marked `HEALED`, recording the old vs new target in the report.
5. **Verifications are NEVER healed**: Assertion failures indicate real product defects or regressions and must always fail the test.

---

## 9. Security & Secret Protection

- Credentials in prompts must be formatted as `${VAR_NAME}`.
- Secret values are resolved at execution time from environment variables or `secrets.properties`.
- Secret values are **never sent to the LLM** in planning prompts.
- All logs, traces, HTML reports, and screenshot names automatically mask registered secret values as `********`.
- Navigation and API calls to hosts not in `ai.allowedHosts` are blocked with a `SecurityException`.

---

## 10. MCP (Model Context Protocol) Integration

The framework implements the standard MCP specification over Stdio and HTTP transports.

### Available MCP Tools:
- `run_prompt_file`: Executes a single prompt file with optional `dryRun` and `replan`.
- `run_prompt_suite`: Runs discovered prompt files in parallel.
- `get_prompt_run_status`: Returns execution status and metrics for a run ID.
- `get_prompt_run_report`: Returns HTML and JSON report paths and data.
- `get_failed_tests`: Returns real failure list from the latest execution.
- `run_test`: Programmatically executes a TestNG test class.
- `run_test_suite`: Executes a TestNG XML suite with path traversal protection.
- `generate_prompt_file`: Creates a new draft prompt file from requirement text.

### Running MCP Servers:
```powershell
# Run Stdio Server
mvn compile exec:java -Dexec.mainClass="framework.mcp.McpStdioServer"

# Run HTTP Server (binds 127.0.0.1:8080)
mvn compile exec:java -Dexec.mainClass="framework.mcp.McpHttpServer"
```
