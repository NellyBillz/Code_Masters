# CODE MASTERS
## Product Definition, Feature Scope and Build Contract

**Version:** 1.0  
**Project:** Code Masters  
**Stream:** Open Source Agenda  
**Team:** 4 developers  
**Build starts:** 14 September 2026  
**Official hackathon build window:** 25–27 September 2026  
**Available development time:** 2 weeks before the official window, with the official window used for final build work, polish, testing and submission.

---

# 1. READ THIS FIRST

This document is the single product-definition document for Code Masters.

It exists to make sure that every teammate, developer, designer, frontend developer, backend developer, database developer, and any LLM working on the project has the same understanding of what we are building.

The API specification and API design document are companion documents.

**The three documents together define the project:**

1. `CODE_MASTERS_PRODUCT_DEFINITION.md`  
   What the product is and what features it has.

2. `codemasters-api-spec.yaml`  
   What the backend API exposes.

3. `codemasters-api-design.md`  
   Why the API is structured this way and how the backend concepts fit together.

If something is not described in these documents, do not assume that it is part of the MVP. Ask the team before adding it.

**Revision note:** `codemasters-api-spec.yaml` and `codemasters-api-design.md` have been
revised to v2 for the 2-week Phase 1 build (maintainer management, sync job polling,
multi-claimant claims, session-based auth, comment moderation, issue classification
override, unified pagination/search discriminator). Sections 32 and 33 of this document
below have been updated to match. If you are looking at an older copy of this document,
the v2 API files are the ones to build against.

---

# 2. THE PRODUCT IN ONE SENTENCE

**Code Masters is a discovery and community platform for African open-source software that helps developers discover projects, understand where help is needed, connect with the people behind those projects, and then contribute through GitHub.**

We are NOT replacing GitHub.

GitHub remains where the actual source code, commits, pull requests and repository-level contribution work happen.

Code Masters is the layer that helps people FIND the right project and understand HOW they can help.

---

# 3. THE PROBLEM

African and South African developers are building open-source software, but that work is difficult to discover as part of a connected ecosystem.

A developer who wants to contribute to open source may have several problems:

- They do not know which African/South African projects exist.
- Projects are scattered across individual GitHub profiles and organisations.
- They may find a project but not know whether it is active.
- They may not know what programming language or technology the project uses.
- They may not know what contribution opportunities are available.
- A GitHub issue can be technically correct but intimidating to a beginner.
- There is no dedicated local community layer connecting developers around these projects.
- A developer may want to ask a question before contributing but has no Code Masters discussion space.

The problem is therefore NOT:

> "GitHub does not have open-source projects."

GitHub already has them.

The problem is:

> **There is no dedicated discovery and community front door for African open-source projects and the developers who want to contribute to them.**

---

# 4. THE SOLUTION

Code Masters creates that front door.

A developer should be able to:

1. Open Code Masters.
2. Discover African/South African open-source projects.
3. Search and filter those projects.
4. Open a project and understand what it does.
5. See useful project information and activity.
6. See the project's contribution opportunities.
7. Find beginner-friendly opportunities.
8. Open an issue and understand what needs to be done.
9. Ask questions or discuss the issue with the community.
10. Signal that they intend to work on an issue.
11. Click through to GitHub.
12. Perform the actual contribution on GitHub.

The core experience is:

**DISCOVER → UNDERSTAND → DISCUSS → CONTRIBUTE**

---

# 5. WHAT WE ARE BUILDING

The product has two conceptual layers.

## Layer 1: The Open-Source Discovery and Community Layer

This is the foundation.

It must work BEFORE we build the differentiator.

Layer 1 allows users to:

- discover projects
- search projects
- filter projects
- view project details
- view project issues
- view issue details
- discuss projects
- discuss issues
- create a developer profile
- authenticate using GitHub
- submit projects
- update projects they are allowed to manage
- claim/signal intent on contribution opportunities
- click through to GitHub to contribute

Layer 1 is the base product.

## Layer 2: The Differentiator

The differentiator is built ONLY AFTER Layer 1 is working end-to-end.

We do not build the differentiator while the foundation is incomplete.

The differentiator should make Code Masters meaningfully different from generic GitHub issue aggregators.

The current direction is to use **African/South African context, project verification, project health and contribution suitability** as important differentiating information.

Potential differentiating capabilities include:

- South African/African project verification
- local project categories
- project health information
- contribution suitability scoring
- issue difficulty classification
- maintainer responsiveness information
- freshness/activity information
- community context around projects
- eventually, intelligent matching between developers and projects/issues

The exact differentiator must not compromise the Layer 1 product.

---

# 6. IMPORTANT SCOPE RULE

## We build Layer 1 first.

There is a strict dependency:

```text
LAYER 1
Discovery + Projects + Issues + Users + Community + GitHub
                    ↓
             MUST WORK
                    ↓
LAYER 2
Differentiator
```

Do NOT start building:

- AI matching
- sophisticated recommendation algorithms
- leaderboards
- rewards
- learning systems
- AI lecturers
- complex reputation systems

while the basic discovery and contribution flow is broken.

A beautiful differentiator sitting on top of a broken product is not a successful hackathon project.

---

# 7. CORE USERS

There are two primary user groups.

## 7.1 Contributor

A developer who wants to find an open-source project to contribute to.

Their primary goal is:

> "Show me projects I can contribute to and help me understand where I can start."

They need:

- project discovery
- search
- filters
- project details
- issue discovery
- beginner-friendly issues
- issue details
- discussions
- GitHub contribution link
- optional issue claiming

## 7.2 Project Maintainer

A developer or team responsible for an open-source project.

Their primary goal is:

> "Help more developers discover my project and make it easier for them to contribute."

They need:

- project submission
- project metadata
- project issue synchronization
- project discussion
- developer identity
- ability to maintain the Code Masters-specific project information they are responsible for

A person can be both a contributor and a maintainer.

---

# 8. THE CORE DOMAIN OBJECTS

The application revolves around these objects.

## 8.1 User

A developer using Code Masters.

A user has:

- Code Masters ID
- GitHub identity
- username
- display name
- avatar
- bio
- location
- skills
- projects
- contributions
- reputation

The GitHub account is the primary identity provider.

We do not create a separate password-based authentication system for the MVP.

---

## 8.2 Project

A project is an open-source GitHub repository listed on Code Masters.

A project contains information such as:

- name
- description
- GitHub URL
- owner
- primary language
- languages
- category
- tags
- country/region connection
- license
- stars
- forks
- open issues
- contributor count
- last activity
- verification state

Some information comes directly from GitHub.

Some information belongs specifically to Code Masters.

This distinction matters.

### GitHub-derived information

Examples:

- repository name
- description
- stars
- forks
- language
- GitHub URL
- issue information
- activity

### Code Masters information

Examples:

- Code Masters category
- Code Masters tags
- African/South African connection
- verification
- local/community context
- Code Masters discussions

---

## 8.3 Issue

An issue represents a contribution opportunity.

The issue may originate from a real GitHub issue.

We are NOT creating a second GitHub issue tracker.

We store enough information locally to make the issue discoverable and understandable on Code Masters.

An issue includes:

- project
- GitHub issue number
- title
- description/excerpt
- GitHub URL
- labels
- open/closed state
- difficulty
- beginner-friendly status
- useful contribution/quality information

When the user wants to actually work on it, they go to GitHub.

---

## 8.4 Comment

A comment is a discussion message written on Code Masters.

Comments can belong to:

- a project
- an issue

Comments are part of the Code Masters community layer.

This is one of the places where we deliberately add functionality around GitHub instead of trying to duplicate GitHub itself.

---

## 8.5 Claim

A claim represents:

> "I intend to work on this issue."

It is a Code Masters signal.

It is NOT a GitHub assignment.

It does not automatically assign the GitHub issue to the user.

The user can release the claim.

This feature may be treated as P1 if time becomes limited.

---

# 9. FEATURE LIST

## FEATURE 1: Project Discovery

Users can browse open-source projects.

The project list should show enough information for a developer to decide which project to investigate.

Possible displayed information:

- project name
- short description
- language
- category
- tags
- stars
- contributor count
- recent activity
- African/South African connection
- availability of beginner-friendly issues

Users should be able to paginate through projects.

---

# 10. FEATURE 2: Project Search

Users can search for projects.

Search should be able to consider relevant project information such as:

- name
- description
- owner
- topics/tags

Example:

```text
Search: "fintech"
```

Possible results:

```text
SA Payment Gateway
African FinTech API
Open Banking Toolkit
```

The exact search implementation can start simple.

Do not build a complex search engine unless the basic search is already working.

---

# 11. FEATURE 3: Project Filtering

Users can narrow project discovery using filters.

Initial filters include:

- programming language
- category
- tag
- country/connection
- presence of beginner-friendly issues
- sorting

Possible sorting:

- relevance
- recent activity
- stars
- contributor count

---

# 12. FEATURE 4: Project Detail Page

When a user opens a project, they should immediately understand:

1. What is this project?
2. Who is behind it?
3. What technologies does it use?
4. How active is it?
5. Where can I contribute?
6. What are other developers saying about it?

The page should contain:

- project name
- description
- GitHub link
- owner
- language
- tags
- category
- project statistics
- project activity
- maintainers
- contribution opportunities
- recent discussion

---

# 13. FEATURE 5: Issue Discovery

A project page should expose its contribution opportunities.

Users can filter issues by:

- difficulty
- label
- status

The most important experience is finding issues that are realistic starting points for contributors.

Example:

```text
Project: Open Source Banking API

Contribution opportunities

[Beginner]
Add validation for transaction amount

[Beginner]
Improve API documentation

[Intermediate]
Implement transaction filtering

[Advanced]
Refactor authentication module
```

The issue should link back to the actual GitHub issue.

---

# 14. FEATURE 6: Issue Detail

The issue detail page should explain the opportunity.

It should show:

- issue title
- issue description/excerpt
- project
- labels
- difficulty
- beginner-friendly status
- GitHub link
- relevant project information
- discussion
- active claim, if one exists

The key question the page must answer is:

> "Do I understand this enough to decide whether I can contribute?"

---

# 15. FEATURE 7: Community Discussion

Users can discuss projects and issues directly on Code Masters.

### Project discussion

Useful for questions such as:

- "Has anyone contributed to this project before?"
- "What should I know before getting started?"
- "Which part of the codebase should a beginner look at?"

### Issue discussion

Useful for:

- asking for clarification
- discussing possible approaches
- helping another contributor
- sharing context
- communicating with other developers

Authenticated users can create comments.

Public users can read comments.

---

# 16. FEATURE 8: GitHub Authentication

Users authenticate through GitHub OAuth.

Why?

Because the platform is about open-source contribution and GitHub is where the actual code lives.

This means users do not need to create another password account.

Authentication provides Code Masters with the user's GitHub identity.

Authentication is required for actions such as:

- commenting
- claiming an issue
- submitting a project
- editing their profile
- editing a project they are allowed to manage

Browsing remains public.

---

# 17. FEATURE 9: Developer Profiles

Every authenticated user has a developer profile.

The profile can show:

- username
- display name
- avatar
- bio
- location
- skills
- projects
- contributions
- reputation

The profile should eventually become a lightweight developer portfolio.

Do not build a full social network.

---

# 18. FEATURE 10: Submit a Project

Authenticated users can submit an open-source GitHub repository to Code Masters.

The submission starts with:

- GitHub URL
- project connection
- category
- tags

The backend then obtains relevant repository information from GitHub.

We should not ask users to manually type information that GitHub can provide.

---

# 19. FEATURE 11: GitHub Project Synchronization

Code Masters should retrieve real project information from GitHub.

This is important.

The demo should not depend on fake project statistics.

For a project, synchronization can retrieve/update:

- repository metadata
- languages
- stars
- forks
- issue count
- activity
- issues
- other useful GitHub information

Synchronization can be triggered for a project.

The API defines:

```text
POST /projects/{projectId}/issues/sync
```

The synchronization operation may be asynchronous.

---

# 20. FEATURE 12: Issue Claiming

An authenticated user can signal that they intend to work on an issue.

Example:

```text
Issue: Add CSV export

[Claim Issue]
```

After claiming:

```text
You intend to work on this issue.
[Release Claim]
[Contribute on GitHub]
```

Again:

**Claiming on Code Masters does NOT assign the GitHub issue.**

It is only a local signal.

This feature is useful but is not more important than project discovery, issue discovery and community discussion.

---

# 21. FEATURE 13: African/South African Project Context

This is part of the differentiator layer.

A project can have a Code Masters connection such as:

- South African
- Africa-focused
- Africa-led
- community verified

The purpose is to help users discover open-source work connected to the region.

This is NOT simply a geographic filter.

The larger goal is to make African open-source activity visible as an ecosystem.

---

# 22. FEATURE 14: Project Verification

A project can eventually have a verification state.

Example:

```text
✓ South African Project
```

or:

```text
✓ Africa-focused
```

Verification should have a clear definition.

Do not simply add a "verified" badge because it looks good.

The team must define what qualifies a project for each verification state before this becomes part of the public product.

If the team cannot establish a trustworthy verification process during the hackathon, keep this feature simple or mark it as future work.

---

# 23. FEATURE 15: Project Health

This belongs primarily to the differentiator layer.

Instead of showing only:

```text
★ 143 stars
```

Code Masters can eventually communicate whether the project appears healthy and approachable.

Potential signals include:

- recent activity
- issue activity
- contributor activity
- maintainer responsiveness
- freshness of issues
- number of open issues
- other observable GitHub signals

The purpose is NOT to declare that a project is "good" or "bad."

The purpose is to give contributors more useful context than a star count.

---

# 24. FEATURE 16: Contribution Suitability

This is a differentiator-layer feature.

Instead of simply showing:

```text
good first issue
```

Code Masters can eventually help answer:

> "Is this actually a sensible issue for me to attempt?"

Potential factors:

- issue age
- labels
- issue complexity
- project activity
- maintainer activity
- issue description quality
- required technology
- beginner-friendly signals

The exact scoring algorithm is NOT fixed by this document.

Do not invent a complicated scoring algorithm before Layer 1 is complete.

---

# 25. FEATURE 17: Unified Search

The API provides a unified search endpoint.

Users can search across:

- projects
- issues

Search can be narrowed by:

- language
- difficulty
- country/connection
- type

Example:

```text
q = Java
type = issues
difficulty = beginner
country = ZA
```

The result should help a developer move directly from:

```text
"I know Java"
```

to:

```text
"Here are open-source contribution opportunities involving Java."
```

---

# 26. WHAT WE ARE NOT BUILDING

This section is extremely important.

## We are NOT building GitHub.

We are not building:

- Git hosting
- repositories
- commits
- branches
- pull requests
- merge systems
- code review
- Git itself

GitHub already does these things.

---

## We are NOT building a full learning platform in the MVP.

The original concept includes:

- programming lessons
- coding challenges
- algorithms
- SQL challenges
- interview problems
- AI teaching
- voice teaching

Those are future ecosystem possibilities.

They are NOT part of Layer 1.

---

## We are NOT building a rewards marketplace.

The original concept includes:

- points
- rewards
- technology vouchers
- electronics discounts
- cloud credits
- conference opportunities

These are future possibilities.

They are NOT part of the first build.

---

## We are NOT building a social network.

We have discussion functionality.

That does NOT mean we are building:

- private messaging
- followers
- feeds
- stories
- social media profiles
- a social graph

Keep the community layer focused on open-source contribution.

---

## We are NOT building microservices.

For this hackathon, use a single backend application unless there is a compelling technical reason otherwise.

The goal is a working product, not an architecture diagram.

---

# 27. MVP PRIORITY

Every feature belongs to a priority.

## P0: MUST WORK

These are non-negotiable.

### Discovery

- project listing
- project search
- project filtering
- project detail

### Contribution

- issue listing
- issue filtering
- issue detail
- GitHub contribution link

### Community

- project comments
- issue comments

### GitHub

- real GitHub project data
- GitHub OAuth

These features must work together as one complete journey.

---

# 28. P1: BUILD AFTER P0

Once P0 works end-to-end:

- project submission
- developer profile
- issue claiming
- project synchronization improvements
- African/South African project context
- project verification
- project health information
- contribution suitability information

---

# 29. P2: ONLY IF EVERYTHING ELSE WORKS

Only consider these after the core product is stable:

- reputation
- leaderboard
- badges
- advanced recommendations
- intelligent contributor/project matching
- AI issue explanation
- AI contribution recommendations
- learning integration

P2 features must never delay P0.

---

# 30. THE SINGLE MOST IMPORTANT USER JOURNEY

The entire team should be able to explain this journey.

A new developer arrives.

### Step 1

They see African/South African open-source projects.

### Step 2

They search/filter for something interesting.

Example:

```text
Language: Java
Difficulty: Beginner
Country: South Africa
```

### Step 3

They open a project.

They see:

```text
What it does
Who maintains it
Technology
Activity
Issues
```

### Step 4

They find a contribution opportunity.

Example:

```text
Improve transaction validation
Beginner
Java
```

### Step 5

They open the issue.

They understand:

```text
What needs to be done
Why it matters
What project it belongs to
What GitHub issue it corresponds to
```

### Step 6

They ask a question or read existing discussion.

### Step 7

They optionally claim the issue.

### Step 8

They click:

```text
Contribute on GitHub
```

### Step 9

They continue the actual coding contribution on GitHub.

This is the product.

If this journey works beautifully, Code Masters has a viable hackathon MVP.

---

# 31. PRODUCT ARCHITECTURE

The expected architecture is:

```text
                    ┌──────────────────┐
                    │      User        │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │  Next.js / React │
                    │    Frontend      │
                    └────────┬─────────┘
                             │ HTTP/JSON
                             ▼
                    ┌──────────────────┐
                    │ Spring Boot API  │
                    │                  │
                    │ Controllers      │
                    │ Services         │
                    │ GitHub Client    │
                    │ Security         │
                    └──────┬─────┬─────┘
                           │     │
                    ┌──────┘     └───────────┐
                    ▼                        ▼
            ┌──────────────┐          ┌──────────────┐
            │ PostgreSQL   │          │ GitHub API   │
            │              │          │              │
            │ Users        │          │ Repositories │
            │ Projects     │          │ Issues       │
            │ Issues       │          │ Users        │
            │ Comments     │          │ Activity     │
            │ Claims       │          └──────────────┘
            └──────────────┘
```

GitHub remains the external source of truth for repository-level contribution data.

PostgreSQL stores the Code Masters application data and the local discovery index.

---

# 32. BACKEND API CONTRACT (v2)

The API specification is the technical contract between frontend and backend. This
section is a human-readable summary; `codemasters-api-spec.yaml` v2 is authoritative
for request/response shapes.

## Projects

```text
GET    /api/v1/projects
POST   /api/v1/projects
GET    /api/v1/projects/{projectId}
PATCH  /api/v1/projects/{projectId}
```

## Project Maintainers

```text
POST   /api/v1/projects/{projectId}/maintainers
DELETE /api/v1/projects/{projectId}/maintainers/{userId}
```

## Project Issues

```text
GET  /api/v1/projects/{projectId}/issues
POST /api/v1/projects/{projectId}/issues/sync
GET  /api/v1/sync-jobs/{jobId}
```

## Issues

```text
GET   /api/v1/issues/{issueId}
PATCH /api/v1/issues/{issueId}
```

## Project Comments

```text
GET  /api/v1/projects/{projectId}/comments
POST /api/v1/projects/{projectId}/comments
```

## Issue Comments

```text
GET  /api/v1/issues/{issueId}/comments
POST /api/v1/issues/{issueId}/comments
```

## Comments

```text
PATCH  /api/v1/comments/{commentId}
DELETE /api/v1/comments/{commentId}
```

## Claims

```text
POST   /api/v1/issues/{issueId}/claim
DELETE /api/v1/issues/{issueId}/claim
GET    /api/v1/issues/{issueId}/claims
```

## Search

```text
GET /api/v1/search
```

## Users

```text
GET   /api/v1/users/me
PATCH /api/v1/users/me
GET   /api/v1/users/{username}
```

## Authentication

```text
GET  /api/v1/auth/github
GET  /api/v1/auth/github/callback
POST /api/v1/auth/logout
```

## Health

```text
GET /api/v1/health
```

The OpenAPI specification is authoritative for request and response shapes.

---

# 33. DATABASE CONCEPT (v2)

The database contains these main concepts:

```text
User
 │
 ├──────── ProjectMaintainer ──────── Project
 │                                       │
 │                                       ├──────── Issue
 │                                       │            │
 │                                       │            └──────── Claim
 │                                       ├──────── ProjectTag
 │                                       └──────── ProjectCountry
 │
 └──────── Comment
               │
               ├── Project
               └── Issue

Project ──────── SyncJob
```

The main tables are:

```text
users
projects
project_tags
project_countries
project_maintainers
issues
comments
claims
sync_jobs
```

`project_maintainers` links a Code Masters user to a project with a role (`owner` or
`maintainer`) and is what authorizes editing a project's listing or triggering sync —
GitHub ownership (the `github_owner` field on `projects`) is a separate, informational
concept and does not by itself grant edit rights. `sync_jobs` records each GitHub
synchronization run so its status can be polled instead of guessed at.

The exact implementation details belong to the API design and database implementation.

---

# 34. GITHUB INTEGRATION RULES

There are several important rules.

## Rule 1

Do not fake GitHub data in the final demo.

Use real repositories wherever possible.

## Rule 2

Do not copy the entire GitHub platform into Code Masters.

Store only what the product needs.

## Rule 3

Always preserve the original GitHub URL.

Users need a clear path from Code Masters to GitHub.

## Rule 4

Code Masters is not the final place where code contribution happens.

The final contribution happens on GitHub.

## Rule 5

GitHub API failures must not destroy the entire application.

If practical, use stored/synchronised data so Code Masters can still display existing project information when GitHub is temporarily unavailable.

---

# 35. FRONTEND EXPECTED PAGES

The frontend should eventually have approximately these pages.

## Public

```text
/
```

Landing/discovery page.

```text
/projects
```

Project discovery.

```text
/projects/{id}
```

Project details.

```text
/projects/{id}/issues
```

Project contribution opportunities.

```text
/issues/{id}
```

Issue details and discussion.

```text
/users/{username}
```

Public developer profile.

## Authenticated

```text
/profile
```

Current user's profile.

```text
/projects/new
```

Submit project.

The exact routing can differ, but the product behavior should not.

---

# 36. TWO-PHASE BUILD PLAN

We have two weeks.

We start building NOW.

Do not treat 25 September as the first day of development.

The official build window is the final deadline/build phase.

---

## PHASE 1: BUILD THE FOUNDATION

### Goal

Build the entire discovery/community layer without the differentiator.

The phase is complete only when the core user journey works from frontend to database to GitHub and back.

### Phase 1 includes

- project model
- issue model
- user model
- comments
- project listing
- project search
- project filters
- project detail
- issue listing
- issue filters
- issue detail
- GitHub data retrieval
- GitHub OAuth
- comments
- GitHub contribution links
- basic project submission
- basic profile
- database
- API
- frontend
- deployment

### Phase 1 does NOT include

- advanced AI
- complex recommendations
- sophisticated scoring
- leaderboards
- rewards
- learning platform
- AI lecturer

---

# 37. PHASE 1 DEFINITION OF DONE

Phase 1 is DONE only when a person can perform this entire sequence:

```text
Open Code Masters
      ↓
Browse projects
      ↓
Search/filter projects
      ↓
Open a project
      ↓
See real GitHub information
      ↓
See project issues
      ↓
Filter issues
      ↓
Open an issue
      ↓
Read issue information
      ↓
Read discussion
      ↓
Log in with GitHub
      ↓
Comment
      ↓
Optionally claim issue
      ↓
Click GitHub
      ↓
Reach the actual GitHub contribution
```

If any major step is missing, Phase 1 is not complete.

---

# 38. PHASE 2: BUILD THE DIFFERENTIATOR

Only after Phase 1 is complete.

The team then chooses the strongest differentiating capability that can be implemented reliably.

The current candidate is:

## "Contribution Intelligence for African Open Source"

Instead of simply saying:

```text
Here are GitHub issues.
```

Code Masters can say:

```text
Here are African projects.
Here is how healthy they appear.
Here are the contribution opportunities.
Here is how approachable each opportunity appears.
Here is why this project/issue may be a good match for you.
```

This is much more defensible than simply building another "good first issue" website.

---

# 39. DIFFERENTIATOR DESIGN PRINCIPLE

The differentiator must use the foundation.

It should not feel like a completely separate feature.

Bad:

```text
Project directory
+
random AI chatbot
```

Good:

```text
Project discovery
       +
Project/issue data
       +
Developer profile
       +
Community context
       +
Contribution intelligence
```

The differentiator should make the existing discovery loop better.

---

# 40. FOUR-PERSON TEAM

The exact team split is up to the team.

However, the product naturally separates into:

## Track A: Backend/API

Responsible for:

- Spring Boot
- controllers
- services
- DTOs
- validation
- authentication
- API integration

## Track B: Database/GitHub integration

Responsible for:

- PostgreSQL
- JPA/repositories
- migrations
- GitHub API
- synchronization
- GitHub-derived data

## Track C: Frontend

Responsible for:

- Next.js
- project discovery
- project pages
- issue pages
- comments
- profile
- API integration

## Track D: Product/Integration/UX

Responsible for:

- UI/UX
- project seed data
- African/South African project research
- verification/content rules
- end-to-end integration
- testing
- demo flow
- pitch
- differentiator implementation support

This is a suggested ownership model, not a restriction.

---

# 41. TEAM WORKING RULE

The API contract should be agreed upon before everyone builds independently.

The frontend should not invent API responses.

The backend should not invent frontend requirements.

The database should not invent domain concepts independently.

The three technical documents should act as the shared contract.

If something needs to change:

1. Discuss it.
2. Decide why it needs to change.
3. Update the contract.
4. Tell the team.
5. Then implement.

---

# 42. DEMO REQUIREMENTS

The final demo should use real data.

The strongest demo is not:

> "Imagine if a developer could..."

It is:

> "Let me show you."

The presenter should be able to:

1. Open Code Masters.
2. Show real African/South African projects.
3. Search/filter.
4. Open a project.
5. Show project information.
6. Show real GitHub issues.
7. Show a beginner-friendly opportunity.
8. Open the issue.
9. Show community discussion.
10. Log in.
11. Comment or claim.
12. Click through to GitHub.
13. Show the differentiator.

The judge should understand the product without needing a long technical explanation.

---

# 43. SUCCESS CRITERIA

A successful Code Masters prototype should demonstrate all of the following:

### Real

It uses real GitHub projects and issues.

### Useful

A developer can genuinely use it to discover an open-source contribution.

### Local

African/South African open-source work is visible as an ecosystem.

### Community-driven

Developers can communicate around projects and contribution opportunities.

### Connected

The platform leads users from discovery to actual contribution on GitHub.

### Differentiated

The second layer provides something beyond a generic GitHub issue directory.

### Working

The core flow works end-to-end.

---

# 44. FINAL PRODUCT DEFINITION

If someone asks:

## "What are you building?"

The answer is:

> **Code Masters is an African open-source discovery and contribution platform. It creates a front door to open-source projects that are otherwise scattered across GitHub. Developers can discover projects, search by technology and category, inspect real contribution opportunities, discuss projects and issues with other developers, and then move to GitHub to make the actual contribution. After this foundation works, we add a contribution-intelligence layer that gives developers richer African/local context and helps them identify projects and issues that are genuinely approachable and worth contributing to.**

If someone asks:

## "Are you competing with GitHub?"

The answer is:

> **No. GitHub is where the code lives and where contributions happen. Code Masters is the discovery and community layer before GitHub.**

If someone asks:

## "What is your core feature?"

The answer is:

> **Helping a developer go from "I want to contribute to African open source" to "I found a project and a specific issue I can realistically work on."**

If someone asks:

## "What makes it different?"

The answer is:

> **The foundation is built around African open-source discovery and community context, and the second layer adds contribution intelligence rather than simply copying GitHub's issue list.**

---

# 45. NON-NEGOTIABLE DEVELOPMENT ORDER

The order is:

```text
1. Agree on product definition
             ↓
2. Agree on API contract
             ↓
3. Build database
             ↓
4. Build backend
             ↓
5. Connect GitHub
             ↓
6. Build frontend
             ↓
7. Connect frontend + backend
             ↓
8. Test complete discovery/contribution journey
             ↓
9. DECLARE LAYER 1 COMPLETE
             ↓
10. Design/implement differentiator
             ↓
11. Polish
             ↓
12. Demo + submit
```

Do not reverse this order.

In particular:

**Do not build the differentiator before Layer 1 is complete.**

---

# 46. RELATIONSHIP TO THE OTHER PROJECT DOCUMENTS

This document answers:

> **WHAT are we building?**

The API specification answers:

> **WHAT does the backend expose?**

The API design answers:

> **HOW and WHY is the backend/API structured this way?**

The three documents should be supplied together to any LLM working on Code Masters.

An LLM should treat this document as the product scope and should not invent additional MVP features without explicitly identifying them as suggestions.

---

# END OF PRODUCT DEFINITION
