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

function postComment(issueId, body) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/issues/${issueId}/comments`, {
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

module.exports = {
  listProjects,
  createProject,
  syncProject,
  getSyncJob,
  search,
  getProject,
  getProjectIssues,
  updateProject,
  getIssue,
  getIssueComments,
  postComment,
  ApiError,
  getIssueClaims,
  getCurrentUser,
  updateCurrentUser,
  getPublicProfile,
  getUserContributions,
  getMaintainerActivity,
  postClaim,
  deleteClaim,
  logout,
  inviteMaintainer,
  removeMaintainer,
  updateIssueClassification,
  listPendingProjects,
  moderateProject,
};
