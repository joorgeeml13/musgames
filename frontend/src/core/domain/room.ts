export type KingsMode = '8 Reyes' | '4 Reyes'
export type PointsTarget = 30 | 40
export type RoomFilterType = 'all' | 'ranked' | 'casual'

export interface Room {
  id: string
  name: string
  host: string
  playersCount: number
  maxPlayers: number
  pointsTarget: PointsTarget
  mode: KingsMode
  isRanked: boolean
  isPrivate: boolean
  avgElo: number
  createdAt?: string
}

export interface CreateRoomCommand {
  name: string
  pointsTarget: PointsTarget
  mode: KingsMode
  isRanked: boolean
  isPrivate?: boolean
}
