'use client';

import { useId, useMemo, useState } from 'react';

type Props = {
  id: number;
  name: string;
  shelfCount: number;
  onSave: (formData: FormData) => void;
  onDelete: (formData: FormData) => void;
};

export default function FreezerRow({ id, name, shelfCount, onSave, onDelete }: Props) {
  const formId = useId();
  const [currentName, setCurrentName] = useState(name);
  const [currentShelfCount, setCurrentShelfCount] = useState(String(shelfCount));

  const isDirty = useMemo(() => {
    return currentName.trim() !== name || Number(currentShelfCount) !== shelfCount;
  }, [currentName, currentShelfCount, name, shelfCount]);

  return (
    <tr>
      <td>
        <input
          form={formId}
          name="name"
          defaultValue={name}
          onChange={(event) => setCurrentName(event.target.value)}
        />
      </td>
      <td className="numeric">
        <input
          form={formId}
          name="shelfCount"
          type="number"
          min={1}
          max={10}
          defaultValue={shelfCount}
          onChange={(event) => setCurrentShelfCount(event.target.value)}
        />
      </td>
      <td>
        <div className="actions">
          <form id={formId} action={onSave}>
            <input type="hidden" name="id" value={id} />
            <button type="submit" className="secondary" disabled={!isDirty}>
              Save
            </button>
          </form>
          <form action={onDelete}>
            <input type="hidden" name="id" value={id} />
            <input type="hidden" name="name" value={name} />
            <button type="submit" className="secondary">
              Delete
            </button>
          </form>
        </div>
      </td>
    </tr>
  );
}
