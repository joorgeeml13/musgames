import type {
  IPlayerRepository,
  LeaderboardQueryParams,
} from '../../ports/IPlayerRepository'
import type {
  Player,
  LeaderboardEntry,
  PlayerStatsSummary,
} from '../../domain/player'
import { httpClient, type HttpClient } from '../api/httpClient'

const MOCK_CURRENT_USER: Player = {
  id: 'player-you',
  username: 'Jorge (Tú)',
  displayName: 'Jorge',
  title: 'Tahúr de Primera',
  elo: 1645,
  gamesPlayed: 184,
  wins: 115,
  losses: 69,
  winRate: 62.5,
  ordagosWon: 39,
  tier: 'platinum',
}

const MOCK_LEADERBOARD_PLAYERS: Player[] = [
  {
    id: 'p-1',
    username: 'Don_Ignacio_Navarra',
    displayName: 'Don Ignacio',
    title: 'Gran Maestro del Órdago',
    elo: 2240,
    gamesPlayed: 612,
    wins: 454,
    losses: 158,
    winRate: 74.2,
    ordagosWon: 184,
    tier: 'grandmaster',
  },
  {
    id: 'p-2',
    username: 'Mikel_San_Sebastian',
    displayName: 'Mikel San Sebastián',
    title: 'Gran Maestro del Órdago',
    elo: 2195,
    gamesPlayed: 540,
    wins: 388,
    losses: 152,
    winRate: 71.8,
    ordagosWon: 142,
    tier: 'grandmaster',
  },
  {
    id: 'p-3',
    username: 'Aitor_Korta',
    displayName: 'Aitor Korta',
    title: 'Maestro Envidador',
    elo: 2110,
    gamesPlayed: 489,
    wins: 340,
    losses: 149,
    winRate: 69.5,
    ordagosWon: 119,
    tier: 'master',
  },
  {
    id: 'p-4',
    username: 'Carmen_Tahur_Madrid',
    displayName: 'Carmen Madrid',
    title: 'Maestro Envidador',
    elo: 2045,
    gamesPlayed: 430,
    wins: 293,
    losses: 137,
    winRate: 68.1,
    ordagosWon: 98,
    tier: 'master',
  },
  {
    id: 'p-5',
    username: 'Patxi_El_Duplex',
    displayName: 'Patxi el Dúplex',
    title: 'Tahúr de Primera',
    elo: 1980,
    gamesPlayed: 395,
    wins: 262,
    losses: 133,
    winRate: 66.4,
    ordagosWon: 85,
    tier: 'diamond',
  },
  {
    id: 'p-6',
    username: 'Javier_31_Real',
    displayName: 'Javier 31 Real',
    title: 'Tahúr de Primera',
    elo: 1920,
    gamesPlayed: 360,
    wins: 234,
    losses: 126,
    winRate: 65.0,
    ordagosWon: 74,
    tier: 'diamond',
  },
  {
    id: 'p-7',
    username: 'Lucia_Reyes_Ases',
    displayName: 'Lucía Reyes Ases',
    title: 'Envidador Veterano',
    elo: 1860,
    gamesPlayed: 310,
    wins: 198,
    losses: 112,
    winRate: 63.8,
    ordagosWon: 62,
    tier: 'platinum',
  },
  MOCK_CURRENT_USER,
]

export class PlayerRepository implements IPlayerRepository {
  private client: HttpClient

  constructor(client: HttpClient = httpClient) {
    this.client = client
  }

  public async getLeaderboard(params?: LeaderboardQueryParams): Promise<LeaderboardEntry[]> {
    try {
      const response = await this.client.get<LeaderboardEntry[]>('/api/leaderboard', {
        filter: params?.filter,
        search: params?.search,
      })
      if (response.data && Array.isArray(response.data) && response.data.length > 0) {
        return response.data
      }
    } catch {
      // Backend offline or endpoint not yet loaded: use domain mock
    }

    // Filter mock data
    let filtered = [...MOCK_LEADERBOARD_PLAYERS]

    if (params?.search && params.search.trim()) {
      const q = params.search.toLowerCase().trim()
      filtered = filtered.filter(
        (p) =>
          p.username.toLowerCase().includes(q) ||
          p.displayName.toLowerCase().includes(q) ||
          p.title.toLowerCase().includes(q),
      )
    }

    if (params?.filter === 'grandmaster') {
      filtered = filtered.filter((p) => p.tier === 'grandmaster')
    } else if (params?.filter === 'friends') {
      // Simulate subset of friends
      filtered = filtered.filter(
        (p) => p.id === 'player-you' || p.username === 'Mikel_San_Sebastian' || p.username === 'Aitor_Korta',
      )
    }

    return filtered.map((player, index) => ({
      rank: index + 1,
      player,
      winRate: player.winRate,
      tier: player.tier,
    }))
  }

  public async getPlayerProfile(username: string): Promise<Player> {
    try {
      const response = await this.client.get<Player>(`/api/leaderboard/${encodeURIComponent(username)}`)
      if (response.data) {
        return response.data
      }
    } catch {
      // Fallback
    }

    const found = MOCK_LEADERBOARD_PLAYERS.find(
      (p) => p.username.toLowerCase() === username.toLowerCase(),
    )
    if (found) {
      return found
    }

    return MOCK_CURRENT_USER
  }

  public async getCurrentPlayer(): Promise<Player> {
    try {
      const response = await this.client.get<Player>('/api/player/me')
      if (response.data) {
        return response.data
      }
    } catch {
      // Fallback
    }
    return MOCK_CURRENT_USER
  }

  public async getCurrentPlayerStats(): Promise<PlayerStatsSummary> {
    return {
      elo: MOCK_CURRENT_USER.elo,
      globalRank: 8,
      winRate: MOCK_CURRENT_USER.winRate,
      wins: MOCK_CURRENT_USER.wins,
      losses: MOCK_CURRENT_USER.losses,
      ordagosWon: MOCK_CURRENT_USER.ordagosWon,
      bluffSuccessRate: 78,
      tier: MOCK_CURRENT_USER.tier,
      title: MOCK_CURRENT_USER.title,
    }
  }
}

export const playerRepository = new PlayerRepository()
