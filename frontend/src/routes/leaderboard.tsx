import { createRoute } from '@tanstack/react-router'
import { rootRoute } from './__root'
import { Badge } from '@/components/ui/badge'
import { Trophy } from 'lucide-react'
import { useTranslation } from '@/i18n/useTranslation'
import { useLeaderboard, usePlayerStats } from '@/core/application'
import {
  LeaderboardStatsCards,
  LeaderboardFilterBar,
  LeaderboardTable,
} from '@/features/leaderboard'

export const leaderboardRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/leaderboard',
  component: LeaderboardPage,
})

function LeaderboardPage() {
  const { t } = useTranslation()
  const { stats } = usePlayerStats()
  const {
    data,
    loading,
    filter,
    setFilter,
    search,
    setSearch,
  } = useLeaderboard()

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8 space-y-8">
      {/* HEADER */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-border/50 pb-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Trophy className="h-7 w-7 text-amber-400" />
            <h1 className="text-3xl font-black text-white">{t('leaderboard.title')}</h1>
          </div>
          <p className="text-sm text-muted-foreground">
            {t('leaderboard.subtitle')}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant="gold" className="text-xs py-1 px-3">
            {t('leaderboard.seasonInfo')}
          </Badge>
        </div>
      </div>

      {/* USER STATS HIGHLIGHT */}
      <LeaderboardStatsCards stats={stats} />

      {/* FILTER & SEARCH BAR */}
      <LeaderboardFilterBar
        search={search}
        onSearchChange={setSearch}
        filter={filter}
        onFilterChange={setFilter}
      />

      {/* LEADERBOARD TABLE */}
      <LeaderboardTable entries={data} loading={loading} />
    </div>
  )
}
