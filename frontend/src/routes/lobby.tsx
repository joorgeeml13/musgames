import { useState } from 'react'
import { createRoute } from '@tanstack/react-router'
import { rootRoute } from './__root'
import { TableScene } from '@/features/table3d'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Plus, Play } from 'lucide-react'
import { useTranslation } from '@/i18n/useTranslation'
import { useLobby } from '@/core/application'
import {
  MatchmakingBanner,
  RoomCard,
  RoomFilterBar,
  CreateRoomModal,
} from '@/features/lobby'

export const lobbyRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/lobby',
  component: LobbyPage,
})

function LobbyPage() {
  const { t } = useTranslation()
  const [showCreateModal, setShowCreateModal] = useState(false)

  const {
    rooms,
    loading,
    filterType,
    setFilterType,
    searchQuery,
    setSearchQuery,
    isSearchingMatch,
    searchSeconds,
    toggleMatchmaking,
    createRoom,
    joinRoom,
  } = useLobby()

  return (
    <div className="container mx-auto px-4 sm:px-6 py-8 space-y-8">
      {/* HEADER SECTION */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-border/50 pb-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-3xl font-black text-white">{t('lobby.title')}</h1>
            <Badge variant="live" className="text-xs">
              {t('lobby.activeTables')}
            </Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            {t('lobby.description')}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="gold"
            className="gap-2 font-bold shadow-lg shadow-amber-950/40"
            onClick={() => setShowCreateModal(true)}
          >
            <Plus className="h-4 w-4" />
            {t('lobby.createRoomBtn')}
          </Button>
        </div>
      </div>

      {/* QUICK MATCHMAKING QUEUE HERO CARD */}
      <MatchmakingBanner
        isSearching={isSearchingMatch}
        searchSeconds={searchSeconds}
        onToggleSearch={toggleMatchmaking}
      />

      {/* MODAL / FORM TO CREATE ROOM */}
      <CreateRoomModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreate={async (cmd) => {
          await createRoom(cmd)
          alert(t('lobby.tableCreatedAlert'))
        }}
      />

      {/* TABLE BROWSER & FILTERS */}
      <div className="space-y-4">
        <RoomFilterBar
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          filterType={filterType}
          onFilterChange={setFilterType}
        />

        {/* ROOM LIST */}
        {loading ? (
          <div className="text-center py-12 text-muted-foreground">
            {t('common.loading')}
          </div>
        ) : rooms.length === 0 ? (
          <div className="text-center py-12 text-muted-foreground border border-dashed border-border/60 rounded-xl">
            No se han encontrado salas con los filtros actuales.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {rooms.map((room) => (
              <RoomCard
                key={room.id}
                room={room}
                onJoin={(r) => {
                  joinRoom(r.id)
                  alert(`Unido a la mesa: ${r.name}`)
                }}
              />
            ))}
          </div>
        )}
      </div>

      {/* 3D TABLE INTEGRATION CONTAINER PREVIEW */}
      <div className="pt-8 space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-black text-white flex items-center gap-2">
              <Play className="h-4 w-4 text-emerald-400 fill-current" />
              {t('lobby.previewTitle')}
            </h3>
            <p className="text-xs text-muted-foreground">
              {t('lobby.previewSubtitle')}
            </p>
          </div>
          <Badge variant="outline" className="text-xs">
            {t('lobby.assignedSeat')}
          </Badge>
        </div>
        <TableScene />
      </div>
    </div>
  )
}
