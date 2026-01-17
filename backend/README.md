# Web Messenger Backend

Spring WebFlux 기반의 리액티브 실시간 채팅 서버입니다.

## 기술 스택

| 기술 | 버전 | 설명 |
|------|------|------|
| Java | 21+ | LTS 버전 |
| Spring Boot | 3.5.0 | 프레임워크 |
| Spring WebFlux | - | 리액티브 웹 |
| Spring Security | - | 인증/인가 |
| Spring Data R2DBC | - | 리액티브 DB 접근 |
| Spring Data Redis Reactive | - | 리액티브 Redis |
| PostgreSQL | 16 | 메인 데이터베이스 |
| Valkey | 8 | Redis 호환 캐시/Pub-Sub |
| JWT (jjwt) | 0.12.6 | 토큰 기반 인증 |
| Lombok | - | 보일러플레이트 제거 |
| Testcontainers | - | 통합 테스트 |

## 프로젝트 구조

```
backend/
├── src/main/java/com/messenger/
│   ├── MessengerApplication.java       # 메인 애플리케이션
│   ├── config/                         # 설정 클래스
│   │   ├── CorsConfig.java            # CORS 설정
│   │   ├── DatabaseConfig.java        # R2DBC 스키마 초기화
│   │   ├── RedisConfig.java           # Redis 템플릿 설정
│   │   ├── SecurityConfig.java        # Spring Security 설정
│   │   └── WebSocketConfig.java       # WebSocket 핸들러 매핑
│   ├── auth/                          # 인증 도메인
│   │   ├── controller/AuthController.java
│   │   ├── dto/
│   │   ├── filter/JwtAuthenticationFilter.java
│   │   └── service/
│   │       ├── AuthService.java       # OAuth 인증 로직
│   │       └── JwtService.java        # JWT 토큰 처리
│   ├── common/exception/              # 공통 예외 처리
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── user/                          # 사용자 도메인
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   ├── chatroom/                      # 채팅방 도메인
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   ├── message/                       # 메시지 도메인
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   └── websocket/                     # WebSocket 처리
│       ├── ChatWebSocketHandler.java  # 메인 핸들러
│       └── dto/
│           ├── MessageType.java       # 메시지 타입 enum
│           └── WebSocketMessage.java  # WebSocket DTO
├── src/main/resources/
│   ├── application.yml                # 애플리케이션 설정
│   ├── schema.sql                     # 테이블 스키마
│   └── data.sql                       # 초기 테스트 데이터
├── src/test/                          # 테스트 코드
├── compose.yaml                       # Docker Compose (PostgreSQL, Valkey)
└── build.gradle                       # Gradle 빌드 설정
```

## 시작하기

### 사전 요구사항

- Java 21+
- Docker & Docker Compose

### 실행

```bash
# 애플리케이션 실행 (Docker Compose 자동 시작)
./gradlew bootRun
```

Spring Docker Compose Support가 자동으로 `compose.yaml`을 감지하여 PostgreSQL과 Valkey 컨테이너를 시작합니다.

### 테스트

```bash
./gradlew test
```

Testcontainers를 사용하여 실제 PostgreSQL 컨테이너에서 테스트가 실행됩니다.

## API 엔드포인트

Base URL: `http://localhost:8080/api/v1`

### 인증 (Auth)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/auth/google` | Google OAuth URL 반환 |
| GET | `/auth/oauth2/callback/google` | Google OAuth 콜백 (리다이렉트) |
| GET | `/auth/callback/google?code={code}` | Google OAuth 콜백 (JSON) |
| POST | `/auth/refresh` | Access Token 갱신 |
| GET | `/auth/me` | 현재 로그인 사용자 정보 |
| POST | `/auth/logout` | 로그아웃 |

> **인증 흐름**
> 1. `/auth/google` 호출 → Google 로그인 URL 반환
> 2. 사용자가 Google 로그인 완료 → `/auth/oauth2/callback/google`로 리다이렉트
> 3. 서버에서 JWT 토큰 발급 → 프론트엔드로 리다이렉트
> 4. 이후 API 호출 시 `Authorization: Bearer {token}` 헤더 사용

### 사용자 (User)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/users` | 사용자 생성 |
| GET | `/users/{id}` | 사용자 조회 |
| GET | `/users/username/{username}` | 사용자명으로 조회 |
| PUT | `/users/{id}` | 사용자 정보 수정 |
| PUT | `/users/{id}/status` | 접속 상태 변경 |
| DELETE | `/users/{id}` | 사용자 삭제 |

### 채팅방 (ChatRoom)

> 모든 요청에 `Authorization: Bearer {token}` 헤더 필요

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/rooms` | 채팅방 생성 |
| GET | `/rooms` | 내 채팅방 목록 |
| GET | `/rooms/{id}` | 채팅방 상세 |
| PUT | `/rooms/{id}` | 채팅방 정보 수정 |
| DELETE | `/rooms/{id}` | 채팅방 삭제 |
| POST | `/rooms/{id}/members?userId={userId}` | 멤버 추가 |
| DELETE | `/rooms/{id}/members/{userId}` | 멤버 제거 |
| GET | `/rooms/{id}/members` | 멤버 목록 |

### 메시지 (Message)

> 모든 요청에 `Authorization: Bearer {token}` 헤더 필요

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/rooms/{roomId}/messages` | 메시지 전송 |
| GET | `/rooms/{roomId}/messages` | 메시지 목록 (커서 페이지네이션) |
| DELETE | `/messages/{id}` | 메시지 삭제 |

#### 메시지 목록 쿼리 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `cursor` | String | null | 페이지네이션 커서 (메시지 ID) |
| `limit` | Integer | 50 | 조회 개수 (최대 100) |

## WebSocket

### 연결

```
ws://localhost:8080/ws/chat?token={accessToken}
```

> JWT Access Token을 쿼리 파라미터로 전달합니다.

### 메시지 타입

| 타입 | 방향 | 설명 |
|------|------|------|
| `CHAT` | 양방향 | 일반 채팅 메시지 |
| `JOIN` | C→S | 채팅방 입장 |
| `LEAVE` | C→S | 채팅방 퇴장 |
| `TYPING` | C→S | 입력 중 상태 |
| `USER_JOINED` | S→C | 사용자 입장 알림 |
| `USER_LEFT` | S→C | 사용자 퇴장 알림 |
| `ERROR` | S→C | 에러 메시지 |

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

## 데이터베이스 스키마

### ERD

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   users     │     │ room_members │     │ chat_rooms  │
├─────────────┤     ├──────────────┤     ├─────────────┤
│ id (PK)     │←────│ user_id (FK) │     │ id (PK)     │
│ username    │     │ room_id (FK) │────→│ name        │
│ display_name│     │ role         │     │ description │
│ avatar_url  │     │ joined_at    │     │ type        │
│ status      │     │ last_read_at │     │ created_by  │←┐
│ created_at  │     └──────────────┘     │ created_at  │ │
│ updated_at  │                          └─────────────┘ │
└─────────────┘                                          │
       │                                                 │
       │         ┌─────────────┐                         │
       │         │  messages   │                         │
       │         ├─────────────┤                         │
       └────────→│ sender_id   │                         │
                 │ room_id (FK)│─────────────────────────┘
                 │ content     │
                 │ message_type│
                 │ created_at  │
                 │ deleted_at  │
                 └─────────────┘
```

### 테이블

| 테이블 | 설명 |
|--------|------|
| `users` | 사용자 정보 |
| `chat_rooms` | 채팅방 정보 |
| `room_members` | 채팅방-사용자 매핑 (다대다) |
| `messages` | 메시지 내용 |

## 설정

### application.yml

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
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

server:
  port: 8080

jwt:
  secret: ${JWT_SECRET:your-secret-key}
  access-token-expiry: 3600000   # 1시간
  refresh-token-expiry: 604800000 # 7일

app:
  oauth:
    google:
      redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:8080/api/v1/auth/oauth2/callback/google}
```

### 환경변수

| 변수 | 필수 | 설명 |
|------|------|------|
| `GOOGLE_CLIENT_ID` | Yes | Google OAuth 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Yes | Google OAuth 클라이언트 시크릿 |
| `GOOGLE_REDIRECT_URI` | No | OAuth 콜백 URL (기본값: localhost) |
| `JWT_SECRET` | No | JWT 서명 키 (기본값: 내장) |

### Docker Compose 서비스

| 서비스 | 이미지 | 포트 | 설명 |
|--------|--------|------|------|
| postgres | postgres:16-alpine | 5432 | PostgreSQL 데이터베이스 |
| valkey | valkey/valkey:8-alpine | 6379 | Redis 호환 캐시 서버 |

## 아키텍처

### 리액티브 스택

```
HTTP/WebSocket Request
         │
         ▼
┌─────────────────────────────┐
│     Spring WebFlux          │  논블로킹 I/O
│  (Netty Event Loop)         │
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│      Service Layer          │  비즈니스 로직
│   (Mono<T>, Flux<T>)        │
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│    R2DBC Repository         │  리액티브 DB 접근
│   (Non-blocking SQL)        │
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│      PostgreSQL             │  데이터 저장
└─────────────────────────────┘
```

### WebSocket 메시지 흐름

```
Client A                    Server                    Client B
   │                          │                          │
   │──── CHAT message ───────→│                          │
   │                          │── Save to DB             │
   │                          │── Broadcast ────────────→│
   │                          │                          │
```

## 주요 클래스

| 클래스 | 설명 |
|--------|------|
| `ChatWebSocketHandler` | WebSocket 연결 및 메시지 처리 |
| `DatabaseConfig` | R2DBC 스키마 초기화 (schema.sql, data.sql 실행) |
| `RedisConfig` | ReactiveRedisTemplate 빈 설정 |
| `GlobalExceptionHandler` | 전역 예외 처리 (@ControllerAdvice) |

## 확장 포인트

- **Valkey Pub/Sub**: 다중 서버 환경에서 메시지 동기화
- **메시지 캐싱**: 최근 메시지 Redis 캐싱
- **읽음 확인**: `last_read_at` 필드 활용
- **파일 첨부**: `message_type` 필드로 IMAGE, FILE 지원 가능
