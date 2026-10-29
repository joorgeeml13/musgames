import { createRoute } from '@tanstack/react-router'
import { rootRoute } from './__root'
import { useTranslation } from '@/i18n/useTranslation'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

export const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: HomePage,
})

function HomePage() {
  const { t } = useTranslation()

  return (
    <div className="flex-1 flex items-center justify-center p-6">
      <Card className="max-w-md w-full border-border/60 bg-card/80 backdrop-blur shadow-xl">
        <CardHeader className="text-center space-y-3">
          <div className="flex justify-center">
            <Badge variant="gold" className="text-xs px-2.5 py-0.5">
              Mus Platform
            </Badge>
          </div>
          <CardTitle className="text-2xl font-black tracking-tight text-white">
            {t('app.title')}
          </CardTitle>
          <CardDescription className="text-sm text-muted-foreground">
            {t('common.welcome')}
          </CardDescription>
        </CardHeader>
        <CardContent className="text-center text-xs text-muted-foreground/80">
          Clean Architecture & Technical Baseline
        </CardContent>
      </Card>
    </div>
  )
}
