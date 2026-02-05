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
6. [학습 (Study)](#6-학습-study)
7. [학습 세션 (Study Session)](#7-학습-세션-study-session)
8. [통계 (Stats)](#8-통계-stats)
9. [대시보드 (Dashboard)](#9-대시보드-dashboard)
10. [알림 (Notification)](#10-알림-notification)
11. [구독 (Subscription)](#11-구독-subscription)
12. [관리자 API (Admin)](#12-관리자-api-admin)
13. [AI 문제 생성 (Generation)](#13-ai-문제-생성-generation)
14. [에러 코드 (Error Codes)](#14-에러-코드-error-codes)
15. [웹훅 (Webhook)](#15-웹훅-webhook)

---

## 공통 사항

### 인증 헤더
```
Authorization: Bearer {accessToken}
```

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
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
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
| password | String | O | 8자 이상 |
| passwordConfirm | String | O | password와 일치 |
| nickname | String | O | - |

**Response** `201 Created`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "닉네임",
  "roles": ["ROLE_USER"],
  "streak": 0,
  "masteryRate": 0.0
}
```

---

### 1.2 로그인
```
POST /api/auth/signin
```

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

### 1.7 소셜 로그인 (OAuth2)

> Spring Security OAuth2가 처리 - 현재 비활성화 상태

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
  "streak": 5,
  "masteryRate": 75.5
}
```

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

**Response** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "새닉네임",
  "roles": ["ROLE_USER"],
  "streak": 5,
  "masteryRate": 75.5
}
```

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

### 4.5 카드 수 조회
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

## 6. 학습 (Study)

> 모든 API 인증 필요

### 6.1 오늘의 학습 카드 조회
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
      }
    }
  ]
}
```

---

### 6.2 답변 제출
```
POST /api/study/answer
```

> 자동으로 학습 세션이 생성/사용됩니다.

**Request Body**
```json
{
  "cardId": 1,
  "isCorrect": true
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| cardId | Long | O | 카드 ID |
| isCorrect | Boolean | O | 정답 여부 |

**Response** `200 OK`
```json
{
  "cardId": 1,
  "isCorrect": true,
  "nextReviewDate": "2024-01-02",
  "newEfFactor": 2.6
}
```

---

## 7. 학습 세션 (Study Session)

> 모든 API 인증 필요

### 7.1 현재 활성 세션 조회
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

### 7.2 현재 세션 종료
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

### 7.3 세션 히스토리 조회
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

### 7.4 특정 세션 조회
```
GET /api/study/sessions/{sessionId}
```

**Response** `200 OK` (SessionResponse)

---

### 7.5 세션 상세 통계 조회
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

## 8. 통계 (Stats)

### 8.1 학습 통계 조회
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

## 9. 대시보드 (Dashboard)

### 9.1 대시보드 조회
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

## 10. 알림 (Notification)

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
| STREAK_7 | 7일 연속 학습 달성 |
| STREAK_30 | 30일 연속 학습 달성 |
| STREAK_100 | 100일 연속 학습 달성 |
| CATEGORY_MASTERED | 카테고리 마스터 달성 |

---

### 10.1 FCM 토큰 등록
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

### 10.2 FCM 토큰 삭제
```
DELETE /api/notifications/fcm-token
```

**Response** `204 No Content`

---

### 10.3 푸시 설정 조회
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

### 10.4 푸시 설정 변경
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

### 10.5 전체 알림 목록 조회
```
GET /api/notifications
```

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "type": "DAILY_REVIEW",
    "title": "오늘의 학습",
    "body": "복습할 카드가 10개 있습니다.",
    "isRead": false,
    "referenceId": null,
    "createdAt": "2024-01-01T09:00:00"
  }
]
```

---

### 10.6 읽지 않은 알림 조회
```
GET /api/notifications/unread
```

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "type": "DAILY_REVIEW",
    "title": "오늘의 학습",
    "body": "복습할 카드가 10개 있습니다.",
    "isRead": false,
    "referenceId": null,
    "createdAt": "2024-01-01T09:00:00"
  }
]
```

---

### 10.7 읽지 않은 알림 수 조회
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

### 10.8 알림 읽음 처리
```
PATCH /api/notifications/{id}/read
```

**Path Parameters**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| id | Long | 알림 ID |

**Response** `200 OK`

---

### 10.9 전체 알림 읽음 처리
```
PATCH /api/notifications/read-all
```

**Response** `200 OK`

---

## 11. 구독 (Subscription)

### 요금제 정보

| 플랜 | 일일 제한 | AI 카드 접근 | 월간 가격 | 연간 가격 |
|------|----------|-------------|----------|----------|
| FREE | 15문제 | ❌ | 무료 (비로그인) | - |
| BASIC | 100문제 | ❌ | 무료 (로그인) | - |
| PREMIUM | 무제한 | ⭕ | ₩3,900 | ₩39,000 |

---

### 11.1 요금제 목록 조회
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
    "dailyLimit": 15,
    "monthlyPrice": 0,
    "yearlyPrice": 0,
    "canAccessAiCards": false,
    "isPurchasable": false
  },
  {
    "plan": "BASIC",
    "displayName": "기본",
    "dailyLimit": 100,
    "monthlyPrice": 0,
    "yearlyPrice": 0,
    "canAccessAiCards": false,
    "isPurchasable": false
  },
  {
    "plan": "PREMIUM",
    "displayName": "프리미엄",
    "dailyLimit": 2147483647,
    "monthlyPrice": 3900,
    "yearlyPrice": 39000,
    "canAccessAiCards": true,
    "isPurchasable": true
  }
]
```

---

### 11.2 내 구독 정보 조회
```
GET /api/subscriptions/me
```

**Response** `200 OK`
```json
{
  "id": 1,
  "plan": "PREMIUM",
  "planDisplayName": "프리미엄",
  "status": "ACTIVE",
  "billingCycle": "MONTHLY",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-02-01T00:00:00",
  "isActive": true,
  "dailyLimit": 2147483647,
  "canAccessAiCards": true
}
```

**Response** `204 No Content` (구독 없음)

---

### 11.3 결제 세션 생성 (Checkout)
```
POST /api/subscriptions/checkout
```

**Request Body**
```json
{
  "plan": "PREMIUM",
  "billingCycle": "MONTHLY"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| plan | String | O | PREMIUM만 가능 |
| billingCycle | String | O | MONTHLY 또는 YEARLY |

**Response** `200 OK`
```json
{
  "orderId": "ORDER_ABC123XYZ",
  "customerKey": "CK_DEF456UVW",
  "amount": 3900,
  "orderName": "프리미엄 월간 구독"
}
```

> 이 정보로 Toss SDK의 `requestPayment()` 호출

---

### 11.4 결제 확정
```
POST /api/subscriptions/confirm
```

> Toss 결제 완료 후 successUrl에서 전달받은 값으로 호출

**Request Body**
```json
{
  "paymentKey": "toss_payment_key",
  "orderId": "ORDER_ABC123XYZ",
  "amount": 3900
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| paymentKey | String | O | Toss에서 전달받은 결제 키 |
| orderId | String | O | checkout에서 받은 주문 ID |
| amount | Integer | O | 결제 금액 (검증용) |

**Response** `200 OK`
```json
{
  "id": 1,
  "plan": "PREMIUM",
  "planDisplayName": "프리미엄",
  "status": "ACTIVE",
  "billingCycle": "MONTHLY",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-02-01T00:00:00",
  "isActive": true,
  "dailyLimit": 2147483647,
  "canAccessAiCards": true
}
```

---

### 11.5 구독 취소
```
POST /api/subscriptions/cancel
```

**Request Body** (선택)
```json
{
  "reason": "취소 사유"
}
```

**Response** `204 No Content`

> 구독은 즉시 취소되지 않고 현재 결제 기간 종료 시점까지 유지됩니다.

---

### 11.6 결제 내역 조회
```
GET /api/subscriptions/invoices
```

**Query Parameters**
| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| page | 0 | 페이지 번호 |
| size | 20 | 페이지 크기 |

**Response** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "orderId": "ORDER_ABC123XYZ",
      "amount": 3900,
      "status": "COMPLETED",
      "type": "INITIAL",
      "method": "카드",
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

| type | 설명 |
|------|------|
| INITIAL | 최초 결제 |
| RENEWAL | 자동 갱신 |
| UPGRADE | 업그레이드 |

---

## 12. 관리자 API (Admin)

> ADMIN 권한 필요

### 12.1 카테고리 관리

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

### 12.2 카드 관리

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

## 13. AI 문제 생성 (Generation)

> ADMIN 권한 필요
> 기존 카드 데이터를 기반으로 AI가 새로운 문제를 생성합니다.

### 생성 플로우
```
1. AI 문제 생성 → GeneratedCard 테이블에 저장 (status: PENDING)
2. 관리자 확인 → 승인(APPROVED) / 거부(REJECTED)
3. 승인된 문제 → Card 테이블로 이동 (status: MIGRATED)
```

### 카테고리별 생성 형식

| 카테고리 | question | questionSub | answer | answerSub |
|---------|----------|-------------|--------|-----------|
| TOEIC/EN_* | 빈칸 예문 | 선택지 | 정답 | 해설 |
| JLPT/JN_* | 빈칸 예문 | 선택지 | 정답 | 해설 |
| CS | 질문 | - | 답변 | - |

---

### 13.1 AI 문제 생성
```
POST /api/admin/generation/cards
```

**Request Body**
```json
{
  "categoryCode": "TOEIC",
  "count": 5,
  "model": "gpt-5-mini"
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| categoryCode | String | O | 카테고리 코드 |
| count | Integer | O | 생성할 문제 수 (1-20) |
| model | String | X | AI 모델 (기본: gpt-5-mini) |

**Response** `201 Created`
```json
{
  "generatedCards": [
    {
      "id": 1,
      "model": "gpt-5-mini",
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
  "model": "gpt-5-mini"
}
```

---

### 13.2 생성 통계 조회
```
GET /api/admin/generation/stats
```

**Response** `200 OK`
```json
{
  "byModel": [
    {
      "model": "gpt-5-mini",
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

### 13.3 생성된 문제 목록 조회
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

### 13.4 생성된 문제 상세 조회
```
GET /api/admin/generation/cards/{id}
```

**Response** `200 OK`
```json
{
  "id": 1,
  "model": "gpt-5-mini",
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
```

---

### 13.5 문제 승인
```
PATCH /api/admin/generation/cards/{id}/approve
```

**Response** `200 OK`
```json
{
  "id": 1,
  "status": "APPROVED",
  "approvedAt": "2024-01-01T12:00:00",
  ...
}
```

---

### 13.6 문제 거부
```
PATCH /api/admin/generation/cards/{id}/reject
```

**Response** `200 OK`
```json
{
  "id": 1,
  "status": "REJECTED",
  ...
}
```

---

### 13.7 일괄 승인
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
  { "id": 1, "status": "APPROVED", ... },
  { "id": 2, "status": "APPROVED", ... },
  ...
]
```

---

### 13.8 승인된 문제 Card로 이동
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

## 14. 에러 코드 (Error Codes)

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
| EMAIL_ALREADY_EXISTS | 409 | 이미 존재하는 이메일 |
| PASSWORD_MISMATCH | 400 | 비밀번호 불일치 |

### 카드 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| CARD_NOT_FOUND | 404 | 카드를 찾을 수 없음 |

### 카테고리 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| CATEGORY_NOT_FOUND | 404 | 카테고리를 찾을 수 없음 |
| CATEGORY_CODE_EXISTS | 409 | 이미 존재하는 카테고리 코드 |

### 학습 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| SESSION_NOT_FOUND | 404 | 세션을 찾을 수 없음 |
| SESSION_ALREADY_ENDED | 400 | 이미 종료된 세션 |
| SESSION_ACCESS_DENIED | 403 | 세션 접근 권한 없음 |
| NO_ACTIVE_SESSION | 404 | 활성 세션 없음 |
| DAILY_LIMIT_EXCEEDED | 429 | 일일 학습 제한 초과 |
| AI_CARD_ACCESS_DENIED | 403 | AI 카드 접근 권한 없음 (Premium 플랜 필요) |

### 구독 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| SUBSCRIPTION_NOT_FOUND | 404 | 구독을 찾을 수 없음 |
| SUBSCRIPTION_ALREADY_EXISTS | 409 | 이미 활성 구독이 존재함 |
| SUBSCRIPTION_NOT_ACTIVE | 400 | 구독이 활성 상태가 아님 |
| INVALID_PLAN | 400 | 유효하지 않은 요금제 |
| PAYMENT_NOT_FOUND | 404 | 결제 정보를 찾을 수 없음 |
| PAYMENT_AMOUNT_MISMATCH | 400 | 결제 금액 불일치 |
| PAYMENT_FAILED | 500 | 결제 처리 실패 |
| PAYMENT_ALREADY_COMPLETED | 400 | 이미 완료된 결제 |

### AI 생성 관련
| 코드 | HTTP 상태 | 설명 |
|-----|----------|------|
| GENERATED_CARD_NOT_FOUND | 404 | 생성된 카드를 찾을 수 없음 |
| ALREADY_APPROVED | 400 | 이미 승인된 카드 |
| ALREADY_REJECTED | 400 | 이미 거부된 카드 |
| ALREADY_MIGRATED | 400 | 이미 이동된 카드 |
| AI_GENERATION_FAILED | 500 | AI 문제 생성 실패 |
| NO_CARDS_TO_GENERATE | 400 | 생성할 카드가 없음 |

---

## 15. 웹훅 (Webhook)

> 서버 to 서버 통신용 - 클라이언트에서 직접 호출하지 않음

### 15.1 Toss 결제 웹훅
```
POST /api/webhooks/toss
```

Toss Payments에서 결제 상태 변경 시 호출

**Headers**
| 헤더 | 필수 | 설명 |
|------|------|------|
| Toss-Signature | X | HMAC-SHA256 시그니처 |

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
