import { createRootRoute, Outlet } from '@tanstack/react-router'
import { Navbar } from '@/components/layout/Navbar'
import { Footer } from '@/components/layout/Footer'
import { I18nProvider } from '@/i18n/I18nContext'

export const rootRoute = createRootRoute({
  component: RootLayout,
})

function RootLayout() {
  return (
    <I18nProvider>
      <div className="min-h-screen flex flex-col bg-background text-foreground antialiased selection:bg-emerald-500/30 selection:text-emerald-200">
        <Navbar />
        <main className="flex-1">
          <Outlet />
        </main>
        <Footer />
      </div>
    </I18nProvider>
  )
}
