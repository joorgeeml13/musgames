import { describe, it, expect } from 'vitest'
import {
  cn,
  formatWinRate,
  getTierByElo,
  getPlayerInitials,
  validateUsername,
} from './utils'

describe('cn utility', () => {
  it('combines multiple class names', () => {
    expect(cn('btn', 'btn-primary')).toBe('btn btn-primary')
  })

  it('handles conditional class names and falsy values', () => {
    const isHidden = false
    const isActive = true
    expect(cn('btn', isHidden && 'hidden', isActive && 'active', null, undefined, false)).toBe('btn active')
  })

  it('correctly resolves Tailwind CSS conflicting classes', () => {
    expect(cn('p-4', 'p-2')).toBe('p-2')
    expect(cn('text-red-500', 'text-blue-500')).toBe('text-blue-500')
    expect(cn('bg-red-500 hover:bg-red-600', 'bg-blue-500')).toBe('hover:bg-red-600 bg-blue-500')
  })

  it('handles empty inputs or whitespace gracefully', () => {
    expect(cn()).toBe('')
    expect(cn('', undefined)).toBe('')
  })
})

describe('formatWinRate', () => {
  it('formats standard win rates correctly', () => {
    expect(formatWinRate(74, 100)).toBe('74.0%')
    expect(formatWinRate(1, 3)).toBe('33.3%')
    expect(formatWinRate(2, 3)).toBe('66.7%')
  })

  it('handles edge cases with zero or negative total games', () => {
    expect(formatWinRate(0, 0)).toBe('0.0%')
    expect(formatWinRate(5, 0)).toBe('0.0%')
    expect(formatWinRate(5, -10)).toBe('0.0%')
  })

  it('handles zero or negative wins', () => {
    expect(formatWinRate(0, 10)).toBe('0.0%')
    expect(formatWinRate(-2, 10)).toBe('0.0%')
  })

  it('clamps wins to totalGames if wins exceed totalGames', () => {
    expect(formatWinRate(15, 10)).toBe('100.0%')
  })
})

describe('getTierByElo', () => {
  it('identifies grandmaster tier at boundary and above', () => {
    expect(getTierByElo(2150)).toBe('grandmaster')
    expect(getTierByElo(2800)).toBe('grandmaster')
  })

  it('identifies master tier at boundary and range', () => {
    expect(getTierByElo(2149)).toBe('master')
    expect(getTierByElo(2000)).toBe('master')
  })

  it('identifies diamond tier at boundary and range', () => {
    expect(getTierByElo(1999)).toBe('diamond')
    expect(getTierByElo(1850)).toBe('diamond')
  })

  it('identifies platinum tier at boundary and range', () => {
    expect(getTierByElo(1849)).toBe('platinum')
    expect(getTierByElo(1600)).toBe('platinum')
  })

  it('identifies gold tier below platinum', () => {
    expect(getTierByElo(1599)).toBe('gold')
    expect(getTierByElo(1200)).toBe('gold')
    expect(getTierByElo(0)).toBe('gold')
    expect(getTierByElo(-50)).toBe('gold')
  })
})

describe('getPlayerInitials', () => {
  it('extracts initials from multi-word names', () => {
    expect(getPlayerInitials('Don Ignacio')).toBe('DI')
    expect(getPlayerInitials('Mikel San Sebastian')).toBe('MS')
  })

  it('extracts initials from underscore-separated handles', () => {
    expect(getPlayerInitials('Patxi_El_Duplex')).toBe('PE')
    expect(getPlayerInitials('Aitor_Korta')).toBe('AK')
  })

  it('handles single word names', () => {
    expect(getPlayerInitials('Patxi')).toBe('PA')
    expect(getPlayerInitials('M')).toBe('M')
  })

  it('handles edge cases such as empty strings, whitespace, or invalid values', () => {
    expect(getPlayerInitials('')).toBe('??')
    expect(getPlayerInitials('   ')).toBe('??')
    // @ts-expect-error testing runtime robustness with null
    expect(getPlayerInitials(null)).toBe('??')
    // @ts-expect-error testing runtime robustness with undefined
    expect(getPlayerInitials(undefined)).toBe('??')
  })
})

describe('validateUsername', () => {
  it('accepts valid usernames', () => {
    expect(validateUsername('patxi_99').valid).toBe(true)
    expect(validateUsername('Mus-Master').valid).toBe(true)
    expect(validateUsername('abc').valid).toBe(true) // exactly 3 chars
    expect(validateUsername('12345678901234567890').valid).toBe(true) // exactly 20 chars
  })

  it('rejects empty or whitespace-only usernames', () => {
    const emptyResult = validateUsername('')
    expect(emptyResult.valid).toBe(false)
    expect(emptyResult.error).toBe('El nombre de usuario es obligatorio')

    const spaceResult = validateUsername('   ')
    expect(spaceResult.valid).toBe(false)
    expect(spaceResult.error).toBe('El nombre de usuario es obligatorio')
  })

  it('rejects usernames that are too short (< 3 chars)', () => {
    const res1 = validateUsername('a')
    expect(res1.valid).toBe(false)
    expect(res1.error).toContain('al menos 3')

    const res2 = validateUsername('ab')
    expect(res2.valid).toBe(false)
  })

  it('rejects usernames that are too long (> 20 chars)', () => {
    const res = validateUsername('a'.repeat(21))
    expect(res.valid).toBe(false)
    expect(res.error).toContain('no puede superar los 20')
  })

  it('rejects usernames with invalid characters', () => {
    expect(validateUsername('user name').valid).toBe(false)
    expect(validateUsername('player@mus').valid).toBe(false)
    expect(validateUsername('king#1').valid).toBe(false)
    expect(validateUsername('tahur!').valid).toBe(false)
    expect(validateUsername('tahur.real').valid).toBe(false)
  })
})
