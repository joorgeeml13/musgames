import { STORAGE_KEY_LANGUAGE, DEFAULT_LANGUAGE } from '@/i18n/I18nContext'
import type { IStorageService } from '../../ports/IStorageService'
import { localStorageService } from '../storage/localStorageService'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string
  timestamp: string
}

export interface ApiError {
  success: false
  status: number
  error: string
  code: string
  message: string
  path: string
  timestamp: string
  details?: unknown[]
}

export class ApiErrorResponse extends Error {
  public readonly status: number
  public readonly code: string
  public readonly path: string
  public readonly timestamp: string
  public readonly details?: unknown[]
  public readonly apiError: ApiError

  constructor(error: ApiError) {
    super(error.message || error.error || 'API Request Failed')
    this.name = 'ApiErrorResponse'
    this.status = error.status
    this.code = error.code
    this.path = error.path
    this.timestamp = error.timestamp
    this.details = error.details
    this.apiError = error
  }
}

export interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | undefined>
}

export class HttpClient {
  private baseUrl: string
  private storage: IStorageService

  constructor(baseUrl: string = '', storage: IStorageService = localStorageService) {
    this.baseUrl = baseUrl
    this.storage = storage
  }

  /**
   * Retrieves the current selected language from storage to inject into headers.
   */
  public getCurrentLanguage(): string {
    const stored = this.storage.getItem(STORAGE_KEY_LANGUAGE)
    if (stored === 'es' || stored === 'en') {
      return stored
    }
    return DEFAULT_LANGUAGE
  }

  /**
   * Builds request headers with dynamic Accept-Language injection and safe Content-Type handling.
   */
  private buildHeaders(customHeaders?: HeadersInit, body?: BodyInit | null): Headers {
    const headers = new Headers(customHeaders)
    // Avoid setting JSON Content-Type if already present or if body is FormData
    if (!headers.has('Content-Type') && (typeof FormData === 'undefined' || !(body instanceof FormData))) {
      headers.set('Content-Type', 'application/json')
    }
    // Dynamic Accept-Language header strictly validated from storage
    const currentLang = this.getCurrentLanguage()
    headers.set('Accept-Language', currentLang)
    return headers
  }

  /**
   * Normalizes URLs with optional query parameters and guards against protocol-relative injections.
   */
  private buildUrl(path: string, params?: Record<string, string | number | boolean | undefined>): string {
    // Security: Prevent protocol-relative URLs (e.g. //attacker.com/api) to avoid token/credential leakage
    if (path.startsWith('//')) {
      throw new Error('Protocol-relative URLs are prohibited for security')
    }
    const isAbsolute = path.startsWith('http://') || path.startsWith('https://')
    const fullTarget = isAbsolute ? path : `${this.baseUrl}${path}`
    const url = new URL(fullTarget, 'http://localhost')
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          url.searchParams.append(key, String(value))
        }
      })
    }
    // Return relative path + query only if baseUrl is empty and target was relative
    if (!this.baseUrl && !isAbsolute) {
      return url.pathname + url.search
    }
    return url.toString()
  }

  /**
   * Executes an HTTP request and standardizes success and error handling.
   */
  public async request<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
    const { params, headers: customHeaders, body: requestBody, ...restOptions } = options
    const url = this.buildUrl(path, params)
    const headers = this.buildHeaders(customHeaders, requestBody)

    let response: Response
    try {
      response = await fetch(url, {
        ...restOptions,
        body: requestBody,
        headers,
      })
    } catch (networkError) {
      const fallbackError: ApiError = {
        success: false,
        status: 0,
        error: 'Network Error',
        code: 'NETWORK_ERROR',
        message: networkError instanceof Error ? networkError.message : 'Failed to connect to server',
        path,
        timestamp: new Date().toISOString(),
      }
      throw new ApiErrorResponse(fallbackError)
    }

    const contentType = response.headers.get('content-type')
    let responseData: unknown = null
    if (contentType && contentType.includes('application/json')) {
      try {
        responseData = await response.json()
      } catch {
        responseData = null
      }
    } else {
      responseData = await response.text()
    }

    if (!response.ok) {
      let apiError: ApiError
      if (responseData && typeof responseData === 'object' && 'code' in (responseData as Record<string, unknown>)) {
        apiError = responseData as ApiError
      } else {
        apiError = {
          success: false,
          status: response.status,
          error: response.statusText || 'Http Error',
          code: `HTTP_${response.status}`,
          message: typeof responseData === 'string' && responseData.length ? responseData : `Request failed with status ${response.status}`,
          path,
          timestamp: new Date().toISOString(),
        }
      }
      throw new ApiErrorResponse(apiError)
    }

    // Check if the backend already wrapped the payload in ApiResponse<T>
    if (responseData && typeof responseData === 'object' && 'success' in (responseData as Record<string, unknown>) && 'data' in (responseData as Record<string, unknown>)) {
      return responseData as ApiResponse<T>
    }

    // Normalize standard response
    return {
      success: true,
      data: responseData as T,
      message: 'OK',
      timestamp: new Date().toISOString(),
    }
  }

  public async get<T>(path: string, params?: Record<string, string | number | boolean | undefined>, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, { ...options, method: 'GET', params })
  }

  public async post<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, {
      ...options,
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  }

  public async put<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, {
      ...options,
      method: 'PUT',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  }

  public async delete<T>(path: string, options?: RequestOptions): Promise<ApiResponse<T>> {
    return this.request<T>(path, { ...options, method: 'DELETE' })
  }
}

export const httpClient = new HttpClient()
