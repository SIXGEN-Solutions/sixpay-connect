# Angular raw XSRF token compatibility fix

## Symptom

Angular sends:

```text
Cookie:
XSRF-TOKEN=a4d48244-3a22-405c-8e90-85af19ee5fc7

Header:
X-XSRF-TOKEN=a4d48244-3a22-405c-8e90-85af19ee5fc7
```

Spring Security rejects the request with:

```text
Invalid CSRF token found
HTTP 403
```

## Cause

`CookieCsrfTokenRepository` and Angular agree on the cookie/header names, but
Spring Security's default CSRF request handler uses XOR/BREACH token handling.

For this SIXPAY Angular SPA contract, Angular sends the raw cookie token back in
the `X-XSRF-TOKEN` header. The request handler must therefore resolve the raw
header value.

## Fix

Keep:

```java
CookieCsrfTokenRepository.withHttpOnlyFalse()
```

and configure:

```java
csrf.csrfTokenRequestHandler(
    new CsrfTokenRequestAttributeHandler()
);
```

This preserves CSRF validation and only changes how the submitted token is
resolved.

Do **not** disable CSRF and do **not** exempt administration endpoints.

## Regression coverage

The focused security auto-configuration test proves:

1. matching raw `XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header => accepted;
2. mismatching header => `403 Forbidden`.

## Validation

From `backend/`:

```bash
mvn -pl security \
  -Dtest=SixpaySecurityAutoConfigurationTest \
  test

mvn -pl security -am test
mvn clean package
```

Then restart the backend and retry an administration mutation.
