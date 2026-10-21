import type { IStorageService } from '../../ports/IStorageService'

export class LocalStorageService implements IStorageService {
  getItem(key: string): string | null {
    if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') return null
    try {
      return window.localStorage.getItem(key)
    } catch {
      return null
    }
  }

  setItem(key: string, value: string): void {
    if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') return
    try {
      window.localStorage.setItem(key, value)
    } catch {
      // Ignore write errors in restricted contexts
    }
  }

  removeItem(key: string): void {
    if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') return
    try {
      window.localStorage.removeItem(key)
    } catch {
      // Ignore remove errors
    }
  }
}

export class MemoryStorageService implements IStorageService {
  private store = new Map<string, string>()

  getItem(key: string): string | null {
    return this.store.get(key) ?? null
  }

  setItem(key: string, value: string): void {
    this.store.set(key, value)
  }

  removeItem(key: string): void {
    this.store.delete(key)
  }

  clear(): void {
    this.store.clear()
  }
}

export const localStorageService = new LocalStorageService()
