import './globals.css';
import Link from 'next/link';
import { Space_Grotesk } from 'next/font/google';
import packageJson from '../../package.json';

const body = Space_Grotesk({
  subsets: ['latin'],
  variable: '--font-body'
});

export const metadata = {
  title: 'Freezer Tracking',
  description: 'Track freezer items and expiry dates.',
  icons: {
    icon: '/favicon.png'
  }
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={body.variable}>
      <body>
        <div className="app-shell">
          <div className="app-content">{children}</div>
          <footer className="app-footer">
            <nav className="footer-links" aria-label="Footer">
              <Link href="/privacy">Privacy</Link>
              <Link href="/support">Support</Link>
            </nav>
            <div className="app-version" aria-label="Application version">
              v{packageJson.version}
            </div>
          </footer>
        </div>
      </body>
    </html>
  );
}
