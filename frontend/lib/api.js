/**
 * Typed API client (FE-01.3). Every page must import API calls from this
 * module only — no component should call fetch() directly. Field names and
 * types here are kept in sync with codemasters-api-spec.yml (the
 * authoritative contract), so treat that spec as the source of truth if the
 * two ever disagree.
 *
 * All calls go through /api/v1/*, which next.config.js rewrites to
 * `${BACKEND_URL}/api/v1/*` for browser requests — so this file works
 * unchanged against the mock server (FE-01.2) today and the real backend
 * later, with BACKEND_URL as the only thing that ever changes.
 *
 * On the server (SSR, React Server Components, or a plain Node script like
 * scripts/check-api-client.js) there's no browser to proxy through, so we
 * call BACKEND_URL directly instead. Same contract, same single switch —
 * we're just skipping the same-origin hop that only matters for browser
 * cookies (see codemasters-api-design.md §4.1).
 */

/**
 * @typedef {'south_african'|'community_verified'} ProjectConnection
 */

/**
 * @typedef {'pending'|'published'|'rejected'} ProjectListingStatus
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
 * @property {boolean} [hasContributingGuide] From GitHub's community-profile
 *   endpoint (GH-03.3) — a plain onboarding-readiness fact, not a score.
 * @property {boolean} [hasCodeOfConduct] Same source as hasContributingGuide.
 * @property {string} [lastActivityAt] ISO date-time
 * @property {ProjectListingStatus} [listingStatus] `pending` until a site
 *   admin approves it (FE-03.1/.2); only `published` projects are publicly
 *   discoverable.
 * @property {boolean} [acceptingContributions]
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

const RATE_LIMIT_MESSAGE =
  "You're doing that a lot — please wait a bit before trying again.";

/**
 * A clear, friendly message for a failed write (FE-03.8) — specifically,
 * one that never surfaces a raw `RATE_LIMITED` error straight from the API.
 * Every write action's catch block should route its error through this
 * instead of showing `err.message` directly.
 *
 * @param {unknown} err
 * @param {string} fallback Used for any error that isn't a rate limit.
 * @returns {string}
 */
function friendlyErrorMessage(err, fallback) {
  if (err instanceof ApiError && err.code === 'RATE_LIMITED') {
    return RATE_LIMIT_MESSAGE;
  }
  return (err && err.message) || fallback;
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
 * @param {string} path
 * @param {RequestInit} [init]
 */
async function apiFetch(path, init) {
  const res = await fetch(`${getApiBase()}${path}`, {
    headers: { Accept: 'application/json', ...((init && init.headers) || {}) },
    ...init,
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
 * Public, non-personal, aggregate platform-impact metrics (FE-03.9) — no
 * auth required.
 *
 * @returns {Promise<Object>} PlatformStats: { publishedProjects,
 *   activeProjectsAcceptingContributions, totalContributorsEngaged,
 *   totalActiveClaims, totalContributionsCompleted, generatedAt }
 */
function getStats() {
  return apiFetch('/stats');
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
 * List projects awaiting moderation, oldest first (FE-03.1). Site-admin only
 * — throws a 403 `ApiError` (code `FORBIDDEN`) for anyone else.
 *
 * @param {Object} [params]
 * @param {number} [params.page]
 * @param {number} [params.size]
 * @returns {Promise<PagedProjects>}
 */
function getPendingProjects(params) {
  return apiFetch(`/admin/projects/pending${buildQuery(params)}`);
}

/**
 * Approve or reject a pending project submission. Site-admin only.
 *
 * @param {number|string} projectId
 * @param {Object} params
 * @param {'approve'|'reject'} params.decision
 * @param {string} [params.reason]
 * @returns {Promise<Project>} The project, with its updated `listingStatus`.
 */
function moderateProject(projectId, { decision, reason }) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/admin/projects/${projectId}/moderation`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify({ decision, reason }),
  });
}

/**
 * Update a project's Code-Masters-specific metadata (FE-03.7 uses this for
 * `acceptingContributions`; the same partial-update endpoint also accepts
 * `category`/`tags`/`connection`/`countryCodes`). Maintainer-only (any
 * role); only the fields provided are changed.
 *
 * @param {number|string} projectId
 * @param {Object} updates
 * @param {boolean} [updates.acceptingContributions]
 * @param {string} [updates.category]
 * @param {string[]} [updates.tags]
 * @param {ProjectConnection} [updates.connection]
 * @returns {Promise<Project>} The updated project.
 */
function updateProject(projectId, updates) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/projects/${projectId}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify(updates),
  });
}

/**
 * Report a comment for a site admin to review (FE-03.7). Authenticated,
 * once per user per comment — a repeat attempt throws a 409 `ApiError` with
 * code `REPORT_ALREADY_EXISTS`.
 *
 * @param {number|string} commentId
 * @param {string} reason 3-500 characters.
 * @returns {Promise<Object>} The created ReportDto.
 */
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
 * Report a project listing for a site admin to review (FE-03.7).
 * Authenticated, once per user per project — a repeat attempt throws a 409
 * `ApiError` with code `REPORT_ALREADY_EXISTS`.
 *
 * @param {number|string} projectId
 * @param {string} reason 3-500 characters.
 * @returns {Promise<Object>} The created ReportDto.
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

  function getCurrentUser() {
    return apiFetch('/users/me');
  }

  /**
   * Anonymize the caller's account (FE-03.8): clears username/displayName/
   * avatarUrl/bio/location/skills/email and unlinks the GitHub identity, but
   * leaves past comments and completed contributions attached to the
   * now-anonymized row untouched. Deletes the session server-side, which
   * clears both cookies via this response's Set-Cookie headers — no
   * separate logout() call needed after this succeeds.
   *
   * @returns {Promise<null>}
   */
  function deleteAccount() {
    const csrfToken = getCsrfToken();

    return apiFetch('/users/me', {
      method: 'DELETE',
      headers: csrfToken ? { 'X-CSRF-Token': csrfToken } : {},
    });
  }

  /**
   * A developer's verified contribution history (FE-03.5): only `completed`
   * claims, most recent first. Public — no auth required.
   *
   * @param {string} username
   * @param {Object} [params]
   * @param {number} [params.page]
   * @param {number} [params.size]
   * @returns {Promise<Object>} PagedContributions
   */
  function getContributions(username, params) {
    return apiFetch(`/users/${username}/contributions${buildQuery(params)}`);
  }

  /**
   * The maintainer-sanity activity rollup (FE-03.6): per project the caller
   * maintains, its active claims, claims awaiting review, and recent
   * comments, in one call. Session-authenticated; a user maintaining zero
   * projects gets `{ projects: [] }`, not an error.
   *
   * @returns {Promise<Object>} MaintainerActivitySummary
   */
  function getMaintainerActivity() {
    return apiFetch('/users/me/maintainer-activity');
  }

  /**
   * Log out the current session. Hits /auth/logout directly (not under
   * /api/v1, same as the /auth/github login link — see next.config.js's
   * rewrite for /auth/:path*), clearing the session and CSRF cookies.
   */
  function logout() {
    const csrfToken = getCsrfToken();

    return fetch('/auth/logout', {
      method: 'POST',
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
 * Attach or update the pull request link on the caller's own claim (FE-03.3).
 * The claim's owner only — a maintainer of the project is not exempt from
 * this check.
 *
 * @param {number|string} issueId
 * @param {number|string} claimId
 * @param {string} pullRequestUrl
 * @returns {Promise<Object>} The updated ClaimDto.
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
 * A maintainer's review decision on a claim's attached pull request
 * (FE-03.3). Maintainer-only.
 *
 * @param {number|string} issueId
 * @param {number|string} claimId
 * @param {Object} params
 * @param {'request_changes'|'confirm_completed'} params.decision
 * @param {string} [params.feedback] Required when decision is `request_changes`.
 * @returns {Promise<Object>} The updated ClaimDto.
 */
function reviewClaim(issueId, claimId, { decision, feedback }) {
  const csrfToken = getCsrfToken();

  return apiFetch(`/issues/${issueId}/claims/${claimId}/review`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(csrfToken ? { 'X-CSRF-Token': csrfToken } : {}),
    },
    body: JSON.stringify({ decision, feedback }),
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

module.exports = {
  listProjects,
  getStats,
  getProject,
  updateProject,
  getPendingProjects,
  moderateProject,
  reportComment,
  reportProject,
  getIssue,
  getIssueComments,
  postComment,
  ApiError,
  friendlyErrorMessage,
  getIssueClaims,
  getCurrentUser,
  deleteAccount,
  getContributions,
  getMaintainerActivity,
  postClaim,
  deleteClaim,
  attachPullRequest,
  reviewClaim,
  logout,
  inviteMaintainer,
  removeMaintainer,
  updateIssueClassification,
};
