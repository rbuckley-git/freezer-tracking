'use server';

import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import { createItem, deleteItem, login, logout, updateItem } from '../lib/api';
import { clearAuthCookie, setAuthCookie } from '../lib/auth';
import { clearFlashMessage, clearFormState, setFlashMessage, setFormState } from '../lib/flashStore';

function getString(formData: FormData, key: string) {
  const value = formData.get(key);
  return value ? value.toString() : '';
}

function getNumber(formData: FormData, key: string) {
  const value = formData.get(key);
  return value ? Number(value) : 0;
}

function getOptionalString(formData: FormData, key: string) {
  const value = formData.get(key)?.toString().trim();
  return value ? value : undefined;
}

function isRedirectError(error: unknown): error is { digest: string } {
  return typeof error === 'object'
    && error !== null
    && 'digest' in error
    && typeof (error as { digest: string }).digest === 'string'
    && (error as { digest: string }).digest.startsWith('NEXT_REDIRECT');
}

export async function createItemAction(formData: FormData) {
  let created;
  try {
    created = await createItem({
      reference: getString(formData, 'reference'),
      freezeDate: getString(formData, 'freezeDate'),
      bestBefore: getString(formData, 'bestBefore'),
      description: getString(formData, 'description'),
      freezerId: getNumber(formData, 'freezerId'),
      shelfNumber: getNumber(formData, 'shelfNumber'),
      weight: getOptionalString(formData, 'weight'),
      size: getOptionalString(formData, 'size') as 'S' | 'M' | 'L' | undefined
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unable to create item';
    await setFormState({
      reference: getString(formData, 'reference'),
      freezeDate: getString(formData, 'freezeDate'),
      bestBefore: getString(formData, 'bestBefore'),
      description: getString(formData, 'description'),
      freezerId: String(getNumber(formData, 'freezerId')),
      shelfNumber: String(getNumber(formData, 'shelfNumber')),
      weight: getString(formData, 'weight'),
      size: getString(formData, 'size')
    });
    await setFlashMessage('error', message);
    redirect('/item');
  }

  revalidatePath('/');
  await clearFormState();
  await setFlashMessage('success', `Item ${created.reference} - ${created.description} added.`);
  redirect('/');
}

export async function updateItemAction(id: string, formData: FormData) {
  try {
    await updateItem(id, {
      reference: getString(formData, 'reference'),
      freezeDate: getString(formData, 'freezeDate'),
      bestBefore: getString(formData, 'bestBefore'),
      description: getString(formData, 'description'),
      freezerId: getNumber(formData, 'freezerId'),
      shelfNumber: getNumber(formData, 'shelfNumber'),
      weight: getOptionalString(formData, 'weight'),
      size: getOptionalString(formData, 'size') as 'S' | 'M' | 'L' | undefined
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unable to update item';
    await setFlashMessage('error', message);
    redirect(`/items/${id}/edit`);
  }

  revalidatePath('/');
  await setFlashMessage('success', 'Item updated.');
  redirect('/');
}

export async function deleteItemAction(formData: FormData) {
  const id = getString(formData, 'id');
  if (id) {
    await deleteItem(id);
  }

  revalidatePath('/');
  redirect('/');
}

export async function resetFiltersAction() {
  await clearFlashMessage();
  redirect('/');
}

export async function loginAction(formData: FormData) {
  const username = getString(formData, 'username');
  const password = getString(formData, 'password');
  try {
    const result = await login(username, password);
    await setAuthCookie({
      apiKey: result.apiKey,
      apiKeyExpiry: result.apiKeyExpiry,
      isAdmin: result.isAdmin,
      isSuperAdmin: result.isSuperAdmin,
      username
    });
  } catch (error) {
    if (isRedirectError(error)) {
      throw error;
    }
    const message = error instanceof Error ? error.message : 'Login failed';
    const safeMessage = message.startsWith('NEXT_REDIRECT') ? 'Login failed' : message;
    const friendlyMessage = safeMessage.includes('Forbidden')
      ? 'Invalid email or password or account lockout.'
      : safeMessage;
    await setFlashMessage('error', friendlyMessage);
    redirect('/login');
  }

  await clearFlashMessage();
  redirect('/');
}

export async function logoutAction() {
  try {
    await logout();
  } catch {
    // Ignore logout failures - client cookie will be cleared regardless.
  }
  await clearAuthCookie();
  redirect('/login');
}
