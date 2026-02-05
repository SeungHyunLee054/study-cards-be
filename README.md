# Study Cards API Server

SM-2 알고리즘 기반 간격 반복 학습 플래시카드 서비스의 백엔드 API 서버입니다.

> **Frontend Repository**: [study-cards-fe](https://github.com/SeungHyunLee054/study-cards-fe)

## 주요 기능

- **간격 반복 학습**: SM-2 알고리즘 기반 EF Factor, 복습 간격 자동 계산
- **플래시카드 관리**: 공용 카드(Card) 및 사용자 정의 카드(UserCard) 지원
- **AI 카드 생성**: AI 기반 플래시카드 자동 생성
- **카테고리 관리**: 계층형 카테고리 구조 (parent-child)
- **학습 통계 및 대시보드**: 학습 기록, 통계, 대시보드 제공
- **구독 관리**: 구독 기반 프리미엄 기능
- **푸시 알림**: FCM 기반 일일 복습 알림
- **사용자 인증**: JWT 기반 인증 + OAuth2 소셜 로그인 지원

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL |
| Cache | Redis |
| Authentication | Spring Security + JWT + OAuth2 |
| ORM | Spring Data JPA + QueryDSL |
| Push Notification | Firebase Cloud Messaging (FCM) |
| Documentation | SpringDoc OpenAPI (Swagger) |

## 환경 설정

### 요구사항

- Java 17+
- PostgreSQL 15+
- Redis 7+
- Gradle 8.x

### 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `DB_URL` | PostgreSQL 접속 URL | O |
| `DB_USERNAME` | DB 사용자명 | O |
| `DB_PASSWORD` | DB 비밀번호 | O |
| `REDIS_HOST` | Redis 호스트 | O |
| `REDIS_PORT` | Redis 포트 | O |
| `JWT_SECRET` | JWT 서명 키 | O |
| `FCM_CREDENTIALS_PATH` | Firebase 서비스 계정 키 경로 | X |

### 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

## API 문서

Swagger UI: `/swagger-ui.html`

### 주요 API

| API | 설명 |
|-----|------|
| Auth | 회원가입, 로그인, 토큰 갱신, OAuth2 |
| User | 사용자 정보 관리 |
| Card | 공용 카드 CRUD |
| UserCard | 사용자 정의 카드 CRUD |
| Category | 카테고리 관리 |
| Study | 학습 세션, SM-2 평가 |
| Stats | 학습 통계 조회 |
| Dashboard | 대시보드 데이터 |
| Generation | AI 카드 생성 |
| Subscription | 구독 관리 |
| Notification | 푸시 알림 설정 |

## 아키텍처

Light DDD(Light Domain Driven Design) 아키텍처 기반

```
├── application/    # Controller, Service, DTO
├── domain/         # Entity, Repository, Domain Service
├── infra/          # Redis, Security, FCM
├── common/         # Exception, Response, Util
└── config/         # Configuration
```

## 라이선스

이 프로젝트는 [AGPL-3.0](LICENSE) 라이선스 하에 배포됩니다.
