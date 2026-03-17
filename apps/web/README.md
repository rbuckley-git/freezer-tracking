# Freezer Tracking Web

Next.js frontend for the Freezer Tracking platform.

## Dependencies

Runtime dependencies:

- `next` `16.1.6` — App Router framework, server rendering, route handlers, and build tooling
- `react` `18.3.1` — component runtime
- `react-dom` `18.3.1` — DOM rendering for React
- `qrcode` `1.5.4` — generate QR code images for label printing

Development dependencies:

- `typescript` `5.5.4` — TypeScript compiler and type checking
- `tsx` `4.19.4` — execute TypeScript-based test files
- `eslint` `9.19.0` — JavaScript and TypeScript linting
- `eslint-config-next` `16.1.6` — Next.js ESLint rules
- `@types/node` `20.14.10` — Node.js type definitions
- `@types/react` `18.3.3` — React type definitions
- `@types/react-dom` `18.3.0` — React DOM type definitions
- `@types/qrcode` `^1.5.6` — TypeScript types for `qrcode`

Platform-provided modules used directly in the codebase:

- Node.js built-ins including `crypto`, `node:assert/strict`, `node:child_process`, and `node:test`
- Next.js features including `next/font/google`, `next/headers`, `next/navigation`, and `next/server`

## Commands

- `npm run dev`
- `npm run build`
- `npm run start`
- `npm run lint`
- `npm run test`
