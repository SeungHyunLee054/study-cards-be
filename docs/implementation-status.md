# 구현 현황

프로젝트의 구현 진행 상황을 추적하는 문서입니다.

---

## 핵심 기능 체크리스트

| 상태 | 기능 | 설명 |
|:---:|------|------|
| :white_check_mark: | 카드 CRUD API | 생성, 조회, 수정, 삭제 완료 |
| :white_check_mark: | 비로그인 Rate Limiting | IP 기반 15개/일 카드 제한 (자정 초기화) |
| :white_check_mark: | JWT 인증 | Access Token + Refresh Token + 블랙리스트 |
| :white_check_mark: | 학습 플로우 | SM-2 기반 스페이싱 반복 학습 구현 완료 |
| :white_check_mark: | Anki SM-2 알고리즘 | efFactor 업데이트 + interval 계산 구현 완료 |
| :white_check_mark: | 학습 통계 | 정답률, 마스터리율, 스트릭, 일별 학습 현황 |
| :x: | 일일 추천 푸시 | 미구현 |

---

## API 현황

### Auth API (/api/auth)

| 상태 | 엔드포인트 | 설명 |
|:---:|------------|------|
| :white_check_mark: | POST /signup | 회원가입 |
| :white_check_mark: | POST /signin | 로그인 (JWT 발급) |
| :white_check_mark: | POST /signout | 로그아웃 (토큰 블랙리스트) |
| :white_check_mark: | POST /refresh | Access Token 갱신 |

### Card API (/api/cards)

| 상태 | 엔드포인트 | 설명 |
|:---:|------------|------|
| :white_check_mark: | GET / | 전체 카드 조회 (카테고리 필터 지원) |
| :white_check_mark: | GET /{id} | 단일 카드 조회 |
| :white_check_mark: | GET /study | 학습용 카드 조회 (Rate Limiting 적용) |
| :white_check_mark: | POST / | 카드 생성 |
| :white_check_mark: | PUT /{id} | 카드 수정 |
| :white_check_mark: | DELETE /{id} | 카드 삭제 |

### Study API (/api/study)

| 상태 | 엔드포인트 | 설명 |
|:---:|------------|------|
| :white_check_mark: | GET /cards | 오늘 학습할 카드 조회 (인증 필요) |
| :white_check_mark: | POST /answer | 답변 제출 및 SM-2 알고리즘 적용 (인증 필요) |

### Stats API (/api/stats)

| 상태 | 엔드포인트 | 설명 |
|:---:|------------|------|
| :white_check_mark: | GET / | 학습 통계 조회 (인증 필요) |

---

## 도메인 모델 현황

### Card

```
- id: Long
- questionEn: String (영문 질문)
- questionKo: String (한글 질문)
- answerEn: String (영문 답변)
- answerKo: String (한글 답변)
- efFactor: Double (난이도 팩터, 기본값 2.5)
- category: Category (CS, ENGLISH, SQL, JAPANESE)

메서드:
- updateEfFactor(boolean isCorrect): SM-2 공식으로 efFactor 업데이트
- update(...): 카드 정보 수정
```

### StudySession

```
- id: Long
- user: User
- startedAt: LocalDateTime
- endedAt: LocalDateTime
- totalCards: Integer
- correctCount: Integer

메서드:
- endSession(): 세션 종료 (endedAt 설정)
- incrementTotalCards(): 총 카드 수 증가
- incrementCorrectCount(): 정답 수 증가
```

### StudyRecord

```
- id: Long
- user: User
- card: Card
- session: StudySession
- studiedAt: LocalDateTime
- isCorrect: Boolean
- nextReviewDate: LocalDate
- repetitionCount: Integer
- interval: Integer (복습 간격, 일 단위)

메서드:
- updateForReview(Boolean isCorrect, LocalDate newNextReviewDate, Integer newInterval): 복습 정보 업데이트
```

---

## SM-2 알고리즘 구현

### efFactor 업데이트 공식

```java
int quality = isCorrect ? 4 : 2;
double delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
efFactor = Math.max(efFactor + delta, 1.3);
```

- 정답 (quality=4): delta = 0 → efFactor 유지
- 오답 (quality=2): delta = -0.32 → efFactor 감소
- 최소값: 1.3

### interval 계산 로직

```
오답: interval = 1 (다음 날 복습)
첫 번째 정답 (repetition=1): interval = 1
두 번째 정답 (repetition=2): interval = 6
이후 정답: interval = prevInterval * efFactor
```

---

## 테스트 현황

### 단위 테스트

| 레이어 | 테스트 파일 | 상태 |
|--------|-------------|:---:|
| Auth | AuthServiceUnitTest | :white_check_mark: |
| Card | CardServiceUnitTest, CardDomainServiceTest, CardTest | :white_check_mark: |
| Study | StudyServiceUnitTest, StudyDomainServiceTest, StudySessionTest, StudyRecordTest | :white_check_mark: |
| Stats | StatsServiceUnitTest | :white_check_mark: |
| User | UserDomainServiceTest, UserTest | :white_check_mark: |
| Security | JwtAuthenticationFilterTest, JwtTokenProviderTest | :white_check_mark: |

### 통합 테스트

| 레이어 | 테스트 파일 | REST Docs |
|--------|-------------|:---:|
| Auth | AuthControllerTest, AuthServiceIntegrationTest | :white_check_mark: |
| Card | CardControllerTest | :white_check_mark: |
| Study | StudyControllerTest | :white_check_mark: |
| Stats | StatsControllerTest | :white_check_mark: |
| Redis | RefreshTokenServiceIntegrationTest, TokenBlacklistServiceIntegrationTest, UserCacheServiceIntegrationTest | - |

### REST Docs 스니펫

- `auth/signup`, `auth/signin`, `auth/signout`, `auth/refresh`
- `card/get-all`, `card/get-one`, `card/get-study`, `card/create`, `card/update`, `card/delete`
- `study/get-today-cards`, `study/submit-answer`
- `stats/get-stats`

---

## 다음 구현 예정

### 1. 일일 추천 푸시

- [ ] 푸시 알림 인프라 구축
- [ ] 복습 대상 카드 알림

### 2. 추가 개선

- [ ] 학습 세션 기반 통계
- [ ] 사용자 대시보드 API

---

## 완료된 기능

### 학습 통계 (2026-02-02)

- [x] 일별 학습 카드 수 (`recentActivity`)
- [x] 전체 정답률 (`overview.accuracyRate`)
- [x] 연속 학습일 - 스트릭 (`overview.streak`)
- [x] 카테고리별 마스터리율 (`deckStats[].masteryRate`)

---

## 인프라 현황

| 구성 요소 | 상태 | 비고 |
|----------|:---:|------|
| PostgreSQL | :white_check_mark: | 메인 데이터베이스 |
| Redis | :white_check_mark: | JWT 블랙리스트, Rate Limiting, Refresh Token |
| Testcontainers | :white_check_mark: | 통합 테스트 환경 |
| REST Docs | :white_check_mark: | API 문서 자동 생성 |
| Swagger | :white_check_mark: | /swagger-ui.html |

---

## 참고

- 프로젝트 개요: [docs/project.md](./project.md)
- 개발 가이드: [CLAUDE.md](../CLAUDE.md)
