import React from 'react'
import { Card } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from '@/components/ui/table'
import { TrendingUp } from 'lucide-react'
import type { LeaderboardEntry } from '@/core/domain/player'
import { useTranslation } from '@/i18n/useTranslation'
import { getPlayerInitials } from '@/lib/utils'

interface LeaderboardTableProps {
  entries: LeaderboardEntry[]
  loading?: boolean
}

export const LeaderboardTable: React.FC<LeaderboardTableProps> = ({ entries, loading }) => {
  const { t } = useTranslation()

  return (
    <Card className="border-border/70 overflow-hidden shadow-xl bg-card/70 backdrop-blur-md">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead className="w-16 text-center">{t('leaderboard.rankCol')}</TableHead>
            <TableHead>{t('leaderboard.playerCol')}</TableHead>
            <TableHead>{t('leaderboard.tierCol')}</TableHead>
            <TableHead className="text-right">{t('leaderboard.eloCol')}</TableHead>
            <TableHead className="text-right hidden sm:table-cell">{t('leaderboard.gamesCol')}</TableHead>
            <TableHead className="text-right hidden sm:table-cell">{t('leaderboard.winrateCol')}</TableHead>
            <TableHead className="text-right hidden md:table-cell">{t('leaderboard.ordagosCol')}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {loading ? (
            <TableRow>
              <TableCell colSpan={7} className="text-center py-10 text-muted-foreground">
                {t('common.loading')}
              </TableCell>
            </TableRow>
          ) : entries.length === 0 ? (
            <TableRow>
              <TableCell colSpan={7} className="text-center py-10 text-muted-foreground">
                No hay jugadores que coincidan con la búsqueda.
              </TableCell>
            </TableRow>
          ) : (
            entries.map((entry) => {
              const { rank, player, winRate, tier } = entry
              const isCurrentUser = player.username.includes('(Tú)') || player.id === 'player-you'

              return (
                <TableRow
                  key={player.id || rank}
                  className={isCurrentUser ? 'bg-emerald-950/30 border-l-4 border-l-emerald-500 font-semibold' : ''}
                >
                  <TableCell className="text-center font-bold">
                    {rank === 1 && (
                      <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-amber-500 text-slate-950 shadow-md">
                        🥇
                      </span>
                    )}
                    {rank === 2 && (
                      <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-slate-300 text-slate-950 shadow-md">
                        🥈
                      </span>
                    )}
                    {rank === 3 && (
                      <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-amber-700 text-white shadow-md">
                        🥉
                      </span>
                    )}
                    {rank > 3 && (
                      <span className="text-muted-foreground font-mono">#{rank}</span>
                    )}
                  </TableCell>

                  <TableCell>
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-secondary border border-border/80 flex items-center justify-center font-bold text-xs text-white">
                        {getPlayerInitials(player.displayName || player.username)}
                      </div>
                      <div className="flex flex-col">
                        <span className={`text-sm ${isCurrentUser ? 'text-emerald-400 font-extrabold' : 'text-white'}`}>
                          {player.displayName || player.username}
                        </span>
                      </div>
                    </div>
                  </TableCell>

                  <TableCell>
                    <Badge
                      variant={
                        tier === 'grandmaster'
                          ? 'gold'
                          : tier === 'master'
                          ? 'default'
                          : 'secondary'
                      }
                      className="text-[11px]"
                    >
                      {player.title}
                    </Badge>
                  </TableCell>

                  <TableCell className="text-right font-mono font-bold text-white text-base">
                    {player.elo}
                  </TableCell>

                  <TableCell className="text-right font-mono text-muted-foreground hidden sm:table-cell">
                    {player.gamesPlayed}
                  </TableCell>

                  <TableCell className="text-right font-mono text-emerald-400 font-semibold hidden sm:table-cell">
                    <span className="inline-flex items-center gap-1 justify-end">
                      <TrendingUp className="h-3.5 w-3.5 text-emerald-400" />
                      {winRate}%
                    </span>
                  </TableCell>

                  <TableCell className="text-right font-mono text-amber-400 hidden md:table-cell">
                    {player.ordagosWon}
                  </TableCell>
                </TableRow>
              )
            })
          )}
        </TableBody>
      </Table>
    </Card>
  )
}
