import { useState, useEffect, useCallback } from 'react'
import type { Room, CreateRoomCommand, RoomFilterType } from '../domain/room'
import { lobbyRepository } from '../infrastructure/repositories/lobbyRepository'

export function useLobby(initialFilter: RoomFilterType = 'all') {
  const [rooms, setRooms] = useState<Room[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [error, setError] = useState<string | null>(null)
  const [filterType, setFilterType] = useState<RoomFilterType>(initialFilter)
  const [searchQuery, setSearchQuery] = useState<string>('')
  const [isSearchingMatch, setIsSearchingMatch] = useState<boolean>(false)
  const [searchSeconds, setSearchSeconds] = useState<number>(0)

  const fetchRooms = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await lobbyRepository.getRooms({
        filter: filterType,
        search: searchQuery,
      })
      setRooms(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error loading rooms')
    } finally {
      setLoading(false)
    }
  }, [filterType, searchQuery])

  useEffect(() => {
    let isMounted = true
    lobbyRepository
      .getRooms({ filter: filterType, search: searchQuery })
      .then((result) => {
        if (isMounted) {
          setRooms(result)
          setLoading(false)
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err instanceof Error ? err.message : 'Error loading rooms')
          setLoading(false)
        }
      })

    return () => {
      isMounted = false
    }
  }, [filterType, searchQuery])

  // Matchmaking timer simulation
  useEffect(() => {
    if (!isSearchingMatch) return

    const timer = setInterval(() => {
      setSearchSeconds((s) => s + 1)
    }, 1000)

    return () => clearInterval(timer)
  }, [isSearchingMatch])

  const toggleMatchmaking = useCallback(() => {
    setIsSearchingMatch((prev) => {
      if (prev) {
        setSearchSeconds(0)
        return false
      }
      return true
    })
  }, [])

  const createRoom = useCallback(
    async (command: CreateRoomCommand): Promise<Room> => {
      const newRoom = await lobbyRepository.createRoom(command)
      await fetchRooms()
      return newRoom
    },
    [fetchRooms],
  )

  const joinRoom = useCallback(
    async (roomId: string, seat?: string) => {
      await lobbyRepository.joinRoom(roomId, seat)
      await fetchRooms()
    },
    [fetchRooms],
  )

  return {
    rooms,
    loading,
    error,
    filterType,
    setFilterType,
    searchQuery,
    setSearchQuery,
    isSearchingMatch,
    searchSeconds,
    toggleMatchmaking,
    createRoom,
    joinRoom,
    refetch: fetchRooms,
  }
}
