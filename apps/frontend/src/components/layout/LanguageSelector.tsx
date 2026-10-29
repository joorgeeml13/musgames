import React from 'react'
import { useTranslation } from '@/i18n/useTranslation'
import type { Language } from '@/i18n/types'
import { Languages } from 'lucide-react'

export const LanguageSelector: React.FC = () => {
  const { language, setLanguage } = useTranslation()

  const toggleLanguage = () => {
    const nextLang: Language = language === 'es' ? 'en' : 'es'
    setLanguage(nextLang)
  }

  return (
    <div className="flex items-center gap-1 bg-secondary/70 border border-border/80 rounded-xl p-0.5 shadow-inner">
      <button
        type="button"
        onClick={() => setLanguage('es')}
        className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold transition-all ${
          language === 'es'
            ? 'bg-emerald-600 text-white shadow-md shadow-emerald-950'
            : 'text-muted-foreground hover:text-white'
        }`}
        title="Cambiar a Español"
        aria-label="Español"
      >
        <span>🇪🇸</span>
        <span className="hidden sm:inline">ES</span>
      </button>

      <button
        type="button"
        onClick={() => setLanguage('en')}
        className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold transition-all ${
          language === 'en'
            ? 'bg-emerald-600 text-white shadow-md shadow-emerald-950'
            : 'text-muted-foreground hover:text-white'
        }`}
        title="Switch to English"
        aria-label="English"
      >
        <span>🇬🇧</span>
        <span className="hidden sm:inline">EN</span>
      </button>

      {/* Mobile compact toggle button */}
      <button
        type="button"
        onClick={toggleLanguage}
        className="sm:hidden p-1 text-muted-foreground hover:text-white"
        aria-label="Toggle language"
      >
        <Languages className="h-3.5 w-3.5" />
      </button>
    </div>
  )
}
