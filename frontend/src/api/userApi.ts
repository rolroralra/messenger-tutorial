import apiClient from './client';
import type { User } from '@/types';

export interface CreateUserRequest {
  username: string;
  displayName: string;
  avatarUrl?: string;
}

export interface UpdateUserRequest {
  displayName: string;
  avatarUrl?: string;
}

export const userApi = {
  create: (data: CreateUserRequest): Promise<User> => {
    return apiClient.post<User>('/users', data);
  },

  getById: (id: string): Promise<User> => {
    return apiClient.get<User>(`/users/${id}`);
  },

  getByUsername: (username: string): Promise<User> => {
    return apiClient.get<User>(`/users/username/${username}`);
  },

  search: (query: string): Promise<User[]> => {
    return apiClient.get<User[]>(`/users/search?q=${encodeURIComponent(query)}`);
  },

  update: (id: string, data: UpdateUserRequest): Promise<User> => {
    return apiClient.put<User>(`/users/${id}`, data);
  },

  updateStatus: (id: string, status: string): Promise<User> => {
    return apiClient.patch<User>(`/users/${id}/status?status=${status}`);
  },

  delete: (id: string): Promise<void> => {
    return apiClient.delete(`/users/${id}`);
  },
};
