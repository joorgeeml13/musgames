import React, { useState } from 'react'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Plus, CheckCircle2 } from 'lucide-react'
import type { CreateRoomCommand, PointsTarget, KingsMode } from '@/core/domain/room'
import { useTranslation } from '@/i18n/useTranslation'

interface CreateRoomModalProps {
  isOpen: boolean
  onClose: () => void
  onCreate: (command: CreateRoomCommand) => void
}

export const CreateRoomModal: React.FC<CreateRoomModalProps> = ({
  isOpen,
  onClose,
  onCreate,
}) => {
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [pointsTarget, setPointsTarget] = useState<PointsTarget>(40)
  const [mode, setMode] = useState<KingsMode>('8 Reyes')
  const [isRanked, setIsRanked] = useState(true)

  if (!isOpen) return null

  const handleCreate = () => {
    onCreate({
      name: name.trim() || 'Taberna Nueva',
      pointsTarget,
      mode,
      isRanked,
      isPrivate: false,
    })
    onClose()
  }

  return (
    <Card className="border-amber-500/40 bg-slate-900/95 backdrop-blur-xl">
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <Plus className="h-5 w-5 text-amber-400" /> {t('lobby.modalTitle')}
          </CardTitle>
          <Button variant="ghost" size="sm" onClick={onClose}>
            ✕
          </Button>
        </div>
        <CardDescription>
          {t('lobby.modalDesc')}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1.5">
              {t('lobby.roomNameLabel')}
            </label>
            <Input
              placeholder={t('lobby.roomNamePlaceholder')}
              value={name}
              maxLength={50}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1.5">
              {t('lobby.pointsLabel')}
            </label>
            <div className="flex gap-2">
              <Button
                type="button"
                variant={pointsTarget === 40 ? 'default' : 'outline'}
                className="flex-1"
                onClick={() => setPointsTarget(40)}
              >
                {t('lobby.points40')}
              </Button>
              <Button
                type="button"
                variant={pointsTarget === 30 ? 'default' : 'outline'}
                className="flex-1"
                onClick={() => setPointsTarget(30)}
              >
                {t('lobby.points30')}
              </Button>
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1.5">
              {t('lobby.modeLabel')}
            </label>
            <div className="flex gap-2">
              <Button
                type="button"
                variant={mode === '8 Reyes' ? 'default' : 'outline'}
                className="flex-1"
                onClick={() => setMode('8 Reyes')}
              >
                {t('lobby.mode8Kings')}
              </Button>
              <Button
                type="button"
                variant={mode === '4 Reyes' ? 'default' : 'outline'}
                className="flex-1"
                onClick={() => setMode('4 Reyes')}
              >
                {t('lobby.mode4Kings')}
              </Button>
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1.5">
              {t('lobby.gameTypeLabel')}
            </label>
            <div className="flex gap-2">
              <Button
                type="button"
                variant={isRanked ? 'gold' : 'outline'}
                className="flex-1"
                onClick={() => setIsRanked(true)}
              >
                Ranked (ELO)
              </Button>
              <Button
                type="button"
                variant={!isRanked ? 'default' : 'outline'}
                className="flex-1"
                onClick={() => setIsRanked(false)}
              >
                Casual
              </Button>
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-3 border-t border-border/60">
          <Button variant="ghost" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button
            variant="gold"
            className="gap-2"
            onClick={handleCreate}
          >
            <CheckCircle2 className="h-4 w-4" /> {t('lobby.createAndSitBtn')}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
