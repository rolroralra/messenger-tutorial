import { useState, useCallback, useRef, type KeyboardEvent } from 'react';
import { Send } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { useChatWebSocket } from '@/hooks/useWebSocket';
import { useChatStore } from '@/stores/useChatStore';

export function MessageInput() {
  const [message, setMessage] = useState('');
  const { sendChatMessage, sendTypingStatus, isConnected } = useChatWebSocket();
  const { currentRoomId } = useChatStore();
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleSend = useCallback(() => {
    if (!message.trim() || !isConnected || !currentRoomId) return;

    sendChatMessage(message.trim());
    setMessage('');

    // 타이핑 상태 초기화
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = null;
    }
    sendTypingStatus(false);
  }, [message, isConnected, currentRoomId, sendChatMessage, sendTypingStatus]);

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleChange = (value: string) => {
    setMessage(value);

    // 디바운스된 타이핑 상태 전송
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    if (value.length > 0) {
      sendTypingStatus(true);
      // 2초 후 타이핑 중지로 변경
      typingTimeoutRef.current = setTimeout(() => {
        sendTypingStatus(false);
      }, 2000);
    } else {
      sendTypingStatus(false);
    }
  };

  if (!currentRoomId) return null;

  return (
    <div className="border-t p-4">
      <div className="flex gap-2">
        <Textarea
          value={message}
          onChange={(e) => handleChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="메시지를 입력하세요..."
          className="min-h-[44px] max-h-[120px] resize-none"
          disabled={!isConnected}
        />
        <Button
          onClick={handleSend}
          disabled={!message.trim() || !isConnected}
          size="icon"
          className="h-11 w-11 flex-shrink-0"
        >
          <Send className="h-4 w-4" />
        </Button>
      </div>
      {!isConnected && (
        <p className="text-xs text-destructive mt-2">
          서버에 연결되지 않았습니다
        </p>
      )}
    </div>
  );
}
