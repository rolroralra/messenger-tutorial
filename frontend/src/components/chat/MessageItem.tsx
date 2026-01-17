import { memo } from 'react';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { useChatStore } from '@/stores/useChatStore';
import type { Message } from '@/types';
import { cn } from '@/lib/utils';
import { getAvatarUrl } from '@/lib/avatar';

interface MessageItemProps {
  message: Message;
}

export const MessageItem = memo(function MessageItem({ message }: MessageItemProps) {
  const { currentUser } = useChatStore();
  const isOwnMessage = currentUser?.id === message.sender.id;

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div
      className={cn(
        'flex gap-3 px-4 py-2',
        isOwnMessage ? 'flex-row-reverse' : 'flex-row'
      )}
    >
      <Avatar className="h-8 w-8 flex-shrink-0">
        <AvatarImage src={getAvatarUrl(message.sender.id)} />
        <AvatarFallback>
          {message.sender.displayName.charAt(0).toUpperCase()}
        </AvatarFallback>
      </Avatar>

      <div
        className={cn(
          'flex flex-col max-w-[70%]',
          isOwnMessage ? 'items-end' : 'items-start'
        )}
      >
        {!isOwnMessage && (
          <span className="text-xs text-muted-foreground mb-1">
            {message.sender.displayName}
          </span>
        )}
        <div
          className={cn(
            'rounded-lg px-3 py-2',
            isOwnMessage
              ? 'bg-primary text-primary-foreground'
              : 'bg-muted'
          )}
        >
          <p className="text-sm whitespace-pre-wrap break-words">
            {message.content}
          </p>
        </div>
        <span className="text-xs text-muted-foreground mt-1">
          {formatTime(message.createdAt)}
        </span>
      </div>
    </div>
  );
});
