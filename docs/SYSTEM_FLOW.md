# Plant System Flow (아키텍처 · 데이터 흐름)

> **최초 작성일**: 2026-09-11  
> **최종 수정일**: 2026-10-09

## 목차

1. [아키텍처 개요](#1-아키텍처-개요)
2. [핵심 시스템 플로우](#2-핵심-시스템-플로우)
3. [예외 처리 아키텍처](#3-예외-처리-아키텍처)

---

## 1. 아키텍처 개요

### 1-1. 3계층 Clean Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                    │
│                                                         │
│   Screen (Compose)  ←──StateFlow──  ViewModel (@Hilt)   │
│   사용자 입력  ────────────────────▶  UseCase 호출        │
│                                     UiState 관리        │
├─────────────────────────────────────────────────────────┤
│                     Domain Layer                        │
│                                                         │
│   UseCase            Repository (Interface)             │
│   비즈니스 로직       데이터 접근 추상화                    │
│                                                         │
│   Model              Result<T>                          │
│   순수 도메인 모델     성공/실패 래퍼                       │
├─────────────────────────────────────────────────────────┤
│                      Data Layer                         │
│                                                         │
│   Repository (Impl)     DataSource                      │
│   데이터 조합/변환       Remote (Firebase)                │
│                          Local (DataStore)              │
│   Mapper                                                │
│   DTO ↔ Domain 변환                                      │
└─────────────────────────────────────────────────────────┘
```

### 1-2. 단방향 데이터 흐름 (UDF)

```
┌─────────┐   이벤트    ┌───────────┐   UseCase    ┌────────────┐   Repository  ┌────────────┐
│         │ ─────────▶ │           │ ──────────▶  │            │ ────────────▶ │            │
│   UI    │            │ ViewModel │              │  UseCase   │               │    Data    │
│(Compose)│ ◀───────── │           │ ◀──────────  │            │ ◀──────────── │   Source   │
│         │  StateFlow │           │   Result<T>  │            │   Result<T>   │            │
└─────────┘            └───────────┘              └────────────┘               └────────────┘
```

**핵심 원칙**:
- UI → ViewModel: **이벤트** (버튼 탭, 입력 등)
- ViewModel → UI: **StateFlow** (단방향, UI는 상태를 구독만 함)
- ViewModel → UseCase: 비즈니스 로직 위임
- UseCase → Repository: 데이터 접근 (인터페이스를 통해)
- Data Layer: Firebase/DataStore에서 데이터를 가져와 Domain Model로 변환

### 1-3. 실시간 구독 흐름 (Flow)

```
Firestore Collection/Document
  │ addSnapshotListener
  │
  ▼
RemoteDataSource
  │ callbackFlow { ... }
  │
  ▼
Repository Impl
  │ Flow<List<DTO>> → Flow<List<DomainModel>>  (Mapper 적용)
  │
  ▼
ViewModel
  │ .stateIn(viewModelScope) or .collectLatest { }
  │
  ▼
UI (자동 리컴포지션)
```

**실시간 구독 사용처:**

| 데이터 | 구독 메서드 | 사용 화면 |
|--------|-----------|----------|
| 화분 목록 | `PotRepository.getPots()` | HomeScreen, PotListScreen |
| 학습 중인 유저 | `StudyingRepository.observeStudyingUser()` | StudyingScreen |
| 커뮤니티 활동 | `CommunityRepository.observeActivity()` | CommunityActivityScreen |
| 학습 기록 | `StudyLogRepository.getStudyLogs()` | StudyPlanDetailScreen |
| 태그 목록 | `PotRepository.getAvailableTags()` | NewBornTreeScreen |
| 현재 유저 | `UserRepository.currentUser` | 전역 (StateFlow) |

---

## 2. 핵심 시스템 플로우

### 2-1. 인증 · 세션 시스템

```
┌──────────────────────────────────────────────────────────────────┐
│                        인증 시스템 전체 구조                        │
│                                                                  │
│  ┌──────────┐   idToken  ┌──────────┐   uid     ┌─────────────┐  │
│  │ Google   │ ──────────▶│ Firebase │──────────▶│ Firestore   │  │
│  │Credential│            │   Auth   │           │ users/{uid} │  │
│  │ Manager  │            └──────────┘           └──────┬──────┘  │
│  └──────────┘                                          │         │
│                                                        ▼         │
│                                                 ┌─────────────┐  │
│                                                 │  nicknames/ │  │
│                                                 │ {nickname}  │  │
│                                                 └─────────────┘  │
└──────────────────────────────────────────────────────────────────┘

흐름:
1. Credential Manager → Google idToken 획득
2. Firebase Auth → idToken으로 인증 → uid 발급
3. Firestore users/{uid} 문서 조회 or 생성
4. nicknames/{nickname} 중복 검사 + 등록
5. UserRepository.startUserSession(user) → 실시간 구독 시작
```

#### 세션 생명주기

```
앱 시작
  │
  ▼
SplashViewModel.checkAuthLogin()
  │
  ├─ 세션 없음 → SignInScreen
  │
  └─ 세션 있음 → UserRepository.startUserSession(user)
                   │
                   ├─ currentUser StateFlow 세팅
                   └─ Firestore users/{uid} 실시간 구독 시작
                        │
                        ▼
                   앱 사용 중 (currentUser 실시간 반영)
                        │
     ┌──────────────────┼──────────────────┐
     │                  │                  │
     ▼                  ▼                  ▼
  로그아웃           회원탈퇴                  세션 만료
     │                  │                        │
     ▼                  ▼                        ▼
  endUserSession()   getSignInProvider()       notifySessionExpired()
  signOut()          → Google 재인증            → MainActivity 이벤트 수신
     │               (reauthenticateWithGoogle) → signOut()
     ▼               → endUserSession()        → SignInScreen 이동
  SignInScreen       → deleteUserData()        → 세션 만료 다이얼로그 표시
                     → deleteAuthAccount()
                     → SignInScreen
```

#### EnsureCurrentUser 가드

`currentUser`가 필요한 UseCase는 실행 전 `EnsureCurrentUserUseCase`를 호출하여 세션 유효성을 검증한다.

- `currentUser != null` → UseCase 계속 실행
- `currentUser == null` → `notifySessionExpired()` → 로그인 화면 이동 + 세션 만료 다이얼로그 표시

트리거 시점: UseCase가 Firestore에 접근하기 직전. ViewModel에서 직접 주입하거나, 다른 UseCase 내부에서 호출한다.

### 2-2. 학습 시스템

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         학습 시스템 데이터 흐름                            │
│                                                                         │
│   StudyingScreen                                                        │
│       │                                                                 │
│       ├── 시작 ──▶ studying/{uid} 학습 세션 문서 생성                      │
│       │            + 로컬 DataStore에 세션 백업 저장                       │
│       │                                                                 │
│       ├── 진행                                                           │
│       │     ├─ 5초마다 ──▶ 로컬 백업 갱신 (비정상 종료 복구용)               │
│       │     └─ 1분마다 ──▶ 서버 학습 시간 동기화 (같은 태그 학습자에게 공유)   │
│       │                                                                 │
│       └── 종료 ──▶ 병렬 실행:                                             │
│                    ├─ 학습 기록 저장                                      │
│                    ├─ 화분 누적 학습 시간 갱신                             │
│                    └─ 유저 총 학습 시간 갱신                               │
│                                                                         │
│                    + 학습 세션 문서 삭제 + 로컬 백업 클리어                  │
│                    + 학습 완료 보상 지급 (→ 2-6 참조)                      │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2-3. 커뮤니티 시스템

```
┌──────────────────────────────────────────────────────────────────┐
│                     커뮤니티 데이터 흐름                           │
│                                                                  │
│   게시글 작성                                                     │
│   ├─ posts/{postId} 문서 생성                                     │
│   └─ activities/{activityId} 활동 기록 (type="게시물")             │
│                                                                  │
│   좋아요 토글                                                     │
│   ├─ posts/{postId} 좋아요 목록 추가/제거 + 카운트 갱신              │
│   └─ activities/{activityId} 활동 기록 생성/삭제 (type="좋아요")    │
│                                                                  │
│   댓글 작성                                                       │
│   ├─ posts/{postId}/comments/{commentId} 문서 생성                │
│   ├─ posts/{postId} 댓글 카운트 +1                                │
│   └─ activities/{activityId} 활동 기록 (type="댓글")               │
│                                                                  │
│   페이지네이션 (게시글 목록)                                        │
│   ├─ 커서 기반: 마지막 게시글의 생성 시각을 기준으로 다음 페이지 로드    │
│   └─ 반환: 게시글 목록 + 다음 커서 + 다음 페이지 존재 여부             │
│                                                                  │
│   북마크 토글                                                     │
│   ├─ posts/{postId} 북마크 목록 추가/제거 + 카운트 갱신              │
│                                                                  │
│   검색 (클라이언트 측)                                             │
│   ├─ 전체 게시글 로드 후 제목/본문 키워드 필터링                      │
└──────────────────────────────────────────────────────────────────┘
```

### 2-4. 화분 성장 시스템

```
┌──────────────────────────────────────────────────────────────┐
│                     화분 성장 데이터 흐름                       │
│                                                              │
│   학습 종료                                                   │
│       │                                                      │
│       ▼                                                      │
│   pots/{potId} 누적 학습 시간 갱신                              │
│       │                                                      │
│       ▼                                                      │
│   레벨 판정 (누적 시간 기반 Lv.0~6, 도메인 계산)                  │
│       │                                                      │
│       ▼                                                      │
│   UI: 화분 종류 + 레벨 → 화분 이미지 결정                        │
│                                                              │
│   레벨업 이중 조건 (누적 학습 시간 + 아이템 소비)                 │
│   ├─ 기준 시점 도달 시 필요 아이템 차감 시도                      │
│   ├─ 아이템 부족 시 → 레벨업 대기                               │
│   └─ 레벨별 시간 기준·아이템 조건표는 PRD 참조                    │
└──────────────────────────────────────────────────────────────┘
```

### 2-5. 출석체크 시스템

```
┌──────────────────────────────────────────────────────────────────┐
│                    출석체크 데이터 흐름                            │
│                                                                  │
│   HomeScreen → 출석체크 아이콘 탭 → AttendanceCheckDialog           │
│       │                                                          │
│       ▼ [출석하기] 버튼 탭                                         │
│   출석 판정 (오늘 이미 출석 / 28일 완료 / 달 변경 / 정상 출석)        │
│       │                                                          │
│       ├─ 오늘 이미 출석 → 출석 불가                                 │
│       ├─ 28일 완료 (같은 달) → 출석 불가                            │
│       ├─ 달 바뀜 → 출석 횟수 1로 재시작                             │
│       └─ 정상 출석 → 출석 횟수 +1                                  │
│              │                                                   │
│              ▼ Firestore Transaction (출석 기록 + 보상 원자적 저장) │
│              ├─ users/{uid} 출석 횟수·시각 갱신                    │
│              └─ 보상 지급 (Gold 또는 아이템)                        │
│                                                                  │
│   ※ 28일 출석판 보상표 상세는 PRD 참조                              │
└──────────────────────────────────────────────────────────────────┘
```

### 2-6. 아이템 · 보상 · 상점 시스템

```
┌───────────────────────────────────────────────────────────────────────┐
│                  아이템 · 보상 · 상점 흐름                               │
│                                                                       │
│   학습 완료 보상                                                        │
│   ├─ 세션 공부 시간 티어별 아이템 + 보너스박스 지급                         │
│   └─ users/{uid} 아이템 수량 갱신                                       │
│                                                                       │
│   보너스박스 개봉                                                       │
│   ├─ 확률 테이블 기반 드롭 (아이템 / Gold / 보너스박스)                    │
│   └─ users/{uid} 결과 반영                                             │
│                                                                       │
│   상점 구매                                                            │
│   ├─ Gold 잔액 확인 → Firestore Transaction으로 Gold 차감 + 아이템 증가   │
│   │                                                                   │
│   Gold 수급 경로:                                                      │
│   ├─ 보너스박스 드롭                                                    │
│   ├─ 출석체크 보상                                                      │
│   └─ 목표 달성 보상 (기획 미정)                                          │
│                                                                       │
│   ※ 티어별 보상표, 확률, 가격표 등 상세는 PRD 참조                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 예외 처리 아키텍처

### 3-1. Result<T> 패턴

```
sealed class Result<out T>
├── Success<T>(val data: T)
└── Failure(val error: AppError)

확장 함수:
├── onSuccess { data -> ... }     // 성공 시 실행
├── onFailure { error -> ... }    // 실패 시 실행
├── map { transform(data) }       // 데이터 변환
└── getOrNull()                   // T? 반환
```

### 3-2. 예외 전파 경로

```
DataSource (Firebase 호출)
  │ try-catch → AppError 변환
  │
  ▼
Repository
  │ Result<T> 반환
  │
  ▼
UseCase
  │ 비즈니스 검증 실패 → Result.Failure(AppError.Custom(...))
  │ Repository 실패 → 그대로 전파
  │
  ▼
ViewModel
  │ onFailure → UiState.error 갱신
  │
  ▼
UI
  │ 토스트 / 다이얼로그 표시
  │ error.message 사용
```

### 3-3. AppError 분류 및 대응

| 에러 타입 | 메시지 | 트리거 상황 | 대응 |
|-----------|--------|------------|------|
| `Network` | 인터넷 연결이 원활하지 않습니다 | Firestore 타임아웃, 오프라인 | 재시도 가능 |
| `Auth` | 인증에 실패했습니다 | Firebase Auth 에러 | 세션 클리어 |
| `UnknownUser` | 사용자 정보를 가져오지 못했습니다 | `currentUser == null` | 세션 만료 → 로그인 화면 이동 |
| `Email` | 이메일 형식이 올바르지 않습니다 | 이메일 유효성 검증 | 입력 재요청 |
| `Password` | 비밀번호 형식이 올바르지 않습니다 | 비밀번호 유효성 검증 | 입력 재요청 |
| `Server` | 서버에 오류가 발생했습니다 | Firestore 서버 에러 | 재시도 가능 |
| `Unknown` | 알 수 없는 오류가 발생했습니다 | 예상 외 예외 | 토스트 표시 |
| `Upload` | 저장에 실패했습니다 | 문서 저장 실패 | 재시도 가능 |
| `Local` | 로컬 저장 실패 | DataStore 에러 | 에러 로깅, 무시 또는 재시도 |
| `Permission` | 권한이 없습니다 | Firestore 보안 규칙 위반 | 권한 안내 |
| `Custom(msg)` | (동적 메시지) | 닉네임 중복, 본인 좋아요 등 | 비즈니스 로직별 분기 |

### 3-4. SessionExpiredEvent 구조

```
┌───────────────────┐        ┌─────────────────────┐        ┌──────────────┐
│  UseCase          │        │  SessionExpired     │        │  MainActivity│
│  (Domain Layer)   │        │  Event              │        │  (UI Layer)  │
│                   │        │  (Data Layer)       │        │              │
│  Notifier         │──emit─▶│  MutableSharedFlow  │──collect─▶ Observer   │
│  .notifyExpired() │        │  <Unit>             │        │  .event      │
└───────────────────┘        └─────────────────────┘        └──────────────┘

Hilt 바인딩:
SessionExpiredEvent 하나의 인스턴스가
├── SessionExpiredNotifier (Domain Interface)
└── SessionExpiredObserver (Domain Interface)
두 인터페이스를 모두 구현 (같은 @Singleton)
```

---

## 관련 문서

| 문서 | 경로 |
|------|------|
| PRD | [docs/PRD.md](PRD.md) |
| User Flow | [docs/USER_FLOW.md](USER_FLOW.md) |
| 화면명세 | docs/SCREEN_SPEC.md (작성 예정) |
| 데이터 모델 | [docs/DATA_MODEL.md](DATA_MODEL.md) |
