# Play Console 앱 콘텐츠 답변 초안 (#632)

비공개 테스트 트랙에 올리기 전에 Play Console > 정책 > **앱 콘텐츠**에 적을 답변을 미리 정리한 문서다. 콘솔 화면의 질문 문구·항목 이름은 바뀔 수 있으므로, 실제로 입력할 때는 **그 시점의 콘솔 안내를 기준**으로 하고 이 문서와 다르면 이 문서를 고친다.

근거 표시: **(코드)** 레포 코드에서 직접 확인, **(처리방침)** `docs/legal/privacy.md`, **(미확인)** 콘솔·정책 문서로 아직 확인하지 않음.

## 0. 이 문서와 함께 바꿔야 하는 것

앱이 수집하는 데이터, 탈퇴 처리, 포그라운드 서비스가 바뀌면 아래를 같은 PR에서 함께 확인한다.

| 바뀌는 것 | 함께 확인할 곳 |
|-----------|---------------|
| 수집 항목·SDK 추가/설정 변경 | 처리방침(`docs/legal/privacy.md`), 이 문서 2절(데이터 보안), 4절(광고 ID) |
| 탈퇴 처리(`functions/accountDeletion.js`의 `deleteUserData`), 계정 삭제 안내 1절이 인용하는 탈퇴 경로 문구(`BottomBar.kt` "마이페이지", `MypageScreen.kt` "계정 관리", `AccountManageScreen.kt` 회원탈퇴·확인 창 문구·버튼, `DeleteAccountViewModel.kt` 완료 안내) | 계정 삭제 안내(`docs/legal/account-deletion.md`), 처리방침 3절, 이 문서 3절 |
| `StudyingTimerService`의 서비스 유형·서브타입, 서비스 동작(`onStartCommand`, `START_NOT_STICKY`, 알림 갱신·주기 저장 간격) | `AndroidManifest.xml`, 이 문서 1절 |
| 학습 타이머 서비스를 시작·중지하는 곳(`StudyingViewModel`의 `sendServiceIntent` 호출, `StudyingTimerServiceController.stop()` 호출, 알림을 눌렀을 때 여는 화면) | 이 문서 1절 실제 동작 표와 영어 설명 |

## 1. 포그라운드 서비스 권한

### 앱의 현재 선언 (코드)

- 권한: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` (`app/src/main/AndroidManifest.xml`)
- 서비스: `StudyingTimerService`, `foregroundServiceType="specialUse"`
- 서브타입(`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`): "User-initiated study stopwatch that keeps counting elapsed time and shows it in an ongoing notification while the timer is running"

### 실제 동작 (코드, `StudyingTimerService.kt`·`StudyingViewModel.kt`)

서비스를 시작·중지하는 곳을 앱 전체에서 검색해 나열했다 (`ACTION_START`·`ACTION_PAUSE`·`ACTION_STOP`·`stopService`·`StudyingTimerServiceController.stop()`). 디버그 빌드 전용 경로는 따로 표시했다.

| 동작 | 코드 |
|------|------|
| 시작: 세션을 시작할 때 목표 입력 창을 닫으면("완료", 또는 "닫기" — 작성한 글이 있으면 "버리기"까지), 일시정지 뒤 "학습하기"를 누를 때, 공부 중에 연 종료 확인 창을 종료하지 않고 닫을 때, 학습 화면에 들어갈 때 같은 화분의 공부 중 백업이 남아 있을 때(일시정지 상태가 아니던 세션). 마지막 경우는 주로 시스템이 백그라운드에서 앱 프로세스를 끝낸 뒤 앱으로 돌아와 학습 화면이 복원될 때다. 앱을 새로 시작하면 보통은 홈의 복구 창이 먼저 떠서 이어서 공부하지 않지만, 쌓인 시간이 0인 백업은 복구 창 대상이 아니라(`isRecoverableBy`: `time > 0L`, 밀리초) 학습 화면에 들어가면 바로 이어짐(목표도 없는 0 백업은 목표 입력 창만 다시 띄움) | `startStopwatch()` → `ACTION_START` → `startForeground(...)` (목표 입력 창 닫기: `onStudyingStatusChange(restore)`, 종료 확인 창 닫기: `onStudyingStatusChange(true)`, 복원: `restoreFromLocalBackup`) |
| 디버그 빌드 전용: 공부 중 "+1h" 버튼은 서비스에 늘어난 시간을 다시 보냄 | `addDebugTime()` → `ACTION_START` (develop 기준 `BuildConfig.DEBUG`일 때만 버튼 표시, #590) |
| 실행 중 알림의 경과 시간을 1초마다 갱신 | `ms - lastNotificationMs >= 1_000L`일 때 `updateNotification(ms)` |
| 실행 중 30초마다 기기에 학습 시간 백업 (강제 종료 뒤 복원용) | `ms - lastSaveMs >= LOCAL_BACKUP_INTERVAL_MS`(`30_000L`)일 때 `saveTime(ms)` |
| 알림을 누르면 앱이 열림. 학습 화면이 남아 있으면 학습 화면으로 돌아오고, 최근 앱에서 밀어서 닫은 뒤라면 로그인 상태일 때 홈이 열려 아래 "홈 화면이 보일 때마다" 줄처럼 서비스가 정리되고 복구 창이 뜸 (세션 만료면 로그인 화면, 업데이트가 필요하면 업데이트 안내가 열리고 같은 줄의 다른 경로로 서비스가 정리됨. 자동 로그인 결과가 로그인 안 됨·실패면 로그인 화면이 열리지만 `SplashViewModel`이 `startSignIn()`만 불러 서비스·알림이 남고, 로그인해 홈이 보일 때 정리됨. 로그인하지 않고 나가면 계속 남음) | `setContentIntent(contentIntent)` (`MainActivity`를 `FLAG_ACTIVITY_SINGLE_TOP or FLAG_ACTIVITY_NEW_TASK`로 엶) |
| 일시정지(일시정지 버튼, 공부 중에 종료 확인 창 열기: "학습종료" 버튼·뒤로 가기) → 서비스와 알림 종료 | `stopStopwatch()` → `ACTION_PAUSE` → `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf(startId)` |
| 학습 종료, 시작한 지 7일이 지난 세션(`stopIfSessionTooOld`), 오류 창 확인(`onErrorConfirmClicked`) → 서비스와 알림 종료 | `ACTION_STOP` → `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()` |
| 세션 만료, 업데이트 필요 화면, 홈 화면이 보일 때마다(처음 들어올 때 포함) → 남아 있는 서비스 정리. 예: 공부 중 최근 앱에서 밀어서 닫으면 프로세스·서비스·알림은 남고 화면만 사라지며, 그 뒤 알림을 누르면 홈이 열려 서비스·알림이 정리되고 복구 창("이전 학습 기록 발견")이 뜸. 복구 창의 시간은 마지막으로 백업한 시간이라 직전 알림 시간보다 짧을 수 있음 (이 경우 마지막 백업은 학습 화면이 가려질 때 `onScreenStopped`가 쓴 값이거나 그 뒤 서비스의 주기 저장 값) (2026-11-25 SM-N986N Android 13, develop `4d24fa5c` debug 빌드로 확인. 다른 기기·OS는 미확인) | `StudyingTimerServiceController.stop()` → `stopService(...)` (`MainActivity`, `HomeViewModel.onHomeResumed`) |
| 시스템이 서비스를 끝내도 다시 시작하지 않음 | `START_NOT_STICKY` |

### 콘솔 설명 초안 (영어)

> 아래 문단을 그대로 붙여 넣고, 콘솔이 요구하는 글자 수에 맞춰 줄인다

Plant is a study app where users grow a virtual plant with the time they spend studying. The user starts a study session on the study screen: the stopwatch starts when the user finishes or closes the study-goal input dialog at the beginning of a session, or taps the resume button after pausing. Opening the end-session confirmation dialog pauses a running stopwatch, and closing that dialog without ending the session resumes it. While the stopwatch is running, the foreground service keeps counting the elapsed study time and shows it in an ongoing notification while the user switches to other apps (for example, to read study materials). It also periodically saves the elapsed time on the device. Tapping the notification opens the app (the study screen if it is still open). The service runs only while the user's stopwatch is running: it stops when the user pauses or ends the session, and the system does not restart it. A study session can last for hours, and the use is not media playback, location, phone calls, or network data transfer.

- 확인 필요 (미확인): 콘솔에 넣기 전에 Android 공식 문서의 포그라운드 서비스 유형 목록을 읽고, 시간 제한이 있는 유형(예: shortService)이나 다른 유형이 맞지 않는 이유를 한 문장 덧붙일지 정한다
- 영상에서 "완료"·"학습하기" 버튼이 보이게 찍어, 위 설명의 "finishes the study-goal input dialog", "resume button"과 맞춰 볼 수 있게 한다

### 영상 (Todo)

- 준비: Android 13 이상은 학습 화면에 처음 들어갈 때 알림 권한을 묻는다(`StudyingScreen`의 `POST_NOTIFICATIONS` 요청). 허용해야 알림이 보이므로, 녹화 전에 허용해 두거나 영상에서 허용하는 장면을 넣는다
- 순서: 학습 화면에 들어가 목표 입력 창에서 "완료" → 타이머 시작 → 홈 버튼으로 앱 나가기 → 알림창에서 시간이 올라가는 것 보여 주기 → 알림 눌러 복귀 → "일시정지"를 누르면 알림이 사라지는 것 보여 주기 → "학습하기"로 다시 시작 → 종료. 녹화 중 최근 앱에서 Plant를 밀어서 닫지 않는다 (밀어서 닫은 뒤 알림을 누르면 학습 화면이 아니라 홈이 열림, 1절 실제 동작 표)
- 링크: YouTube "일부 공개" 등 로그인 없이 볼 수 있는 링크 (미확인: 콘솔이 받는 링크 형식)
- 녹화 빌드: release 빌드로 찍는다 (debug 빌드는 학습 화면에 "+1h [DEBUG]" 버튼이 보임). 영상은 앱 동작이 선언과 같은지 보여 주는 용도라, 타이머·알림 코드가 심사에 올릴 버전과 같으면 된다. 가장 확실한 건 올릴 AAB와 같은 커밋으로 만든 빌드

### 거절되면

거절 사유를 보고 앱 수정 이슈를 새로 만든다. 검토할 대안:

| 대안 | 설명 | 걸리는 점 |
|------|------|----------|
| 서비스 없이 시작 시각만 저장 | 시작 시각을 저장하고 앱으로 돌아오면 지금 시각과 차이로 계산. 알림 대신 앱 복귀 시 표시 | 학습 중 알림이 없어짐. 기기 시각 변경 대응 필요 |
| 다른 서비스 유형 | 공식 유형 목록에서 학습 타이머에 맞는 유형을 다시 찾음 | 미확인: 맞는 유형이 있는지 |

## 2. 데이터 보안(Data safety)

처리방침 1절의 항목은 모두 아래 "수집 항목" 표 중 한 줄에 넣거나, "넣지 않은 항목" 표에 이유를 적는다. 콘솔의 데이터 유형 이름과 분류는 콘솔에서 확인한다 (미확인). "(분류 미확인)"이 붙은 줄은 어느 유형에 넣을지 콘솔 도움말을 보고 정한다.

### 수집 항목

| 콘솔 데이터 유형 (예상) | 앱의 데이터 | 목적 | 근거 |
|------------------------|-------------|------|------|
| 개인 정보 > 이메일 주소 | Google 계정 이메일 | 계정 관리 | 처리방침 1절 가 |
| 개인 정보 > 이름 | 닉네임 | 앱 기능 (커뮤니티 표시 이름) | 처리방침 1절 나. Play 도움말의 "이름" 정의에 nickname이 들어 있음 |
| 개인 정보 > 사용자 ID | Firebase UID | 계정 관리 | 처리방침 1절 가 |
| 앱 활동 > 앱 상호작용 | 학습 시간, 좋아요·북마크, 출석 체크 기록, 차단 목록(차단 시각 포함), 실시간 학습 현황, 프로필 이미지 선택값(앱이 제공하는 이미지 중 고른 값이라 "사진"이 아님) | 앱 기능 | 처리방침 1절 나·다 |
| 앱 활동 > 앱 상호작용 | Firebase Analytics가 기록하는 앱 사용 이벤트, 앱 실행 횟수·세션 시간 | 분석 | 처리방침 1절 마 |
| 앱 활동 > 기타 사용자 제작 콘텐츠 | 게시글·댓글(게시글에 공유한 학습 기록 포함), 학습 일지, 신고(대상·사유·상세 내용, 신고 일시·처리 상태 포함), 버그 제보·개선 제안(분류·내용·재현 방법, 작성 일시 포함) | 앱 기능, 서비스 개선 | 처리방침 1절 다·라 |
| 앱 활동 (분류 미확인) | 화분(학습 계획) 데이터, 골드·아이템 보유량, 보너스 박스 열기 결과 기록, 가입 동의 기록, 가입·최근 로그인 시각 | 앱 기능, 계정 관리 | 처리방침 1절 가·다 |
| 앱 정보 및 성능 > 비정상 종료 로그·진단 | Crashlytics 오류 보고서·사용 세션 정보·전송 환경 정보(언어·국가 설정, 통신사 국가 코드 제외), 버그 제보·개선 제안의 앱 버전, 버그 제보의 기기 모델·Android 버전, Analytics의 기기 정보(OS 버전·기기 모델) | 분석 | 처리방침 1절 라·마·바 |
| 기기 또는 기타 ID | 앱 인스턴스 ID, 광고 ID, Firebase 설치 ID, Crashlytics 설치 식별자 | 분석 | 처리방침 1절 마·바 |
| 기기 또는 기타 ID (분류 미확인) | 로그인 세션 식별값 | 계정 관리 (다른 기기 로그인 시 이전 기기 로그아웃) | 처리방침 1절 다 |

### 넣지 않은 항목

| 항목 | 이유 |
|------|------|
| 기기에만 저장되는 정보 (처리방침 1절 사) | 서버로 보내지 않음 (미확인: 콘솔의 "수집" 정의와 맞는지) |
| 국가 및 언어 설정 (처리방침 1절 마), Crashlytics 전송 환경 정보 중 언어·국가 설정과 통신사 국가 코드 (1절 바) | 미확인: "대략적인 위치" 등 어느 유형에 해당하는지 콘솔 도움말로 확인한 뒤 위 표로 옮긴다. 1절 바의 나머지 전송 환경 정보는 위 진단 줄에 넣었다 |
| 사진·위치·연락처·금융 정보 | 수집하지 않음 (처리방침에 없음) |

### 처리방침에 아직 없는 것 (#781)

Firebase [Play 데이터 공개 안내](https://firebase.google.com/docs/android/play-data-disclosure) 기준으로, 앱이 쓰는 SDK가 아래를 수집한다. 처리방침과 위 표에 넣을지 #781에서 정하고, **데이터 보안 양식 입력 전에** 끝낸다.

- IP 주소: Authentication(가입·인증 중 보안·남용 방지), Cloud Functions 클라이언트(함수 호출 시)
- Play Integrity 무결성 토큰: App Check (release 빌드가 쓰는 Play Integrity 제공자 `PlayIntegrityAppCheckProviderFactory`, `app/src/release/.../AppCheckProvider.kt`)

### 그 밖의 질문

| 질문 | 답변 초안 | 근거 |
|------|----------|------|
| 전송 중 암호화 | 예 | 처리방침 8절 (Firebase는 HTTPS) |
| 사용자가 데이터 삭제를 요청할 수 있음 | 예 (앱 내 탈퇴 + 계정 삭제 안내 페이지) | 이 문서 3절 |
| 제3자와 공유 | 아니오. Play 도움말은 개발자를 대신해 처리하는 "서비스 제공자"(Firebase)로의 전송을 공유로 보지 않음 | 처리방침 5절 |
| 커뮤니티 게시글이 다른 이용자에게 공개됨 | 미확인: 이용자가 스스로 공개하는 것이 "공유"에 해당하는지 콘솔 도움말로 확인 | 처리방침 1절 다 |

## 3. 계정 삭제

- 계정 삭제 URL: https://plant-82b09.web.app/account-deletion.html (원본 `docs/legal/account-deletion.md`, 배포 절차 `docs/LEGAL_HOSTING.md`)
- 앱 안 경로: 마이페이지 > 계정 관리 > 회원탈퇴 → 확인 2번 → Google 계정 재확인 (코드: `AccountManageScreen.kt`)
- 앱 없이 요청: 문의 메일 (안내 페이지 2절). 운영자 처리 절차: `docs/ACCOUNT_DELETION_REQUEST.md` (#778)
- 계정을 유지한 채 일부만 삭제: 앱에서 직접 지울 수 있는 것은 내 게시글·댓글, 좋아요·북마크(해제), 화분·학습 기록, 커뮤니티 활동 내역(선택 삭제), 차단(해제). 신고·제보 내용 속 개인정보는 메일로 요청 (안내 페이지 3절 나). 콘솔에 일부 삭제 방법을 따로 적는 칸이 있는지는 콘솔에서 확인 (미확인)
  - 코드: `CommunityDetailViewModel.deletePost`·`deleteComment`·`ToggleLikeUseCase`·`ToggleBookmarkUseCase`, `StudyPlanDetailViewModel`(`DeleteEntirePotUseCase`·`DeleteStudyLogUseCase`), `CommunityActivityViewModel.deleteSelected`, `BlockedUsersViewModel`(`UnblockUserUseCase`)

## 4. 그 밖의 선언

| 항목 | 답변 초안 | 근거·비고 |
|------|----------|----------|
| 개인정보처리방침 URL | https://plant-82b09.web.app/privacy-policy.html | `LegalUrls.PRIVACY_POLICY` |
| 광고 포함 | 아니오 | 광고 SDK 없음 (코드: `app/build.gradle.kts`) |
| 광고 ID | 사용함, 용도 "분석" | #707 결정. `firebase-analytics`가 `AD_ID` 권한을 추가함 (이슈 본문 참고) |
| 앱 액세스 권한 | 로그인 필요. 심사용 Google 계정을 제공 (계정 준비됨. 이메일·비밀번호는 콘솔에만 적고 레포·이슈에 남기지 않음) | 첫 로그인 때 만 14세 이상 확인·약관 동의·닉네임 설정이 필요하고, 다른 기기에서 같은 계정으로 로그인하면 이전 기기는 로그아웃된다는 점을 함께 적기 |
| 타겟층 | 결정 필요. 약관상 만 14세 이상만 가입 | `docs/legal/terms.md` 제4조 2항 |
| 콘텐츠 등급 설문 | 사용자 간 소통(게시글·댓글) 있음, 사용자 제작 콘텐츠 있음, 실제 돈으로 사는 상품 없음(골드는 앱 안 보상으로만 얻음) | 결제 SDK 없음 (코드) |
| 사용자 제작 콘텐츠 관리 | 게시글·댓글 신고, 신고 5건 이상이면 블라인드, 사용자 차단(차단한 사용자의 글·댓글을 내 화면에서 숨김, 마이페이지 > 차단 목록에서 해제) | (코드) `Post.kt`의 `BLIND_REPORT_THRESHOLD = 5`, #660 |
| 정부 앱·금융 기능·건강 앱 | 해당 없음 | |

## 5. 비공개 테스트 트랙

- 테스터 목록(이메일·Google 그룹), 국가, 스토어 등록 정보(앱 이름·설명·스크린샷·아이콘)
- 개발자 계정 종류에 따른 프로덕션 신청 전 조건(테스터 수·기간)은 콘솔에서 확인 (미확인)
- 올릴 파일: #621 서명 AAB

## 6. 입력 전 확인 목록

- [ ] 계정 삭제 안내 페이지가 배포되어 로그인 없이 열린다
- [ ] 운영 Firestore에 `users/{uid}/blocks` 규칙이 배포되어 있다 (사용자 차단 설명 전에)
- [ ] 최신 처리방침(2026년 11월 26일 시행본 이후, #781 반영본)이 배포되어 있고, 데이터 보안 답변과 항목이 같다
- [ ] 포그라운드 서비스 영상 링크가 로그인 없이 열린다
- [ ] #781(IP 주소·App Check 토큰의 처리방침 반영)이 끝났다 (데이터 보안 양식 입력 전에)
- [ ] #778(메일로 받은 삭제 요청 처리 도구)이 준비되어 있다 (처리방침 9절이 2026년 11월 21일 시행본부터 가입을 마치지 않은 계정의 메일 삭제 요청을 안내하고, 11월 26일 시행본은 4절·6절에서 앱 없이 삭제하려는 경우까지 넓히며, 계정 삭제 URL을 등록하면 더 늘어남)
- [ ] 스토어 등록 정보의 앱 이름·개발자 이름이 계정 삭제 안내 페이지의 이름과 맞다 (미확인: Play가 요구하는 일치 범위)
- [ ] 다른 이용자의 차단 목록에 남는 기록(#779)을 고쳤다면 처리방침 3절 첫 문단의 "다만 …(아래 가의 ※)" 문장, 처리방침 3절 가 ※, 계정 삭제 안내 3절 가 ※를 함께 고쳤다
