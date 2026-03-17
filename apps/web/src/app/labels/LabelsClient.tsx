'use client';

import { useEffect, useState } from 'react';
import QRCode from 'qrcode';

type QrEntry = {
  reference: string;
  dataUrl: string;
};

const CODES_COUNT = 35;

function todayStamp() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}${month}${day}`;
}

export default function LabelsClient() {
  const [startReference, setStartReference] = useState('');
  const [entries, setEntries] = useState<QrEntry[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [testPrintOnly, setTestPrintOnly] = useState(false);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    fetch('/api/next-reference')
      .then((response) => response.json())
      .then((response) => {
        if (isMounted) {
          setStartReference(response.nextReference);
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });
    return () => {
      isMounted = false;
    };
  }, []);

  async function generateCodes(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = startReference.trim();
    if (!/^\d{8}$/.test(trimmed)) {
      return;
    }
    const start = Number(trimmed);
    const refs = Array.from({ length: CODES_COUNT }, (_, index) => String(start + index).padStart(8, '0'));
    const dateStamp = todayStamp();
    const dataUrls = await Promise.all(
      refs.map((reference) =>
        QRCode.toDataURL(`r:${reference},d:${dateStamp}`, { margin: 0, width: 320 })
      )
    );
    setEntries(refs.map((reference, index) => ({ reference, dataUrl: dataUrls[index] })));
  }

  return (
    <>
      <section className="card mt-24 no-print">
        <form onSubmit={generateCodes} className="grid gap-16">
          <div className="field">
            <label htmlFor="referenceStart">Starting reference</label>
            <input
              id="referenceStart"
              name="referenceStart"
              value={startReference}
              onChange={(event) => setStartReference(event.target.value)}
              placeholder="00000001"
              pattern="[0-9]{8}"
              required
              disabled={isLoading}
            />
          </div>
          <label className="radio-pill" htmlFor="testPrintOnly">
            <input
              id="testPrintOnly"
              name="testPrintOnly"
              type="checkbox"
              checked={testPrintOnly}
              onChange={(event) => setTestPrintOnly(event.target.checked)}
              disabled={isLoading}
            />
            Test print only (top and bottom rows)
          </label>
          <div className="actions">
            <button type="submit" className="secondary" disabled={isLoading}>Generate</button>
          </div>
        </form>
        <div className="mt-16">
          <p><strong>Print guidance:</strong> Use your browser print dialog, set paper size to A4, scale to 100%, margins to none, and disable headers/footers.</p>
          <p>Print preview should show a full sheet of 35 labels (5 x 7).</p>
        </div>
      </section>

      {entries.length > 0 && (
        <section className="label-sheet mt-24">
          <div className="label-grid">
            {entries.map((entry, index) => {
              const shouldPrintLabel = !testPrintOnly || index < 5 || index >= CODES_COUNT - 5;
              return (
                <div className="label-cell" key={entry.reference}>
                  {shouldPrintLabel && (
                    <div className="label-content">
                      <img
                        src={entry.dataUrl}
                        alt={`QR code for ${entry.reference}`}
                        className="label-qr"
                      />
                      <div className="label-ref">{entry.reference}</div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </section>
      )}
    </>
  );
}
