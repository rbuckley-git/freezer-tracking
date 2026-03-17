import { revalidatePath } from 'next/cache';
import AppMenu from '../../components/AppMenu';
import { requireSuperAdmin } from '../../lib/auth';
import { createConfig, deleteConfig, listConfig, updateConfig } from '../../lib/api';
import ConfigTable from './ConfigTable';

export const dynamic = 'force-dynamic';

async function createConfigAction(formData: FormData) {
  'use server';
  const key = String(formData.get('key') || '').trim();
  const value = String(formData.get('value') || '').trim();
  if (!key || !value) {
    return;
  }
  await createConfig({ key, value });
  revalidatePath('/config');
}

async function updateConfigAction(formData: FormData) {
  'use server';
  const key = String(formData.get('key') || '').trim();
  const value = String(formData.get('value') || '').trim();
  if (!key || !value) {
    return;
  }
  await updateConfig(key, { value });
  revalidatePath('/config');
}

async function deleteConfigAction(formData: FormData) {
  'use server';
  const key = String(formData.get('key') || '').trim();
  if (!key) {
    return;
  }
  await deleteConfig(key);
  revalidatePath('/config');
}

export default async function ConfigPage() {
  const auth = await requireSuperAdmin();
  const entries = await listConfig();

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Configuration</h1>
          <p>Manage system configuration values.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>

      <section className="card mt-24">
        <h2>Add configuration</h2>
        <form action={createConfigAction} className="grid gap-16 mt-16">
          <div className="field">
            <label htmlFor="key">Key</label>
            <input id="key" name="key" required />
          </div>
          <div className="field">
            <label htmlFor="value">Value</label>
            <input id="value" name="value" required />
          </div>
          <button type="submit">Add entry</button>
        </form>
      </section>

      <section className="card mt-24">
        <h2>Existing configuration</h2>
        <ConfigTable entries={entries} onUpdate={updateConfigAction} onDelete={deleteConfigAction} />
      </section>
    </main>
  );
}
