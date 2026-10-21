import type { ILobbyService, LobbyQueryParams } from '../../ports/ILobbyService'
import type { Room, CreateRoomCommand } from '../../domain/room'
import { httpClient, type HttpClient } from '../api/httpClient'

const INITIAL_MOCK_ROOMS: Room[] = [
  {
    id: 'room-1',
    name: 'Taberna San Mamés [Ranked]',
    host: 'Iñaki Bilbao',
    playersCount: 3,
    maxPlayers: 4,
    pointsTarget: 40,
    mode: '8 Reyes',
    isRanked: true,
    isPrivate: false,
    avgElo: 1650,
  },
  {
    id: 'room-2',
    name: 'Mesa de Amigos de Chamberí',
    host: 'Carlos M.',
    playersCount: 2,
    maxPlayers: 4,
    pointsTarget: 30,
    mode: '4 Reyes',
    isRanked: false,
    isPrivate: true,
    avgElo: 1420,
  },
  {
    id: 'room-3',
    name: 'Veteranos del Órdago - ELO Alto',
    host: 'Don Pedro',
    playersCount: 3,
    maxPlayers: 4,
    pointsTarget: 40,
    mode: '8 Reyes',
    isRanked: true,
    isPrivate: false,
    avgElo: 1820,
  },
  {
    id: 'room-4',
    name: 'Partida Rápida para Principiantes',
    host: 'Lucía Mus',
    playersCount: 1,
    maxPlayers: 4,
    pointsTarget: 30,
    mode: '8 Reyes',
    isRanked: false,
    isPrivate: false,
    avgElo: 1250,
  },
]

export class LobbyRepository implements ILobbyService {
  private client: HttpClient
  private memoryRooms: Room[] = [...INITIAL_MOCK_ROOMS]

  constructor(client: HttpClient = httpClient) {
    this.client = client
  }

  public async getRooms(params?: LobbyQueryParams): Promise<Room[]> {
    try {
      const response = await this.client.get<Room[]>('/api/lobby/rooms', {
        filter: params?.filter,
        search: params?.search,
      })
      if (response.data && Array.isArray(response.data)) {
        return response.data
      }
    } catch {
      // Fallback to local memory rooms
    }

    let rooms = [...this.memoryRooms]

    if (params?.search && params.search.trim()) {
      const q = params.search.toLowerCase().trim()
      rooms = rooms.filter(
        (r) => r.name.toLowerCase().includes(q) || r.host.toLowerCase().includes(q),
      )
    }

    if (params?.filter === 'ranked') {
      rooms = rooms.filter((r) => r.isRanked)
    } else if (params?.filter === 'casual') {
      rooms = rooms.filter((r) => !r.isRanked)
    }

    return rooms
  }

  public async getRoomById(id: string): Promise<Room> {
    try {
      const response = await this.client.get<Room>(`/api/lobby/rooms/${encodeURIComponent(id)}`)
      if (response.data) return response.data
    } catch {
      // Fallback
    }

    const room = this.memoryRooms.find((r) => r.id === id)
    if (!room) {
      throw new Error(`Room with ID ${id} not found`)
    }
    return room
  }

  public async createRoom(command: CreateRoomCommand): Promise<Room> {
    try {
      const response = await this.client.post<Room>('/api/lobby/rooms', command)
      if (response.data) {
        this.memoryRooms.unshift(response.data)
        return response.data
      }
    } catch {
      // Fallback
    }

    const newRoom: Room = {
      id: `room-${Date.now()}`,
      name: command.name || 'Mesa Sin Nombre',
      host: 'Jorge (Tú)',
      playersCount: 1,
      maxPlayers: 4,
      pointsTarget: command.pointsTarget,
      mode: command.mode,
      isRanked: command.isRanked,
      isPrivate: command.isPrivate ?? false,
      avgElo: 1645,
      createdAt: new Date().toISOString(),
    }

    this.memoryRooms.unshift(newRoom)
    return newRoom
  }

  public async joinRoom(roomId: string, seat?: string): Promise<void> {
    try {
      await this.client.post(`/api/lobby/rooms/${encodeURIComponent(roomId)}/join`, { seat })
    } catch {
      // Mock join update
      const room = this.memoryRooms.find((r) => r.id === roomId)
      if (room && room.playersCount < room.maxPlayers) {
        room.playersCount += 1
      }
    }
  }
}

export const lobbyRepository = new LobbyRepository()
