export const metadata = {
  title: 'Support | Freezer Tracking',
  description: 'Support information for Freezer Tracking.'
};

export default function SupportPage() {
  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Support</h1>
          <p>Support for Freezer Tracking is provided by the organisation that issued your account.</p>
        </div>
      </div>

      <section className="card legal-copy">
        <p>
          If you need help with access, account issues, freezer setup, or data corrections, contact the administrator
          or organisation that invited you to use Freezer Tracking.
        </p>
        <p>
          If your query relates to privacy or data retention, refer to the privacy policy page for the current
          retention and backup terms.
        </p>
      </section>
    </main>
  );
}
