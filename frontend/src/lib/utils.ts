import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatWinRate(wins: number, totalGames: number): string {
  if (totalGames <= 0 || wins <= 0) {
    return "0.0%"
  }
  const safeWins = Math.min(wins, totalGames)
  const rate = (safeWins / totalGames) * 100
  return `${rate.toFixed(1)}%`
}

export type PlayerTier = 'grandmaster' | 'master' | 'diamond' | 'platinum' | 'gold'

export function getTierByElo(elo: number): PlayerTier {
  if (elo >= 2150) return 'grandmaster'
  if (elo >= 2000) return 'master'
  if (elo >= 1850) return 'diamond'
  if (elo >= 1600) return 'platinum'
  return 'gold'
}

export function getPlayerInitials(name: string): string {
  if (!name || !name.trim()) {
    return "??"
  }
  const cleaned = name.trim()
  const parts = cleaned.split(/[\s_]+/).filter(Boolean)
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase()
  }
  return cleaned.substring(0, 2).toUpperCase()
}

export interface ValidationResult {
  valid: boolean
  error?: string
}

export function validateUsername(username: string): ValidationResult {
  if (!username || !username.trim()) {
    return { valid: false, error: "El nombre de usuario es obligatorio" }
  }
  const trimmed = username.trim()
  if (trimmed.length < 3) {
    return { valid: false, error: "El nombre debe tener al menos 3 caracteres" }
  }
  if (trimmed.length > 20) {
    return { valid: false, error: "El nombre no puede superar los 20 caracteres" }
  }
  const validRegex = /^[a-zA-Z0-9_-]+$/
  if (!validRegex.test(trimmed)) {
    return { valid: false, error: "Solo se permiten caracteres alfanuméricos, guiones y guiones bajos" }
  }
  return { valid: true }
}
