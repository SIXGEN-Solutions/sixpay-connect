"""Static contract checks for local M2M-1 artifacts; no Docker/network required."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class TestLocalKeycloakConfiguration(unittest.TestCase):
    def setUp(self):
        self.realm = json.loads((ROOT / "realm" / "sixpay-m2m-local-realm.json").read_text(encoding="utf-8"))
        self.compose = (ROOT / "docker-compose.yml").read_text(encoding="utf-8")

    def test_realm_is_m2m_only(self):
        self.assertEqual(self.realm["realm"], "sixpay-m2m-local")
        self.assertFalse(self.realm["registrationAllowed"])
        self.assertEqual(self.realm.get("users", []), [])
        self.assertEqual(len(self.realm["clients"]), 1)

    def test_client_is_service_account_only(self):
        client = self.realm["clients"][0]
        self.assertEqual(client["clientId"], "sixpay-partner-test")
        self.assertTrue(client["serviceAccountsEnabled"])
        self.assertFalse(client["standardFlowEnabled"])
        self.assertFalse(client["directAccessGrantsEnabled"])
        self.assertFalse(client["publicClient"])
        self.assertEqual(client["secret"], "${SIXPAY_M2M_TEST_CLIENT_SECRET}")
        self.assertIn("payment.initiate", client["optionalClientScopes"])

    def test_local_introspection_audience_mapper(self):
        client = self.realm["clients"][0]
        mappers = [m for m in client.get("protocolMappers", [])
                   if m.get("name") == "local-test-self-audience"]
        self.assertEqual(len(mappers), 1)
        mapper = mappers[0]
        self.assertEqual(mapper["protocolMapper"], "oidc-audience-mapper")
        self.assertEqual(mapper["config"]["included.client.audience"], "sixpay-partner-test")
        self.assertEqual(mapper["config"]["access.token.claim"], "true")
        self.assertEqual(mapper["config"]["id.token.claim"], "false")

    def test_sixpay_api_audience_for_m2m2(self):
        client = self.realm["clients"][0]
        mappers = [mapper for mapper in client.get("protocolMappers", [])
                   if mapper.get("name") == "local-sixpay-api-audience"]
        self.assertEqual(len(mappers), 1)
        self.assertEqual(mappers[0]["protocolMapper"], "oidc-audience-mapper")
        self.assertEqual(mappers[0]["config"]["included.custom.audience"], "sixpay-api")
        self.assertEqual(mappers[0]["config"]["access.token.claim"], "true")

    def test_payment_scope_declared_and_no_user_auth_modification(self):
        expected_scopes = {
            "payment.initiate",
            "payment.confirmation.read",
            "payment.confirmation.verify",
            "payment.confirmation.resend",
        }
        self.assertEqual({x["name"] for x in self.realm["clientScopes"]}, expected_scopes)
        self.assertEqual(set(self.realm["clients"][0]["optionalClientScopes"]), expected_scopes)
        for scope in self.realm["clientScopes"]:
            self.assertEqual(scope["protocol"], "openid-connect")
            self.assertEqual(scope["attributes"]["include.in.token.scope"], "true")
        self.assertIn("127.0.0.1:", self.compose)
        self.assertIn("--import-realm", self.compose)
        self.assertNotIn("application-oidc.yml", self.compose)
        self.assertNotIn("application-local-auth.yml", self.compose)
        self.assertNotIn("application-hybrid-auth.yml", self.compose)


if __name__ == "__main__":
    unittest.main()
