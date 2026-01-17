import apiClient from './client';
import type { ChatRoom } from '@/types';

export interface CreateRoomRequest {
  name: string;
  description?: string;
  type?: 'DIRECT' | 'GROUP';
  memberIds?: string[];
}

export interface UpdateRoomRequest {
  name: string;
  description?: string;
}

export interface RoomMember {
  id: string;
  roomId: string;
  userId: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  role: string;
  joinedAt: string;
}

export const roomApi = {
  create: (data: CreateRoomRequest): Promise<ChatRoom> => {
    return apiClient.post<ChatRoom>('/rooms', data);
  },

  getAll: (): Promise<ChatRoom[]> => {
    return apiClient.get<ChatRoom[]>('/rooms');
  },

  getById: (roomId: string): Promise<ChatRoom> => {
    return apiClient.get<ChatRoom>(`/rooms/${roomId}`);
  },

  update: (roomId: string, data: UpdateRoomRequest): Promise<ChatRoom> => {
    return apiClient.put<ChatRoom>(`/rooms/${roomId}`, data);
  },

  delete: (roomId: string): Promise<void> => {
    return apiClient.delete(`/rooms/${roomId}`);
  },

  addMember: (roomId: string, userId: string): Promise<RoomMember> => {
    return apiClient.post<RoomMember>(`/rooms/${roomId}/members?userId=${userId}`);
  },

  removeMember: (roomId: string, userId: string): Promise<void> => {
    return apiClient.delete(`/rooms/${roomId}/members/${userId}`);
  },

  getMembers: (roomId: string): Promise<RoomMember[]> => {
    return apiClient.get<RoomMember[]>(`/rooms/${roomId}/members`);
  },
};
