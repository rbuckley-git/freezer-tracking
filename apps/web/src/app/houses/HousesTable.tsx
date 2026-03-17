'use client';

import { useMemo, useState } from 'react';

type House = {
  id: string;
  name: string;
};

type HousesTableProps = {
  houses: House[];
  onUpdate: (formData: FormData) => void;
  onDelete: (formData: FormData) => void;
};

function HouseRow({ house, onUpdate, onDelete }: { house: House; onUpdate: HousesTableProps['onUpdate']; onDelete: HousesTableProps['onDelete'] }) {
  const [name, setName] = useState(house.name);
  const isDirty = useMemo(() => name !== house.name, [name, house.name]);

  return (
    <tr>
      <td>
        <input name="name" value={name} onChange={(event) => setName(event.target.value)} required />
      </td>
      <td>
        <div className="actions">
          <form action={onUpdate}>
            <input type="hidden" name="id" value={house.id} />
            <input type="hidden" name="name" value={name} />
            <button type="submit" className="secondary" disabled={!isDirty}>Save</button>
          </form>
          <form action={onDelete}>
            <input type="hidden" name="id" value={house.id} />
            <button type="submit" className="secondary">Delete</button>
          </form>
        </div>
      </td>
    </tr>
  );
}

export default function HousesTable({ houses, onUpdate, onDelete }: HousesTableProps) {
  return (
    <table className="table mt-16">
      <thead>
        <tr>
          <th>Name</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {houses.map((house) => (
          <HouseRow key={house.id} house={house} onUpdate={onUpdate} onDelete={onDelete} />
        ))}
      </tbody>
    </table>
  );
}
