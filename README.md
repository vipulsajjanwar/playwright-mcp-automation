# Enterprise AI-Enabled Playwright Java Automation Framework

[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://www.oracle.com/java/)
[![Playwright Java](https://img.shields.io/badge/Playwright-1.49.0-green.svg)](https://playwright.dev/java/)
[![TestNG](https://img.shields.io/badge/TestNG-7.10.2-orange.svg)](https://testng.org/)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

A production-ready, enterprise-grade test automation framework built from scratch using **Java 21, Maven, Playwright Java, TestNG, REST Assured, JDBC, Apache POI, ExtentReports, Logback, AI features, and MCP (Model Context Protocol) integration**.

Designed as a modern, high-performance replacement for legacy Selenium POM frameworks while offering context-isolated parallel execution, automatic Playwright tracing, AI-driven test data generation & failure analysis, and external AI agent accessibility via MCP.

---

### Author
Vipul Sajjanwar

## 🏗 Framework Architecture

```text
                               AI Agent / IDE
                                     │
                                     ▼
                            MCP Interface Layer
                                     │
       ┌─────────────────────────────┼─────────────────────────────┐
       ▼                             ▼                             ▼
Test Data Generation        Test Execution Runner        AI Failure & Locator
(AiTestDataGenerator)        (ExecutionManager)           Analysis Engines
       │                             │                             │
       └─────────────────────────────┼─────────────────────────────┘
                                     ▼
                      Playwright Java Framework
                                     │
       ┌─────────────────────────────┼─────────────────────────────┐
       ▼                             ▼                             ▼
   UI Testing                   API Automation               Database Testing
(Playwright Page/Locators)     (REST Assured)                 (JDBC / H2 / HikariCP)
```

---

## 🛠 Technology Stack

* **Core Language**: Java 21+
* **Build System**: Apache Maven 3.9+
* **UI Engine**: Playwright Java 1.49.0
* **Test Runner**: TestNG 7.10.2
* **Build Execution**: Maven Surefire Plugin 3.5.2
* **API Testing**: REST Assured 5.5.0
* **Database Testing**: JDBC, H2 (Embedded Default), HikariCP 6.2.1, PostgreSQL Driver
* **Excel Data-Driven**: Apache POI 5.3.0
* **JSON Processing**: Jackson 2.18.2
* **Reporting**: ExtentReports 5.1.2 (Interactive HTML Reports)
* **Logging**: SLF4J 2.0 + Logback 1.5
* **AI Engine**: Provider-agnostic engine with OpenAI API support & smart local fallback
* **Agent Integration**: Model Context Protocol (MCP) JSON-RPC tool server

---

## 📁 Repository Structure

```text
playwright-mcp-automation/
├── pom.xml                                   # Dependency & build configurations
├── README.md                                  # Documentation
├── Jenkinsfile                               # Jenkins CI pipeline
├── docker/
│   ├── Dockerfile                            # Docker image containerization
│   └── docker-compose.yml                    # Docker Compose runner
├── docs/
│   └── selenium-to-playwright.md             # Migration guide from Selenium
└── src/
    ├── main/
    │   ├── java/framework/
    │   │   ├── api/                          # REST Assured API client & validators
    │   │   ├── ai/                           # AI test data & failure analysis engines
    │   │   ├── base/                         # BaseTest, BasePage, BaseApiTest
    │   │   ├── browser/                      # Playwright, Browser & Context lifecycle
    │   │   ├── components/                   # UI components (Header, Table, Modal)
    │   │   ├── config/                       # Configuration & Environment manager
    │   │   ├── constants/                    # Framework constants
    │   │   ├── database/                     # JDBC Connection pool & Query executor
    │   │   ├── excel/                        # Apache POI reader & writer
    │   │   ├── exceptions/                   # Custom Exception hierarchy
    │   │   ├── execution/                    # TestNG & Retry executors
    │   │   ├── listeners/                    # TestNG listeners & transformers
    │   │   ├── mcp/                          # Model Context Protocol tools & client
    │   │   ├── pages/                        # Page Object Model pages
    │   │   ├── reporting/                    # ExtentReports manager & loggers
    │   │   └── utils/                        # Page, Wait, Element & Trace utilities
    │   └── resources/
    │       └── logback.xml                   # Logging layout configuration
    └── test/
        ├── java/tests/
        │   ├── ui/                           # UI test classes
        │   ├── api/                          # API test classes
        │   ├── database/                     # JDBC Database test classes
        │   └── integration/                  # Data-driven & AI test classes
        └── resources/
            ├── config/                       # Environment property files (qa, staging, prod)
            ├── testdata/                     # Excel test data files
            └── testng/                       # TestNG XML suite definitions
```

---

## 🚀 Quick Start & Prerequisites

### Prerequisites

* Java Development Kit (JDK) 21 or higher
* Apache Maven 3.9+
* Node.js & Playwright CLI (for browser binary installation)

### 1. Installation

Clone the repository and compile the framework:

```bash
git clone https://github.com/vipulsajjanwar/playwright-mcp-automation.git
cd playwright-mcp-automation
mvn clean test-compile
```

Install Playwright browser binaries:

```bash
npx playwright install --with-deps
```

---

## 💻 Test Execution Commands

### Run Default Smoke Suite (QA Environment)

```bash
mvn clean test -DsuiteXmlFile=src/test/resources/testng/smoke.xml
```

### Run Full Regression Suite in Staging Environment

```bash
mvn clean test -Denv=staging -DsuiteXmlFile=src/test/resources/testng/regression.xml
```

### Run Parallel Execution (4 Parallel Threads)

```bash
mvn clean test -DsuiteXmlFile=src/test/resources/testng/parallel.xml -Dthreads=4
```

### Run API Automation Suite

```bash
mvn clean test -DsuiteXmlFile=src/test/resources/testng/api.xml
```

### Run Database Automation Suite

```bash
mvn clean test -DsuiteXmlFile=src/test/resources/testng/database.xml
```

---

## 📊 Extent HTML Reports & Tracing

* **Interactive HTML Report**: Generated automatically at `target/reports/ExtentReport.html` featuring execution summary, steps, failure screenshots, and system info.
* **Playwright Trace Recordings**: Saved at `target/traces/` on test failure. View any trace using Playwright Inspector:

```bash
npx playwright show-trace target/traces/LoginTest_loginTest_12345.zip
```

---

## 🤖 AI & MCP Capabilities

### AI Features

1. **AI Test Data Generation**: Generate complex structured JSON mock datasets using `AiTestDataGenerator`.
2. **AI Failure Diagnostics**: Analyzes failed locators, stack traces, and DOM states to recommend root causes and fixes using `AiFailureAnalyzer`.
3. **AI Locator Analyzer**: Suggests alternative accessible locators (`getByRole`, `getByLabel`) for brittle selectors.

### MCP (Model Context Protocol) Integration

The framework implements 12 MCP tools (`run_test`, `generate_test_data`, `analyze_failure`, `find_locator`, `validate_api`, `execute_database_query`) allowing external AI coding agents (such as Gemini/Claude/Cursor) to execute tests and inspect framework results programmatically.

---

## 🐳 Docker Execution

Build and run tests inside Docker container:

```bash
docker-compose -f docker/docker-compose.yml up --build
```
