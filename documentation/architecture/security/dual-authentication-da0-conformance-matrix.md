# SIXPAY CONNECT — DA-0 Architecture Conformance Matrix

## Scope

This matrix is the compact review artefact for DA-0.

| Concern | Existing state | Target state | Owner | DA lot | Business modules changed? |
|---|---|---|---|---|---|
| Authentication selection | One exclusive mode | Independent Local/OIDC capabilities | Security + frontend auth | DA-2 | No |
| Standalone | Runtime auth mode | Dev/demo only | Frontend | DA-2 | No |
| Local login | Existing | Preserved + hybrid-compatible | Security + frontend auth | DA-3 | No |
| OIDC login | Existing | Preserved + hybrid-compatible | Security + frontend auth | DA-4 | No |
| Canonical identity | Partial/implicit | Unified SIXPAY Principal | Security | DA-1 | No |
| Session restoration | Per exclusive mode | Hybrid-aware normalization | Security + frontend auth | DA-8 | No |
| Authorization | SIXPAY roles | SIXPAY roles/permissions | Security | DA-6 | No functional change |
| External provider claims | Used by OIDC flow | Identity input only | Security adapter | DA-4/DA-5 | No |
| Identity linking | Not formalized | External identity → SIXPAY user | Security | DA-5 | No |
| Login UI | Local OR OIDC | Local AND/OR OIDC | Frontend auth | DA-7 | No |
| Admin auth methods | Not formalized | Manage identities/methods | Administration + security ports | DA-9 | Administration only |
| Audit | Existing security coverage only | Auth lifecycle events | Security/audit | DA-9 | No |
| Provider secrets | Environment-specific | Deployment/Vault | Bootstrap/deployment | DA-4 | No |
| `partner` | Golden business module | Remains golden reference | Partner | N/A | No auth-mode logic |
| `payment` | Business module | Unchanged | Payment | N/A | No auth-mode logic |
| `customer` | Business module | Unchanged | Customer | N/A | No auth-mode logic |
| `accounting` | Business module | Unchanged | Accounting | N/A | No auth-mode logic |
| `reporting` | Business module | Unchanged | Reporting | N/A | No auth-mode logic |
| `incident` | Business module | Unchanged | Incident | N/A | No auth-mode logic |

## Mandatory conformance rules

```text
RULE-DA0-01
Business modules MUST NOT branch on LOCAL vs OIDC.

RULE-DA0-02
Business modules MUST NOT depend on OIDC provider-specific classes or claims.

RULE-DA0-03
Authentication method MUST converge to one canonical SIXPAY identity.

RULE-DA0-04
SIXPAY remains owner of business roles and permissions.

RULE-DA0-05
standalone is not a production authentication mechanism.

RULE-DA0-06
production(local=false, oidc=false) is invalid.

RULE-DA0-07
security assets stay under backend/security.

RULE-DA0-08
partner remains the golden implementation/structure reference, not the parent module of security.

RULE-DA0-09
Existing business APIs and authorization semantics remain unchanged during DA-0.

RULE-DA0-10
No production code change is permitted as part of DA-0.
```
