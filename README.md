# Web Messenger

Spring WebFlux와 React를 활용한 실시간 웹 메신저 애플리케이션입니다.

## 기술 스택

### Backend
- **Java 21+**
- **Spring Boot 3.5.0**
- **Spring WebFlux** - 리액티브 웹 프레임워크
- **Spring WebSocket** - 실시간 통신
- **Spring Data R2DBC** - 리액티브 데이터베이스 접근
- **Spring Docker Compose Support** - 로컬 개발 환경 자동화
- **PostgreSQL 16** - 메인 데이터베이스
- **Valkey 8** (Redis 호환) - Pub/Sub 및 캐싱

### Frontend
- **React 18** - UI 라이브러리
- **TypeScript** - 타입 안정성
- **Vite** - 빌드 도구
- **Tailwind CSS v4** - 스타일링
- **Shadcn UI** - UI 컴포넌트
- **Zustand** - 클라이언트 상태 관리
- **TanStack Query** - 서버 상태 관리
- **React Virtuoso** - 메시지 목록 가상화

## 프로젝트 구조

```
web-messenger/
├── backend/                    # Spring WebFlux 백엔드
│   ├── src/main/java/com/messenger/
│   │   ├── user/              # 사용자 도메인
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── chatroom/          # 채팅방 도메인
│   │   ├── message/           # 메시지 도메인
│   │   ├── websocket/         # WebSocket 핸들러
│   │   ├── config/            # 설정
│   │   └── exception/         # 예외 처리
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── schema.sql         # 테이블 스키마
│   │   └── data.sql           # 초기 테스트 데이터
│   ├── src/test/              # 테스트 코드
│   ├── compose.yaml           # Docker Compose (PostgreSQL, Valkey)
│   └── build.gradle
├── frontend/                   # React 프론트엔드
│   ├── src/
│   │   ├── api/               # API 클라이언트
│   │   ├── components/        # UI 컴포넌트
│   │   │   ├── chat/
│   │   │   ├── layout/
│   │   │   └── ui/            # Shadcn UI 컴포넌트
│   │   ├── hooks/             # 커스텀 훅
│   │   ├── stores/            # Zustand 스토어
│   │   ├── types/             # TypeScript 타입
│   │   └── lib/               # 유틸리티
│   ├── package.json
│   └── vite.config.ts
└── PRD.md                      # 제품 요구사항 문서
```

## 주요 기능

- **실시간 채팅**: WebSocket을 통한 실시간 메시지 송수신
- **수동 연결 제어**: 사용자가 직접 서버 연결/해제 (자동 재연결 없음)
- **다중 채팅방**: 여러 그룹 채팅방 생성 및 참여
- **메시지 히스토리**: 커서 기반 페이지네이션으로 이전 메시지 로드
- **타이핑 표시**: 상대방이 입력 중일 때 실시간 표시 (디바운스 적용)
- **온라인 상태**: 사용자 접속 상태 표시
- **메시지 가상화**: 대량의 메시지도 성능 저하 없이 렌더링

## 시작하기

### 사전 요구사항

- **Java 21+**
- **Node.js 18+**
- **Docker & Docker Compose**

### Backend 실행

```bash
# backend 디렉토리로 이동
cd backend

# 애플리케이션 실행 (Docker Compose가 자동으로 PostgreSQL, Valkey 실행)
./gradlew bootRun
```

Spring Docker Compose Support가 자동으로 `compose.yaml`을 감지하여 PostgreSQL과 Valkey 컨테이너를 시작합니다.

### Frontend 실행

```bash
# frontend 디렉토리로 이동
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

### 접속

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **WebSocket**: ws://localhost:8080/ws/chat

### 사용 방법

1. 브라우저에서 http://localhost:5173 접속
2. 좌측 상단 사용자 프로필 옆 **연결 버튼**(Wifi 아이콘) 클릭하여 서버 연결
3. 채팅방 선택 후 메시지 송수신

> 자동 재연결이 비활성화되어 있으므로, 연결이 끊어지면 수동으로 다시 연결해야 합니다.

## API 엔드포인트

### 사용자 (User)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/users` | 사용자 생성 |
| GET | `/api/users/{id}` | 사용자 조회 |
| GET | `/api/users/username/{username}` | 사용자명으로 조회 |
| PUT | `/api/users/{id}` | 사용자 정보 수정 |
| PUT | `/api/users/{id}/status` | 접속 상태 변경 |
| DELETE | `/api/users/{id}` | 사용자 삭제 |

### 채팅방 (ChatRoom)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/rooms` | 채팅방 생성 |
| GET | `/api/rooms` | 내 채팅방 목록 |
| GET | `/api/rooms/{id}` | 채팅방 상세 |
| PUT | `/api/rooms/{id}` | 채팅방 정보 수정 |
| DELETE | `/api/rooms/{id}` | 채팅방 삭제 |
| POST | `/api/rooms/{id}/members` | 멤버 추가 |
| DELETE | `/api/rooms/{id}/members/{userId}` | 멤버 제거 |
| GET | `/api/rooms/{id}/members` | 멤버 목록 |

### 메시지 (Message)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/rooms/{roomId}/messages` | 메시지 전송 |
| GET | `/api/rooms/{roomId}/messages` | 메시지 목록 (커서 페이지네이션) |
| DELETE | `/api/messages/{id}` | 메시지 삭제 |

## WebSocket 프로토콜

### 연결

```
ws://localhost:8080/ws/chat?userId={userId}
```

### 메시지 형식

**클라이언트 → 서버**
```json
{
  "type": "CHAT",
  "roomId": "550e8400-e29b-41d4-a716-446655440001",
  "content": "메시지 내용"
}
```

**서버 → 클라이언트**
```json
{
  "type": "CHAT",
  "roomId": "550e8400-e29b-41d4-a716-446655440001",
  "messageId": "message-uuid",
  "content": "메시지 내용",
  "sender": {
    "id": "user-uuid",
    "displayName": "사용자명",
    "avatarUrl": "https://..."
  },
  "createdAt": "2026-01-17T12:00:00+09:00"
}
```

### 메시지 타입

| 타입 | 방향 | 설명 |
|------|------|------|
| CHAT | 양방향 | 일반 채팅 메시지 |
| JOIN | C→S | 채팅방 입장 요청 |
| LEAVE | C→S | 채팅방 퇴장 요청 |
| TYPING | C→S | 입력 중 상태 전송 |
| USER_JOINED | S→C | 사용자 입장 알림 |
| USER_LEFT | S→C | 사용자 퇴장 알림 |
| ERROR | S→C | 에러 메시지 |

## 테스트

### Backend 테스트 실행

```bash
cd backend
./gradlew test
```

### Frontend 빌드 검증

```bash
cd frontend
npm run build
```

## 환경 설정

### Backend (`application.yml`)

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/messenger
    username: messenger
    password: messenger
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8080
```

### Docker Compose (`compose.yaml`)

- **PostgreSQL 16**: 포트 5432
- **Valkey 8**: 포트 6379

## 아키텍처

### 데이터 흐름

```
Client (React)
    ↓ HTTP/WebSocket
Backend (Spring WebFlux)
    ↓ R2DBC (Reactive)
PostgreSQL (Persistence)
    ↑
Valkey (Pub/Sub, Cache)
```

### 리액티브 스택

- **Spring WebFlux**: 논블로킹 I/O로 높은 동시 접속 처리
- **R2DBC**: 리액티브 데이터베이스 드라이버
- **Reactor**: 리액티브 스트림 구현체 (Mono, Flux)

### 실시간 메시징

1. 클라이언트가 WebSocket으로 메시지 전송
2. ChatWebSocketHandler가 메시지 수신
3. 동일 채팅방의 모든 세션에 브로드캐스트
4. (확장 시) Valkey Pub/Sub으로 다중 서버 동기화

## 개발 시 참고사항

### Tailwind CSS v4

이 프로젝트는 Tailwind CSS v4를 사용합니다. v3와 달리 `@theme` 지시어로 CSS 변수를 정의합니다.

```css
@import "tailwindcss";

@theme {
  --color-background: oklch(1 0 0);
  --color-foreground: oklch(0.145 0 0);
  /* ... */
}
```

### Shadcn UI 컴포넌트

```bash
# 새 컴포넌트 추가
npx shadcn@latest add button
```

컴포넌트는 `frontend/src/components/ui/`에 설치됩니다.

## 라이선스

MIT License
