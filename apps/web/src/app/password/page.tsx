import { revalidatePath } from 'next/cache';
import AppMenu from '../../components/AppMenu';
import { requireAuth } from '../../lib/auth';
import { changePassword } from '../../lib/api';
import { consumeFlashMessage, setFlashMessage } from '../../lib/flashStore';
import { logoutAction } from '../actions';
import AutoLogout from './AutoLogout';

export const dynamic = 'force-dynamic';

async function changePasswordAction(formData: FormData) {
  'use server';
  const currentPassword = String(formData.get('currentPassword') || '');
  const newPassword = String(formData.get('newPassword') || '');
  if (!currentPassword || !newPassword) {
    return;
  }
  try {
    await changePassword(currentPassword, newPassword);
    await setFlashMessage('success', 'Password updated. You will be logged out in a few seconds.');
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unable to update password';
    await setFlashMessage('error', message);
  }
  revalidatePath('/password');
}

export default async function PasswordPage() {
  const auth = await requireAuth();
  const flash = await consumeFlashMessage();

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Change password</h1>
          <p>Update your account password.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>
      {flash && <div className="notice mt-16">{flash.message}</div>}

      <section className="card mt-24 max-w-520">
        <form action={changePasswordAction} className="grid gap-16">
          <div className="field">
            <label htmlFor="currentPassword">Current password</label>
            <input id="currentPassword" name="currentPassword" type="password" required autoComplete="current-password" />
          </div>
          <div className="field">
            <label htmlFor="newPassword">New password</label>
            <input id="newPassword" name="newPassword" type="password" required autoComplete="new-password" />
          </div>
          <button type="submit">Update password</button>
        </form>
      </section>

      {flash?.type === 'success' && (
        <form action={logoutAction} id="logout-after-password" />
      )}

      {flash?.type === 'success' && <AutoLogout />}
    </main>
  );
}
