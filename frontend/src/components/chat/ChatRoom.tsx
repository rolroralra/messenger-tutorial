import { useEffect, useState, useRef } from 'react';
import { useChatStore } from '@/stores/useChatStore';
import { useChatWebSocket } from '@/hooks/useWebSocket';
import { messageApi } from '@/api';
import { inviteApi } from '@/api/inviteApi';
import { MessageList } from './MessageList';
import { MessageInput } from './MessageInput';
import { Button } from '@/components/ui/button';
import { Link, Check } from 'lucide-react';

export function ChatRoom() {
  const { currentRoomId, rooms, typingUsersByRoom, setMessages } = useChatStore();
  const { joinRoom, leaveRoom, isConnected } = useChatWebSocket();
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [isCopying, setIsCopying] = useState(false);
  const [isCopied, setIsCopied] = useState(false);
  const isCopyingRef = useRef(false);

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

  // 초대 링크 복사
  const handleCopyInviteLink = async () => {
    if (!currentRoomId || isCopyingRef.current) return;

    isCopyingRef.current = true;
    setIsCopying(true);
    try {
      const invite = await inviteApi.create(currentRoomId);
      await navigator.clipboard.writeText(invite.inviteUrl);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 2000);
    } catch (error) {
      console.error('Failed to create invite:', error);
    } finally {
      isCopyingRef.current = false;
      setIsCopying(false);
    }
  };

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
          <div className="flex items-center gap-2">
            {currentRoom && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleCopyInviteLink}
                disabled={isCopying}
                title="초대 링크 복사"
              >
                {isCopied ? (
                  <>
                    <Check className="h-4 w-4 mr-1" />
                    복사됨
                  </>
                ) : (
                  <>
                    <Link className="h-4 w-4 mr-1" />
                    {isCopying ? '복사 중...' : '초대 링크'}
                  </>
                )}
              </Button>
            )}
          </div>
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
