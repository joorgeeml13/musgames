import React, { createContext, useState, useEffect, useCallback, useMemo } from 'react'
import type { Language, TranslationDictionary } from './types'
import { es } from './translations/es'
import { en } from './translations/en'
import type { IStorageService } from '@/core/ports/IStorageService'
import { localStorageService } from '@/core/infrastructure/storage/localStorageService'

export const STORAGE_KEY_LANGUAGE = 'mus_language'
export const DEFAULT_LANGUAGE: Language = 'es'

const dictionaries: Record<Language, TranslationDictionary> = {
  es,
  en,
}

export function getCurrentStoredLanguage(storage: IStorageService = localStorageService): Language {
  try {
    const saved = storage.getItem(STORAGE_KEY_LANGUAGE)
    if (saved === 'es' || saved === 'en') {
      return saved
    }
  } catch {
    // LocalStorage might fail in restricted environments
  }
  return DEFAULT_LANGUAGE
}

export interface I18nContextType {
  language: Language
  setLanguage: (lang: Language) => void
  t: (keyPath: string, params?: Record<string, string | number>) => string
  dictionary: TranslationDictionary
}

export const I18nContext = createContext<I18nContextType | null>(null)

export interface I18nProviderProps {
  children: React.ReactNode
  initialLanguage?: Language
  storage?: IStorageService
}

export const I18nProvider: React.FC<I18nProviderProps> = ({
  children,
  initialLanguage,
  storage = localStorageService,
}) => {
  const [language, setLanguageState] = useState<Language>(() => {
    return initialLanguage || getCurrentStoredLanguage(storage)
  })

  const setLanguage = useCallback(
    (lang: Language) => {
      setLanguageState(lang)
      try {
        storage.setItem(STORAGE_KEY_LANGUAGE, lang)
      } catch {
        // Ignore storage error
      }
      // Sync document language attribute if running in browser
      if (typeof document !== 'undefined') {
        document.documentElement.lang = lang
      }
    },
    [storage],
  )

  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = language
    }
  }, [language])

  const t = useCallback(
    (keyPath: string, params?: Record<string, string | number>): string => {
      const keys = keyPath.split('.')
      let currentVal: unknown = dictionaries[language]

      for (const k of keys) {
        if (currentVal && typeof currentVal === 'object' && k in currentVal) {
          currentVal = (currentVal as Record<string, unknown>)[k]
        } else {
          // Fallback to Spanish if missing in target
          let fallbackVal: unknown = dictionaries.es
          for (const fbKey of keys) {
            if (fallbackVal && typeof fallbackVal === 'object' && fbKey in fallbackVal) {
              fallbackVal = (fallbackVal as Record<string, unknown>)[fbKey]
            } else {
              fallbackVal = null
              break
            }
          }
          currentVal = fallbackVal ?? keyPath
          break
        }
      }

      if (typeof currentVal !== 'string') {
        return keyPath
      }

      let result = currentVal
      if (params) {
        Object.entries(params).forEach(([paramKey, paramVal]) => {
          result = result.replace(new RegExp(`{${paramKey}}`, 'g'), String(paramVal))
        })
      }

      return result
    },
    [language],
  )

  const value = useMemo<I18nContextType>(
    () => ({
      language,
      setLanguage,
      t,
      dictionary: dictionaries[language],
    }),
    [language, setLanguage, t],
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}
