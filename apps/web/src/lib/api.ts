import 'server-only';

import { Item, ItemCreate, ItemList, ItemUpdate } from '../types/items';
import { Freezer, FreezerCreate, FreezerUpdate } from '../types/freezers';
import { Statistics } from '../types/statistics';
import { getApiKey } from './auth';
import { redirect } from 'next/navigation';

const DEFAULT_API_BASE_URL = 'http://127.0.0.1:9080';

function apiBaseUrl() {
  return process.env.API_BASE_URL || DEFAULT_API_BASE_URL;
}

type RequestOptions = {
  skipAuth?: boolean;
};

async function request<T>(path: string, options?: RequestInit, requestOptions?: RequestOptions): Promise<T> {
  const apiKey = requestOptions?.skipAuth ? null : await getApiKey();
  const response = await fetch(`${apiBaseUrl()}${path}`, {
    cache: 'no-store',
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(apiKey ? { 'X-API-Key': apiKey } : {}),
      ...(options?.headers || {})
    }
  });

  if (!response.ok) {
    if (response.status === 403 && !requestOptions?.skipAuth) {
      redirect('/login');
    }
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      const body = (await response.json()) as { message?: string };
      throw new Error(body.message || `Request failed: ${response.status}`);
    }
    const message = await response.text();
    throw new Error(message || `Request failed: ${response.status}`);
  }

  if (response.status === 204) {
    return null as T;
  }

  return response.json() as Promise<T>;
}

export async function listItems(page = 0, size = 100): Promise<ItemList> {
  return request<ItemList>(`/items?page=${page}&size=${size}`);
}

export async function getItem(id: string): Promise<Item> {
  return request<Item>(`/items/${id}`);
}

export async function createItem(payload: ItemCreate): Promise<Item> {
  return request<Item>('/items', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateItem(id: string, payload: ItemUpdate): Promise<Item> {
  return request<Item>(`/items/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteItem(id: string): Promise<void> {
  await request<void>(`/items/${id}`, {
    method: 'DELETE'
  });
}

export async function listFreezers(): Promise<Freezer[]> {
  return request<Freezer[]>('/freezers');
}

export async function nextReference(): Promise<{ nextReference: string }> {
  return request<{ nextReference: string }>('/items/next-reference');
}

export async function createFreezer(payload: FreezerCreate): Promise<Freezer> {
  return request<Freezer>('/freezers', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateFreezer(id: number, payload: FreezerUpdate): Promise<Freezer> {
  return request<Freezer>(`/freezers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteFreezer(id: number): Promise<void> {
  await request<void>(`/freezers/${id}`, {
    method: 'DELETE'
  });
}

export async function login(
  username: string,
  password: string
): Promise<{ apiKey: string; apiKeyExpiry: string; isAdmin: boolean; isSuperAdmin: boolean }> {
  return request<{ apiKey: string; apiKeyExpiry: string; isAdmin: boolean; isSuperAdmin: boolean }>('/login', {
    method: 'POST',
    body: JSON.stringify({ username, password, clientType: 'WEB' })
  }, { skipAuth: true });
}

export async function logout(): Promise<void> {
  await request<void>('/logout', { method: 'POST' });
}

export async function listAccounts(): Promise<{
  id: string;
  username: string;
  isAdmin: boolean;
  isSuperAdmin: boolean;
  houseId: string;
}[]> {
  return request('/accounts');
}

export async function createAccount(payload: {
  username: string;
  password: string;
  isAdmin: boolean;
  isSuperAdmin: boolean;
  houseId: string;
}) {
  return request('/accounts', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateAccount(id: string, payload: {
  username: string;
  isAdmin: boolean;
  isSuperAdmin: boolean;
  houseId: string;
}) {
  return request(`/accounts/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteAccount(id: string) {
  await request(`/accounts/${id}`, { method: 'DELETE' });
}

export async function listAccountApiKeys(accountId: string): Promise<{
  apiKey: string;
  clientType: 'WEB' | 'IOS' | 'LEGACY';
  createdAt: string;
  expiresAt: string;
  lastUsedAt?: string | null;
  revokedAt?: string | null;
  deviceLabel?: string | null;
}[]> {
  return request(`/accounts/${accountId}/api-keys`);
}

export async function revokeAccountApiKey(accountId: string, apiKey: string) {
  await request(`/accounts/${accountId}/api-keys/revoke`, {
    method: 'POST',
    body: JSON.stringify({ apiKey })
  });
}

export async function revokeAllAccountApiKeys(accountId: string) {
  await request(`/accounts/${accountId}/api-keys/revoke-all`, { method: 'POST' });
}

export async function changePassword(currentPassword: string, newPassword: string) {
  await request('/account/password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword })
  });
}

export async function listHouses(): Promise<{ id: string; name: string }[]> {
  return request('/houses');
}

export async function createHouse(payload: { name: string }) {
  return request('/houses', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateHouse(id: string, payload: { name: string }) {
  return request(`/houses/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteHouse(id: string) {
  await request(`/houses/${id}`, { method: 'DELETE' });
}

export async function getAccountHouse(): Promise<{ id: string; name: string }> {
  return request('/account/house');
}

export async function listConfig(): Promise<{ key: string; value: string }[]> {
  return request('/config');
}

export async function getStatistics(): Promise<Statistics> {
  return request('/statistics');
}

export async function createConfig(payload: { key: string; value: string }) {
  return request('/config', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export async function updateConfig(key: string, payload: { value: string }) {
  return request(`/config/${encodeURIComponent(key)}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });
}

export async function deleteConfig(key: string) {
  await request(`/config/${encodeURIComponent(key)}`, { method: 'DELETE' });
}
