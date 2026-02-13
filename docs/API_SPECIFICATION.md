# Study Cards API 명세서

> Base URL: `/api`
> 인증: JWT Bearer Token (Authorization 헤더)

---

## 목차

1. [인증 (Auth)](#1-인증-auth)
2. [사용자 (User)](#2-사용자-user)
3. [카테고리 (Category)](#3-카테고리-category)
4. [카드 (Card)](#4-카드-card)
5. [사용자 카드 (User Card)](#5-사용자-카드-user-card)
6. [북마크 (Bookmark)](#6-북마크-bookmark)
7. [학습 (Study)](#7-학습-study)
8. [학습 세션 (Study Session)](#8-학습-세션-study-session)
9. [통계 (Stats)](#9-통계-stats)
10. [대시보드 (Dashboard)](#10-대시보드-dashboard)
11. [알림 (Notification)](#11-알림-notification)
12. [구독 (Subscription)](#12-구독-subscription)
13. [결제 (Payment)](#13-결제-payment)
14. [AI 카드 생성 (User AI)](#14-ai-카드-생성-user-ai)
15. [관리자 API (Admin)](#15-관리자-api-admin)
16. [관리자 AI 문제 생성 (Generation)](#16-관리자-ai-문제-생성-generation)
17. [웹훅 (Webhook)](#17-웹훅-webhook)
18. [에러 코드 (Error Codes)](#18-에러-코드-error-codes)

---

## 공통 사항

### 인증 헤더
```
Authorization: Bearer {accessToken}
```

### Rate Limiting

인증 관련 엔드포인트에는 Rate Limiting이 적용됩니다.

| 대상 엔드포인트 | 제한 | 기준 |
|---------------|------|------|
| `/api/auth/signin` | 5회 / 5분 | 이메일 기준 |
| `/api/auth/password-reset/*` | 5회 / 5분 | 이메일 기준 |
| `/api/auth/email-verification/*` | 5회 / 5분 | 이메일 기준 |

### 페이지네이션 파라미터
| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| page | Integer | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | 20 | 페이지 크기 |
| sort | String | - | 정렬 기준 (예: `createdAt,desc`) |

### 페이지네이션 응답 구조
```json
{
  "content": [...],
  "totalElements": 100,
  "page": 1,
  "size": 20,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false,
  "isFirst": true,
  "isLast": false
}
```

### 에러 응답 구조
```json
{
  "code": "ERROR_CODE",
  "message": "에러 메시지",
  "timestamp": "2024-01-01T00:00:00"
}
```

---

## 1. 인증 (Auth)

### 1.1 회원가입
```
POST /api/auth/signup
```

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "passwordConfirm": "password123",
  "nickname": "닉네임"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|-----|------|------|--------|
| email | String | O | 이메일 형식 |
| password | String | O | 8-20자 |
| passwordConfirm | String | O | password와 일치 |
| nickname | String | O | 2-20자 |

**Response** `201 Created`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "닉네임",
  "roles": ["ROLE_USER"],
  "provider": "LOCAL",
  "streak": 0
}
```

---

### 1.2 로그인
```
POST /api/auth/signin
```

> Rate Limited: 5회 / 5분 (이메일 기준)

**Request Body**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

> **Note**: Refresh Token은 HttpOnly 쿠키로 설정됨

---

### 1.3 로그아웃
```
POST /api/auth/signout
```

**Headers**
```
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

### 1.4 토큰 갱신
```
POST /api/auth/refresh
```

> Refresh Token 쿠키 필요

**Response** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

---

### 1.5 비밀번호 재설정 요청
```
POST /api/auth/password-reset/request
```

> Rate Limited: 5회 / 5분 (이메일 기준)

**Request Body**
```json
{
  "email": "user@example.com"
}
```

**Response** `200 OK`

---

### 1.6 비밀번호 재설정 확인
```
POST /api/auth/password-reset/verify
```

> Rate Limited: 5회 / 5분 (이메일 기준)

**Request Body**
```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "newpassword123"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|-----|------|------|--------|
| email | String | O | 이메일 형식 |
| code | String | O | 6자리 |
| newPassword | String | O | 8-20자 |

**Response** `204 No Content`

---

### 1.7 이메일 인증 요청
```
POST /api/auth/email-verification/request
```

> Rate Limited: 5회 / 5분 (이메일 기준)

**Request Body**
```json
{
  "email": "user@example.com"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|-----|------|------|--------|
| email | String | O | 이메일 형식 |

**Response** `200 OK`

---

### 1.8 이메일 인증 확인
```
POST /api/auth/email-verification/verify
```

> Rate Limited: 5회 / 5분 (이메일 기준)

**Request Body**
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|-----|------|------|--------|
| email | String | O | 이메일 형식 |
| code | String | O | 6자리 |

**Response** `204 No Content`

---

### 1.9 소셜 로그인 (OAuth2)

> Spring Security OAuth2가 처리

#### 로그인 시작
```
GET /oauth2/authorization/{provider}
```

| provider | 설명 |
|----------|------|
| google | Google 로그인 |
| kakao | Kakao 로그인 |
| naver | Naver 로그인 |

**Response**: 해당 OAuth2 제공자의 로그인 페이지로 리다이렉트

---

#### 콜백 처리
```
GET /login/oauth2/code/{provider}
```

> OAuth2 제공자가 인증 완료 후 호출하는 콜백 URL

**성공 시**: 프론트엔드로 리다이렉트 + access token 쿼리 파라미터
**실패 시**: 프론트엔드로 리다이렉트 + error 메시지

---

## 2. 사용자 (User)

### 2.1 내 정보 조회
```
GET /api/users/me
```

> 인증 필요

**Response** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "닉네임",
  "roles": ["ROLE_USER"],
  "provider": "LOCAL",
  "streak": 5
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| provider | String | LOCAL, GOOGLE, KAKAO, NAVER |

---

### 2.2 내 정보 수정
```
PATCH /api/users/me
```

> 인증 필요

**Request Body**
```json
{
  "nickname": "새닉네임"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|-----|------|------|--------|
| nickname | String | O | 2-20자 |

**Response** `200 OK` (UserResponse)

---

### 2.3 비밀번호 변경
```
PATCH /api/users/me/password
```

> 인증 필요

**Request Body**
```json
{
  "currentPassword": "oldpassword123",
  "newPassword": "newpassword123"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|-----|------|------|--------|
| currentPassword | String | O | - |
| newPassword | String | O | 8-20자 |

**Response** `204 No Content`

---

## 3. 카테고리 (Category)

### 3.1 카테고리 목록 조회
```
GET /api/categories
```

**Query Parameters**
| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| size | 50 | 페이지 크기 |
| sort | displayOrder,asc | 정렬 |

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "code": "CS",
      "name": "컴퓨터 과학",
      "parentId": null,
      "parentCode": null
    }
  ],
  "totalElements": 10
}
```

---

### 3.2 카테고리 트리 조회
```
GET /api/categories/tree
```

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "code": "CS",
    "name": "컴퓨터 과학",
    "depth": 0,
    "displayOrder": 1,
    "children": [
      {
        "id": 2,
        "code": "CS_ALGO",
        "name": "알고리즘",
        "depth": 1,
        "displayOrder": 1,
        "children": []
      }
    ]
  }
]
```

---

### 3.3 카테고리 상세 조회
```
GET /api/categories/{code}
```

**Response** `200 OK`
```json
{
  "id": 1,
  "code": "CS",
  "name": "컴퓨터 과학",
  "parentId": null,
  "parentCode": null
}
```

---

### 3.4 하위 카테고리 조회
```
GET /api/categories/{code}/children
```

**Response** `200 OK` (페이지네이션)

---

## 4. 카드 (Card)

### 4.1 카드 상세 조회
```
GET /api/cards/{id}
```

**Response** `200 OK`
```json
{
  "id": 1,
  "question": "질문 내용",
  "questionSub": "Question in English",
  "answer": "정답 내용",
  "answerSub": "Answer in English",
  "efFactor": 2.5,
  "category": {
    "id": 1,
    "code": "CS",
    "name": "컴퓨터 과학",
    "parentId": null,
    "parentCode": null
  },
  "cardType": "PUBLIC",
  "createdAt": "2024-01-01T00:00:00"
}
```

| cardType | 설명 |
|----------|------|
| PUBLIC | 공개 카드 |
| CUSTOM | 사용자 생성 카드 |

---

### 4.2 학습용 카드 조회 (비로그인 가능)
```
GET /api/cards/study
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| category | X | 카테고리 코드 |

**Response** `200 OK` (페이지네이션)

---

### 4.3 전체 카드 조회 (사용자 카드 포함)
```
GET /api/cards/all
```

> 인증 필요

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| category | X | 카테고리 코드 |

**Response** `200 OK` (페이지네이션)

---

### 4.4 학습용 카드 조회 (사용자 카드 포함)
```
GET /api/cards/study/all
```

> 인증 필요

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| category | X | 카테고리 코드 |

**Response** `200 OK` (페이지네이션)

---

### 4.5 카드 검색
```
GET /api/cards/search
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| keyword | O | 검색 키워드 |
| category | X | 카테고리 코드 |

**Response** `200 OK` (페이지네이션)

---

### 4.6 카드 수 조회
```
GET /api/cards/count
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| category | X | 카테고리 코드 |

**Response** `200 OK`
```json
100
```

---

## 5. 사용자 카드 (User Card)

> 모든 API 인증 필요

### 5.1 사용자 카드 목록 조회
```
GET /api/user/cards
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| category | X | 카테고리 코드 |

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "question": "나의 질문",
      "questionSub": "My question",
      "answer": "나의 정답",
      "answerSub": "My answer",
      "efFactor": 2.5,
      "category": {
        "id": 1,
        "code": "CS",
        "name": "컴퓨터 과학",
        "parentId": null,
        "parentCode": null
      },
      "createdAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### 5.2 사용자 카드 상세 조회
```
GET /api/user/cards/{id}
```

**Response** `200 OK` (위와 동일)

---

### 5.3 학습용 사용자 카드 조회
```
GET /api/user/cards/study
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| category | X | 카테고리 코드 |

**Response** `200 OK` (페이지네이션)

---

### 5.4 사용자 카드 생성
```
POST /api/user/cards
```

**Request Body**
```json
{
  "question": "나의 질문",
  "questionSub": "My question",
  "answer": "나의 정답",
  "answerSub": "My answer",
  "category": "CS"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| question | String | O | 질문 (한글) |
| questionSub | String | X | 질문 (부제) |
| answer | String | O | 정답 (한글) |
| answerSub | String | X | 정답 (부제) |
| category | String | O | 카테고리 코드 |

**Response** `201 Created`

---

### 5.5 사용자 카드 수정
```
PUT /api/user/cards/{id}
```

**Request Body** (생성과 동일)

**Response** `200 OK`

---

### 5.6 사용자 카드 삭제
```
DELETE /api/user/cards/{id}
```

**Response** `204 No Content`

---

## 6. 북마크 (Bookmark)

> 모든 API 인증 필요

### BookmarkResponse 구조
```json
{
  "bookmarkId": 1,
  "cardId": 10,
  "cardType": "PUBLIC",
  "question": "질문 내용",
  "questionSub": "Question",
  "answer": "정답 내용",
  "answerSub": "Answer",
  "category": {
    "id": 1,
    "code": "CS",
    "name": "컴퓨터 과학",
    "parentId": null,
    "parentCode": null
  },
  "bookmarkedAt": "2024-01-01T00:00:00"
}
```

| cardType | 설명 |
|----------|------|
| PUBLIC | 공개 카드 북마크 |
| CUSTOM | 사용자 카드 북마크 |

---

### 6.1 카드 북마크
```
POST /api/bookmarks/cards/{cardId}
```

**Response** `200 OK` (BookmarkResponse)

---

### 6.2 카드 북마크 해제
```
DELETE /api/bookmarks/cards/{cardId}
```

**Response** `204 No Content`

---

### 6.3 사용자 카드 북마크
```
POST /api/bookmarks/user-cards/{userCardId}
```

**Response** `200 OK` (BookmarkResponse)

---

### 6.4 사용자 카드 북마크 해제
```
DELETE /api/bookmarks/user-cards/{userCardId}
```

**Response** `204 No Content`

---

### 6.5 북마크 목록 조회
```
GET /api/bookmarks
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| category | X | 카테고리 코드 |

**Response** `200 OK` (페이지네이션, BookmarkResponse)

---

### 6.6 카드 북마크 상태 조회
```
GET /api/bookmarks/cards/{cardId}/status
```

**Response** `200 OK`
```json
{
  "bookmarked": true
}
```

---

### 6.7 사용자 카드 북마크 상태 조회
```
GET /api/bookmarks/user-cards/{userCardId}/status
```

**Response** `200 OK`
```json
{
  "bookmarked": false
}
```

---

## 7. 학습 (Study)

> 모든 API 인증 필요

### 7.1 오늘의 학습 카드 조회
```
GET /api/study/cards
```

**Query Parameters**
| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| category | CS | 카테고리 코드 |

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "question": "질문",
      "questionSub": "Question",
      "answer": "정답",
      "answerSub": "Answer",
      "category": {
        "id": 1,
        "code": "CS",
        "name": "컴퓨터 과학",
        "parentId": null,
        "parentCode": null
      },
      "cardType": "PUBLIC"
    }
  ]
}
```

---

### 7.2 답변 제출
```
POST /api/study/answer
```

> 자동으로 학습 세션이 생성/사용됩니다.

**Request Body**
```json
{
  "cardId": 1,
  "cardType": "PUBLIC",
  "isCorrect": true
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| cardId | Long | O | 카드 ID |
| cardType | String | O | PUBLIC 또는 CUSTOM |
| isCorrect | Boolean | O | 정답 여부 |

**Response** `200 OK`
```json
{
  "cardId": 1,
  "cardType": "PUBLIC",
  "isCorrect": true,
  "nextReviewDate": "2024-01-02",
  "newEfFactor": 2.6
}
```

---

### 7.3 추천 카드 조회
```
GET /api/study/recommendations
```

> PRO 플랜의 AI 추천 기능 포함

**Query Parameters**
| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| limit | 20 | 추천 카드 수 |

**Response** `200 OK`
```json
{
  "recommendations": [
    {
      "cardId": 1,
      "userCardId": null,
      "question": "질문 내용",
      "questionSub": "Question",
      "priorityScore": 85,
      "nextReviewDate": "2024-01-01",
      "efFactor": 2.1,
      "lastCorrect": false
    }
  ],
  "totalCount": 5,
  "aiExplanation": "AI 분석 메시지 (PRO 플랜만)"
}
```

---

### 7.4 카테고리별 정확도 조회
```
GET /api/study/category-accuracy
```

**Response** `200 OK`
```json
[
  {
    "categoryId": 1,
    "categoryCode": "CS",
    "categoryName": "컴퓨터 과학",
    "totalCount": 50,
    "correctCount": 40,
    "accuracy": 80.0
  }
]
```

---

## 8. 학습 세션 (Study Session)

> 모든 API 인증 필요

### 8.1 현재 활성 세션 조회
```
GET /api/study/sessions/current
```

**Response** `200 OK`
```json
{
  "id": 1,
  "startedAt": "2024-01-01T10:00:00",
  "endedAt": null,
  "totalCards": 10,
  "correctCount": 8,
  "accuracyRate": 80.0,
  "durationSeconds": null
}
```

---

### 8.2 현재 세션 종료
```
PUT /api/study/sessions/end
```

**Response** `200 OK`
```json
{
  "id": 1,
  "startedAt": "2024-01-01T10:00:00",
  "endedAt": "2024-01-01T10:30:00",
  "totalCards": 15,
  "correctCount": 12,
  "accuracyRate": 80.0,
  "durationSeconds": 1800
}
```

---

### 8.3 세션 히스토리 조회
```
GET /api/study/sessions
```

**Query Parameters**
| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| size | 20 | 페이지 크기 |
| sort | startedAt,desc | 정렬 |

**Response** `200 OK` (페이지네이션)

---

### 8.4 특정 세션 조회
```
GET /api/study/sessions/{sessionId}
```

**Response** `200 OK` (SessionResponse)

---

### 8.5 세션 상세 통계 조회
```
GET /api/study/sessions/{sessionId}/stats
```

**Response** `200 OK`
```json
{
  "id": 1,
  "startedAt": "2024-01-01T10:00:00",
  "endedAt": "2024-01-01T10:30:00",
  "totalCards": 15,
  "correctCount": 12,
  "accuracyRate": 80.0,
  "durationSeconds": 1800,
  "records": [
    {
      "id": 1,
      "cardId": 1,
      "userCardId": null,
      "question": "질문 내용",
      "isCorrect": true,
      "studiedAt": "2024-01-01T10:05:00"
    }
  ]
}
```

---

## 9. 통계 (Stats)

### 9.1 학습 통계 조회
```
GET /api/stats
```

> 인증 필요

**Response** `200 OK`
```json
{
  "overview": {
    "dueToday": 10,
    "totalStudied": 150,
    "newCards": 50,
    "streak": 5,
    "accuracyRate": 85.5
  },
  "deckStats": [
    {
      "category": "CS",
      "newCount": 20,
      "learningCount": 30,
      "reviewCount": 10,
      "masteryRate": 60.0
    }
  ],
  "recentActivity": [
    {
      "date": "2024-01-01",
      "studied": 20,
      "correct": 18
    }
  ]
}
```

---

## 10. 대시보드 (Dashboard)

### 10.1 대시보드 조회
```
GET /api/dashboard
```

> 인증 필요

**Response** `200 OK`
```json
{
  "user": {
    "id": 1,
    "nickname": "닉네임",
    "streak": 5,
    "level": 4,
    "totalStudied": 100
  },
  "today": {
    "dueCards": 10,
    "newCardsAvailable": 50,
    "studiedToday": 15,
    "todayAccuracy": 90.0
  },
  "categoryProgress": [
    {
      "categoryCode": "CS",
      "totalCards": 200,
      "studiedCards": 100,
      "progressRate": 50.0,
      "masteryRate": 25.0
    }
  ],
  "recentActivity": [
    {
      "date": "2024-01-01",
      "studied": 20,
      "correct": 18,
      "accuracy": 90.0
    }
  ],
  "recommendation": {
    "message": "10개의 복습 카드가 있어요!",
    "recommendedCategory": "CS",
    "cardsToStudy": 10,
    "type": "REVIEW"
  }
}
```

### 레벨 기준
| 레벨 | 필요 학습 카드 수 |
|-----|------------------|
| 1 | 0 |
| 2 | 20 |
| 3 | 50 |
| 4 | 100 |
| 5 | 150 |
| 6 | 250 |
| 7 | 350 |
| 8 | 500 |
| 9 | 700 |
| 10 | 1000 |

### 추천 타입
| type | 설명 |
|------|------|
| REVIEW | 복습 카드가 있음 |
| STREAK_KEEP | 스트릭 유지를 위해 학습 필요 |
| NEW | 새 카드 학습 권장 |
| COMPLETE | 오늘 학습 완료 |

---

## 11. 알림 (Notification)

> 모든 API 인증 필요

### NotificationResponse 구조
```json
{
  "id": 1,
  "type": "DAILY_REVIEW",
  "title": "오늘의 학습",
  "body": "복습할 카드가 10개 있습니다.",
  "isRead": false,
  "referenceId": null,
  "createdAt": "2024-01-01T09:00:00"
}
```

### NotificationType
| 타입 | 설명 |
|------|------|
| DAILY_REVIEW | 일일 복습 알림 |
| SUBSCRIPTION_EXPIRING_7 | 구독 만료 7일 전 알림 |
| SUBSCRIPTION_EXPIRING_3 | 구독 만료 3일 전 알림 |
| SUBSCRIPTION_EXPIRING_1 | 구독 만료 1일 전 알림 |
| PAYMENT_FAILED | 결제 실패 알림 |
| PAYMENT_CANCELED | 결제 취소 알림 |
| AUTO_RENEWAL_DISABLED | 자동 갱신 비활성화 알림 |
| STREAK_7 | 7일 연속 학습 달성 |
| STREAK_30 | 30일 연속 학습 달성 |
| STREAK_100 | 100일 연속 학습 달성 |
| CATEGORY_MASTERED | 카테고리 마스터 달성 |

---

### 11.1 FCM 토큰 등록
```
POST /api/notifications/fcm-token
```

**Request Body**
```json
{
  "fcmToken": "dKjf8sdfjKJDF..."
}
```

**Response** `200 OK`

---

### 11.2 FCM 토큰 삭제
```
DELETE /api/notifications/fcm-token
```

**Response** `204 No Content`

---

### 11.3 푸시 설정 조회
```
GET /api/notifications/settings
```

**Response** `200 OK`
```json
{
  "pushEnabled": true,
  "hasFcmToken": true
}
```

---

### 11.4 푸시 설정 변경
```
PATCH /api/notifications/settings
```

**Request Body**
```json
{
  "pushEnabled": false
}
```

**Response** `200 OK`
```json
{
  "pushEnabled": false,
  "hasFcmToken": true
}
```

---

### 11.5 전체 알림 목록 조회
```
GET /api/notifications
```

**Response** `200 OK` (페이지네이션, NotificationResponse)

---

### 11.6 읽지 않은 알림 조회
```
GET /api/notifications/unread
```

**Response** `200 OK` (페이지네이션, NotificationResponse)

---

### 11.7 읽지 않은 알림 수 조회
```
GET /api/notifications/unread/count
```

**Response** `200 OK`
```json
{
  "count": 5
}
```

---

### 11.8 알림 읽음 처리
```
PATCH /api/notifications/{id}/read
```

**Path Parameters**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| id | Long | 알림 ID |

**Response** `200 OK`

---

### 11.9 전체 알림 읽음 처리
```
PATCH /api/notifications/read-all
```

**Response** `200 OK`

---

## 12. 구독 (Subscription)

### 요금제 정보

| 플랜 | 표시명 | AI 카드 생성 | AI 추천 | AI 일일 제한 | 월간 가격 | 연간 가격 |
|------|--------|-------------|---------|-------------|----------|----------|
| FREE | 무료 | O | X | 5회 (평생) | 무료 | - |
| PRO | 프로 | O | O | 30회 / 일 | ₩9,900 | ₩99,000 |

> FREE 플랜의 AI 생성 제한은 **평생 5회**이며 리셋되지 않습니다.
> PRO 플랜의 AI 생성 제한은 **매일 자정에 리셋**됩니다.

---

### 12.1 요금제 목록 조회
```
GET /api/subscriptions/plans
```

> 인증 불필요

**Response** `200 OK`
```json
[
  {
    "plan": "FREE",
    "displayName": "무료",
    "monthlyPrice": 0,
    "yearlyPrice": 0,
    "canGenerateAiCards": true,
    "canUseAiRecommendations": false,
    "aiGenerationDailyLimit": 5,
    "isPurchasable": false
  },
  {
    "plan": "PRO",
    "displayName": "프로",
    "monthlyPrice": 9900,
    "yearlyPrice": 99000,
    "canGenerateAiCards": true,
    "canUseAiRecommendations": true,
    "aiGenerationDailyLimit": 30,
    "isPurchasable": true
  }
]
```

---

### 12.2 내 구독 정보 조회
```
GET /api/subscriptions/me
```

> 인증 필요

**Response** `200 OK`
```json
{
  "id": 1,
  "plan": "PRO",
  "planDisplayName": "프로",
  "status": "ACTIVE",
  "billingCycle": "MONTHLY",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-02-01T00:00:00",
  "isActive": true,
  "canGenerateAiCards": true,
  "canUseAiRecommendations": true,
  "aiGenerationDailyLimit": 30
}
```

**Response** `204 No Content` (구독 없음)

| status | 설명 |
|--------|------|
| ACTIVE | 활성 |
| CANCELED | 취소됨 |
| EXPIRED | 만료됨 |
| PENDING | 대기 중 |

| billingCycle | 설명 |
|-------------|------|
| MONTHLY | 월간 (1개월) |
| YEARLY | 연간 (12개월) |

---

### 12.3 구독 취소
```
POST /api/subscriptions/cancel
```

> 인증 필요

**Request Body** (선택)
```json
{
  "reason": "취소 사유"
}
```

**Response** `204 No Content`

> 구독은 즉시 취소되지 않고 현재 결제 기간 종료 시점까지 유지됩니다.

---

## 13. 결제 (Payment)

> 모든 API 인증 필요
> 결제 제공자: Toss Payments (토스페이먼츠)

### 13.1 결제 세션 생성 (Checkout)
```
POST /api/payments/checkout
```

**Request Body**
```json
{
  "plan": "PRO",
  "billingCycle": "MONTHLY"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| plan | String | O | PRO만 가능 |
| billingCycle | String | O | MONTHLY 또는 YEARLY |

**Response** `200 OK`
```json
{
  "orderId": "ORDER_ABC123XYZ",
  "customerKey": "CK_DEF456UVW",
  "amount": 9900,
  "orderName": "프로 월간 구독"
}
```

> 이 정보로 Toss SDK의 `requestPayment()` 호출

---

### 13.2 빌링키 결제 확정
```
POST /api/payments/confirm-billing
```

> 자동결제(정기구독)용 빌링키 발급 및 최초 결제

**Request Body**
```json
{
  "authKey": "toss_auth_key",
  "customerKey": "CK_DEF456UVW",
  "orderId": "ORDER_ABC123XYZ"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| authKey | String | O | Toss에서 전달받은 인증 키 |
| customerKey | String | O | checkout에서 받은 고객 키 |
| orderId | String | O | checkout에서 받은 주문 ID |

**Response** `200 OK` (SubscriptionResponse)

---

### 13.3 일반 결제 확정
```
POST /api/payments/confirm
```

**Request Body**
```json
{
  "paymentKey": "toss_payment_key",
  "orderId": "ORDER_ABC123XYZ",
  "amount": 9900
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| paymentKey | String | O | Toss에서 전달받은 결제 키 |
| orderId | String | O | checkout에서 받은 주문 ID |
| amount | Integer | O | 결제 금액 (양수, 검증용) |

**Response** `200 OK` (SubscriptionResponse)

---

### 13.4 결제 내역 조회
```
GET /api/payments/invoices
```

**Query Parameters**
| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| page | 0 | 페이지 번호 |
| size | 10 | 페이지 크기 |

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "orderId": "ORDER_ABC123XYZ",
      "amount": 9900,
      "status": "COMPLETED",
      "paymentType": "INITIAL",
      "planType": "PRO",
      "billingCycle": "MONTHLY",
      "paidAt": "2024-01-01T10:00:00",
      "createdAt": "2024-01-01T09:55:00"
    }
  ],
  "totalElements": 1
}
```

| status | 설명 |
|--------|------|
| PENDING | 결제 대기 |
| COMPLETED | 결제 완료 |
| CANCELED | 결제 취소 |
| FAILED | 결제 실패 |

| paymentType | 설명 |
|-------------|------|
| INITIAL | 최초 결제 |
| RENEWAL | 자동 갱신 |
| UPGRADE | 업그레이드 |

---

## 14. AI 카드 생성 (User AI)

> 모든 API 인증 필요
> 사용자가 텍스트를 입력하면 AI가 플래시카드를 자동 생성합니다.

### 14.1 AI 카드 생성
```
POST /api/ai/generate-cards
```

**Request Body**
```json
{
  "sourceText": "REST API는 Representational State Transfer의 약자로...",
  "categoryCode": "CS",
  "count": 5,
  "difficulty": "보통"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|-----|------|------|--------|
| sourceText | String | O | 최대 5000자 |
| categoryCode | String | O | 카테고리 코드 |
| count | Integer | O | 1-20 |
| difficulty | String | X | 난이도 |

**Response** `201 Created`
```json
{
  "generatedCards": [
    {
      "id": 123,
      "question": "REST API란?",
      "questionSub": null,
      "answer": "Representational State Transfer...",
      "answerSub": null,
      "categoryCode": "CS",
      "aiGenerated": true
    }
  ],
  "count": 5,
  "remainingLimit": 25
}
```

---

### 14.2 AI 생성 한도 조회
```
GET /api/ai/generation-limit
```

**Response** `200 OK`
```json
{
  "limit": 30,
  "used": 5,
  "remaining": 25,
  "isLifetime": false
}
```

| 필드 | 설명 |
|-----|------|
| isLifetime | true: FREE 플랜 (평생 제한), false: PRO 플랜 (일일 제한) |

---

## 15. 관리자 API (Admin)

> ADMIN 권한 필요

### 15.1 카테고리 관리

#### 카테고리 생성
```
POST /api/admin/categories
```

**Request Body**
```json
{
  "code": "NEW_CAT",
  "name": "새 카테고리",
  "parentCode": null,
  "displayOrder": 10
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| code | String | O | 카테고리 코드 |
| name | String | O | 카테고리 이름 |
| parentCode | String | X | 상위 카테고리 코드 |
| displayOrder | Integer | X | 표시 순서 |

**Response** `201 Created`

---

#### 카테고리 수정
```
PUT /api/admin/categories/{id}
```

**Request Body**
```json
{
  "code": "NEW_CAT",
  "name": "수정된 카테고리",
  "displayOrder": 5
}
```

**Response** `200 OK`

---

#### 카테고리 삭제
```
DELETE /api/admin/categories/{id}
```

**Response** `204 No Content`

---

### 15.2 카드 관리

#### 카드 목록 조회
```
GET /api/admin/cards
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| category | X | 카테고리 코드 |

**Response** `200 OK` (페이지네이션)

---

#### 카드 상세 조회
```
GET /api/admin/cards/{id}
```

**Response** `200 OK`

---

#### 카드 생성
```
POST /api/admin/cards
```

**Request Body**
```json
{
  "question": "질문",
  "questionSub": "Question",
  "answer": "정답",
  "answerSub": "Answer",
  "category": "CS"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| question | String | O | 질문 |
| questionSub | String | X | 질문 부제 |
| answer | String | O | 정답 |
| answerSub | String | X | 정답 부제 |
| category | String | O | 카테고리 코드 |

**Response** `201 Created`

---

#### 카드 수정
```
PUT /api/admin/cards/{id}
```

**Request Body** (생성과 동일)

**Response** `200 OK`

---

#### 카드 삭제
```
DELETE /api/admin/cards/{id}
```

**Response** `204 No Content`

---

## 16. 관리자 AI 문제 생성 (Generation)

> ADMIN 권한 필요
> 기존 카드 데이터를 기반으로 AI가 새로운 문제를 생성합니다.

### 생성 플로우
```
1. AI 문제 생성 → GeneratedCard 테이블에 저장 (status: PENDING)
2. 관리자 확인 → 승인(APPROVED) / 거부(REJECTED)
3. 승인된 문제 → Card 테이블로 이동 (status: MIGRATED)
```

> 스케줄러가 매일 새벽 3시에 승인된 카드를 자동으로 Card 테이블로 이동합니다.

### 카테고리별 생성 형식

| 카테고리 | question | questionSub | answer | answerSub |
|---------|----------|-------------|--------|-----------|
| TOEIC/EN_* | 빈칸 예문 | 선택지 | 정답 | 해설 |
| JLPT/JN_* | 빈칸 예문 | 선택지 | 정답 | 해설 |
| CS | 질문 | - | 답변 | - |

---

### 16.1 AI 문제 생성
```
POST /api/admin/generation/cards
```

**Request Body**
```json
{
  "categoryCode": "TOEIC",
  "count": 5,
  "model": "gemini-2.0-flash"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| categoryCode | String | O | 카테고리 코드 |
| count | Integer | O | 생성할 문제 수 (1-20) |
| model | String | X | AI 모델 (기본: gemini-2.0-flash) |

**Response** `201 Created`
```json
{
  "generatedCards": [
    {
      "id": 1,
      "model": "gemini-2.0-flash",
      "sourceWord": "executive",
      "question": "The company's _____ decided to expand overseas.",
      "questionSub": "(A) executive (B) execution (C) execute (D) executor",
      "answer": "A",
      "answerSub": "executive는 명사로 '경영진, 임원'을 의미합니다.",
      "category": {
        "id": 5,
        "code": "TOEIC",
        "name": "토익"
      },
      "status": "PENDING",
      "approvedAt": null,
      "createdAt": "2024-01-01T00:00:00"
    }
  ],
  "totalGenerated": 5,
  "categoryCode": "TOEIC",
  "model": "gemini-2.0-flash"
}
```

---

### 16.2 생성 통계 조회
```
GET /api/admin/generation/stats
```

**Response** `200 OK`
```json
{
  "byModel": [
    {
      "model": "gemini-2.0-flash",
      "totalGenerated": 100,
      "approved": 85,
      "rejected": 10,
      "pending": 5,
      "migrated": 80,
      "approvalRate": 89.5
    }
  ],
  "overall": {
    "totalGenerated": 150,
    "approved": 120,
    "rejected": 15,
    "pending": 10,
    "migrated": 100,
    "approvalRate": 88.9
  }
}
```

---

### 16.3 생성된 문제 목록 조회
```
GET /api/admin/generation/cards
```

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|---------|------|------|
| status | X | PENDING, APPROVED, REJECTED, MIGRATED |
| model | X | AI 모델명 |
| page | X | 페이지 번호 (기본: 0) |
| size | X | 페이지 크기 (기본: 20) |
| sort | X | 정렬 (기본: createdAt,desc) |

**Response** `200 OK` (페이지네이션)

---

### 16.4 생성된 문제 상세 조회
```
GET /api/admin/generation/cards/{id}
```

**Response** `200 OK` (GeneratedCardResponse)

---

### 16.5 문제 승인
```
PATCH /api/admin/generation/cards/{id}/approve
```

**Response** `200 OK`
```json
{
  "id": 1,
  "status": "APPROVED",
  "approvedAt": "2024-01-01T12:00:00"
}
```

---

### 16.6 문제 거부
```
PATCH /api/admin/generation/cards/{id}/reject
```

**Response** `200 OK`
```json
{
  "id": 1,
  "status": "REJECTED"
}
```

---

### 16.7 일괄 승인
```
POST /api/admin/generation/cards/batch-approve
```

**Request Body**
```json
{
  "ids": [1, 2, 3, 4, 5]
}
```

**Response** `200 OK`
```json
[
  { "id": 1, "status": "APPROVED" },
  { "id": 2, "status": "APPROVED" }
]
```

---

### 16.8 승인된 문제 Card로 이동
```
POST /api/admin/generation/migrate
```

> 승인된(APPROVED) 문제를 Card 테이블로 이동하고 상태를 MIGRATED로 변경합니다.
> 스케줄러가 매일 새벽 3시에 자동 실행됩니다.

**Response** `200 OK`
```json
{
  "migratedCount": 10,
  "message": "승인된 카드 10개를 Card로 이동 완료"
}
```

---

## 17. 웹훅 (Webhook)

> 서버 to 서버 통신용 - 클라이언트에서 직접 호출하지 않음

### 17.1 Toss 결제 웹훅
```
POST /api/webhooks/toss
```

Toss Payments에서 결제 상태 변경 시 호출

**Headers**
| 헤더 | 필수 | 설명 |
|------|------|------|
| Toss-Signature | O | HMAC-SHA256 시그니처 |

**Request Body**
```json
{
  "eventType": "PAYMENT_STATUS_CHANGED",
  "data": {
    "paymentKey": "toss_payment_key",
    "orderId": "ORDER_ABC123",
    "status": "DONE",
    "cancelReason": null
  }
}
```

| eventType | 설명 |
|-----------|------|
| PAYMENT_STATUS_CHANGED | 결제 상태 변경 |
| BILLING_KEY_DELETED | 빌링키 삭제 |

| status | 설명 |
|--------|------|
| DONE | 결제 완료 |
| CANCELED | 결제 취소 |
| ABORTED | 결제 중단 |
| EXPIRED | 결제 만료 |

**Response** `200 OK`

---

## 18. 에러 코드 (Error Codes)

### 인증 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| UNAUTHORIZED | 401 | 인증되지 않음 |
| TOKEN_EXPIRED | 401 | 토큰 만료 |
| TOKEN_INVALID | 401 | 유효하지 않은 토큰 |
| ACCESS_DENIED | 403 | 접근 권한 없음 |

### 사용자 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| USER_NOT_FOUND | 404 | 사용자를 찾을 수 없음 |
| DUPLICATE_EMAIL | 409 | 이미 존재하는 이메일 |
| INVALID_PASSWORD | 401 | 비밀번호 불일치 |
| OAUTH_USER_CANNOT_CHANGE_PASSWORD | 400 | OAuth 사용자는 비밀번호 변경 불가 |

### 카드 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| CARD_NOT_FOUND | 404 | 카드를 찾을 수 없음 |
| INVALID_CATEGORY | 400 | 유효하지 않은 카테고리 |
| RATE_LIMIT_EXCEEDED | 429 | 비로그인 사용자 조회 제한 초과 |
| CARD_HAS_STUDY_RECORDS | 409 | 학습 기록이 있는 카드 삭제 불가 |
| INVALID_SEARCH_KEYWORD | 400 | 유효하지 않은 검색 키워드 |

### 사용자 카드 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| USER_CARD_NOT_FOUND | 404 | 사용자 카드를 찾을 수 없음 |
| USER_CARD_NOT_OWNER | 403 | 본인의 카드가 아님 |

### 카테고리 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| CATEGORY_NOT_FOUND | 404 | 카테고리를 찾을 수 없음 |
| CATEGORY_CODE_ALREADY_EXISTS | 409 | 이미 존재하는 카테고리 코드 |
| CATEGORY_HAS_CHILDREN | 400 | 하위 카테고리가 있어 삭제 불가 |
| INVALID_PARENT_CATEGORY | 400 | 유효하지 않은 상위 카테고리 |

### 북마크 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| ALREADY_BOOKMARKED | 409 | 이미 북마크됨 |
| BOOKMARK_NOT_FOUND | 404 | 북마크를 찾을 수 없음 |

### 학습 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| SESSION_NOT_FOUND | 404 | 세션을 찾을 수 없음 |
| SESSION_ALREADY_ENDED | 400 | 이미 종료된 세션 |
| SESSION_ACCESS_DENIED | 403 | 세션 접근 권한 없음 |
| NO_ACTIVE_SESSION | 404 | 활성 세션 없음 |
| RECORD_NOT_FOUND | 404 | 학습 기록을 찾을 수 없음 |

### 구독 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| SUBSCRIPTION_NOT_FOUND | 404 | 구독을 찾을 수 없음 |
| SUBSCRIPTION_ALREADY_EXISTS | 409 | 이미 활성 구독이 존재함 |
| SUBSCRIPTION_NOT_ACTIVE | 400 | 구독이 활성 상태가 아님 |
| SUBSCRIPTION_ALREADY_CANCELED | 400 | 이미 취소된 구독 |
| SUBSCRIPTION_EXPIRED | 400 | 만료된 구독 |
| INVALID_PLAN_CHANGE | 400 | 유효하지 않은 플랜 변경 |
| FREE_PLAN_NOT_PURCHASABLE | 400 | 무료 플랜은 구매 불가 |

### 결제 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| PAYMENT_NOT_FOUND | 404 | 결제 정보를 찾을 수 없음 |
| PAYMENT_ALREADY_COMPLETED | 400 | 이미 완료된 결제 |
| PAYMENT_ALREADY_PROCESSED | 400 | 이미 처리된 결제 |
| PAYMENT_AMOUNT_MISMATCH | 400 | 결제 금액 불일치 |
| PAYMENT_CONFIRMATION_FAILED | 400 | 결제 확인 실패 |
| PAYMENT_CANCEL_FAILED | 400 | 결제 취소 실패 |
| BILLING_KEY_ISSUE_FAILED | 400 | 빌링키 발급 실패 |
| BILLING_PAYMENT_FAILED | 400 | 빌링 결제 실패 |
| INVALID_WEBHOOK_SIGNATURE | 401 | 유효하지 않은 웹훅 시그니처 |

### AI 카드 생성 관련 (사용자)
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| AI_FEATURE_NOT_AVAILABLE | 403 | AI 기능을 사용할 수 없는 플랜 |
| GENERATION_LIMIT_EXCEEDED | 429 | AI 생성 횟수 제한 초과 |
| AI_GENERATION_FAILED | 500 | AI 카드 생성 실패 |
| INVALID_AI_RESPONSE | 500 | AI 응답 파싱 실패 |

### 관리자 AI 생성 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| GENERATED_CARD_NOT_FOUND | 404 | 생성된 카드를 찾을 수 없음 |
| INVALID_STATUS_TRANSITION | 400 | 유효하지 않은 상태 전환 |
| ALREADY_APPROVED | 400 | 이미 승인된 카드 |
| ALREADY_REJECTED | 400 | 이미 거부된 카드 |
| ALREADY_MIGRATED | 400 | 이미 이동된 카드 |
| AI_NOT_ENABLED | 503 | AI 서비스 비활성화 |
| NO_CARDS_TO_GENERATE | 400 | 생성할 카드가 없음 |

### 알림 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| FCM_SEND_FAILED | 500 | FCM 전송 실패 |
| INVALID_FCM_TOKEN | 400 | 유효하지 않은 FCM 토큰 |
| NOTIFICATION_NOT_FOUND | 404 | 알림을 찾을 수 없음 |
| NOTIFICATION_ACCESS_DENIED | 403 | 알림 접근 권한 없음 |
