/**
 * Mus Tactics Design Tokens
 * Cohesive single visual theme: Tavern Dark Felt Gaming
 * Deep Spanish tavern ambiance, mahogany wood, green felt, and aged amber gold.
 */

export const themeTokens = {
  colors: {
    felt: {
      deep: '#071d11',
      dark: '#0b2917',
      base: '#0f3823',
      light: '#185a37',
      border: '#246f48',
    },
    wood: {
      dark: '#140c08',
      mahogany: '#1c130e',
      border: '#2e180e',
      light: '#3d2314',
    },
    gold: {
      antique: '#b8902d',
      amber: '#d4af37',
      bright: '#f59e0b',
      glow: '#fbbf24',
    },
    tavern: {
      charcoal: '#090d13',
      slate: '#0d1117',
      card: '#121820',
      border: '#1f2937',
    },
    accent: {
      cyan: '#06b6d4',
      ruby: '#dc2626',
      emerald: '#10b981',
    },
  },
  typography: {
    fontSans: 'ui-sans-serif, system-ui, sans-serif',
    fontMono: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
  },
  radius: {
    sm: '0.375rem',
    md: '0.5rem',
    lg: '0.75rem',
    xl: '1rem',
    full: '9999px',
  },
} as const

export type ThemeTokens = typeof themeTokens
