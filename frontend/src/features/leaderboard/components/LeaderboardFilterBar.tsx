import React from 'react'
import { Search } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { useTranslation } from '@/i18n/useTranslation'

interface LeaderboardFilterBarProps {
  search: string
  onSearchChange: (value: string) => void
  filter: 'global' | 'grandmaster' | 'friends'
  onFilterChange: (filter: 'global' | 'grandmaster' | 'friends') => void
}

export const LeaderboardFilterBar: React.FC<LeaderboardFilterBarProps> = ({
  search,
  onSearchChange,
  filter,
  onFilterChange,
}) => {
  const { t } = useTranslation()

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
      <div className="relative w-full sm:w-80">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder={t('leaderboard.searchPlaceholder')}
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-9"
        />
      </div>

      <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
        <Button
          variant={filter === 'global' ? 'secondary' : 'ghost'}
          size="sm"
          onClick={() => onFilterChange('global')}
        >
          {t('leaderboard.filterGlobal')}
        </Button>
        <Button
          variant={filter === 'grandmaster' ? 'secondary' : 'ghost'}
          size="sm"
          onClick={() => onFilterChange('grandmaster')}
        >
          {t('leaderboard.filterGrandmaster')}
        </Button>
        <Button
          variant={filter === 'friends' ? 'secondary' : 'ghost'}
          size="sm"
          onClick={() => onFilterChange('friends')}
        >
          {t('leaderboard.filterFriends')}
        </Button>
      </div>
    </div>
  )
}
