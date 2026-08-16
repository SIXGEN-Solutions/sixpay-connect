# DA-10.3 — Password history

DA-10.3 stores historical hashes separately in `security_password_history`.
The configured `PasswordPolicy.historySize` controls how many previous hashes
are retained.

Replacement flow:

```text
proposed raw password
  -> PasswordPolicy.validate(...)
  -> lock current LOCAL credential
  -> PasswordEncoder.matches(raw, current hash)
  -> PasswordEncoder.matches(raw, N recent historical hashes)
  -> reject on reuse
  -> encode proposed password
  -> archive current hash
  -> prune history to N
  -> replace current credential
```

The existing administrative reset path is the first integration point. The
future user-owned change-password use case must reuse the same `PasswordHistoryPort`.
