import React from 'react'
import { Shield, Clock, Play, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useTranslation } from '@/i18n/useTranslation'

interface MatchmakingBannerProps {
  isSearching: boolean
  searchSeconds: number
  onToggleSearch: () => void
}

export const MatchmakingBanner: React.FC<MatchmakingBannerProps> = ({
  isSearching,
  searchSeconds,
  onToggleSearch,
}) => {
  const { t } = useTranslation()

  const formatTimer = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60)
    const secs = totalSeconds % 60
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  }

  return (
    <div className="rounded-2xl bg-gradient-to-r from-emerald-950/90 via-slate-900 to-emerald-950/60 border border-emerald-500/30 p-6 shadow-xl">
      <div className="flex flex-col lg:flex-row items-center justify-between gap-6">
        <div className="space-y-2 text-center lg:text-left">
          <div className="flex items-center justify-center lg:justify-start gap-2">
            <Shield className="h-5 w-5 text-emerald-400" />
            <span className="text-sm font-bold text-white tracking-wide uppercase">
              {t('lobby.matchmakingTitle')}
            </span>
          </div>
          <h2 className="text-2xl font-black text-white">
            {t('lobby.matchmakingSubtitle')}
          </h2>
          <p className="text-xs text-slate-300 max-w-lg">
            {t('lobby.matchmakingDesc')}
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-4 w-full lg:w-auto">
          <div className="flex items-center gap-4 bg-background/60 border border-border/70 px-4 py-2.5 rounded-xl text-xs w-full sm:w-auto justify-around">
            <div>
              <span className="text-muted-foreground block text-[10px]">{t('lobby.estTime')}</span>
              <span className="font-bold text-white flex items-center gap-1">
                <Clock className="h-3 w-3 text-emerald-400" /> ~35 seg
              </span>
            </div>
            <div className="h-8 w-px bg-border/60" />
            <div>
              <span className="text-muted-foreground block text-[10px]">{t('lobby.inQueue')}</span>
              <span className="font-bold text-emerald-400">{t('lobby.pairsCount')}</span>
            </div>
          </div>

          <Button
            size="lg"
            variant={isSearching ? 'destructive' : 'gold'}
            className="gap-2.5 font-bold w-full sm:w-auto min-w-[180px]"
            onClick={onToggleSearch}
          >
            {isSearching ? (
              <>
                <Loader2 className="h-5 w-5 animate-spin" />
                {t('lobby.cancelSearchBtn')}
              </>
            ) : (
              <>
                <Play className="h-5 w-5 fill-current" />
                {t('lobby.searchMatchBtn')}
              </>
            )}
          </Button>
        </div>
      </div>

      {isSearching && (
        <div className="mt-4 pt-4 border-t border-emerald-500/20 flex items-center justify-between text-xs text-emerald-300 animate-pulse">
          <span className="flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin" /> {t('lobby.searchingStatus')}
          </span>
          <span className="font-mono font-bold text-amber-400">{formatTimer(searchSeconds)}</span>
        </div>
      )}
    </div>
  )
}
