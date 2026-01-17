import { Virtuoso } from 'react-virtuoso';
import { useChatStore } from '@/stores/useChatStore';
import { MessageItem } from './MessageItem';

export function MessageList() {
  const { currentRoomId, messagesByRoom } = useChatStore();
  const messages = currentRoomId ? messagesByRoom[currentRoomId] || [] : [];

  if (!currentRoomId) {
    return (
      <div className="flex-1 flex items-center justify-center text-muted-foreground">
        채팅방을 선택해주세요
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center text-muted-foreground">
        메시지가 없습니다
      </div>
    );
  }

  return (
    <Virtuoso
      className="flex-1"
      data={messages}
      initialTopMostItemIndex={messages.length - 1}
      followOutput="smooth"
      itemContent={(_, message) => <MessageItem message={message} />}
    />
  );
}
