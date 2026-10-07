<#
.SYNOPSIS
    Advanced Allure Report and History Management Script for Playwright Test Automation Framework.

.DESCRIPTION
    Automates Allure report generation, history trend preservation, and interactive report serving.
    - Scans for the latest execution folder under 'reports/allure/' or custom suite folders.
    - Copies previous execution history to maintain trend charts, retry history, duration metrics, and categories.
    - Generates clean, production-grade Allure HTML reports.
    - Automatically opens or serves the report in default browser.

.PARAMETER SuiteName
    Optional filter for a specific test suite (e.g. "UserSuite", "AdminSuite", "RegressionSuite").
    If omitted, detects the most recent execution across all suites.

.PARAMETER ExecutionDir
    Explicit path to a specific execution folder (e.g. "reports/allure/UserSuite_2026-10-07_11-45-20").

.PARAMETER BaseAllurePath
    The base directory where suite runs are located. Default: "reports/allure".

.PARAMETER Action
    Operation mode:
      'GenerateAndOpen' (Default) - Preserves history, generates report, and opens in browser.
      'GenerateOnly'             - Preserves history and generates report without launching browser.
      'OpenOnly'                 - Opens existing latest generated report without re-generating.
      'Serve'                    - Directly serves results using 'allure serve'.

.PARAMETER Port
    Port number for the Allure HTTP server. Default: 8088.

.EXAMPLE
    .\run-allure.ps1
    .\run-allure.ps1 -SuiteName "UserSuite"
    .\run-allure.ps1 -Action "GenerateOnly"
    .\run-allure.ps1 -Port 9090
#>

[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$SuiteName,

    [Parameter(Position = 1)]
    [string]$ExecutionDir,

    [string]$BaseAllurePath = "reports/allure",

    [ValidateSet("GenerateAndOpen", "GenerateOnly", "OpenOnly", "Serve")]
    [string]$Action = "GenerateAndOpen",

    [int]$Port = 8088
)

$ErrorActionPreference = "Stop"

# ==============================================================================
# Helpers & Styling
# ==============================================================================
function Write-Banner {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "             ALLURE REPORT & HISTORY MANAGEMENT ENGINE                          " -ForegroundColor Cyan
    Write-Host "================================================================================" -ForegroundColor Cyan
}

function Write-Info([string]$msg) {
    Write-Host "[INFO]  $msg" -ForegroundColor White
}

function Write-Success([string]$msg) {
    Write-Host "[SUCCESS] $msg" -ForegroundColor Green
}

function Write-Warn([string]$msg) {
    Write-Host "[WARN]  $msg" -ForegroundColor Yellow
}

function Write-Err([string]$msg) {
    Write-Host "[ERROR] $msg" -ForegroundColor Red
}

# ==============================================================================
# Resolve Root Directory
# ==============================================================================
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (Test-Path (Join-Path $ScriptDir "pom.xml")) {
    $ProjectRoot = $ScriptDir
} elseif (Test-Path (Join-Path (Split-Path $ScriptDir -Parent) "pom.xml")) {
    $ProjectRoot = Split-Path $ScriptDir -Parent
} else {
    $ProjectRoot = (Get-Location).Path
}

Set-Location $ProjectRoot

Write-Banner

# ==============================================================================
# Check Allure CLI Availability
# ==============================================================================
$AllureCmd = Get-Command "allure" -ErrorAction SilentlyContinue
if (-not $AllureCmd) {
    Write-Err "Allure CLI command 'allure' was not found in System PATH."
    Write-Host ""
    Write-Host "How to install Allure CLI on Windows:" -ForegroundColor Yellow
    Write-Host "  1. Using Scoop:      scoop install allure" -ForegroundColor Gray
    Write-Host "  2. Using Chocolatey:  choco install allure-commandline -y" -ForegroundColor Gray
    Write-Host "  3. Manual Download:  https://github.com/allure-framework/allure2/releases" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

# ==============================================================================
# Resolve Base & Target Execution Directory
# ==============================================================================
$ResolvedBasePath = Join-Path $ProjectRoot $BaseAllurePath

if (-not (Test-Path $ResolvedBasePath)) {
    # Check fallback legacy path
    $LegacyResultsPath = Join-Path $ProjectRoot "reports/allure-results"
    if (Test-Path $LegacyResultsPath) {
        Write-Warn "Base path '$ResolvedBasePath' not found. Using legacy path '$LegacyResultsPath'."
        $TargetResultsDir = $LegacyResultsPath
        $TargetReportDir = Join-Path $ProjectRoot "reports/allure-report/latest"
        $TargetExecutionDir = $ProjectRoot
        $CurrentSuite = "DefaultSuite"
        $ExecDate = Get-Date -Format "yyyy-MM-dd"
        $ExecTime = Get-Date -Format "HH:mm:ss"
    } else {
        Write-Err "No Allure execution directories found in '$ResolvedBasePath' or '$LegacyResultsPath'."
        Write-Host "Please run tests first to generate results: mvn test" -ForegroundColor Yellow
        exit 1
    }
} else {
    # Scan all execution folders in reports/allure/
    $AllExecDirs = Get-ChildItem -Path $ResolvedBasePath -Directory | Sort-Object Name -Descending

    if ($SuiteName) {
        $FilteredDirs = $AllExecDirs | Where-Object { $_.Name -like "$SuiteName*" }
    } else {
        $FilteredDirs = $AllExecDirs
    }

    if ($ExecutionDir) {
        $TargetExecutionDir = (Resolve-Path $ExecutionDir).Path
    } elseif ($FilteredDirs -and $FilteredDirs.Count -gt 0) {
        $TargetExecutionDir = $FilteredDirs[0].FullName
    } else {
        Write-Err "No matching execution directory found for SuiteName: '$SuiteName' in '$ResolvedBasePath'."
        exit 1
    }

    # Parse Suite Name and Timestamp from folder name: <SuiteName>_<yyyy-MM-dd>_<HH-mm-ss>
    $FolderName = Split-Path -Leaf $TargetExecutionDir
    $Parts = $FolderName -split "_"
    if ($Parts.Count -ge 3) {
        $CurrentSuite = ($Parts[0..($Parts.Count - 3)]) -join "_"
        $ExecDate = $Parts[$Parts.Count - 2]
        $ExecTime = ($Parts[$Parts.Count - 1] -replace "-", ":")
    } else {
        $CurrentSuite = $FolderName
        $ExecDate = (Get-Item $TargetExecutionDir).CreationTime.ToString("yyyy-MM-dd")
        $ExecTime = (Get-Item $TargetExecutionDir).CreationTime.ToString("HH:mm:ss")
    }

    $TargetResultsDir = Join-Path $TargetExecutionDir "allure-results"
    $TargetReportDir  = Join-Path $TargetExecutionDir "allure-report"
    $TargetHistoryDir = Join-Path $TargetExecutionDir "history"
}

# ==============================================================================
# Display Execution Information
# ==============================================================================
Write-Info "Suite Name       : $CurrentSuite"
Write-Info "Execution Date   : $ExecDate"
Write-Info "Execution Time   : $ExecTime"
Write-Info "Execution Folder : $TargetExecutionDir"
Write-Info "Results Folder   : $TargetResultsDir"
Write-Info "Report Folder    : $TargetReportDir"
Write-Info "Action Mode      : $Action"
Write-Info "Server Port      : $Port"
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor Gray

# ==============================================================================
# Action: OpenOnly
# ==============================================================================
if ($Action -eq "OpenOnly") {
    if (-not (Test-Path $TargetReportDir)) {
        Write-Err "Report folder does not exist: $TargetReportDir. Run with Action 'GenerateAndOpen' first."
        exit 1
    }
    Write-Info "Opening existing Allure report in browser..."
    allure open $TargetReportDir --port $Port
    exit 0
}

# ==============================================================================
# Action: Serve
# ==============================================================================
if ($Action -eq "Serve") {
    if (-not (Test-Path $TargetResultsDir)) {
        Write-Err "Results folder does not exist: $TargetResultsDir."
        exit 1
    }
    Write-Info "Starting Allure serve on $TargetResultsDir..."
    allure serve $TargetResultsDir --port $Port
    exit 0
}

# ==============================================================================
# History Preservation Logic (GenerateAndOpen / GenerateOnly)
# ==============================================================================
if (Test-Path $ResolvedBasePath) {
    # Find the latest previous execution folder (excluding the current one)
    $PreviousRuns = Get-ChildItem -Path $ResolvedBasePath -Directory | 
                    Where-Object { $_.FullName -ne $TargetExecutionDir } | 
                    Sort-Object Name -Descending

    $PreviousRun = $null
    if ($CurrentSuite) {
        $PreviousRun = $PreviousRuns | Where-Object { $_.Name -like "$CurrentSuite*" } | Select-Object -First 1
    }
    if (-not $PreviousRun) {
        $PreviousRun = $PreviousRuns | Select-Object -First 1
    }

    if ($PreviousRun) {
        Write-Info "Found previous execution run: $($PreviousRun.Name)"
        
        # Priority 1: allure-report/history, Priority 2: history
        $PrevHistoryDir = Join-Path $PreviousRun.FullName "allure-report\history"
        if (-not (Test-Path $PrevHistoryDir)) {
            $PrevHistoryDir = Join-Path $PreviousRun.FullName "history"
        }

        if (Test-Path $PrevHistoryDir) {
            $DestHistoryDir = Join-Path $TargetResultsDir "history"
            if (-not (Test-Path $DestHistoryDir)) {
                New-Item -ItemType Directory -Path $DestHistoryDir -Force | Out-Null
            }

            Write-Info "Copying previous history from '$PrevHistoryDir' to '$DestHistoryDir'..."
            Copy-Item -Path (Join-Path $PrevHistoryDir "*") -Destination $DestHistoryDir -Recurse -Force
            Write-Success "History trends successfully preserved."
        } else {
            Write-Warn "Previous execution had no generated history. Starting new history baseline."
        }
    } else {
        Write-Info "No previous execution found for suite '$CurrentSuite'. Starting fresh history baseline."
    }
}

# ==============================================================================
# Ingest / Synchronize Staged Results
# ==============================================================================
$DefaultResultsDir = Join-Path $ProjectRoot "reports/allure-results"
if ((Test-Path $DefaultResultsDir) -and ($DefaultResultsDir -ne $TargetResultsDir)) {
    $RawFiles = Get-ChildItem -Path $DefaultResultsDir -File -ErrorAction SilentlyContinue
    if ($RawFiles -and $RawFiles.Count -gt 0) {
        Write-Info "Syncing $($RawFiles.Count) result artifacts from '$DefaultResultsDir' into '$TargetResultsDir'..."
        if (-not (Test-Path $TargetResultsDir)) {
            New-Item -ItemType Directory -Path $TargetResultsDir -Force | Out-Null
        }
        Copy-Item -Path (Join-Path $DefaultResultsDir "*") -Destination $TargetResultsDir -Recurse -Force
    }
}

# ==============================================================================
# Validate Results
# ==============================================================================
if (-not (Test-Path $TargetResultsDir)) {
    Write-Err "Allure results folder does not exist: $TargetResultsDir"
    exit 1
}

$ResultFiles = Get-ChildItem -Path $TargetResultsDir -File -ErrorAction SilentlyContinue
if (-not $ResultFiles) {
    Write-Warn "No result files found in '$TargetResultsDir'. Test execution may not have generated results."
}

# ==============================================================================
# Generate Report (Compatible with Allure 2.x and 3.x)
# ==============================================================================
Write-Info "Generating Allure report..."

# Clean old target report directory to ensure fresh generation across all CLI versions
if (Test-Path $TargetReportDir) {
    Remove-Item -Path $TargetReportDir -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Executing: allure generate `"$TargetResultsDir`" -o `"$TargetReportDir`"" -ForegroundColor DarkCyan

allure generate $TargetResultsDir -o $TargetReportDir

if (-not (Test-Path $TargetReportDir)) {
    Write-Err "Report generation failed. Target report folder not created: $TargetReportDir"
    exit 1
}

# Backup generated history to root execution folder
$ReportGeneratedHistory = Join-Path $TargetReportDir "history"
if (Test-Path $ReportGeneratedHistory) {
    if (-not (Test-Path $TargetHistoryDir)) {
        New-Item -ItemType Directory -Path $TargetHistoryDir -Force | Out-Null
    }
    Copy-Item -Path (Join-Path $ReportGeneratedHistory "*") -Destination $TargetHistoryDir -Recurse -Force
    Write-Success "History backup created at: $TargetHistoryDir"
}

Write-Success "Allure Report successfully generated at: $TargetReportDir"
Write-Host ""

# ==============================================================================
# Open Report
# ==============================================================================
if ($Action -eq "GenerateAndOpen") {
    Write-Info "Starting Allure web server on port $Port and launching browser..."
    allure open $TargetReportDir --port $Port
} else {
    Write-Success "Allure report generation finished. Run 'run-allure.ps1 -Action OpenOnly' to view."
}
