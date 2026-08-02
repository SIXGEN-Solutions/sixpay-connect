$ErrorActionPreference = "Stop"

$RepositoryRoot = Resolve-Path(
    Join-Path $PSScriptRoot "../.."
)
$BackendRoot = Join-Path $RepositoryRoot "backend"

Write-Host "== SIXPAY CONNECT / Payment Phase 3 final validation =="
Write-Host "Repository: $RepositoryRoot"

Push-Location $BackendRoot

try {
    Write-Host "== Build environment =="
    java -version
    mvn --version

    Write-Host "== Unit, architecture and contract validation =="
    mvn --batch-mode --no-transfer-progress `
        clean verify `
        -pl payment `
        -am

    if ($LASTEXITCODE -ne 0) {
        throw "Payment unit validation failed."
    }

    Write-Host "== Integration, concurrency and coverage validation =="
    mvn --batch-mode --no-transfer-progress `
        clean verify `
        -Pfull-tests,coverage `
        -pl payment `
        -am

    if ($LASTEXITCODE -ne 0) {
        throw "Payment integration validation failed."
    }

    $RequiredPaths = @(
        "payment/target/surefire-reports",
        "payment/target/failsafe-reports",
        "payment/target/site/jacoco/jacoco.xml"
    )

    foreach ($RequiredPath in $RequiredPaths) {
        if (-not (Test-Path $RequiredPath)) {
            throw "Missing validation output: $RequiredPath"
        }
    }

    Write-Host "Payment Phase 3 validation completed successfully."
}
finally {
    Pop-Location
}
