import React from 'react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Shield, Medal, Percent, Flame } from 'lucide-react'
import type { PlayerStatsSummary } from '@/core/domain/player'
import { useTranslation } from '@/i18n/useTranslation'

interface LeaderboardStatsCardsProps {
  stats: PlayerStatsSummary | null
}

export const LeaderboardStatsCards: React.FC<LeaderboardStatsCardsProps> = ({ stats }) => {
  const { t } = useTranslation()

  if (!stats) return null

  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
      <Card className="border-emerald-500/40 bg-gradient-to-br from-emerald-950/40 via-card/70 to-card">
        <CardHeader className="pb-2">
          <CardDescription className="text-xs uppercase font-bold text-emerald-400 flex items-center gap-1.5">
            <Shield className="h-3.5 w-3.5" /> {t('leaderboard.yourElo')}
          </CardDescription>
          <CardTitle className="text-3xl font-black text-white">{stats.elo.toLocaleString()}</CardTitle>
        </CardHeader>
        <CardContent className="text-xs text-muted-foreground">
          {stats.title} · {t('leaderboard.rankGap')}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardDescription className="text-xs uppercase font-bold text-amber-400 flex items-center gap-1.5">
            <Medal className="h-3.5 w-3.5" /> {t('leaderboard.globalPos')}
          </CardDescription>
          <CardTitle className="text-3xl font-black text-white">#{stats.globalRank}</CardTitle>
        </CardHeader>
        <CardContent className="text-xs text-muted-foreground">
          {t('leaderboard.topPercent')}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardDescription className="text-xs uppercase font-bold text-cyan-400 flex items-center gap-1.5">
            <Percent className="h-3.5 w-3.5" /> {t('leaderboard.winRate')}
          </CardDescription>
          <CardTitle className="text-3xl font-black text-white">{stats.winRate}%</CardTitle>
        </CardHeader>
        <CardContent className="text-xs text-muted-foreground">
          {stats.wins} {t('common.wins')} / {stats.losses} {t('common.losses')}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardDescription className="text-xs uppercase font-bold text-red-400 flex items-center gap-1.5">
            <Flame className="h-3.5 w-3.5" /> {t('leaderboard.ordagosWon')}
          </CardDescription>
          <CardTitle className="text-3xl font-black text-white">{stats.ordagosWon}</CardTitle>
        </CardHeader>
        <CardContent className="text-xs text-muted-foreground">
          {t('leaderboard.bluffSuccess')}
        </CardContent>
      </Card>
    </div>
  )
}
