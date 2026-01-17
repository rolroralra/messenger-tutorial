# Web Messenger - Claude Code Instructions

이 문서는 Claude Code가 프로젝트 작업 시 따라야 하는 규칙과 컨벤션을 정의합니다.

## Git Branch Convention

### 브랜치 네이밍

| 타입 | 형식 | 예시 |
|------|------|------|
| 기능 개발 | `feature/{issue-number}` | `feature/6`, `feature/7` |
| 버그 수정 | `fix/{issue-number}` | `fix/12` |
| 핫픽스 | `hotfix/{issue-number}` | `hotfix/15` |
| 리팩토링 | `refactor/{issue-number}` | `refactor/20` |
| 문서 | `docs/{issue-number}` | `docs/25` |

### 브랜치 생성 예시

```bash
# GitHub Issue #6에 대한 기능 브랜치
git checkout -b feature/6
```

## Commit Convention

### 커밋 메시지 형식

```
<type>: <subject>

<body>

<footer>
```

### Type

| Type | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `style` | 코드 포맷팅 (기능 변화 없음) |
| `refactor` | 리팩토링 |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드, 설정 변경 |

### Footer

- `Closes #<issue-number>` - 관련 이슈 자동 닫기
- `Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>` - Claude 협업 표시

### 예시

```
feat: Implement Google OAuth 2.0 authentication

- Add Spring Security with OAuth2 Client
- Implement JWT token generation and validation
- Create login page with Google button

Closes #6

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

## Issue 관리

### Issue 파일 위치

모든 실행 계획은 `issues/` 디렉토리에 마크다운 파일로 작성합니다.

```
issues/
├── 001-redis-pubsub.md
├── 002-online-status.md
├── 006-google-oauth.md
└── ...
```

### Issue 파일 명명 규칙

`{번호}-{간략한-설명}.md`

## 프로젝트 구조

### Backend (Spring WebFlux)

```
backend/src/main/java/com/messenger/
├── auth/          # 인증 (OAuth, JWT)
├── config/        # 설정 클래스
├── user/          # 사용자 도메인
├── chatroom/      # 채팅방 도메인
├── message/       # 메시지 도메인
└── websocket/     # WebSocket 처리
```

### Frontend (React + TypeScript)

```
frontend/src/
├── api/           # API 클라이언트
├── components/    # UI 컴포넌트
├── hooks/         # 커스텀 훅
├── pages/         # 페이지 컴포넌트
├── stores/        # Zustand 스토어
└── types/         # TypeScript 타입
```

## 코드 스타일

### Java

- Lombok 사용 (`@Data`, `@Builder`, `@RequiredArgsConstructor`)
- R2DBC 리액티브 패턴 (`Mono<T>`, `Flux<T>`)

### TypeScript/React

- 함수형 컴포넌트
- Zustand 상태 관리
- TanStack Query (React Query)

## 환경 설정

### 필수 환경변수

```bash
# Google OAuth
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/auth/oauth2/callback/google

# JWT
JWT_SECRET=your-secret-key
```

## PR 체크리스트

- [ ] 관련 Issue 번호 연결 (`Closes #N`)
- [ ] 빌드 성공 확인 (`./gradlew build`, `npm run build`)
- [ ] 코드 리뷰 요청
