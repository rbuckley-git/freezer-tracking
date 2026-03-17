import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import AppMenu from '../../components/AppMenu';
import { consumeFlashMessage, setFlashMessage } from '../../lib/flashStore';
import { requireAdmin } from '../../lib/auth';
import { createAccount, deleteAccount, getAccountHouse, listAccounts, listHouses, updateAccount } from '../../lib/api';
import AccountsTable from './AccountsTable';

export const dynamic = 'force-dynamic';

async function createAccountAction(formData: FormData) {
  'use server';
  const auth = await requireAdmin();
  const username = String(formData.get('username') || '').trim();
  const password = String(formData.get('password') || '');
  const houseId = String(formData.get('houseId') || '').trim();
  const requestedAdmin = formData.get('isAdmin') === 'on';
  const requestedSuperAdmin = formData.get('isSuperAdmin') === 'on';
  const isSuperAdmin = auth.isSuperAdmin ? requestedSuperAdmin : false;
  const isAdmin = requestedAdmin || isSuperAdmin;
  if (!username || !password || !houseId) {
    return;
  }
  try {
    await createAccount({ username, password, isAdmin, isSuperAdmin, houseId });
  } catch (error) {
    await handleAccountError(error);
    return;
  }
  revalidatePath('/accounts');
}

async function updateAccountAction(formData: FormData) {
  'use server';
  const auth = await requireAdmin();
  const id = String(formData.get('id') || '');
  const username = String(formData.get('username') || '').trim();
  const houseId = String(formData.get('houseId') || '').trim();
  const requestedAdmin = formData.get('isAdmin') === 'on';
  const requestedSuperAdmin = formData.get('isSuperAdmin') === 'on';
  const isSuperAdmin = auth.isSuperAdmin ? requestedSuperAdmin : false;
  const isAdmin = requestedAdmin || isSuperAdmin;
  if (!id || !username || !houseId) {
    return;
  }
  try {
    await updateAccount(id, { username, isAdmin, isSuperAdmin, houseId });
  } catch (error) {
    await handleAccountError(error);
    return;
  }
  revalidatePath('/accounts');
}

async function deleteAccountAction(formData: FormData) {
  'use server';
  const id = String(formData.get('id') || '');
  if (!id) {
    return;
  }
  try {
    await deleteAccount(id);
  } catch (error) {
    await handleAccountError(error);
    return;
  }
  revalidatePath('/accounts');
}

async function handleAccountError(error: unknown) {
  const message = error instanceof Error ? error.message : '';
  if (message.includes('403') || message.toLowerCase().includes('forbidden')) {
    return redirect('/login');
  }
  await setFlashMessage('error', message || 'Unable to update accounts.');
  redirect('/accounts');
}

export default async function AccountsPage() {
  const auth = await requireAdmin();
  const flash = await consumeFlashMessage();
  const accounts = await listAccounts();
  const houses = auth.isSuperAdmin
    ? await listHouses()
    : [await getAccountHouse()];

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Accounts</h1>
          <p>Create and manage user access.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>
      {flash && <div className="notice mt-16">{flash.message}</div>}

      <section className="card mt-24">
        <h2>Create account</h2>
        <form action={createAccountAction} className="grid gap-16 mt-16">
          <div className="field">
            <label htmlFor="username">Email</label>
            <input id="username" name="username" type="email" required />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" name="password" type="password" required />
          </div>
          <div className="field">
            <label htmlFor="houseId">House</label>
            <select id="houseId" name="houseId" required>
              {houses.map((house) => (
                <option key={house.id} value={house.id}>
                  {house.name}
                </option>
              ))}
            </select>
          </div>
          <label className="radio-pill">
            <input type="checkbox" name="isAdmin" />
            Admin user
          </label>
          {auth.isSuperAdmin && (
            <label className="radio-pill">
              <input type="checkbox" name="isSuperAdmin" />
              Super-admin user
            </label>
          )}
          <button type="submit">Add account</button>
        </form>
      </section>

      <section className="card mt-24">
        <h2>Existing accounts</h2>
        <AccountsTable
          accounts={accounts}
          houses={houses}
          canManageSuperAdmins={auth.isSuperAdmin}
          onUpdate={updateAccountAction}
          onDelete={deleteAccountAction}
        />
      </section>
    </main>
  );
}
