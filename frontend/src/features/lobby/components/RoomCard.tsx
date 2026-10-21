import React from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Lock, Globe, Users } from 'lucide-react'
import type { Room } from '@/core/domain/room'
import { useTranslation } from '@/i18n/useTranslation'

interface RoomCardProps {
  room: Room
  onJoin: (room: Room) => void
}

export const RoomCard: React.FC<RoomCardProps> = ({ room, onJoin }) => {
  const { t } = useTranslation()
  const isFull = room.playersCount >= room.maxPlayers

  return (
    <Card className="border-border/70 hover:border-emerald-500/50 bg-card/60 backdrop-blur-md transition-all hover:shadow-lg hover:shadow-black/40">
      <CardContent className="p-5 flex flex-col justify-between h-full space-y-4">
        <div className="flex items-start justify-between gap-2">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="font-bold text-white text-base">{room.name}</span>
              {room.isPrivate ? (
                <Lock className="h-3.5 w-3.5 text-amber-400" />
              ) : (
                <Globe className="h-3.5 w-3.5 text-emerald-400" />
              )}
            </div>
            <span className="text-xs text-muted-foreground flex items-center gap-1.5">
              {t('lobby.host')}: <strong className="text-slate-300">{room.host}</strong>
            </span>
          </div>

          <div className="flex flex-col items-end gap-1">
            <Badge variant={room.isRanked ? 'gold' : 'secondary'} className="text-[10px]">
              {room.isRanked ? t('lobby.rankedBadge') : t('lobby.casualBadge')}
            </Badge>
            <span className="text-[11px] text-muted-foreground font-mono">
              {t('lobby.avgElo')}: {room.avgElo}
            </span>
          </div>
        </div>

        {/* Badges & Meta */}
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <span className="bg-secondary/60 px-2 py-0.5 rounded text-slate-300 border border-border/40">
            {room.mode}
          </span>
          <span className="bg-secondary/60 px-2 py-0.5 rounded text-slate-300 border border-border/40">
            {room.pointsTarget} {t('common.stones')}
          </span>
          <span className="text-emerald-400 font-semibold flex items-center gap-1 ml-auto">
            <Users className="h-3.5 w-3.5" /> {room.playersCount}/{room.maxPlayers} {t('lobby.playersCount')}
          </span>
        </div>

        {/* Action button */}
        <div className="pt-2 border-t border-border/40 flex items-center justify-between">
          <span className="text-[11px] text-muted-foreground">
            {isFull ? t('lobby.tableFull') : t('lobby.seatsAvailable')}
          </span>
          <Button
            size="sm"
            variant={isFull ? 'outline' : 'default'}
            className="gap-1.5"
            onClick={() => onJoin(room)}
          >
            {isFull ? t('lobby.spectateBtn') : t('lobby.sitDownBtn')}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
