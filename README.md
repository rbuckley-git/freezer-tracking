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

## Clean Machine Bootstrap

On a clean machine, install the repository tooling before running any root `npm run ...` command. The root scripts use packages from the root `node_modules` directory, including `dotenv-cli`, so they will fail with `dotenv: command not found` until the root dependencies are installed.

Prerequisites:
- Node.js 20+
- npm
- Java 21
- Podman and Podman Compose

Bootstrap steps:
- `brew install openjdk@21`
- `npm install`
- `npm --prefix apps/web install`
- `cp .env.example .env.local`
- Create `.env.api` and `.env.web` as needed for local commands and Compose

If your network uses TLS interception, make sure [certs/ZscalerRootCertificate-2048-SHA256.pem](/Users/richb/git/freezer-tracking/certs/ZscalerRootCertificate-2048-SHA256.pem) contains the correct corporate root certificate for that machine and network before building containers. An outdated or incorrect certificate can cause Docker build failures such as `UNABLE_TO_GET_ISSUER_CERT_LOCALLY`.

Common first-run commands:
- `npm run api:build`
- `npm run api:test`
- `npm --prefix apps/web run build`
- `podman compose up -d --build`

If you only need the frontend package dependencies refreshed, `npm --prefix apps/web install` is enough. If you want to use the root helper scripts such as `npm run api:build`, `npm run api:run`, or `npm run dev`, you must run `npm install` at the repository root first.

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
- `npm run api:build`
- `./apps/api/gradlew bootRun`
- `./apps/api/gradlew test`

Interface:
- `npm run lint:interface`

All tests:
- `npm run test`

Compose:
- `podman compose up -d`
- `podman compose up -d --build`
- `IMAGE_PLATFORM=linux/amd64 podman compose up -d --build` — optional x86_64 override when you need images for an x86 host instead of the default ARM images.

Docker Swarm:
- `cp .env.stack.example .env.stack`
- Update `.env.stack` with registry image tags and secrets before deployment.
- `docker swarm init` — only needed once per cluster.
- `set -a && source .env.stack && set +a && docker stack deploy -c docker-stack.yml "${STACK_NAME}"` — deploy the database, API, and web stack.
- `docker stack services "${STACK_NAME}"` — inspect service rollout status.
- `docker service logs "${STACK_NAME}_api"` — inspect API logs after deployment.
- `docker stack rm "${STACK_NAME}"` — remove the stack.

Swarm notes:
- `docker stack deploy` does not build images, so build and push the API and web images before deploying the stack.
- The database service uses a local named volume and is constrained to a manager node to keep storage placement predictable. For a multi-node highly available database, replace that with shared or managed storage.
- Swarm ignores Compose startup ordering, so the API relies on its restart policy to come up once Postgres is ready.

Database:
- `npm run db:reset` — drop and recreate the public schema in the Postgres container.

Bootstrap admin user:
- `scripts/bootstrap-admin.sh <container_name> <username> <password> [house_name]`
- Requires `.env.api` with `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and `PASSWORD_PEPPER`.
- Uses `podman exec` to run `psql` inside the database container.
- Clears `accounts` and `houses` before inserting the admin user.

Production Compose:
- `podman compose up -d --build` — builds and runs Postgres, API, and web app.
- API and web image builds default to `linux/arm64`. Set `IMAGE_PLATFORM=linux/amd64` to build images that can run on x86_64 hosts.

ECS hard bounce:
- `scripts/ecs-bounce-services.sh` — scale API and web services down then up, waiting for each phase to complete.
