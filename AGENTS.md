# Repository Guidelines

This repository is a monorepo with a Next.js frontend and a Java Spring Boot REST API backend. The guidance below assumes a standard setup; update it once the project is initialized to reflect the actual structure and tooling.

- The frontend component is called freezer-tracking-web.
- The backend component is called freezer-tracking-api.
- Data storage is provided by a postgres database but use an in-memory h2 database for unit tests if needed.
- The supporting infrastructure is managed by Terraform and targets AWS.

## Security & Configuration Tips

- Use security best practices and have all web inputs sanitised for characters that might be used for cross site scripting attacks. Also validate and reject API requests that have such characters in them.
- Do not store passwords or keys or other sensitive material in source code. Use environment variables provided by a .env file.
- Keep secrets in `.env.local` and never commit them.
- Document required environment variables in `README.md` or `.env.example`.
- Enforce input length limits and format checks in API DTOs to align with database column sizes.
- Prefer CSP with nonces (via Next.js `proxy` in `apps/web/src/proxy.ts`) and avoid inline styles/scripts.
- Do not ship debug/auth diagnostic pages or seed credentials in production.
- Log authentication refusals with enough context to diagnose lockouts without leaking secrets.

## Project Structure & Module Organization

- `apps/web/` for the Next.js app.
- `apps/web/src/app/` for App Router routes, layouts, and page components.
- `apps/web/src/components/` for shared UI components.
- `apps/web/src/lib/` for utilities and API clients.
- `apps/web/src/types/` for shared TypeScript types.
- `apps/web/public/` for static assets (favicons, images).
- `apps/web/scripts/` for build-time scripts.
- `apps/api/` for the Spring Boot service.
- `apps/api/src/main/java/` for application code.
- `apps/api/src/test/java/` for unit and integration tests.
- `apps/ios/` for the SwiftUI iOS app.
- `apps/ios/scripts/` for iOS support scripts (for example app icon generation).
- `interface/` for the OpenAPI contract between frontend and backend.
- `infra/terraform/` for infrastructure as code modules and environment configuration.

Document any additional folders (e.g., `styles/`, `content/`) with examples.

## Infrastructure Management (Terraform)

- Manage cloud infrastructure through Terraform in `infra/terraform/`.
- Keep Terraform changes small, reviewed, and in the same PR as related app changes when they must ship together.
- Do not hardcode secrets, account IDs, or credentials in Terraform; pass sensitive values via environment variables or a secure secrets manager.
- Prefer reusable modules and shared variables over duplicated resource blocks.
- Run formatting and validation before merging infrastructure changes:
  - `terraform -chdir=infra/terraform fmt -recursive`
  - `terraform -chdir=infra/terraform validate`
- For any infrastructure change, include a plan in the PR description and apply only through the agreed deployment workflow (do not apply ad hoc from a local machine unless explicitly required).

## Database Schema Management

- Use Liquibase for database schema management.
- Use the `public` schema in Postgres.

## Build, Test, and Development Commands

Expected commands for each app:

- `npm --prefix apps/web run dev` — start the Next.js dev server.
- `npm --prefix apps/web run build` — create the Next.js production build.
- `npm --prefix apps/web run start` — start the Next.js production server.
- `npm --prefix apps/web run lint` — run frontend lint checks.
- `npm --prefix apps/web test` — run frontend tests.
- `npm --prefix apps/web run assets` — copy GOV.UK Frontend assets into `public/assets`.
- `./apps/api/gradlew bootRun` — run the API locally (Gradle wrapper).
- `./apps/api/gradlew test` — run API tests (Gradle wrapper).
- `npm run lint:interface` — validate the OpenAPI spec with Spectral.
- `npm run api:run` — run the API via the Gradle wrapper.
- `npm run api:test` — run API tests via the Gradle wrapper.
- `npm run test` — run all tests (web, API, interface).
- `podman compose up -d` — start Compose in detached mode.
- `podman compose up -d --build` — start Compose in detached mode and build images.

Keep this list aligned with `package.json` scripts.

## Coding Style & Naming Conventions

- Frontend indentation: 2 spaces; no tabs.
- Frontend naming: `PascalCase` components, `camelCase` functions, `kebab-case` route segments.
- Backend naming: Java standard (classes `PascalCase`, methods `camelCase`, packages `lowercase`).
- Formatting: `prettier` and `eslint` for frontend; `spotless` or `google-java-format` for backend (document the choice).
- Use Java 21 for backend builds.

## Testing Guidelines

Current frameworks: `jest` for frontend tests, `junit` for backend tests.

- Frontend unit tests: `*.test.ts` or `*.spec.ts`.
- Backend tests: `*Test.java` under `apps/api/src/test/java/`.
- E2E tests: `apps/web/tests/e2e/` (if used).
- Include accessibility checks where possible (e.g., `axe`).
- When adding new app pages, update `apps/web/tests/csp.integration.test.ts` to include the route in the unauthenticated redirect assertions.

## Commit & Pull Request Guidelines

Use Conventional Commits (e.g., `feat: add header`). Pull requests should include:

- Summary and rationale.
- Linked issue/ticket.

## Versioning References

- Root workspace version: `package.json` (`version`)
- Web app version: `apps/web/package.json` (`version`)
- API app version: `apps/api/build.gradle` (`version = '...'`)
- Infrastructure image tags: `infra/terraform/terraform.tfvars` (`api_image_tag`, `web_image_tag`) when pinning releases for deployment


## Agent-Specific Instructions

- Keep changes small and focused.
- Use UK English spelling in all documentation including commit messages, comments, methods and classes.
- When adding new APIs, update the OpenAPI spec in `interface/` first and ensure CORS config matches the new endpoints.
- New functionality should be supported by unit tests.
- Next.js 16 uses `apps/web/src/proxy.ts` instead of `middleware.ts` for request interception (including CSP headers).
- Prefer Jest for React/TypeScript unit testing.
- After starting Compose, always check container logs to confirm services are running.
- Use `podman` instead of `docker` for local image builds and Compose commands.
- Run Compose in detached mode (e.g., `podman compose up -d`).
- When using dates and times across APIs, use ISO 8601 formatted strings.
- For Terraform changes, update only `infra/terraform/` unless a shared contract change requires app updates in the same PR.
- When a small, complete chunk of work is finished, suggest making a commit before moving on.
- When making a change, always suggest an appropriate semantic version bump and identify which version file(s) should be updated.
- When application versions are bumped, update infrastructure image tag inputs (`api_image_tag` and/or `web_image_tag`) to match the new release versions.
- Treat database schema/data migrations as backwards-compatible by default so new and previous application versions can run concurrently during rolling deployments.
