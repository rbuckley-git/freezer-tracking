import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { test } from 'node:test';

function randomPort() {
  return 20000 + Math.floor(Math.random() * 10000);
}

async function waitForServer(url: string, timeoutMs = 20000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      const response = await fetch(url, { redirect: 'manual' });
      return response;
    } catch {
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
  }
  throw new Error(`Timed out waiting for ${url}`);
}

async function fetchNoRedirect(url: string) {
  return fetch(url, { redirect: 'manual' });
}

let buildPromise: Promise<void> | null = null;

async function ensureProductionBuild() {
  if (!buildPromise) {
    buildPromise = new Promise<void>((resolve, reject) => {
      const child = spawn(
        'node',
        ['node_modules/next/dist/bin/next', 'build'],
        {
          cwd: process.cwd(),
          env: {
            ...process.env,
            NODE_ENV: 'production'
          },
          stdio: 'inherit'
        }
      );
      child.on('error', reject);
      child.on('exit', (code) => {
        if (code === 0) {
          resolve();
        } else {
          reject(new Error(`next build failed with code ${code ?? 'unknown'}`));
        }
      });
    });
  }
  return buildPromise;
}

async function startProductionServer(port: number) {
  await ensureProductionBuild();
  const child = spawn(
    'node',
    ['node_modules/next/dist/bin/next', 'start', '-p', String(port)],
    {
      cwd: process.cwd(),
      env: {
        ...process.env,
        NODE_ENV: 'production',
        API_BASE_URL: 'http://127.0.0.1:9999'
      },
      stdio: 'ignore'
    }
  );
  child.once('error', () => {});
  return child;
}

test('proxy applies CSP headers in a running app', async (t) => {
  const port = randomPort();
  const child = await startProductionServer(port);

  t.after(() => {
    if (!child.killed) {
      child.kill('SIGTERM');
    }
  });

  const response = await waitForServer(`http://127.0.0.1:${port}/login`);
  const csp = response.headers.get('Content-Security-Policy');
  const nonce = response.headers.get('x-nonce');

  assert.ok(csp, 'expected CSP header to be set');
  assert.ok(csp?.includes("default-src 'self'"));
  assert.ok(nonce, 'expected nonce header to be set');
});

test('proxy redirects protected routes to /login when unauthenticated', async (t) => {
  const port = randomPort();
  const child = await startProductionServer(port);

  t.after(() => {
    if (!child.killed) {
      child.kill('SIGTERM');
    }
  });

  await waitForServer(`http://127.0.0.1:${port}/login`);

  const response = await fetchNoRedirect(`http://127.0.0.1:${port}/`);
  const location = response.headers.get('location');

  assert.ok([307, 308].includes(response.status), `expected redirect status, got ${response.status}`);
  assert.ok(location?.startsWith('/login'), `expected redirect to /login, got ${location}`);
});

test('proxy redirects protected app pages to /login when unauthenticated', async (t) => {
  const port = randomPort();
  const child = await startProductionServer(port);

  t.after(() => {
    if (!child.killed) {
      child.kill('SIGTERM');
    }
  });

  await waitForServer(`http://127.0.0.1:${port}/login`);

  const protectedPaths = [
    '/',
    '/password',
    '/freezers',
    '/config',
    '/statistics',
    '/layout',
    '/houses',
    '/labels',
    '/sessions',
    '/accounts',
    '/item',
    '/items/00000000-0000-0000-0000-000000000000/edit'
  ];

  for (const path of protectedPaths) {
    const response = await fetchNoRedirect(`http://127.0.0.1:${port}${path}`);
    const location = response.headers.get('location');
    assert.ok([307, 308].includes(response.status), `expected redirect for ${path}, got ${response.status}`);
    assert.ok(location?.startsWith('/login'), `expected redirect to /login for ${path}, got ${location}`);
  }
});

test('proxy allows public privacy and support pages when unauthenticated', async (t) => {
  const port = randomPort();
  const child = await startProductionServer(port);

  t.after(() => {
    if (!child.killed) {
      child.kill('SIGTERM');
    }
  });

  await waitForServer(`http://127.0.0.1:${port}/login`);

  for (const path of ['/privacy', '/support']) {
    const response = await fetchNoRedirect(`http://127.0.0.1:${port}${path}`);
    assert.equal(response.status, 200, `expected public access for ${path}, got ${response.status}`);
  }
});
