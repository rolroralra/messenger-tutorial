import { useEffect } from 'react';
import { AppLayout } from '@/components/layout/AppLayout';
import { useChatStore } from '@/stores/useChatStore';
import { apiClient, roomApi } from '@/api';
import type { User } from '@/types';

// 테스트용 고정 사용자 ID (추후 로그인 기능으로 대체)
const TEST_USER: User = {
  id: '550e8400-e29b-41d4-a716-446655440000',
  username: 'testuser',
  displayName: '테스트 사용자',
  status: 'ONLINE',
};

function App() {
  const { setCurrentUser, setRooms } = useChatStore();

  useEffect(() => {
    // 사용자 설정
    setCurrentUser(TEST_USER);
    apiClient.setUserId(TEST_USER.id);

    // API에서 채팅방 목록 로드
    const loadRooms = async () => {
      try {
        const rooms = await roomApi.getAll();
        setRooms(rooms);
      } catch (error) {
        console.error('Failed to load rooms:', error);
        // API 실패 시 빈 배열 유지
        setRooms([]);
      }
    };

    loadRooms();
  }, [setCurrentUser, setRooms]);

  return <AppLayout />;
}

export default App;
