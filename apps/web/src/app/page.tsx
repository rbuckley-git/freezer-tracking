import Link from 'next/link';
import { getAccountHouse, listFreezers, listItems } from '../lib/api';
import { Item } from '../types/items';
import { consumeFlashMessage } from '../lib/flashStore';
import { resetFiltersAction } from './actions';
import AppMenu from '../components/AppMenu';
import { requireAuth } from '../lib/auth';

export const dynamic = 'force-dynamic';

type SearchParams = {
  q?: string;
  sort?: string;
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

function sortItems(items: Item[], sort: string, dir: string) {
  const direction = dir === 'desc' ? -1 : 1;
  const copy = [...items];
  copy.sort((a, b) => {
    const valueA =
      sort === 'daysLeft'
        ? daysUntil(a.bestBefore) ?? Number.POSITIVE_INFINITY
        : (a as Record<string, string | number>)[sort];
    const valueB =
      sort === 'daysLeft'
        ? daysUntil(b.bestBefore) ?? Number.POSITIVE_INFINITY
        : (b as Record<string, string | number>)[sort];
    if (valueA < valueB) return -1 * direction;
    if (valueA > valueB) return 1 * direction;
    return 0;
  });
  return copy;
}

function headerLink(label: string, field: string, sort: string, query: string) {
  const params = new URLSearchParams();
  if (query) {
    params.set('q', query);
  }
  params.set('sort', field);
  const isActive = sort === field;
  return { label, href: `/?${params.toString()}`, isActive };
}

function filterItems(items: Item[], query: string) {
  if (!query) return items;
  const normalised = query.toLowerCase();
  return items.filter(
    (item) =>
      item.reference.includes(normalised) ||
      item.description.toLowerCase().includes(normalised) ||
      item.freezerName.toLowerCase().includes(normalised)
  );
}

function daysUntil(dateString: string) {
  const today = new Date();
  const todayUtc = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
  const [year, month, day] = dateString.split('-').map(Number);
  if (!year || !month || !day) {
    return null;
  }
  const targetUtc = Date.UTC(year, month - 1, day);
  return Math.ceil((targetUtc - todayUtc) / (1000 * 60 * 60 * 24));
}

export default async function Page({
  searchParams
}: {
  searchParams?: Promise<SearchParams>;
}) {
  const auth = await requireAuth();
  const resolved = (await searchParams) ?? {};
  const query = resolved.q?.toString() ?? '';
  const sort = resolved.sort?.toString() ?? 'bestBefore';
  const dir = 'asc';

  const flash = await consumeFlashMessage();

  const [itemsResult, freezersResult, houseResult] = await Promise.allSettled([
    listItems(0, 100),
    listFreezers(),
    getAccountHouse()
  ]);
  [itemsResult, freezersResult, houseResult].forEach(throwIfRedirect);
  const itemsData =
    itemsResult.status === 'fulfilled'
      ? itemsResult.value
      : { items: [], page: 0, size: 0, totalItems: 0, totalPages: 0 };
  const freezers = freezersResult.status === 'fulfilled' ? freezersResult.value : [];
  const houseName = houseResult.status === 'fulfilled' ? houseResult.value.name : 'your house';
  const apiError =
    itemsResult.status === 'rejected' || freezersResult.status === 'rejected' || houseResult.status === 'rejected'
      ? 'The API is unavailable. Start the API and refresh to load data.'
      : null;

  const filtered = filterItems(itemsData.items, query);
  const sorted = sortItems(filtered, sort, dir);
  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Freezer items for {houseName}</h1>
          <p>Keep a tight view on what is frozen, where it is stored, and when it needs using.</p>
        </div>
        <div className="header-actions">
          <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
          <span className="tag">{sorted.length} items</span>
        </div>
      </div>
      {apiError && <div className="notice mt-16">{apiError}</div>}
      {flash && <div className="notice mt-16">{flash.message}</div>}

      <div className="grid grid-2 mt-32">
        <section className="card">
          <h2>New item</h2>
          <p>Create a new inventory entry with its freeze and best before dates.</p>
          <Link href="/item" className="secondary">Add item</Link>
        </section>

        <section className="card">
          <h2>Find items</h2>
          <div className="actions mb-16">
            {freezers.map((freezer) => (
              <Link
                key={freezer.id}
                href={`/?q=${encodeURIComponent(freezer.name)}&sort=${encodeURIComponent(sort)}`}
                className="secondary"
              >
                {freezer.name}
              </Link>
            ))}
          </div>
          <form method="get" className="grid gap-16">
            <div className="field">
              <label htmlFor="q">Search</label>
              <input id="q" name="q" defaultValue={query} placeholder="Reference, description, freezer" />
            </div>
            <div className="actions actions-between">
              <button type="submit" formAction={resetFiltersAction} className="secondary">Reset</button>
              <button type="submit" className="secondary">Apply</button>
            </div>
          </form>
        </section>
      </div>

      <section className="card mt-24">
        <h2>Items</h2>
        <table className="table">
          <thead>
            <tr>
              {[
                headerLink('Reference', 'reference', sort, query),
                headerLink('Description', 'description', sort, query),
                headerLink('Freezer', 'freezerName', sort, query),
                headerLink('Shelf', 'shelfNumber', sort, query),
                headerLink('Frozen', 'freezeDate', sort, query),
                headerLink('Best before', 'bestBefore', sort, query),
                headerLink('Days left', 'daysLeft', sort, query)
              ].map((col) => (
                <th key={col.label} className={['Shelf', 'Days left'].includes(col.label) ? 'numeric' : ''}>
                  {col.href ? (
                    <Link href={col.href} className={`secondary${col.isActive ? ' active' : ''}`}>
                      {col.label}
                    </Link>
                  ) : (
                    col.label
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {sorted.map((item) => (
              <tr key={item.id}>
                <td>
                  <Link href={`/item?id=${item.id}`} className="secondary">
                    {item.reference}
                  </Link>
                </td>
                <td>{item.description}</td>
                <td>{item.freezerName}</td>
                <td className="numeric">{item.shelfNumber}</td>
                <td>{item.freezeDate}</td>
                <td>{item.bestBefore}</td>
                <td className="numeric">{daysUntil(item.bestBefore) ?? '—'}</td>
              </tr>
            ))}
            {sorted.length === 0 && (
              <tr>
                <td colSpan={7}>No items match your search yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

    </main>
  );
}
