'use client';

import { useEffect } from 'react';

export default function AutoLogout() {
  useEffect(() => {
    const timer = window.setTimeout(() => {
      const form = document.getElementById('logout-after-password') as HTMLFormElement | null;
      form?.requestSubmit();
    }, 2000);
    return () => window.clearTimeout(timer);
  }, []);

  return null;
}
