# Code Masters — Phase 1, Round 2 Tickets

Round 2 is the integration work Round 1 deliberately deferred: real persistence
behind the API, real GitHub sync, real login, and the write endpoints/pages the
Definition of Done (product doc §37) actually requires. Same rules as Round 1:
every ticket's contract is copied verbatim from `codemasters-api-spec.yaml` v2.1
or `codemasters-api-design.md` v2 — never paraphrased — and every ticket names
what it depends on so nobody starts on a contract that isn't settled yet.

**Assumed starting state**, based on the current repo plus the corrections
already discussed: DB-01.1–01.7 done (all 5 migrations + seed data + docs, with
the two Mockito-based tests still owed a real-database rewrite — that debt is
paid off in DB-02.5 below, not repeated here). API-01.1–01.6 done (all four
read endpoints, backed by `MockDataStore`). GH-01.1–01.7 done (client, DTOs,
pagination, conditional requests, rate-limit handling, contract documented in
code). FE-01.1, .3, .5, .7 done; FE-01.2 dropped (superseded — the real backend
already works, a mock server has no purpose left); FE-01.6 done per the
contract corrections just discussed; **FE-01.4 (homepage) is still not built**
and is carried into this file as the first frontend ticket, not skipped.

---

## Two decisions this file had to make instead of assuming — flag both to the team

**1. Who owns JPA entities — the source docs disagree with each other.**
`phase1-round1-tickets.md`'s DB-01.1 says *"Person 1 owns entity mapping once
this lands"* (i.e. the API person). `codemasters-api-design.md` §10 says
*"Person 2 (persistence): PostgreSQL schema above, **JPA entities/repositories**"*
(i.e. the DB person). These directly contradict each other, and neither
document was updated to resolve it. **This file assigns entities/repositories
to the Persistence (DB) track**, matching the design doc's implementation-order
table, on the reasoning that the DB person already knows every column/constraint
firsthand from writing the migrations, and it keeps a clean boundary: DB owns
the whole persistence layer, API consumes it. If the team prefers the other
split, swap DB-02.1–02.4 and part of API-02.1 between the two people — the
ticket contracts below don't change either way, only who picks them up.

**2. How do *real* GitHub projects get into the database at all?**
The Definition of Done requires real GitHub data, but nothing in Round 1 or
Round 2 as originally scoped puts a real project row in the `projects` table —
`POST /projects` (submission) exists in the spec but isn't in the demo walkthrough
(design doc §8), and building a full submission flow costs real time. Two
options, pick one as a team before GH-02.3:
- **(a) Recommended for time:** hand-write a `V6__real_project_seed.sql` (or a
  `prod`/`demo`-profile seed) inserting a handful of real, hand-picked
  `github_owner`/`github_repo` rows directly — no new endpoint, no new UI, and
  GH-02.3's sync job then populates real issues for them.
- **(b)** Build `POST /projects` for real (API-02.7 below has it written out,
  in case the team chooses this route) plus a small frontend submission form.
Recommendation: (a) now, (b) only if there's real time left after every
Required ticket below is done.

---

## Priority key

Every ticket is tagged against the product doc's Definition of Done (§37) and
demo walkthrough (design doc §8):
- **Required** — the demo literally does not work without this.
- **Bonus** — explicitly called out in the product doc as a nice-to-have
  "bonus beat" (§8), not required for a passing Phase 1.

---

## Track: Persistence (DB person)

### DB-02.1 — JPA entities + repositories: `users`, `sessions`, `projects`, `project_tags`, `project_countries`
**Depends on:** DB-01.1, DB-01.2 (the migrations already exist; this maps Java onto them). **Est:** 4h. **Priority: Required.**

**Goal:** Real JPA entities for the tables with no dependency on anything else,
plus their Spring Data repositories.

**Deliverables:**
- `@Entity` classes for `User`, `Session`, `Project`, mapped **exactly** onto
  DB-01.1/.2's column names and types — do not rename a column while mapping it;
  if a column genuinely needs to change, say so in the team channel first, per
  the original migration ticket's own rule.
- `project_tags`/`project_countries` mapped as `@ElementCollection` lists on
  `Project` (matching how the domain DTOs already expose `tags`/`countryCodes`
  as plain lists) — not as separate entity classes, since neither table has its
  own identity beyond the parent project.
- `UserRepository`, `ProjectRepository` (Spring Data `JpaRepository`), each with
  the specific finder methods the API track will need: `findByGithubId`,
  `findBySlug`, and enough for `ProjectRepository` to support the same
  filters API-01.3 already implements in Java (language, category, tag,
  country, `hasBeginnerFriendlyIssues`) — via `@Query` or Spring Data derived
  queries, whichever the assignee is more comfortable with; get correctness
  first, elegance second.

**Acceptance criteria:**
- `./mvnw compile` succeeds with all three entities present.
- A `@DataJpaTest` proves: saving a `User`, saving a `Project` with tags and
  country codes, and reading both back with the collections intact.

**Contract:** `ProjectRepository`'s filter methods are what API-02.1 replaces
`MockDataStore.projects()` with — their method signatures should be written
with that swap in mind (return `Project` entities or a `Page<Project>`, not a
DTO — API-02.1 does the entity→DTO mapping).

---

### DB-02.2 — JPA entities + repositories: `project_maintainers`, `issues`
**Depends on:** DB-02.1, DB-01.3. **Est:** 4h. **Priority: Required.**

**Goal:** The two tables hanging off `projects` (and, for maintainers, `users`).

**Deliverables:**
- `@Entity` for `ProjectMaintainer` (composite relationship to `Project` +
  `User`, plus `role`), enforcing the same `(project_id, user_id)` uniqueness
  the DB migration already has — don't rely on the DB constraint alone to catch
  a duplicate; let the repository/service layer check first so a duplicate
  invite fails with a clean `409`, not a raw SQL exception.
- `@Entity` for `Issue`, mapped onto every column in DB-01.3's migration
  exactly, including the placeholder score fields (`maintainer_response_score`
  etc.) even though nothing populates them yet.
- `ProjectMaintainerRepository`, `IssueRepository` — the latter needs finder
  methods supporting API-01.5's existing filters (difficulty, label, status,
  scoped to a project), plus a lookup by `(project_id, github_issue_number)`
  for GH-02.3's sync upsert logic.

**Acceptance criteria:** `@DataJpaTest` proving the `(project_id, user_id)`
unique constraint on maintainers is enforced, and that an issue can be found by
its `(project_id, github_issue_number)` pair.

**Contract:** `IssueRepository`'s lookup-by-github-issue-number method is what
GH-02.3 calls to decide "insert new issue" vs. "update existing issue" during
sync.

---

### DB-02.3 — JPA entities + repositories: `comments`, `claims`
**Depends on:** DB-02.1, DB-02.2, DB-01.4. **Est:** 4h. **Priority: Required.**

**Goal:** The two trickiest-constraint tables, same as in Round 1 — give them
their own ticket so the constraint logic is fully tested, not squeezed in.

**Deliverables:**
- `@Entity` for `Comment`: nullable `Project`/`Issue` associations, matching
  the "exactly one of the two is set" DB check constraint. Enforce this in the
  service layer too (API-02.3), not just the DB — a client sending both or
  neither should get a clean `400`, not a raw constraint-violation stack trace.
- `@Entity` for `Claim`: `status` enum, nullable `note`. **Do not rely on JPA
  to enforce the partial unique index** (`one active claim per (issue_id,
  user_id)`) — Hibernate has no native concept of a *partial* unique index, so
  this must stay a raw DB constraint (already in DB-01.4's migration) that the
  service layer catches and translates to a `409`, not something the entity
  mapping can express directly. Flag this explicitly in a code comment so
  nobody "fixes" it into a plain `@UniqueConstraint` later and silently breaks
  multi-claimant support.
- `CommentRepository`, `ClaimRepository`.

**Acceptance criteria:** `@DataJpaTest` proving: a comment with both
`project`/`issue` set is rejected (service-layer check), and a second `active`
claim by the same user on the same issue is rejected by the real partial
index — not mocked, an actual insert against a real test database.

**Contract:** This is also where DB-01.4's still-outstanding Round 1 test debt
finally gets closed for real — DB-02.5 below explicitly supersedes
`MockCheckConstraintTest`/`DevSeedDataMockTest` with real-database tests.

---

### DB-02.4 — JPA entity + repository: `sync_jobs`
**Depends on:** DB-02.1, DB-01.5. **Est:** 1.5h. **Priority: Required** (GH-02.3 needs it).

**Goal:** Small, isolated — same reasoning as Round 1's equivalent ticket.

**Deliverables:** `@Entity` for `SyncJob` (uuid pk), `SyncJobRepository` with a
simple `findById`.

**Acceptance criteria:** `@DataJpaTest` proving a `SyncJob` row round-trips with
its uuid intact.

**Contract:** `SyncJobRepository` is what GH-02.3 and API's `GET
/sync-jobs/{jobId}` both read/write — one source of truth for job status.

---

### DB-02.5 — Close out the Round 1 test debt with real database tests
**Depends on:** DB-02.1 through DB-02.4. **Est:** 2h. **Priority: Required.**

**Goal:** Replace the two still-mocked tests flagged repeatedly since the
DB-01.1 review — `MockCheckConstraintTest` and `DevSeedDataMockTest` — with
tests that hit a real database, now that real entities/repositories exist to
test through.

**Deliverables:** Delete both mocked test files. Replace with `@DataJpaTest`
(or `@SpringBootTest` against a real local/test Postgres) proving: the
`connection` check constraint on `projects` really rejects an invalid value,
and the seed dataset (DB-01.6) loads under the `dev` profile without violating
any constraint from DB-02.1–02.4.

**Acceptance criteria:** Both new tests fail if you temporarily comment out
the constraint they're testing (a good sanity check to run once, then revert)
— proving they test the real thing, not Mockito's opinion of the real thing.

---

## Track: API/domain (you)

### API-02.1 — Swap `MockDataStore` for real repositories
**Depends on:** DB-02.1, DB-02.2. **Est:** 4h. **Priority: Required.**

**Goal:** The actual "Round 2" moment for every existing read endpoint — same
external contract, real data underneath.

**Deliverables:** `ProjectQueryService`'s four methods (`search`,
`getProjectDetail`, `getProjectIssues`, `getIssueDetail`) now query
`ProjectRepository`/`IssueRepository` instead of `MockDataStore`, mapping JPA
entities to the existing `Project`/`Issue`/`ProjectDetail`/`IssueDetail`
records — **the DTOs themselves do not change**, only what fills them.
`MockDataStore` and `MockDataStoreTest` are deleted in this same ticket, not
left behind "just in case."

**Acceptance criteria:** Every existing test from API-01.3–.6
(`ProjectQueryServiceTest`, `ProjectDetailTest`, `ProjectIssuesTest`,
`IssueDetailTest`) still passes, now running against a real (test) database
instead of in-memory data — if a test needs to change at all, it should only be
*how test data is set up* (seeding a test DB row vs. constructing a record),
never the assertions themselves.

**Contract:** `GET /projects`, `/projects/{id}`, `/projects/{id}/issues`,
`/issues/{id}` all keep their exact existing response shapes — this is what
makes the frontend need zero changes for this ticket specifically.

---

### API-02.2 — Auth resolver + CSRF enforcement, replacing `permitAll()`
**Depends on:** GH-02.2 (session creation must exist before enforcing it). **Est:** 4h. **Priority: Required.**

**Goal:** Replace API-01.1's temporary `SecurityConfig` with the real rule from
design doc §4: `GET` requests stay public; `POST`/`PATCH`/`DELETE` require a
valid session cookie **and** a matching `X-CSRF-Token` header.

**Deliverables:**
- A `SecurityFilterChain` permitting all `GET`s, requiring authentication for
  everything else.
- A mechanism (filter or `HandlerMethodArgumentResolver`) that resolves "who is
  the current user" from the `CODEMASTERS_SESSION` cookie via
  `SessionRepository` (DB-02.1), for controllers to inject as a method
  parameter — e.g. `@AuthenticatedUser User currentUser`.
- CSRF check: the session's stored `csrf_token` (DB-01.1's `sessions.csrf_token`
  column) must match the `X-CSRF-Token` header on every state-changing request,
  or the request is rejected before it reaches the controller.
- New `ApiException` codes: `UNAUTHENTICATED` (401, no/invalid session) and
  `CSRF_TOKEN_MISMATCH` (401 or 403 — pick one and note it in the PR, since the
  spec doesn't specify which).

**Acceptance criteria:** A `POST` with no session cookie → 401
`UNAUTHENTICATED`. A `POST` with a valid session but a missing/wrong
`X-CSRF-Token` → rejected. A `POST` with both correct → reaches the controller
(even if the controller itself doesn't exist yet — test against a throwaway
endpoint if every real write endpoint is still pending).

**Contract:** Every write-endpoint ticket below (API-02.3 onward) can assume
"the current authenticated user" is available as an injected parameter — none
of them re-implement session/CSRF checking themselves.

---

### API-02.3 — Comments: `POST`/`GET` on both projects and issues
**Depends on:** API-02.2, DB-02.3. **Est:** 3h. **Priority: Required.**

**Goal:** The comment half of the demo flow (design doc §8, step 5).

**Deliverables:** `POST /projects/{projectId}/comments` and `POST
/issues/{issueId}/comments`, both taking `CreateCommentRequest { body: string,
1–5000 chars }`, returning `201` with the created `Comment`. `GET
/projects/{projectId}/comments` and `GET /issues/{issueId}/comments`, both
returning `PagedComments` (new DTO: `{ items: Comment[], meta: PageMeta }` —
same shape pattern as `PagedProjects`/`PagedIssues`, add it as its own small
class rather than reusing one of those). Enforce "exactly one of
project/issue" at the service layer (see DB-02.3's note) — a comment always
belongs to whichever parent its URL named, never both.

**Acceptance criteria:** Posting a comment while unauthenticated → 401
(proves API-02.2 is actually wired in, not just built). A body over 5000 chars
→ 400. A successful post appears in the corresponding `GET` list.

**Contract:** `PagedComments` shape is fixed for the frontend's comment list
(FE-02.6).

---

### API-02.4 — Comments: `PATCH`/`DELETE`
**Depends on:** API-02.3. **Est:** 2h. **Priority: Bonus** (comment moderation isn't in the demo walkthrough, but is cheap once API-02.3 exists).

**Goal:** Comment moderation, per design doc's "what changed" list.

**Deliverables:** `PATCH /comments/{commentId}` (author-only, sets `edited:
true`), `DELETE /comments/{commentId}` (author or the parent project's
maintainer — soft delete via `deleted_at`, per DB-01.4's schema). A deleted
comment's body is never returned by any subsequent `GET`, filtered at the
query layer per design doc §6's note, not just hidden in the response mapper.

**Acceptance criteria:** A non-author, non-maintainer attempting to delete →
403. A deleted comment disappears from `GET` comment lists entirely (not shown
with a "[deleted]" placeholder — the spec says the body specifically isn't
returned, doesn't require deciding whether it needs to be flagged as
"present but hidden" or "not listed at all"; latter chosen for simplicity — say
so in the PR if the team wants the alternative).

---

### API-02.5 — Claims: `POST`/`DELETE`/`GET`
**Depends on:** API-02.2, DB-02.3. **Est:** 3h. **Priority: Required.**

**Goal:** The claim half of the demo flow (design doc §8, step 6).

**Deliverables:** `POST /issues/{issueId}/claim` (optional body
`CreateClaimRequest { note?: string, ≤280 chars }`), returning `201` with the
created `Claim`. `DELETE /issues/{issueId}/claim` releases the caller's own
active claim (`204`). `GET /issues/{issueId}/claims` returns the full list,
oldest first, per design doc §7. A second `active` claim by the same user on
the same issue → `409` with a new code, `CLAIM_ALREADY_ACTIVE` (ours to name;
spec doesn't specify it) — caught from the DB-02.3 partial unique index
violation, not pre-checked with a separate query that could race.

**Acceptance criteria:** Two different users can each hold an active claim on
the same issue simultaneously (this is the one to explicitly test — it's the
whole point of design doc §7, and the easiest thing to accidentally break by
treating claims as exclusive). The same user claiming twice → `409`.

**Contract:** `claimCount` on the `Issue`/`IssueDetail` DTOs (already a field,
currently always `0` against mock data) should now reflect the real active
claim count — small addition to API-02.1's mapping if not already covered
there.

---

### API-02.6 — Issue classification override
**Depends on:** API-02.2, DB-02.2. **Est:** 2h. **Priority: Bonus** (explicitly named as a "bonus beat," design doc §8).

**Goal:** Let a maintainer correct `difficulty`/`isBeginnerFriendly` when
automated scoring gets it wrong.

**Deliverables:** `PATCH /issues/{issueId}` with `UpdateIssueRequest {
difficulty?, isBeginnerFriendly? }` (both optional; only provided fields
change), maintainer-only. Sets `difficulty_overridden_by_user_id` so a future
sync (GH-02.3) doesn't silently clobber the human correction — this is the
entire reason that column exists (design doc §6).

**Acceptance criteria:** A non-maintainer → 403. After an override,
`difficulty_overridden_by_user_id` is set to the caller's id.

---

### API-02.7 — Project submission and maintainer-only update
**Depends on:** API-02.2, DB-02.1. **Est:** 3h. **Priority: Bonus**, unless the team picks option (b) from this file's decision section above, in which case **Required.**

**Goal:** `POST /projects` and `PATCH /projects/{projectId}`.

**Deliverables:** `POST /projects` takes `CreateProjectRequest {
githubUrl, connection, category, tags?, countryCodes? }`, creates the project
**and** adds the submitter as `project_maintainers` with role `owner` in one
transaction (design doc's stated behavior — not two separate calls a client
could interleave badly). Duplicate `githubUrl` → 409 `PROJECT_ALREADY_EXISTS`
(ours to name). `PATCH /projects/{projectId}` takes `UpdateProjectRequest`
(category/tags/connection/countryCodes only — GitHub-derived fields like
`stars` are sync's job, not this endpoint's), maintainer-only (any role).

**Acceptance criteria:** A successful `POST /projects` immediately shows the
caller in that project's maintainer list. A duplicate `githubUrl` → 409, not a
raw DB exception.

---

### API-02.8 — Maintainer invite/removal
**Depends on:** API-02.7, DB-02.2. **Est:** 2.5h. **Priority: Bonus** (explicitly named as a "bonus beat," design doc §8).

**Goal:** `POST /projects/{projectId}/maintainers` and `DELETE
/projects/{projectId}/maintainers/{userId}`.

**Deliverables:** Invite by username, owner-only, `role` defaults to
`maintainer` if not specified (per `AddMaintainerRequest`'s spec default).
Already-a-maintainer → 409 `MAINTAINER_ALREADY_EXISTS`. Removal, owner-only,
**refuses to remove the last remaining owner** — 409
`CANNOT_REMOVE_LAST_OWNER` — per the spec's explicit rule.

**Acceptance criteria:** Attempting to remove the sole owner → 409, project
still has that owner afterward. A second invite of the same user → 409.

---

### API-02.9 — `GET /users/me`, `GET /users/{username}`
**Depends on:** API-02.2. **Est:** 1.5h. **Priority: Required** (the frontend's "am I logged in" check needs this; full profile editing does not).

**Goal:** The minimum profile surface the frontend needs to show "logged in as
X" after OAuth.

**Deliverables:** `GET /users/me` (session-authenticated, returns
`UserProfile` — includes `email`/`githubAccess` on top of the public fields).
`GET /users/{username}` (public, returns `PublicUserProfile`). **`PATCH
/users/me` (profile editing) is out of scope for this ticket** — not required
for the demo; pick it up later only if time allows.

**Acceptance criteria:** `GET /users/me` with no session → 401. With a valid
session → the caller's own profile, including `email` (never returned from
the public endpoint).

---

## Track: GitHub integration + auth (GitHub person)

Per design doc §10, session/auth work sits with this track alongside sync —
both involve talking to GitHub's own APIs (OAuth is, after all, another GitHub
API call), so the person already deep in `GitHubClient` is best placed to
extend it.

### GH-02.1 — GitHub OAuth: `GET /auth/github`, `GET /auth/github/callback`
**Depends on:** DB-02.1 (`users`/`sessions` entities). **Est:** 4h. **Priority: Required.**

**Goal:** Real login — the demo flow's step before commenting/claiming can
happen at all.

**Deliverables:** `GET /auth/github` redirects (`302`) to GitHub's OAuth
authorize URL with the app's client id and a generated `state` value (stored
temporarily — session or short-lived cache — and checked on callback to
prevent CSRF on the OAuth flow itself, standard OAuth practice, not optional).
`GET /auth/github/callback` receives `code`+`state`, verifies `state`,
exchanges `code` for a GitHub access token, fetches the GitHub user's profile,
**finds or creates** a `User` row keyed on `github_id`, creates a `Session` row
with a freshly generated `csrf_token`, sets the session cookie
(`CODEMASTERS_SESSION`, HttpOnly per design doc §4) and a **readable**
CSRF cookie (not HttpOnly — the frontend needs to read it to attach the
`X-CSRF-Token` header), then redirects (`302`) to the frontend.

**Acceptance criteria:** A real, manual walkthrough (this one's hard to
automate meaningfully): hitting `/auth/github` in a browser, authorizing on
GitHub's real consent screen, landing back on the frontend with a session
cookie set. A second login by the same GitHub account reuses the existing
`User` row (matched by `github_id`), not a duplicate.

**Contract:** The CSRF cookie's name/format is what FE-02.3 reads to attach
the header — document the exact cookie name chosen in the PR, since the spec
names the *header* (`X-CSRF-Token`) but not a specific cookie name for
delivering the token to the frontend.

---

### GH-02.2 — `POST /auth/logout` + the shared "current user" resolver
**Depends on:** GH-02.1. **Est:** 2h. **Priority: Required.**

**Goal:** Logout, plus the one piece every other Round 2 write-endpoint ticket
depends on: a single, shared way to answer "who is calling this request."

**Deliverables:** `POST /auth/logout` deletes the session row and clears both
cookies. A `SessionRepository`-backed component that, given a request's
session cookie, returns the matching `User` (or empty/expired) — this is the
exact mechanism API-02.2 wires into Spring Security as the auth resolver, so
coordinate directly with whoever picks up API-02.2 rather than building this
in isolation and hoping the shapes match.

**Acceptance criteria:** After logout, the old session cookie no longer
resolves to a user (a subsequent authenticated request with the stale cookie →
401).

---

### GH-02.3 — Wire the GitHub client into real sync: `POST /projects/{id}/issues/sync`, `GET /sync-jobs/{jobId}`
**Depends on:** GH-01.2–.7 (client, done), GH-02.2 (maintainer-only auth), DB-02.2 (issues/projects repos), DB-02.4 (sync_jobs repo). **Est:** 5h. **Priority: Required.**

**Goal:** The actual "Round 2" moment for GitHub data — turns the standalone,
already-tested client into a real, callable feature.

**Deliverables:** `POST /projects/{projectId}/issues/sync`, maintainer-only,
immediately creates a `SyncJob` row (`status: accepted`) and returns it
(`202`), then — synchronously or via a simple background task, whichever is
faster to get right in the time remaining — calls
`GitHubClient.fetchProjectMetadata` and `fetchOpenIssues`, branching on
`GitHubFetchResult`'s three cases exactly as already documented in that
class's own Javadoc:
- **Success** → update the `Project` row's GitHub-derived fields (stars,
  forks, language, etc.), upsert each fetched issue by
  `(project_id, github_issue_number)` (via DB-02.2's lookup method) —
  **skip any issue whose `difficulty_overridden_by_user_id` is set**, per
  design doc §6's explicit rule that sync must not clobber a maintainer's
  manual correction. Mark the job `completed`, set
  `issues_created_count`/`issues_updated_count`.
- **Not modified** → mark the job `completed` with zero counts; existing data
  is already current, per design doc §5.1's conditional-request rationale.
- **Rate limited** → mark the job `failed`, set `error_message` and
  `retry_after` from the typed result — **never blank or remove existing
  project/issue data** on a failed sync (design doc §5.1, "Rule 5").

`GET /sync-jobs/{jobId}` returns the current `SyncJob` row — this is what the
frontend polls.

**Acceptance criteria:** Running sync against a real public repo (or a small
test one) actually populates real issues. Running it a second time immediately
after → the not-modified path, zero new rows. A simulated rate-limit response
→ job `failed`, `retry_after` populated, and a `GET /projects` immediately
after still shows the project's *previous* data unchanged, not blanked.

**Contract:** `SyncJob`'s shape is fixed for FE polling, if a sync-status UI
gets built — not required for the core demo flow, but the ticket exists either
way for a maintainer to trigger sync on a real project (needed regardless, per
this file's decision-2 discussion of how real data gets in).

---

## Track: Frontend

### FE-02.1 — Homepage *(carried over from Round 1 — FE-01.4 was never built)*
**Depends on:** nothing. **Est:** 3h. **Priority: Required.**

**Goal:** Close out the Round 1 gap before adding Round 2 pages on top of a
homepage that's still the default Next.js template.

**Deliverables:** Real hero content framing Code Masters per product doc §2/§4,
entry point into `/projects` — exactly what FE-01.4 originally asked for.

**Acceptance criteria:** The Next.js starter boilerplate (logo, "get started by
editing," Vercel links) is gone.

---

### FE-02.2 — Login UI
**Depends on:** GH-02.1, API-02.9. **Est:** 3h. **Priority: Required.**

**Goal:** "Log in with GitHub" and knowing who's logged in.

**Deliverables:** A login button linking to `GET /auth/github` (a plain
navigation, not a fetch — it's a redirect flow). On page load, call `GET
/users/me`; if it 401s, show the login button; if it succeeds, show the user's
name/avatar and a logout control (calling `POST /auth/logout`, then
refreshing).

**Acceptance criteria:** A full manual login → see profile → logout → see
login button again, round-trip.

**Contract:** Whatever piece of state/context holds "the current user" here is
what FE-02.6/.7 (comment form, claim button) check before showing
write-actions.

---

### FE-02.3 — Extend `lib/api.js` with authenticated writes
**Depends on:** FE-02.2 (needs to know how the CSRF cookie/token is delivered — see GH-02.1's contract note). **Est:** 2h. **Priority: Required.**

**Goal:** One place, same as `listProjects`, for every write call — no
component should hand-roll its own `fetch` with headers.

**Deliverables:** New functions in `lib/api.js`: `postComment`,
`postClaim`/`deleteClaim`, each reading the CSRF token (per GH-02.1's chosen
delivery mechanism) and attaching it as `X-CSRF-Token`, plus `credentials:
"include"` so the session cookie is sent.

**Acceptance criteria:** A call from browser dev tools (or a quick manual test
page) to `postComment` against a real logged-in session succeeds; the same
call with the CSRF header stripped fails with the 401/403 API-02.2 defines.

---

### FE-02.4 — Project detail page
**Depends on:** nothing new — `GET /projects/{id}` has existed since API-01.4. **Est:** 3h. **Priority: Required.**

**Goal:** Consume an endpoint that's been ready and unused since Round 1.

**Deliverables:** A `/projects/[projectId]` page rendering `ProjectDetail`:
project info, maintainers list, featured issues (linking to the issue detail
page, FE-02.5), recent comments.

**Acceptance criteria:** Navigating from a `ProjectCard` (FE-01.7, once its
`connectionStatus`→`verified`/`connection` fix lands) to this page shows real
data end to end.

---

### FE-02.5 — Issue detail page
**Depends on:** nothing new — `GET /issues/{id}` has existed since API-01.6. **Est:** 3h. **Priority: Required.**

**Goal:** Same situation as FE-02.4 — the backend's been ready, waiting.

**Deliverables:** A `/issues/[issueId]` page rendering `IssueDetail`: issue
info, parent project link, comment thread, claim list — this page is where
FE-02.6 and FE-02.7 attach their forms/buttons next.

**Acceptance criteria:** Renders real issue data, including the real GitHub
`githubUrl` as a clickable link — the actual literal last step of the entire
demo flow (design doc §8, step 7).

---

### FE-02.6 — Comment form + list
**Depends on:** API-02.3, FE-02.3, FE-02.5. **Est:** 2.5h. **Priority: Required.**

**Goal:** Design doc §8, step 5.

**Deliverables:** A comment box on the issue detail page (hidden/disabled if
not logged in, per FE-02.2's state), posting via `lib/api.js`'s
`postComment`, and a list rendering `PagedComments`.

**Acceptance criteria:** Posting a comment while logged in appears in the list
without a full page reload.

---

### FE-02.7 — Claim button
**Depends on:** API-02.5, FE-02.3, FE-02.5. **Est:** 2h. **Priority: Required.**

**Goal:** Design doc §8, step 6 — and design doc §7's multi-claimant display.

**Deliverables:** A "claim this issue" button (toggles to "release" if the
current user already has an active claim), and a visible claimant list/count
— **not** framed as "someone already has this," since multiple simultaneous
claims are a deliberate, correct product decision (design doc §7), not an
edge case to hide.

**Acceptance criteria:** Two different logged-in accounts can each claim the
same issue and both appear in the list — worth actually testing with two
accounts, not assuming from the backend test alone.

---

### FE-02.8 — Maintainer tools: invite/remove, issue override
**Depends on:** API-02.6, API-02.8. **Est:** 3h. **Priority: Bonus** (matches the backend tickets it depends on).

**Goal:** Design doc §8's two named "bonus beats" — inviting a co-maintainer
live, and correcting an issue's difficulty in front of judges.

**Deliverables:** On the project detail page (maintainers only, checked
against the already-loaded `ProjectDetail.maintainers` list): an invite-by-
username form, a remove button per maintainer. On the issue detail page
(maintainers only): a small difficulty/beginner-friendly override control.

**Acceptance criteria:** Both actions work end-to-end for an actual maintainer
account; both are simply absent (not just disabled) for a non-maintainer
viewing the same pages.

---

## Rough sequencing

```
DB:  02.1 → 02.2 → 02.3 → 02.4 → 02.5
              (02.1/02.2 also unblock GH-02.1/.3 and API-02.1)

GH:  02.1 → 02.2 → 02.3
      (02.1/.2 block API-02.2 and FE-02.2/.3)

API: 02.1 (needs DB-02.1/.2)
     02.2 (needs GH-02.2)
       → 02.3, 02.5, 02.9 (Required, any order)
       → 02.4, 02.6, 02.7, 02.8 (Bonus, only if time remains)

FE:  02.1 (no dependency — start immediately)
     02.4, 02.5 (no new backend dependency — start immediately)
     02.2 → 02.3 (needs GH-02.1)
     02.6, 02.7 (need their matching API-02.x + FE-02.3)
     02.8 (Bonus, needs its matching API-02.x)
```

Note the two genuinely free starting points for frontend: **FE-02.1
(homepage) and FE-02.4/FE-02.5 (detail pages) need nothing from Round 2's
backend work at all** — those endpoints have existed since Round 1. If the
frontend person is ever blocked waiting on someone else, these three are
always available.

---

## What "done" looks like when this file is finished

Every **Required**-tagged ticket landing means the product doc's Definition of
Done (§37) walkthrough works for real: browse real projects, open one, see
real GitHub data, browse and filter its real issues, open one, read and post
real discussion, log in for real, claim it, click through to the real GitHub
issue. That's Phase 1, complete. Every **Bonus**-tagged ticket is exactly
that — genuinely worth doing if time allows, and genuinely fine to cut for the
pitch without it being a failure, per the product doc's own scope boundary.
