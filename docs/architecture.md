# Architecture

CupQueue starts as a single-store modular monolith. This keeps the first version small enough to finish in one week while still showing clear business boundaries.

## Applications

- `backend`: Spring Boot REST API and business logic.
- `frontend/merchant-web`: desktop-oriented merchant management UI.
- `frontend/customer-web`: mobile-first customer ordering UI.

Both frontends call the same backend API. They are separate applications because employees and customers have different workflows, permissions, navigation, and release needs.

## Backend packages

- `auth`: employee identity, password verification, and JWT authorization.
- `store`: the single store's profile, hours, timezone, and tax settings.
- `catalog`: categories, products, prices, and availability.
- `ordering`: carts, orders, pickup queue, and order status.
- `config`: shared Spring configuration such as security, caching, and CORS.

PostgreSQL is the source of truth. Redis may be introduced later as an optional performance layer if the MVP demonstrates a concrete caching need. Flyway owns database schema changes so every environment creates the same schema in the same order.

Schema migrations do not contain environment-specific seed rows. Optional seed scripts live separately under `backend/src/main/resources/db/seed` and are run manually when needed.

The initial HTTP security chain is stateless and permit-all so frontend-to-backend development can proceed before identity endpoints exist. JWT validation and authenticated route rules must replace the permit-all policy before deployment.

## Database-managed timestamps

PostgreSQL is the only writer of audit timestamps:

- Every timestamped table defines `created_at` and `updated_at` as `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.
- `created_at` is initialized by the default during `INSERT` and is not changed automatically afterward.
- `updated_at` is initialized by the same default, then refreshed by the shared `set_updated_at_timestamp()` trigger function before every `UPDATE`.
- Each table binds its own `<table>_set_updated_at` trigger to that shared function in the Flyway migration that creates or upgrades the table.

JPA entities with these columns extend `DatabaseTimestampedEntity`. Its fields use ordinary read-only column mappings with `insertable = false` and `updatable = false`, and expose getters without setters. Hibernate does not synchronize trigger-generated values automatically; reload or refresh the entity when the newest `updated_at` value is needed immediately after a write.
