\
$ErrorActionPreference = "Stop"

$paths = @(
  "backend/payment/src/main/java/com/sixpay/payment/infrastructure/customer/CustomerVerificationModuleAdapter.java",
  "backend/payment/src/main/java/com/sixpay/payment/infrastructure/customer/package-info.java",
  "backend/payment/src/test/java/com/sixpay/payment/infrastructure/customer/CustomerVerificationModuleAdapterTest.java"
)

foreach ($path in $paths) {
  if (Test-Path $path) {
    Remove-Item $path -Force
    Write-Host "Deleted $path"
  }
}

$directories = @(
  "backend/payment/src/main/java/com/sixpay/payment/infrastructure/customer",
  "backend/payment/src/test/java/com/sixpay/payment/infrastructure/customer"
)

foreach ($directory in $directories) {
  if ((Test-Path $directory) -and
      -not (Get-ChildItem $directory -Force | Select-Object -First 1)) {
    Remove-Item $directory -Force
  }
}

Write-Host "Lot 4.4.3 cleanup completed."
