/**
 * Typed API client (FE-01.3). Every page must import API calls from this
 * module only, no component should call fetch() directly. Field names and
 * types here are kept in sync with codemasters-api-spec.yml (the
 * authoritative contract), so treat that spec as the source of truth if the
 * two ever disagree.
 *
 * All calls go through /api/v1/*, which next.config.js rewrites to
 * `${BACKEND_URL}/api/v1/*` for browser requests, so this file works
 * unchanged against the mock server (FE-01.2) today and the real backend
 * later, with BACKEND_URL as the only thing that ever changes.
 *
 * On the server (SSR, React Server Components, or a plain Node script like
 * scripts/check-api-client.js) there's no browser to proxy through, so we
 * call BACKEND_URL directly instead. Same contract, same single switch,
 * we're just skipping the same-origin hop that only matters for browser
 * cookies (see codemasters-api-design.md §4.1).
 */

/**
 * @typedef {'south_african'|'community_verified'} ProjectConnection
 */

/**
 * @typedef {Object} Project
 * @property {number} id
 * @property {string} name
 * @property {string} [slug]
 * @property {string} description
 * @property {string} githubUrl
 * @property {string} [owner]
 * @property {string} primaryLanguage
 * @property {string[]} [languages]
 * @property {string} [category]
 * @property {string[]} [tags]
 * @property {string[]} [countryCodes]
 * @property {ProjectConnection} connection
 * @property {string} [license]
 * @property {number} [stars]
 * @property {number} [forks]
 * @property {number} [openIssues]
 * @property {number} [contributors]
 * @property {boolean} [hasBeginnerFriendlyIssues]
 * @property {string} [lastActivityAt] ISO date-time
 * @property {boolean} [verified]
 * @property {string|null} [verifiedAt] ISO date-time or null
 * @property {string} createdAt ISO date-time
 * @property {string} updatedAt ISO date-time
 */

/**
 * @typedef {Object} PageMeta
 * @property {number} page
 * @property {number} size
 * @property {number} total
 */

/**
 * @typedef {Object} PagedProjects
 * @property {Project[]} items
 * @property {PageMeta} meta
 */

/**
 * @typedef {Object} ApiErrorBody
 * @property {string} code
 * @property {string} message
 * @property {string} [timestamp]
 * @property {string} [path]
 * @property {Object} [details]
 */

class ApiError extends Error {
  /**
   * @param {number} status
   * @param {ApiErrorBody} body
   */
  constructor(status, body) {
    super((body && body.message) || `API request failed with status ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.code = body && body.code;
    this.body = body;
  }
}

const isServer = typeof window === 'undefined';

function getApiBase() {
  if (isServer) {
    if (!process.env.BACKEND_URL) {
      throw new Error(
        'BACKEND_URL is not set. Copy .env.local.example to .env.local and set it.'
      );
    }
    return `${process.env.BACKEND_URL}/api/v1`;
  }
  return '/api/v1';
}

/**
 * @param {Record<string, unknown>} [paramsObj]
 */
function buildQuery(paramsObj) {
  const qs = new URLSearchParams();
  for (const [key, value] of Object.entries(paramsObj || {})) {
    if (value === undefined || value === null || value === '') continue;
    qs.set(key, String(value));
  }
  const s = qs.toString();
  return s ? `?${s}` : '';
}

/**
 * @typedef {Object} UserProfile
 * @property {number} id
 * @property {string} username
 * @property {string} [displayName]
 * @property {string} [avatarUrl]
 * @property {string} [bio]
 * @property {string} [location]
 * @property {string[]} [skills]
 * @property {number} [projectsCount]
 * @property {number} [contributionsCount]
 * @property {number} [reputation]
 * @property {string} [email] Only present on GET /users/me, never on public profiles.
 * @property {boolean} [githubAccess] Only present on GET /users/me.
 * @property {boolean} [isSiteAdmin] Only present on GET /users/me. Gates the
 *   /admin/* moderation views, absent (falsy) for everyone else.
 */

/**
 * @param {string} path
 * @param {RequestInit} [init]
 */
async function apiFetch(path, init) {
  const { headers: initHeaders, ...restInit } = init || {};

  const res = await fetch(`${getApiBase()}${path}`, {
    // Every write call (postComment, postClaim, deleteClaim, etc.) needs
    // the session cookie sent for the server to know who's making the
    // request. Setting this once here, rather than in each function below
    //, is what "one place for every write call" actually means: a new
    // write function added later gets this for free just by using
    // apiFetch, instead of every caller needing to remember it.
    credentials: 'include',
    // headers is destructured out of init above and merged explicitly, so
    // a caller's Content-Type/X-CSRF-Token headers don't silently clobber
    // the Accept default the way `{ headers: {...}, ...init }` would
    // (init.headers, spread last, would otherwise win outright).
    headers: { Accept: 'application/json', ...initHeaders },
    ...restInit,
  });

  if (!res.ok) {
    /** @type {ApiErrorBody} */
    let body;
    try {
      body = await res.json();
    } catch {
      body = { code: 'UNKNOWN_ERROR', message: res.statusText };
    }
    throw new ApiError(res.status, body);
  }

  // DELETE (and some POSTs) may legitimately return no body (204).
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

/**
 * List and filter projects. Backs FE-01.5/.6 (search, filters, sort,
 * pagination on the /projects page).
 *
 * @param {Object} [params]
 * @param {number} [params.page] Zero-based page index. Default 0.
 * @param {number} [params.size] Page size, 1-50. Default 20.
 * @param {string} [params.q] Search project name, description, topics or owner.
 * @param {string} [params.language] Primary programming language.
 * @param {string} [params.category]
 * @param {string} [params.tag]
 * @param {string} [params.country] e.g. 'ZA'. Default discovery focus is ZA.
 * @param {'relevance'|'recent'|'stars'|'contributors'} [params.sort]
 * @param {boolean} [params.hasBeginnerIssues]
 * @returns {Promise<PagedProjects>}
 */
function listProjects(params) {
  return apiFetch(`/projects${buildQuery(params)}`);
}

/**
 * Public, non-personal, aggregate platform-impact metrics (GET /stats).
 * `totalContributionsCompleted` is the headline figure per the backend's own
 * doc comment, it only increments on an actually-verified merge/confirmation.
 * `totalStars`/`languageBreakdown`/`topProjects` (Impact Dashboard
 * wow-feature, 2026-09-24) are all derived from published projects only.
 *
 * @returns {Promise<{publishedProjects: number, activeProjectsAcceptingContributions: number, totalContributorsEngaged: number, totalActiveClaims: number, totalContributionsCompleted: number, totalStars: number, languageBreakdown: {language: string, projectCount: number}[], topProjects: {id: number, name: string, slug: string, primaryLanguage: string|null, stars: number}[], generatedAt: string}>}
 */
function getStats() {
  return apiFetch('/stats');
}

/**
 * Submit a new project. Creates it with listingStatus 'pending' and makes
 * the caller its owner, it won't appear in GET /projects or /search until
 * a site admin approves it. GitHub-derived fields (name, description,
 * stars, etc.) are deliberately not accepted here; they're filled in later
 * by a maintainer triggering project sync.
 *
 * @param {Object} payload
 * @param {string} payload.githubUrl Required. Must match https://github.com/{owner}/{repo}.
 * @param {'south_african'|'community_verified'} payload.connection Required.
 * @param {string} payload.category Required, free text.
 * @param {string[]} [payload.tags]
 * @param {string[]} [payload.countryCodes]
 * @returns {Promise<Project>} The created project (id, listingStatus: 'pending', etc.)
 */
function createProject(payload) {
  const csrfToken = getCsrfToken();

  return apiFetch('/projects', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify(payload),
  });
}

/**
 * Trigger a GitHub sync for a project, populates GitHub-derived fields
 * (name, description, stars, language, issues, etc.) that submission alone
 * never fills in. Requires the caller to be a maintainer (any role); the
 * backend throws ApiError with status 403 (code FORBIDDEN) otherwise. Runs
 * asynchronously server-side, returns a SyncJob (status: accepted) to poll
 * via getSyncJob, not the finished result.
 *
 * The job's id field is `id`, NOT `jobId` as codemasters-api-spec.yaml's
 * SyncJob schema claims, SyncController returns the JPA entity directly
 * (no dedicated DTO), and its only id getter is getId() -> `id`. Confirmed
 * against a live POST /projects/{id}/issues/sync response; this is a real
 * spec/implementation drift, not a typo here.
 *
 * @param {number|string} projectId
 * @returns {Promise<Object>} SyncJob, keyed by `id` (not `jobId`)
 */
function syncProject(projectId) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/projects/${projectId}/issues/sync`, {
    method: 'POST',
    headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
  });
}

/**
 * Poll a sync job's status (accepted -> running -> completed|failed).
 *
 * @param {string} jobId
 * @returns {Promise<Object>} SyncJob
 */
function getSyncJob(jobId) {
  return apiFetch(`/sync-jobs/${jobId}`);
}

/**
 * Unified cross-resource search, projects and issues in one ranked,
 * paginated list, discriminated by `resultType` ('project' | 'issue'). A
 * project result is a Project plus `resultType`; an issue result is an
 * Issue plus `resultType`. Backs the header search and /search page.
 *
 * @param {Object} params
 * @param {string} params.q Required, minimum 2 characters.
 * @param {'all'|'projects'|'issues'} [params.type] Default 'all'.
 * @param {string} [params.language] Only narrows project results.
 * @param {'beginner'|'intermediate'|'advanced'|'unknown'} [params.difficulty] Only narrows issue results.
 * @param {string} [params.country] e.g. 'ZA'. Only narrows project results.
 * @param {number} [params.page] Zero-based page index. Default 0.
 * @param {number} [params.size] Page size, 1-50. Default 20.
 * @returns {Promise<{items: Object[], meta: PageMeta}>}
 */
function search(params) {
  return apiFetch(`/search${buildQuery(params)}`);
}

/**
 * Report a project listing for moderation (API-03.9). Any authenticated
 * user may flag a given project once; a second report of the same project
 * by the same caller is rejected by the backend.
 *
 * @param {number|string} projectId
 * @param {string} reason Required, 3-500 characters (server-validated).
 * @returns {Promise<Object>} The created ReportDto (id, targetType: 'project',
 *   targetId, reason, status: 'open', createdAt, ...).
 * @throws {ApiError} status 400 (invalid/missing reason), 401 (not signed
 *   in), 404 (project doesn't exist), or 409 code REPORT_ALREADY_EXISTS
 *   (caller already reported this project).
 */
function reportProject(projectId, reason) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/projects/${projectId}/reports`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify({ reason }),
  });
}

/**
 * Get a single project's details, including maintainers, featured issues,
 * and recent comments.
 *
 * @param {number|string} projectId
 * @returns {Promise<Project & { maintainers?: unknown[], featuredIssues?: unknown[], recentComments?: unknown[] }>}
 */
function getProject(projectId) {
  return apiFetch(`/projects/${projectId}`);
}

/**
 * A project's real issues, filtered and paginated, GET /projects/{id} never
 * includes them (its featuredIssues field is hardcoded empty backend-side),
 * so this is the only way to actually populate the Issues tab.
 *
 * @param {number|string} projectId
 * @param {Object} [params]
 * @param {number} [params.page] Zero-based page index. Default 0.
 * @param {number} [params.size] Page size, 1-50. Default 20.
 * @param {'beginner'|'intermediate'|'advanced'|'unknown'} [params.difficulty]
 * @param {string} [params.label]
 * @param {'open'|'closed'|'claimed'} [params.status]
 * @returns {Promise<{items: Object[], meta: PageMeta}>}
 */
function getProjectIssues(projectId, params) {
  return apiFetch(`/projects/${projectId}/issues${buildQuery(params)}`);
}

/**
 * Update a project's Code Masters-owned metadata. Requires the caller to be
 * a maintainer (any role) on this project, the backend throws ApiError with
 * status 403 (code FORBIDDEN) otherwise. Only GitHub-derived fields are
 * excluded (name, description, stars, etc., those come from sync, not this
 * endpoint); listingStatus is also excluded (site-admin moderation only).
 * All fields optional; only provided fields change.
 *
 * @param {number|string} projectId
 * @param {Object} update
 * @param {string} [update.category]
 * @param {string[]} [update.tags]
 * @param {'south_african'|'community_verified'} [update.connection]
 * @param {string[]} [update.countryCodes]
 * @param {boolean} [update.acceptingContributions]
 * @returns {Promise<Project>} The updated project.
 */
function updateProject(projectId, update) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/projects/${projectId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify(update),
  });
}

/**
 * Invite a maintainer to a project by username. Owner-only.
 *
 * @param {number|string} projectId
 * @param {string} username
 * @returns {Promise<Object>} ProjectMaintainerDto
 */
function inviteMaintainer(projectId, username) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/projects/${projectId}/maintainers`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify({
      username,
      role: 'maintainer',
    }),
  });
}

/**
 * Remove a maintainer from a project.
 * Owner-only; refuses to remove the last remaining owner.
 *
 * @param {number|string} projectId
 * @param {number|string} userId
 * @returns {Promise<null>}
 */
function removeMaintainer(projectId, userId) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/projects/${projectId}/maintainers/${userId}`, {
    method: 'DELETE',
    headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
  });
}

/**
 * Get a single issue's details, including its parent project,
 * comments, and claims.
 *
 * @param {number|string} issueId
 * @returns {Promise<Object>}
 */
function getIssue(issueId) {
  return apiFetch(`/issues/${issueId}`);
}

/**
 * Evidence-based contribution context for an issue (API-04.1) — plain
 * facts and counts only (has a contributing guide, days since last
 * activity, completed-contribution count, issue age, label count,
 * in-flight claim count), deliberately no weighting, score, or AI
 * judgment. Backs the "Contribution context" panel on the issue detail
 * page.
 *
 * @param {number|string} issueId
 * @returns {Promise<{project: {hasContributingGuide: boolean, hasCodeOfConduct: boolean, daysSinceLastActivity: number|null, completedContributionsCount: number}, issue: {ageInDays: number, labelCount: number, isBeginnerFriendly: boolean, inFlightClaimCount: number}}>}
 * @throws {ApiError} status 404 if the issue doesn't exist.
 */
function getContributionContext(issueId) {
  return apiFetch(`/issues/${issueId}/contribution-context`);
}

/**
 * Get paginated comments for an issue.
 *
 * @param {number|string} issueId
 * @param {Object} [params]
 * @param {number} [params.page]
 * @param {number} [params.size]
 * @returns {Promise<Object>}
 */
function getIssueComments(issueId, params) {
  return apiFetch(`/issues/${issueId}/comments${buildQuery(params)}`);
}

/**
 * Get paginated comments for a project (project-level discussion, distinct
 * from an issue's comments).
 *
 * @param {number|string} projectId
 * @param {Object} [params]
 * @param {number} [params.page]
 * @param {number} [params.size]
 * @returns {Promise<Object>}
 */
function getProjectComments(projectId, params) {
  return apiFetch(`/projects/${projectId}/comments${buildQuery(params)}`);
}

/**
 * Create a comment on an issue.
 *
 * @param {number|string} issueId
 * @param {string} body
 * @returns {Promise<Object>}
 */
function getCsrfToken() {
  if (typeof document === 'undefined') return null;

  const cookie = document.cookie
    .split('; ')
    .find((row) => row.startsWith('CODEMASTERS_CSRF='));

  return cookie ? decodeURIComponent(cookie.split('=')[1]) : null;
}

/**
 * Report a comment for moderation (API-03.9). Any authenticated user may
 * flag a given comment once; a second report of the same comment by the
 * same caller is rejected by the backend.
 *
 * @param {number|string} commentId
 * @param {string} reason Required, 3-500 characters (server-validated).
 * @returns {Promise<Object>} The created ReportDto (id, targetType: 'comment',
 *   targetId, reason, status: 'open', createdAt, ...).
 * @throws {ApiError} status 400 (invalid/missing reason), 401 (not signed
 *   in), 404 (comment doesn't exist), or 409 code REPORT_ALREADY_EXISTS
 *   (caller already reported this comment).
 */
/**
 * Edit a comment's body (API-02.4). Author-only; the backend rejects edits
 * from anyone else with 403.
 *
 * @param {number|string} commentId
 * @param {string} body Required, 1-5000 characters (server-validated).
 * @returns {Promise<Object>} The updated CommentDto (id, author, body,
 *   edited: true, createdAt, updatedAt).
 * @throws {ApiError} status 400 (invalid/missing body), 401 (not signed
 *   in), 403 code FORBIDDEN (caller isn't the comment's author), or 404
 *   code COMMENT_NOT_FOUND.
 */
function updateComment(commentId, body) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/comments/${commentId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify({ body }),
  });
}

/**
 * Delete a comment (soft-delete; API-02.4). Author-only from this UI's
 * point of view — the backend also allows the parent project's maintainer,
 * but this ticket only wires up the "delete your own comment" path, so the
 * Delete action is only ever shown to the comment's author (see
 * DeleteCommentButton.jsx / IssueComments.jsx).
 *
 * @param {number|string} commentId
 * @returns {Promise<null>}
 * @throws {ApiError} status 401 (not signed in), 403 code FORBIDDEN
 *   (caller is neither the author nor a maintainer), or 404 code
 *   COMMENT_NOT_FOUND.
 */
function deleteComment(commentId) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/comments/${commentId}`, {
    method: 'DELETE',
    headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
  });
}

function reportComment(commentId, reason) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/comments/${commentId}/reports`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify({ reason }),
  });
}

/**
 * Post a comment on an issue.
 *
 * @param {number|string} issueId
 * @param {string} body
 * @param {boolean} [isQuestion] Flags this as a blocking question — "I need
 *   this answered before I can start" — surfaced to the issue's project
 *   maintainers as a triaged queue until resolved. Defaults to false.
 */
function postComment(issueId, body, isQuestion) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/issues/${issueId}/comments`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify(isQuestion ? { body, isQuestion: true } : { body }),
  });
}

/**
 * A maintainer marks a flagged blocking question answered.
 *
 * @param {number|string} commentId
 * @returns {Promise<Object>} The updated comment.
 */
function resolveQuestion(commentId) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/comments/${commentId}/resolve`, {
    method: 'POST',
    headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
  });
}

/**
 * Create a comment on a project (project-level discussion). Requires
 * authentication (401 if not signed in).
 *
 * @param {number|string} projectId
 * @param {string} body Required, 1-5000 characters (server-validated).
 * @returns {Promise<Object>} The created CommentDto.
 */
function postProjectComment(projectId, body) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/projects/${projectId}/comments`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify({ body }),
  });
}


  function getIssueClaims(issueId) {
    return apiFetch(`/issues/${issueId}/claims`);
  }

  /** @returns {Promise<UserProfile>} */
  function getCurrentUser() {
    return apiFetch('/users/me');
  }

  /**
   * Update the caller's own profile. Only displayName/bio/location/skills are
   * editable, username, avatarUrl, reputation and email are GitHub-derived
   * or system-managed and aren't accepted here. All fields optional; only
   * provided fields change.
   *
   * @param {Object} update
   * @param {string} [update.displayName]
   * @param {string} [update.bio] Max 1000 characters (server-validated).
   * @param {string} [update.location]
   * @param {string[]} [update.skills]
   * @returns {Promise<UserProfile>} The updated profile.
   */
  function updateCurrentUser(update) {
    const csrfToken = getCsrfToken();

    return apiFetch('/users/me', {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
      },
      body: JSON.stringify(update),
    });
  }

  /**
   * Permanently delete (anonymize) the caller's own account (API-03.11).
   * The backend also clears the session/CSRF cookies server-side as part of
   * this call, so callers should treat a successful response as an implicit
   * logout, there's no need to also call logout() afterwards. Irreversible;
   * callers are responsible for getting explicit confirmation first, this
   * function itself performs no confirmation.
   *
   * @returns {Promise<null>}
   */
  function deleteCurrentUser() {
    const csrfToken = getCsrfToken();

    return apiFetch('/users/me', {
      method: 'DELETE',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    });
  }

  /**
   * A developer's public profile, never includes email, githubAccess, or
   * isSiteAdmin (those are only on GET /users/me). Throws ApiError with
   * status 404 (code USER_NOT_FOUND) if the username doesn't exist.
   *
   * @param {string} username
   * @returns {Promise<Object>} PublicUserProfile
   */
  function getPublicProfile(username) {
    return apiFetch(`/users/${username}`);
  }

  /**
   * A developer's verified contribution history, only claims with
   * status `completed` (GitHub-merge-verified or maintainer-confirmed),
   * most recent first. Backs the "activity summary" on /profile and public
   * /users/{username} pages.
   *
   * @param {string} username
   * @param {Object} [params]
   * @param {number} [params.page] Zero-based page index. Default 0.
   * @param {number} [params.size] Page size, 1-50. Default 20.
   * @returns {Promise<{items: Object[], meta: PageMeta}>}
   */
  function getUserContributions(username, params) {
    return apiFetch(`/users/${username}/contributions${buildQuery(params)}`);
  }

  /**
   * Aggregated activity across every project the caller maintains: active
   * claims, claims awaiting review (has a pull request attached, not yet
   * reviewed), and recent comments, one rollup instead of checking each
   * maintained project individually.
   *
   * @returns {Promise<{projects: Object[]}>}
   */
  function getMaintainerActivity() {
    return apiFetch('/users/me/maintainer-activity');
  }

  /**
   * Skill-Matching Recommendation Engine (wow-feature, 2026-09-24): up to 10
   * open issues ranked by fit against the caller's own skills, each with the
   * plain-language reasons it matched. No combined score is ever returned.
   * Session-authenticated; there's no anonymous/public version since it's
   * personalized to the caller.
   *
   * @returns {Promise<{issue: Object, project: {id: number, name: string, slug: string, primaryLanguage: string|null}, reasons: string[]}[]>}
   */
  function getRecommendedIssues() {
    return apiFetch('/users/me/recommended-issues');
  }

  /**
   * Recognition Leaderboard (wow-feature, 2026-09-24): public, unauthenticated
   * — the top 20 credited contributors, ranked, most contributions first.
   *
   * @returns {Promise<{rank: number, userId: number, username: string, displayName: string|null, avatarUrl: string|null, contributionCount: number}[]>}
   */
  function getLeaderboard() {
    return apiFetch('/leaderboard');
  }

  /**
   * The caller's own leaderboard rank, even if outside the public top 20.
   * `rank` is `null` if the caller has zero credited contributions (not yet
   * ranked) — never a made-up placement. Session-authenticated.
   *
   * @returns {Promise<{rank: number|null, userId: number, username: string, displayName: string|null, avatarUrl: string|null, contributionCount: number}>}
   */
  function getMyLeaderboardRank() {
    return apiFetch('/users/me/leaderboard-rank');
  }

  /**
   * Log out the current session. Hits /auth/logout directly (not under
   * /api/v1, same as the /auth/github login link, see next.config.js's
   * rewrite for /auth/:path*), clearing the session and CSRF cookies.
   */
  function logout() {
    const csrfToken = getCsrfToken();

    return fetch('/auth/logout', {
      method: 'POST',
      credentials: 'include',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    });
  }

  function postClaim(issueId, note) {
    const csrfToken = getCsrfToken();

    return apiFetch(`/issues/${issueId}/claim`, {
      method: 'POST',
      headers: {
        ...(note ? { 'Content-Type': 'application/json' } : {}),
        ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
      },
      body: note ? JSON.stringify({ note }) : undefined,
    });
  }

  function deleteClaim(issueId) {
    const csrfToken = getCsrfToken();

    return apiFetch(`/issues/${issueId}/claim`, {
      method: 'DELETE',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    });
  }

  /**
   * Attach or update the pull request link on the caller's own claim
   * (API-03.3). Sets pullRequestState to 'open' server-side; GitHub sync is
   * what later corrects it to 'merged'/'closed_unmerged'.
   *
   * @param {number|string} issueId
   * @param {number|string} claimId
   * @param {string} pullRequestUrl
   * @returns {Promise<Object>} The updated ClaimDto.
   * @throws {ApiError} status 403 if the caller doesn't own the claim, 404
   *   if the issue or claim doesn't exist.
   */
  function attachPullRequest(issueId, claimId, pullRequestUrl) {
    const csrfToken = getCsrfToken();

    return apiFetch(`/issues/${issueId}/claims/${claimId}`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
      },
      body: JSON.stringify({ pullRequestUrl }),
    });
  }

  /**
   * A maintainer's review decision on a claim's pull request (API-03.4):
   * either request changes with feedback, or manually confirm completion
   * (the fallback for cases GitHub sync can't verify itself). Maintainer of
   * the claim's project only.
   *
   * @param {number|string} issueId
   * @param {number|string} claimId
   * @param {'request_changes'|'confirm_completed'} decision
   * @param {string} [feedback] Required (non-blank) when decision is
   *   'request_changes', up to 2000 characters.
   * @returns {Promise<Object>} The updated ClaimDto.
   * @throws {ApiError} status 400 (missing feedback), 403 (caller isn't a
   *   maintainer of this project), 404, or 409 (CLAIM_NOT_REVIEWABLE — the
   *   claim is released, or already completed and decision is
   *   request_changes).
   */
  function reviewClaim(issueId, claimId, decision, feedback) {
    const csrfToken = getCsrfToken();

    return apiFetch(`/issues/${issueId}/claims/${claimId}/review`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
      },
      body: JSON.stringify(feedback ? { decision, feedback } : { decision }),
    });
  }

  /**
   * Request to join another contributor's claim as a collaborator (not a
   * merge of two claims — the claim keeps exactly one owner, who a pull
   * request is expected to be attached to on GitHub). Allowed regardless of
   * whether the caller already holds their own separate claim on the same
   * issue; if the owner later accepts, that separate claim is released
   * automatically.
   *
   * @param {number|string} issueId
   * @param {number|string} claimId
   * @returns {Promise<Object>} The created ClaimCollaborationRequestDto (status: 'pending').
   * @throws {ApiError} status 400 (the caller owns this claim), 404, 409
   *   (CLAIM_NOT_JOINABLE — the claim is completed/released, or
   *   COLLABORATION_ALREADY_REQUESTED — a pending/accepted request already
   *   exists from this caller), or 429 (RATE_LIMITED).
   */
  function requestCollaboration(issueId, claimId) {
    const csrfToken = getCsrfToken();

    return apiFetch(`/issues/${issueId}/claims/${claimId}/collaboration-requests`, {
      method: 'POST',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    });
  }

  /**
   * Every collaboration request on a claim, regardless of status, oldest
   * first. Public, no auth required, same visibility as the claim itself.
   *
   * @param {number|string} issueId
   * @param {number|string} claimId
   * @returns {Promise<Object[]>} ClaimCollaborationRequestDto[].
   */
  function listCollaborationRequests(issueId, claimId) {
    return apiFetch(`/issues/${issueId}/claims/${claimId}/collaboration-requests`);
  }

  /**
   * The claim owner's accept/decline decision on a pending collaboration
   * request. Accepting releases the requester's own separate active claim
   * on the same issue, if they hold one.
   *
   * @param {number|string} issueId
   * @param {number|string} claimId
   * @param {number|string} requestId
   * @param {'accept'|'decline'} decision
   * @returns {Promise<Object>} The updated ClaimCollaborationRequestDto.
   * @throws {ApiError} status 403 (caller isn't the claim's owner), 404, or
   *   409 (COLLABORATION_NOT_PENDING — already responded to).
   */
  function respondToCollaborationRequest(issueId, claimId, requestId, decision) {
    const csrfToken = getCsrfToken();

    return apiFetch(`/issues/${issueId}/claims/${claimId}/collaboration-requests/${requestId}/response`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
      },
      body: JSON.stringify({ decision }),
    });
  }

  /**
   * Withdraws the caller's own still-pending collaboration request.
   *
   * @param {number|string} issueId
   * @param {number|string} claimId
   * @param {number|string} requestId
   * @throws {ApiError} status 403 (caller isn't the request's own author),
   *   404, or 409 (COLLABORATION_NOT_PENDING — already responded to).
   */
  function cancelCollaborationRequest(issueId, claimId, requestId) {
    const csrfToken = getCsrfToken();

    return apiFetch(`/issues/${issueId}/claims/${claimId}/collaboration-requests/${requestId}`, {
      method: 'DELETE',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    });
  }

  /**
   * The caller's own in-app notifications, most recent first — a small,
   * fixed set of state-change events (a claim reviewed, a collaboration
   * request received/responded to, a project submission moderated), not a
   * general activity feed.
   *
   * @param {Object} [params]
   * @param {number} [params.page]
   * @param {number} [params.size]
   * @returns {Promise<{items: Object[], meta: PageMeta}>}
   */
  function getNotifications(params) {
    return apiFetch(`/users/me/notifications${buildQuery(params)}`);
  }

  /**
   * Backs the bell icon's unread badge — a cheap count, not the full list.
   *
   * @returns {Promise<{count: number}>}
   */
  function getUnreadNotificationCount() {
    return apiFetch('/users/me/notifications/unread-count');
  }

  /**
   * Marks one of the caller's own notifications read. Idempotent.
   *
   * @param {number|string} notificationId
   * @returns {Promise<Object>} The updated notification.
   */
  function markNotificationRead(notificationId) {
    const csrfToken = getCsrfToken();

    return apiFetch(`/users/me/notifications/${notificationId}/read`, {
      method: 'POST',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    });
  }

  /** Marks every one of the caller's unread notifications read in one call. */
  function markAllNotificationsRead() {
    const csrfToken = getCsrfToken();

    return apiFetch('/users/me/notifications/read-all', {
      method: 'POST',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    });
  }

/**
 * Update an issue's difficulty and/or beginner-friendly status.
 * Maintainer-only. Only provided fields are changed.
 *
 * @param {number|string} issueId
 * @param {Object} update
 * @param {string} [update.difficulty] One of: beginner, intermediate, advanced, unknown
 * @param {boolean} [update.isBeginnerFriendly]
 * @returns {Promise<Object>} Updated IssueDto
 */
function updateIssueClassification(issueId, update) {
  const csrfToken = getCsrfToken();
  const payload = {};

  if (update.difficulty !== undefined) payload.difficulty = update.difficulty;
  if (update.isBeginnerFriendly !== undefined) payload.isBeginnerFriendly = update.isBeginnerFriendly;

  return apiFetch(`/issues/${issueId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify(payload),
  });
}

/**
 * List projects awaiting moderation. Site-admin only, the backend enforces
 * this via SiteAdminGuard and returns 403 FORBIDDEN for anyone else; this
 * function doesn't attempt its own client-side check, since the server is
 * the real enforcement point.
 *
 * @param {Object} [params]
 * @param {number} [params.page] Zero-based page index. Default 0.
 * @param {number} [params.size] Page size, 1-50. Default 20.
 * @returns {Promise<PagedProjects>}
 */
function listPendingProjects(params) {
  return apiFetch(`/admin/projects/pending${buildQuery(params)}`);
}

/**
 * Approve or reject a pending project submission. Site-admin only. Only
 * ever changes the project's listingStatus (published/rejected), never
 * touches verified/verifiedAt, which is a separate concept.
 *
 * @param {number|string} projectId
 * @param {'approve'|'reject'} decision
 * @param {string} [reason] Recommended (not required) when rejecting.
 * @returns {Promise<Project>} The updated project.
 */
function moderateProject(projectId, decision, reason) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/admin/projects/${projectId}/moderation`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify(reason ? { decision, reason } : { decision }),
  });
}

/**
 * List abuse reports (filed against a comment or a project listing).
 * Site-admin only; the backend returns 403 FORBIDDEN for anyone else.
 * Defaults to `status=open` server-side when no status is passed. Read-only,
 * resolving/dismissing a report (PATCH /admin/reports/{id}) is FE-GAP-11's
 * job, not this one.
 *
 * @param {Object} [params]
 * @param {'open'|'resolved'|'dismissed'} [params.status]
 * @param {number} [params.page] Zero-based page index. Default 0.
 * @param {number} [params.size] Page size, 1-50. Default 20.
 * @returns {Promise<{items: Object[], meta: PageMeta}>} items are Report DTOs.
 */
function listReports(params) {
  return apiFetch(`/admin/reports${buildQuery(params)}`);
}

/**
 * Resolve or dismiss an abuse report (API-03.9, site-admin only). This
 * updates the report's own record only — it never itself hides, deletes,
 * or otherwise touches the reported comment/project; an admin who agrees
 * with the report acts on the content separately (e.g. deleteComment()).
 *
 * @param {number|string} reportId
 * @param {"resolved"|"dismissed"} status The only two values the API
 *   accepts here; a report can never be set back to "open" this way.
 * @param {string} [resolution] Optional note, up to 1000 characters.
 * @returns {Promise<Object>} The updated ReportDto (status, resolution,
 *   resolvedAt, ...).
 * @throws {ApiError} status 400 (missing/invalid status, or resolution too
 *   long), 401 (not signed in), 403 (caller isn't a site admin), or 404
 *   code REPORT_NOT_FOUND.
 */
function reviewReport(reportId, status, resolution) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/admin/reports/${reportId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify({ status, resolution: resolution || undefined }),
  });
}

module.exports = {
  listProjects,
  getStats,
  createProject,
  syncProject,
  getSyncJob,
  search,
  getProject,
  getProjectIssues,
  updateProject,
  reportProject,
  getIssue,
  getContributionContext,
  getIssueComments,
  postComment,
  resolveQuestion,
  getProjectComments,
  postProjectComment,
  updateComment,
  deleteComment,
  reportComment,
  ApiError,
  getIssueClaims,
  getCurrentUser,
  updateCurrentUser,
  deleteCurrentUser,
  getPublicProfile,
  getUserContributions,
  getMaintainerActivity,
  getRecommendedIssues,
  getLeaderboard,
  getMyLeaderboardRank,
  postClaim,
  deleteClaim,
  attachPullRequest,
  reviewClaim,
  requestCollaboration,
  listCollaborationRequests,
  respondToCollaborationRequest,
  cancelCollaborationRequest,
  getNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
  logout,
  inviteMaintainer,
  removeMaintainer,
  updateIssueClassification,
  listPendingProjects,
  moderateProject,
  listReports,
  reviewReport,
};