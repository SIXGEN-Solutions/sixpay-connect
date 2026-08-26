$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir "../..")
$Backend = Join-Path $RepoRoot "backend"
$ReportDir = Join-Path $RepoRoot "target/readiness"
$Report = Join-Path $ReportDir "phase5-readiness-report.md"

New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

function Invoke-Gate {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    Write-Host "==> $Name"
    & $Command

    if ($LASTEXITCODE -ne 0) {
        throw "Readiness gate failed: $Name"
    }
}

Push-Location $Backend
try {
    Invoke-Gate "Full backend verification" {
        mvn -U clean verify
    }

    Invoke-Gate "Phase 5 readiness guards" {
        mvn -U -pl bootstrap -am `
          "-Dtest=com.sixpay.bootstrap.readiness.*" `
          test
    }
}
finally {
    Pop-Location
}

$ExternalVariables = @(
    "TRESORPAY_BASE_URL",
    "TRESORPAY_CLIENT_ID",
    "AMPLITUDE_BASE_URL",
    "AMPLITUDE_TOKEN_URL",
    "AMPLITUDE_CLIENT_ID",
    "ACCOUNTING_API_BASE_URL",
    "ACCOUNTING_API_CLIENT_ID",
    "SIXPAY_NOTIFICATION_OPERATIONS_ADMIN_EMAIL"
)

$Missing = @()

foreach ($Variable in $ExternalVariables) {
    if ([string]::IsNullOrWhiteSpace(
        [Environment]::GetEnvironmentVariable($Variable)
    )) {
        $Missing += $Variable
    }
}

$SandboxStatus =
    if ($Missing.Count -eq 0) {
        "INPUTS_PRESENT_NOT_YET_CERTIFIED"
    }
    else {
        "BLOCKED_EXTERNAL"
    }

$Lines = @(
    "# Phase 5 readiness report",
    "",
    "- Generated: $(Get-Date -Format o)",
    "- MODULAR_MONOLITH_READINESS: PASS",
    "- EXTERNAL_SANDBOX_CERTIFICATION: $SandboxStatus",
    "- PRODUCTION_READINESS: NOT_ASSERTED",
    "",
    "## External inputs"
)

if ($Missing.Count -eq 0) {
    $Lines += "- Required environment variables are present."
    $Lines += "- Their correctness and provider acceptance still require sandbox execution."
}
else {
    foreach ($Variable in $Missing) {
        $Lines += "- MISSING: ``$Variable``"
    }
}

$Lines += @(
    "",
    "## Important",
    "",
    "This report deliberately does not print secret values.",
    "A PASS for the modular-monolith gate does not imply external provider certification."
)

Set-Content -Path $Report -Value $Lines -Encoding UTF8

Write-Host ""
Write-Host "Readiness report: $Report"
Write-Host "MODULAR_MONOLITH_READINESS=PASS"
Write-Host "EXTERNAL_SANDBOX_CERTIFICATION=$SandboxStatus"
