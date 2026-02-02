# 스터디 카드 프로젝트

## 🎯 핵심 기능

### 1. **카드 데이터 관리** ⭐
초기 데이터: CS 오픈소스 (jwasham GitHub 1K+ 카드)

CSV import (SQLite → Postgres)

카테고리: CS 우선 (영단어/SQL/일본어 확장 예정)

카드 형식: 앞면(문제) | 뒷면(정답) + efFactor

정답/오답 체크 → 난이도 자동 조정

AI 생성 준비: Gemini 3 Flash API (Phase 2, 월 100원)

### 2. 스페이싱 반복 학습
- 학습 세션 기록
- 다음 학습 시점 계산 (Anki 알고리즘)
- 문제 카드 우선 노출

### 3. Freemium 모델
- 비로그인: 15개/일 (자정 초기화)
- 로그인: 무제한 + 진행도 저장

### 4. 일일 추천
- 새벽 10개 자동 푸시
- 카테고리별 맞춤

### 5. 인증 & 통계
- JWT + Redis 블랙리스트
- 스트릭, 마스터리율

## 📁 프로젝트 구조 예시
study-cards-be/ (Spring Boot + 라이트 DDD)
├── application/
│ ├── card/ # CardController, CardUseCase
│ ├── ai/ # GeminiCardGenerator (준비)
│ ├── study/ # StudySessionService
│ ├── daily/ # DailyScheduler
│ └── user/ # AuthController
├── domain/
│ ├── card/ # Card(question, answer, efFactor)
│ ├── study/ # SpacingAlgorithm
│ └── user/ # UserProgress
├── infrastructure/
│ ├── jpa/ # CardRepository
│ ├── redis/ # RedisService
│ └── import/ # CsvCardImporter (jwasham)
├── resources/csv/
│ └── cs-jwasham.csv
└── docker-compose.yml

study-cards-fe/ (React + Vite + TS + Shadcn/UI)
├── components/
│ ├── CardDeck.tsx # 정답/오답 버튼
│ └── DifficultyGauge.tsx
└── hooks/useStudyCards.ts

## 🛠️ 기술 스택
FE: React + Vite + Shadcn/UI → Vercel
BE: Spring Boot + Postgres + Redis → Docker
AI: Gemini 3 Flash (준비, 월 100원)
Deploy: Oracle Always Free VM 2개 + Nginx + Cloudflare
CI/CD: GitHub Actions

## 👥 사용자 플로우

### 🔓 비로그인
[Cloudflare CDN] → CS 선택 → Redis 체크
├─ jwasham CS 카드 15개 (자정 초기화)
└─ 로그인 CTA

text

### ✅ 로그인
대시보드 → efFactor 낮은 문제 우선
→ 정답/오답 → 실시간 업데이트
→ 새벽 AI 카드 추천 푸시 (준비)