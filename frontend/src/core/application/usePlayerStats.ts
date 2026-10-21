import { useState, useEffect, useCallback } from 'react'
import type { PlayerStatsSummary } from '../domain/player'
import { playerRepository } from '../infrastructure/repositories/playerRepository'

export function usePlayerStats() {
  const [stats, setStats] = useState<PlayerStatsSummary | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)

  const fetchStats = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await playerRepository.getCurrentPlayerStats()
      setStats(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error loading player stats')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let isMounted = true
    playerRepository
      .getCurrentPlayerStats()
      .then((result) => {
        if (isMounted) {
          setStats(result)
          setLoading(false)
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err instanceof Error ? err.message : 'Error loading player stats')
          setLoading(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  return {
    stats,
    loading,
    error,
    refetch: fetchStats,
  }
}
