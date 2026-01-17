# Redis Pub/Sub 연동

## 개요
다중 서버 인스턴스 환경에서 실시간 메시지 동기화를 위한 Redis(Valkey) Pub/Sub 구현

## 배경
현재 WebSocket 메시지 브로드캐스트는 단일 서버 인스턴스 내에서만 동작합니다.
서버를 수평 확장할 경우, 다른 인스턴스에 연결된 클라이언트에게 메시지가 전달되지 않습니다.

## 작업 내용

### Backend
1. **ReactiveRedisTemplate 설정 확장**
   - `RedisConfig.java`에 Pub/Sub용 MessageListenerContainer 추가
   - 채널별 구독 관리

2. **메시지 발행 서비스 구현**
   - `RedisMessagePublisher` 클래스 생성
   - 채팅 메시지, 타이핑 상태, 입/퇴장 이벤트 발행

3. **메시지 구독 서비스 구현**
   - `RedisMessageSubscriber` 클래스 생성
   - 수신된 메시지를 해당 채팅방의 WebSocket 세션으로 브로드캐스트

4. **ChatWebSocketHandler 수정**
   - 로컬 브로드캐스트 → Redis Pub/Sub 발행으로 변경
   - Redis 구독 메시지 수신 시 클라이언트에게 전달

### 채널 구조
```
chat:room:{roomId}     # 채팅 메시지
chat:typing:{roomId}   # 타이핑 상태
chat:presence:{roomId} # 입/퇴장 알림
```

## 수락 조건
- [ ] 2개 이상의 서버 인스턴스에서 메시지가 동기화됨
- [ ] 타이핑 상태가 모든 인스턴스에 전파됨
- [ ] 사용자 입/퇴장 알림이 모든 인스턴스에 전파됨
- [ ] 단위 테스트 작성

## 기술 참고
- Spring Data Redis Reactive
- ReactiveRedisOperations
- ChannelTopic / PatternTopic

## Labels
- area: backend
- type: feature
- medium
