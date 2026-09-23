// FE-01.3 acceptance criteria: "A small test/manual check confirms
// listProjects() against the running mock server returns typed,
// correctly-shaped data."
//
// Usage:
//   1. In another terminal: cd ../mock-server && npm run mock
//   2. npm run check-api   (or: node scripts/check-api-client.js)

const fs = require('fs');
const path = require('path');

// Next.js loads .env.local automatically; a plain `node` script doesn't,
// so load it ourselves here.
function loadEnvLocal() {
  const envPath = path.join(__dirname, '..', '.env.local');
  if (!fs.existsSync(envPath)) return;
  const lines = fs.readFileSync(envPath, 'utf8').split('\n');
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq === -1) continue;
    const key = trimmed.slice(0, eq).trim();
    const value = trimmed.slice(eq + 1).trim();
    if (!(key in process.env)) process.env[key] = value;
  }
}

loadEnvLocal();

if (!process.env.BACKEND_URL) {
  console.error('BACKEND_URL is not set. Either:');
  console.error('  cp .env.local.example .env.local');
  console.error('or run:');
  console.error('  BACKEND_URL=http://localhost:4010 node scripts/check-api-client.js');
  process.exit(1);
}

const { listProjects } = require('../lib/api');

const REQUIRED_PROJECT_FIELDS = [
  'id',
  'name',
  'githubUrl',
  'description',
  'primaryLanguage',
  'connection',
  'createdAt',
  'updatedAt',
];

(async () => {
  console.log(`Checking listProjects() against ${process.env.BACKEND_URL} ...`);
  try {
    const result = await listProjects({ country: 'ZA', sort: 'stars' });
    const errors = [];

    if (!Array.isArray(result.items)) errors.push('items is not an array');
    if (!result.meta || typeof result.meta.total !== 'number') {
      errors.push('meta.total is missing or not a number');
    }

    const first = result.items && result.items[0];
    if (!first) {
      errors.push('items is empty, expected several seeded mock projects');
    } else {
      for (const field of REQUIRED_PROJECT_FIELDS) {
        if (!(field in first)) errors.push(`items[0].${field} is missing`);
      }
    }

    if (errors.length) {
      console.error('\n❌ Response does not match the Project/PagedProjects schema:');
      errors.forEach((e) => console.error(`   - ${e}`));
      process.exit(1);
    }

    console.log(`\n✅ Got ${result.items.length} project(s).`);
    console.log('✅ meta:', result.meta);
    console.log('✅ First project matches the Project schema, e.g.:', {
      id: first.id,
      name: first.name,
      connection: first.connection,
      hasBeginnerFriendlyIssues: first.hasBeginnerFriendlyIssues,
    });
  } catch (err) {
    console.error('\n❌ listProjects() threw:', err.message);
    if (err.status) console.error(`   HTTP status: ${err.status}`);
    process.exit(1);
  }
})();