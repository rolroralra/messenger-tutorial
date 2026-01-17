# Web Messenger

실시간 채팅 애플리케이션 - Spring WebFlux + React

## Overview

리액티브 스택 기반의 실시간 웹 메신저입니다.

- **Backend**: Spring WebFlux, R2DBC, WebSocket
- **Frontend**: React 19, TypeScript, Zustand
- **Database**: PostgreSQL, Valkey (Redis)
- **Auth**: Google OAuth 2.0, JWT

## Features

- Google OAuth 2.0 로그인
- 실시간 채팅 (WebSocket)
- 채팅방 생성/관리
- JWT 토큰 기반 인증

## Quick Start

### Prerequisites

- Java 21+
- Node.js 20+
- Docker & Docker Compose

### Environment Setup

```bash
# Google OAuth 설정 (필수)
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
```

### Run

```bash
# Backend (Port 8080)
cd backend
./gradlew bootRun

# Frontend (Port 5173)
cd frontend
npm install
npm run dev
```

http://localhost:5173 에서 앱에 접속합니다.

## Project Structure

```
web-messenger/
├── backend/           # Spring WebFlux 서버
│   ├── src/
│   │   └── main/java/com/messenger/
│   │       ├── auth/      # OAuth, JWT 인증
│   │       ├── user/      # 사용자 관리
│   │       ├── chatroom/  # 채팅방 관리
│   │       ├── message/   # 메시지 관리
│   │       └── websocket/ # WebSocket 핸들러
│   └── compose.yaml   # PostgreSQL, Valkey
├── frontend/          # React 클라이언트
│   └── src/
│       ├── api/       # API 클라이언트
│       ├── components/# UI 컴포넌트
│       ├── pages/     # 페이지 (Login, Chat)
│       └── stores/    # Zustand 상태 관리
├── issues/            # 실행 계획 문서
└── CLAUDE.md          # 개발 컨벤션
```

## Tech Stack

### Backend

| Technology | Description |
|------------|-------------|
| Spring Boot 3.5 | Framework |
| Spring WebFlux | Reactive Web |
| Spring Security | Authentication |
| R2DBC | Reactive Database |
| PostgreSQL | Database |
| Valkey | Redis-compatible Cache |
| JWT | Token Authentication |

### Frontend

| Technology | Description |
|------------|-------------|
| React 19 | UI Library |
| TypeScript | Type Safety |
| Zustand | State Management |
| TanStack Query | Data Fetching |
| Tailwind CSS | Styling |
| shadcn/ui | UI Components |

## API Documentation

See [Backend README](./backend/README.md) for detailed API documentation.

### Authentication Flow

```
1. User clicks "Google Login"
2. Redirect to Google OAuth
3. Google callback → Server issues JWT
4. Frontend stores token
5. Subsequent requests use Authorization header
```

## Development

### Branch Convention

- `feature/{issue-number}` - 기능 개발
- `fix/{issue-number}` - 버그 수정
- `docs/{issue-number}` - 문서 작업

See [CLAUDE.md](./CLAUDE.md) for detailed conventions.

## License

MIT
