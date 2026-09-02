# Database seed files

Put optional, manually managed seed scripts in this directory.

Flyway only scans `db/migration`, so files placed here are packaged with the application but are not executed automatically. Schema changes must remain in `db/migration`.
