import { create } from 'zustand';
import { roomApi } from '@/api';
import type { Message, ChatRoom, User } from '@/types';

interface ChatState {
  // 현재 사용자
  currentUser: User | null;
  setCurrentUser: (user: User | null) => void;

  // 현재 선택된 채팅방
  currentRoomId: string | null;
  setCurrentRoomId: (roomId: string | null) => void;

  // 채팅방 목록
  rooms: ChatRoom[];
  setRooms: (rooms: ChatRoom[]) => void;
  addRoom: (room: ChatRoom) => void;
  fetchRooms: () => Promise<void>;

  // 메시지 (roomId별로 관리)
  messagesByRoom: Record<string, Message[]>;
  addMessage: (roomId: string, message: Message) => void;
  setMessages: (roomId: string, messages: Message[]) => void;

  // 타이핑 상태
  typingUsersByRoom: Record<string, string[]>;
  setTypingUsers: (roomId: string, users: string[]) => void;

  // WebSocket 연결 상태
  connectionStatus: 'connecting' | 'connected' | 'disconnected';
  setConnectionStatus: (status: 'connecting' | 'connected' | 'disconnected') => void;

  // 수동 연결 제어
  shouldConnect: boolean;
  setShouldConnect: (shouldConnect: boolean) => void;
}

export const useChatStore = create<ChatState>((set) => ({
  // 현재 사용자
  currentUser: null,
  setCurrentUser: (user) => set({ currentUser: user }),

  // 현재 선택된 채팅방
  currentRoomId: null,
  setCurrentRoomId: (roomId) => set({ currentRoomId: roomId }),

  // 채팅방 목록
  rooms: [],
  setRooms: (rooms) => set({ rooms }),
  addRoom: (room) => set((state) => ({ rooms: [...state.rooms, room] })),
  fetchRooms: async () => {
    try {
      const rooms = await roomApi.getAll();
      set({ rooms });
    } catch (error) {
      console.error('Failed to fetch rooms:', error);
    }
  },

  // 메시지
  messagesByRoom: {},
  addMessage: (roomId, message) =>
    set((state) => {
      const existingMessages = state.messagesByRoom[roomId] || [];
      // 중복 메시지 체크
      if (existingMessages.some((m) => m.id === message.id)) {
        return state; // 이미 존재하면 추가하지 않음
      }
      return {
        messagesByRoom: {
          ...state.messagesByRoom,
          [roomId]: [...existingMessages, message],
        },
      };
    }),
  setMessages: (roomId, messages) =>
    set((state) => ({
      messagesByRoom: {
        ...state.messagesByRoom,
        [roomId]: messages,
      },
    })),

  // 타이핑 상태
  typingUsersByRoom: {},
  setTypingUsers: (roomId, users) =>
    set((state) => ({
      typingUsersByRoom: {
        ...state.typingUsersByRoom,
        [roomId]: users,
      },
    })),

  // WebSocket 연결 상태
  connectionStatus: 'disconnected',
  setConnectionStatus: (status) => set({ connectionStatus: status }),

  // 연결 제어 (기본값 true: 로그인 시 자동 연결)
  shouldConnect: true,
  setShouldConnect: (shouldConnect) => set({ shouldConnect }),
}));
