import { useState, type FormEvent } from 'react'
import { api, describeProblem, type Portfolio } from '../api/client'

const PORTFOLIOS: Portfolio[] = [
  'EXEC', 'HR', 'SOCIALS', 'IT', 'MARKETING', 'PUBLICATIONS', 'EDUCATION', 'SPONSORSHIPS', 'COMPETITIONS',
]

export default function SubmitPage({ onSubmitted }: { onSubmitted: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    setBusy(true)
    setError(null)
    const { error } = await api.POST('/api/v1/requests', {
      body: {
        firstName: String(f.get('firstName')),
        lastName: String(f.get('lastName')),
        email: String(f.get('email')),
        bsb: String(f.get('bsb')),
        accountNumber: String(f.get('accountNumber')),
        portfolio: String(f.get('portfolio')) as Portfolio,
        eventName: String(f.get('eventName')),
        eventDate: String(f.get('eventDate')),
        amount: Number(f.get('amount')),
        declarationAccepted: f.get('declarationAccepted') === 'on',
        declarationSignature: String(f.get('declarationSignature')),
      },
    })
    setBusy(false)
    if (error) setError(describeProblem(error))
    else onSubmitted()
  }

  return (
    <section className="narrow">
      <h2>New reimbursement claim</h2>
      <p className="hint">
        Submit within 2 weeks of your event. A receipt is required — upload support is coming; until then,
        send it to the treasurer directly.
      </p>
      <form onSubmit={onSubmit}>
        <div className="row">
          <label>First name<input name="firstName" autoComplete="given-name" required /></label>
          <label>Last name<input name="lastName" autoComplete="family-name" required /></label>
        </div>
        <label>Email<input name="email" type="email" autoComplete="email" required /></label>
        <div className="row">
          <label>Bank BSB<input name="bsb" inputMode="numeric" placeholder="062-000" pattern="\d{3}-?\d{3}" required /></label>
          <label>Bank account number<input name="accountNumber" inputMode="numeric" pattern="\d{6,10}" required /></label>
        </div>
        <label>
          Portfolio
          <select name="portfolio" required defaultValue="">
            <option value="" disabled>Choose…</option>
            {PORTFOLIOS.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
        </label>
        <label>Name of event (full Facebook name if possible)<input name="eventName" maxLength={200} required /></label>
        <div className="row">
          <label>Date of event<input name="eventDate" type="date" required /></label>
          <label>
            Total amount (AUD, inc. GST)
            <input name="amount" type="number" min="0.01" step="0.01" inputMode="decimal" required />
          </label>
        </div>
        <fieldset>
          <legend>Declaration</legend>
          <ol>
            <li>The expenses were for UNSW business purposes only and in accordance with UNSW policy.</li>
            <li>All receipts and tax invoices are attached.</li>
            <li>This is the only claim I have made, or will make, for these expenses.</li>
            <li>Payment details provided are correct if I am a student claiming reimbursement of expenses.</li>
          </ol>
          <label className="check"><input name="declarationAccepted" type="checkbox" required /> I agree to the declaration</label>
          <label>Full name as signature<input name="declarationSignature" required /></label>
        </fieldset>
        {error && <p role="alert" className="error">{error}</p>}
        <button type="submit" disabled={busy}>Submit claim</button>
      </form>
    </section>
  )
}
