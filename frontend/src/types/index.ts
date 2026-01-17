// User 타입
export interface User {
  id: string;
  email?: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  status: 'ONLINE' | 'OFFLINE' | 'AWAY';
  oauthProvider?: string;
}

// Auth 응답 타입
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

// ChatRoom 타입
export interface ChatRoom {
  id: string;
  name: string;
  description?: string;
  type: 'DIRECT' | 'GROUP';
  memberCount: number;
  createdAt: string;
  updatedAt: string;
}

// Message 타입
export interface Message {
  id: string;
  roomId: string;
  sender: {
    id: string;
    displayName: string;
    avatarUrl?: string;
  };
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM';
  createdAt: string;
}

// WebSocket 메시지 타입
export type MessageType = 'CHAT' | 'JOIN' | 'LEAVE' | 'TYPING' | 'USER_JOINED' | 'USER_LEFT' | 'ERROR';

export interface WebSocketMessage {
  type: MessageType;
  roomId?: string;
  messageId?: string;
  content?: string;
  sender?: {
    id: string;
    displayName: string;
    avatarUrl?: string;
  };
  createdAt?: string;
  isTyping?: boolean;
  typingUsers?: string[];
  errorCode?: string;
  errorMessage?: string;
}
