import type { User, AuthResponse } from '@/types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const authApi = {
  // Google OAuth URL 가져오기
  async getGoogleAuthUrl(): Promise<{ authUrl: string }> {
    const response = await fetch(`${API_BASE_URL}/auth/google`);
    if (!response.ok) {
      throw new Error('Failed to get Google auth URL');
    }
    return response.json();
  },

  // Google 콜백 처리 (code로 토큰 교환)
  async handleGoogleCallback(code: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/callback/google?code=${encodeURIComponent(code)}`);
    if (!response.ok) {
      throw new Error('Failed to authenticate with Google');
    }
    return response.json();
  },

  // 토큰 갱신
  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) {
      throw new Error('Failed to refresh token');
    }
    return response.json();
  },

  // 현재 사용자 정보 조회
  async getCurrentUser(accessToken: string): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/auth/me`, {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    });
    if (!response.ok) {
      throw new Error('Failed to get current user');
    }
    return response.json();
  },

  // 로그아웃
  async logout(accessToken: string): Promise<void> {
    await fetch(`${API_BASE_URL}/auth/logout`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
      },
    });
  },
};
