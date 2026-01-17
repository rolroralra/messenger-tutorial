# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.3.0] - 2026-01-17

### Added

#### Room Invite Link (채팅방 초대 링크)
- 채팅방별 고유 초대 링크 생성 기능
  - 8자리 영숫자 초대 코드 생성
  - Valkey(Redis) 기반 저장 (7일 TTL 자동 만료)
- Backend API
  - `POST /api/v1/rooms/{roomId}/invites`: 초대 코드 생성
  - `GET /api/v1/invites/{code}`: 초대 정보 조회 (공개)
  - `POST /api/v1/invites/{code}/join`: 초대 코드로 참여
  - `DELETE /api/v1/invites/{code}`: 초대 코드 삭제
- `RoomInviteService`: Valkey 기반 초대 비즈니스 로직
- `RoomInviteController`: 초대 API 엔드포인트
- `RoomInviteResponse` DTO

#### Frontend
- `InvitePage`: 초대 참여 페이지 (`/invite/:code`)
  - 채팅방 정보 표시
  - 미로그인 시 로그인 페이지 리다이렉트 (returnUrl 포함)
  - 이미 참여한 채팅방은 바로 이동
- `inviteApi`: 초대 API 클라이언트
- `ChatRoom` 헤더에 "초대 링크" 버튼 추가
  - 클릭 시 초대 링크 생성 및 클립보드 복사
  - "복사됨" 피드백 표시

### Changed

#### Backend
- `SecurityConfig`: `/api/v1/invites/{code}` GET 요청 공개 설정

### Fixed

#### Backend - Reactive Stream 중복 실행 방지
- `RoomInviteService`: `.cache()` 연산자 추가로 Mono 중복 구독 시 중복 실행 방지
- `MessageService`: `getMessages()` 메서드에 `.cache()` 추가
  - 동일 요청 ID로 두 번 처리되어 `UnsupportedOperationException: ServerHttpResponse already committed` 발생하던 문제 해결

#### Frontend
- `ChatRoom`: `useRef` 기반 즉각적인 중복 클릭 방지 (초대 링크 버튼)
- `MessageInput`: 한글 IME 조합 중 Enter 키 처리 문제 해결
  - `e.nativeEvent.isComposing` 체크 추가
  - 한글 입력 중 Enter 시 메시지가 분리되던 버그 수정 (예: "머냐 이거 버그냐" → "머냐 이거 버그냐", "냐")

---

## [0.2.0] - 2026-01-17

### Added

#### Authentication (Google OAuth 2.0 + JWT)
- Google OAuth 2.0 로그인 구현
  - `AuthController`: OAuth URL 반환, 콜백 처리, 토큰 갱신
  - `AuthService`: Google 사용자 정보 조회, 사용자 생성/조회
  - `JwtService`: Access Token / Refresh Token 생성 및 검증
- 최초 로그인 시 자동 사용자 등록 (users 테이블에 OAuth 정보 저장)
- JWT 기반 API 인증
  - `JwtAuthenticationFilter`: 요청별 토큰 검증
  - `@AuthenticationPrincipal User user`: 컨트롤러에서 인증된 사용자 주입
- Spring Security WebFlux 설정
  - Public: `/api/v1/auth/**`, `/api/v1/avatar/**`, `/ws/**`
  - Protected: `/api/v1/**` (인증 필요)

#### Frontend Auth
- `useAuthStore`: 인증 상태 관리 (Zustand + persist)
- `LoginPage`: Google 로그인 버튼
- `AuthCallbackPage`: OAuth 콜백 처리 및 토큰 저장
- API 클라이언트 `Authorization: Bearer {token}` 헤더 자동 추가
- 401 응답 시 자동 로그아웃 및 로그인 페이지 리다이렉트
- 로그아웃 버튼 (Sidebar)

#### Avatar Image Proxy
- `AvatarService`: Google 프로필 이미지 다운로드 및 Valkey 캐싱
  - Base64 인코딩으로 Valkey에 저장
  - 24시간 TTL 캐시
- `AvatarController`: `GET /api/v1/avatar/{userId}` 엔드포인트
- 프론트엔드 `getAvatarUrl(userId)` 유틸리티 함수
- Google 이미지 서버 429 에러 방지

#### UX Improvements
- 로그인 시 자동 WebSocket 연결 (`shouldConnect` 기본값 true)
- Avatar 이미지 로드 실패 시 fallback 표시 및 재요청 방지

### Changed

#### Backend
- `ChatRoomController`: `@RequestHeader("X-User-Id")` → `@AuthenticationPrincipal User user`
- `MessageController`: `@RequestHeader("X-User-Id")` → `@AuthenticationPrincipal User user`
- `ChatWebSocketHandler`: `?userId=` → `?token=` JWT 토큰 인증 방식
- `schema.sql`: OAuth 관련 컬럼 추가 (email, oauth_provider, oauth_id)

#### Frontend
- Avatar 컴포넌트: Google URL → 서버 프록시 URL
- WebSocket 연결: `?userId=` → `?token=` JWT 토큰 방식

### Security
- JWT 토큰 기반 stateless 인증
- Access Token: 1시간, Refresh Token: 7일
- CSRF 비활성화 (stateless API)

---

## [0.1.0] - 2026-01-17

### Added

#### Backend
- Spring WebFlux 기반 실시간 채팅 서버 구현
- WebSocket 핸들러 (`ChatWebSocketHandler`)로 실시간 메시징 처리
  - CHAT, JOIN, LEAVE, TYPING 메시지 타입 지원
  - 채팅방별 세션 관리 및 메시지 브로드캐스트
- R2DBC 기반 리액티브 데이터 접근 계층
  - User, ChatRoom, RoomMember, Message 엔티티
  - 각 엔티티별 Repository 구현
- REST API 컨트롤러
  - UserController: 사용자 CRUD, 상태 변경
  - ChatRoomController: 채팅방 CRUD, 멤버 관리
  - MessageController: 메시지 전송, 조회 (커서 기반 페이지네이션)
- Docker Compose 설정 (PostgreSQL 16, Valkey 8)
- `DatabaseConfig`: R2DBC 스키마 자동 초기화
- `data.sql`: 테스트용 초기 데이터 (사용자, 채팅방)
- 단위 테스트 (UserService, ChatRoomService, MessageService)

#### Frontend
- React + TypeScript + Vite 프로젝트 구성
- Tailwind CSS v4 + Shadcn UI 컴포넌트 적용
- Zustand 기반 상태 관리 (`useChatStore`)
- WebSocket 훅 (`useChatWebSocket`)
  - 수동 연결/해제 지원
  - 메시지 송수신 처리
- 채팅 UI 컴포넌트
  - `Sidebar`: 사용자 프로필, 채팅방 목록, 연결 토글 버튼
  - `ChatRoom`: 채팅방 헤더, 메시지 목록, 입력창
  - `MessageList`: React Virtuoso 기반 가상화 목록
  - `MessageInput`: 메시지 입력 및 타이핑 상태 전송
  - `CreateRoomModal`: 채팅방 생성 다이얼로그
- API 클라이언트 모듈 (userApi, roomApi, messageApi)

### Fixed

#### Backend
- WebSocket 핸들러 import 충돌 해결 (`WebSocketMessage` 클래스명 중복)
- `ReactiveRedisTemplate` 빈 중복 문제 해결 (`@Primary` 추가)
- WebSocket 연결 즉시 종료 문제 해결 (`Mono.zip` → `Mono.when`)

#### Frontend
- WebSocket 연결 공유 설정 (`share: true`)
- 타이핑 상태 디바운스 적용 (과도한 메시지 전송 방지)
- `useEffect` 의존성 배열 최적화 (불필요한 재실행 방지)
- 메시지 중복 렌더링 방지 (ID 기반 중복 체크)
- TypeScript 타입 오류 수정 (`NodeJS.Timeout` → `ReturnType<typeof setTimeout>`)

### Changed

#### Frontend
- WebSocket 자동 재연결 비활성화
- 수동 연결 토글 버튼 추가 (Sidebar 사용자 프로필 영역)
