export type Language = 'es' | 'en'

export interface TranslationDictionary {
  app: {
    title: string
  }
  common: {
    welcome: string
    language: string
  }
}
