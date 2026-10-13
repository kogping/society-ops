import { useState, type FormEvent } from 'react'
import { useAuth } from '../auth'

export default function LoginPage() {
  const { login } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    setBusy(true)
    setError(null)
    const ok = await login(String(form.get('username')), String(form.get('password')))
    if (!ok) setError('Wrong username or password')
    setBusy(false)
  }

  return (
    <main className="narrow">
      <h1>Society Ops</h1>
      <form onSubmit={onSubmit}>
        <label>
          Username
          <input name="username" autoComplete="username" required autoFocus />
        </label>
        <label>
          Password
          <input name="password" type="password" autoComplete="current-password" required />
        </label>
        {error && <p role="alert" className="error">{error}</p>}
        <button type="submit" disabled={busy}>Log in</button>
      </form>
    </main>
  )
}
