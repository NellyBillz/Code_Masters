Contributing to Code Masters

Thanks for helping South African open-source projects grow. Contributions to code, documentation, design, testing, and issue triage are welcome. Please follow our Code of Conduct.

Get started

Fork the repository and clone your fork.

Install Java 21, PostgreSQL 16 or newer, and Node.js with npm.

Create the database codemaster_db in PostgreSQL.

In backend, create a local .env with LOCAL_DB_USERNAME=postgres and LOCAL_DB_PASSWORD=your_password. Do not commit credentials.

Start the backend from backend with ./mvnw spring-boot:run (Windows PowerShell: .\mvnw.cmd spring-boot:run). Flyway applies migrations on startup. The API uses port 8080 by default.

In frontend, copy .env.local.example to .env.local and set BACKEND_URL=http://localhost:8080 and FRONTEND_URL on the backend to http://localhost:3002 for local OAuth redirects. Run npm ci, then npm run dev. Open http://localhost:3002.

GitHub login requires an OAuth App and GITHUB_OAUTH_CLIENT_ID, GITHUB_OAUTH_CLIENT_SECRET, and GITHUB_OAUTH_CALLBACK_URL configured for http://localhost:8080/auth/github/callback. See backend/AUTH.md.

Make a change

Check existing issues or open one describing the proposed change. Comment on an issue before taking a large task so efforts can be coordinated.

Create a focused branch, for example feature/project-search or fix/login-redirect.

Keep changes small, explain the behavior, and update documentation if setup or API behavior changes.

Never commit .env, .env.local, tokens, database passwords, logs, or generated build files.

Before opening a pull request, run npm run lint and npm run build in frontend, and ./mvnw test (or .\mvnw.cmd test on Windows) in backend. Report any failure and its cause in the pull request.

Open a pull request against main describing what changed, how you tested it, and any related issue. Include screenshots for visible UI changes.

Reviewers may request changes. Be considerate and keep discussion focused on the work.
