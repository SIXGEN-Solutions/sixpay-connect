$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location (Join-Path $Root "backend")

Write-Host "[1/4] Customer and Bootstrap verification"
mvn -pl customer,bootstrap -am clean verify
if ($LASTEXITCODE -ne 0) {
    throw "Maven verification failed."
}

Write-Host "[2/4] Forbidden dependency scan"
$ObservationRoot = "customer\src\main\java\com\sixpay\customer\observation"
$Forbidden = Get-ChildItem $ObservationRoot -Recurse -Filter *.java |
    Select-String -Pattern `
        'import com\.sixpay\.payment\.', `
        'Amplitude', `
        'amplitude'

if ($Forbidden) {
    $Forbidden | ForEach-Object { Write-Host $_ }
    throw "Forbidden Customer Observation dependency detected."
}

Write-Host "[3/4] Application/domain framework boundary scan"
$BoundaryViolations = Get-ChildItem `
        "$ObservationRoot\application", `
        "$ObservationRoot\domain" `
        -Recurse -Filter *.java |
    Select-String -Pattern `
        'import org\.springframework\.', `
        'import jakarta\.persistence\.', `
        'import org\.hibernate\.', `
        'Thread\.sleep\('

if ($BoundaryViolations) {
    $BoundaryViolations | ForEach-Object { Write-Host $_ }
    throw "Framework or blocking retry leaked into application/domain."
}

Write-Host "[4/4] Documentation presence"
$Documents = @(
    "documentation\implementation\customer-observation\README.md",
    "documentation\implementation\customer-observation\E2E-ACCEPTANCE-MATRIX.md",
    "documentation\implementation\customer-observation\OPERATIONS-RUNBOOK.md",
    "documentation\implementation\customer-observation\PHASE-CLOSURE-CHECKLIST.md"
)

Set-Location $Root

foreach ($Document in $Documents) {
    if (-not (Test-Path $Document -PathType Leaf)) {
        throw "Missing phase document: $Document"
    }
}

Write-Host "Customer Observation phase validation passed."
