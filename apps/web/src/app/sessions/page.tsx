import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import AppMenu from '../../components/AppMenu';
import { consumeFlashMessage, setFlashMessage } from '../../lib/flashStore';
import { listAccountApiKeys, listAccounts, revokeAccountApiKey, revokeAllAccountApiKeys } from '../../lib/api';
import { requireAdmin } from '../../lib/auth';

export const dynamic = 'force-dynamic';

async function revokeSessionAction(formData: FormData) {
  'use server';
  const accountId = String(formData.get('accountId') || '');
  const apiKey = String(formData.get('apiKey') || '');
  if (!accountId || !apiKey) {
    return;
  }
  try {
    await revokeAccountApiKey(accountId, apiKey);
  } catch (error) {
    await handleSessionError(error);
    return;
  }
  revalidatePath('/sessions');
}

async function revokeAllSessionsAction(formData: FormData) {
  'use server';
  const accountId = String(formData.get('accountId') || '');
  if (!accountId) {
    return;
  }
  try {
    await revokeAllAccountApiKeys(accountId);
  } catch (error) {
    await handleSessionError(error);
    return;
  }
  revalidatePath('/sessions');
}

async function handleSessionError(error: unknown) {
  const message = error instanceof Error ? error.message : '';
  if (message.includes('403') || message.toLowerCase().includes('forbidden')) {
    return redirect('/login');
  }
  await setFlashMessage('error', message || 'Unable to update sessions.');
  redirect('/sessions');
}

function formatTimestamp(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (input: number) => String(input).padStart(2, '0');
  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());
  return `${year}-${month}-${day} ${hours}:${minutes}`;
}

function formatKey(value: string) {
  if (value.length <= 12) return value;
  return `${value.slice(0, 8)}…${value.slice(-4)}`;
}

function formatClientType(value: 'WEB' | 'IOS' | 'LEGACY') {
  switch (value) {
    case 'WEB':
      return 'Web';
    case 'IOS':
      return 'iOS';
    case 'LEGACY':
      return 'Legacy';
  }
}

function statusFor(expiresAt?: string | null, revokedAt?: string | null) {
  if (revokedAt) return 'Revoked';
  if (!expiresAt) return 'Unknown';
  const expiry = new Date(expiresAt).getTime();
  if (Number.isNaN(expiry)) return 'Unknown';
  return expiry <= Date.now() ? 'Expired' : 'Active';
}

export default async function SessionsPage() {
  const auth = await requireAdmin();
  const flash = await consumeFlashMessage();
  const accounts = await listAccounts();
  const sessionsByAccount = await Promise.all(
    accounts.map(async (account) => ({
      account,
      sessions: await listAccountApiKeys(account.id)
    }))
  );

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>API key sessions</h1>
          <p>Review active sessions and revoke access when needed.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>
      {flash && <div className="notice mt-16">{flash.message}</div>}

      {sessionsByAccount.map(({ account, sessions }) => (
        <section key={account.id} className="card mt-24">
          <div className="actions actions-between">
            <div>
              <h2>Sessions for {account.username}</h2>
            </div>
            <form action={revokeAllSessionsAction}>
              <input type="hidden" name="accountId" value={account.id} />
              <button type="submit" className="secondary">Revoke all sessions</button>
            </form>
          </div>

          {sessions.length === 0 ? (
            <p className="mt-16">No sessions found for this account.</p>
          ) : (
            <table className="table mt-16">
              <thead>
                <tr>
                  <th>Key</th>
                  <th>Client</th>
                  <th>Created</th>
                  <th>Last used</th>
                  <th>Expires</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((session) => (
                  <tr key={session.apiKey}>
                    <td>{formatKey(session.apiKey)}</td>
                    <td>{formatClientType(session.clientType)}</td>
                    <td>{formatTimestamp(session.createdAt)}</td>
                    <td>{formatTimestamp(session.lastUsedAt)}</td>
                    <td>{formatTimestamp(session.expiresAt)}</td>
                    <td>{statusFor(session.expiresAt, session.revokedAt)}</td>
                    <td>
                      <form action={revokeSessionAction}>
                        <input type="hidden" name="accountId" value={account.id} />
                        <input type="hidden" name="apiKey" value={session.apiKey} />
                        <button type="submit" className="secondary">Revoke</button>
                      </form>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      ))}
    </main>
  );
}
