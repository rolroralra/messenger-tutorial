# Web Messenger PRD (Product Requirements Document)

## 1. 프로젝트 개요

### 1.1 프로젝트 명
**Web Messenger** - 실시간 웹 기반 메신저 서비스

### 1.2 목표
Spring WebFlux와 React를 활용한 실시간 채팅 애플리케이션 구축

### 1.3 핵심 기능
- 다중 사용자 그룹 채팅방 지원
- 실시간 메시지 송수신
- 사용자 온라인 상태 표시
- 타이핑 인디케이터
- 메시지 히스토리 조회

---

## 2. 기술 스택

### 2.1 백엔드
| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21+ | 메인 언어 |
| Spring Boot | 3.2+ | 프레임워크 |
| Spring WebFlux | - | 리액티브 웹 서버 |
| R2DBC | - | 리액티브 DB 접근 |
| PostgreSQL | 16 | 메인 데이터베이스 |
| Valkey (Redis 호환) | 7.2 | Pub/Sub, 캐싱, 세션 |
| Docker Compose | - | 개발 환경 컨테이너 |

### 2.2 프론트엔드
| 기술 | 용도 |
|------|------|
| React 18 | UI 프레임워크 |
| TypeScript | 타입 안정성 |
| Tailwind CSS | 스타일링 |
| Shadcn UI | UI 컴포넌트 |
| Zustand | 클라이언트 상태 관리 |
| TanStack Query | 서버 상태 관리 |
| React Virtuoso | 메시지 목록 가상화 |
| react-use-websocket | WebSocket 통신 |

---

## 3. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        클라이언트 계층                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │           React + TypeScript + Tailwind CSS              │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────┐    │   │
│  │  │ Zustand │ │ TanStack│ │ Shadcn  │ │    React    │    │   │
│  │  │ (State) │ │  Query  │ │   UI    │ │   Virtuoso  │    │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                    │ HTTP (REST)    │ WebSocket                 │
└────────────────────┼────────────────┼───────────────────────────┘
                     ▼                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        서버 계층                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Spring WebFlux (Java 21+)                   │   │
│  │  ┌────────────────┐  ┌──────────────────────────────┐   │   │
│  │  │  REST API      │  │   WebSocket Handler           │   │   │
│  │  │  Controller    │  │   (실시간 메시지)              │   │   │
│  │  └────────────────┘  └──────────────────────────────┘   │   │
│  │  ┌────────────────────────────────────────────────────┐ │   │
│  │  │                  Service Layer                      │ │   │
│  │  │  UserService │ ChatRoomService │ MessageService     │ │   │
│  │  └────────────────────────────────────────────────────┘ │   │
│  │  ┌────────────────────────────────────────────────────┐ │   │
│  │  │               Repository Layer (R2DBC)              │ │   │
│  │  └────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                     │                │
                     ▼                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        데이터 계층                               │
│  ┌────────────────────┐      ┌────────────────────────────┐    │
│  │    PostgreSQL      │      │      Valkey (Redis)        │    │
│  │  ┌──────────────┐  │      │  ┌──────────────────────┐  │    │
│  │  │ users        │  │      │  │ Pub/Sub              │  │    │
│  │  │ chat_rooms   │  │      │  │ (메시지 브로드캐스트)  │  │    │
│  │  │ room_members │  │      │  ├──────────────────────┤  │    │
│  │  │ messages     │  │      │  │ 세션 캐시            │  │    │
│  │  └──────────────┘  │      │  │ 온라인 상태          │  │    │
│  └────────────────────┘      └────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 데이터베이스 스키마

### 4.1 PostgreSQL

```sql
-- 사용자 테이블
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(50) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    status          VARCHAR(20) DEFAULT 'OFFLINE',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 채팅방 테이블
CREATE TABLE chat_rooms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    type            VARCHAR(20) DEFAULT 'GROUP',
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 채팅방 멤버 테이블
CREATE TABLE room_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(20) DEFAULT 'MEMBER',
    joined_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_read_at    TIMESTAMP WITH TIME ZONE,
    UNIQUE(room_id, user_id)
);

-- 메시지 테이블
CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    sender_id       UUID NOT NULL REFERENCES users(id),
    content         TEXT NOT NULL,
    message_type    VARCHAR(20) DEFAULT 'TEXT',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at      TIMESTAMP WITH TIME ZONE
);
```

### 4.2 Valkey (Redis) 데이터 구조

```
# Pub/Sub 채널 (실시간 메시지)
Channel: chat:room:{roomId}

# 사용자 온라인 상태
Key: user:status:{userId}
Value: { status, lastSeen }

# 채팅방 세션 매핑
Key: room:sessions:{roomId}
Type: Set

# 타이핑 상태 (TTL: 3초)
Key: room:typing:{roomId}
Type: Set
```

---

## 5. API 설계

### 5.1 REST API

```
Base URL: /api/v1

=== 사용자 ===
POST   /users              # 회원가입
GET    /users/me           # 내 정보
PUT    /users/me           # 정보 수정
GET    /users/search       # 사용자 검색

=== 채팅방 ===
POST   /rooms              # 채팅방 생성
GET    /rooms              # 내 채팅방 목록
GET    /rooms/{id}         # 채팅방 정보
PUT    /rooms/{id}         # 채팅방 수정
DELETE /rooms/{id}         # 채팅방 삭제

POST   /rooms/{id}/members         # 멤버 초대
DELETE /rooms/{id}/members/{userId} # 멤버 퇴장
GET    /rooms/{id}/members         # 멤버 목록

=== 메시지 ===
GET    /rooms/{id}/messages        # 메시지 히스토리
DELETE /messages/{id}              # 메시지 삭제
```

### 5.2 WebSocket 프로토콜

```
Endpoint: ws://localhost:8080/ws/chat

=== 메시지 타입 ===

# 채팅 메시지
{ "type": "CHAT", "roomId": "uuid", "content": "Hello!" }

# 채팅방 입장/퇴장
{ "type": "JOIN", "roomId": "uuid" }
{ "type": "LEAVE", "roomId": "uuid" }

# 타이핑 상태
{ "type": "TYPING", "roomId": "uuid", "isTyping": true }
```

---

## 6. 프로젝트 구조

### 6.1 백엔드

```
backend/
├── src/main/java/com/messenger/
│   ├── MessengerApplication.java
│   ├── config/
│   │   ├── WebSocketConfig.java
│   │   ├── R2dbcConfig.java
│   │   └── RedisConfig.java
│   ├── websocket/
│   │   ├── ChatWebSocketHandler.java
│   │   └── WebSocketSessionManager.java
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── entity/
│   ├── chatroom/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── entity/
│   └── message/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       └── entity/
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql
├── compose.yaml
└── build.gradle
```

### 6.2 프론트엔드

```
frontend/
├── src/
│   ├── components/
│   │   ├── ui/              # Shadcn 컴포넌트
│   │   ├── layout/          # 레이아웃
│   │   ├── chat/            # 채팅 컴포넌트
│   │   ├── room/            # 채팅방 컴포넌트
│   │   └── user/            # 사용자 컴포넌트
│   ├── hooks/
│   │   └── useWebSocket.ts
│   ├── stores/              # Zustand
│   ├── api/                 # API 클라이언트
│   ├── queries/             # TanStack Query
│   └── types/
├── tailwind.config.js
└── package.json
```

---

## 7. 구현 단계

### Phase 1: 프로젝트 기반 구축
- [x] Spring Boot 프로젝트 생성
- [x] Docker Compose 설정 (PostgreSQL, Valkey)
- [x] Vite + React + TypeScript 프로젝트 생성
- [x] Tailwind CSS + Shadcn UI 설정

### Phase 2: 사용자 및 채팅방 기능
- [x] User CRUD API
- [x] ChatRoom CRUD API
- [x] 프론트엔드 기본 레이아웃
- [x] 채팅방 목록/생성 UI

### Phase 3: 실시간 메시징
- [x] WebSocket 설정 및 핸들러
- [ ] Redis Pub/Sub 연동
- [x] 메시지 저장 및 브로드캐스트
- [x] 프론트엔드 WebSocket 훅
- [x] 실시간 메시지 UI (React Virtuoso)

### Phase 4: 고급 기능
- [x] 메시지 페이지네이션
- [x] 타이핑 인디케이터
- [ ] 온라인 상태 표시
- [ ] 반응형 레이아웃

### Phase 5: 테스트 및 최적화
- [x] 단위/통합 테스트
- [ ] 성능 최적화
- [ ] 배포 설정

---

## 8. 핵심 설계 결정

| 항목 | 선택 | 이유 |
|------|------|------|
| 실시간 통신 | WebSocket + Handler | 브라우저 호환성, 구현 단순성 |
| DB 접근 | R2DBC | WebFlux Non-blocking 특성 활용 |
| 캐시/Pub-Sub | Valkey | 오픈소스, Redis 호환, 다중 인스턴스 확장 |
| 상태 관리 | Zustand + TanStack Query | 클라이언트/서버 상태 분리 |
| 메시지 가상화 | React Virtuoso | 대량 메시지 렌더링 성능 |

---

## 9. 비기능 요구사항

### 성능
- 메시지 전송 지연: < 100ms
- 동시 접속자: 초기 100명 지원
- 메시지 로딩: 무한 스크롤, 50개씩 페이지네이션

### 확장성
- Redis Pub/Sub으로 다중 서버 인스턴스 지원
- Stateless 설계

### 안정성
- WebSocket 자동 재연결
- 오프라인 시 메시지 DB 저장, 재연결 시 동기화
