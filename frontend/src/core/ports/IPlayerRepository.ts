import type { Player, LeaderboardEntry, PlayerStatsSummary } from '../domain/player'

export interface LeaderboardQueryParams {
  filter?: 'global' | 'grandmaster' | 'friends'
  search?: string
}

export interface IPlayerRepository {
  getLeaderboard(params?: LeaderboardQueryParams): Promise<LeaderboardEntry[]>
  getPlayerProfile(username: string): Promise<Player>
  getCurrentPlayer(): Promise<Player>
  getCurrentPlayerStats(): Promise<PlayerStatsSummary>
}
