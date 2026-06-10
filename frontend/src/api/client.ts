import axios, { AxiosError } from 'axios'
import type { ApiError } from './types'

/*
 * Shared axios instance. The SPA talks only to the backend under the `/api` base
 * path using same-origin relative URLs; in dev, Vite proxies `/api` to the Spring
 * Boot backend (see vite.config.ts), so no absolute host is configured here.
 */
export const http = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

/** True when `err` is an axios error carrying the backend's uniform error body. */
export function isApiError(err: unknown): err is AxiosError<ApiError> {
  return axios.isAxiosError(err)
}

/**
 * Best-effort human-readable message for a failed request: the backend's uniform
 * `message`, else the HTTP/network error text.
 */
export function getErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    return err.response?.data?.message ?? err.message
  }
  if (err instanceof Error) {
    return err.message
  }
  return 'An unexpected error occurred'
}
