# Code Masters API Design (v2 — Phase 1 Foundation)

## 0. What changed in this revision

The team now has a 2-week build window feeding into the official hackathon dates, not
a 3-day sprint. This revision keeps the same Layer 1 → Layer 2 discipline as v1
(differentiator still does not get built until discovery/community/GitHub integration
works end-to-end) but stops under-specifying things that a 2-week team can and should
get right the first time:

- **Project ownership is now modeled.** `project_maintainers` replaces the implicit,
  undefined "owner/maintainer action" language from v1.
- **Sync is a trackable job**, not a fire-and-forget `202` with nowhere to check status.
- **Claims are explicitly multi-claimant** (a decision, not an accident) with a visible
  count, and the pointless `expiresAt` TTL mechanic is dropped.
- **Auth transport is decided**: server-side session cookie, not left as "session or
  token."
- **Comments are moderatable** (edit/delete) instead of write-only.
- **Maintainers can correct local classification** (difficulty, beginner-friendly) when
  the scoring gets it wrong — this was previously not possible for any local field.
- **Search results carry an explicit `resultType`** instead of asking the frontend to
  infer project-vs-issue from field shape.
- **Pagination is one shape everywhere**, including `/search`.
- **Cookie transport is decided**: same-origin via a Next.js proxy, not CORS (§4.1).
- **GitHub rate-limit handling is decided**: authenticated app token + conditional
  requests + manual-trigger sync for Phase 1 (§5.1).

Everything here is still Layer 1. Verification workflows, scoring algorithms, and
matching intelligence are Phase 2 and are deliberately *not* touched in this revision —
see §11.

---

## 1. Product direction

Code Masters is the discovery and community layer on top of GitHub: users discover
South African/African open-source projects, understand what help is needed, discuss it
on Code Masters, then go to GitHub to make the actual contribution. Code Masters does
not become another GitHub.

Core domain resources:

- **Project** — a listed open-source repository.
- **Issue** — a contribution opportunity linked to a GitHub issue.
- **Comment** — Code Masters discussion attached to a project or issue.
- **User** — a developer identity connected to GitHub.
- **ProjectMaintainer** — a Code Masters user's management relationship to a project.
- **Claim** — a lightweight signal that a contributor intends to work on an issue.
- **SyncJob** — a trackable record of a GitHub synchronization run.

## 2. Recommended differentiator (unchanged, deferred)

Basic issue discovery is a crowded category (Up For Grabs, CodeTriage, FirstIssue.dev,
Good First Issue). The differentiator is **local context**: a trusted South
African/African open-source map that makes local projects visible, explains where help
is needed, and turns discovery into an actual contribution path. This stays Phase 2 —
see §11.

## 3. Endpoint groups

### Public discovery

- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `GET /api/v1/projects/{projectId}/issues`
- `GET /api/v1/issues/{issueId}`
- `GET /api/v1/issues/{issueId}/claims`
- `GET /api/v1/projects/{projectId}/comments`
- `GET /api/v1/issues/{issueId}/comments`
- `GET /api/v1/users/{username}`
- `GET /api/v1/search`

### Authenticated community actions

- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me`
- `POST /api/v1/projects`
- `PATCH /api/v1/projects/{projectId}` — maintainer only
- `POST /api/v1/projects/{projectId}/maintainers` — owner only, invite by username
- `DELETE /api/v1/projects/{projectId}/maintainers/{userId}` — owner only
- `POST /api/v1/projects/{projectId}/comments`
- `POST /api/v1/issues/{issueId}/comments`
- `PATCH /api/v1/comments/{commentId}` — author only
- `DELETE /api/v1/comments/{commentId}` — author or project maintainer
- `PATCH /api/v1/issues/{issueId}` — maintainer only, local classification override
- `POST /api/v1/issues/{issueId}/claim`
- `DELETE /api/v1/issues/{issueId}/claim`

### GitHub integration

- `GET /api/v1/auth/github`
- `GET /api/v1/auth/github/callback`
- `POST /api/v1/auth/logout`
- `POST /api/v1/projects/{projectId}/issues/sync` — maintainer only, returns a job
- `GET /api/v1/sync-jobs/{jobId}` — poll job status

### Operations

- `GET /api/v1/health`

## 4. Authentication

**Decision: server-side session, delivered as a secure, HttpOnly, SameSite=Lax
cookie.** Not a bearer JWT.

Rationale for a 2-week build with a Spring Boot backend and a Next.js frontend on the
same top-level domain (or a proxied path): sessions need no client-side token storage,
no refresh-token rotation logic, and are trivially revocable (delete the session row)
if a demo account needs to be logged out. A bearer-token approach buys nothing here and
costs the team a refresh flow they don't have two weeks to get right.

Consequences the team should build to:
- State-changing requests (`POST`, `PATCH`, `DELETE`) require a CSRF token, issued on
  session creation and expected in an `X-CSRF-Token` header. `GET` requests do not.
- Sessions are stored server-side in a `sessions` table in the same PostgreSQL
  database — not Redis. The architecture already has one datastore (per the product
  definition's architecture diagram in §31); adding Redis buys negligible latency
  benefit at this scale and costs the team a second piece of infrastructure to
  provision, secure, and keep running through the demo. `POST /auth/logout` and
  admin-forced logout both just delete the row.
- Public browsing requires no session.
- Required for: submitting a project, editing a project, managing maintainers,
  commenting, editing/deleting own comments, claiming/releasing an issue, editing a
  profile, triggering sync, overriding issue classification.

GitHub OAuth client secrets are never exposed to the frontend; the callback exchange
happens server-side only.

### 4.1 Transport: same-origin via Next.js proxy, not CORS

**Decision: the frontend proxies all `/api/v1/*` calls to the backend through Next.js
rewrites, so the browser always sees the API as same-origin.** In `next.config.js`:

```js
async rewrites() {
  return [{ source: '/api/v1/:path*', destination: `${process.env.BACKEND_URL}/api/v1/:path*` }];
}
```

This is why `SameSite=Lax` is sufficient — the browser never makes a cross-origin
request to the API, so the cookie is always sent. It also means the team never has to
configure CORS, `Access-Control-Allow-Credentials`, or a `SameSite=None; Secure`
cookie, all of which are easy to get subtly wrong under deadline pressure. The same
rewrite pattern works in local dev (`BACKEND_URL=http://localhost:8080`) and in
production behind a shared domain or reverse proxy — no environment-specific auth
branching. If the team later needs a genuinely separate API domain (e.g. a mobile
client), that's a deliberate Phase 2+ decision to add CORS + `SameSite=None`, not a
default to build against now.

## 5. GitHub is the source of truth (unchanged)

Store the GitHub issue number, URL, title/body excerpt, labels, and locally computed
discovery metadata. "Contribute" always sends the user to the real GitHub issue. Code
Masters never becomes a second issue tracker or a version-control platform.

### 5.1 GitHub API rate limits

Unauthenticated GitHub API requests are capped at 60/hour — too low to sync real
projects during a demo, let alone during 2 weeks of development. Decision:

- The backend calls the GitHub API using a **GitHub App installation token or a
  fine-grained PAT** stored server-side (never in frontend code), giving 5,000
  requests/hour.
- Sync uses **conditional requests** (`If-None-Match` with the stored `ETag`) so
  unchanged resources return `304` and don't count meaningfully against the quota.
- `POST /projects/{id}/issues/sync` is manually triggered per project for Phase 1 —
  no background scheduler yet. This keeps quota usage predictable and demo-safe; an
  automatic refresh cadence (e.g. nightly) is a Phase 1.5/2 addition once real usage
  patterns are known.
- If GitHub responds `403`/`429` (rate limited), the sync job is marked `failed` with
  `errorMessage` set to a human-readable reason and `retryAfter` populated from
  GitHub's `Retry-After`/`X-RateLimit-Reset` header (see updated `SyncJob` schema).
  The frontend surfaces this rather than retrying silently.
- Per Rule 5 (§9 of the product definition): a failed or rate-limited sync never
  removes or blanks existing project/issue data — the last successful sync's data
  keeps serving reads.

## 6. Data model

### users
id, github_id, username, display_name, avatar_url, email, bio, location, skills
(text[]), reputation, created_at, updated_at

### projects
id, github_owner, github_repo, github_url, name, slug, description, primary_language,
languages (text[]), category, connection, license, stars, forks, open_issues,
contributors, has_beginner_friendly_issues (denormalized, recomputed on issue
sync/classification change), last_activity_at, verified, verified_at, created_at,
updated_at

### project_tags
project_id, tag

### project_countries
project_id, country_code

> Normalized rather than a `text[]` column so `country=ZA` filtering can use a plain
> index instead of an array-contains scan. Same pattern as `project_tags`.

### project_maintainers
id, project_id, user_id, role (`owner` | `maintainer`), created_at

> The user who submits a project via `POST /projects` is inserted here automatically
> with role `owner`. This table is what `PATCH /projects/{id}` authorization actually
> checks against — v1 referenced "owner/maintainer" without a table to back it.
> Unique constraint on (project_id, user_id).

### issues
id, project_id, github_issue_id, github_issue_number, github_url, title, body_excerpt,
status, difficulty, is_beginner_friendly, difficulty_overridden_by_user_id (nullable),
maintainer_response_score, project_health_score, freshness_score, contribution_score,
created_at, updated_at

> `difficulty_overridden_by_user_id` records when a maintainer manually corrected the
> auto-classification, so sync doesn't silently clobber a human judgment call on
> re-sync.

### comments
id, user_id, project_id (nullable), issue_id (nullable), body, edited, deleted_at
(nullable), created_at, updated_at

> Check constraint: exactly one of (project_id, issue_id) is non-null. Deletes are
> soft (`deleted_at` set); the API never returns a deleted comment's body — it's
> filtered at the query layer, not the presentation layer, so there's no risk of a
> client bug leaking it.

### claims
id, issue_id, user_id, status (`active` | `released` | `completed`), note (nullable),
created_at, updated_at

> No `expires_at`. v1's TTL mechanic implied a reaper job that was never specified
> anywhere else in the docs — dropped rather than half-built. Claims stay active until
> the user releases them or a maintainer marks the issue closed.
> Partial unique index: one `active` claim per (issue_id, user_id) — a user can't
> double-claim the same issue, but **multiple different users can each hold an active
> claim on the same issue.** This is a deliberate product decision (see §7) not an
> oversight.

### sync_jobs
id (uuid), project_id, status (`accepted` | `running` | `completed` | `failed`),
started_at, completed_at (nullable), error_message (nullable), retry_after (nullable),
issues_created_count, issues_updated_count, created_at

### sessions
id, user_id, csrf_token, created_at, expires_at, last_seen_at

## 7. Claim semantics — decided

Claims are **signals of interest, not a lock.** Any number of authenticated users may
hold an active claim on the same issue simultaneously. The issue detail view shows the
full list of claimants (oldest first) and a `claimCount`. This mirrors how open-source
work actually happens — "I'm looking at this too" is common and useful information,
not a conflict to prevent. A maintainer can still see who's interested and coordinate
in the comment thread. If usage during the build reveals real confusion, a "primary
claimant" concept can be layered on in Phase 2 without a breaking API change (it would
just be an additional field on the existing claim list).

## 8. The main demo flow

1. `GET /projects?country=ZA`
2. User opens a project — `GET /projects/{id}`
3. Frontend loads `GET /projects/{id}/issues?difficulty=beginner`
4. User opens `GET /issues/{issueId}` — sees claim count and discussion
5. User comments — `POST /issues/{issueId}/comments`
6. User signals intent — `POST /issues/{issueId}/claim`
7. User clicks the GitHub URL and makes the real contribution on GitHub

Bonus beats that are now possible and worth including in the demo since the API
supports them: a maintainer inviting a co-maintainer live, and a maintainer correcting
an issue's difficulty label in front of the judges to show the system isn't a black
box.

## 9. What should NOT be built in Phase 1

Still out of scope for this revision: Git hosting, pull requests, code review, a custom
Git implementation, a full LMS, a rewards marketplace, a leaderboard, a custom
messaging system, a full AI tutor, microservices, project verification workflows,
scoring-algorithm intelligence beyond the placeholder fields already in `issues`.

These become Phase 2+ items once Phase 1 is demonstrably working end-to-end.

## 10. Implementation order for a four-person team

Same track split as v1, with the new surfaces slotted in:

- **Person 1 (API/domain):** Project, Issue, Comment, Maintainer, Claim controllers,
  DTOs, validation, CSRF middleware.
- **Person 2 (persistence):** PostgreSQL schema above, JPA entities/repositories,
  migrations, the partial unique index on claims.
- **Person 3 (GitHub integration):** GitHub API client, project import, issue sync,
  `sync_jobs` lifecycle, session store.
- **Person 4 (frontend integration):** Next.js pages, auth flow against the session
  cookie + CSRF header, demo UX.

Agree the OpenAPI contract (attached) before writing controllers. The frontend builds
against the contract while the backend is being built.

## 11. Phase boundary

Phase 1 is done when the full demo flow in §8 works against real GitHub data,
including maintainer management, comment moderation, and sync job polling. Only then
does the team pick up Phase 2 (verification workflow, scoring algorithms, contribution
intelligence) — that will be a separate revision of these documents so Phase 1 doesn't
get destabilized while Phase 2 is designed.
