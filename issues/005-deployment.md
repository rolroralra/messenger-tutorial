# 배포 설정

## 개요
프로덕션 환경 배포를 위한 설정 및 인프라 구성

## 작업 내용

### Docker 이미지

1. **Backend Dockerfile**
   ```dockerfile
   # Multi-stage build
   FROM eclipse-temurin:21-jdk as builder
   WORKDIR /app
   COPY . .
   RUN ./gradlew bootJar

   FROM eclipse-temurin:21-jre
   COPY --from=builder /app/build/libs/*.jar app.jar
   ENTRYPOINT ["java", "-jar", "app.jar"]
   ```

2. **Frontend Dockerfile**
   ```dockerfile
   FROM node:20-alpine as builder
   WORKDIR /app
   COPY package*.json ./
   RUN npm ci
   COPY . .
   RUN npm run build

   FROM nginx:alpine
   COPY --from=builder /app/dist /usr/share/nginx/html
   COPY nginx.conf /etc/nginx/nginx.conf
   ```

### Docker Compose (Production)

1. **compose.prod.yaml**
   - Backend 서비스
   - Frontend 서비스 (Nginx)
   - PostgreSQL (볼륨 영속화)
   - Valkey (볼륨 영속화)
   - Nginx 리버스 프록시

### 환경 설정

1. **Backend 프로파일 분리**
   - `application.yml` (공통)
   - `application-dev.yml` (개발)
   - `application-prod.yml` (프로덕션)

2. **환경 변수**
   ```
   DATABASE_URL
   DATABASE_USERNAME
   DATABASE_PASSWORD
   REDIS_HOST
   REDIS_PORT
   ```

3. **보안 설정**
   - CORS 프로덕션 도메인 설정
   - 환경 변수로 시크릿 관리

### CI/CD (GitHub Actions)

1. **Build & Test**
   - PR 시 자동 테스트
   - 빌드 검증

2. **Docker 이미지 빌드**
   - GitHub Container Registry 푸시
   - 태그 관리 (latest, 버전)

3. **배포 자동화** (선택)
   - 클라우드 배포 스크립트

## 디렉토리 구조
```
deploy/
├── docker/
│   ├── backend.Dockerfile
│   ├── frontend.Dockerfile
│   └── nginx.conf
├── compose.prod.yaml
└── .env.example
```

## 수락 조건
- [ ] Backend/Frontend Docker 이미지 빌드 성공
- [ ] docker-compose up으로 전체 스택 실행 가능
- [ ] 환경 변수로 설정 관리
- [ ] GitHub Actions CI 파이프라인 동작
- [ ] README에 배포 가이드 추가

## Labels
- area: backend
- area: frontend
- type: feature
- hard
