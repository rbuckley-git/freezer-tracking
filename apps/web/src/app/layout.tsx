import './globals.css';
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
          <footer className="app-version" aria-label="Application version">
            v{packageJson.version}
          </footer>
        </div>
      </body>
    </html>
  );
}
