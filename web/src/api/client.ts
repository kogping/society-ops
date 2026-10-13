import createClient, { type Middleware } from 'openapi-fetch'
import type { components, paths } from './schema'

export type RequestResponse = components['schemas']['RequestResponse']
export type SubmitReimbursementRequest = components['schemas']['SubmitReimbursementRequest']
export type Portfolio = RequestResponse['portfolio']
export type RequestStatus = RequestResponse['status']

/** RFC 9457 body returned for every error. `errors` is present on validation failures. */
export type Problem = {
  status?: number
  title?: string
  detail?: string
  errors?: { field: string; message: string }[]
}

// Temporary (P0): HTTP Basic credentials held in sessionStorage until P1 adds OIDC login.
const STORAGE_KEY = 'society-ops.credentials'

export function loadCredentials(): string | null {
  try {
    return sessionStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

export function saveCredentials(username: string, password: string): void {
  const token = btoa(`${username}:${password}`)
  try {
    sessionStorage.setItem(STORAGE_KEY, token)
  } catch {
    // Storage unavailable: the session just won't survive a reload.
  }
  currentToken = token
}

export function clearCredentials(): void {
  try {
    sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }
  currentToken = null
}

let currentToken: string | null = loadCredentials()

const auth: Middleware = {
  onRequest({ request }) {
    if (currentToken) request.headers.set('Authorization', `Basic ${currentToken}`)
    return request
  },
}

export const api = createClient<paths>({ baseUrl: '' })
api.use(auth)

export function describeProblem(error: unknown): string {
  const problem = (error ?? {}) as Problem
  if (problem.errors?.length) {
    return problem.errors.map((e) => `${e.field}: ${e.message}`).join('; ')
  }
  return problem.detail ?? problem.title ?? 'Something went wrong'
}
