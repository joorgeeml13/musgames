import { Spade, GitBranch, ShieldAlert, Award, Compass } from 'lucide-react'
import { useTranslation } from '@/i18n/useTranslation'

export function Footer() {
  const { t } = useTranslation()

  return (
    <footer className="border-t border-border/60 bg-card/40 backdrop-blur-md mt-auto">
      <div className="container mx-auto px-4 py-8 sm:px-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          {/* Col 1 */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-600 text-white">
                <Spade className="h-4 w-4 fill-current" />
              </div>
              <span className="font-bold text-white tracking-wide">MUS TACTICS</span>
            </div>
            <p className="text-xs text-muted-foreground leading-relaxed">
              {t('footer.about')}
            </p>
          </div>

          {/* Col 2 */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold text-white tracking-wider uppercase flex items-center gap-1.5">
              <Award className="h-3.5 w-3.5 text-amber-400" /> {t('footer.modesTitle')}
            </h4>
            <ul className="text-xs space-y-1.5 text-muted-foreground">
              <li>{t('footer.modesGrande')}</li>
              <li>{t('footer.modesChica')}</li>
              <li>{t('footer.modesPares')}</li>
              <li>{t('footer.modesJuego')}</li>
            </ul>
          </div>

          {/* Col 3 */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold text-white tracking-wider uppercase flex items-center gap-1.5">
              <ShieldAlert className="h-3.5 w-3.5 text-emerald-400" /> {t('footer.eloTitle')}
            </h4>
            <ul className="text-xs space-y-1.5 text-muted-foreground">
              <li>{t('footer.eloStones')}</li>
              <li>{t('footer.eloAntiCheat')}</li>
              <li>{t('footer.eloMatchmaking')}</li>
              <li>{t('footer.eloPrivateRooms')}</li>
            </ul>
          </div>

          {/* Col 4 */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold text-white tracking-wider uppercase flex items-center gap-1.5">
              <Compass className="h-3.5 w-3.5 text-cyan-400" /> {t('footer.engineTitle')}
            </h4>
            <p className="text-xs text-muted-foreground">
              {t('footer.engineDesc')}
            </p>
            <div className="flex items-center gap-3 pt-2 text-muted-foreground">
              <span className="text-[11px] font-mono bg-secondary/80 px-2 py-1 rounded border border-border/40">
                v1.0.0-beta
              </span>
              <span className="text-[11px] font-mono bg-emerald-500/10 text-emerald-400 px-2 py-1 rounded border border-emerald-500/20">
                {t('footer.tickrate')}
              </span>
            </div>
          </div>
        </div>

        <div className="pt-6 border-t border-border/40 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-muted-foreground">
          <span>&copy; 2026 Mus Tactics Platform. {t('footer.allRightsReserved')}</span>
          <div className="flex items-center gap-4">
            <a href="#" className="hover:text-white transition-colors">{t('footer.terms')}</a>
            <a href="#" className="hover:text-white transition-colors">{t('footer.officialRules')}</a>
            <a href="#" className="hover:text-white transition-colors flex items-center gap-1">
              <GitBranch className="h-3.5 w-3.5" /> {t('footer.monorepoCode')}
            </a>
          </div>
        </div>
      </div>
    </footer>
  )
}
