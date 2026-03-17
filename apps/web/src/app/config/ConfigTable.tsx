'use client';

import { useMemo, useState } from 'react';

type ConfigEntry = {
  key: string;
  value: string;
};

type ConfigTableProps = {
  entries: ConfigEntry[];
  onUpdate: (formData: FormData) => void;
  onDelete: (formData: FormData) => void;
};

function ConfigRow({
  entry,
  onUpdate,
  onDelete
}: {
  entry: ConfigEntry;
  onUpdate: ConfigTableProps['onUpdate'];
  onDelete: ConfigTableProps['onDelete'];
}) {
  const [value, setValue] = useState(entry.value);
  const isDirty = useMemo(() => value !== entry.value, [value, entry.value]);

  return (
    <tr>
      <td>{entry.key}</td>
      <td>
        <input
          name="value"
          value={value}
          onChange={(event) => setValue(event.target.value)}
        />
      </td>
      <td>
        <div className="actions">
          <form action={onUpdate}>
            <input type="hidden" name="key" value={entry.key} />
            <input type="hidden" name="value" value={value} />
            <button type="submit" className="secondary" disabled={!isDirty}>Save</button>
          </form>
          <form action={onDelete}>
            <input type="hidden" name="key" value={entry.key} />
            <button type="submit" className="secondary">Delete</button>
          </form>
        </div>
      </td>
    </tr>
  );
}

export default function ConfigTable({ entries, onUpdate, onDelete }: ConfigTableProps) {
  return (
    <table className="table mt-16">
      <thead>
        <tr>
          <th>Key</th>
          <th>Value</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {entries.map((entry) => (
          <ConfigRow key={entry.key} entry={entry} onUpdate={onUpdate} onDelete={onDelete} />
        ))}
      </tbody>
    </table>
  );
}
