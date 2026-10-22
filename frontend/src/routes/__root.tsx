import { createRootRoute, Outlet } from '@tanstack/react-router'
import { LanguageSelector } from '@/components/layout/LanguageSelector'
import { I18nProvider } from '@/i18n/I18nContext'
import { useTranslation } from '@/i18n/useTranslation'

export const rootRoute = createRootRoute({
  component: RootLayout,
})

function AppHeader() {
  const { t } = useTranslation()
  return (
    <header className="border-b border-border/60 bg-card/60 backdrop-blur px-4 py-3 flex items-center justify-between">
      <span className="font-bold tracking-tight text-white">{t('app.title')}</span>
      <LanguageSelector />
    </header>
  )
}

function RootLayout() {
  return (
    <I18nProvider>
      <div className="min-h-screen flex flex-col bg-background text-foreground antialiased selection:bg-emerald-500/30 selection:text-emerald-200">
        <AppHeader />
        <main className="flex-1 flex flex-col">
          <Outlet />
        </main>
      </div>
    </I18nProvider>
  )
}
