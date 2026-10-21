import React from 'react'
import { Search, SlidersHorizontal } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import type { RoomFilterType } from '@/core/domain/room'
import { useTranslation } from '@/i18n/useTranslation'

interface RoomFilterBarProps {
  searchQuery: string
  onSearchChange: (value: string) => void
  filterType: RoomFilterType
  onFilterChange: (type: RoomFilterType) => void
}

export const RoomFilterBar: React.FC<RoomFilterBarProps> = ({
  searchQuery,
  onSearchChange,
  filterType,
  onFilterChange,
}) => {
  const { t } = useTranslation()

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
      <div className="relative w-full sm:w-80">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder={t('lobby.searchPlaceholder')}
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-9"
        />
      </div>

      <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
        <SlidersHorizontal className="h-4 w-4 text-muted-foreground hidden sm:block mr-1" />
        <Button
          variant={filterType === 'all' ? 'secondary' : 'ghost'}
          size="sm"
          onClick={() => onFilterChange('all')}
        >
          {t('lobby.filterAll')}
        </Button>
        <Button
          variant={filterType === 'ranked' ? 'secondary' : 'ghost'}
          size="sm"
          onClick={() => onFilterChange('ranked')}
        >
          {t('lobby.filterRanked')}
        </Button>
        <Button
          variant={filterType === 'casual' ? 'secondary' : 'ghost'}
          size="sm"
          onClick={() => onFilterChange('casual')}
        >
          {t('lobby.filterCasual')}
        </Button>
      </div>
    </div>
  )
}
