import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import { useChatStore } from '@/stores/useChatStore';
import { inviteApi } from '@/api/inviteApi';
import { Button } from '@/components/ui/button';
import type { RoomInviteResponse } from '@/types';
import { MessageSquare, Users } from 'lucide-react';

export function InvitePage() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();
  const { setCurrentRoomId, fetchRooms } = useChatStore();

  const [invite, setInvite] = useState<RoomInviteResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isJoining, setIsJoining] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchInvite = async () => {
      if (!code) {
        setError('잘못된 초대 링크입니다.');
        setIsLoading(false);
        return;
      }

      try {
        const data = await inviteApi.getByCode(code);
        setInvite(data);
      } catch (err) {
        console.error('Failed to fetch invite:', err);
        setError('초대 링크를 찾을 수 없거나 만료되었습니다.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchInvite();
  }, [code]);

  const handleJoin = async () => {
    if (!code) return;

    if (!isAuthenticated) {
      // Save the return URL and redirect to login
      const returnUrl = encodeURIComponent(window.location.pathname);
      navigate(`/login?returnUrl=${returnUrl}`);
      return;
    }

    setIsJoining(true);
    try {
      const room = await inviteApi.join(code);
      await fetchRooms();
      setCurrentRoomId(room.id);
      navigate('/');
    } catch (err) {
      console.error('Failed to join room:', err);
      setError('채팅방 참여에 실패했습니다.');
    } finally {
      setIsJoining(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">초대 정보를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="max-w-md w-full p-8 bg-white rounded-lg shadow-lg text-center">
          <div className="text-red-500 mb-4">
            <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">오류</h2>
          <p className="text-gray-600 mb-6">{error}</p>
          <Button onClick={() => navigate('/')} variant="outline">
            홈으로 돌아가기
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="max-w-md w-full p-8 bg-white rounded-lg shadow-lg">
        <div className="text-center mb-6">
          <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-8 h-8 text-primary" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">채팅방 초대</h1>
          <p className="mt-2 text-gray-600">아래 채팅방에 초대되었습니다</p>
        </div>

        {invite && (
          <div className="bg-gray-50 rounded-lg p-4 mb-6">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 bg-secondary rounded-full flex items-center justify-center">
                <Users className="w-6 h-6 text-muted-foreground" />
              </div>
              <div>
                <h2 className="font-semibold text-gray-900">{invite.roomName}</h2>
                <p className="text-sm text-gray-500">
                  초대 코드: {invite.inviteCode}
                </p>
              </div>
            </div>
          </div>
        )}

        <Button
          onClick={handleJoin}
          disabled={isJoining}
          className="w-full py-6"
        >
          {isJoining ? '참여 중...' : isAuthenticated ? '채팅방 참여하기' : '로그인하고 참여하기'}
        </Button>

        {!isAuthenticated && (
          <p className="mt-4 text-center text-sm text-gray-500">
            채팅방에 참여하려면 로그인이 필요합니다.
          </p>
        )}
      </div>
    </div>
  );
}
