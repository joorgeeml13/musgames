import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { HttpClient, ApiErrorResponse } from './httpClient'
import { STORAGE_KEY_LANGUAGE } from '@/i18n/I18nContext'
import { MemoryStorageService } from '../storage/localStorageService'

describe('HttpClient Clean Infrastructure', () => {
  let client: HttpClient
  let storage: MemoryStorageService
  const originalFetch = globalThis.fetch

  beforeEach(() => {
    storage = new MemoryStorageService()
    client = new HttpClient('http://api.mus.local', storage)
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
    vi.restoreAllMocks()
  })

  it('injects Accept-Language header dynamically as "es" by default', async () => {
    let capturedHeaders: Headers | undefined

    globalThis.fetch = vi.fn().mockImplementation(async (_url: string, init?: RequestInit) => {
      capturedHeaders = new Headers(init?.headers)
      return new Response(JSON.stringify({ success: true, data: { test: true }, message: 'OK', timestamp: '' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    await client.get('/api/test')
    expect(capturedHeaders?.get('Accept-Language')).toBe('es')
  })

  it('injects Accept-Language header dynamically as "en" when set in storage', async () => {
    storage.setItem(STORAGE_KEY_LANGUAGE, 'en')
    let capturedHeaders: Headers | undefined

    globalThis.fetch = vi.fn().mockImplementation(async (_url: string, init?: RequestInit) => {
      capturedHeaders = new Headers(init?.headers)
      return new Response(JSON.stringify({ success: true, data: { ok: true }, message: 'OK', timestamp: '' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    await client.get('/api/test')
    expect(capturedHeaders?.get('Accept-Language')).toBe('en')
  })

  it('normalizes a successful response without backend envelope into standard ApiResponse<T>', async () => {
    globalThis.fetch = vi.fn().mockImplementation(async () => {
      return new Response(JSON.stringify({ elo: 1645, username: 'patxi' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    const res = await client.get<{ elo: number; username: string }>('/api/player/patxi')
    expect(res.success).toBe(true)
    expect(res.data.elo).toBe(1645)
    expect(res.data.username).toBe('patxi')
    expect(res.message).toBe('OK')
    expect(res.timestamp).toBeDefined()
  })

  it('normalizes an already enveloped ApiResponse<T>', async () => {
    const enveloped = {
      success: true,
      data: [{ rank: 1, name: 'Don Ignacio' }],
      message: 'Operación exitosa',
      timestamp: '2026-10-21T18:45:00Z',
    }

    globalThis.fetch = vi.fn().mockImplementation(async () => {
      return new Response(JSON.stringify(enveloped), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    const res = await client.get('/api/leaderboard')
    expect(res.success).toBe(true)
    expect(res.data).toEqual(enveloped.data)
    expect(res.message).toBe('Operación exitosa')
  })

  it('throws structured ApiErrorResponse when server returns error status code with ApiError json payload', async () => {
    const errorPayload = {
      success: false,
      status: 404,
      error: 'Not Found',
      code: 'PLAYER_NOT_FOUND',
      message: "Jugador 'patxi' no encontrado",
      path: '/api/leaderboard/patxi',
      timestamp: '2026-10-21T18:45:00Z',
    }

    globalThis.fetch = vi.fn().mockImplementation(async () => {
      return new Response(JSON.stringify(errorPayload), {
        status: 404,
        statusText: 'Not Found',
        headers: { 'Content-Type': 'application/json' },
      })
    })

    await expect(client.get('/api/leaderboard/patxi')).rejects.toThrow(ApiErrorResponse)
    try {
      await client.get('/api/leaderboard/patxi')
    } catch (err) {
      const apiErr = err as ApiErrorResponse
      expect(apiErr.status).toBe(404)
      expect(apiErr.code).toBe('PLAYER_NOT_FOUND')
      expect(apiErr.message).toBe("Jugador 'patxi' no encontrado")
      expect(apiErr.path).toBe('/api/leaderboard/patxi')
    }
  })

  it('throws ApiErrorResponse with fallback details on network failure', async () => {
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('Failed to fetch'))

    await expect(client.get('/api/unreachable')).rejects.toThrow(ApiErrorResponse)
    try {
      await client.get('/api/unreachable')
    } catch (err) {
      const apiErr = err as ApiErrorResponse
      expect(apiErr.code).toBe('NETWORK_ERROR')
      expect(apiErr.status).toBe(0)
    }
  })

  it('appends query parameters correctly on GET requests', async () => {
    let requestedUrl = ''
    globalThis.fetch = vi.fn().mockImplementation(async (url: string) => {
      requestedUrl = url
      return new Response(JSON.stringify({ success: true, data: [] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    await client.get('/api/leaderboard', { filter: 'grandmaster', search: 'mikel' })
    expect(requestedUrl).toContain('filter=grandmaster')
    expect(requestedUrl).toContain('search=mikel')
  })

  it('rejects protocol-relative URLs to prevent SSRF and token leaking', async () => {
    await expect(client.get('//evil.com/leak')).rejects.toThrow('Protocol-relative URLs are prohibited for security')
  })

  it('preserves configured baseUrl on relative endpoint requests', async () => {
    let requestedUrl = ''
    globalThis.fetch = vi.fn().mockImplementation(async (url: string) => {
      requestedUrl = url
      return new Response(JSON.stringify({ success: true, data: [] }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    await client.get('/api/custom-endpoint')
    expect(requestedUrl).toBe('http://api.mus.local/api/custom-endpoint')
  })
})
