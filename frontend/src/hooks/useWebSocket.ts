import { useCallback, useEffect } from 'react';
import useWebSocket, { ReadyState } from 'react-use-websocket';
import { useChatStore } from '@/stores/useChatStore';
import { useAuthStore } from '@/stores/useAuthStore';
import type { WebSocketMessage, Message } from '@/types';

const WS_URL = 'ws://localhost:8080/ws/chat';

export function useChatWebSocket() {
  const {
    currentRoomId,
    addMessage,
    setTypingUsers,
    setConnectionStatus,
    shouldConnect,
  } = useChatStore();

  const { accessToken } = useAuthStore();

  // shouldConnect가 true이고 accessToken이 있을 때만 연결
  const socketUrl = shouldConnect && accessToken
    ? `${WS_URL}?token=${accessToken}`
    : null;

  const { sendJsonMessage, lastJsonMessage, readyState } = useWebSocket(
    socketUrl,
    {
      share: true,
      shouldReconnect: () => false, // 자동 재연결 비활성화
    }
  );

  // 연결 상태 업데이트
  useEffect(() => {
    const status = {
      [ReadyState.CONNECTING]: 'connecting',
      [ReadyState.OPEN]: 'connected',
      [ReadyState.CLOSING]: 'disconnected',
      [ReadyState.CLOSED]: 'disconnected',
      [ReadyState.UNINSTANTIATED]: 'disconnected',
    }[readyState] as 'connecting' | 'connected' | 'disconnected';

    setConnectionStatus(status);
  }, [readyState, setConnectionStatus]);

  // 메시지 수신 처리
  useEffect(() => {
    if (!lastJsonMessage) return;

    const wsMessage = lastJsonMessage as WebSocketMessage;

    switch (wsMessage.type) {
      case 'CHAT':
        if (wsMessage.roomId && wsMessage.sender) {
          const message: Message = {
            id: wsMessage.messageId || crypto.randomUUID(),
            roomId: wsMessage.roomId,
            sender: wsMessage.sender,
            content: wsMessage.content || '',
            messageType: 'TEXT',
            createdAt: wsMessage.createdAt || new Date().toISOString(),
          };
          addMessage(wsMessage.roomId, message);
        }
        break;

      case 'TYPING':
        if (wsMessage.roomId && wsMessage.typingUsers) {
          setTypingUsers(wsMessage.roomId, wsMessage.typingUsers);
        }
        break;

      case 'USER_JOINED':
      case 'USER_LEFT':
        // 사용자 입장/퇴장 알림 처리
        console.log('User event:', wsMessage.type, wsMessage.sender);
        break;

      case 'ERROR':
        console.error('WebSocket error:', wsMessage.errorMessage);
        break;
    }
  }, [lastJsonMessage, addMessage, setTypingUsers]);

  // 채팅 메시지 전송
  const sendChatMessage = useCallback(
    (content: string) => {
      if (!currentRoomId || readyState !== ReadyState.OPEN) return;

      const message: WebSocketMessage = {
        type: 'CHAT',
        roomId: currentRoomId,
        content,
      };
      sendJsonMessage(message);
    },
    [currentRoomId, readyState, sendJsonMessage]
  );

  // 채팅방 입장
  const joinRoom = useCallback(
    (roomId: string) => {
      if (readyState !== ReadyState.OPEN) return;

      const message: WebSocketMessage = {
        type: 'JOIN',
        roomId,
      };
      sendJsonMessage(message);
    },
    [readyState, sendJsonMessage]
  );

  // 채팅방 퇴장
  const leaveRoom = useCallback(
    (roomId: string) => {
      if (readyState !== ReadyState.OPEN) return;

      const message: WebSocketMessage = {
        type: 'LEAVE',
        roomId,
      };
      sendJsonMessage(message);
    },
    [readyState, sendJsonMessage]
  );

  // 타이핑 상태 전송
  const sendTypingStatus = useCallback(
    (isTyping: boolean) => {
      if (!currentRoomId || readyState !== ReadyState.OPEN) return;

      const message: WebSocketMessage = {
        type: 'TYPING',
        roomId: currentRoomId,
        isTyping,
      };
      sendJsonMessage(message);
    },
    [currentRoomId, readyState, sendJsonMessage]
  );

  return {
    sendChatMessage,
    joinRoom,
    leaveRoom,
    sendTypingStatus,
    isConnected: readyState === ReadyState.OPEN,
  };
}
