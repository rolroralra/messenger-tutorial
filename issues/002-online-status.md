# 온라인 상태 실시간 표시

## 개요
사용자의 온라인/오프라인 상태를 실시간으로 표시하고 동기화

## 배경
현재 User 엔티티에 `status` 필드가 있지만, 실시간으로 상태가 업데이트되지 않습니다.
WebSocket 연결/해제 시 자동으로 상태가 변경되고 다른 사용자에게 알림이 전달되어야 합니다.

## 작업 내용

### Backend
1. **WebSocket 연결 시 온라인 상태 업데이트**
   - `ChatWebSocketHandler`에서 연결 시 User status를 ONLINE으로 변경
   - Redis에 온라인 사용자 Set 저장 (`online:users`)

2. **WebSocket 해제 시 오프라인 상태 업데이트**
   - 연결 종료 시 User status를 OFFLINE으로 변경
   - `lastSeen` 타임스탬프 저장

3. **상태 변경 브로드캐스트**
   - 사용자 상태 변경 시 해당 사용자가 속한 채팅방 멤버들에게 알림
   - WebSocket 메시지 타입: `USER_STATUS`

4. **온라인 사용자 목록 API**
   - `GET /api/v1/rooms/{roomId}/online-members`

### Frontend
1. **useChatStore 확장**
   - `onlineUsers: Record<string, boolean>` 상태 추가
   - `USER_STATUS` 메시지 처리

2. **UI 업데이트**
   - 채팅방 멤버 목록에 온라인 상태 표시 (녹색 점)
   - Sidebar 사용자 아바타에 상태 표시

### Redis 데이터 구조
```
Key: online:users
Type: Set
Value: [userId1, userId2, ...]

Key: user:lastseen:{userId}
Type: String
Value: ISO timestamp
TTL: 7 days
```

## 수락 조건
- [ ] WebSocket 연결 시 자동으로 온라인 상태로 변경
- [ ] WebSocket 해제 시 자동으로 오프라인 상태로 변경
- [ ] 같은 채팅방 멤버에게 상태 변경 알림 전송
- [ ] UI에 온라인/오프라인 상태 표시
- [ ] 단위 테스트 작성

## Labels
- area: backend
- area: frontend
- type: feature
- medium
