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
 * @typedef {'south_african'|'africa_focused'|'africa_led'|'community_verified'} ProjectConnection
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

  return res.json();
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

module.exports = {
  listProjects,
  getProject,
  getIssue,
  getIssueComments,
  postComment,
  ApiError,
};