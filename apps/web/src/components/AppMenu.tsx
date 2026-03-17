'use client';

import Link from 'next/link';
import { useState } from 'react';
import { logoutAction } from '../app/actions';

type AppMenuProps = {
  isAdmin?: boolean;
  isSuperAdmin?: boolean;
  userEmail?: string;
};

export default function AppMenu({ isAdmin, isSuperAdmin, userEmail }: AppMenuProps) {
  const [open, setOpen] = useState(false);

  return (
    <div className="app-menu-shell">
      {userEmail && <span className="app-menu-email">{userEmail}</span>}
      <details className="app-menu" open={open} onToggle={(event) => setOpen((event.target as HTMLDetailsElement).open)}>
        <summary className="hamburger-button" aria-label="Open menu">
          <span className="hamburger-lines" aria-hidden="true">
            <span></span>
            <span></span>
            <span></span>
          </span>
        </summary>
        <div className="menu-panel">
          <Link href="/" className="menu-item" onClick={() => setOpen(false)}>Search items</Link>
          <Link href="/freezers" className="menu-item" onClick={() => setOpen(false)}>Freezers</Link>
          <Link href="/layout" className="menu-item" onClick={() => setOpen(false)}>Layout</Link>
          <Link href="/labels" className="menu-item" onClick={() => setOpen(false)}>Labels</Link>
          {isSuperAdmin && (
            <>
              <Link href="/houses" className="menu-item" onClick={() => setOpen(false)}>Houses</Link>
              <Link href="/statistics" className="menu-item" onClick={() => setOpen(false)}>Statistics</Link>
              <Link href="/config" className="menu-item" onClick={() => setOpen(false)}>Configuration</Link>
            </>
          )}
          {isAdmin && (
            <>
              <Link href="/accounts" className="menu-item" onClick={() => setOpen(false)}>Accounts</Link>
              <Link href="/sessions" className="menu-item" onClick={() => setOpen(false)}>API sessions</Link>
            </>
          )}
          <Link href="/password" className="menu-item" onClick={() => setOpen(false)}>Change password</Link>
          <form action={logoutAction} onSubmit={() => setOpen(false)}>
            <button type="submit" className="menu-item menu-logout">Logout</button>
          </form>
        </div>
      </details>
    </div>
  );
}
