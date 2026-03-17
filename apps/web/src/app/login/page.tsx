import { loginAction } from '../actions';
import { consumeFlashMessage } from '../../lib/flashStore';

export default async function LoginPage() {
  const flash = await consumeFlashMessage();
  return (
    <main>
      <div className="header-bar">
        <div>
          <h1>Sign in</h1>
          <p>Use your account credentials to access freezer data.</p>
        </div>
      </div>
      {flash && <div className="notice mt-16">{flash.message}</div>}

      <section className="card mt-24 max-w-520">
        <form action={loginAction} className="grid gap-16">
          <div className="field">
            <label htmlFor="username">Email</label>
            <input id="username" name="username" type="email" required autoComplete="username" />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" name="password" type="password" required autoComplete="current-password" />
          </div>
          <button type="submit">Sign in</button>
        </form>
      </section>
    </main>
  );
}
