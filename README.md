# Freezer Tracking

Monorepo with a Next.js frontend and a Spring Boot REST API backend.

## Apps

- `apps/web/` — Next.js app
- `apps/api/` — Spring Boot service
- `apps/ios/` — SwiftUI iOS client
- `interface/` — OpenAPI contract
- `infra/terraform/` — AWS infrastructure as code

## Dependency Inventory

Workspace tooling dependencies:

- `@stoplight/spectral-cli` `^6.15.0` — OpenAPI linting for `interface/openapi.yaml`
- `concurrently` `8.2.2` — run the web app and API together in local development
- `dotenv-cli` `^11.0.0` — load `.env` files for workspace scripts

Component dependency inventories:

- [apps/web/README.md](/Users/richb/git/freezer-tracking/apps/web/README.md)
- [apps/api/README.md](/Users/richb/git/freezer-tracking/apps/api/README.md)
- [apps/ios/README.md](/Users/richb/git/freezer-tracking/apps/ios/README.md)
- [infra/terraform/README.md](/Users/richb/git/freezer-tracking/infra/terraform/README.md)

## Environment

Copy `.env.example` to `.env.local` and fill in values. Never commit secrets.

The API runs on port 9080 by default. Use `NEXT_PUBLIC_API_BASE_URL` or `API_BASE_URL` (for example `http://127.0.0.1:9080`) to point the web app at the API.
For the local Postgres container, the default host port is `5434`.

`PASSWORD_PEPPER` is required to validate passwords. The initial account seed hash was generated using `PASSWORD_PEPPER=change-me-pepper`. If you change the pepper, update the stored hash in the `accounts` table accordingly.

`COOKIE_KEY` must be a 16 character string and is supplied via `.env.local`.

## iOS Simulator TLS Trust

If your network uses TLS interception (for example Zscaler), install the corporate root CA into the booted iOS simulator:

```bash
xcrun simctl keychain booted add-root-cert /full/path/to/ZscalerRoot.cer
```

Then in the simulator open `Settings -> General -> About -> Certificate Trust Settings` and enable trust for the installed certificate.

## Commands

Frontend (runs on port 10000):
- `npm --prefix apps/web run dev`
- `npm --prefix apps/web run build`
- `npm --prefix apps/web run start`
- `npm --prefix apps/web run lint`
- `npm --prefix apps/web test`
- `npm --prefix apps/web run assets`

Backend:
- `./apps/api/gradlew bootRun`
- `./apps/api/gradlew test`

Interface:
- `npm run lint:interface`

All tests:
- `npm run test`

Compose:
- `podman compose up -d`
- `podman compose up -d --build`

Database:
- `npm run db:reset` — drop and recreate the public schema in the Postgres container.

Bootstrap admin user:
- `scripts/bootstrap-admin.sh <container_name> <username> <password> [house_name]`
- Requires `.env.api` with `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and `PASSWORD_PEPPER`.
- Uses `podman exec` to run `psql` inside the database container.
- Clears `accounts` and `houses` before inserting the admin user.

Production Compose:
- `podman compose up -d --build` — builds and runs Postgres, API, and web app.

ECS hard bounce:
- `scripts/ecs-bounce-services.sh` — scale API and web services down then up, waiting for each phase to complete.
