# Plant 기능명세서 (Feature Specification)

> **최초 작성일**: 2026-09-11  
> **최종 수정일**: 2026-09-15

## 목차

1. [인증 · 계정 관리](#1-인증--계정-관리)
2. [홈 · 화분 관리](#2-홈--화분-관리)
3. [학습 (Studying)](#3-학습-studying)
4. [학습 기록](#4-학습-기록)
5. [커뮤니티](#5-커뮤니티)
6. [마이페이지](#6-마이페이지)
7. [출석체크](#7-출석체크)
8. [아이템 · 보상 · 상점](#8-아이템--보상--상점)
9. [리포트 · 통계](#9-리포트--통계)
10. [알림](#10-알림)

---

## 1. 인증 · 계정 관리

### 1-1. 자동 로그인 (세션 복원) ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | AUTH-001 |
| **진입점** | 앱 실행 → `SplashScreen` |
| **UseCase** | `CheckAutoLoginUseCase` |
| **Repository** | `AuthRepository.currentUid()`, `UserRepository.getUser()` |

**동작 흐름**

1. `SplashScreen` 진입 시 `CheckAutoLoginUseCase` 호출
2. `AuthRepository.currentUid()`로 Firebase 세션의 uid 확인
   - uid가 `null` → `AutoLoginResult.NotLoggedIn` 반환 → `SignInScreen`으로 이동
3. uid가 존재하면 `UserRepository.getUser(uid)`로 Firestore 유저 문서 조회
   - 유저 문서가 `null`이거나 `isFirstLogin == true` → `authRepository.signOut()` 호출 후 `NotLoggedIn` 반환
4. 유저 문서가 정상이면:
   - `UserRepository.startUserSession(user)` → 실시간 구독 시작
   - `CurrentUser.set(...)` → 전역 싱글톤 세팅 (과도기 브릿지)
   - `AutoLoginResult.LoggedIn(uid, user)` 반환 → `HomeScreen`으로 이동

**에러 처리**

| 에러 상황 | 처리 |
|-----------|------|
| Firestore 조회 실패 | `Result.Failure` 반환 → 로그인 화면으로 이동 |
| 세션은 있으나 유저 문서 없음 | `signOut()` 후 로그인 화면 |

---

### 1-2. Google 로그인 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | AUTH-002 |
| **진입점** | `SignInScreen` → Google 로그인 버튼 |
| **UseCase** | `SignInWithGoogleUseCase` → `ResolveUserSessionUseCase` |
| **Repository** | `AuthRepository.signInWithGoogle()`, `UserRepository.getUser()`, `UserRepository.createUser()` |

**동작 흐름**

1. 사용자가 Google 로그인 버튼 탭
2. Android Credential Manager를 통해 Google 계정 선택 → `idToken` 추출
3. `SignInWithGoogleUseCase(idToken)` 호출
4. `AuthRepository.signInWithGoogle(idToken)` → Firebase Auth 인증 → uid 반환
5. `ResolveUserSessionUseCase(uid)` 호출:
   - `UserRepository.getUser(uid)`로 기존 유저 확인
   - 기존 유저 없으면 → `UserRepository.createUser(uid)` (신규 문서 생성, `isFirstLogin = true`)
   - `UserRepository.startUserSession(user)` → 실시간 구독 시작
   - `CurrentUser.set(...)` → 전역 싱글톤 세팅
   - `SignInResult(uid, nickname, isFirstLogin)` 반환
6. 반환된 `isFirstLogin` 값에 따라:
   - `true` → `SignUpScreen`(닉네임 설정 화면)으로 이동
   - `false` → `HomeScreen`으로 이동

**에러 처리**

| 에러 상황 | 처리 |
|-----------|------|
| Google 계정 선택 취소 | 로그인 화면 유지 |
| Firebase Auth 실패 | `AppError.Auth` → 토스트 메시지 |
| Firestore 문서 생성 실패 | `AppError.Server` → 토스트 메시지 |

---

### 1-3. 첫 로그인 닉네임 설정 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | AUTH-003 |
| **진입점** | `SignUpScreen` (Google 로그인 후 `isFirstLogin == true`일 때) |
| **UseCase** | `SetNicknameUseCase` |
| **Repository** | `UserRepository.isNicknameTaken()`, `UserRepository.registerNickname()`, `UserRepository.completeFirstLogin()` |

**동작 흐름**

1. 닉네임 입력 필드에 텍스트 입력
2. "확인" 버튼 탭 → `SetNicknameUseCase(uid, nickname)` 호출
3. **중복 검사**: `UserRepository.isNicknameTaken(nickname)` → `nicknames/{nickname}` 문서 존재 여부 확인
   - 중복이면 → `AppError.Custom("사용 중인 닉네임입니다.")` 반환
4. **닉네임 등록**: `UserRepository.registerNickname(nickname)` → `nicknames/{nickname}` 문서 생성
5. **첫 로그인 완료**: `UserRepository.completeFirstLogin(uid, nickname)` → `users/{uid}` 문서의 `isFirstLogin = false`, `nickname` 갱신
6. `CurrentUser` 전역 싱글톤 동기화
7. `HomeScreen`으로 이동

**입력 유효성 검증**

| 규칙 | 조건 |
|------|------|
| 글자 수 | 2자 이상, 10자 이하 |
| 중복 불가 | `nicknames` 컬렉션에서 동일 닉네임 존재 시 차단 |

---

### 1-4. 로그아웃 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | AUTH-004 |
| **진입점** | `MyPageScreen` → 로그아웃 버튼 |
| **UseCase** | `SignOutUseCase` |

**동작 흐름**

1. 로그아웃 버튼 탭 → 확인 다이얼로그 표시
2. "확인" 선택 → `SignOutUseCase()` 호출
3. 처리 순서 (**순서 중요** — 리스너 크래시 방지):
   1. `UserRepository.endUserSession()` → Firestore 실시간 구독 해제, `currentUser` 비움
   2. `AuthRepository.signOut()` → Firebase Auth 세션 종료
   3. `CurrentUser.clear()` → 전역 싱글톤 초기화
4. `SignInScreen`으로 이동

> ⚠️ `signOut()`은 항상 성공하므로 `Result`를 반환하지 않음

---

### 1-5. 회원탈퇴 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | AUTH-005 |
| **진입점** | `MyPageScreen` → 회원탈퇴 버튼 |
| **UseCase** | `DeleteAccountUseCase` |
| **ViewModel** | `DeleteAccountViewModel` |

**동작 흐름**

1. 회원탈퇴 버튼 탭 → **2단계 확인**:
   - 1단계: "정말 탈퇴하시겠습니까?" 다이얼로그
   - 2단계: 최종 확인 다이얼로그
2. "확인" 선택 → `DeleteAccountUseCase()` 호출
3. `EnsureCurrentUserUseCase()`로 현재 유저 정보 조회
4. 삭제 순서:
   1. `UserRepository.deleteUserData(uid)` → Firestore `users/{uid}` 및 하위 문서 삭제
   2. `UserRepository.deleteNickname(nickname)` → `nicknames/{nickname}` 문서 삭제
   3. `UserRepository.endUserSession()` → 실시간 구독 해제
   4. `CurrentUser.clear()` → 전역 싱글톤 초기화
   5. `AuthRepository.deleteAuthAccount()` → Firebase Auth 계정 삭제 (마지막에 실행)
5. `SignInScreen`으로 이동

**에러 처리**

| 에러 상황 | 처리 |
|-----------|------|
| 유저 정보 없음 | `AppError.UnknownUser` → 세션 만료 이벤트 발송 |
| Firestore 삭제 실패 | `Result.Failure` → 에러 메시지 표시, Auth 계정은 삭제하지 않음 |
| Auth 삭제 실패 | 재인증 필요 가능성 → 에러 메시지 표시 |

---

### 1-6. 세션 만료 관리 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | AUTH-006 |
| **관련 클래스** | `SessionExpiredNotifier`, `SessionExpiredObserver`, `SessionExpiredEvent` |
| **UseCase** | `EnsureCurrentUserUseCase` |

**동작 흐름**

1. 모든 UseCase는 실행 전 `EnsureCurrentUserUseCase()`를 호출하여 현재 유저 확인
2. `UserRepository.currentUser.value`가 `null`이면:
   - `SessionExpiredNotifier.notifySessionExpired()` 호출
   - `AppError.UnknownUser` 반환
3. `SessionExpiredObserver`를 구독하고 있는 `MainActivity`에서 이벤트 수신
4. 세션 만료 다이얼로그 표시 → 로그인 화면으로 이동

---

## 2. 홈 · 화분 관리

### 2-1. 홈 화면 (대표 화분 표시) ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | HOME-001 |
| **화면** | `HomeScreen` |
| **ViewModel** | `HomeViewModel` |
| **Repository** | `PotRepository.getPots()`, `UserRepository.currentUser` |

**동작 흐름**

1. `HomeScreen` 진입 시 `PotRepository.getPots(uid)` Flow 구독 → 화분 목록 실시간 수신
2. `UserRepository.currentUser`에서 `lastSelectedPotId` 확인
3. `lastSelectedPotId`에 해당하는 화분을 대표 화분으로 표시
   - 선택된 화분이 없으면 `Pot.EMPTY` (빈 상태, "화분을 추가해주세요" 메시지)
4. 대표 화분의 성장 이미지(레벨에 따라 변경), 이름, 태그, 누적 학습 시간 표시

**표시 정보**

| 요소 | 데이터 소스 |
|------|------------|
| 화분 이미지 | `tagId` + `level` 조합으로 결정 |
| 화분 이름 | `Pot.name` |
| 태그 라벨 | `Pot.tagName` |
| 누적 학습 시간 | `Pot.potTotalStudyingTime` (밀리초 → 시:분:초 포맷) |
| 레벨 | `Pot.level` (누적 시간 기반 자동 계산) |

---

### 2-2. 화분 생성 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | POT-001 |
| **화면** | `NewBornTreeScreen` |
| **ViewModel** | `NewBornTreeViewModel` |
| **Repository** | `PotRepository.getAvailableTags()`, `PotRepository.addPot()` |

**동작 흐름**

1. 홈 화면에서 "화분 추가" 버튼 탭 → `NewBornTreeScreen` 이동
2. **1단계 — 태그 선택**:
   - `PotRepository.getAvailableTags()` Flow 구독 → 목표 유형 태그 목록 표시
   - 태그는 2단계 구조: 상위 카테고리 → 하위 세부 태그
   - 상위 태그 선택 → 하위 태그 목록 표시 → 하위 태그 선택
3. **2단계 — 이름 입력**:
   - 화분 이름 입력 필드 표시
   - 최대 20자 제한
4. "생성" 버튼 탭 → `PotRepository.addPot(uid, tag, name)` 호출
5. Firestore `users/{uid}/pots/{potId}` 문서 생성:
   - `id`: 자동 생성 문서 ID
   - `tag_id`, `tag_name`: 선택된 태그 정보
   - `name`: 입력한 이름
   - `imageUrl`: 태그 기반 기본 이미지 URL
   - `potTotalStudyingTime`: 0
   - `createdAt`: 서버 타임스탬프
   - `isCompleted`: false
6. 생성 완료 → 홈 화면으로 복귀, 생성된 화분이 대표 화분으로 설정

**입력 유효성 검증**

| 규칙 | 조건 |
|------|------|
| 태그 | 반드시 하위 태그까지 선택 필요 |
| 이름 | 1자 이상, 20자 이하 |

---

### 2-3. 화분 목록 조회 (나의 정원) ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | POT-002 |
| **화면** | `PotListScreen` |
| **UseCase** | `GetPotListUseCase`, `GetActivePotUseCase` |
| **Repository** | `PotRepository.getPots()`, `PotRepository.getUserPotsByStatus()` |

**동작 흐름**

1. 홈 → "나의 정원" 탭 → `PotListScreen` 이동
2. **탭 필터** (3종):
   - **전체**: 모든 화분 표시
   - **공부 중**: `isCompleted == false`인 화분만 표시
   - **완료**: `isCompleted == true`인 화분만 표시
3. `PotRepository.getPots(uid)` Flow 구독 → 실시간 목록 갱신
4. 각 화분 카드에 표시되는 정보:
   - 화분 이미지 (레벨 기반)
   - 화분 이름
   - 태그명
   - 누적 학습 시간
   - 레벨 표시
5. 화분 카드 탭 → `StudyPlanDetailScreen`으로 이동

**정렬 기준 (5종)** 📋

| 정렬 | 필드 | 방향 | 기본값 |
|------|------|:----:|:------:|
| 생성일 최신순 | `createdAt` | DESC | 공부 중 탭 기본 |
| 누적 학습 시간순 | `potTotalStudyingTime` | DESC | 기른 화분 탭 기본 |
| 레벨순 | `level` | DESC | |
| 이름순 (가나다) | `name` | ASC | |
| 최근 학습순 | `lastStudiedAt` | DESC | 신규 필드 추가 필요 |

---

### 2-4. 대표 화분 선택/변경 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | POT-003 |
| **Repository** | `UserRepository.updateLastSelectedPot()` |

**동작 흐름**

1. 화분 목록 또는 화분 상세에서 "대표 화분 선택" 동작
2. `UserRepository.updateLastSelectedPot(uid, potId)` 호출
3. Firestore `users/{uid}.lastSelectedPotId` 필드 갱신
4. 홈 화면 복귀 시 새 대표 화분이 즉시 반영

---

### 2-5. 화분 이름 변경 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | POT-004 |
| **UseCase** | `UpdatePotNameUseCase` |
| **Repository** | `PotRepository.updatePotName()` |

**동작 흐름**

1. `StudyPlanDetailScreen`에서 화분 이름 옆 편집 버튼 탭
2. 이름 수정 다이얼로그 표시
3. 새 이름 입력 (최대 20자) → "확인" 탭
4. `PotRepository.updatePotName(uid, potId, newName)` 호출
5. Firestore `users/{uid}/pots/{potId}.name` 필드 갱신

---

### 2-6. 화분 삭제 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | POT-005 |
| **UseCase** | `DeleteEntirePotUseCase` |
| **Repository** | `PotRepository.deleteEntirePot()` |

**동작 흐름**

1. `StudyPlanDetailScreen`에서 삭제 버튼 탭 → 확인 다이얼로그
2. "확인" 선택 → `DeleteEntirePotUseCase(uid, potId, totalStudyingTime)` 호출
3. 삭제 처리:
   1. `users/{uid}/pots/{potId}` 문서 및 하위 `logs` 서브컬렉션 전체 삭제
   2. `users/{uid}.totalStudyTime`에서 해당 화분의 누적 시간 차감
4. 화분 목록 화면으로 복귀

**비즈니스 규칙**

- 삭제 시 해당 화분의 누적 학습 시간이 유저 총 학습 시간에서 차감됨
- 삭제된 화분이 대표 화분이었을 경우 → `lastSelectedPotId`가 비워짐 → 홈에서 `Pot.EMPTY` 표시

---

### 2-7. 화분 학습 완료 처리 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | POT-006 |
| **UseCase** | `CompleteStudyPlanUseCase` |
| **Repository** | `PotRepository.completeStudyPlan()` |

**동작 흐름**

1. `StudyPlanDetailScreen`에서 "학습 완료" 버튼 탭 → 확인 다이얼로그
2. "확인" 선택 → `CompleteStudyPlanUseCase(uid, potId)` 호출
3. Firestore 갱신:
   - `users/{uid}/pots/{potId}.isCompleted` → `true`
   - `users/{uid}/pots/{potId}.completedAt` → 서버 타임스탬프
   - `users/{uid}.completedPotsCount` → +1 증가
4. 화분이 "완료" 탭으로 이동

**비즈니스 규칙**

- 완료 처리 후에도 해당 화분의 학습 기록은 유지됨
- 완료된 화분에서는 학습 시작 불가
- "기른 나무 아카이브"에서 조회 가능

---

### 2-8. 화분 성장 (레벨 시스템) 🚧

| 항목 | 내용 |
|------|------|
| **기능 ID** | POT-007 |
| **도메인 타입** | `PlantLevel` (Lv.0 ~ Lv.6, 총 7단계) |
| **계산 위치** | `Pot.level` (computed property) |

**레벨 판정 로직** (현재 시간 기준만 구현)

```
val hours = potTotalStudyingTime / 3600000.0
level = when {
    hours >= 500.0 → 6
    hours >= 200.0 → 5
    hours >= 90.0  → 4
    hours >= 40.0  → 3
    hours >= 15.0  → 2
    hours >= 5.0   → 1
    else           → 0
}
```

**레벨별 요구 조건 (전체)**

| 전환 | 누적 학습 시간 | 아이템 소비 (미구현) |
|:----:|:-:|------|
| Lv.0 → Lv.1 | 5h | 🩷 ×1 |
| Lv.1 → Lv.2 | 15h | ☀️ ×3 + 💧 ×1 + 🩷 ×5 |
| Lv.2 → Lv.3 | 40h | 💧 ×5 + ☀️ ×15 + 🩷 ×22 |
| Lv.3 → Lv.4 | 90h | 🌿 ×3 + ☀️ ×13 + 💧 ×4 |
| Lv.4 → Lv.5 | 200h | 💊 ×1 + 🌿 ×10 + ☀️ ×17 + 🩷 ×30 |
| Lv.5 → Lv.6 | 500h | 💊 ×5 + 🌿 ×10 + 💧 ×35 + ☀️ ×30 + 🩷 ×50 |

> 🚧 **현재 상태**: 시간 기반 레벨 판정만 구현됨. 아이템 소비 조건은 미구현 상태로, 시간 충족 시 자동 레벨업

---

## 3. 학습 (Studying)

### 3-1. 학습 시작 (스톱워치) ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | STUDY-001 |
| **화면** | `StudyingScreen` |
| **ViewModel** | `StudyingViewModel` |
| **UseCase** | `StartStudyingSessionUseCase` |

**동작 흐름**

1. 홈 화면에서 대표 화분의 "학습 시작" 버튼 탭
2. `StudyingScreen` 진입 → 즉시 스톱워치 시작
3. `StartStudyingSessionUseCase(tag, title, potId, time, log)` 호출:
   - `EnsureCurrentUserUseCase()`로 현재 유저 확인
   - `StudyingRepository.initStudyingUser(StudyingUser(...))` → Firestore `studying/{uid}` 문서 생성
   - `StudyingRepository.saveLocalSession(StudyingSession(...))` → DataStore에 세션 백업

**스톱워치 기능**

| 동작 | 설명 |
|------|------|
| **시작** | 화면 진입 시 자동 시작 |
| **일시정지** | 일시정지 버튼 탭 → 타이머 멈춤 |
| **재개** | 재개 버튼 탭 → 타이머 재시작 |
| **종료** | 종료 버튼 탭 → 학습 종료 플로우 진입 |

---

### 3-2. 로컬 백업 (비정상 종료 복구) ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | STUDY-002 |
| **UseCase** | `UpdateLocalStudyingSessionUseCase` |
| **DataSource** | `StudyingLocalDataSource` (Preferences DataStore) |

**동작 흐름**

1. 학습 진행 중 **5초마다** 로컬 DataStore에 세션 정보 저장
2. 저장 데이터 (`StudyingSession`):
   - `userId`: 사용자 uid
   - `tag`: 학습 태그
   - `title`: 화분 이름
   - `potId`: 화분 ID
   - `time`: 현재까지의 학습 시간 (밀리초)
   - `log`: 기록 내용 (있을 경우)
3. 비정상 종료 (강제 종료, 크래시 등) 후 재진입:
   - `StudyingRepository.readLocalSession()` → 저장된 세션 확인
   - 세션이 있으면 → 복구 다이얼로그 표시 → "계속" 선택 시 이어서 학습

---

### 3-3. 서버 동기화 (실시간 학습 시간 공유) ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | STUDY-003 |
| **Repository** | `StudyingRepository.updateStudyingTime()` |

**동작 흐름**

1. 학습 진행 중 **1분마다** Firestore 서버에 학습 시간 동기화
2. `StudyingRepository.updateStudyingTime(tag, time)` 호출
3. Firestore `studying/{uid}.studyingTime` 필드 갱신
4. 이를 통해 다른 사용자가 실시간으로 학습 중인 유저의 시간을 확인 가능

---

### 3-4. 같은 분야 공부 사용자 조회 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | STUDY-004 |
| **Repository** | `StudyingRepository.observeStudyingUser()` |

**동작 흐름**

1. 학습 화면 진입 시 `StudyingRepository.observeStudyingUser(tag)` Flow 구독
2. Firestore `studying` 컬렉션에서 **같은 태그**로 학습 중인 유저를 실시간 수신
3. 현재 세션의 공부 시간 기준으로 **상위 3명** 표시
4. 표시 정보: 닉네임, 프로필 이미지, 현재 학습 시간

**구독 방식**

- Firestore `addSnapshotListener`로 실시간 구독
- 해당 태그의 학습 중인 사용자가 추가/삭제/시간 갱신될 때마다 자동 업데이트

---

### 3-5. 학습 종료 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | STUDY-005 |
| **UseCase** | `FinishStudyingUseCase`, `ClearStudyingSessionUseCase` |

**동작 흐름**

1. 학습 화면에서 "종료" 버튼 탭
2. **학습 내용 입력** (선택사항):
   - 제목(타임스탬프 자동 생성)
   - 학습 내용 (여러 줄 입력 가능, `List<String>`)
3. "저장" 버튼 탭 → `FinishStudyingUseCase(potId, timestamp, log, time)` 호출
4. **병렬 실행** (async/awaitAll):
   1. `StudyingRepository.saveStudyLog(potId, StudyLog.write(...))` → `users/{uid}/pots/{potId}/logs/{logId}` 생성
   2. `StudyingRepository.updateTotalStudyTime(potId, studyTime)` → 화분 누적 시간 갱신
   3. `StudyingRepository.updateUserTotalStudyTime(time)` → 유저 총 학습 시간 갱신
5. `ClearStudyingSessionUseCase()` 호출:
   1. `StudyingRepository.deleteStudyingUserInfo()` → Firestore `studying/{uid}` 문서 삭제
   2. `StudyingRepository.clearLocalSession()` → DataStore 로컬 세션 삭제
6. 병렬 작업 중 하나라도 실패하면 → `Result.Failure` 반환

**비정상 종료 시 클리어**

`ClearStudyingSessionUseCase(isInterrupted = true)`:
- 원격 `studying` 문서만 삭제하고 로컬 세션은 보존 (복구에 사용)
- 네트워크 에러일 경우에도 원격 삭제 결과를 반환

---

### 3-6. 학습 결과 화면 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | STUDY-006 |
| **화면** | `StudyResultScreen` |
| **ViewModel** | `StudyResultViewModel` |

**표시 정보**

| 요소 | 설명 |
|------|------|
| 공부 시간 | 이번 세션의 학습 시간 (시:분:초) |
| 학습 내용 | 입력한 학습 내용 목록 |
| 화분 성장 결과 | 레벨 변화 표시 (레벨업 발생 시 축하 애니메이션) |

**추가 기능**

| 기능 | 설명 |
|------|------|
| **갤러리 저장** | 결과 화면을 이미지로 캡처하여 기기 갤러리(MediaStore)에 저장 |
| **커뮤니티 공유** | 토글 켜면 학습 기록을 커뮤니티에 공유 게시글로 작성 |

---

## 4. 학습 기록

### 4-1. 화분별 학습 기록 조회 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | LOG-001 |
| **화면** | `StudyPlanDetailScreen` |
| **ViewModel** | `StudyPlanDetailViewModel` |
| **UseCase** | `GetStudyLogsUseCase` |
| **Repository** | `StudyLogRepository.getStudyLogs()` |

**동작 흐름**

1. `StudyPlanDetailScreen` 진입 시 `StudyLogRepository.getStudyLogs(uid, potId)` Flow 구독
2. Firestore `users/{uid}/pots/{potId}/logs` 서브컬렉션의 전체 문서를 실시간 수신
3. 각 학습 기록 카드에 표시:
   - 제목 (타임스탬프)
   - 학습 시간
   - 생성일
4. 기록 카드 탭 → 상세 조회 다이얼로그 (`StudyLogDetailDialog`)

---

### 4-2. 학습 기록 상세 조회 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | LOG-002 |
| **UseCase** | `GetSelectedStudyLogUseCase` |
| **Repository** | `StudyLogRepository.getSelectedStudyLog()` |

**동작 흐름**

1. 학습 기록 카드 탭 → `GetSelectedStudyLogUseCase(uid, potId, logId)` 호출
2. `StudyLogRepository.getSelectedStudyLog(uid, potId, logId)` → 단건 문서 조회
3. 상세 다이얼로그에 표시:
   - 제목
   - 학습 내용 목록 (`contents: List<String>`)
   - 학습 시간
   - 작성 일시

---

### 4-3. 학습 기록 삭제 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | LOG-003 |
| **UseCase** | `DeleteStudyLogUseCase` |
| **Repository** | `StudyLogRepository.deleteStudyLog()` |

**동작 흐름**

1. 학습 기록 상세에서 "삭제" 버튼 탭 → 확인 다이얼로그
2. "확인" 선택 → `DeleteStudyLogUseCase(uid, potId, logId, studyingTime)` 호출
3. 삭제 처리:
   1. `users/{uid}/pots/{potId}/logs/{logId}` 문서 삭제
   2. `users/{uid}/pots/{potId}.potTotalStudyingTime`에서 해당 기록의 시간 차감
   3. `users/{uid}.totalStudyTime`에서 해당 기록의 시간 차감

**비즈니스 규칙**

| 규칙 | 설명 |
|------|------|
| 누적 시간 차감 | 삭제된 학습 기록의 시간은 화분·유저 양쪽에서 차감 |
| 레벨 변동 | 시간 차감으로 인해 레벨이 내려갈 수 있음 (시간 기반 재계산) |
| 보상 유지 | 이미 지급된 학습 완료 보상은 회수하지 않음 |

---

### 4-4. 학습 기록 커뮤니티 공유 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | LOG-004 |
| **UseCase** | `CreatePostUseCase` (isShared = true) |

**동작 흐름**

1. 학습 기록 목록에서 공유할 기록 **다중 선택** (체크박스)
2. "공유" 버튼 탭 → 커뮤니티 공유 게시글 작성 화면으로 이동
3. 제목 입력 (본문 자동 구성: 선택한 학습 기록 목록)
4. `CreatePostUseCase(isShared = true, ...)` 호출
5. `Post.createShared(author, title, studyLogs, tag)` 생성 → Firestore에 저장

**다중 선택 지원**

- `StudyLog.isSelected` 플래그로 선택 상태 관리
- 선택된 기록들의 `studyLogs` 리스트가 게시글의 `studyLogs` 필드에 포함

---

## 5. 커뮤니티

### 5-1. 게시글 목록 조회 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-001 |
| **화면** | `CommunityListScreen` |
| **ViewModel** | `CommunityListViewModel` |
| **Repository** | `CommunityRepository.loadPostPage()` |

**동작 흐름**

1. 커뮤니티 탭 진입 → `CommunityRepository.loadPostPage(cursor, pageSize, tagIds, sharedOnly)` 호출
2. **커서 기반 페이지네이션**:
   - `cursor`: 마지막 게시글의 `createdAt` (epoch millis), 첫 페이지는 `null`
   - `pageSize`: 한 페이지당 불러올 게시글 수
   - 반환값: `PostPage(items, nextCursor, hasMore)`
   - `nextCursor`가 `null`이면 더 이상 게시글 없음
3. 무한 스크롤: 스크롤 끝 도달 시 `nextCursor`로 다음 페이지 로드
4. **pull-to-refresh**: 당겨서 새로고침 → `cursor = null`로 처음부터 재조회
5. 각 게시글 카드에 표시:
   - 작성자 닉네임, 프로필 이미지
   - 제목
   - 태그
   - 좋아요 수, 댓글 수
   - 작성 시간

---

### 5-2. 게시글 검색 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-002 |
| **Repository** | `CommunityRepository.getAllPosts()` |

**동작 흐름**

1. 검색 아이콘 탭 → 검색 입력 필드 표시
2. 검색어 입력 시 `CommunityRepository.getAllPosts(tagIds, sharedOnly)` 호출
3. **클라이언트 측 필터링**:
   - 전체 게시글을 로드한 후 제목/본문에 대해 `contains` 비교
   - Firestore의 전문 검색 미지원으로 인한 클라이언트 측 구현
4. 필터링된 결과 목록 표시

> ⚠️ 전체 게시글 로드 방식이므로, 게시글 수 증가 시 성능 이슈 가능

---

### 5-3. 태그별 필터 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-003 |
| **Repository** | `CommunityRepository.getTags()` |

**필터 유형**

| 필터 | 동작 |
|------|------|
| **전체** | 모든 게시글 표시 (`tagIds = []`, `sharedOnly = false`) |
| **공유** | 학습 기록 공유 게시글만 (`sharedOnly = true`) |
| **태그별** | 특정 태그의 게시글만 (`tagIds = [선택된 tagId]`) |

---

### 5-4. 일반 게시글 작성 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-004 |
| **화면** | `CommunityPostScreen` |
| **ViewModel** | `CommunityPostViewModel` |
| **UseCase** | `CreatePostUseCase` |

**동작 흐름**

1. 게시글 목록에서 "글 작성" FAB 탭 → `CommunityPostScreen` 이동
2. 입력 필드:
   - **제목**: 필수, 최대 50자
   - **본문**: 선택, 최대 1,000자
   - **태그**: 목록에서 선택
3. "등록" 버튼 탭 → `CreatePostUseCase(isShared = false, title, content, null, tag)` 호출
4. `Post.createOriginal(author, title, content, tag)` 생성
5. `CommunityRepository.savePost(post, CommunityActivity.post(uid, title))`:
   - Firestore `posts/{postId}` 문서 생성
   - `activities/{activityId}` 문서 생성 (활동 기록)
   - `postId`를 반환하여 `activity.targetId`에 저장
6. 목록 화면으로 복귀

**입력 유효성 검증**

| 규칙 | 조건 |
|------|------|
| 제목 | 1자 이상, 50자 이하 |
| 본문 | 0자 이상, 1,000자 이하 |
| 태그 | 필수 선택 |

---

### 5-5. 게시글 상세 조회 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-005 |
| **화면** | `CommunityDetailScreen` |
| **ViewModel** | `CommunityDetailViewModel` |
| **Repository** | `CommunityRepository.getPostDetail()`, `CommunityRepository.getComments()` |

**동작 흐름**

1. 게시글 카드 탭 → `CommunityDetailScreen(postId)` 이동
2. `CommunityRepository.getPostDetail(postId)` → 게시글 단건 조회
3. `CommunityRepository.getComments(postId)` → 댓글 목록 조회
4. 표시 정보:
   - 작성자 프로필 (닉네임, 이미지)
   - 제목, 본문
   - 태그
   - 좋아요 수, 좋아요 여부
   - 댓글 목록
   - 작성 시간, 수정 시간 (수정된 경우)
   - 학습 기록 공유 게시글인 경우 → `studyLogs` 목록 표시
5. pull-to-refresh → 상세 정보 재조회

---

### 5-6. 게시글 수정/삭제 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-006 |
| **Repository** | `CommunityRepository.updatePost()`, `CommunityRepository.deletePost()` |

**수정 동작**

1. 본인 게시글의 "수정" 메뉴 탭 → `CommunityPostScreen`(편집 모드)으로 이동
2. 기존 내용이 미리 채워진 상태로 수정
3. "수정" 버튼 탭 → `CommunityRepository.updatePost(isShared, postId, title, content, tag)` 호출
4. Firestore `posts/{postId}` 문서 갱신

**삭제 동작**

1. 본인 게시글의 "삭제" 메뉴 탭 → 확인 다이얼로그
2. "확인" 선택 → `CommunityRepository.deletePost(postId, activityId)` 호출
3. `posts/{postId}` 문서 삭제 + 관련 `activities/{activityId}` 문서 삭제

**권한 제어**

- 수정/삭제 메뉴는 `post.author.id == currentUser.uid`일 때만 표시

---

### 5-7. 좋아요 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-007 |
| **UseCase** | `ToggleLikeUseCase` |
| **Repository** | `CommunityRepository.toggleLike()` |

**동작 흐름**

1. 게시글의 좋아요 버튼 탭 → `ToggleLikeUseCase(postId, authorId, title)` 호출
2. `EnsureCurrentUserUseCase()`로 현재 유저 확인
3. **본인 게시글 체크**: `user.uid == authorId` → `AppError.Custom("본인 게시글은 좋아요할 수 없습니다.")` 반환
4. `CommunityRepository.toggleLike(postId, uid, title)`:
   - `likedBy` 배열에 uid 존재 여부 확인
   - 없으면 → 추가 + `likeCount +1` + 활동 기록 생성
   - 있으면 → 제거 + `likeCount -1` + 활동 기록 삭제
5. 반환값: `true`(좋아요 추가) / `false`(좋아요 취소)

---

### 5-8. 댓글 CRUD ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-008 |
| **UseCase** | `AddCommentUseCase` |
| **Repository** | `CommunityRepository.addComment()`, `.updateComment()`, `.deleteComment()` |

**댓글 작성**

1. 게시글 상세 화면의 댓글 입력 필드에 내용 입력 (최대 100자)
2. "등록" 버튼 탭 → `AddCommentUseCase(postId, title, content)` 호출
3. 댓글 문서 생성: `posts/{postId}/comments/{commentId}`
4. 활동 기록 생성: `activities/{activityId}` (type = "댓글")
5. `posts/{postId}.commentCount` +1 갱신

**댓글 수정**

1. 본인 댓글의 "수정" 메뉴 탭 → 수정 입력 UI
2. `CommunityRepository.updateComment(postId, commentId, newContent)` 호출

**댓글 삭제**

1. 본인 댓글의 "삭제" 메뉴 탭 → 확인 다이얼로그
2. `CommunityRepository.deleteComment(postId, commentId)` 호출
3. `posts/{postId}.commentCount` -1 갱신

---

### 5-9. 커뮤니티 활동 내역 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-009 |
| **화면** | `CommunityActivityScreen` |
| **ViewModel** | `CommunityActivityViewModel` |
| **Repository** | `CommunityRepository.observeActivity()` |

**동작 흐름**

1. 마이페이지 → "활동 내역" 탭 → `CommunityActivityScreen` 이동
2. `CommunityRepository.observeActivity(selected)` Flow 구독
3. 필터링 기준:
   - "게시물": `ActivityType.POST` 활동만
   - "댓글": `ActivityType.COMMENT` 활동만
   - "좋아요": `ActivityType.LIKE` 활동만
4. 활동 아이템 탭 → 해당 게시글(`targetId`)로 이동
   - 이동 전 `CommunityRepository.isPostExist(postId)` 확인
   - 게시글이 삭제된 경우 → "삭제된 게시글입니다" 메시지

**활동 데이터 (`CommunityActivity`)**

| 필드 | 설명 |
|------|------|
| `uid` | 활동한 유저 |
| `type` | "게시물" / "댓글" / "좋아요" |
| `title` | 관련 게시글 제목 |
| `targetId` | 관련 게시글 ID |
| `comment` | 댓글 내용 (type이 "댓글"인 경우) |
| `commentId` | 댓글 ID (type이 "댓글"인 경우) |
| `createAt` | 활동 시간 |

---

### 5-10. 북마크 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | COMM-010 |
| **데이터 모델** | `Post.bookmarkedBy: List<String>`, `Post.bookmarkCount: Int` |

**기획 내용**

- 좋아요와 별개로 게시글을 개인 목적으로 저장
- `bookmarkedBy` 배열에 uid 추가/제거로 토글
- 북마크한 게시글 목록을 마이페이지에서 조회 가능
- 본인 게시글 북마크 가능 여부: 허용

> 📋 데이터 모델은 정의되어 있으나 UI/로직 미구현

---

## 6. 마이페이지

### 6-1. 프로필 조회 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | MY-001 |
| **화면** | `MypageScreen` |
| **ViewModel** | `MyPageViewModel` |
| **Repository** | `UserRepository.currentUser` |

**표시 정보**

| 요소 | 데이터 소스 |
|------|------------|
| 닉네임 | `User.nickname` |
| 프로필 이미지 | `User.profileImg` (화분 레벨 이미지 기반) |
| 총 공부 시간 | `User.totalStudyTime` (밀리초 → 포맷 변환) |

---

### 6-2. 프로필 수정 (닉네임 + 이미지) ✅ / 🚧

| 항목 | 내용 |
|------|------|
| **기능 ID** | MY-002 |
| **UseCase** | `UpdateProfileUseCase` |
| **Repository** | `UserRepository.isNicknameTaken()`, `.registerNickname()`, `.deleteNickname()`, `.updateProfile()` |

**동작 흐름**

1. 마이페이지에서 프로필 편집 버튼 탭
2. 닉네임 변경:
   - 새 닉네임 입력 (2~10자)
   - `UserRepository.isNicknameTaken(newNickname)` → 중복 검사
   - 중복 아니면 → `registerNickname(newNickname)` + `deleteNickname(oldNickname)`
3. 프로필 이미지 변경 (🚧):
   - `GetProfileImageLevelListUseCase`로 선택 가능한 이미지 목록 조회
   - 화분 성장 레벨 이미지 중 선택
4. `UserRepository.updateProfile(updatedUser)` → Firestore 갱신

**닉네임 변경 롤백 로직**

실패 시 닉네임 일관성 유지를 위한 롤백 처리:
- 프로필 갱신 실패 → `rollbackNicknameChange()`:
  1. 이전 닉네임 재등록 (`registerNickname(previousNickname)`)
  2. 새 닉네임 삭제 (`deleteNickname(newNickname)`)
- 이전 닉네임 삭제 실패 → `rollbackRegisteredNickname()`:
  1. 새 닉네임 삭제 (`deleteNickname(newNickname)`)

---

### 6-3. 다크모드 ✅

| 항목 | 내용 |
|------|------|
| **기능 ID** | MY-003 |
| **UseCase** | `UpdateDarkModeUseCase` |
| **Repository** | `UserRepository.updateDarkMode()` |

**동작 흐름**

1. 마이페이지에서 다크모드 토글 스위치 조작
2. `UpdateDarkModeUseCase(isDarkMode)` 호출
3. `UserRepository.updateDarkMode()` → 로컬 DataStore에 기기 설정으로 저장
4. `ObserveDarkModeUseCase`가 DataStore 변경을 구독 → 앱 테마 즉시 전환

**저장 방식**

- 로컬 Preferences DataStore에 기기별 설정으로 저장 (계정과 무관)
- `SettingsLocalDataSource.isDarkMode: Flow<Boolean?>` 구독으로 실시간 반영
- 참고: `User.isDarkMode` 필드가 Firestore DTO에 남아 있으나, 실제 다크모드 제어에는 사용되지 않음

---

## 7. 출석체크

### 7-1. 출석판 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | ATT-001 |
| **화면** | `HomeScreen` → `AttendanceCheckDialog` |
| **Repository** | `UserRepository.checkAttendance()` |
| **도메인 모델** | `DailyCheckThisMonth`, `AttendanceDecision`, `AttendanceRewardTable` |

**동작 흐름**

1. 홈 화면에서 출석체크 버튼 탭 → `AttendanceCheckDialog` 표시
2. 28일 고정 출석판 표시 (윤달/윤년 구분 없이 통일)
3. "출석하기" 버튼 탭 → `UserRepository.checkAttendance(uid)` 호출
4. `DailyCheckThisMonth.decideNext(nowKst)` 호출하여 출석 판정:

**출석 판정 로직**

```
KST 기준 현재 날짜 확인
├─ 마지막 출석 날짜와 같은 달이고 28회 이상 → MonthCompleted (이달 출석 완료)
├─ 마지막 출석 날짜와 오늘이 같음 → AlreadyChecked (이미 출석함)
├─ 같은 달이면 count + 1, 다른 달이면 1로 리셋
│   └─ newCount > 28 → MonthCompleted
│   └─ AttendanceDecision.Success(newCount, reward)
```

**출석 보상 테이블**

| 출석 횟수 | 보상 |
|:---------:|------|
| 2일 | 🩷 하트 ×1 |
| 4일 | ☀️ 햇빛 ×1 |
| 7일 | 💰 100 Gold |
| 9일 | 💧 물 ×1 |
| 12일 | 🩷 하트 ×1 |
| 14일 | 💰 200 Gold |
| 17일 | ☀️ 햇빛 ×1 |
| 20일 | 🌿 비료 ×1 |
| 21일 | 💰 300 Gold |
| 23일 | 💧 물 ×1 |
| 25일 | 💊 영양제 ×1 |
| 28일 | 💰 500 Gold |

> 보상이 없는 날(1, 3, 5, 6, 8, 10, 11, 13, 15, 16, 18, 19, 22, 24, 26, 27일)에도 출석 카운트는 증가

**비즈니스 규칙**

| 규칙 | 설명 |
|------|------|
| 하루 1회 | 서버 시간(KST) 기준, 당일 중복 출석 불가 |
| 28일 고정 | 달의 실제 일수와 무관하게 28칸 |
| 누적 방식 | 연속 출석 아닌 누적 출석 (빈 날이 있어도 카운트 이어짐) |
| 월 리셋 | 다른 달로 넘어가면 카운트 1부터 재시작 |
| 중복 보상 방지 | Transaction 기반 (`isRewarded` 플래그) |

---

## 8. 아이템 · 보상 · 상점

### 8-1. 성장 아이템 시스템 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | ITEM-001 |
| **도메인 타입** | `ItemType` |
| **데이터 모델** | `User.item: Item(heart, sun, water, fertilizer, nutrient, box)` |

**아이템 정의**

| `ItemType` | 한국어명 | 등급 | 상점가격(Gold) | 보너스박스 확률 |
|:----------:|:-------:|:----:|:-------------:|:-------------:|
| `HEART` | 정성 | 기본 | 50 | 60% |
| `SUN` | 햇빛 | 일반 | 100 | 13% |
| `WATER` | 물 | 일반 | 250 | 9% |
| `FERTILIZER` | 비료 | 고급 | 450 | 5% |
| `NUTRIENT` | 영양제 | 최고 등급 | 950 | 2% |
| `BOX` | 보너스 박스 | — | 250 | 5% |
| `GOLD_300` | 300골드 | — | — | 3.5% |
| `GOLD_500` | 500골드 | — | — | 2% |
| `GOLD_1000` | 1000골드 | — | — | 0.5% |

> 📋 아이템 수급/소비 로직은 기획 완료, UI/기능 미구현

---

### 8-2. 학습 완료 보상 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | ITEM-002 |

**보상 정책** (해당 티어 하나만 적용, 하위 티어 중복 없음)

| 세션 공부 시간 | 아이템 보상 | 보너스박스 |
|:-------------:|------------|:---------:|
| ≥ 1h | 🩷 ×1 | 1개 |
| ≥ 3h | 🩷 ×1, ☀️ ×1 | 2개 |
| ≥ 5h | 🩷 ×1, ☀️ ×1, 💧 ×1 | 3개 |
| ≥ 7h | 🩷 ×1, ☀️ ×1, 💧 ×1, 🌿 ×1 | 4개 |
| ≥ 9h | 🩷 ×1, ☀️ ×1, 💧 ×1, 🌿 ×1, 💊 ×1 | 5개 |

---

### 8-3. 보너스박스 (랜덤박스) 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | ITEM-003 |

**확률 분포** (1개 개봉 시 1가지 보상)

| 보상 | 확률 |
|------|:----:|
| 🩷 하트 ×1 | 60% |
| ☀️ 햇빛 ×1 | 13% |
| 💧 물 ×1 | 9% |
| 🌿 비료 ×1 | 5% |
| 💊 영양제 ×1 | 2% |
| 💰 300 Gold | 3.5% |
| 💰 500 Gold | 2% |
| 💰 1,000 Gold | 0.5% |

> ⚠️ PRD 기준 Gold 합산 확률은 11%이나, 코드 기준(`ItemType`)에서는 보너스 박스(5%) 항목이 추가되어 총합 100%

---

### 8-4. Gold 시스템 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | ITEM-004 |
| **데이터 모델** | `User.coin: Int` |

**Gold 수급 경로**

| 경로 | 상세 |
|------|------|
| 보너스박스 | 300G(3.5%) / 500G(2%) / 1,000G(0.5%) |
| 출석 보상 | 7일(100G) / 14일(200G) / 21일(300G) / 28일(500G) |
| 목표 달성 | 기획 미정 |

---

### 8-5. 상점 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | ITEM-005 |

**상점 아이템 가격**

| 아이템 | 가격 (Gold) |
|--------|:-----------:|
| 🩷 하트 | 50 |
| ☀️ 햇빛 | 100 |
| 💧 물 | 250 |
| 🌿 비료 | 450 |
| 💊 영양제 | 950 |
| 🪴 화분 | 3,000 |

> 📋 모든 기본 기능 완료 후 개발 예정

---

## 9. 리포트 · 통계

### 9-1. 학습 달력 🚧

| 항목 | 내용 |
|------|------|
| **기능 ID** | REPORT-001 |
| **화면** | `ReportScreen` |

**기획 내용**

- 달력 UI에 공부한 날짜를 초록색 원으로 표시
- 날짜 선택 시 해당 일자의 학습 기록 목록 조회
- `ReportScreen` 컴포저블은 존재하나 세부 구현 미확인

---

### 9-2. 일간/월간 학습 통계 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | REPORT-002 |

**기획 내용**

| 통계 | 항목 |
|------|------|
| 일간 | 총 공부 시간, 최대 공부 시간, 최소 공부 시간, 평균 공부 시간 |
| 월간 | 월간 학습 시간 그래프, 총/최대/최소/평균 |

---

## 10. 알림

### 10-1. 학습 리마인더 / 학습 목표 알림 📋

| 항목 | 내용 |
|------|------|
| **기능 ID** | NOTI-001 |
| **기술 스택** | FCM (Firebase Cloud Messaging) |

**기획 내용**

| 알림 유형 | 설명 |
|-----------|------|
| 학습 리마인더 | 설정한 시간에 공부 시작 알림 발송 |
| 학습 목표 | 일간/월간 목표 달성 시 알림 |

> 📋 FCM 의존성은 추가되어 있으나 기능 미구현

---

## 부록

### A. UseCase 매핑 테이블

| 패키지 | UseCase | 설명 |
|--------|---------|------|
| `auth` | `CheckAutoLoginUseCase` | 자동 로그인 판정 |
| | `SignInWithGoogleUseCase` | Google 로그인 |
| | `SignInWithEmailUseCase` | 이메일 로그인 (⛔ 제거 예정) |
| | `SignUpWithEmailUseCase` | 이메일 회원가입 (⛔ 제거 예정) |
| | `SetNicknameUseCase` | 닉네임 설정 |
| | `SignOutUseCase` | 로그아웃 |
| | `DeleteAccountUseCase` | 회원탈퇴 |
| | `ResolveUserSessionUseCase` | 로그인 후 유저 세션 초기화 |
| `pot` | `GetPotListUseCase` | 화분 목록 조회 |
| | `GetActivePotUseCase` | 활성 화분 조회 |
| | `GetPotDetailUseCase` | 화분 상세 조회 |
| | `UpdatePotNameUseCase` | 화분 이름 변경 |
| | `DeleteEntirePotUseCase` | 화분 삭제 |
| | `CompleteStudyPlanUseCase` | 화분 학습 완료 |
| `studying` | `StartStudyingSessionUseCase` | 학습 시작 |
| | `UpdateLocalStudyingSessionUseCase` | 로컬 세션 갱신 (5초마다) |
| | `FinishStudyingUseCase` | 학습 종료 |
| | `ClearStudyingSessionUseCase` | 학습 세션 정리 |
| `studyLog` | `GetStudyLogsUseCase` | 학습 기록 목록 조회 |
| | `GetSelectedStudyLogUseCase` | 학습 기록 단건 조회 |
| | `DeleteStudyLogUseCase` | 학습 기록 삭제 |
| `community` | `CreatePostUseCase` | 게시글 작성 |
| | `AddCommentUseCase` | 댓글 작성 |
| | `ToggleLikeUseCase` | 좋아요 토글 |
| | `DeleteActivitiesUseCase` | 활동 기록 삭제 |
| `mypage` | `UpdateProfileUseCase` | 프로필 수정 |
| | `UpdateDarkModeUseCase` | 다크모드 토글 |
| | `ObserveDarkModeUseCase` | 다크모드 설정 구독 (DataStore) |
| | `GetProfileImageLevelListUseCase` | 프로필 이미지 목록 |
| `session` | `EnsureCurrentUserUseCase` | 현재 유저 확인 (세션 가드) |

### B. Repository 인터페이스 매핑

| Repository | 구현체 | 주요 데이터소스 |
|------------|--------|----------------|
| `AuthRepository` | `AuthRepositoryImpl` | Firebase Auth |
| `UserRepository` | (StateFlow 기반) | Firestore `users/{uid}`, `nicknames` |
| `PotRepository` | — | Firestore `users/{uid}/pots` |
| `StudyingRepository` | — | Firestore `studying`, DataStore |
| `StudyLogRepository` | `StudyLogRepositoryImpl` | Firestore `users/{uid}/pots/{potId}/logs` |
| `CommunityRepository` | — | Firestore `posts`, `activities` |

### C. 에러 분류 체계

| AppError | 메시지 | 트리거 상황 |
|----------|--------|------------|
| `Network` | 인터넷 연결이 원활하지 않습니다 | Firestore 타임아웃, 오프라인 |
| `Auth` | 인증에 실패했습니다 | Firebase Auth 에러 |
| `UnknownUser` | 사용자 정보를 가져오지 못했습니다 | `currentUser == null` |
| `Email` | 이메일 형식이 올바르지 않습니다 | 이메일 유효성 검증 |
| `Password` | 비밀번호 형식이 올바르지 않습니다 | 비밀번호 유효성 검증 |
| `Server` | 서버에 오류가 발생했습니다 | Firestore 서버 에러 |
| `Unknown` | 알 수 없는 오류가 발생했습니다 | 예상 외 예외 |
| `Upload` | 저장에 실패했습니다 | 문서 저장 실패 |
| `Local` | 로컬 저장 실패 | DataStore 에러 |
| `Permission` | 권한이 없습니다 | Firestore 보안 규칙 위반 |
| `Custom(msg)` | (동적 메시지) | 닉네임 중복, 본인 좋아요 등 |

---

## 관련 문서

| 문서 | 경로 |
|------|------|
| PRD | [docs/PRD.md](PRD.md) |
| User Flow | [docs/USER_FLOW.md](USER_FLOW.md) |
| System Flow | [docs/SYSTEM_FLOW.md](SYSTEM_FLOW.md) |
| 화면명세 | docs/SCREEN_SPEC.md (작성 예정) |
| 데이터 모델 | [docs/DATA_MODEL.md](DATA_MODEL.md) |
