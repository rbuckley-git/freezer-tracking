export const metadata = {
  title: 'Privacy Policy | Freezer Tracking',
  description: 'Privacy information for Freezer Tracking.'
};

export default function PrivacyPage() {
  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Privacy policy</h1>
          <p>Freezer Tracking stores only the information needed to provide the service.</p>
        </div>
      </div>

      <section className="card legal-copy">
        <p>
          Data entered into Freezer Tracking is retained until it is deleted by the user or by an administrator acting
          within the same house or organisation.
        </p>
        <p>
          Backups of application data are taken daily and retained for 3 days for resilience and recovery purposes.
        </p>
        <p>
          Passwords are not stored in clear text. Passwords are stored in hashed form using industry-standard password
          protection practices.
        </p>
        <p>
          Application data is encrypted at rest. Data transmitted between the app and the service is encrypted in
          transit over HTTPS.
        </p>
        <p>
          Access to data is limited to authenticated users with permission to the relevant house or organisation, and
          administrative access is restricted to authorised administrators.
        </p>
      </section>
    </main>
  );
}
