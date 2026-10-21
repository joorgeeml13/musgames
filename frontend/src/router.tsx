import { createRouter } from '@tanstack/react-router'
import { rootRoute } from './routes/__root'
import { indexRoute } from './routes/index'
import { lobbyRoute } from './routes/lobby'
import { leaderboardRoute } from './routes/leaderboard'

const routeTree = rootRoute.addChildren([
  indexRoute,
  lobbyRoute,
  leaderboardRoute,
])

export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
