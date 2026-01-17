import apiClient from './client';
import type { Message } from '@/types';

export interface SendMessageRequest {
  content: string;
  messageType?: 'TEXT' | 'IMAGE' | 'FILE';
}

export interface MessagePageResponse {
  messages: Message[];
  nextCursor: string | null;
  hasMore: boolean;
}

export const messageApi = {
  send: (roomId: string, data: SendMessageRequest): Promise<Message> => {
    return apiClient.post<Message>(`/rooms/${roomId}/messages`, data);
  },

  getMessages: (
    roomId: string,
    cursor?: string,
    limit: number = 50
  ): Promise<MessagePageResponse> => {
    const params = new URLSearchParams();
    if (cursor) params.set('cursor', cursor);
    params.set('limit', limit.toString());

    return apiClient.get<MessagePageResponse>(
      `/rooms/${roomId}/messages?${params.toString()}`
    );
  },

  delete: (messageId: string): Promise<void> => {
    return apiClient.delete(`/messages/${messageId}`);
  },
};
