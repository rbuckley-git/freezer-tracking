import Link from 'next/link';
import { createFreezer, deleteFreezer, getAccountHouse, listFreezers, listItems, updateFreezer } from '../../lib/api';
import { consumeFlashMessage, setFlashMessage } from '../../lib/flashStore';
import { revalidatePath } from 'next/cache';
import FreezerRow from '../../components/FreezerRow';
import { requireAuth } from '../../lib/auth';
import AppMenu from '../../components/AppMenu';

export const dynamic = 'force-dynamic';

async function updateFreezerAction(formData: FormData) {
  'use server';
  const id = Number(formData.get('id'));
  const name = String(formData.get('name') || '').trim();
  const shelfCount = Number(formData.get('shelfCount'));
  if (!id || !name || !shelfCount) {
    return;
  }
  try {
    await updateFreezer(id, { name, shelfCount });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unable to update freezer';
    await setFlashMessage('error', message);
    revalidatePath('/freezers');
    return;
  }
  revalidatePath('/freezers');
}

async function createFreezerAction(formData: FormData) {
  'use server';
  const name = String(formData.get('name') || '').trim();
  const shelfCount = Number(formData.get('shelfCount'));
  if (!name || !shelfCount) {
    return;
  }
  try {
    await createFreezer({ name, shelfCount });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Unable to create freezer';
    await setFlashMessage('error', message);
    revalidatePath('/freezers');
    return;
  }
  revalidatePath('/freezers');
}

async function deleteFreezerAction(formData: FormData) {
  'use server';
  const id = Number(formData.get('id'));
  const name = String(formData.get('name') || '').trim();
  if (!id) {
    return;
  }
  const freezerName = name || (await listFreezers()).find((freezer) => freezer.id === id)?.name || '';
  if (freezerName) {
    let page = 0;
    const size = 100;
    while (true) {
      const response = await listItems(page, size);
      if (response.items.some((item) => item.freezerName === freezerName)) {
        await setFlashMessage('error', `Cannot delete ${freezerName}. Move items out of this freezer first.`);
        revalidatePath('/freezers');
        return;
      }
      if (page >= response.totalPages - 1) {
        break;
      }
      page += 1;
    }
  }
  await deleteFreezer(id);
  revalidatePath('/freezers');
}

export default async function FreezerAdminPage() {
  const auth = await requireAuth();
  const flash = await consumeFlashMessage();
  const freezers = (await listFreezers()).sort((a, b) => a.name.localeCompare(b.name));
  const house = await getAccountHouse();

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Manage freezers for {house.name}</h1>
          <p>Edit freezer names and shelf counts.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>
      {flash && <div className="notice mt-16">{flash.message}</div>}

      <section className="card mt-24">
        <h2>Add freezer</h2>
        <form action={createFreezerAction} className="grid gap-16 mt-16">
          <div className="field">
            <label htmlFor="name">Name</label>
            <input id="name" name="name" required />
          </div>
          <div className="field">
            <label htmlFor="shelfCount">Shelves</label>
            <input id="shelfCount" name="shelfCount" type="number" min={1} max={10} defaultValue={5} required />
          </div>
          <button type="submit">Add freezer</button>
        </form>
      </section>

      <section className="card mt-24">
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th className="numeric">Shelves</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {freezers.map((freezer) => (
              <FreezerRow
                key={freezer.id}
                id={freezer.id}
                name={freezer.name}
                shelfCount={freezer.shelfCount}
                onSave={updateFreezerAction}
                onDelete={deleteFreezerAction}
              />
            ))}
          </tbody>
        </table>
      </section>
    </main>
  );
}
