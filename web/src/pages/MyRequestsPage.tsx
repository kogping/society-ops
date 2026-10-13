import { useEffect, useState } from 'react'
import { api, describeProblem, type RequestResponse } from '../api/client'
import RequestTable from './RequestTable'

export default function MyRequestsPage() {
  const [requests, setRequests] = useState<RequestResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    api.GET('/api/v1/requests', { params: { query: { mine: true, size: 100 } } }).then(({ data, error }) => {
      if (cancelled) return
      if (data) setRequests(data.content)
      else setError(describeProblem(error))
    })
    return () => { cancelled = true }
  }, [])

  return (
    <section>
      <h2>My requests</h2>
      {error && <p role="alert" className="error">{error}</p>}
      {!error && !requests && <p>Loading…</p>}
      {requests && <RequestTable requests={requests} />}
    </section>
  )
}
