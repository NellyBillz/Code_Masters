# Database Management Guide (Flyway & PostgreSQL)

This document covers local database provisioning, migration workflows, resets, and environment-specific data seeding for the **Code Master** backend.

---

## 1. Prerequisites

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

# Run via Flyway

./mvnw flyway:migrate -Dflyway.password="your_password_here"

# Run via Spring boot

./mvnw clean compile spring-boot:run

### Resetting the Local Database

Open your SQL SHELL Terminal and run psql as user postgres: psql -U postgres

The past the following:

-- Terminate any active backend connections
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'codemaster_db' AND pid <> pg_backend_pid();

-- Drop and recreate
DROP DATABASE IF EXISTS codemaster_db;
CREATE DATABASE codemaster_db;
\q