# Play Console 앱 콘텐츠 답변 초안 (#632)

비공개 테스트 트랙에 올리기 전에 Play Console > 정책 > **앱 콘텐츠**에 적을 답변을 미리 정리한 문서다. 콘솔 화면의 질문 문구·항목 이름은 바뀔 수 있으므로, 실제로 입력할 때는 **그 시점의 콘솔 안내를 기준**으로 하고 이 문서와 다르면 이 문서를 고친다.

근거 표시: **(코드)** 레포 코드에서 직접 확인, **(처리방침)** `docs/legal/privacy.md`, **(미확인)** 콘솔·정책 문서로 아직 확인하지 않음.

## 0. 이 문서와 함께 바꿔야 하는 것

앱이 수집하는 데이터, 탈퇴 처리, 포그라운드 서비스가 바뀌면 아래를 같은 PR에서 함께 확인한다.

| 바뀌는 것 | 함께 확인할 곳 |
|-----------|---------------|
| 수집 항목·SDK 추가/설정 변경 | 처리방침(`docs/legal/privacy.md`), 이 문서 2절(데이터 보안), 4절(광고 ID) |
| 탈퇴 처리(`functions/index.js`의 `deleteUserData`), 탈퇴 화면 문구 | 계정 삭제 안내(`docs/legal/account-deletion.md`), 처리방침 3절, 이 문서 3절 |
| `StudyingTimerService`의 서비스 유형·서브타입 | `AndroidManifest.xml`, 이 문서 1절 |

## 1. 포그라운드 서비스 권한

### 앱의 현재 선언 (코드)

- 권한: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` (`app/src/main/AndroidManifest.xml`)
- 서비스: `StudyingTimerService`, `foregroundServiceType="specialUse"`
- 서브타입(`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`): "User-initiated study stopwatch that keeps counting elapsed time and shows it in an ongoing notification while the timer is running"

### 실제 동작 (코드, `StudyingTimerService.kt`)

| 동작 | 코드 |
|------|------|
| 사용자가 학습 화면에서 타이머를 시작·재개하면 시작 | `ACTION_START` → `startForeground(...)` |
| 실행 중 알림의 경과 시간을 1초마다 갱신 | `ms - lastNotificationMs >= 1_000L`일 때 `updateNotification(ms)` |
| 알림을 누르면 앱으로 돌아옴 | `setContentIntent(contentIntent)` |
| 일시정지하면 서비스와 알림 종료 | `ACTION_PAUSE` → `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf(startId)` |
| 학습 종료 시 서비스와 알림 종료 | `ACTION_STOP` → `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()` |
| 시스템이 서비스를 끝내도 다시 시작하지 않음 | `START_NOT_STICKY` |

### 콘솔 설명 초안 (영어)

> 아래 문단을 그대로 붙여 넣고, 콘솔이 요구하는 글자 수에 맞춰 줄인다

Plant is a study app where users grow a virtual plant with the time they spend studying. When the user taps the start button on the study screen, the app starts a stopwatch. The foreground service keeps counting the elapsed study time and shows it in an ongoing notification while the user switches to other apps (for example, to read study materials). Tapping the notification returns to the study screen. The service runs only while the user-started timer is running and stops immediately when the user pauses or ends the session. No other foreground service type fits this use: the session has no fixed upper time limit, and it is not media playback, location, data sync, or a phone call.

### 영상 (Todo)

- 순서: 학습 화면에서 시작 → 홈 버튼으로 앱 나가기 → 알림창에서 시간이 올라가는 것 보여 주기 → 알림 눌러 복귀 → 종료하면 알림이 사라지는 것 보여 주기
- 링크: YouTube "일부 공개" 등 로그인 없이 볼 수 있는 링크 (미확인: 콘솔이 받는 링크 형식)
- 녹화 빌드: 영상은 앱 동작이 선언과 같은지 보여 주는 용도라, 타이머·알림 코드가 심사에 올릴 버전과 같으면 된다. 가장 확실한 건 올릴 AAB와 같은 커밋으로 만든 빌드
### 거절되면

거절 사유를 보고 앱 수정 이슈를 새로 만든다. 검토할 대안:

| 대안 | 설명 | 걸리는 점 |
|------|------|----------|
| 서비스 없이 시작 시각만 저장 | 시작 시각을 저장하고 앱으로 돌아오면 지금 시각과 차이로 계산. 알림 대신 앱 복귀 시 표시 | 학습 중 알림이 없어짐. 기기 시각 변경 대응 필요 |
| 다른 서비스 유형 | 공식 유형 목록에서 학습 타이머에 맞는 유형을 다시 찾음 | 미확인: 맞는 유형이 있는지 |

## 2. 데이터 보안(Data safety)

처리방침 1절·2절과 1:1로 맞춘다. 콘솔의 데이터 유형 이름은 콘솔에서 확인 (미확인).

### 수집 항목

| 콘솔 데이터 유형 (예상) | 앱의 데이터 | 목적 | 근거 |
|------------------------|-------------|------|------|
| 개인 정보 > 이메일 주소 | Google 계정 이메일 | 계정 관리 | 처리방침 1절 가 |
| 개인 정보 > 이름 | 닉네임 | 앱 기능 (커뮤니티 표시 이름) | 처리방침 1절 나. Play 도움말의 "이름" 정의에 nickname이 들어 있음 |
| 개인 정보 > 사용자 ID | Firebase UID | 계정 관리 | 처리방침 1절 가 |
| 앱 활동 > 앱 상호작용 | 학습 시간, 좋아요·북마크, 출석, 차단 목록, 실시간 학습 현황, 프로필 이미지 선택값(앱이 제공하는 이미지 중 고른 값이라 "사진"이 아님) | 앱 기능 | 처리방침 1절 다 |
| 앱 활동 > 기타 사용자 제작 콘텐츠 | 게시글·댓글, 학습 일지, 신고 상세 내용, 버그 제보·개선 제안 | 앱 기능, 서비스 개선 | 처리방침 1절 다·라 |
| 앱 정보 및 성능 > 비정상 종료 로그·진단 | Crashlytics 오류 보고서·사용 세션 정보, 버그 제보의 기기 모델·Android 버전 | 분석 | 처리방침 1절 라·바 |
| 기기 또는 기타 ID | 앱 인스턴스 ID, 광고 ID, Firebase 설치 ID, Crashlytics 설치 식별자 | 분석 | 처리방침 1절 마·바 |

- 사진·위치·연락처·금융 정보: 수집하지 않음 (처리방침에 없음)
- 기기에만 저장되는 정보(처리방침 1절 사)는 서버로 보내지 않으므로 "수집"에 넣지 않음 (미확인: 콘솔의 "수집" 정의와 맞는지)

### 그 밖의 질문

| 질문 | 답변 초안 | 근거 |
|------|----------|------|
| 전송 중 암호화 | 예 | 처리방침 8절 (Firebase는 HTTPS) |
| 사용자가 데이터 삭제를 요청할 수 있음 | 예 (앱 내 탈퇴 + 계정 삭제 안내 페이지) | 3절 |
| 제3자와 공유 | 아니오. Play 도움말은 개발자를 대신해 처리하는 "서비스 제공자"(Firebase)로의 전송을 공유로 보지 않음 | 처리방침 5절 |
| 커뮤니티 게시글이 다른 이용자에게 공개됨 | 미확인: 이용자가 스스로 공개하는 것이 "공유"에 해당하는지 콘솔 도움말로 확인 | 처리방침 1절 다 |

## 3. 계정 삭제

- 계정 삭제 URL: https://plant-82b09.web.app/account-deletion.html (원본 `docs/legal/account-deletion.md`, 배포 절차 `docs/LEGAL_HOSTING.md`)
- 앱 안 경로: 마이페이지 > 계정 관리 > 회원탈퇴 → 확인 2번 → Google 계정 재확인 (코드: `AccountManageScreen.kt`)
- 앱 없이 요청: 문의 메일 (안내 페이지 2절)
- 일부 데이터만 삭제 요청 받기: 따로 제공하지 않음 (신고·제보 내용 속 개인정보는 메일로 요청, 안내 페이지 3절 나)

## 4. 그 밖의 선언

| 항목 | 답변 초안 | 근거·비고 |
|------|----------|----------|
| 개인정보처리방침 URL | https://plant-82b09.web.app/privacy-policy.html | `LegalUrls.PRIVACY_POLICY` |
| 광고 포함 | 아니오 | 광고 SDK 없음 (코드: `app/build.gradle.kts`) |
| 광고 ID | 사용함, 용도 "분석" | #707 결정. `firebase-analytics`가 `AD_ID` 권한을 추가함 (이슈 본문 참고) |
| 앱 액세스 권한 | 로그인 필요. Google 로그인만 있으므로 심사용 Google 계정 또는 안내를 제공 (결정 필요) | 첫 로그인 때 만 14세 이상 확인·약관 동의·닉네임 설정이 필요함을 함께 적기 |
| 타겟층 | 결정 필요. 약관상 만 14세 이상만 가입 | `docs/legal/terms.md` 제4조 2항 |
| 콘텐츠 등급 설문 | 사용자 간 소통(게시글·댓글) 있음, 사용자 제작 콘텐츠 있음, 실제 돈으로 사는 상품 없음(골드는 앱 안 보상으로만 얻음) | 결제 SDK 없음 (코드) |
| 사용자 제작 콘텐츠 관리 | 게시글·댓글 신고, 신고 5건 이상이면 블라인드, 사용자 차단(차단한 사람의 글·댓글 숨김, 마이페이지 > 차단 목록에서 해제) | (코드) `Post.kt`의 `BLIND_REPORT_THRESHOLD = 5`, #660 |
| 정부 앱·금융 기능·건강 앱 | 해당 없음 | |

## 5. 비공개 테스트 트랙

- 테스터 목록(이메일·Google 그룹), 국가, 스토어 등록 정보(앱 이름·설명·스크린샷·아이콘)
- 개발자 계정 종류에 따른 프로덕션 신청 전 조건(테스터 수·기간)은 콘솔에서 확인 (미확인)
- 올릴 파일: #621 서명 AAB

## 6. 입력 전 확인 목록

- [ ] 계정 삭제 안내 페이지가 배포되어 로그인 없이 열린다
- [ ] 운영 Firestore에 `users/{uid}/blocks` 규칙이 배포되어 있다 (사용자 차단 설명 전에)
- [ ] 처리방침 수정본(#607)이 배포되어 있고, 데이터 보안 답변과 항목이 같다
- [ ] 포그라운드 서비스 영상 링크가 로그인 없이 열린다
