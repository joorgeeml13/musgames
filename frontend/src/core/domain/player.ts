export type PlayerRankTier = 'grandmaster' | 'master' | 'diamond' | 'platinum' | 'gold'

export interface Player {
  id: string
  username: string
  displayName: string
  title: string
  elo: number
  gamesPlayed: number
  wins: number
  losses: number
  winRate: number
  ordagosWon: number
  tier: PlayerRankTier
  avatarUrl?: string
}

export interface LeaderboardEntry {
  rank: number
  player: Player
  winRate: number
  tier: PlayerRankTier
}

export interface PlayerStatsSummary {
  elo: number
  globalRank: number
  winRate: number
  wins: number
  losses: number
  ordagosWon: number
  bluffSuccessRate: number
  tier: PlayerRankTier
  title: string
}
