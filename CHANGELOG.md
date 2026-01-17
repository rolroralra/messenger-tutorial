# Changelog

All notable changes to this project will be documented in this file.

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
