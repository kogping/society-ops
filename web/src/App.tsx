import { useState } from 'react'
import { useAuth } from './auth'
import LoginPage from './pages/LoginPage'
import MyRequestsPage from './pages/MyRequestsPage'
import SubmitPage from './pages/SubmitPage'
import TreasurerQueuePage from './pages/TreasurerQueuePage'

type Tab = 'submit' | 'mine' | 'queue'

export default function App() {
  const { user, logout } = useAuth()
  const [tab, setTab] = useState<Tab>('mine')
  const [submissions, setSubmissions] = useState(0)

  if (!user) return <LoginPage />

  return (
    <>
      <header className="topbar">
        <h1>Society Ops</h1>
        <nav aria-label="Main">
          <button aria-current={tab === 'submit'} onClick={() => setTab('submit')}>New claim</button>
          <button aria-current={tab === 'mine'} onClick={() => setTab('mine')}>My requests</button>
          {user.isTreasurer && (
            <button aria-current={tab === 'queue'} onClick={() => setTab('queue')}>Treasurer queue</button>
          )}
        </nav>
        <span className="who">
          {user.username} <button className="link" onClick={logout}>Log out</button>
        </span>
      </header>
      <main>
        {tab === 'submit' && (
          <SubmitPage
            onSubmitted={() => {
              setSubmissions((n) => n + 1)
              setTab('mine')
            }}
          />
        )}
        {tab === 'mine' && <MyRequestsPage key={submissions} />}
        {tab === 'queue' && user.isTreasurer && <TreasurerQueuePage />}
      </main>
    </>
  )
}
