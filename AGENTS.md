# AGENTS.md

이 파일은 Codex CLI가 이 저장소의 코드를 작업할 때 참고해야 하는 가이드입니다.

---

## 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.example.study_cards.SomeTest"

# 특정 테스트 메서드 실행
./gradlew test --tests "com.example.study_cards.SomeTest.methodName"

# REST Docs 생성 (테스트 선행)
./gradlew asciidoctor
```

---

## 기술 스택

- Java 17
- Spring Boot 3.5
- PostgreSQL (메인 데이터베이스)
- Redis (캐시, TTL, JWT 블랙리스트)
- Spring Security + JWT
- Spring Data JPA + QueryDSL
- Firebase Cloud Messaging (푸시 알림)
- SpringDoc OpenAPI (Swagger)
- Lombok
- Testcontainers (통합 테스트용)
- Fixture Monkey (테스트 데이터 생성)

---

## 개발 환경

로컬 개발 시 `./gradlew bootRun` 사용

PostgreSQL과 Redis는 로컬 또는 Docker 환경에 별도로 준비되어 있어야 합니다.

---

## 테스트 환경

- 통합 테스트는 Testcontainers 사용 (Docker 필수)
- 테스트 데이터 생성은 Fixture Monkey 사용
- 엔티티, DTO, VO 자동 생성
- 테스트에서 수동 객체 생성 지양

---

## 테스트 코드 작성 규칙

### 테스트 구조

```
src/test/java/com/example/study_cards/
├── support/                          # 테스트 인프라
│   ├── BaseUnitTest.java             # 단위 테스트 베이스
│   ├── BaseIntegrationTest.java      # 통합 테스트 베이스
│   └── WithMockCustomUser.java       # Security 테스트 어노테이션
├── application/{도메인}/
│   ├── controller/
│   │   └── {도메인}ControllerTest.java
│   └── service/
│       ├── {도메인}ServiceUnitTest.java
│       └── {도메인}ServiceIntegrationTest.java
├── domain/{도메인}/
│   └── service/
│       └── {도메인}DomainServiceTest.java
└── infra/
    └── {모듈}/
        └── {클래스}Test.java 또는 {클래스}IntegrationTest.java
```

### 베이스 클래스 상속

| 테스트 유형 | 상속 클래스 | 설명 |
|------------|------------|------|
| 단위 테스트 | `BaseUnitTest` | Mockito + FixtureMonkey |
| 통합 테스트 | `BaseIntegrationTest` | Testcontainers + 트랜잭션 + FixtureMonkey |

### 네이밍 규칙

- 단위 테스트: `*UnitTest.java` 또는 `*Test.java`
- 통합 테스트: `*IntegrationTest.java`
- 컨트롤러 테스트: `*ControllerTest.java` (통합 테스트)

### 테스트 메서드 구조

```java
@Nested
@DisplayName("메서드명")
class MethodNameTest {

    @Test
    @DisplayName("한글로 테스트 설명")
    void methodName_조건_결과() {
        // given

        // when

        // then
    }
}
```

### FixtureMonkey 사용

```java
// DTO 생성
SignUpRequest request = fixtureMonkey.giveMeBuilder(SignUpRequest.class)
        .set("email", "test@example.com")
        .set("password", "password123")
        .sample();

// 엔티티는 Builder + Reflection 사용 (protected 생성자인 경우)
User user = User.builder()
        .email(EMAIL)
        .password(PASSWORD)
        .build();
// ID 설정이 필요한 경우 Reflection 사용
```

### 컨트롤러 테스트 + REST Docs

```java
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class SomeControllerTest extends BaseIntegrationTest {

    @Test
    void someMethod_success() throws Exception {
        mockMvc.perform(post("/api/some")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("some/method",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(...),
                        responseFields(...)
                ));
    }
}
```

### 인증이 필요한 테스트

```java
// 방법 1: 실제 토큰 사용
TokenResult tokenResult = authService.signIn(signInRequest);
mockMvc.perform(post("/api/some")
        .header("Authorization", "Bearer " + tokenResult.accessToken()))

// 방법 2: @WithMockCustomUser 사용 (단위 테스트)
@Test
@WithMockCustomUser(userId = 1L, email = "test@example.com")
void someMethod_withAuth() { }
```

### 비동기/TTL 테스트

```java
// awaitility 사용
await().atMost(3, TimeUnit.SECONDS)
        .untilAsserted(() -> {
            assertThat(result).isEmpty();
        });
```

---

## 아키텍처 개요 (Light DDD)

이 프로젝트는 라이트 DDD(Light Domain Driven Design) 구조를 따릅니다.

- **application**: 도메인 로직을 조합하고 흐름을 제어
- **domain**: 순수 비즈니스 로직
- **infra**: 저장소 및 외부 시스템

---

## 프로젝트 구조

```
com.example.study_cards/
├── application/              # 애플리케이션 레이어
│   └── {도메인}/
│       ├── controller/
│       ├── dto/
│       │   ├── request/
│       │   └── response/
│       └── service/
│
├── domain/                   # 도메인 레이어
│   ├── common/
│   │   └── audit/            # BaseEntity
│   └── {도메인}/
│       ├── entity/
│       ├── repository/
│       ├── exception/        # 도메인별 예외
│       └── service/          # 도메인 서비스 (선택)
│
├── infra/                    # 인프라 레이어
│   ├── redis/
│   ├── fcm/                  # Firebase Cloud Messaging
│   └── security/
│       ├── config/
│       ├── filter/
│       └── jwt/
│
├── common/                   # 공통
│   ├── aop/
│   ├── exception/            # BaseException, ErrorCode, GlobalExceptionHandler
│   ├── response/             # CommonResponse, CommonPageResponse
│   └── util/
│
└── config/                   # 설정
    ├── JpaConfig
    ├── RedisConfig
    ├── SecurityConfig
    ├── SwaggerConfig
    └── ...
```

---

## 레이어별 책임

**application**
- Controller (HTTP 요청/응답 처리)
- Service (유스케이스, 트랜잭션 처리)
- DTO (Request/Response)

**domain**
- Entity (JPA 엔티티)
- Repository 인터페이스
- 도메인 로직, 비즈니스 규칙
- 도메인별 예외

**infra**
- Repository 구현체 (QueryDSL 등)
- Redis 접근
- Security (JWT, 필터)
- FCM (푸시 알림 발송)
- 외부 시스템 연동

**common**
- 전역 예외 처리
- 공통 응답 객체
- AOP (로깅 등)
- 유틸리티

**config**
- Spring 설정 클래스들

---

## 의존성 방향

```
application → domain
application → infra
infra → domain

domain → (의존성 없음)
common → (의존성 없음, 단 exception은 Spring 의존)
```

---

## 코딩 규칙

- Controller에서 Entity 직접 반환 금지 (DTO 사용)
- 비즈니스 로직은 domain 또는 application에만 위치
- Repository 인터페이스는 domain, 구현체는 infra
- `@Transactional`은 application 서비스에만 사용
    - 클래스 레벨: `@Transactional(readOnly = true)`
    - 메서드 레벨: DB 쓰기가 필요한 메서드만 `@Transactional`
- 성공 응답은 `ResponseEntity<T>`로 반환
- 에러 응답은 `CommonResponse`로 통일 (GlobalExceptionHandler)

---

## 예외 처리

도메인별 예외는 `ErrorCode` 인터페이스를 구현한 enum과 `BaseException`을 상속한 예외 클래스로 작성

```java
// 예시: CardErrorCode.java
public enum CardErrorCode implements ErrorCode {
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}

// 예시: CardException.java
public class CardException extends BaseException {
    private final CardErrorCode errorCode;

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

---

## 보안

- JWT 기반 인증 (Access Token + Refresh Token)
- Access Token: 15분, Authorization Header
- Refresh Token: 14일, Redis 저장
- 로그아웃 시 Access Token은 Redis 블랙리스트 등록

---

## API 문서

Swagger UI: `/swagger-ui.html`

---

## Git 커밋 규칙

### 커밋 메시지 구조

```
Type: Subject

Body
```

### Type

| Type | 설명 |
|------|------|
| Feat | 새로운 기능 추가 |
| Fix | 버그 수정 |
| Docs | 문서 수정 |
| Style | 코드 포맷팅, 세미콜론 누락 등 (코드 변경 없음) |
| Refactor | 코드 리팩토링 |
| Test | 테스트 코드 추가/수정 |
| Chore | 빌드, 패키지 매니저 설정 등 |
| Design | UI/UX 디자인 변경 |
| Comment | 주석 추가/수정 |
| Rename | 파일/폴더명 수정 또는 이동 |
| Remove | 파일 삭제 |

### Subject (제목)

- 50글자 이내
- 첫 문자는 대문자
- 끝에 마침표 및 특수문자 사용 금지
- 콜론 뒤에만 스페이스 사용

```
// bad
feat : 로그인 기능 추가.

// good
Feat: 로그인 기능 추가
```

### Body (본문)

- 한 줄 당 72자 이내
- **무엇을**, **왜** 변경했는지 설명 (어떻게 X)
- 상세히 작성

### 기타 규칙

- 커밋 메시지에 `Co-Authored-By` 추가 금지
- 기능 단위로 커밋 분리
