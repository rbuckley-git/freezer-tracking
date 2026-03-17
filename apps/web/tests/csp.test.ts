import assert from 'node:assert/strict';
import { test } from 'node:test';
import { NextRequest, NextResponse } from 'next/server';
import { proxy } from '../src/proxy';

function makeRequest(path: string, baseUrl = 'http://localhost:10000') {
  const url = new URL(path, baseUrl);
  return new NextRequest(new Request(url));
}

function assertBaselineSecurityHeaders(response: NextResponse) {
  assert.equal(response.headers.get('X-Content-Type-Options'), 'nosniff');
  assert.equal(response.headers.get('Referrer-Policy'), 'strict-origin-when-cross-origin');
  assert.equal(response.headers.get('X-Frame-Options'), 'DENY');
  assert.equal(response.headers.get('Permissions-Policy'), 'camera=(), microphone=(), geolocation=()');
}

test('proxy sets CSP and nonce headers for app routes in production', () => {
  const previousEnv = process.env.NODE_ENV;
  const previousApiBaseUrl = process.env.API_BASE_URL;
  process.env.NODE_ENV = 'production';
  process.env.API_BASE_URL = 'https://freezer-api.learnsharegrow.io';
  const response = proxy(makeRequest('/'));
  const csp = response.headers.get('Content-Security-Policy');
  const nonce = response.headers.get('x-nonce');

  assertBaselineSecurityHeaders(response);
  assert.equal(response.headers.get('Strict-Transport-Security'), null);
  assert.ok(csp, 'expected CSP header to be set');
  assert.ok(csp?.includes("default-src 'self'"));
  assert.ok(csp?.includes("connect-src 'self' https://freezer-api.learnsharegrow.io"));
  assert.equal(csp?.includes("connect-src 'self' http: https:"), false);
  assert.ok(nonce, 'expected nonce header to be set');
  process.env.NODE_ENV = previousEnv;
  process.env.API_BASE_URL = previousApiBaseUrl;
});

test('proxy does not allow non-local HTTP API origins in production connect-src', () => {
  const previousEnv = process.env.NODE_ENV;
  const previousApiBaseUrl = process.env.API_BASE_URL;
  process.env.NODE_ENV = 'production';
  process.env.API_BASE_URL = 'http://example.internal:9080';

  const response = proxy(makeRequest('/'));
  const csp = response.headers.get('Content-Security-Policy');

  assertBaselineSecurityHeaders(response);
  assert.equal(response.headers.get('Strict-Transport-Security'), null);
  assert.ok(csp, 'expected CSP header to be set');
  assert.ok(csp?.includes("connect-src 'self'"));
  assert.equal(csp?.includes('example.internal'), false);

  process.env.NODE_ENV = previousEnv;
  process.env.API_BASE_URL = previousApiBaseUrl;
});

test('proxy skips CSP for static assets in production', () => {
  const previousEnv = process.env.NODE_ENV;
  process.env.NODE_ENV = 'production';
  const response = proxy(makeRequest('/_next/static/chunk.js'));
  const csp = response.headers.get('Content-Security-Policy');

  assertBaselineSecurityHeaders(response);
  assert.equal(response.headers.get('Strict-Transport-Security'), null);
  assert.equal(csp, null);
  process.env.NODE_ENV = previousEnv;
});

test('proxy skips CSP in non-production environments', () => {
  const previousEnv = process.env.NODE_ENV;
  process.env.NODE_ENV = 'development';
  const response = proxy(makeRequest('/'));
  const csp = response.headers.get('Content-Security-Policy');

  assertBaselineSecurityHeaders(response);
  assert.equal(response.headers.get('Strict-Transport-Security'), null);
  assert.equal(csp, null);
  process.env.NODE_ENV = previousEnv;
});

test('proxy sets HSTS only for production HTTPS requests', () => {
  const previousEnv = process.env.NODE_ENV;
  process.env.NODE_ENV = 'production';

  const httpsResponse = proxy(makeRequest('/login', 'https://freezer.learnsharegrow.io'));
  const httpResponse = proxy(makeRequest('/login', 'http://localhost:10000'));

  assertBaselineSecurityHeaders(httpsResponse);
  assertBaselineSecurityHeaders(httpResponse);
  assert.equal(
    httpsResponse.headers.get('Strict-Transport-Security'),
    'max-age=31536000; includeSubDomains; preload'
  );
  assert.equal(httpResponse.headers.get('Strict-Transport-Security'), null);

  process.env.NODE_ENV = previousEnv;
});
