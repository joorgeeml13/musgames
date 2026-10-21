import { createRoute, Link } from '@tanstack/react-router'
import { rootRoute } from './__root'
import { TableScene } from '@/features/table3d'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Play,
  Users,
  Trophy,
  Flame,
  Swords,
  Crown,
  Sparkles,
  Zap,
  ArrowRight,
  ShieldCheck,
} from 'lucide-react'
import { useTranslation } from '@/i18n/useTranslation'

export const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: HomePage,
})

function HomePage() {
  const { t } = useTranslation()

  return (
    <div className="space-y-16 pb-20">
      {/* HERO SECTION */}
      <section className="relative overflow-hidden pt-10 pb-16 md:pt-16 md:pb-24 border-b border-border/40">
        <div className="container mx-auto px-4 sm:px-6">
          <div className="flex flex-col items-center text-center max-w-4xl mx-auto space-y-6">
            <Badge variant="gold" className="px-3 py-1 gap-1.5 text-xs">
              <Sparkles className="h-3.5 w-3.5" /> {t('home.seasonBadge')}
            </Badge>

            <h1 className="text-4xl sm:text-6xl font-black tracking-tight text-white leading-[1.1]">
              {t('home.heroTitlePart1')} <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-teal-300 to-amber-400">
                {t('home.heroTitlePart2')}
              </span>
            </h1>

            <p className="text-base sm:text-lg text-muted-foreground max-w-2xl leading-relaxed">
              {t('home.heroDescription')}
            </p>

            <div className="flex flex-wrap items-center justify-center gap-4 pt-2">
              <Link to="/lobby">
                <Button variant="gold" size="lg" className="gap-2.5 text-base font-bold shadow-xl shadow-amber-950/40">
                  <Play className="h-5 w-5 fill-current" />
                  {t('home.playNowBtn')}
                </Button>
              </Link>
              <Link to="/lobby">
                <Button variant="outline" size="lg" className="gap-2 text-base">
                  <Users className="h-5 w-5 text-emerald-400" />
                  {t('home.browseRoomsBtn')}
                </Button>
              </Link>
              <Link to="/leaderboard">
                <Button variant="ghost" size="lg" className="gap-2 text-base text-muted-foreground hover:text-white">
                  <Trophy className="h-5 w-5 text-amber-400" />
                  {t('home.leaderboardBtn')}
                </Button>
              </Link>
            </div>

            {/* Quick trust / features badge row */}
            <div className="flex flex-wrap items-center justify-center gap-6 pt-4 text-xs text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="h-4 w-4 text-emerald-400" /> {t('home.eloFeature')}
              </span>
              <span className="flex items-center gap-1.5">
                <Crown className="h-4 w-4 text-amber-400" /> {t('home.kingsFeature')}
              </span>
              <span className="flex items-center gap-1.5">
                <Zap className="h-4 w-4 text-cyan-400" /> {t('home.realtimeFeature')}
              </span>
            </div>
          </div>

          {/* 3D TABLE SHOWCASE */}
          <div className="mt-14 max-w-5xl mx-auto">
            <div className="flex items-center justify-between mb-3 px-2">
              <div className="flex items-center gap-2">
                <Flame className="h-4 w-4 text-amber-400" />
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
                  {t('home.tablePreviewTitle')}
                </span>
              </div>
              <Badge variant="live" className="text-[11px]">
                {t('home.spectatorBadge')}
              </Badge>
            </div>
            <TableScene />
          </div>
        </div>
      </section>

      {/* GAME PHASES & MECHANICS */}
      <section className="container mx-auto px-4 sm:px-6">
        <div className="text-center max-w-2xl mx-auto mb-10 space-y-2">
          <Badge variant="default" className="text-xs">{t('home.rulesBadge')}</Badge>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white">
            {t('home.rulesTitle')}
          </h2>
          <p className="text-sm text-muted-foreground">
            {t('home.rulesDescription')}
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Card className="hover:border-emerald-500/50 transition-all">
            <CardHeader>
              <div className="w-10 h-10 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 font-black text-lg mb-2">
                1
              </div>
              <CardTitle>{t('phases.grandeTitle')}</CardTitle>
              <CardDescription>
                {t('phases.grandeDesc')}
              </CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground">
              {t('phases.grandeTip')}
            </CardContent>
          </Card>

          <Card className="hover:border-emerald-500/50 transition-all">
            <CardHeader>
              <div className="w-10 h-10 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 font-black text-lg mb-2">
                2
              </div>
              <CardTitle>{t('phases.chicaTitle')}</CardTitle>
              <CardDescription>
                {t('phases.chicaDesc')}
              </CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground">
              {t('phases.chicaTip')}
            </CardContent>
          </Card>

          <Card className="hover:border-amber-500/50 transition-all">
            <CardHeader>
              <div className="w-10 h-10 rounded-lg bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 font-black text-lg mb-2">
                3
              </div>
              <CardTitle>{t('phases.paresTitle')}</CardTitle>
              <CardDescription>
                {t('phases.paresDesc')}
              </CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground">
              {t('phases.paresTip')}
            </CardContent>
          </Card>

          <Card className="hover:border-red-500/50 transition-all">
            <CardHeader>
              <div className="w-10 h-10 rounded-lg bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-400 font-black text-lg mb-2">
                4
              </div>
              <CardTitle>{t('phases.juegoTitle')}</CardTitle>
              <CardDescription>
                {t('phases.juegoDesc')}
              </CardDescription>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground">
              {t('phases.juegoTip')}
            </CardContent>
          </Card>
        </div>
      </section>

      {/* QUICK CTA BANNER */}
      <section className="container mx-auto px-4 sm:px-6">
        <div className="rounded-2xl bg-gradient-to-r from-emerald-950 via-slate-900 to-amber-950/60 border border-emerald-500/30 p-8 md:p-12 flex flex-col md:flex-row items-center justify-between gap-6 shadow-2xl">
          <div className="space-y-2 text-center md:text-left">
            <h3 className="text-2xl font-black text-white flex items-center justify-center md:justify-start gap-2">
              <Swords className="h-6 w-6 text-amber-400" /> {t('home.ctaTitle')}
            </h3>
            <p className="text-sm text-slate-300 max-w-xl">
              {t('home.ctaDescription')}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <Link to="/lobby">
              <Button variant="gold" size="lg" className="gap-2">
                {t('home.enterLobbyBtn')}
                <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  )
}
