# Local development

## Prerequisites

- Java 21
- Docker Desktop
- Node.js 24 and npm

## Start infrastructure

From the repository root:

```powershell
Copy-Item .env.example .env
docker compose up -d --wait
docker compose ps
```

The copy command is only needed once. Compose ignores `.env.example` unless it is passed explicitly, so the local `.env` file supplies the PostgreSQL variables while remaining excluded from Git. The `--wait` flag waits for the PostgreSQL health check to pass before returning.

This starts PostgreSQL on `localhost:5432`. The local defaults are database user `root` and password `1234`. These credentials are for local development only; use secrets or environment variables in AWS.

## Start the backend

Open `backend` in IntelliJ IDEA and run `CupQueueApplication`. Flyway automatically applies the SQL files in `src/main/resources/db/migration` before Hibernate validates the schema.

Health endpoint: `http://localhost:8080/actuator/health`

The initial security configuration is intentionally open so the end-to-end development flow works before authentication is implemented. It disables sessions and CSRF, explicitly exposes the actuator health endpoints, and currently permits all API requests. Replace the final permit-all rule with authenticated JWT authorization before deployment.

## Run backend tests

The default backend test uses Testcontainers to start an isolated PostgreSQL 17 instance. Docker Desktop must be running, but the project's local `cupqueue` database is not read or modified:

```powershell
cd backend
.\mvnw.cmd test
```

To run the separate smoke test against the PostgreSQL instance started by Compose:

```powershell
cd backend
.\mvnw.cmd "-Dtest=LocalDatabaseApplicationTests" "-Dcupqueue.test.local-database=true" test
```

The local-database test is disabled unless that system property is set, so normal test runs remain isolated.

## Start the frontends

Open the repository in VS Code and use separate terminals:

```powershell
cd frontend/merchant-web
npm install
npm run dev
```

Merchant UI: `http://localhost:5173`

```powershell
cd frontend/customer-web
npm install
npm run dev
```

Customer UI: `http://localhost:5174`

Both Vite development servers proxy relative `/api` and `/actuator` requests to `http://localhost:8080`. Frontend code should use relative URLs such as `/api/orders` during local development.

## Manage seed data

Flyway migrations under `backend/src/main/resources/db/migration` contain schema changes only. Put optional manual seed scripts under `backend/src/main/resources/db/seed`; Spring Boot does not execute that directory automatically.

## Stop infrastructure

```powershell
docker compose down
```

Use `docker compose down -v` only when you intentionally want to delete all local PostgreSQL data.
