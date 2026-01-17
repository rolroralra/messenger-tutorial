import { useEffect, useState } from 'react';
import { useChatStore } from '@/stores/useChatStore';
import { useChatWebSocket } from '@/hooks/useWebSocket';
import { messageApi } from '@/api';
import { MessageList } from './MessageList';
import { MessageInput } from './MessageInput';

export function ChatRoom() {
  const { currentRoomId, rooms, typingUsersByRoom, setMessages, connectionStatus } = useChatStore();
  const { joinRoom, leaveRoom, isConnected } = useChatWebSocket();
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);

  const currentRoom = rooms.find((room) => room.id === currentRoomId);
  const typingUsers = currentRoomId ? typingUsersByRoom[currentRoomId] || [] : [];

  // 채팅방 입장 시 메시지 로드
  useEffect(() => {
    if (!currentRoomId) return;

    const loadMessages = async () => {
      setIsLoadingMessages(true);
      try {
        const response = await messageApi.getMessages(currentRoomId);
        // API 응답을 스토어 형식으로 변환 (최신 메시지가 마지막에 오도록 역순)
        const messages = response.messages.reverse();
        setMessages(currentRoomId, messages);
      } catch (error) {
        console.error('Failed to load messages:', error);
        setMessages(currentRoomId, []);
      } finally {
        setIsLoadingMessages(false);
      }
    };

    loadMessages();
  }, [currentRoomId, setMessages]);

  // WebSocket 연결 시 채팅방 JOIN
  useEffect(() => {
    if (currentRoomId && isConnected) {
      joinRoom(currentRoomId);
      return () => {
        leaveRoom(currentRoomId);
      };
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentRoomId, isConnected]);

  return (
    <div className="flex-1 flex flex-col h-full">
      {/* 헤더 */}
      <div className="border-b px-4 py-3">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-semibold">
              {currentRoom?.name || '채팅방을 선택하세요'}
            </h2>
            {currentRoom && (
              <p className="text-sm text-muted-foreground">
                {currentRoom.memberCount}명 참여 중
              </p>
            )}
          </div>
          {connectionStatus !== 'connected' && currentRoomId && (
            <span className="text-xs text-amber-500">
              서버 연결 필요
            </span>
          )}
        </div>
      </div>

      {/* 메시지 목록 */}
      {isLoadingMessages ? (
        <div className="flex-1 flex items-center justify-center text-muted-foreground">
          메시지 불러오는 중...
        </div>
      ) : (
        <MessageList />
      )}

      {/* 타이핑 인디케이터 */}
      {typingUsers.length > 0 && (
        <div className="px-4 py-2 text-sm text-muted-foreground">
          {typingUsers.join(', ')}님이 입력 중...
        </div>
      )}

      {/* 메시지 입력 */}
      <MessageInput />
    </div>
  );
}
