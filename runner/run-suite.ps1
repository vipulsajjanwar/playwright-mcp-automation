param(
    [string[]]$SuiteXmlFiles = @("src/test/resources/testng/smoke.xml"),
    [string[]]$Browsers = @("chrome", "firefox", "webkit", "msedge"),
    [switch]$Parallel = $true,
    [switch]$Headless,
    [switch]$Visible,
    [string]$MavenCommand = "mvn",
    [string]$Goals = "test"
)

$ErrorActionPreference = "Stop"

$projectRoot = (Get-Location).Path

$validBrowsers = @("chromium", "chrome", "firefox", "webkit", "msedge")
foreach ($browser in $Browsers) {
    if ($browser -notin $validBrowsers) {
        throw "Unsupported browser '$browser'. Valid values: $($validBrowsers -join ', ')"
    }
}

$resolvedSuites = @()
foreach ($suite in $SuiteXmlFiles) {
    $suitePath = if ([System.IO.Path]::IsPathRooted($suite)) {
        $suite
    } else {
        Join-Path $projectRoot $suite
    }

    if (-not (Test-Path $suitePath)) {
        throw "Suite XML not found: $suitePath"
    }

    $resolvedSuites += (Resolve-Path $suitePath).Path
}

$headlessValue = if ($Headless) { "true" } elseif ($Visible) { "false" } else { "false" }

Write-Host "Project Root: $projectRoot" -ForegroundColor Cyan
Write-Host "Headless mode: $headlessValue" -ForegroundColor Cyan
Write-Host "Suites: $($resolvedSuites.Count)" -ForegroundColor Cyan
Write-Host "Browsers: $($Browsers -join ', ')" -ForegroundColor Cyan

$jobs = @()

function Invoke-MavenSuite {
    param(
        [string]$Root,
        [string]$SuitePath,
        [string]$Browser
    )

    Push-Location $Root
    try {
        Write-Host "=======================================================" -ForegroundColor Yellow
        Write-Host "Running suite: $SuitePath on browser: $Browser" -ForegroundColor Yellow
        Write-Host "Command: $MavenCommand $Goals -Dbrowser=$Browser -Dheadless=$headlessValue -DsuiteXmlFile=$SuitePath" -ForegroundColor DarkYellow

        & $MavenCommand $Goals "-Dbrowser=$Browser" "-Dheadless=$headlessValue" "-DsuiteXmlFile=$SuitePath"

        if ($LASTEXITCODE -ne 0) {
            throw "Maven command failed for suite '$SuitePath' with browser '$Browser'. Exit code: $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

foreach ($suite in $resolvedSuites) {
    foreach ($browser in $Browsers) {
        if ($Parallel) {
            $job = Start-Job -Name "Run_$($browser)_$([System.IO.Path]::GetFileName($suite))" -ScriptBlock {
                param($root, $suitePath, $browser, $mvnCommand, $goals, $headlessValue)

                Push-Location $root
                try {
                    Write-Host "Running suite: $suitePath on browser: $browser"
                    & $mvnCommand $goals "-Dbrowser=$browser" "-Dheadless=$headlessValue" "-DsuiteXmlFile=$suitePath"

                    if ($LASTEXITCODE -ne 0) {
                        throw "Maven command failed for $suitePath on $browser"
                    }
                }
                finally {
                    Pop-Location
                }
            } -ArgumentList $projectRoot, $suite, $browser, $MavenCommand, $Goals, $headlessValue

            $jobs += $job
        }
        else {
            Invoke-MavenSuite -Root $projectRoot -SuitePath $suite -Browser $browser
        }
    }
}

if ($Parallel) {
    if ($jobs.Count -eq 0) {
        Write-Host "No jobs to run." -ForegroundColor Yellow
        exit 0
    }

    $jobs | Wait-Job | Out-Null

    foreach ($job in $jobs) {
        Receive-Job -Job $job
        Remove-Job -Job $job -Force
    }
}

Write-Host "All requested suite/browser combinations completed." -ForegroundColor Green
