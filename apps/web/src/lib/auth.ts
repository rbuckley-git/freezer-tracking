import 'server-only';

import crypto from 'crypto';
import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

const COOKIE_NAME = 'ft_api';
const EXPIRY_BUFFER_MS = 24 * 60 * 60 * 1000;

type AuthPayload = {
  apiKey: string;
  apiKeyExpiry: string | number;
  isAdmin: boolean;
  isSuperAdmin: boolean;
  username: string;
};

function getCookieKey(): Buffer {
  const raw = process.env.COOKIE_KEY || '';
  if (raw.length !== 16) {
    throw new Error('COOKIE_KEY must be a 16 character string');
  }
  return Buffer.from(raw, 'utf8');
}

function encrypt(payload: AuthPayload): string {
  const key = getCookieKey();
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-128-gcm', key, iv);
  const json = JSON.stringify(payload);
  const encrypted = Buffer.concat([cipher.update(json, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return [iv, encrypted, tag].map((part) => part.toString('base64url')).join('.');
}

function decrypt(token: string): AuthPayload | null {
  try {
    const [ivRaw, encryptedRaw, tagRaw] = token.split('.');
    if (!ivRaw || !encryptedRaw || !tagRaw) return null;
    const key = getCookieKey();
    const iv = Buffer.from(ivRaw, 'base64url');
    const encrypted = Buffer.from(encryptedRaw, 'base64url');
    const tag = Buffer.from(tagRaw, 'base64url');
    const decipher = crypto.createDecipheriv('aes-128-gcm', key, iv);
    decipher.setAuthTag(tag);
    const decrypted = Buffer.concat([decipher.update(encrypted), decipher.final()]).toString('utf8');
    const payload = JSON.parse(decrypted) as AuthPayload;
    if (!payload.apiKey || !payload.apiKeyExpiry) return null;
    if (typeof payload.isAdmin !== 'boolean') {
      payload.isAdmin = false;
    }
    if (typeof payload.isSuperAdmin !== 'boolean') {
      payload.isSuperAdmin = false;
    }
    if (typeof payload.username !== 'string') {
      payload.username = '';
    }
    return payload;
  } catch {
    return null;
  }
}

export function debugDecrypt(token: string): { payload: AuthPayload | null; error?: string } {
  try {
    const [ivRaw, encryptedRaw, tagRaw] = token.split('.');
    if (!ivRaw || !encryptedRaw || !tagRaw) {
      return { payload: null, error: 'Token format invalid' };
    }
    const key = getCookieKey();
    const iv = Buffer.from(ivRaw, 'base64url');
    const encrypted = Buffer.from(encryptedRaw, 'base64url');
    const tag = Buffer.from(tagRaw, 'base64url');
    const decipher = crypto.createDecipheriv('aes-128-gcm', key, iv);
    decipher.setAuthTag(tag);
    const decrypted = Buffer.concat([decipher.update(encrypted), decipher.final()]).toString('utf8');
    const payload = JSON.parse(decrypted) as AuthPayload;
    return { payload };
  } catch (error) {
    return { payload: null, error: error instanceof Error ? error.message : String(error) };
  }
}

function isExpiringSoon(expiryValue: string | number): boolean {
  const expiry =
    typeof expiryValue === 'number'
      ? expiryValue
      : new Date(expiryValue).getTime();
  if (Number.isNaN(expiry)) return false;
  return expiry - Date.now() <= EXPIRY_BUFFER_MS;
}

export async function setAuthCookie(payload: AuthPayload) {
  const token = encrypt(payload);
  const cookieStore = await cookies();
  cookieStore.set(COOKIE_NAME, token, {
    httpOnly: true,
    sameSite: 'strict',
    secure: process.env.NODE_ENV === 'production',
    path: '/'
  });
}

export async function clearAuthCookie() {
  const cookieStore = await cookies();
  cookieStore.set(COOKIE_NAME, '', {
    httpOnly: true,
    sameSite: 'strict',
    secure: process.env.NODE_ENV === 'production',
    path: '/',
    maxAge: 0
  });
}

export async function getAuthPayload(): Promise<AuthPayload | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get(COOKIE_NAME)?.value;
  if (!token) return null;
  const payload = decrypt(token);
  if (!payload) return null;
  if (isExpiringSoon(payload.apiKeyExpiry)) {
    return null;
  }
  return payload;
}

export async function requireAuth() {
  const payload = await getAuthPayload();
  if (!payload) {
    redirect('/login');
  }
  return payload;
}

export async function requireAdmin() {
  const payload = await requireAuth();
  if (!payload.isAdmin) {
    redirect('/');
  }
  return payload;
}

export async function requireSuperAdmin() {
  const payload = await requireAdmin();
  if (!payload.isSuperAdmin) {
    redirect('/');
  }
  return payload;
}

export async function getApiKey(): Promise<string | null> {
  const payload = await getAuthPayload();
  return payload?.apiKey ?? null;
}
