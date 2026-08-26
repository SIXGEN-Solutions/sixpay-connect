## Persistence ownership

The tests module owns no production tables or Flyway baseline. It may contain
test-only migration fixtures used to validate another module, but production
schema ownership always remains with the owning backend module.
