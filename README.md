# CupQueue

CupQueue is a Canadian coffee-shop order-ahead and pickup-queue project. It is being built as a portfolio-ready, single-store MVP with separate merchant and customer experiences.

## Repository structure

```text
cup-queue/
├── backend/                  Spring Boot REST API
├── frontend/
│   ├── merchant-web/         Merchant management application
│   └── customer-web/         Mobile-first customer application
├── docs/                     Architecture and development notes
├── .github/workflows/        GitHub Actions CI
└── compose.yml               Local PostgreSQL
```

## Technology stack

- Backend: Java 21, Spring Boot 4.0.8, Spring Security, JPA, Flyway
- Data: PostgreSQL 17
- Frontend: React, TypeScript, Vite
- Delivery: Docker and GitHub Actions

## Run locally

Start PostgreSQL:

```powershell
Copy-Item .env.example .env
docker compose up -d --wait
```

Run the Spring Boot application from IntelliJ IDEA. Then start either frontend from VS Code:

```powershell
cd frontend/merchant-web
npm install
npm run dev
```

See [local development](docs/local-development.md) for both applications,
[API development](docs/api-development.md) for generated OpenAPI documentation,
[architecture](docs/architecture.md) for the design rationale,
and the [database schema](docs/database-schema.md) for table definitions and constraints.

## Current status

Day 1 foundation: project structure, local infrastructure, database migration, Docker image definition, and CI workflow.
