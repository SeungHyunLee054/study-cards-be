# Study Cards API Server

SM-2 알고리즘 기반 간격 반복 학습 플래시카드 서비스의 백엔드 API 서버입니다.

> **Frontend Repository**: [study-cards-fe](https://github.com/SeungHyunLee054/study-cards-fe)

## 주요 기능

- **간격 반복 학습**: SM-2 알고리즘 기반 EF Factor, 복습 간격 자동 계산
- **플래시카드 관리**: 공용 카드(Card) 및 사용자 정의 카드(UserCard) 지원
- **AI 카드 생성**: AI 기반 플래시카드 자동 생성 (구독 플랜별 생성 한도 관리)
- **AI 추천 학습**: AI 기반 개인화 학습 카드 추천 (PRO 플랜 전용)
- **북마크**: 공용 카드 및 사용자 카드 북마크 관리
- **카테고리 관리**: 계층형 카테고리 구조 (parent-child)
- **학습 통계 및 대시보드**: 학습 기록, 통계, 카테고리별 정확도, 대시보드 제공
- **구독 및 결제**: Toss Payments 기반 구독 결제 및 자동 갱신
- **이메일 인증**: 회원가입 시 이메일 인증 코드 검증
- **푸시 알림**: FCM 기반 일일 복습 알림, 구독/결제/스트릭 알림
- **사용자 인증**: JWT 기반 인증 + OAuth2 소셜 로그인 (Google, Kakao, Naver)
- **Rate Limiting**: 인증 엔드포인트 요청 제한 (5회/5분)

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL |
| Cache | Redis (캐시, JWT 블랙리스트, Rate Limiting, 분산 락) |
| Authentication | Spring Security + JWT + OAuth2 (Google, Kakao, Naver) |
| ORM | Spring Data JPA + QueryDSL |
| AI | Spring AI (Google Gemini) |
| Payment | Toss Payments |
| Push Notification | Firebase Cloud Messaging (FCM) |
| Mail | Spring Mail (이메일 인증) |
| Documentation | SpringDoc OpenAPI (Swagger) |
| Test | JUnit 5, Testcontainers, Fixture Monkey |

## 환경 설정

### 요구사항

- Java 17+
- PostgreSQL 15+
- Redis 7+
- Gradle 8.x
- Docker (통합 테스트용 Testcontainers)

### 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `DB_HOST` | PostgreSQL 호스트 | O |
| `DB_PORT` | PostgreSQL 포트 | O |
| `DB_NAME` | 데이터베이스명 | O |
| `DB_USER` | DB 사용자명 | O |
| `DB_PASSWORD` | DB 비밀번호 | O |
| `REDIS_HOST` | Redis 호스트 | O |
| `REDIS_PORT` | Redis 포트 | O |
| `REDIS_PASSWORD` | Redis 비밀번호 | O |
| `APP_JWT_SECRET` | JWT 서명 키 (최소 32바이트) | O |
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID | O |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret | O |
| `KAKAO_CLIENT_ID` | Kakao OAuth2 Client ID | O |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 Client Secret | O |
| `NAVER_CLIENT_ID` | Naver OAuth2 Client ID | O |
| `NAVER_CLIENT_SECRET` | Naver OAuth2 Client Secret | O |
| `GEMINI_API_KEY` | Google Gemini API 키 | O |
| `TOSS_CLIENT_KEY` | Toss Payments Client Key | O |
| `TOSS_SECRET_KEY` | Toss Payments Secret Key | O |
| `TOSS_WEBHOOK_SECRET` | Toss Webhook 검증 키 | X |
| `MAIL_HOST` | SMTP 서버 호스트 | O |
| `MAIL_PORT` | SMTP 서버 포트 | O |
| `MAIL_USERNAME` | SMTP 사용자명 | O |
| `MAIL_PASSWORD` | SMTP 비밀번호 | O |
| `OAUTH2_REDIRECT_URI` | OAuth2 리다이렉트 URI | O |
| `COOKIE_DOMAIN` | 쿠키 도메인 | O |
| `FCM_CREDENTIALS_PATH` | Firebase 서비스 계정 키 경로 | X |

### 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

## API 문서

Swagger UI: `/swagger-ui.html`

상세 API 명세: [docs/API_SPECIFICATION.md](docs/API_SPECIFICATION.md)

### 주요 API

| API | 설명 |
|-----|------|
| Auth | 회원가입, 로그인, 토큰 갱신, OAuth2, 이메일 인증 |
| User | 사용자 정보 관리 |
| Card | 공용 카드 CRUD, 검색 |
| UserCard | 사용자 정의 카드 CRUD |
| Category | 카테고리 관리 |
| Bookmark | 카드 북마크 관리 |
| Study | 학습 세션, SM-2 평가, 추천 카드, 카테고리별 정확도 |
| Stats | 학습 통계 조회 |
| Dashboard | 대시보드 데이터 |
| Subscription | 구독 플랜 관리 (FREE / PRO) |
| Payment | Toss Payments 결제, 빌링키 자동 갱신, 결제 내역 |
| AI | 사용자 AI 카드 생성, 생성 한도 조회 |
| Generation | 관리자 AI 문제 생성 |
| Notification | 푸시 알림 설정 및 조회 |

## 아키텍처

Light DDD(Light Domain Driven Design) 아키텍처 기반

```
com.example.study_cards/
├── application/           # Controller, Service, DTO
│   ├── ai/                # AI 카드 생성
│   ├── auth/              # 인증
│   ├── bookmark/          # 북마크
│   ├── card/              # 공용 카드
│   ├── category/          # 카테고리
│   ├── dashboard/         # 대시보드
│   ├── generation/        # 관리자 AI 생성
│   ├── notification/      # 알림
│   ├── payment/           # 결제
│   ├── stats/             # 통계
│   ├── study/             # 학습
│   ├── subscription/      # 구독
│   ├── user/              # 사용자
│   └── usercard/          # 사용자 카드
├── domain/                # Entity, Repository, Domain Service
│   ├── ai/                # AI 도메인
│   ├── bookmark/          # 북마크 도메인
│   ├── card/              # 카드 도메인
│   ├── category/          # 카테고리 도메인
│   ├── common/            # 공통 도메인 (BaseEntity 등)
│   ├── generation/        # AI 생성 도메인
│   ├── notification/      # 알림 도메인
│   ├── payment/           # 결제 도메인
│   ├── study/             # 학습 도메인
│   ├── subscription/      # 구독 도메인
│   ├── user/              # 사용자 도메인
│   └── usercard/          # 사용자 카드 도메인
├── infra/                 # 외부 시스템 연동
│   ├── ai/                # Google Gemini 연동
│   ├── fcm/               # Firebase Cloud Messaging
│   ├── mail/              # 이메일 발송
│   ├── payment/           # Toss Payments 연동
│   ├── redis/             # Redis 캐시/락
│   └── security/          # JWT, OAuth2, 필터
├── common/                # Exception, Response, Util
└── config/                # Configuration
```

## 라이선스

이 프로젝트는 [AGPL-3.0](LICENSE) 라이선스 하에 배포됩니다.
