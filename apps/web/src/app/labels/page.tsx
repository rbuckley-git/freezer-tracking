import { requireAuth } from '../../lib/auth';
import AppMenu from '../../components/AppMenu';
import LabelsClient from './LabelsClient';

export default async function LabelsPage() {
  const auth = await requireAuth();
  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>QR labels</h1>
          <p>Generate printable labels for freezer references.</p>
        </div>
        <AppMenu isAdmin={auth.isAdmin} isSuperAdmin={auth.isSuperAdmin} userEmail={auth.username} />
      </div>
      <LabelsClient />
    </main>
  );
}
