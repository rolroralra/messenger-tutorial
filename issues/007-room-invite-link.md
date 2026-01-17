# 채팅방 초대 링크 기능 구현

## 개요

채팅방별 고유 초대 링크를 생성하여 공유하면, 링크를 받은 사용자가 해당 채팅방에 참여할 수 있는 기능

## 배경

현재 채팅방은 생성자만 입장할 수 있으며, 다른 사용자를 초대하는 기능이 없습니다.
초대 링크를 통해 다른 사용자가 채팅방에 쉽게 참여할 수 있도록 합니다.

## 사용자 플로우

```
1. 채팅방 소유자가 "초대 링크 복사" 버튼 클릭
2. 초대 링크 생성: http://localhost:5173/invite/{code}
3. 링크를 외부에서 공유 (카카오톡, 이메일 등)
4. 링크를 받은 사용자가 클릭
5. 로그인 상태 확인 → 미로그인 시 로그인 페이지로
6. 채팅방 정보 확인 후 "참여하기" 버튼 클릭
7. 채팅방 멤버로 추가 → 채팅방으로 이동
```

## 기술 결정

### 저장소: Valkey (Redis)

PostgreSQL 테이블 대신 Valkey를 사용하는 이유:
- **자동 만료**: TTL 설정으로 7일 후 자동 삭제
- **간단한 구조**: 별도 테이블/마이그레이션 불필요
- **일시적 데이터**: 초대 링크는 영구 저장 불필요

**저장 구조:**
```
Key: invite:{inviteCode}
Value: {roomId}
TTL: 7일
```

## 작업 내용

### Backend

#### 1. 신규 파일

| 파일 | 설명 |
|------|------|
| `chatroom/dto/RoomInviteResponse.java` | 초대 응답 DTO |
| `chatroom/service/RoomInviteService.java` | Valkey 기반 초대 비즈니스 로직 |
| `chatroom/controller/RoomInviteController.java` | 초대 API 컨트롤러 |

#### 2. DTO (`RoomInviteResponse.java`)

```java
@Data
@Builder
public class RoomInviteResponse {
    private UUID roomId;
    private String roomName;
    private String inviteCode;
    private String inviteUrl;
    private OffsetDateTime createdAt;
}
```

#### 3. Service (`RoomInviteService.java`)

| 메서드 | 기능 |
|--------|------|
| `createInvite(roomId, userId)` | 초대 코드 생성 및 Valkey 저장 |
| `getInviteByCode(code)` | Valkey에서 초대 정보 조회 |
| `joinByInviteCode(code, userId)` | 초대 코드로 채팅방 참여 |
| `deleteInvite(code)` | 초대 코드 삭제 |

**초대 코드 생성 방식:**
- 8자리 영숫자 랜덤 코드 (예: `a1b2c3d4`)
- `UUID.randomUUID().toString().replace("-", "").substring(0, 8)`

#### 4. Controller (`RoomInviteController.java`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/v1/rooms/{roomId}/invites` | 초대 코드 생성 | 필요 |
| GET | `/api/v1/invites/{code}` | 초대 정보 조회 | 불필요 |
| POST | `/api/v1/invites/{code}/join` | 초대 코드로 참여 | 필요 |
| DELETE | `/api/v1/invites/{code}` | 초대 코드 삭제 | 필요 |

#### 5. SecurityConfig 수정

```java
.pathMatchers(HttpMethod.GET, "/api/v1/invites/{code}").permitAll()  // 초대 정보는 공개
```

### Frontend

#### 1. 신규 파일

| 파일 | 설명 |
|------|------|
| `api/inviteApi.ts` | 초대 API 클라이언트 |
| `pages/InvitePage.tsx` | 초대 참여 페이지 |

#### 2. API 클라이언트 (`inviteApi.ts`)

```typescript
export const inviteApi = {
  create: (roomId: string) => api.post<RoomInviteResponse>(`/rooms/${roomId}/invites`),
  getByCode: (code: string) => api.get<RoomInviteResponse>(`/invites/${code}`),
  join: (code: string) => api.post<ChatRoom>(`/invites/${code}/join`),
  delete: (code: string) => api.delete(`/invites/${code}`),
}
```

#### 3. 타입 (`types/index.ts`)

```typescript
export interface RoomInviteResponse {
  roomId: string;
  roomName: string;
  inviteCode: string;
  inviteUrl: string;
  createdAt?: string;
}
```

#### 4. 초대 참여 페이지 (`InvitePage.tsx`)

- Route: `/invite/:code`
- 초대 정보 표시 (채팅방 이름)
- "참여하기" 버튼
- 미로그인 시 로그인 페이지로 리다이렉트 (returnUrl 포함)

#### 5. 채팅방 UI 수정 (`ChatRoom.tsx`)

- 헤더에 "초대 링크" 버튼 추가
- 클릭 시 inviteApi.create() 호출
- 클립보드에 링크 복사 + "복사됨" 피드백

#### 6. 라우팅 추가 (`App.tsx`)

```tsx
<Route path="/invite/:code" element={<InvitePage />} />
```

## 파일 변경 목록

### Backend (신규)
- `chatroom/dto/RoomInviteResponse.java`
- `chatroom/service/RoomInviteService.java`
- `chatroom/controller/RoomInviteController.java`

### Backend (수정)
- `config/SecurityConfig.java` - 초대 엔드포인트 권한 설정

### Frontend (신규)
- `api/inviteApi.ts`
- `pages/InvitePage.tsx`

### Frontend (수정)
- `types/index.ts` - RoomInviteResponse 타입 추가
- `App.tsx` - /invite/:code 라우트 추가
- `components/chat/ChatRoom.tsx` - 초대 링크 버튼 추가
- `stores/useChatStore.ts` - fetchRooms 함수 추가

## 수락 조건

- [x] 채팅방에서 "초대 링크" 버튼 클릭 시 초대 링크 생성 및 클립보드 복사
- [x] 초대 링크 접속 시 채팅방 정보 표시
- [x] 미로그인 상태에서 초대 링크 접속 시 로그인 페이지로 리다이렉트
- [x] 로그인 후 초대 링크로 돌아와 "참여하기" 클릭 시 채팅방 참여
- [x] 이미 참여한 채팅방 초대 링크 접속 시 바로 채팅방으로 이동
- [x] 잘못된 초대 코드 접속 시 에러 메시지 표시
- [x] 초대 링크 7일 후 자동 만료

## Labels
- area: backend
- area: frontend
- type: feature
- medium
