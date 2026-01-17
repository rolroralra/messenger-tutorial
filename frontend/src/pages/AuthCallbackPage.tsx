import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import { authApi } from '@/api/auth';

export function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const handleCallback = async () => {
      // URL에서 토큰 추출 (redirect 방식)
      const accessToken = searchParams.get('accessToken');
      const refreshToken = searchParams.get('refreshToken');

      // 에러 체크
      const errorParam = searchParams.get('error');
      if (errorParam) {
        setError('로그인에 실패했습니다. 다시 시도해주세요.');
        setTimeout(() => navigate('/login'), 3000);
        return;
      }

      if (accessToken && refreshToken) {
        try {
          // 토큰으로 사용자 정보 조회
          const user = await authApi.getCurrentUser(accessToken);
          login(accessToken, refreshToken, user);
          navigate('/');
        } catch (err) {
          console.error('Failed to get user info:', err);
          setError('사용자 정보를 가져오는데 실패했습니다.');
          setTimeout(() => navigate('/login'), 3000);
        }
      } else {
        setError('인증 정보가 없습니다.');
        setTimeout(() => navigate('/login'), 3000);
      }
    };

    handleCallback();
  }, [searchParams, navigate, login]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-center">
          <p className="text-red-600 mb-2">{error}</p>
          <p className="text-gray-500">로그인 페이지로 이동합니다...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        <p className="mt-4 text-gray-600">로그인 처리 중...</p>
      </div>
    </div>
  );
}
