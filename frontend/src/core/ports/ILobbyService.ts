import type { Room, CreateRoomCommand, RoomFilterType } from '../domain/room'

export interface LobbyQueryParams {
  filter?: RoomFilterType
  search?: string
}

export interface ILobbyService {
  getRooms(params?: LobbyQueryParams): Promise<Room[]>
  getRoomById(id: string): Promise<Room>
  createRoom(command: CreateRoomCommand): Promise<Room>
  joinRoom(roomId: string, seat?: string): Promise<void>
}
