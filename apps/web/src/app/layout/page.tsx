import Link from 'next/link';
import { listFreezers, listItems } from '../../lib/api';
import { consumeFlashMessage } from '../../lib/flashStore';
import { requireAuth } from '../../lib/auth';
import AppMenu from '../../components/AppMenu';

export const dynamic = 'force-dynamic';

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

export default async function FreezerLayoutPage() {
  const auth = await requireAuth();
  const flash = await consumeFlashMessage();
  const [itemsResult, freezersResult] = await Promise.allSettled([listItems(0, 100), listFreezers()]);
  [itemsResult, freezersResult].forEach(throwIfRedirect);
  const items = itemsResult.status === 'fulfilled' ? itemsResult.value.items : [];
  const freezers = freezersResult.status === 'fulfilled' ? freezersResult.value : [];
  const apiError =
    itemsResult.status === 'rejected' || freezersResult.status === 'rejected'
      ? 'The API is unavailable. Start the API and refresh to load data.'
      : null;

  const itemsByFreezer = freezers.reduce<Record<string, Record<number, string[]>>>((acc, freezer) => {
    const shelves = Array.from({ length: freezer.shelfCount }, (_, index) => index + 1);
    acc[freezer.name] = shelves.reduce<Record<number, string[]>>((shelfAcc, shelf) => {
      shelfAcc[shelf] = [];
      return shelfAcc;
    }, {});
    return acc;
  }, {});

  const chipClasses = ['chip-colour-0', 'chip-colour-1', 'chip-colour-2', 'chip-colour-3', 'chip-colour-4', 'chip-colour-5'];
  const freezerClasses = freezers.reduce<Record<string, string>>((acc, freezer, index) => {
    acc[freezer.name] = chipClasses[index % chipClasses.length];
    return acc;
  }, {});

  for (const item of items) {
    if (!itemsByFreezer[item.freezerName]) {
      itemsByFreezer[item.freezerName] = {};
    }
    if (!itemsByFreezer[item.freezerName][item.shelfNumber]) {
      itemsByFreezer[item.freezerName][item.shelfNumber] = [];
    }
    itemsByFreezer[item.freezerName][item.shelfNumber].push(item.description);
  }

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Freezer layout</h1>
          <p>Visualise each freezer by shelf and the items stored on them.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>
      {apiError && <div className="notice mt-16">{apiError}</div>}
      {flash && <div className="notice mt-16">{flash.message}</div>}

      <section className="freezer-grid mt-24">
        {freezers.map((freezer) => (
          <div key={freezer.id} className="freezer-column">
            <h2>{freezer.name}</h2>
            <div className="shelf-stack">
              {Array.from({ length: freezer.shelfCount }, (_, index) => index + 1).map((shelf) => (
                <div key={shelf} className="shelf-row">
                  <div className="shelf-label">Shelf {shelf}</div>
                  <div className="shelf-items">
                    {(itemsByFreezer[freezer.name]?.[shelf] ?? []).map((itemName, index) => {
                      const item = items.find(
                        (candidate) =>
                          candidate.freezerName === freezer.name &&
                          candidate.shelfNumber === shelf &&
                          candidate.description === itemName
                      );
                      return (
                        <Link
                          key={`${itemName}-${index}`}
                          href={item ? `/item?id=${item.id}` : '/item'}
                          className={`shelf-chip ${freezerClasses[freezer.name] ?? ''}`}
                        >
                          {itemName}
                        </Link>
                      );
                    })}
                    {(itemsByFreezer[freezer.name]?.[shelf] ?? []).length === 0 && (
                      <span className="shelf-empty">Empty</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
        {freezers.length == 0 && !apiError && (
          <div className="card">No freezers found yet.</div>
        )}
      </section>
    </main>
  );
}
