# Shared Kernel Module

## Purpose

Shared Kernel contains intentionally shared domain primitives used by multiple
SIXPAY CONNECT business modules.

It provides stable abstractions such as aggregate roots, domain events,
domain exceptions and shared value objects.

New domain behavior must remain in the owning module. Shared Kernel should grow
only when a concept is genuinely shared and its ownership is approved.
