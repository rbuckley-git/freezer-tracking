import { revalidatePath } from 'next/cache';
import AppMenu from '../../components/AppMenu';
import { requireSuperAdmin } from '../../lib/auth';
import { createHouse, deleteHouse, listHouses, updateHouse } from '../../lib/api';
import HousesTable from './HousesTable';

export const dynamic = 'force-dynamic';

async function createHouseAction(formData: FormData) {
  'use server';
  const name = String(formData.get('name') || '').trim();
  if (!name) {
    return;
  }
  await createHouse({ name });
  revalidatePath('/houses');
}

async function updateHouseAction(formData: FormData) {
  'use server';
  const id = String(formData.get('id') || '');
  const name = String(formData.get('name') || '').trim();
  if (!id || !name) {
    return;
  }
  await updateHouse(id, { name });
  revalidatePath('/houses');
}

async function deleteHouseAction(formData: FormData) {
  'use server';
  const id = String(formData.get('id') || '');
  if (!id) {
    return;
  }
  await deleteHouse(id);
  revalidatePath('/houses');
}

export default async function HousesPage() {
  const auth = await requireSuperAdmin();
  const houses = await listHouses();

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Houses</h1>
          <p>Manage houses and their names.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>

      <section className="card mt-24">
        <h2>Create house</h2>
        <form action={createHouseAction} className="grid gap-16 mt-16">
          <div className="field">
            <label htmlFor="name">Name</label>
            <input id="name" name="name" required />
          </div>
          <button type="submit">Add house</button>
        </form>
      </section>

      <section className="card mt-24">
        <h2>Existing houses</h2>
        <HousesTable houses={houses} onUpdate={updateHouseAction} onDelete={deleteHouseAction} />
      </section>
    </main>
  );
}
