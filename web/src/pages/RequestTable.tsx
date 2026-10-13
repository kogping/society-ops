import type { ReactNode } from 'react'
import type { RequestResponse } from '../api/client'

const money = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' })
const date = new Intl.DateTimeFormat('en-AU', { dateStyle: 'medium' })

export default function RequestTable({
  requests,
  actions,
  showClaimant = false,
}: {
  requests: RequestResponse[]
  actions?: (r: RequestResponse) => ReactNode
  showClaimant?: boolean
}) {
  if (requests.length === 0) return <p className="hint">Nothing here yet.</p>
  return (
    <table>
      <thead>
        <tr>
          <th>Submitted</th>
          {showClaimant && <th>Claimant</th>}
          <th>Event</th>
          <th>Portfolio</th>
          <th className="num">Amount</th>
          {showClaimant && <th>Pay to</th>}
          <th>Status</th>
          {actions && <th><span className="sr-only">Actions</span></th>}
        </tr>
      </thead>
      <tbody>
        {requests.map((r) => (
          <tr key={r.id}>
            <td>{date.format(new Date(r.createdAt))}</td>
            {showClaimant && <td>{r.claimant.firstName} {r.claimant.lastName}</td>}
            <td>
              {r.eventName}
              <br />
              <small>{date.format(new Date(r.eventDate))}</small>
            </td>
            <td>{r.portfolio}</td>
            <td className="num">{money.format(r.amount)}</td>
            {showClaimant && <td><small>{r.bank.bsb} {r.bank.accountNumber}</small></td>}
            <td>
              <span className={`badge ${r.status.toLowerCase()}`}>{r.status}</span>
              {r.rejectionReason && <><br /><small>{r.rejectionReason}</small></>}
            </td>
            {actions && <td className="actions">{actions(r)}</td>}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
