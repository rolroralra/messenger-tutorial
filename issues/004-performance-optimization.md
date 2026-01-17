# 성능 최적화

## 개요
애플리케이션 전반의 성능 최적화

## 작업 내용

### Backend

1. **데이터베이스 쿼리 최적화**
   - N+1 문제 해결 (채팅방 멤버 조회 시)
   - 인덱스 추가 검토
   - 쿼리 실행 계획 분석

2. **커넥션 풀 튜닝**
   - R2DBC 커넥션 풀 설정
   - Redis 커넥션 풀 설정

3. **캐싱 전략**
   - 채팅방 정보 캐싱 (Redis)
   - 사용자 정보 캐싱 (Redis)
   - 캐시 무효화 전략

4. **WebSocket 최적화**
   - 메시지 배치 처리
   - 백프레셔 처리

### Frontend

1. **번들 크기 최적화**
   - 코드 스플리팅 (React.lazy)
   - 트리 쉐이킹 확인
   - 번들 분석 (rollup-plugin-visualizer)

2. **렌더링 최적화**
   - React.memo 적용
   - useMemo, useCallback 최적화
   - 불필요한 리렌더링 제거

3. **네트워크 최적화**
   - API 응답 압축 (gzip)
   - 이미지 최적화 (아바타)

4. **메시지 목록 최적화**
   - React Virtuoso 설정 튜닝
   - 스크롤 성능 개선

## 측정 지표

| 지표 | 현재 | 목표 |
|------|------|------|
| FCP (First Contentful Paint) | - | < 1.5s |
| LCP (Largest Contentful Paint) | - | < 2.5s |
| 메시지 전송 지연 | - | < 100ms |
| 번들 크기 | - | < 300KB (gzipped) |

## 수락 조건
- [ ] Lighthouse 성능 점수 90+ 달성
- [ ] 메시지 전송 지연 100ms 이하
- [ ] 1000개 메시지 스크롤 시 60fps 유지
- [ ] 번들 크기 목표 달성

## Labels
- area: backend
- area: frontend
- type: refactor
- hard
