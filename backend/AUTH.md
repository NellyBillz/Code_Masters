# GitHub OAuth login

The backend exposes `GET /auth/github` and `GET /auth/github/callback` for the real GitHub OAuth web flow.

## Configuration

Set these environment variables before starting the backend:

- `GITHUB_OAUTH_CLIENT_ID` — GitHub OAuth App client ID.
- `GITHUB_OAUTH_CLIENT_SECRET` — GitHub OAuth App client secret.
- `GITHUB_OAUTH_CALLBACK_URL` — callback registered on the OAuth App; defaults to `http://localhost:8080/auth/github/callback`.
- `FRONTEND_URL` — URL to redirect to after login; defaults to `http://localhost:3000`.
- `COOKIE_SECURE` — use `true` in HTTPS environments; defaults to `false` for local HTTP development.
- `SESSION_TTL` — ISO-8601 duration; defaults to `PT168H` (7 days).

## Cookie contract

Successful login sets two cookies:

- `CODEMASTERS_SESSION`: the UUID primary key of the `sessions` row. It is `HttpOnly`, `SameSite=Lax`, and scoped to `/`.
- `CODEMASTERS_CSRF`: the session row's freshly generated `csrf_token`. It is deliberately **not** `HttpOnly`, because FE-02.5 reads this cookie and sends its value in the `X-CSRF-Token` request header. It is `SameSite=Lax` and scoped to `/`.

Both cookies use the configured session TTL. In deployed HTTPS environments, set `COOKIE_SECURE=true`.

## OAuth state

`/auth/github` generates a cryptographically random state value and stores it temporarily in the server-side HTTP session. The callback removes and constant-time compares that value before exchanging the authorization code. The temporary HTTP session is invalidated after successful login.

## Manual acceptance walkthrough

1. Create/configure a GitHub OAuth App whose callback URL matches `GITHUB_OAUTH_CALLBACK_URL`.
2. Start PostgreSQL and the backend with the OAuth environment variables set.
3. Open `http://localhost:8080/auth/github` in a browser and authorize on GitHub.
4. Confirm the browser lands on `FRONTEND_URL` and has `CODEMASTERS_SESSION` plus readable `CODEMASTERS_CSRF` cookies.
5. Log in again with the same GitHub account and verify only one `users` row exists for that GitHub ID. The login uses `ON CONFLICT (github_id) DO UPDATE`, so the existing user is refreshed rather than duplicated.
