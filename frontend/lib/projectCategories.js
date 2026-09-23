/**
 * Shared category taxonomy for project submission and editing, one list so
 * both forms can't drift apart. `category` is free text server-side
 * (CreateProjectRequest.category / UpdateProjectRequest.category are plain
 * strings, not an enum), but presenting an uncontrolled text field lets
 * near-duplicate categories ("ML" / "AI" / "Machine Learning") quietly
 * fragment the GET /projects?category= filter. A general tech taxonomy,
 * plus "Other" for anything not covered.
 */
const CATEGORIES = [
  'AgriTech',
  'AI',
  'Civic Tech',
  'Cybersecurity',
  'Data Engineering',
  'Data Science',
  'Developer Tools',
  'EdTech',
  'FinTech',
  'Health Tech',
  'Machine Learning',
  'Mobile',
  'Web',
];

const OTHER_CATEGORY = '__other__';

module.exports = { CATEGORIES, OTHER_CATEGORY };
