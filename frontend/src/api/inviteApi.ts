import apiClient from './client';
import type { ChatRoom, RoomInviteResponse } from '@/types';

export const inviteApi = {
  create: (roomId: string): Promise<RoomInviteResponse> => {
    return apiClient.post<RoomInviteResponse>(`/rooms/${roomId}/invites`);
  },

  getByCode: (code: string): Promise<RoomInviteResponse> => {
    return apiClient.get<RoomInviteResponse>(`/invites/${code}`);
  },

  join: (code: string): Promise<ChatRoom> => {
    return apiClient.post<ChatRoom>(`/invites/${code}/join`);
  },

  delete: (code: string): Promise<void> => {
    return apiClient.delete(`/invites/${code}`);
  },
};
