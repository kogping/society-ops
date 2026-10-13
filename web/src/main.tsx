import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { loadCredentials } from './api/client.ts'
import { AuthProvider, fetchUser } from './auth.tsx'

const initialUser = loadCredentials() ? await fetchUser() : null

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider initialUser={initialUser}>
      <App />
    </AuthProvider>
  </StrictMode>,
)
