import { createContext, useContext, useState, type ReactNode } from 'react'
import { api, clearCredentials, saveCredentials } from './api/client'

export type User = { username: string; isTreasurer: boolean }

type AuthState = {
  user: User | null
  login: (username: string, password: string) => Promise<boolean>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

async function fetchUser(): Promise<User | null> {
  const { data } = await api.GET('/api/v1/me')
  return data ? { username: data.username, isTreasurer: data.roles.includes('TREASURER') } : null
}

export function AuthProvider({ children, initialUser }: { children: ReactNode; initialUser: User | null }) {
  const [user, setUser] = useState<User | null>(initialUser)

  const login = async (username: string, password: string) => {
    saveCredentials(username, password)
    const found = await fetchUser()
    if (found) {
      setUser(found)
      return true
    }
    clearCredentials()
    return false
  }

  const logout = () => {
    clearCredentials()
    setUser(null)
  }

  return <AuthContext value={{ user, login, logout }}>{children}</AuthContext>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

// eslint-disable-next-line react-refresh/only-export-components
export { fetchUser }
