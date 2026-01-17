# Google OAuth 2.0 로그인 구현

## 개요
Google OAuth 2.0을 통한 사용자 등록 및 로그인 기능 구현

## 배경
현재 시스템은 하드코딩된 테스트 사용자(`X-User-Id` 헤더)를 사용하여 인증 없이 동작합니다.
실제 사용자 인증을 위해 Google OAuth 2.0 기반 로그인 시스템을 구현합니다.

## 작업 내용

### Backend

#### 1. 의존성 추가 (`build.gradle`)
```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
```

#### 2. 데이터베이스 스키마 변경 (`schema.sql`)
```sql
-- users 테이블에 OAuth 필드 추가
ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN oauth_provider VARCHAR(20);
ALTER TABLE users ADD COLUMN oauth_id VARCHAR(255);
ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(500);

CREATE UNIQUE INDEX idx_users_oauth ON users(oauth_provider, oauth_id);
```

#### 3. 엔티티/DTO 변경
- `User.java`: email, oauthProvider, oauthId, profileImageUrl 필드 추가
- `UserRepository.java`: `findByEmail()`, `findByOauthProviderAndOauthId()` 메서드 추가

#### 4. 인증 관련 신규 파일
| 파일 | 설명 |
|------|------|
| `config/SecurityConfig.java` | Spring Security WebFlux 설정 |
| `config/JwtProperties.java` | JWT 설정 프로퍼티 |
| `auth/controller/AuthController.java` | 인증 API 엔드포인트 |
| `auth/service/AuthService.java` | OAuth 인증 비즈니스 로직 |
| `auth/service/JwtService.java` | JWT 토큰 생성/검증 |
| `auth/filter/JwtAuthenticationFilter.java` | 요청별 토큰 검증 필터 |
| `auth/dto/AuthResponse.java` | 인증 응답 DTO |
| `auth/dto/GoogleUserInfo.java` | Google 사용자 정보 DTO |

#### 5. 설정 추가 (`application.yml`)
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, profile, email

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 3600000  # 1시간
  refresh-token-expiry: 604800000  # 7일
```

#### 6. API 엔드포인트
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/auth/google` | Google OAuth URL 반환 |
| GET | `/api/v1/auth/callback/google` | OAuth 콜백 처리, JWT 발급 |
| POST | `/api/v1/auth/refresh` | Access Token 갱신 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| GET | `/api/v1/auth/me` | 현재 사용자 정보 |

#### 7. WebSocket 인증 변경
- `ChatWebSocketHandler.java`: `?userId=` → `?token=` JWT 토큰으로 변경
- 토큰 검증 후 사용자 ID 추출

### Frontend

#### 1. 의존성 추가 (`package.json`)
```json
"react-router-dom": "^7.x"
```

#### 2. 인증 상태 관리 (`stores/useAuthStore.ts`)
```typescript
interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  login: (tokens: AuthTokens, user: User) => void;
  logout: () => void;
}
```

#### 3. 신규 컴포넌트/페이지
| 파일 | 설명 |
|------|------|
| `pages/LoginPage.tsx` | 로그인 페이지 |
| `components/auth/GoogleLoginButton.tsx` | Google 로그인 버튼 |
| `components/auth/PrivateRoute.tsx` | 인증 필요 라우트 |
| `hooks/useAuth.ts` | 인증 관련 훅 |

#### 4. API 클라이언트 수정 (`client.ts`)
- `X-User-Id` 헤더 → `Authorization: Bearer <token>` 헤더
- 401 응답 시 토큰 갱신 또는 로그인 페이지 리다이렉트
- 토큰 자동 갱신 인터셉터

#### 5. 라우팅 (`App.tsx`)
```tsx
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route path="/auth/callback" element={<OAuthCallback />} />
  <Route path="/" element={<PrivateRoute><AppLayout /></PrivateRoute>} />
</Routes>
```

#### 6. 타입 수정 (`types/index.ts`)
```typescript
interface User {
  id: string;
  email: string;
  username?: string;
  displayName: string;
  avatarUrl?: string;
  oauthProvider: 'GOOGLE';
  status: 'ONLINE' | 'OFFLINE' | 'AWAY';
}
```

## OAuth 플로우

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│ Browser │     │ Frontend│     │ Backend │     │ Google  │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ Click Login   │               │               │
     │──────────────>│               │               │
     │               │ GET /auth/google              │
     │               │──────────────>│               │
     │               │ OAuth URL     │               │
     │               │<──────────────│               │
     │ Redirect      │               │               │
     │<──────────────│               │               │
     │               │               │               │
     │ Login with Google             │               │
     │───────────────────────────────────────────────>
     │               │               │               │
     │ Redirect with code            │               │
     │<──────────────────────────────────────────────│
     │               │               │               │
     │ /auth/callback?code=xxx       │               │
     │──────────────>│               │               │
     │               │ GET /auth/callback/google?code=xxx
     │               │──────────────>│               │
     │               │               │ Exchange code │
     │               │               │──────────────>│
     │               │               │ User info     │
     │               │               │<──────────────│
     │               │               │               │
     │               │ JWT Tokens    │               │
     │               │<──────────────│               │
     │ Store tokens  │               │               │
     │<──────────────│               │               │
     │               │               │               │
     │ Redirect to / │               │               │
     │<──────────────│               │               │
```

## 수락 조건

- [ ] Google 로그인 버튼 클릭 시 Google 로그인 페이지로 이동
- [ ] Google 로그인 완료 후 앱으로 리다이렉트 및 JWT 발급
- [ ] JWT를 사용한 API 인증 동작
- [ ] 새로고침 시 로그인 상태 유지 (localStorage)
- [ ] 토큰 만료 시 자동 갱신 또는 로그아웃
- [ ] WebSocket 연결 시 JWT 토큰 검증
- [ ] 로그아웃 기능 동작
- [ ] 미인증 사용자 접근 시 로그인 페이지로 리다이렉트

## 환경 변수

```bash
# Google OAuth
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret

# JWT
JWT_SECRET=your-jwt-secret-key-min-256-bits
```

## Google Cloud Console 설정

1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 새 프로젝트 생성 또는 기존 프로젝트 선택
3. APIs & Services > Credentials
4. Create Credentials > OAuth client ID
5. Application type: Web application
6. Authorized redirect URIs:
   - `http://localhost:8080/api/v1/auth/callback/google`
7. Client ID와 Client Secret 복사

## Labels
- area: backend
- area: frontend
- type: feature
- hard
