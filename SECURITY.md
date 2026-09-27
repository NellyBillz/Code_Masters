# Security policy

Code Masters handles developers' accounts, businesses' licences and, soon, real payouts. We take security reports seriously and thank everyone who reports responsibly.

## Reporting a vulnerability

**Please don't open a public issue for security problems.**

Report privately through GitHub: open the repository's **Security** tab and click **Report a vulnerability**. Only the maintainers can see the report.

Include:

- what the problem is and where (URL, page or file)
- steps to reproduce it
- what an attacker could do with it
- your GitHub username or name if you'd like to be credited

## What happens next

| Step | Our target |
|---|---|
| We confirm we received your report | within 3 business days |
| We tell you whether we can reproduce it and how serious it is | within 10 business days |
| We fix critical and high-severity issues | within 30 days |
| We publish an advisory and credit you (if you want) | after the fix is released |

We'll keep you updated throughout. If a fix needs longer, we'll explain why.

## What's in scope

- This repository: the Spring Boot backend, the Next.js frontend and the database migrations
- The live Code Masters site, once launched
- Especially: login and sessions, one user acting as another, issue-claim verification, licence purchases, contributor payout calculations, support-contract escrow, and exposure of personal information (POPIA)

## Out of scope

- Denial-of-service or load testing against the live site
- Social engineering or phishing of our team or users
- Physical attacks
- Reports from automated scanners with no demonstrated impact
- Missing best-practice headers with no real exploit
- Vulnerabilities in GitHub itself or in third-party services we use (report those to the vendor)

## Safe harbour

If you act in good faith, we won't take legal action against you. That means you:

- test only against your own accounts, or with permission
- don't access, change or keep other people's data beyond what's needed to show the problem
- don't disrupt the service
- give us reasonable time to fix the issue before telling anyone else

If you accidentally access personal information, stop, don't keep it, and tell us in your report.

## Supported versions

Only the latest version on the `main` branch receives security fixes.

## How we protect the project

- Every pull request is scanned by CodeQL (code vulnerabilities) and gitleaks (leaked secrets).
- Dependabot opens pull requests when a dependency has a known vulnerability.
- Production uses post-quantum TLS and never exposes the backend or database to the internet. See [SECURITY-QUANTUM.md](SECURITY-QUANTUM.md).
