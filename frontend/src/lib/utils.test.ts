import { describe, it, expect } from 'vitest'
import { cn } from './utils'

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
