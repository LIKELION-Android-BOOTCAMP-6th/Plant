# Plant System Flow (아키텍처 · 데이터 흐름 · 기술 스택 근거)

> **최초 작성일**: 2026-09-11  
> **최종 수정일**: 2026-09-15

## 목차

1. [기술 스택 및 선정 근거](#1-기술-스택-및-선정-근거)
2. [아키텍처 개요](#2-아키텍처-개요)
3. [레이어별 데이터 흐름](#3-레이어별-데이터-흐름)
4. [핵심 시스템 플로우](#4-핵심-시스템-플로우)
5. [DI 의존성 그래프](#5-di-의존성-그래프)
6. [데이터 동기화 전략](#6-데이터-동기화-전략)
7. [에러 처리 아키텍처](#7-에러-처리-아키텍처)
8. [세션 관리 아키텍처](#8-세션-관리-아키텍처)

---

## 1. 기술 스택 및 선정 근거

### 1-1. 기술 스택 총괄

| 분류 | 기술 | 버전/비고 |
|------|------|-----------|
| **Language** | Kotlin | — |
| **UI Framework** | Jetpack Compose + Material 3 | 선언형 UI |
| **Architecture** | MVVM + Clean Architecture | Feature-based 패키지 구조 |
| **DI** | Hilt | `@HiltViewModel`, `@Module`/`@InstallIn` |
| **Async** | Coroutines & Flow | StateFlow, SharedFlow |
| **Navigation** | Jetpack Navigation Compose | Type-safe Route (kotlinx.serialization) |
| **Backend** | Firebase (Auth, Firestore, FCM, Functions) | Serverless |
| **Local Storage** | Preferences DataStore | 학습 세션 백업 |
| **Image Loading** | Coil | — |
| **Serialization** | kotlinx.serialization | Navigation Route 직렬화 |
| **Build** | Gradle KTS, KSP | — |

### 1-2. 기술 선정 근거

#### Jetpack Compose + Material 3

| 기준 | 근거 |
|------|------|
| **생산성** | XML 대비 코드량 감소, 프리뷰 즉시 확인 |
| **상태 관리** | StateFlow ↔ `collectAsState()` 자연스러운 연결 |
| **테마** | Material 3 Dynamic Color + 다크모드 지원 내장 |
| **팀 학습** | 부트캠프 커리큘럼과 일치하여 학습 곡선 최소화 |

#### MVVM + Clean Architecture

| 기준 | 근거 |
|------|------|
| **관심사 분리** | Presentation / Domain / Data 3계층으로 역할 분리 |
| **테스트 용이성** | UseCase 단위 테스트 가능, Repository 인터페이스로 모킹 가능 |
| **확장성** | 기능 추가 시 UseCase만 추가하면 되는 구조 |
| **팀 협업** | 레이어별 독립 작업 가능 (화면/비즈니스 로직/데이터 분리) |

#### Hilt (DI)

| 기준 | 근거 |
|------|------|
| **Android 통합** | `@AndroidEntryPoint`, `@HiltViewModel` 등 Android 생명주기 통합 |
| **보일러플레이트 감소** | Dagger 대비 설정 코드 최소화 |
| **스코프 관리** | `SingletonComponent` → 앱 전역 싱글톤으로 Firebase, Repository, DataSource 관리 |

#### Firebase (Serverless Backend)

| 기준 | 근거 |
|------|------|
| **개발 속도** | 별도 백엔드 서버 구축 없이 즉시 사용 |
| **실시간 지원** | Firestore `addSnapshotListener`로 실시간 데이터 구독 |
| **인증 통합** | Firebase Auth + Google Sign-In 원스텝 통합 |
| **비용** | 소규모 프로젝트에서 무료 티어 활용 가능 |
| **팀 규모** | Android 4~5명 팀에서 백엔드 별도 인력 없이 운영 가능 |

#### Preferences DataStore

| 기준 | 근거 |
|------|------|
| **비정상 종료 복구** | 5초마다 학습 세션을 로컬에 백업 → 크래시 시 복구 |
| **SharedPreferences 대체** | 코루틴 기반 비동기 I/O, 데이터 일관성 보장 |
| **경량 데이터** | 학습 세션(6개 필드) 정도의 소량 데이터에 적합 |

#### Type-safe Navigation (kotlinx.serialization)

| 기준 | 근거 |
|------|------|
| **타입 안전성** | Route를 `@Serializable data class`로 정의 → 컴파일 타임 검증 |
| **복잡한 파라미터** | `StudyResult` Route처럼 `List<String>` 등 복합 타입 전달 가능 |
| **리팩토링 안정성** | 문자열 키 기반 대비 안전한 화면 전환 |

---

## 2. 아키텍처 개요

### 2-1. 3계층 Clean Architecture

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
│   순수 도메인 모델     성공/실패 래퍼                      │
├─────────────────────────────────────────────────────────┤
│                      Data Layer                         │
│                                                         │
│   Repository (Impl)     DataSource                      │
│   데이터 조합/변환       Remote (Firebase)                │
│                          Local (DataStore)              │
│   Mapper                                                │
│   DTO ↔ Domain 변환                                     │
└─────────────────────────────────────────────────────────┘
```

### 2-2. 단방향 데이터 흐름 (UDF)

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

### 2-3. 패키지 구조 (Feature-based)

```
com.a32b.plant
│
├── core/                          # 공통 인프라
│   ├── base/BaseViewModel.kt     # isLoading StateFlow 관리
│   ├── extension/TimeExt.kt      # Long → 시:분:초 변환
│   ├── navigation/               # Routes (sealed interface) + NavHost
│   └── util/                     # TimeFormatter, RunCatchingUtils
│
├── data/                          # 데이터 레이어
│   ├── mapper/                   # DTO ↔ Domain (6개 매퍼)
│   ├── model/                    # Firestore DTO (7개)
│   ├── repository/               # Repository 구현체 (6개)
│   ├── session/                  # SessionExpiredEvent
│   └── source/
│       ├── local/                # StudyingLocalDataSource, SettingsLocalDataSource (DataStore)
│       └── remote/               # Firebase DataSource (6개)
│           ├── auth/
│           ├── community/
│           ├── pot/
│           ├── studying/
│           ├── StudyLog/
│           └── user/
│
├── di/                            # Hilt DI
│   ├── data/
│   │   ├── FirebaseModule.kt     # FirebaseAuth, Firestore, Functions
│   │   ├── DataSourceModule.kt   # DataSource 바인딩 (7개)
│   │   ├── RepositoryModule.kt   # Repository 바인딩 (6개)
│   │   ├── SessionModule.kt      # SessionExpiredEvent 바인딩
│   │   └── CoroutineModule.kt    # ApplicationScope (SupervisorJob + IO)
│   ├── qualifier/
│   └── CurrentUser.kt            # 전역 싱글톤 (제거 예정 브릿지)
│
├── domain/                        # 도메인 레이어
│   ├── error/AppError.kt         # sealed class (11종)
│   ├── model/                    # 순수 도메인 모델 (10개)
│   ├── repository/               # Repository 인터페이스 (6개)
│   ├── result/Result.kt          # Success<T> / Failure(AppError)
│   ├── session/                  # Notifier / Observer 인터페이스
│   ├── type/                     # PlantLevel, ActivityType, ItemType
│   └── usecase/                  # 비즈니스 로직 (30개)
│       ├── auth/        (8개)
│       ├── community/   (4개)
│       ├── mypage/      (4개)
│       ├── pot/         (6개)
│       ├── session/     (1개)
│       ├── studyLog/    (3개)
│       └── studying/    (4개)
│
└── presentation/                  # 프레젠테이션 레이어
    ├── auth/                     # SignIn, SignUp (Screen + ViewModel)
    ├── community/                # List, Post, Detail, Activity
    ├── core/component/           # BottomBar, Dialog, Tag, Logo, ProfileImage
    ├── home/                     # HomeScreen, AttendanceCheckDialog
    ├── mypage/                   # Mypage, DeleteAccount
    ├── pot/                      # NewBornTree, PotList
    ├── report/                   # ReportScreen
    ├── splash/                   # SplashViewModel
    ├── studyPlanDetail/          # StudyPlanDetailScreen
    ├── studying/                 # StudyingScreen, StudyResultScreen
    └── theme/                    # Color, Type, Theme
```

---

## 3. 레이어별 데이터 흐름

### 3-1. 읽기 흐름 (조회)

```
Compose Screen
  │ collectAsState()
  │
  ▼
ViewModel (StateFlow<UiState>)
  │ viewModelScope.launch { }
  │
  ▼
UseCase
  │ invoke()
  │
  ▼
Repository Interface
  │ (Domain Layer에서 정의)
  │
  ▼
Repository Impl (Data Layer)
  │
  ├─▶ RemoteDataSource
  │     └─ Firestore query / snapshot
  │
  └─▶ Mapper
        └─ DTO → Domain Model
  │
  ▼
Result<T>
  │
  ▼ (역방향으로 전파)
ViewModel → UiState 갱신 → UI 리컴포지션
```

### 3-2. 쓰기 흐름 (생성/수정/삭제)

```
UI 이벤트 (버튼 탭)
  │
  ▼
ViewModel
  │ viewModelScope.launch { }
  │
  ▼
UseCase
  │ ├─ EnsureCurrentUserUseCase()  ← 세션 가드 (거의 모든 UseCase)
  │ ├─ 비즈니스 검증
  │ └─ Repository 호출
  │
  ▼
Repository Impl
  │
  ├─▶ Mapper (Domain Model → DTO)
  │
  ├─▶ RemoteDataSource
  │     └─ Firestore set / update / delete
  │
  └─▶ LocalDataSource (필요 시)
        └─ DataStore edit
  │
  ▼
Result<Unit> or Result<String>
  │
  ▼
ViewModel → 성공: 화면 이동 or UiState 갱신
           → 실패: 에러 메시지 표시
```

### 3-3. 실시간 구독 흐름 (Flow)

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

## 4. 핵심 시스템 플로우

### 4-1. 인증 시스템

```
┌──────────────────────────────────────────────────────────────────┐
│                        인증 시스템 전체 구조                       │
│                                                                 │
│  ┌──────────┐   idToken  ┌──────────┐   uid     ┌─────────────┐ │
│  │ Google   │ ──────────▶│ Firebase │──────────▶│ Firestore   │ │
│  │Credential│            │   Auth   │           │ users/{uid} │ │
│  │ Manager  │            └──────────┘           └──────┬──────┘ │
│  └──────────┘                                          │        │
│                                                        ▼        │
│                                                 ┌─────────────┐ │
│                                                 │  nicknames/ │ │
│                                                 │ {nickname}  │ │
│                                                 └─────────────┘ │
└──────────────────────────────────────────────────────────────────┘

흐름:
1. Credential Manager → Google idToken 획득
2. Firebase Auth → idToken으로 인증 → uid 발급
3. Firestore users/{uid} 문서 조회 or 생성
4. nicknames/{nickname} 중복 검사 + 등록
5. UserRepository.startUserSession(user) → 실시간 구독 시작
```

### 4-2. 학습 시스템

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         학습 시스템 데이터 흐름                            │
│                                                                         │
│   StudyingScreen                                                        │
│       │                                                                 │
│       ├── 시작 ──▶ studying/{uid} 문서 생성 (Firestore)                   │
│       │            + DataStore 세션 저장                                 │
│       │                                                                 │
│       │         ┌──────── 5초마다 ────────┐                              │
│       ├── 진행 ─┤                         │                              │
│       │         └──────── 1분마다 ────────┤                              │
│       │                                   │                             │
│       │              DataStore             Firestore                    │
│       │              (로컬 백업)           studying/{uid}                │
│       │              userId, potId,       .studyingTime 갱신            │
│       │              tag, title,                                        │
│       │              time, log                   ┌────────────────┐     │
│       │                                          │ 다른 유저가     │     │
│       │                                          │ 실시간 구독     │     │
│       │                                          │ (같은 태그)     │     │
│       │                                          └────────────────┘     │
│       │                                                                 │
│       └── 종료 ──▶ 병렬 실행 (async/awaitAll)                             │
│                    ├─ logs/{logId} 생성                                  │
│                    ├─ pots/{potId}.potTotalStudyingTime 갱신             │
│                    └─ users/{uid}.totalStudyTime 갱신                    │
│                                                                         │
│                    + studying/{uid} 문서 삭제                            │
│                    + DataStore 클리어                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4-3. 커뮤니티 시스템

```
┌──────────────────────────────────────────────────────────────────┐
│                     커뮤니티 데이터 흐름                            │
│                                                                  │
│   게시글 작성                                                     │
│   ├─ posts/{postId} 문서 생성                                     │
│   └─ activities/{activityId} 문서 생성 (type="게시물")             │
│                                                                  │
│   좋아요 토글                                                     │
│   ├─ posts/{postId}.likedBy[] 배열 추가/제거                       │
│   ├─ posts/{postId}.likeCount +1/-1                              │
│   └─ activities/{activityId} 생성/삭제 (type="좋아요")             │
│                                                                  │
│   댓글 작성                                                       │
│   ├─ posts/{postId}/comments/{commentId} 문서 생성                │
│   ├─ posts/{postId}.commentCount +1                              │
│   └─ activities/{activityId} 문서 생성 (type="댓글")               │
│                                                                  │
│   페이지네이션 (게시글 목록)                                        │
│   ├─ cursor: 마지막 게시글의 createdAt (epoch millis)              │
│   ├─ Firestore: orderBy("createdAt", DESC).startAfter(cursor)    │
│   └─ 반환: PostPage(items, nextCursor, hasMore)                   │
│                                                                  │
│   북마크 토글 📋                                                  │
│   ├─ posts/{postId}.bookmarkedBy[] 배열 추가/제거                  │
│   └─ posts/{postId}.bookmarkCount +1/-1                          │
│                                                                  │
│   검색 (클라이언트 측)                                             │
│   ├─ getAllPosts() → 전체 로드                                    │
│   └─ title/content.contains(query) 필터링                         │
└──────────────────────────────────────────────────────────────────┘
```

### 4-4. 화분 성장 시스템

```
┌──────────────────────────────────────────────────────────────────┐
│                     화분 성장 데이터 흐름                          │
│                                                                  │
│   학습 종료                                                       │
│       │                                                          │
│       ▼                                                          │
│   pots/{potId}.potTotalStudyingTime += studyTime                 │
│       │                                                          │
│       ▼                                                          │
│   Pot.level (computed property)                                  │
│   ┌─────────────────────────────────────┐                        │
│   │ hours = totalTime / 3600000.0       │                        │
│   │                                     │                        │
│   │ ≥500h → Lv.6   ≥200h → Lv.5       │                          │
│   │ ≥90h  → Lv.4   ≥40h  → Lv.3       │                          │
│   │ ≥15h  → Lv.2   ≥5h   → Lv.1       │                          │
│   │ <5h   → Lv.0                        │                        │
│   └─────────────────────────────────────┘                        │
│       │                                                          │
│       ▼                                                          │
│   UI: tagId + level → 화분 이미지 결정                             │
│   (7종류 화분 × 7단계 레벨 = 최대 49종 이미지)                       │
│                                                                  │
│   [미구현] 아이템 소비 조건                                         │
│   ├─ 시간 충족 + 아이템 보유 시 레벨업 가능                          │
│   └─ User.item 에서 차감                                          │
└──────────────────────────────────────────────────────────────────┘
```

### 4-5. 출석체크 시스템

```
┌──────────────────────────────────────────────────────────────────┐
│                    출석체크 데이터 흐름                            │
│                                                                  │
│   HomeScreen → "출석하기" 탭                                      │
│       │                                                          │
│       ▼                                                          │
│   UserRepository.checkAttendance(uid)                            │
│       │                                                          │
│       ▼                                                          │
│   DailyCheckThisMonth.decideNext(nowKst: LocalDate)              │
│       │                                                          │
│       ├─ lastCheckedAt == today → AlreadyChecked                 │
│       ├─ count >= 28 (같은 달) → MonthCompleted                   │
│       └─ Success(newCount, reward)                               │
│              │                                                   │
│              ├─ Firestore Transaction:                           │
│              │   users/{uid}.monthCheck.count = newCount         │
│              │   users/{uid}.monthCheck.lastCheckedAt = now      │
│              │                                                   │
│              └─ reward != null → 보상 지급                        │
│                  AttendanceRewardTable.of(count)                 │
│                  ├─ Coin(amount) → users/{uid}.coin += amount    │
│                  └─ ItemReward(type, amt)                        │
│                      → users/{uid}.item.{fieldKey} += amount     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 5. DI 의존성 그래프

### 5-1. Hilt 모듈 구성

```
SingletonComponent (앱 전역)
│
├── FirebaseModule (object, @Provides)
│   ├── FirebaseAuth
│   ├── FirebaseFirestore
│   └── FirebaseFunctions
│
├── CoroutineModule (object, @Provides)
│   └── @ApplicationScope CoroutineScope
│       (SupervisorJob + Dispatchers.IO)
│
├── DataSourceModule (abstract, @Binds)
│   ├── AuthRemoteDataSource      ← AuthRemoteDataSourceImpl
│   ├── CommunityRemoteDataSource ← CommunityRemoteDataSourceImpl
│   ├── PotRemoteDataSource       ← PotRemoteDataSourceImpl
│   ├── StudyingRemoteDataSource  ← StudyingRemoteDataSourceImpl
│   ├── UserRemoteDataSource      ← UserRemoteDataSourceImpl
│   ├── StudyLogRemoteDataSource  ← StudyLogRemoteDataSourceImpl
│   ├── StudyingLocalDataSource   ← StudyingLocalDataSourceImpl
│   └── SettingsLocalDataSource   ← SettingsLocalDataSourceImpl
│
├── RepositoryModule (abstract, @Binds)
│   ├── AuthRepository            ← AuthRepositoryImpl
│   ├── CommunityRepository       ← CommunityRepositoryImpl
│   ├── PotRepository             ← PotRepositoryImpl
│   ├── StudyingRepository        ← StudyingRepositoryImpl
│   ├── UserRepository            ← UserRepositoryImpl
│   └── StudyLogRepository        ← StudyLogRepositoryImpl
│
└── SessionModule (abstract, @Binds)
    ├── SessionExpiredNotifier     ← SessionExpiredEvent
    └── SessionExpiredObserver     ← SessionExpiredEvent (같은 인스턴스)
```

### 5-2. ViewModel 의존성

```
@HiltViewModel
├── SplashViewModel
│   ├── CheckAutoLoginUseCase
│   └── ObserveDarkModeUseCase (DataStore 구독)
│
├── SignInViewModel
│   └── SignInWithGoogleUseCase
│       └── ResolveUserSessionUseCase
│
├── SignUpViewModel
│   └── SetNicknameUseCase
│
├── HomeViewModel
│   ├── PotRepository (getPots)
│   └── UserRepository (currentUser)
│
├── StudyingViewModel
│   ├── StartStudyingSessionUseCase
│   ├── UpdateLocalStudyingSessionUseCase
│   ├── ClearStudyingSessionUseCase
│   └── StudyingRepository (observeStudyingUser)
│
├── StudyResultViewModel
│   └── FinishStudyingUseCase
│
├── CommunityListViewModel
│   └── CommunityRepository (loadPostPage, getTags)
│
├── CommunityPostViewModel
│   └── CreatePostUseCase
│
├── CommunityDetailViewModel
│   ├── CommunityRepository (getPostDetail, getComments)
│   ├── ToggleLikeUseCase
│   └── AddCommentUseCase
│
├── CommunityActivityViewModel
│   └── CommunityRepository (observeActivity)
│
├── StudyPlanDetailViewModel
│   ├── GetStudyLogsUseCase / GetSelectedStudyLogUseCase
│   ├── DeleteStudyLogUseCase
│   ├── UpdatePotNameUseCase
│   ├── DeleteEntirePotUseCase
│   └── CompleteStudyPlanUseCase
│
├── NewBornTreeViewModel
│   └── PotRepository (getAvailableTags, addPot)
│
├── MyPageViewModel
│   ├── UpdateProfileUseCase
│   ├── UpdateDarkModeUseCase
│   ├── ObserveDarkModeUseCase
│   ├── GetProfileImageLevelListUseCase
│   └── SignOutUseCase
│
└── DeleteAccountViewModel
    └── DeleteAccountUseCase
```

---

## 6. 데이터 동기화 전략

### 6-1. 쓰기 전략

| 데이터 | 쓰기 방식 | 설명 |
|--------|----------|------|
| 학습 세션 (진행 중) | 이중 저장 | 5초마다 로컬 DataStore + 1분마다 Firestore |
| 학습 완료 | 병렬 쓰기 | `async/awaitAll`로 3개 필드 동시 갱신 |
| 게시글 + 활동 | 순차 쓰기 | 게시글 저장 → activityId를 게시글에 반영 |
| 좋아요 | 토글 쓰기 | `likedBy[]` 배열 원자적 추가/제거 + 카운트 갱신 |
| 출석체크 | Transaction | 중복 보상 방지를 위한 Firestore Transaction |
| 닉네임 변경 | 롤백 가능 쓰기 | 새 닉네임 등록 → 이전 삭제 → 프로필 갱신, 실패 시 롤백 |

### 6-2. 읽기 전략

| 데이터 | 읽기 방식 | 설명 |
|--------|----------|------|
| 현재 유저 | 실시간 구독 (StateFlow) | `UserRepository.currentUser` 앱 전역 |
| 화분 목록 | 실시간 구독 (Flow) | Firestore `addSnapshotListener` |
| 게시글 목록 | 커서 페이지네이션 | `createdAt` 기반 커서, pull-to-refresh |
| 게시글 상세/댓글 | 단건 조회 | pull-to-refresh로 갱신 |
| 학습 중 유저 | 실시간 구독 (Flow) | 같은 태그 기준 Firestore 구독 |
| 학습 기록 | 실시간 구독 (Flow) | 서브컬렉션 전체 구독 |
| 검색 | 전체 로드 + 클라이언트 필터 | Firestore 전문 검색 미지원 |

### 6-3. 로컬 ↔ 리모트 동기화

```
┌─────────────┐                     ┌─────────────┐
│  DataStore  │                     │  Firestore   │
│  (로컬)     │                     │  (리모트)     │
│             │                    │              │
│  학습 세션   │ ◀── 5초마다 ──────  │              │
│  userId     │                    │  studying/   │
│  potId      │ ─── 1분마다 ────▶   │  {uid}       │
│  tag        │                    │  .studyTime  │
│  title      │                    │              │
│  time       │                    │              │
│  log        │                    │              │
│             │                    │              │
│  다크모드    │                    │              │
│  (기기 설정) │  ← DataStore가     │              │
│             │    Source of Truth │              │
└─────────────┘                    └─────────────┘

원칙:
- Firestore가 Single Source of Truth
- DataStore는 비정상 종료 복구 + 오프라인 캐시용
- 충돌 시 Firestore 우선
```

---

## 7. 에러 처리 아키텍처

### 7-1. Result<T> 패턴

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

### 7-2. 에러 전파 경로

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

### 7-3. AppError 분류 및 대응

| 에러 타입 | 사용자 대응 | 시스템 대응 |
|-----------|-----------|-----------|
| `Network` | "네트워크 확인" 안내 | 재시도 가능 |
| `Auth` | "다시 로그인" 안내 | 세션 클리어 |
| `UnknownUser` | 세션 만료 다이얼로그 | 로그인 화면 이동 |
| `Server` | "잠시 후 다시 시도" | 재시도 가능 |
| `Permission` | 권한 안내 | Firestore 규칙 확인 |
| `Custom(msg)` | 동적 메시지 표시 | 비즈니스 로직별 분기 |
| `Local` | 무시 또는 재시도 | DataStore 에러 로깅 |

---

## 8. 세션 관리 아키텍처

### 8-1. 세션 생명주기

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
                   ├─ Firestore users/{uid} 실시간 구독 시작
                   └─ CurrentUser 전역 싱글톤 동기화 (브릿지)
                        │
                        ▼
                   앱 사용 중 (currentUser 실시간 반영)
                        │
     ┌──────────────────┼──────────────────┐
     │                  │                  │
     ▼                  ▼                  ▼
  로그아웃           회원탈퇴          세션 만료
     │                  │                  │
     ▼                  ▼                  ▼
  endUserSession()   endUserSession()   notifySessionExpired()
  signOut()          deleteUserData()   → MainActivity 감지
  CurrentUser.clear() deleteAuthAccount()→ signOut()
     │               CurrentUser.clear()  → SignInScreen
     ▼                  │
  SignInScreen          ▼
                     SignInScreen
```

### 8-2. EnsureCurrentUser 가드 패턴

```
모든 쓰기 UseCase
  │
  ▼
EnsureCurrentUserUseCase()
  │
  ├─ currentUser != null → Result.Success(user) → UseCase 계속 실행
  │
  └─ currentUser == null → notifySessionExpired()
                           → Result.Failure(UnknownUser)
                           → UseCase 즉시 종료
                           → MainActivity에서 이벤트 수신
                           → 로그인 화면으로 이동
```

### 8-3. SessionExpiredEvent 구조

```
┌───────────────────┐        ┌────────────────────┐         ┌──────────────┐
│  UseCase          │        │  SessionExpired     │        │  MainActivity│
│  (Domain Layer)   │        │  Event              │        │  (UI Layer)  │
│                   │        │  (Data Layer)       │        │              │
│  Notifier         │──emit─▶│  MutableSharedFlow  │──collect─▶ Observer   │
│  .notifyExpired() │        │  <Unit>             │        │  .event      │
└───────────────────┘        └────────────────────┘         └──────────────┘

Hilt 바인딩:
SessionExpiredEvent 하나의 인스턴스가
├── SessionExpiredNotifier (Domain Interface)
└── SessionExpiredObserver (Domain Interface)
두 인터페이스를 모두 구현 (같은 @Singleton)
```

### 8-4. CurrentUser 전역 싱글톤 (과도기 브릿지)

```
현재 구조 (과도기):
┌──────────────────┐     ┌──────────────────┐
│  UserRepository  │     │   CurrentUser    │
│  .currentUser    │     │   (전역 싱글톤)   │
│  (StateFlow)     │     │   object         │
│                  │     │   uid, nickname, │
│  ◀── 정석 경로    │     │   profileImg     │
│                  │     │                  │
│                  │     │   ◀── 레거시 경로 │
└──────────────────┘     └──────────────────┘

로그인/닉네임/프로필 변경 시 양쪽 모두 동기화.
TODO: 모든 화면이 UserRepository로 전환 완료 시
      CurrentUser.kt 삭제 + 관련 .set()/.clear() 호출 제거
```

---

## 관련 문서

| 문서 | 경로 |
|------|------|
| PRD | [docs/PRD.md](PRD.md) |
| 기능명세서 | [docs/FEATURE_SPEC.md](FEATURE_SPEC.md) |
| User Flow | [docs/USER_FLOW.md](USER_FLOW.md) |
| 화면명세 | docs/SCREEN_SPEC.md (작성 예정) |
| 데이터 모델 | [docs/DATA_MODEL.md](DATA_MODEL.md) |
