## Persistence ownership

Integration owns no production business tables.

Outbox records are owned and persisted by the originating business module.
Integration only claims and transports those records through the configured
internal or Kafka transport. Test-only migration fixtures do not define
production ownership.
