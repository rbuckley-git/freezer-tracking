import Link from 'next/link';
import { updateItemAction } from '../../../actions';
import { getItem, listFreezers } from '../../../../lib/api';
import { consumeFlashMessage } from '../../../../lib/flashStore';

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

export default async function EditItemPage({
  params
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const [itemResult, freezersResult] = await Promise.allSettled([
    getItem(id),
    listFreezers()
  ]);
  [itemResult, freezersResult].forEach(throwIfRedirect);
  const item = itemResult.status === 'fulfilled' ? itemResult.value : null;
  const freezers = freezersResult.status === 'fulfilled' ? freezersResult.value : [];
  const apiError =
    itemResult.status === 'rejected' || freezersResult.status === 'rejected'
      ? 'The API is unavailable. Start the API and refresh to edit items.'
      : null;

  const flash = await consumeFlashMessage();

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Amend item</h1>
          <p>Update freezer placement, dates, and descriptions with confidence.</p>
        </div>
        <Link href="/" className="secondary">
          Back to list
        </Link>
      </div>
      {apiError && <div className="notice mt-16">{apiError}</div>}
      {flash && <div className="notice mt-16">{flash.message}</div>}

      {item ? (
        <section className="card mt-24">
          <form action={updateItemAction.bind(null, item.id)} className="grid gap-16">
            <div className="field">
              <label htmlFor="reference">Reference</label>
              <input
                id="reference"
                name="reference"
                required
                maxLength={8}
                pattern="[0-9]{8}"
                defaultValue={item.reference}
              />
            </div>
            <div className="field">
              <label htmlFor="description">Description</label>
              <input id="description" name="description" required maxLength={120} defaultValue={item.description} />
            </div>
            <div className="field">
              <label htmlFor="weight">Weight (optional)</label>
              <input id="weight" name="weight" maxLength={8} defaultValue={item.weight ?? ''} />
            </div>
            <div className="field">
              <label>Size (optional)</label>
              <div className="radio-group">
                <label className="radio-pill">
                  <input type="radio" name="size" value="" defaultChecked={!item.size} />
                  Not set
                </label>
                {['S', 'M', 'L'].map((size) => (
                  <label key={size} className="radio-pill">
                    <input type="radio" name="size" value={size} defaultChecked={item.size === size} />
                    {size}
                  </label>
                ))}
              </div>
            </div>
            <div className="field">
              <label>Freezer</label>
              <div className="radio-group">
                {freezers.map((freezer) => (
                  <label key={freezer.id} className="radio-pill">
                    <input
                      type="radio"
                      name="freezerId"
                      value={freezer.id}
                      defaultChecked={freezer.id === item.freezerId}
                      required
                    />
                    {freezer.name}
                  </label>
                ))}
              </div>
            </div>
            <div className="grid grid-2">
              <div className="field">
                <label htmlFor="freezeDate">Freeze date</label>
                <input id="freezeDate" name="freezeDate" type="date" required defaultValue={item.freezeDate} />
              </div>
              <div className="field">
                <label htmlFor="bestBefore">Best before</label>
                <input id="bestBefore" name="bestBefore" type="date" required defaultValue={item.bestBefore} />
              </div>
            </div>
            <div className="field">
              <label htmlFor="shelfNumber">Shelf - counting from top</label>
              <input
                id="shelfNumber"
                name="shelfNumber"
                type="range"
                min={1}
                max={10}
                defaultValue={item.shelfNumber}
                required
                className="range"
              />
              <div className="shelf-ticks" aria-hidden="true">
                {Array.from({ length: 10 }, (_, index) => (
                  <span key={index}>{index + 1}</span>
                ))}
              </div>
            </div>
            <div className="actions">
              <button type="submit">Save changes</button>
              <Link href="/" className="secondary">Cancel</Link>
            </div>
          </form>
        </section>
      ) : (
        <section className="card mt-24">
          <p>We could not load this item. Check the API and try again.</p>
        </section>
      )}
    </main>
  );
}
