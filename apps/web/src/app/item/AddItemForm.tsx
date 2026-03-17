'use client';

import { useEffect, useMemo, useState } from 'react';
import type { Freezer } from '../../types/freezers';

type AddItemFormProps = {
  freezers: Freezer[];
  isDisabled: boolean;
  nextReference: string;
  defaults: {
    reference: string;
    description: string;
    freezerId: number;
    freezeDate: string;
    bestBefore: string;
    shelfNumber: number;
    weight: string;
    size: string;
  };
  action: (formData: FormData) => void;
};

export default function AddItemForm({ freezers, isDisabled, nextReference, defaults, action }: AddItemFormProps) {
  const initialFreezerId = defaults.freezerId || freezers[0]?.id || 0;
  const [freezerId, setFreezerId] = useState(initialFreezerId);
  const [shelfNumber, setShelfNumber] = useState(defaults.shelfNumber);

  const maxShelf = useMemo(() => {
    const match = freezers.find((freezer) => freezer.id === freezerId);
    return match?.shelfCount ?? 10;
  }, [freezerId, freezers]);

  useEffect(() => {
    if (shelfNumber > maxShelf) {
      setShelfNumber(maxShelf);
    }
  }, [maxShelf, shelfNumber]);

  return (
    <form action={action} className="grid gap-16">
      <div className="field">
        <label htmlFor="reference">Reference</label>
        <input
          id="reference"
          name="reference"
          required
          maxLength={8}
          pattern="[0-9]{8}"
          defaultValue={defaults.reference || nextReference}
          disabled={isDisabled}
        />
      </div>
      <div className="field">
        <label htmlFor="description">Description</label>
        <input
          id="description"
          name="description"
          required
          maxLength={120}
          defaultValue={defaults.description}
          disabled={isDisabled}
        />
      </div>
      <div className="field">
        <label htmlFor="weight">Weight (optional)</label>
        <input
          id="weight"
          name="weight"
          maxLength={8}
          defaultValue={defaults.weight}
          disabled={isDisabled}
        />
      </div>
      <div className="field">
        <label>Size (optional)</label>
        <div className="radio-group">
          <label className="radio-pill">
            <input type="radio" name="size" value="" defaultChecked={defaults.size === ''} disabled={isDisabled} />
            Not set
          </label>
          {['S', 'M', 'L'].map((size) => (
            <label key={size} className="radio-pill">
              <input type="radio" name="size" value={size} defaultChecked={defaults.size === size} disabled={isDisabled} />
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
                required
                checked={freezerId === freezer.id}
                onChange={() => setFreezerId(freezer.id)}
                disabled={isDisabled}
              />
              {freezer.name}
            </label>
          ))}
        </div>
      </div>
      <div className="grid grid-2">
        <div className="field">
          <label htmlFor="freezeDate">Freeze date</label>
          <input
            id="freezeDate"
            name="freezeDate"
            type="date"
            required
            defaultValue={defaults.freezeDate}
            disabled={isDisabled}
          />
        </div>
        <div className="field">
          <label htmlFor="bestBefore">Best before</label>
          <input
            id="bestBefore"
            name="bestBefore"
            type="date"
            required
            defaultValue={defaults.bestBefore}
            disabled={isDisabled}
          />
        </div>
      </div>
      <div className="field">
        <label htmlFor="shelfNumber">Shelf - counting from top</label>
        <input
          id="shelfNumber"
          name="shelfNumber"
          type="range"
          min={1}
          max={maxShelf}
          value={shelfNumber}
          onChange={(event) => setShelfNumber(Number(event.target.value))}
          required
          disabled={isDisabled}
          className="range"
        />
        <div className="shelf-ticks" aria-hidden="true">
          {Array.from({ length: maxShelf }, (_, index) => (
            <span key={index}>{index + 1}</span>
          ))}
        </div>
      </div>
      <button type="submit" disabled={isDisabled}>Add item</button>
    </form>
  );
}
