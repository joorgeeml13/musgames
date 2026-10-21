export type TablePosition = 'south' | 'north' | 'east' | 'west'
export type PlayerRole = 'you' | 'teammate' | 'rival'

export interface TablePlayer {
  id: string
  name: string
  position: TablePosition
  role: PlayerRole
  elo: number
  cardsCount: number
  avatarUrl?: string
  hasPlayed?: boolean
  currentAction?: string
}

export type MusPhase =
  | 'mus_check'
  | 'descartes'
  | 'grande'
  | 'chica'
  | 'pares'
  | 'juego'
  | 'punto'
  | 'recuento'

export interface TableState {
  id: string
  phase: MusPhase
  piedrasTanteo: {
    teamA: number
    teamB: number
  }
  targetScore: 30 | 40
  manoPlayerId: string
  players: TablePlayer[]
}

export type CardSuit = 'oros' | 'copas' | 'espadas' | 'bastos'

export interface Card {
  suit: CardSuit
  value: number // 1 to 12
  name: string
}
