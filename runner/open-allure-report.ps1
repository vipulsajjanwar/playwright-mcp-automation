param(
    [string]$SuiteName,
    [string]$ExecutionDir,
    [string]$ResultsDir,
    [string]$ReportDir,
    [ValidateSet("GenerateAndOpen", "GenerateOnly", "OpenOnly", "Serve")]
    [string]$Action = "GenerateAndOpen",
    [int]$Port = 8088
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootScript = Join-Path (Split-Path $ScriptDir -Parent) "run-allure.ps1"

if (Test-Path $RootScript) {
    & $RootScript -SuiteName $SuiteName -ExecutionDir $ExecutionDir -Action $Action -Port $Port
} else {
    Write-Host "Main runner script not found at $RootScript" -ForegroundColor Red
    exit 1
}
