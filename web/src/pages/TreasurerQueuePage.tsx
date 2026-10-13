import { useCallback, useEffect, useState } from 'react'
import { api, describeProblem, type RequestResponse, type RequestStatus } from '../api/client'
import RequestTable from './RequestTable'

const FILTERS: RequestStatus[] = ['SUBMITTED', 'APPROVED', 'PAID', 'REJECTED']

export default function TreasurerQueuePage() {
  const [status, setStatus] = useState<RequestStatus>('SUBMITTED')
  const [requests, setRequests] = useState<RequestResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reloads, setReloads] = useState(0)

  useEffect(() => {
    let cancelled = false
    api.GET('/api/v1/requests', { params: { query: { status, order: 'oldest', size: 100 } } })
      .then(({ data, error }) => {
        if (cancelled) return
        if (data) {
          setRequests(data.content)
          setError(null)
        } else setError(describeProblem(error))
      })
    return () => { cancelled = true }
  }, [status, reloads])

  const reload = useCallback(() => setReloads((n) => n + 1), [])

  async function approve(id: string) {
    const { error } = await api.POST('/api/v1/requests/{id}/approve', { params: { path: { id } } })
    if (error) setError(describeProblem(error))
    reload()
  }

  async function pay(id: string) {
    const { error } = await api.POST('/api/v1/requests/{id}/pay', { params: { path: { id } } })
    if (error) setError(describeProblem(error))
    reload()
  }

  async function reject(id: string) {
    const reason = window.prompt('Reason for rejecting this claim?')?.trim()
    if (!reason) return
    const { error } = await api.POST('/api/v1/requests/{id}/reject', {
      params: { path: { id } },
      body: { reason },
    })
    if (error) setError(describeProblem(error))
    reload()
  }

  return (
    <section>
      <h2>Treasurer queue</h2>
      <div className="filters" role="group" aria-label="Filter by status">
        {FILTERS.map((s) => (
          <button key={s} aria-pressed={s === status} onClick={() => { setStatus(s); setRequests(null) }}>
            {s}
          </button>
        ))}
      </div>
      {error && <p role="alert" className="error">{error}</p>}
      {!requests && !error && <p>Loading…</p>}
      {requests && (
        <RequestTable
          requests={requests}
          showClaimant
          actions={(r) => (
            <>
              {r.status === 'SUBMITTED' && (
                <>
                  <button onClick={() => approve(r.id)}>Approve</button>
                  <button className="danger" onClick={() => reject(r.id)}>Reject</button>
                </>
              )}
              {r.status === 'APPROVED' && <button onClick={() => pay(r.id)}>Mark paid</button>}
            </>
          )}
        />
      )}
    </section>
  )
}
