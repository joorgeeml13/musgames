import { Link, useRouterState } from '@tanstack/react-router'
import { Spade, Trophy, Users, Play, Shield, Circle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useTranslation } from '@/i18n/useTranslation'
import { LanguageSelector } from './LanguageSelector'

export function Navbar() {
  const routerState = useRouterState()
  const currentPath = routerState.location.pathname
  const { t } = useTranslation()

  const navLinks = [
    { to: '/', label: t('nav.home'), icon: Spade },
    { to: '/lobby', label: t('nav.lobby'), icon: Users },
    { to: '/leaderboard', label: t('nav.leaderboard'), icon: Trophy },
  ]

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/70 bg-card/80 backdrop-blur-xl">
      <div className="container mx-auto flex h-16 items-center justify-between px-4 sm:px-6">
        {/* Brand / Logo */}
        <Link to="/" className="flex items-center gap-3 group">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-500 to-emerald-700 text-white shadow-lg shadow-emerald-900/50 group-hover:scale-105 transition-transform">
            <Spade className="h-5 w-5 fill-current" />
          </div>
          <div className="flex flex-col">
            <span className="text-lg font-black tracking-wider text-white uppercase flex items-center gap-2">
              Mus <span className="text-emerald-400">Tactics</span>
              <Badge variant="gold" className="text-[10px] py-0 px-1.5 font-bold">
                PRO
              </Badge>
            </span>
            <span className="text-[11px] font-medium text-muted-foreground tracking-tight">
              {t('common.tagline')}
            </span>
          </div>
        </Link>

        {/* Navigation links */}
        <nav className="hidden md:flex items-center gap-1 bg-secondary/50 p-1 rounded-xl border border-border/50">
          {navLinks.map((link) => {
            const Icon = link.icon
            const isActive = currentPath === link.to
            return (
              <Link
                key={link.to}
                to={link.to}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold transition-all ${
                  isActive
                    ? 'bg-emerald-600 text-white shadow-sm shadow-emerald-950'
                    : 'text-muted-foreground hover:text-white hover:bg-background/40'
                }`}
              >
                <Icon className={`h-4 w-4 ${isActive ? 'text-white' : 'text-emerald-400'}`} />
                {link.label}
              </Link>
            )
          })}
        </nav>

        {/* Right action / status */}
        <div className="flex items-center gap-3">
          <div className="hidden lg:flex items-center gap-2 px-3 py-1.5 rounded-full bg-background/60 border border-border/60 text-xs">
            <Circle className="h-2 w-2 fill-emerald-400 text-emerald-400 animate-pulse" />
            <span className="text-muted-foreground">{t('nav.onlineBadge')}:</span>
            <span className="font-bold text-emerald-400">1,420 {t('common.onlinePlayers')}</span>
          </div>

          {/* Multi-language Selector */}
          <LanguageSelector />

          <div className="flex items-center gap-2">
            <Link to="/lobby">
              <Button variant="gold" size="sm" className="hidden sm:inline-flex gap-2">
                <Play className="h-4 w-4 fill-current" />
                {t('nav.quickPlay')}
              </Button>
            </Link>

            <div className="flex items-center gap-2 pl-2 border-l border-border/60">
              <div className="h-9 w-9 rounded-full bg-slate-800 border border-emerald-500/40 flex items-center justify-center font-bold text-sm text-emerald-300 shadow-inner">
                JD
              </div>
              <div className="hidden xl:flex flex-col text-left">
                <span className="text-xs font-bold text-white">Jorge ({t('common.you')})</span>
                <span className="text-[11px] font-semibold text-amber-400 flex items-center gap-1">
                  <Shield className="h-3 w-3" /> 1,645 ELO
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  )
}
