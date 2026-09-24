# Database Management Guide (Flyway & PostgreSQL)

This document covers local database provisioning, migration workflows, resets, and environment-specific data seeding for the **Code Master** backend.

---

## 1. Prerequisites

### Manually create the codematser_db database in Postgre SQL SHELL
CREATE DATABASE codemaster_db;

Ensure you have a local PostgreSQL instance installed and running:

* **Engine:** PostgreSQL 16+
* **Default Port:** `5432`
* **Default Database:** `codemaster_db`
* **Default User:** `postgres`

### Connection String Format
`jdbc:postgresql://localhost:5432/codemaster_db`

### Store Password

inside the .env store your local postgre password

`LOCAL_DB_PASSWORD=your_postgres_password`

### Running Migrations

# Run via Flyway (add -Dflyway.user=... too if your local Postgres role isn't "postgres")

./mvnw flyway:migrate -Dflyway.user="your_username_here" -Dflyway.password="your_password_here"

# Run via Spring boot

./mvnw clean compile spring-boot:run

### Resetting the Local Database

Open Your bash Terminal  and run the followng command:
./mvnw flyway:clean -Dflyway.password="your_local_postgres_password"

### Alternitively
Open your SQL SHELL Terminal and run psql as user postgres: psql -U postgres

The past the following:

-- Terminate any active backend connections
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'codemaster_db' AND pid <> pg_backend_pid();

-- Drop and recreate
\c postgres
DROP DATABASE IF EXISTS codemaster_db;
CREATE DATABASE codemaster_db;
\c codemaster_db

---

## 2. Test database (required for `DevSeedDataIntegrationTest`)

`DevSeedDataIntegrationTest` (`backend/src/test/java/.../persistence/`) verifies the
dev seed script's actual content by running real, non-transactional deletes and
rewriting `flyway_schema_history` directly — that can't be wrapped in a rollback
the way the rest of the test suite's writes are, since it has to exercise real
Flyway migration behavior. Running it against `codemaster_db` will delete every
project with `github_owner = 'codemaster'` (the dev-seed projects) as a side
effect and not restore them — it did exactly that once already.

**It runs against its own dedicated database, `codemaster_test_db`, never
`codemaster_db`.** One-time setup:

```bash
psql -U postgres -c "CREATE DATABASE codemaster_test_db;"

./mvnw flyway:migrate \
  -Dflyway.user="your_username_here" \
  -Dflyway.password="your_password_here" \
  -Dflyway.url="jdbc:postgresql://localhost:5432/codemaster_test_db"
```

This applies the base schema only (not the dev seed data — the test seeds and
cleans that up itself, per test method). Every other test in the suite already
runs against `codemaster_db` safely, wrapped in a Spring-managed transaction
that's rolled back after each test — this is the one exception, because it
specifically needs to test what happens *outside* a transaction.
