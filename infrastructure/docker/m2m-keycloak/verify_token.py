#!/usr/bin/env python3
"""Local-only OAuth2 client_credentials smoke test, stdlib only.

Validates issuance via Keycloak's authoritative token introspection endpoint.
Never prints secrets or access tokens. Does not call SIXPAY.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
from pathlib import Path
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit
from urllib.request import ProxyHandler, Request, build_opener

HERE = Path(__file__).resolve().parent
REALM = "sixpay-m2m-local"
CLIENT_ID = "sixpay-partner-test"
SCOPE = "payment.initiate"


def load_local_env(path):
    values = {}
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, val = line.split("=", 1)
            values[key.strip()] = val.strip().strip('"').strip("'")
    return values


def post_token(url, secret, scope, opener):
    auth = base64.b64encode((CLIENT_ID + ":" + secret).encode("utf-8")).decode("ascii")
    fields = {"grant_type": "client_credentials"}
    if scope:
        fields["scope"] = scope
    req = Request(url, data=urlencode(fields).encode("utf-8"),
                  headers={"Authorization": "Basic " + auth,
                           "Content-Type": "application/x-www-form-urlencoded"})
    return opener.open(req, timeout=12)


def main():
    p = argparse.ArgumentParser(description="Verify local Keycloak M2M token issuance")
    p.add_argument("--base-url", default="http://127.0.0.1:8180")
    p.add_argument("--negative", action="store_true", help="Also verify invalid client secret is rejected")
    args = p.parse_args()
    u = urlsplit(args.base_url)
    if u.scheme not in {"http", "https"} or u.hostname not in {"localhost", "127.0.0.1", "::1"} or u.username or u.password or u.query or u.fragment or u.path not in {"", "/"}:
        p.error("--base-url must be a loopback-only HTTP(S) origin with no path")
    env = load_local_env(HERE / ".env")
    secret = os.environ.get("SIXPAY_M2M_TEST_CLIENT_SECRET") or env.get("SIXPAY_M2M_TEST_CLIENT_SECRET")
    if not secret:
        p.error("Set SIXPAY_M2M_TEST_CLIENT_SECRET in local .env or environment")
    token_url = args.base_url.rstrip("/") + "/realms/" + REALM + "/protocol/openid-connect/token"
    introspect_url = args.base_url.rstrip("/") + "/realms/" + REALM + "/protocol/openid-connect/token/introspect"
    opener = build_opener(ProxyHandler({}))
    try:
        with post_token(token_url, secret, SCOPE, opener) as resp:
            token_doc = json.load(resp)
        access_token = token_doc.get("access_token")
        if not access_token or token_doc.get("token_type", "").lower() != "bearer":
            raise RuntimeError("Keycloak did not return a bearer access token")
        auth = base64.b64encode((CLIENT_ID + ":" + secret).encode()).decode("ascii")
        body = urlencode({"token": access_token}).encode("utf-8")
        req = Request(introspect_url, data=body, headers={
            "Authorization": "Basic " + auth,
            "Content-Type": "application/x-www-form-urlencoded"})
        with opener.open(req, timeout=12) as resp:
            inspected = json.load(resp)
        if inspected.get("active") is not True:
            raise RuntimeError("Token introspection: active != true")
        scopes = str(inspected.get("scope", "")).split()
        if SCOPE not in scopes:
            raise RuntimeError("Token introspection: payment.initiate scope missing")
        if inspected.get("client_id") != CLIENT_ID:
            raise RuntimeError("Token introspection: unexpected client_id")
        audiences = inspected.get("aud", [])
        if isinstance(audiences, str):
            audiences = [audiences]
        if "sixpay-api" not in audiences:
            raise RuntimeError("Token introspection: sixpay-api audience missing")
        print("PASS: M2M token issued, active, with payment.initiate, expected client_id and sixpay-api audience")
        if args.negative:
            try:
                with post_token(token_url, secret + "-invalid", SCOPE, opener):
                    raise RuntimeError("Invalid client secret was unexpectedly accepted")
            except HTTPError as exc:
                if exc.code not in {400, 401}:
                    raise RuntimeError("Invalid secret produced unexpected HTTP status " + str(exc.code)) from None
            print("PASS: invalid client secret rejected")
    except HTTPError as exc:
        print("FAIL: Keycloak HTTP status " + str(exc.code) + " (check realm/client/secret)", file=sys.stderr)
        return 1
    except (URLError, OSError, ValueError, RuntimeError) as exc:
        print("FAIL: " + str(exc), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
