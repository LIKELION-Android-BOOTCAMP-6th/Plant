# 📋 Plant 기능명세서

## 목차

- [인증](#인증)
- [홈](#홈)
- [공부중](#공부중)
- [학습 계획 기록](#학습-계획-기록)
- [커뮤니티](#커뮤니티)
- [마이페이지/아카이브](#마이페이지아카이브)
- [마이페이지](#마이페이지)
- [마이페이지/세팅](#마이페이지세팅)

---

## 인증

### F-Auth-01 이메일 인증 회원가입

- **기능 설명**: 비회원을 위한 파이어베이스를 활용한 이메일 인증 기반의 회원가입
- **관련 화면**: `SignUpScreen`
- **입력 항목**: `SignInScreen`에서 '회원가입' 텍스트 클릭 시 진입 가능
  - 이메일: String (이메일을 입력하세요.)
  - 비밀번호: String (\*로 마스킹 처리, 눈 아이콘으로 표시/숨김 토글)
  - 비밀번호 확인: String (\*로 마스킹 처리, 눈 아이콘으로 표시/숨김 토글)
  - 회원가입 완료: Button
- **출력**: 이메일 입력 → 비밀번호 입력 → 비밀번호 확인 입력 → 회원가입 완료 버튼 클릭 → 미입력 항목 체크 → 이메일 형식 검증 + 비밀번호 조건 검증(소문자+숫자+특수문자, 6자 이상) + 비밀번호 일치 검증 3가지 동시 실행 → 에러 없으면 `createUserWithEmailAndPassword`로 Firebase Auth 계정 생성(`suspendCancellableCoroutine` 사용) → 인증 메일 발송 → `auth.signOut()`으로 인증 전 로그인 차단 → '회원가입 완료! 인증 메일을 확인해주세요.' 토스트 → `SignInScreen`으로 전환
  > ⇒ 회원가입 완료 후 이메일 인증 전까지 로그인 불가, Firestore User 문서는 첫 로그인 시 생성
- **예외처리**:
  - 이메일, 비밀번호, 비밀번호 확인 중 미입력 항목이 있을 경우 → '모든 항목을 작성해주세요.' 토스트
  - 이메일 형식이 올바르지 않을 경우 → 이메일 필드 하단에 '이메일 형식이 올바르지 않습니다.' 에러 텍스트 표시
  - 비밀번호 조건 미충족 시 (소문자+숫자+특수문자, 6자 이상) → 비밀번호 필드 하단에 '비밀번호 조건을 맞춰주세요.' 에러 텍스트 표시
  - 비밀번호와 비밀번호 확인 불일치 → 비밀번호 확인 필드 하단에 '비밀번호가 일치하지 않습니다.' 에러 텍스트 표시
  - 이미 등록된 이메일 (`ERROR_EMAIL_ALREADY_IN_USE`) → '이미 등록된 계정입니다.' 토스트
  - Firebase 비밀번호 약함 (`ERROR_WEAK_PASSWORD`) → '비밀번호 조건을 맞춰주세요.' 토스트
  - Firebase 이메일 형식 오류 (`ERROR_INVALID_EMAIL`) → '이메일 형식이 올바르지 않습니다.' 토스트
  - 기타 오류 → '회원가입 실패. 다시 시도해주세요.' 토스트
  > 3가지 검증(이메일, 비밀번호 조건, 비밀번호 일치)은 동시 실행되어 해당하는 모든 에러를 한번에 표시

---

### F-Auth-02 구글 로그인

- **기능 설명**: Google 계정을 활용한 Credential Manager 기반 소셜 로그인
- **관련 화면**: `SignInScreen`
- **입력 항목**: `SignInScreen`에서 구글 로그인 이미지(`ic_auth_google`) 클릭 시 진행
  - 구글 로그인 이미지: Image (clickable) — 로그인 버튼 아래 '또는' 구분선 하단에 위치
- **출력**: 구글 로그인 이미지 클릭 → `GetSignInWithGoogleOption`으로 구글 로그인 옵션 생성 (`webClientId` 사용) → `GetCredentialRequest` 생성 → `credentialManager.getCredential()` 호출하여 Google 계정 선택 팝업 표시 → 사용자 계정 선택 → `GoogleIdTokenCredential.createFrom()`으로 idToken 추출 → ViewModel의 `handleGoogleSignIn(idToken)` 호출 → `GoogleAuthProvider.getCredential(idToken, null)`로 Firebase 인증 정보 변환 → `signInWithCredential`로 Firebase Auth 로그인 → `handleLoginSuccess(uid)` 공통 처리 진입 → Firestore `users/{uid}` 문서 조회 → 없으면 `createUser(uid)`로 신규 생성 → `CurrentUser` 싱글톤 세팅 (uid, nickname, profileImg) → `isFirstLogin == true`이면 닉네임 설정 다이얼로그 표시 (닫기 불가, 2~10자 입력, 중복 검사 후 `nicknames` 컬렉션 등록) / `false`이면 '{닉네임}님 환영합니다.' 토스트 후 `HomeScreen`으로 전환 (`popUpTo(0)`)
  > ⇒ 구글 계정은 이미 인증된 상태이므로 이메일 인증 체크 불필요
- **예외처리**:
  - 사용자가 계정 선택을 취소하거나 바깥 터치/뒤로가기로 닫은 경우 (`GetCredentialCancellationException`) → 무시, `SignInScreen` 유지
  - 기기에 등록된 Google 계정이 없는 경우 (`NoCredentialException`) → '기기에 등록된 Google 계정이 없습니다. 설정에서 계정을 추가해주세요.' 토스트 + 기기 계정 추가 화면(`ACTION_ADD_ACCOUNT`) 자동 이동
  - 기타 예외 (Screen 레벨) → '구글 로그인에 실패했습니다.\n다시 시도해주세요.' 토스트
  - Firebase 인증 실패 (ViewModel 레벨) → '구글 로그인 실패: {에러 메시지}' 토스트
  - Firestore 유저 정보 생성 실패 → '정보 생성에 실패했습니다.\n다시 시도해주세요.' 토스트 후 `auth.signOut()`
  > Screen과 ViewModel에서 예외를 이중으로 처리

---

### F-Auth-03 이메일 로그인

- **기능 설명**: 이메일과 비밀번호를 활용한 Firebase Auth 기반 로그인 (세션 지속성으로 자동 로그인 지원)
- **관련 화면**: `SignInScreen`
- **입력 항목**: `SignInScreen`에서 직접 입력
  - 이메일: String (이메일을 입력하세요)
  - 비밀번호: String (\*로 마스킹 처리, 눈 아이콘으로 표시/숨김 토글)
  - 로그인: Button
- **출력**: 이메일, 비밀번호 입력 → 로그인 버튼 클릭 → 빈칸 검증 → 이메일 형식 검증 (정규식) → `signInWithEmailAndPassword` 호출 → 이메일 인증 여부 확인 (`user.isEmailVerified`) → 미인증 시 `auth.signOut()` + 이메일/비밀번호 입력값 초기화 + '이메일을 인증해주세요.' 토스트 → 인증 완료 시 `handleLoginSuccess(uid)` 공통 처리 진입 → Firestore `users/{uid}` 문서 조회 → 없으면 신규 생성 → `CurrentUser` 싱글톤 세팅 → `isFirstLogin == true`이면 닉네임 설정 다이얼로그 / `false`이면 '{닉네임}님 환영합니다.' 토스트 후 `HomeScreen`으로 전환 (`popUpTo(0)`)
  > ⇒ Firebase SDK가 로그인 성공 시 기기 로컬에 인증 토큰 자동 저장 → 앱 재시작 시 `auth.currentUser`로 자동 로그인
- **예외처리**:
  - 이메일 또는 비밀번호가 빈 값인 경우 → '이메일과 비밀번호를 입력해주세요.' 토스트
  - 이메일 형식이 올바르지 않은 경우 → 이메일 필드 하단에 '이메일 형식이 올바르지 않습니다.' 에러 텍스트 표시
  - `ERROR_INVALID_EMAIL` → 이메일 입력값 초기화 + '이메일 형식을 확인해주세요' 토스트
  - `ERROR_USER_NOT_FOUND` 또는 `ERROR_WRONG_PASSWORD` → 비밀번호 입력값 초기화 + '계정 정보가 올바르지 않습니다.' 토스트
  - `ERROR_INVALID_CREDENTIAL` → 이메일, 비밀번호 모두 초기화 + '로그인에 실패했습니다.\n다시 시도해주세요.' 토스트
  - 기타 오류 → 이메일, 비밀번호 모두 초기화 + '로그인에 실패했습니다.\n다시 시도해주세요.' 토스트
  > `ERROR_USER_NOT_FOUND`와 `ERROR_WRONG_PASSWORD`는 보안상 '계정 정보가 올바르지 않습니다.'로 통합 처리

---

### F-Auth-04 비밀번호 재설정

- **기능 설명**: 비밀번호를 분실한 회원을 위한 Firebase Auth 기반 비밀번호 재설정 메일 발송
- **관련 화면**: `SignInScreen` (내 다이얼로그로 처리, 별도 화면 없음)
- **입력 항목**: `SignInScreen`에서 '비밀번호를 잊으셨나요?' 텍스트 클릭 시 다이얼로그 표시
  - 이메일: String (이메일을 입력하세요)
  - 취소: Button
  - 전송: Button
  > 다이얼로그 타이틀: '비밀번호 재설정', 안내 문구: '가입한 이메일을 입력하면 비밀번호 재설정 메일을 보내드립니다.'
- **출력**: '비밀번호를 잊으셨나요?' 클릭 → 비밀번호 재설정 다이얼로그 표시 → 이메일 입력 → '전송' 버튼 클릭 → 클라이언트 측 검증 (빈 값 체크 → 이메일 형식 정규식 검증) → 통과 시 ViewModel의 `sendPasswordResetEmail(email)` 호출 → `auth.sendPasswordResetEmail(email)` 실행 → '재설정 메일을 전송했습니다.' 토스트 → 다이얼로그 자동 닫힘
- **예외처리**:
  - 이메일이 빈 값인 경우 → 이메일 필드 하단에 '이메일을 입력해주세요.' 에러 텍스트 표시 (다이얼로그 유지)
  - 이메일 형식이 올바르지 않은 경우 → 이메일 필드 하단에 '이메일 형식이 올바르지 않습니다.' 에러 텍스트 표시 (다이얼로그 유지)
  - '취소' 버튼 클릭 시 → 다이얼로그 닫기, `SignInScreen` 유지
  - 다이얼로그 바깥 터치 시 → 다이얼로그 닫기
  - 메일 전송 실패 시 → '메일 전송에 실패했습니다.\n이메일을 확인해주세요.' 토스트
  > 클라이언트 측 검증은 Screen에서, Firebase 에러는 ViewModel에서 처리

---

### F-Auth-05 로그아웃

- **기능 설명**: 로그인된 회원의 Firebase Auth 세션을 종료하고 로그인 화면으로 전환
- **관련 화면**: `MyPageScreen`
- **입력 항목**: `MyPageScreen`에서 로그아웃 버튼 클릭 시 진행
  - 로그아웃: Button (ButtonTemplate 컴포넌트)
- **출력**: 로그아웃 버튼 클릭 → `ConfirmDialog` 표시 ('로그아웃 하시겠습니까?') → '예' 클릭 → `firebaseAuth.signOut()` 호출 → `CurrentUser.clear()` → `NavigateToSignIn` 이벤트 발송 → `SignInScreen`으로 전환 (`popUpTo(0) { inclusive = true }`)
- **예외처리**:
  - 확인 다이얼로그에서 '아니오' 클릭 시 → 다이얼로그 닫기 + `clearProfileState()` 호출, `MyPageScreen` 유지

---

### F-Auth-06 회원탈퇴

- **기능 설명**: 회원 탈퇴 시 Firestore 관련 데이터 정리 후 Firebase Auth 계정 삭제
- **관련 화면**: `MypageSettingScreen`
- **입력 항목**: `MyPageScreen` → '앱 설정' 클릭 → `MyPageSettingScreen`에서 회원탈퇴 버튼 클릭 시 진행
  - 회원탈퇴: Button (ButtonTemplate 컴포넌트)
- **출력**: 회원탈퇴 버튼 클릭 → 1단계 `ConfirmDialog` → '예' 클릭 → 2단계 `ConfirmDialog` → '예' 클릭 → Firestore 데이터 삭제:
  1. `users/{uid}/pots/{potId}/logs/{logId}` 문서 삭제
  2. `users/{uid}/pots/{potId}` 문서 삭제
  3. `activities` 컬렉션에서 uid 일치 문서 삭제
  4. 내 게시글(`posts`) + 하위 `comments`, `likes` 서브컬렉션 삭제
  5. 다른 사람 게시글의 내 댓글 소프트 삭제 (content → '- 삭제된 댓글입니다. -', nickname → '(알 수 없음)', profileImg → '')
  6. 다른 사람 게시글의 내 좋아요 삭제
  7. `users/{uid}` 문서 삭제

  → `nicknameRepository.deleteNickname(nickname)` → `firebaseUser.delete()` → `CurrentUser.clear()` → '회원탈퇴가 완료되었습니다.' 토스트 → `SignInScreen`으로 전환
  > Firestore 데이터를 먼저 삭제 후 Auth 계정을 맨 마지막에 삭제 (인증 세션 유지 상태에서 Firestore 접근 보장)
- **예외처리**:
  - 1단계 또는 2단계 다이얼로그에서 '아니오' 클릭 시 → 다이얼로그 닫기 + 1단계로 리셋
  - `firebaseAuth.currentUser`가 null인 경우 → '로그인 정보가 없습니다.' 토스트
  - 재인증 필요 시 (`RECENT_LOGIN_REQUIRED`) → '보안을 위해 재로그인 후 다시 시도해주세요.' 토스트
  - 기타 오류 → '회원탈퇴에 실패했습니다. 다시 시도해주세요.' 토스트

---

## 홈

### F-Home-01 홈 화면 정보 조회

- **기능 설명**: 로그인을 한 사용자의 현재 학습 현황(식물 성장 단계, 목표, 학습 시간)을 시각적으로 보여주고 학습을 시작하는 메인 기능
- **관련 화면**: `Home`, `F-Home-02-1`, `F-Home-02-2`
- **진입 조건**: `SignInScreen`에서 로그인 성공 시, 하단 네비게이션 "홈" 아이콘 클릭 시
- **입력 항목**:
  - **DB**: 사용자 닉네임, 화분 이미지 ID, 화분 제목 & 총 공부시간
  - **디바이스 시스템**: 디바이스의 날짜
- **출력**: 로그인 완료 후 메인 화면으로 전환 → DB에서 사용자 닉네임, 마지막 사용 화분 이미지 ID, 제목, 총 공부시간 조회 → 디바이스 시스템에서 오늘 날짜 조회 → 화면의 지정된 위치에 맵핑 → 출력
- **예외처리**:
  - 공부할 화분의 이미지 로딩 실패 or 생성된 화분이 없을 시 기본 아이콘 출력
  - 홈 화면에서 홈 탭 클릭 시 새로고침, 스택에는 미 쌓임
  - 등록 목표가 없을 경우 → "첫 목표를 추가해보세요" 텍스트 출력 & 기본이미지, 기본형식 시간으로 표시

---

### F-Home-02 학습 화분 목록 조회

- **기능 설명**: 사용자가 생성한 전체 학습 목표 리스트를 그리드(Grid) 형태로 확인 기능
- **관련 화면**: `Home`, `F-Home-01`
- **진입 조건**: `F-Home-01`에서 스크롤 시
- **입력 항목**:
  - **DB**: 화분별 이미지 ID, 화분별 이름 & 총 공부시간
  - **사용자**: 화분 이미지 선택
- **출력**: `F-Home-01`에서 스크롤 시 → DB에서 화분별 이미지 ID, 화분 이름, 총 공부시간 조회 → Grid 형태에서 지정 형태 맞게 데이터 표출. 사용자 특정 화분 클릭 시 → DB에 메인 화분 이미지 ID, 화분 이름, 총 공부시간 저장 → 현재 공부중 태그 변경
- **예외처리**:
  - 키우고 있는 화분들의 이미지 로딩 실패 시 기본 아이콘 출력

---

### F-Home-03 새로운 학습 기록 생성

- **기능 설명**: 새로운 학습 주제를 설정하고, 이를 대표할 나무 종류와 태그를 선택하여 새로운 화분을 생성하는 기능
- **관련 화면**: `Home`, `MainHomeLow`, `NewBornTree`
- **진입 조건**: `F-Home-02`의 화분 추가 버튼 클릭 시
- **입력 항목**:
  - **DB**: 태그 항목 (String)
  - **사용자**: 나무 이미지 (Image), 태그 항목값 (Tag_id), 이름 (String), 완료 버튼 (Button)
- **출력**: 태그 선택, 이름 입력 → 완료 버튼 활성화 → 완료 버튼 클릭 → 사용자 입력 항목들 DB에 저장 → `F-Home-02` 화면으로 이동
- **예외처리**:
  - 이름 기입 시 공백만 기입 불가, 최대 글자 수 제한(한/영 15자)
  - 이미지, 태그, 이름 중 하나라도 미 기입 시 완료 버튼 미 활성화

---

## 공부중

### F-Study-01 학습 시간 측정

- **기능 설명**: 학습을 시작한 회원을 위한 공부 시간 측정 기능
- **관련 화면**: `Studying`, `StudyingScreen`
- **입력 항목**: `HomeScreen`의 공부 시작 버튼 클릭 시 진입 가능
  - uid: String, currentTag: String, profileImg: String, currentTitle: String
- **출력**: 진입과 동시에 스톱워치 작동 → 일시 정지 버튼 클릭 → 스톱워치 일시 정지 → 학습하기 버튼 클릭 → 스톱워치 재가동 → 학습 종료 버튼 클릭 → `StudyFinishDialog` 표시 → 종료 버튼 클릭 → 스톱워치 중지 → `StudyResultScreen` 진입
- **예외처리**:
  - `StudyFinishDialog` 취소 클릭 시 → `StudyScreen` 화면으로 복귀, 스톱워치 재가동
  - 뒤로가기 시 `StudyFinishDialog` 표출

---

### F-Study-02 내 학습 시간 공유

- **기능 설명**: 측정 중인 나의 학습 시간을 DB를 통해 같은 태그로 공부하는 타 사용자와 공유
- **관련 화면**: `Studying`, `StudyingScreen`
- **입력 항목**: uid, nickname, currentTag, myStudyTime (Long)
- **출력**: 5초마다 로컬DB에 내 공부 시간 저장 → 10분마다 서버로 공부 시간 저장 → 학습 종료 시 로컬DB 및 서버 데이터 삭제 → `StudyResultScreen` 진입
- **예외처리**:
  - 앱 진입 시 로컬 DB에 저장된 데이터 확인 → 있다면 비정상 종료로 간주
  - 비정상 종료 후 재진입 시 → 이전 학습 내용 저장 여부 확인 다이얼로그 표출

---

### F-Study-03 함께 공부하는 사람 조회

- **기능 설명**: 같은 태그로 공부하고 있는 사용자들 중 공부 시간 기준 상위 3명만 앱 하단에 노출
- **관련 화면**: `Studying`, `StudyingScreen`
- **입력 항목**: uid, currentTag, StudyUser(uid, nickname, img, studyTime)
- **출력**: 10분마다 DB에서 같은 태그 사용자 조회 → 상위 3명 화면 하단 출력
  > "$StudyUser.nickname $StudyUser.studytime분 째 공부중!"

---

### F-Study-04 학습 기록

- **기능 설명**: 학습을 완료한 사용자에게 학습한 내용을 기록할 수 있는 다이얼로그 제공
- **관련 화면**: `Studying`, `StudyFinishDialog`
- **입력 항목**: `StudyingScreen` 학습 종료 버튼 클릭 시 진입
  - 학습 기록: List\<String\>
- **출력**: 학습 기록 입력 → 종료 버튼 클릭 → 시스템 날짜, 공부 시작/종료 시간, 순공부시간, 제목과 함께 DB 저장 → `StudyResultScreen` 전환
- **예외처리**:
  - 취소 버튼 클릭 시 → `StudyingScreen`으로 복귀, 스톱워치 가동

---

### F-Study-05 결과창 이미지 저장

- **기능 설명**: 학습 완료한 사용자가 공부한 기록을 한 눈에 보고 갤러리에 저장
- **관련 화면**: `Studying`, `StudyResultScreen`
- **입력 항목**: `StudyResultScreen`의 이미지 저장 아이콘 클릭 시
- **출력**: 저장 아이콘 클릭 → 저장 확인 다이얼로그 → 예 버튼 → 결과창 비트맵 변환 → MediaStore로 /pictures에 저장 → "저장이 완료됐습니다!" 토스트
- **예외처리**:
  - 저장 확인 다이얼로그에서 취소 클릭 시 → 다이얼로그 닫힘

---

## 학습 계획 기록

### F-StudyPlanDetails-01 계획 상세보기

- **기능 설명**: 로그인한 사용자에게 기록된 학습 계획의 상세 내역을 제공
- **관련 화면**: `HomeMainLow`, `StudyPlanDetailsScreen`
- **입력 항목**: `HomeMainLow` 화분 이름 클릭 시 진입
  - **DB**: uid (String), 학습 기록 id (String)
- **출력**: uid, 학습 기록 id를 DB로 전송하여 기록된 학습 계획 상세 데이터 화면에 표시

---

### F-StudyPlanDetails-03 계획 제목 수정

- **기능 설명**: 학습 계획의 제목을 변경
- **관련 화면**: `StudyPlanDetailsScreen`
- **입력 항목**: 제목 수정 아이콘 클릭
  - 제목: String
  - **DB**: uid (String), 학습 기록 id (String)
- **출력**: 제목 수정 아이콘 클릭 → TextField에 새 제목 입력 → 확인 버튼 클릭 → 서버 전달 → 화면 반영
- **예외처리**:
  - TextField 비어있다면 확인 버튼 미 활성화

---

### F-StudyPlanDetails-04 개별 학습 삭제

- **기능 설명**: 학습 기록의 개별 학습 기록을 삭제
- **관련 화면**: `StudyPlanDetailsScreen`
- **입력 항목**: **DB** — uid, 학습 기록 id, 선택한 학습 id
- **출력**: 휴지통 아이콘 클릭 → 삭제 다이얼로그 → 예 클릭 → DB 삭제 → 화면 반영
- **예외처리**:
  - 삭제 다이얼로그에서 아니오 클릭 시 → 다이얼로그 닫기

---

### F-StudyPlanDetails-05 학습 기록 전체 삭제

- **기능 설명**: `StudyPlanDetailsScreen`에서 삭제 버튼 클릭하여 학습 기록 삭제
- **관련 화면**: `StudyPlanDetailsScreen`
- **입력 항목**: **DB** — uid, 화분 id
- **출력**: '계획 삭제하기' 버튼 클릭 → 삭제 다이얼로그 → DB 삭제 → `HomeMainLow` 표출
- **예외처리**:
  - 삭제 다이얼로그에서 아니오 클릭 시 → 다이얼로그 닫기

---

### F-StudyPlanDetails-06 학습 완료

- **기능 설명**: 학습 완료 시 상태를 "다 기른 나무"로 변경
- **관련 화면**: `StudyPlanDetailsScreen`
- **진입 조건**: 학습 완료하기 버튼 클릭
- **출력**: '학습 완료하기' 버튼 클릭 → 다이얼로그 → '완료' 클릭 → DB isCompleted update → `HomeMainHigh` 이동
- **예외처리**:
  - 다이얼로그 취소 버튼 클릭 시 닫힘

---

### F-StudyPlanDetails-07 학습 계획 공유

- **기능 설명**: 해당 화면의 학습기록을 게시글로 자동 게시, 완료 시 `CommunityPostScreen`으로 이동
- **관련 화면**: `StudyPlanDetailsScreen`, `CommunityPostScreen`
- **입력 항목**: **DB** — potId, tag_id, tag_name, title, 학습 기록 id
- **출력**: 공유할 리스트 선택 & 공유 아이콘 클릭 → 확인 다이얼로그 → 확인 → `CommunityPostScreen` 이동 → 게시물 작성 확인

---

## 커뮤니티

### F-Community-01 글 목록 조회

- **기능 설명**: 사용자가 커뮤니티에 작성된 게시글 목록을 조회
- **관련 화면**: `CommunityListScreen`
- **입력 항목**: 하단의 커뮤니티 탭 클릭 시 진입
- **출력**: 커뮤니티 탭 클릭 → 게시글 화면 진입 → DB에서 모든 게시글 조회 → 리스트 로딩
- **예외처리**:
  - 예외 발생 시 Toast로 출력

---

### F-Community-02 검색

- **기능 설명**: 검색어 또는 태그를 통해 원하는 게시글을 찾을 수 있는 기능
- **관련 화면**: `CommunityListScreen`
- **입력 항목**: 검색 버튼 또는 태그 버튼 클릭 시 진입
  - 검색: String, 태그: String
- **출력**: 검색어 입력 → IME 표시 → DB Query 실행 → 결과 리스트 출력
- **예외처리**:
  - 검색어가 없을 경우 → Toast: "검색어를 입력해주세요"
  - 검색 결과가 없을 경우 → Toast: "검색 결과가 없습니다"
  - 로딩 상태 (Progress), 빈 리스트 상태 (Empty View)

---

### F-Community-03 글 작성

- **기능 설명**: 사용자가 커뮤니티 게시판에 새로운 게시글을 작성하고 등록
- **관련 화면**: `CommunityPostScreen`
- **입력 항목**: 글쓰기 버튼 클릭 시 진입
  - 제목 입력: String, 태그 선택: Tag, 본문 입력: Text
- **출력**: 제목 입력 → 태그 선택 → 본문 입력 → 등록 버튼 클릭 → `CommunityDetailScreen` 진입
- **예외처리**:
  - 제목이 비어있는 경우 → Toast: "제목을 입력해주세요"
  - 본문이 비어있는 경우 → Toast: "내용을 입력해주세요"
  - 본문 1,000자 초과 시 → "제한된 글자수를 초과했습니다." 토스트 + 입력 불가
  - 뒤로가기 시 → "게시글 작성을 종료하시겠습니까?" 다이얼로그 표시

---

### F-Community-03-01 학습 기록 공유글 작성

- **기능 설명**: 사용자의 학습 기록을 공유 기능을 통해 커뮤니티에 게시
- **관련 화면**: `CommunityPostScreen`
- **입력 항목**: `StudyPlanDetailScreen`에서 공유 버튼 클릭 시 진입
  - 제목: String
- **출력**: 학습 기록 내용(제목, 태그, 학습 기록) 화면 표출 → 등록 버튼 클릭 → `CommunityDetailScreen` 전환
- **예외처리**:
  - 제목이 비어있는 경우 → Toast: "제목을 입력해주세요"
  - 뒤로가기 시 → "게시글 작성을 종료하시겠습니까?" 다이얼로그 표시

---

### F-Community-04 게시글 상세 조회

- **기능 설명**: 게시글 상세 조회 및 댓글 작성
- **관련 화면**: `CommunityDetailScreen`
- **입력 항목**: 글쓰기 완료 후 또는 커뮤니티 탭에서 게시글 클릭 시 진입
  - 작성글 제목, 닉네임, 작성일자, 본문, 기존 댓글, 좋아요/댓글수, 프로필 이미지
- **출력**: DB에서 추출 후 내용 출력
- **예외처리**:
  - 댓글 입력 없이 등록 시 → Toast: "댓글을 입력해주세요"
  - 본문 1,000자 초과 시 → "제한된 글자수를 초과했습니다." 토스트 + 입력 불가

---

### F-Community-05 댓글

- **기능 설명**: 게시글 댓글 작성
- **관련 화면**: `CommunityListScreen`, `CommunityDetailScreen`
- **입력 항목**: 커뮤니티 탭에서 게시글 클릭 시 진입
  - 닉네임, 작성일자, 댓글 본문란, 기존 댓글, 좋아요, 댓글수, 프로필 이미지
- **출력**: DB에서 추출 후 내용 출력
- **예외처리**:
  - 댓글 입력 없이 등록 시 → Toast: "댓글을 입력해주세요"
  - 댓글 1,500자 초과 시 → "제한된 글자수를 초과했습니다." 토스트 + 입력 불가

---

### F-Community-06 내 게시글 편집

- **기능 설명**: 게시글 조회 후 편집
- **관련 화면**: `CommunityDetailScreen`, `CommunityListScreen`
- **입력 항목**: 커뮤니티 탭에서 게시글 클릭 시 진입
  - 작성글 제목, 닉네임, 작성일자, 본문, 기존 본문, 프로필 이미지, 수정하기 버튼
- **출력**: DB에서 추출 후 내용 출력
- **예외처리**:
  - 본문 입력 없이 등록 시 → Toast: "본문을 입력해주세요"

---

### F-Community-07 내 댓글 편집

- **기능 설명**: 커뮤니티 게시글 상세 화면에서 본인이 작성한 댓글을 인라인 수정 또는 삭제
- **관련 화면**: `CommunityDetailScreen`
- **입력 항목**: `CommunityDetailScreen`에서 본인 댓글의 수정/삭제 아이콘 클릭 시 진행
  - 수정 아이콘 (`ic_edit`): IconButton (28dp) → 인라인 편집 모드 진입
  - 삭제 아이콘 (`ic_trash`): IconButton (28dp) → 삭제 확인 다이얼로그 표시
  - 수정 시 댓글 내용: String (최대 100자)
  > 본인 댓글(`commentData.user.uid == CurrentUser.uid`)이고 편집 중이 아닐 때만 아이콘 노출
- **출력**:
  - **[수정]** 수정 아이콘 클릭 → 기존 댓글 내용이 인라인 TextField에 로드 → 내용 수정 (100자 제한, 글자수 카운터 표시) → '저장' 클릭: Firestore `posts/{postId}/comments/{commentId}` 업데이트 → 편집 초기화 → 댓글 새로고침 / '취소' 클릭: 편집 초기화
  - **[삭제]** 삭제 아이콘 클릭 → `ConfirmDialog` → '예' 클릭 → Firestore 문서 삭제 → 댓글 새로고침
- **예외처리**:
  - 삭제 다이얼로그에서 '아니오' 클릭 시 → 기존 화면 유지
  - 수정 시 빈 내용으로 저장 클릭 → 저장 무시 (`return`)
  - 100자 초과 입력 시 → '100자 이하로 입력해주세요.' 토스트 + 입력 차단
  - Firestore 수정/삭제 실패 시 → 로그 출력 후 편집 초기화 + 댓글 새로고침

---

## 마이페이지/아카이브

### F-Mypage-archive-01 학습 완료 상태의 화분 리스트

- **기능 설명**: 사용자가 마이페이지 → "기른 나무 수" 클릭하여 이동, 해당 나무의 학습 기록과 날짜별 세부 학습 내역을 조회
- **관련 화면**: `MypageMain`, `MyPageArchive`
- **입력 항목**:
  - **DB**: uid, users/{userId}/pots isCompleted == true
  - **사용자**: 화분 이미지 선택
- **출력**: 선택된 나무 ID 기반 데이터 조회 → 날짜별 학습 기록 리스트
- **예외처리**:
  - 데이터 응답이 없는 경우 → Toast: "데이터를 불러올 수 없습니다"
  - 기록이 없는 경우 → Empty View: "기록이 없습니다"

---

### F-Mypage-archive-02 기른 나무 상세 조회

- **기능 설명**: 아카이브에서 특정 "기른 나무"를 선택하면 학습 기록(기간, 총 공부시간 등)과 날짜별 세부 내역을 조회
- **관련 화면**: `MyPageArchiveScreen`, `MyPageArchiveDetailScreen`
- **입력 항목**: 제목, 공유 버튼, 화분 이미지, 시작일, 종료일, 총 공부시간
- **출력**: 선택된 나무 ID 기반 데이터 조회 → 날짜별 학습 기록 리스트 (기본 2줄 표시) → 클릭 시 상세 학습기록 다이얼로그 표시
- **예외처리**:
  - 데이터 응답이 없는 경우 → Toast: "데이터를 불러올 수 없습니다"
  - 기록이 없는 경우 → Empty View: "기록이 없습니다"

---

### F-Mypage-archive-03 기른 나무 학습 기록 공유

- **기능 설명**: 공유하기 버튼으로 학습 기록을 커뮤니티에 게시, 완료 시 `CommunityPostScreen`으로 이동
- **관련 화면**: `MyPageArchiveDetailScreen`, `CommunityPostScreen`
- **입력 항목**: **DB** — postId, potId, tag, title, studyLogIds (List\<String\>)
- **출력**: 공유할 리스트 선택 → 공유 아이콘 클릭 → 화분 id, 태그, 제목, 학습 기록 id 전달
- **예외처리**:
  - 선택한 기록 없을 경우 → Toast: "공유할 기록을 선택해 주세요"
  - 데이터 응답이 없는 경우 → Toast: "데이터를 불러올 수 없습니다"
  - 기록이 없는 경우 → Empty View: "기록이 없습니다"

---

## 마이페이지

### F-Mypage-01 프로필 변경

- **기능 설명**: 대표 아이콘, 닉네임 변경
- **관련 화면**: `MyPageScreen`
- **입력 항목**: `MyPageScreen`에서 프로필 이미지 클릭 시 다이얼로그 표시
  - **DB**: uid, nickname, users.pots (각 문서 level), profileImg
  - **사용자**: 변경하려는 닉네임 입력, 변경하려는 화분 이미지 선택
- **출력**: 닉네임 TextField 입력 → 저장 클릭 → 글자수 2~10글자 확인 → 서버 전달 → 닉네임 중복 검사 → 중복 아니면 DB update → "수정이 완료되었습니다" 토스트 → 다이얼로그 닫기 → 화면 반영
- **예외처리**:
  - 닉네임 중복 → "중복된 닉네임입니다." 토스트
  - 닉네임 미기재 → "닉네임을 입력해주세요" 토스트

---

### F-Mypage-02 커뮤니티 활동 내역 조회

- **기능 설명**: 사용자가 작성한 게시글, 댓글을 단 게시글, 좋아요를 누른 게시글 목록을 확인하고 관리
- **관련 화면**: `MyPageScreen`, `MyCommuntiyFeedScreen`
- **진입 조건**: `MyPageScreen`에서 내 활동 버튼 클릭 시
- **입력 항목**: 활동 태그 (List\<String\>), uid, targetId, title, comment, commentId
- **출력**: 게시글/댓글/좋아요 중 택 1 → 선택한 탭에 따라 Firebase activities 컬렉션에서 목록 표출 → 아이템 클릭 시 해당 게시글로 이동

---

## 마이페이지/세팅

### F-MypageSetting-01 앱 테마 설정

- **기능 설명**: 앱 테마 변경 기능
- **관련 화면**: `MypageMain`
- **진입 조건**: `MypageScreen` 화면에서 다크모드 토글버튼 클릭
- **입력 항목**:
  - **DB**: uid, isDarkMode (Boolean)
  - **사용자**: 다크모드 토글 버튼 클릭
- **출력**: 토글 클릭 → isDarkMode 필드 true/false 변경 → 앱 전체에 변경된 테마 반영
