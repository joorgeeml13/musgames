import React from 'react'
import { Sparkles, Shield, User, Flame, Disc } from 'lucide-react'
import type { TablePlayer } from './types'
import { Badge } from '@/components/ui/badge'
import { useTranslation } from '@/i18n/useTranslation'

interface TableSceneProps {
  phase?: string
  teamScoreA?: number
  teamScoreB?: number
  players?: TablePlayer[]
  interactive?: boolean
}

export const TableScene: React.FC<TableSceneProps> = ({
  phase,
  teamScoreA = 18,
  teamScoreB = 14,
  players,
}) => {
  const { t } = useTranslation()

  const defaultPlayers: TablePlayer[] = [
    {
      id: '1',
      name: `Jorge (${t('common.you')})`,
      position: 'south',
      role: 'you',
      elo: 1645,
      cardsCount: 4,
      currentAction: t('phases.envido'),
    },
    {
      id: '2',
      name: `Mikel (${t('common.teammate')})`,
      position: 'north',
      role: 'teammate',
      elo: 1610,
      cardsCount: 4,
      currentAction: t('common.mus'),
    },
    {
      id: '3',
      name: `Iñaki (${t('common.rival')} A)`,
      position: 'west',
      role: 'rival',
      elo: 1680,
      cardsCount: 4,
      currentAction: t('phases.paso'),
    },
    {
      id: '4',
      name: `Aitor (${t('common.rival')} B)`,
      position: 'east',
      role: 'rival',
      elo: 1590,
      cardsCount: 4,
      currentAction: t('phases.quiero'),
    },
  ]

  const activePlayers = players || defaultPlayers
  const currentPhase = phase || `${t('phases.grandeTitle')} - 2 ${t('common.stones')}`

  // Finds player by position
  const getPlayer = (pos: string) => activePlayers.find((p) => p.position === pos)

  const southPlayer = getPlayer('south')
  const northPlayer = getPlayer('north')
  const westPlayer = getPlayer('west')
  const eastPlayer = getPlayer('east')

  return (
    <div className="relative w-full aspect-[16/10] max-h-[640px] rounded-2xl bg-gradient-to-b from-slate-950 via-zinc-900 to-slate-950 border-2 border-amber-900/40 p-4 md:p-8 flex items-center justify-center overflow-hidden shadow-2xl shadow-black/80">
      {/* Tavern Ambient Lighting & Vignette */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_45%,rgba(245,158,11,0.12)_0%,transparent_65%)] pointer-events-none" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_40%,rgba(0,0,0,0.75)_100%)] pointer-events-none" />

      {/* Decorative overhead tavern lamp effect */}
      <div className="absolute -top-10 left-1/2 -translate-x-1/2 w-72 h-16 bg-amber-400/20 blur-2xl rounded-full pointer-events-none" />

      {/* 3D Canvas / Ready Layer Annotation */}
      <div className="absolute top-4 left-4 z-20 flex items-center gap-2 bg-slate-900/80 backdrop-blur-md px-3 py-1.5 rounded-lg border border-border/60 text-xs">
        <Sparkles className="h-3.5 w-3.5 text-amber-400 animate-spin" />
        <span className="font-semibold text-slate-200">{t('table3d.title')}</span>
        <Badge variant="live" className="text-[10px] ml-1">
          {t('table3d.liveEngine')}
        </Badge>
      </div>

      {/* Main Oval Green Felt Poker/Mus Table */}
      <div className="relative w-[92%] h-[82%] rounded-[100px] md:rounded-[180px] bg-[#0c2f1e] border-[12px] border-[#2e180e] shadow-[inset_0_0_80px_rgba(0,0,0,0.85),0_15px_35px_rgba(0,0,0,0.9)] flex items-center justify-center p-6">
        {/* Felt Texture Grain Inner Border */}
        <div className="absolute inset-3 rounded-[90px] md:rounded-[165px] border border-emerald-500/20 pointer-events-none" />

        {/* Center Table Details: Deck, Amarrakos, Phase Banner */}
        <div className="flex flex-col items-center gap-3 z-10 text-center">
          <div className="bg-slate-950/80 backdrop-blur-md border border-amber-500/30 px-4 py-1.5 rounded-full shadow-lg">
            <span className="text-xs font-bold text-amber-300 tracking-wide flex items-center gap-1.5">
              <Flame className="h-3.5 w-3.5 text-amber-400" />
              FASE: {currentPhase.toUpperCase()}
            </span>
          </div>

          {/* Central Deck of Spanish Cards */}
          <div className="relative group cursor-pointer">
            <div className="w-16 h-24 rounded-lg bg-red-950 border-2 border-amber-400/70 shadow-2xl flex flex-col items-center justify-center transform -rotate-3 transition-transform group-hover:scale-105">
              <div className="w-12 h-20 rounded border border-amber-400/30 bg-[#420d09] flex items-center justify-center">
                <span className="text-[10px] font-black text-amber-300 uppercase tracking-tighter">
                  {t('table3d.deck')}
                </span>
              </div>
            </div>
            {/* Amarrakos / Tantitos plate in center */}
            <div className="absolute -bottom-3 -right-6 flex items-center gap-1 bg-black/70 px-2 py-0.5 rounded-full border border-amber-500/30">
              <Disc className="h-3.5 w-3.5 text-amber-400 animate-pulse" />
              <span className="text-[10px] font-bold text-white">{t('table3d.amarrakos')}</span>
            </div>
          </div>

          {/* Tanteo Scoreboard */}
          <div className="flex items-center gap-4 bg-slate-950/85 backdrop-blur-sm border border-border/80 px-4 py-1.5 rounded-xl text-xs">
            <div className="text-right">
              <span className="text-muted-foreground block text-[10px]">{t('common.you')} & {t('common.teammate')}</span>
              <span className="font-extrabold text-emerald-400 text-sm">{teamScoreA} {t('common.points')}</span>
            </div>
            <div className="h-6 w-px bg-border/60" />
            <div className="text-left">
              <span className="text-muted-foreground block text-[10px]">{t('common.rival')}</span>
              <span className="font-extrabold text-red-400 text-sm">{teamScoreB} {t('common.points')}</span>
            </div>
          </div>
        </div>

        {/* NORTH PLAYER (Teammate) */}
        {northPlayer && (
          <div className="absolute -top-3 md:top-2 left-1/2 -translate-x-1/2 flex flex-col items-center gap-1 z-20">
            <div className="flex items-center gap-2 bg-slate-900/90 border border-emerald-500/40 px-3 py-1 rounded-full shadow-lg">
              <div className="w-5 h-5 rounded-full bg-emerald-800 text-emerald-200 text-[10px] font-bold flex items-center justify-center">
                <User className="h-3 w-3" />
              </div>
              <span className="text-xs font-bold text-white">{northPlayer.name}</span>
              <span className="text-[10px] text-amber-400 font-mono">({northPlayer.elo})</span>
            </div>
            {northPlayer.currentAction && (
              <span className="text-[10px] bg-emerald-500/20 text-emerald-300 font-semibold px-2 py-0.5 rounded-md border border-emerald-500/30 animate-pulse">
                {northPlayer.currentAction}
              </span>
            )}
            {/* Cards facedown */}
            <div className="flex -space-x-3 mt-0.5">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="w-6 h-9 rounded bg-amber-950 border border-amber-600/50 shadow-md" />
              ))}
            </div>
          </div>
        )}

        {/* SOUTH PLAYER (You) */}
        {southPlayer && (
          <div className="absolute -bottom-4 md:bottom-2 left-1/2 -translate-x-1/2 flex flex-col items-center gap-1 z-20">
            {/* Player's actual hand (face up) */}
            <div className="flex -space-x-3 -mb-1">
              {['Rey de Copas', 'Rey de Bastos', 'Tres de Espadas', 'As de Oros'].map((card, i) => (
                <div
                  key={i}
                  className="w-10 h-14 md:w-12 md:h-16 rounded-lg bg-amber-50 border border-amber-300 shadow-xl flex flex-col justify-between p-1 hover:-translate-y-2 transition-transform cursor-pointer"
                >
                  <span className="text-[9px] font-black text-slate-900 leading-none">{card.split(' ')[0]}</span>
                  <span className="text-[8px] font-bold text-slate-700 text-right">{card.split(' ')[2]}</span>
                </div>
              ))}
            </div>
            <div className="flex items-center gap-2 bg-emerald-950/95 border-2 border-emerald-400 px-3.5 py-1 rounded-full shadow-lg">
              <Shield className="h-3.5 w-3.5 text-emerald-400" />
              <span className="text-xs font-extrabold text-white">{southPlayer.name}</span>
              <span className="text-[10px] text-amber-400 font-mono font-bold">1,645 ELO</span>
            </div>
            {southPlayer.currentAction && (
              <span className="text-[10px] bg-amber-500 text-slate-950 font-bold px-2.5 py-0.5 rounded-full shadow">
                {southPlayer.currentAction}
              </span>
            )}
          </div>
        )}

        {/* WEST PLAYER (Rival A) */}
        {westPlayer && (
          <div className="absolute -left-2 md:left-4 top-1/2 -translate-y-1/2 flex flex-col items-center gap-1 z-20">
            <div className="flex items-center gap-1.5 bg-slate-900/90 border border-border/80 px-2.5 py-1 rounded-full shadow-lg">
              <span className="text-[11px] font-bold text-slate-200">{westPlayer.name}</span>
            </div>
            <div className="flex flex-col -space-y-4">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="w-8 h-5 rounded bg-amber-950 border border-amber-700/60 shadow" />
              ))}
            </div>
            {westPlayer.currentAction && (
              <span className="text-[10px] bg-slate-800 text-slate-300 px-2 py-0.5 rounded border border-border">
                {westPlayer.currentAction}
              </span>
            )}
          </div>
        )}

        {/* EAST PLAYER (Rival B) */}
        {eastPlayer && (
          <div className="absolute -right-2 md:right-4 top-1/2 -translate-y-1/2 flex flex-col items-center gap-1 z-20">
            <div className="flex items-center gap-1.5 bg-slate-900/90 border border-border/80 px-2.5 py-1 rounded-full shadow-lg">
              <span className="text-[11px] font-bold text-slate-200">{eastPlayer.name}</span>
            </div>
            <div className="flex flex-col -space-y-4">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="w-8 h-5 rounded bg-amber-950 border border-amber-700/60 shadow" />
              ))}
            </div>
            {eastPlayer.currentAction && (
              <span className="text-[10px] bg-red-950/80 text-red-300 px-2 py-0.5 rounded border border-red-500/40">
                {eastPlayer.currentAction}
              </span>
            )}
          </div>
        )}
      </div>

      {/* Quick In-Game Action Bar in Table Footer */}
      <div className="absolute bottom-2 right-4 z-20 hidden md:flex items-center gap-2">
        <span className="text-[11px] text-muted-foreground font-medium">{t('table3d.quickControls')}</span>
        <button className="px-2.5 py-1 text-xs font-bold rounded bg-emerald-600 hover:bg-emerald-500 text-white shadow">
          {t('table3d.passBtn')}
        </button>
        <button className="px-2.5 py-1 text-xs font-bold rounded bg-amber-600 hover:bg-amber-500 text-slate-950 shadow">
          {t('table3d.enviteBtn')}
        </button>
        <button className="px-2.5 py-1 text-xs font-bold rounded bg-red-700 hover:bg-red-600 text-white shadow">
          {t('table3d.ordagoBtn')}
        </button>
      </div>
    </div>
  )
}
