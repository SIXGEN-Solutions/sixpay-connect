#!/usr/bin/env bash
set -euo pipefail

rm -f   backend/payment/src/main/java/com/sixpay/payment/infrastructure/customer/CustomerVerificationModuleAdapter.java   backend/payment/src/main/java/com/sixpay/payment/infrastructure/customer/package-info.java   backend/payment/src/test/java/com/sixpay/payment/infrastructure/customer/CustomerVerificationModuleAdapterTest.java

rmdir --ignore-fail-on-non-empty   backend/payment/src/main/java/com/sixpay/payment/infrastructure/customer   backend/payment/src/test/java/com/sixpay/payment/infrastructure/customer   2>/dev/null || true

echo "Lot 4.4.3 cleanup completed."
