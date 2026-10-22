import { describe, it, expect, beforeEach } from 'vitest'
import { es } from './translations/es'
import { en } from './translations/en'
import { getCurrentStoredLanguage, STORAGE_KEY_LANGUAGE } from './I18nContext'
import { MemoryStorageService } from '@/core/infrastructure/storage/localStorageService'

describe('i18n Multi-Language System', () => {
  let storage: MemoryStorageService

  beforeEach(() => {
    storage = new MemoryStorageService()
  })

  it('provides base Spanish and English dictionaries with symmetric keys', () => {
    expect(es.app.title).toBe('Mus Tactics')
    expect(en.app.title).toBe('Mus Tactics')

    expect(es.common.welcome).toBe('Bienvenido a Mus Tactics')
    expect(en.common.welcome).toBe('Welcome to Mus Tactics')

    expect(es.common.language).toBe('Idioma')
    expect(en.common.language).toBe('Language')
  })

  it('determines current stored language as "es" when unset', () => {
    expect(getCurrentStoredLanguage(storage)).toBe('es')
  })

  it('determines current stored language as "en" when set in storage', () => {
    storage.setItem(STORAGE_KEY_LANGUAGE, 'en')
    expect(getCurrentStoredLanguage(storage)).toBe('en')
  })

  it('ignores invalid language values in storage and falls back to "es"', () => {
    storage.setItem(STORAGE_KEY_LANGUAGE, 'fr')
    expect(getCurrentStoredLanguage(storage)).toBe('es')
  })
})
