'use client';

export default function ErrorPage({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Something went wrong</h1>
          <p>The API may be unavailable. Start it and retry.</p>
        </div>
      </div>
      <section className="card mt-24">
        <p>{error.message}</p>
        <div className="actions mt-16">
          <button onClick={() => reset()}>Try again</button>
        </div>
      </section>
    </main>
  );
}
