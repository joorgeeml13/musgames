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

  it('provides comprehensive Spanish and English dictionaries with symmetric keys', () => {
    expect(es.common.appName).toBe('Mus Tactics')
    expect(en.common.appName).toBe('Mus Tactics')

    expect(es.nav.home).toBe('Inicio')
    expect(en.nav.home).toBe('Home')

    expect(es.phases.grandeTitle).toBe('Grande')
    expect(en.phases.grandeTitle).toBe('Grande (Highs)')

    expect(es.phases.ordago).toBe('¡Órdago!')
    expect(en.phases.ordago).toBe('Órdago!')

    expect(es.lobby.title).toBe('Lobby & Salas Multijugador')
    expect(en.lobby.title).toBe('Lobby & Multiplayer Rooms')

    expect(es.leaderboard.title).toBe('Clasificación General ELO')
    expect(en.leaderboard.title).toBe('General ELO Leaderboard')
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
