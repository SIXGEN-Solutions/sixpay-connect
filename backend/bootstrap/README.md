# Bootstrap Module

## Purpose

Bootstrap is the executable composition module for SIXPAY CONNECT.

It assembles the business and platform modules, loads runtime configuration,
starts the Spring Boot application and exposes the application entry point.

Bootstrap owns application composition and runtime wiring. It does not own
business-domain rules, business persistence or provider-specific mappings.
