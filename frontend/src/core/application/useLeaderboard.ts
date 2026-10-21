import { useState, useEffect, useCallback } from 'react'
import type { LeaderboardEntry } from '../domain/player'
import type { LeaderboardQueryParams } from '../ports/IPlayerRepository'
import { playerRepository } from '../infrastructure/repositories/playerRepository'

export function useLeaderboard(initialParams?: LeaderboardQueryParams) {
  const [data, setData] = useState<LeaderboardEntry[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<'global' | 'grandmaster' | 'friends'>(
    initialParams?.filter || 'global',
  )
  const [search, setSearch] = useState<string>(initialParams?.search || '')

  const fetchLeaderboard = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await playerRepository.getLeaderboard({ filter, search })
      setData(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error loading leaderboard')
    } finally {
      setLoading(false)
    }
  }, [filter, search])

  useEffect(() => {
    let isMounted = true
    playerRepository
      .getLeaderboard({ filter, search })
      .then((result) => {
        if (isMounted) {
          setData(result)
          setLoading(false)
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err instanceof Error ? err.message : 'Error loading leaderboard')
          setLoading(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [filter, search])

  return {
    data,
    loading,
    error,
    filter,
    setFilter,
    search,
    setSearch,
    refetch: fetchLeaderboard,
  }
}
