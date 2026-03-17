import Link from 'next/link';
import { createItemAction, deleteItemAction } from '../actions';
import { getItem, listFreezers, nextReference } from '../../lib/api';
import { consumeFlashMessage, consumeFormState } from '../../lib/flashStore';
import { requireAuth } from '../../lib/auth';
import AppMenu from '../../components/AppMenu';
import AddItemForm from './AddItemForm';

export const dynamic = 'force-dynamic';

type SearchParams = {
  id?: string;
};

function isRedirectError(error: unknown): error is { digest: string } {
  return typeof error === 'object'
    && error !== null
    && 'digest' in error
    && typeof (error as { digest: string }).digest === 'string'
    && (error as { digest: string }).digest.startsWith('NEXT_REDIRECT');
}

function throwIfRedirect(result: PromiseSettledResult<unknown>) {
  if (result.status === 'rejected' && isRedirectError(result.reason)) {
    throw result.reason;
  }
}

export default async function ItemPage({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const auth = await requireAuth();
  const resolved = (await searchParams) ?? {};
  const id = resolved.id?.toString();
  const flash = await consumeFlashMessage();
  const formState = await consumeFormState();
  const today = new Date();
  const freezeDefault = today.toISOString().slice(0, 10);
  const bestBeforeDefault = new Date(today.getFullYear(), today.getMonth() + 6, today.getDate())
    .toISOString()
    .slice(0, 10);
  const formValue = (key: string, fallback: string) => formState?.[key] ?? fallback;

  if (id) {
    const [itemResult, freezersResult] = await Promise.allSettled([getItem(id), listFreezers()]);
    [itemResult, freezersResult].forEach(throwIfRedirect);
    const item = itemResult.status === 'fulfilled' ? itemResult.value : null;
    const freezers = freezersResult.status === 'fulfilled' ? freezersResult.value : [];
    const apiError =
      itemResult.status === 'rejected'
        ? 'The API is unavailable. Start the API and refresh to view items.'
        : null;

    return (
      <main>
      <div className="header-bar">
        <div>
          <h1>Item details</h1>
          <p>Review the selected item and decide on next steps.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>
        {apiError && <div className="notice mt-16">{apiError}</div>}
        {flash && <div className="notice mt-16">{flash.message}</div>}

        <section className="card mt-24">
          {item ? (
            <>
              <h2>{item.reference} - {item.description}</h2>
              <div className="grid grid-2 mt-16">
                <div className="field">
                  <label>Freezer</label>
                  <p>{item.freezerName}</p>
                </div>
                <div className="field">
                  <label>Shelf</label>
                  <p>{item.shelfNumber}</p>
                </div>
                <div className="field">
                  <label>Weight</label>
                  <p>{item.weight || 'Not set'}</p>
                </div>
                <div className="field">
                  <label>Size</label>
                  <p>{item.size || 'Not set'}</p>
                </div>
                <div className="field">
                  <label>Freeze date</label>
                  <p>{item.freezeDate}</p>
                </div>
                <div className="field">
                  <label>Best before</label>
                  <p>{item.bestBefore}</p>
                </div>
              </div>
              <div className="actions mt-16">
                <Link href={`/items/${item.id}/edit`} className="secondary">Edit</Link>
                <form action={deleteItemAction}>
                  <input type="hidden" name="id" value={item.id} />
                  <button type="submit" className="secondary">Delete</button>
                </form>
              </div>
            </>
          ) : (
            <p>We could not load this item. Check the API and try again.</p>
          )}
        </section>

      </main>
    );
  }

  const [freezersResult, nextReferenceResult] = await Promise.allSettled([listFreezers(), nextReference()]);
  [freezersResult, nextReferenceResult].forEach(throwIfRedirect);
  const freezers =
    freezersResult.status === 'fulfilled' ? freezersResult.value : [];
  const nextReferenceValue =
    nextReferenceResult.status === 'fulfilled' ? nextReferenceResult.value.nextReference : '';
  const apiError = freezersResult.status === 'rejected' || nextReferenceResult.status === 'rejected'
    ? 'The API is unavailable. Start the API and refresh to add items.'
    : null;

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Add item</h1>
          <p>Create a new inventory entry with its freeze and best before dates.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>
      {apiError && <div className="notice mt-16">{apiError}</div>}
      {flash && <div className="notice mt-16">{flash.message}</div>}

      <section className="card mt-24">
        <AddItemForm
          freezers={freezers}
          isDisabled={!freezers.length}
          nextReference={nextReferenceValue}
          defaults={{
            reference: formValue('reference', nextReferenceValue),
            description: formValue('description', ''),
            freezerId: Number(formValue('freezerId', String(freezers[0]?.id ?? 0))),
            freezeDate: formValue('freezeDate', freezeDefault),
            bestBefore: formValue('bestBefore', bestBeforeDefault),
            shelfNumber: Number(formValue('shelfNumber', '1')),
            weight: formValue('weight', ''),
            size: formValue('size', '')
          }}
          action={createItemAction}
        />
      </section>
    </main>
  );
}
