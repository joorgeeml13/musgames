import { describe, it, expect, beforeEach } from 'vitest'
import { PlayerRepository } from './playerRepository'
import { HttpClient } from '../api/httpClient'

describe('PlayerRepository Infrastructure & Domain', () => {
  let repository: PlayerRepository

  beforeEach(() => {
    // Create isolated repo without live backend to test domain logic & mock fallbacks
    const client = new HttpClient('http://invalid.local')
    repository = new PlayerRepository(client)
  })

  it('retrieves full leaderboard when no filters are provided', async () => {
    const list = await repository.getLeaderboard()
    expect(list.length).toBeGreaterThanOrEqual(8)
    expect(list[0].rank).toBe(1)
    expect(list[0].player.username).toBe('Don_Ignacio_Navarra')
  })

  it('filters leaderboard by grandmaster tier', async () => {
    const list = await repository.getLeaderboard({ filter: 'grandmaster' })
    expect(list.length).toBe(2)
    list.forEach((entry) => {
      expect(entry.tier).toBe('grandmaster')
    })
  })

  it('filters leaderboard by search query', async () => {
    const list = await repository.getLeaderboard({ search: 'carmen' })
    expect(list.length).toBe(1)
    expect(list[0].player.username).toContain('Carmen')
  })

  it('retrieves current player profile and stats', async () => {
    const player = await repository.getCurrentPlayer()
    expect(player.elo).toBe(1645)
    expect(player.username).toContain('Jorge')

    const stats = await repository.getCurrentPlayerStats()
    expect(stats.elo).toBe(1645)
    expect(stats.globalRank).toBe(8)
    expect(stats.winRate).toBe(62.5)
    expect(stats.ordagosWon).toBe(39)
  })
})
