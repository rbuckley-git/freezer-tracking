import AppMenu from '../../components/AppMenu';
import { requireSuperAdmin } from '../../lib/auth';
import { getStatistics } from '../../lib/api';
import type { HouseStatistics } from '../../types/statistics';

export const dynamic = 'force-dynamic';

type HouseTableRow = {
  freezerName: string;
  shelfNumber: number | null;
  itemCount: number;
};

function buildRowsForHouse(house: HouseStatistics): HouseTableRow[] {
  if (house.freezers.length === 0) {
    return [
      {
        freezerName: '—',
        shelfNumber: null,
        itemCount: 0
      }
    ];
  }

  const rows: HouseTableRow[] = [];
  for (const freezer of house.freezers) {
    for (const shelf of freezer.shelves) {
      rows.push({
        freezerName: freezer.freezerName,
        shelfNumber: shelf.shelfNumber,
        itemCount: shelf.itemCount
      });
    }
  }

  return rows;
}

function freezerLabel(count: number): string {
  return count === 1 ? '1 freezer' : `${count} freezers`;
}

export default async function StatisticsPage() {
  const auth = await requireSuperAdmin();
  const statistics = await getStatistics();

  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Statistics</h1>
          <p>Overview of houses, freezers, shelves, and items.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>

      <section className="card mt-24">
        <h2>Summary</h2>
        <p>{statistics.houseCount} houses in total.</p>
      </section>

      {statistics.houses.map((house) => {
        const rows = buildRowsForHouse(house);
        return (
          <section key={house.houseId} className="card mt-24">
            <h2>{house.houseName}</h2>
            <p>{freezerLabel(house.freezerCount)}</p>
            <table className="table">
              <thead>
                <tr>
                  <th>Freezer</th>
                  <th className="numeric">Shelf</th>
                  <th className="numeric">Items in shelf</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => {
                  const previous = index > 0 ? rows[index - 1] : null;
                  const sameFreezerAsPrevious = previous?.freezerName === row.freezerName;
                  return (
                    <tr key={`${house.houseId}-${row.freezerName}-${row.shelfNumber ?? 'none'}-${index}`}>
                      <td>{sameFreezerAsPrevious ? '' : row.freezerName}</td>
                      <td className="numeric">{row.shelfNumber ?? '—'}</td>
                      <td className="numeric">{row.itemCount}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </section>
        );
      })}

      {statistics.houses.length === 0 && (
        <section className="card mt-24">
          <h2>House and shelf breakdown</h2>
          <p>No houses found.</p>
        </section>
      )}
    </main>
  );
}
