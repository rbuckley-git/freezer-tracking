import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const COOKIE_NAME = 'ft_api';

function isLocalHostname(hostname: string) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1';
}

function connectSrcDirective(isProduction: boolean) {
  const sources = new Set<string>(["'self'"]);
  const apiBaseUrl = process.env.API_BASE_URL;
  if (!apiBaseUrl) {
    return "connect-src 'self'";
  }

  try {
    const parsed = new URL(apiBaseUrl);
    const origin = parsed.origin;
    const protocol = parsed.protocol;
    const hostname = parsed.hostname;
    if (protocol === 'https:') {
      sources.add(origin);
    } else if (!isProduction && protocol === 'http:' && isLocalHostname(hostname)) {
      // Local HTTP API access is allowed for local development/testing only.
      sources.add(origin);
    }
  } catch {
    return "connect-src 'self'";
  }

  return `connect-src ${Array.from(sources).join(' ')}`;
}

function buildCsp(nonce: string, isProduction: boolean) {
  const scriptSrc = `script-src 'self' 'nonce-${nonce}'`;
  const styleSrc = `style-src 'self' 'nonce-${nonce}'`;
  return [
    "default-src 'self'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
    "object-src 'none'",
    "img-src 'self' data:",
    "font-src 'self' data:",
    connectSrcDirective(isProduction),
    scriptSrc,
    styleSrc
  ].join('; ');
}

function createNonce() {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return btoa(String.fromCharCode(...bytes));
}

function applySecurityHeaders(response: NextResponse, request: NextRequest, isProduction: boolean) {
  response.headers.set('X-Content-Type-Options', 'nosniff');
  response.headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');
  response.headers.set('X-Frame-Options', 'DENY');
  response.headers.set('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
  if (isProduction && request.nextUrl.protocol === 'https:') {
    response.headers.set('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
  }
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isProduction = process.env.NODE_ENV === 'production';
  const nonce = isProduction ? createNonce() : null;
  const requestHeaders = new Headers(request.headers);
  if (nonce) {
    requestHeaders.set('x-nonce', nonce);
  }

  const response = NextResponse.next({
    request: {
      headers: requestHeaders
    }
  });
  applySecurityHeaders(response, request, isProduction);

  if (
    pathname.startsWith('/_next') ||
    pathname.startsWith('/favicon') ||
    pathname.startsWith('/api')
  ) {
    return response;
  }

  if (isProduction && nonce) {
    const csp = buildCsp(nonce, isProduction);

    response.headers.set('Content-Security-Policy', csp);
    response.headers.set('x-nonce', nonce);
  }

  if (pathname.startsWith('/login')) {
    return response;
  }

  const token = request.cookies.get(COOKIE_NAME)?.value;
  if (!token) {
    const url = request.nextUrl.clone();
    url.pathname = '/login';
    const redirect = NextResponse.redirect(url);
    applySecurityHeaders(redirect, request, isProduction);
    if (isProduction && nonce) {
      const csp = buildCsp(nonce, isProduction);
      redirect.headers.set('Content-Security-Policy', csp);
      redirect.headers.set('x-nonce', nonce);
    }
    return redirect;
  }

  return response;
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)']
};
