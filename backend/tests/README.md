# SIXPAY CONNECT — Test Foundation

## 1. Purpose

This module defines the common backend testing foundation for SIXPAY CONNECT
and hosts tests that require multiple bounded contexts or the assembled
application.

The repository follows the testing principle:

```text
Module-owned behavior
        ↓
tested inside the owning module

Cross-module / assembled behavior
        ↓
tested inside backend/tests