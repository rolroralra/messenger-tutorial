import { useChatStore } from '@/stores/useChatStore';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { CreateRoomModal } from '@/components/chat/CreateRoomModal';
import { cn } from '@/lib/utils';
import { MessageSquare, Wifi, WifiOff } from 'lucide-react';

export function Sidebar() {
  const {
    currentUser,
    rooms,
    currentRoomId,
    setCurrentRoomId,
    connectionStatus,
    shouldConnect,
    setShouldConnect,
  } = useChatStore();

  const handleToggleConnection = () => {
    setShouldConnect(!shouldConnect);
  };

  return (
    <div className="w-64 border-r flex flex-col h-full bg-muted/30">
      {/* 사용자 프로필 */}
      <div className="p-4 border-b">
        <div className="flex items-center gap-3">
          <Avatar>
            <AvatarImage src={currentUser?.avatarUrl} />
            <AvatarFallback>
              {currentUser?.displayName?.charAt(0).toUpperCase() || '?'}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1 min-w-0">
            <p className="font-medium truncate">
              {currentUser?.displayName || '로그인해주세요'}
            </p>
            <p className={cn(
              'text-xs',
              connectionStatus === 'connected' ? 'text-green-500' : 'text-muted-foreground'
            )}>
              {connectionStatus === 'connected' ? '온라인' :
               connectionStatus === 'connecting' ? '연결 중...' : '오프라인'}
            </p>
          </div>
          <Button
            variant={connectionStatus === 'connected' ? 'default' : 'outline'}
            size="icon"
            onClick={handleToggleConnection}
            title={shouldConnect ? '연결 끊기' : '서버 연결'}
            className="flex-shrink-0"
          >
            {connectionStatus === 'connected' ? (
              <Wifi className="h-4 w-4" />
            ) : connectionStatus === 'connecting' ? (
              <Wifi className="h-4 w-4 animate-pulse" />
            ) : (
              <WifiOff className="h-4 w-4" />
            )}
          </Button>
        </div>
      </div>

      {/* 채팅방 목록 헤더 */}
      <div className="px-4 py-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-muted-foreground">채팅방</h3>
        <CreateRoomModal />
      </div>

      {/* 채팅방 목록 */}
      <ScrollArea className="flex-1">
        <div className="px-2 space-y-1">
          {rooms.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-4">
              채팅방이 없습니다
            </p>
          ) : (
            rooms.map((room) => (
              <button
                key={room.id}
                onClick={() => setCurrentRoomId(room.id)}
                className={cn(
                  'w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left transition-colors',
                  currentRoomId === room.id
                    ? 'bg-primary text-primary-foreground'
                    : 'hover:bg-muted'
                )}
              >
                <div className="h-8 w-8 rounded-full bg-secondary flex items-center justify-center flex-shrink-0">
                  <MessageSquare className="h-4 w-4" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium truncate text-sm">{room.name}</p>
                  <p className={cn(
                    'text-xs truncate',
                    currentRoomId === room.id
                      ? 'text-primary-foreground/70'
                      : 'text-muted-foreground'
                  )}>
                    {room.memberCount}명 참여
                  </p>
                </div>
              </button>
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  );
}
