# syntax=docker/dockerfile:1.6
FROM --platform=$BUILDPLATFORM node:20-bookworm-slim AS deps
WORKDIR /app
COPY certs/ZscalerRootCertificate-2048-SHA256.pem /usr/local/share/ca-certificates/zscaler.crt
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*
ENV NODE_EXTRA_CA_CERTS=/usr/local/share/ca-certificates/zscaler.crt
ENV NPM_CONFIG_CAFILE=/usr/local/share/ca-certificates/zscaler.crt
ENV NPM_CONFIG_CACHE=/root/.npm
COPY apps/web/package.json apps/web/package-lock.json* ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit

FROM --platform=$BUILDPLATFORM node:20-bookworm-slim AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY apps/web/ ./
RUN npm run build

FROM --platform=$TARGETPLATFORM node:20-bookworm-slim AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=10000
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public
USER node
EXPOSE 10000
CMD ["node", "server.js"]
