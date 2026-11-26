# 개인정보처리방침·이용약관·계정 삭제 안내 웹 페이지 관리 (#607, #632)

개인정보처리방침, 이용약관, 계정 삭제 안내는 **레포의 md 파일이 원본**이다. 웹 페이지(Firebase Hosting)는 배포할 때 md 파일로 HTML을 만들어 올린다.

## 구조

| 파일 | 역할 |
|------|------|
| `docs/legal/privacy.md` | 개인정보처리방침 원본 |
| `docs/legal/terms.md` | 서비스 이용약관 원본 |
| `docs/legal/account-deletion.md` | 계정 삭제 안내 원본 (#632, Play Console 계정 삭제 URL용) |
| `hosting/build-legal.mjs` | md → HTML 변환 스크립트 (외부 패키지 없음) |
| `hosting/public/privacy-policy.html` | 변환 결과. 커밋하지 않음 (`.gitignore`) |
| `hosting/public/terms.html` | 변환 결과. 커밋하지 않음 (`.gitignore`) |
| `hosting/public/account-deletion.html` | 변환 결과. 커밋하지 않음 (`.gitignore`) |
| `hosting/public/index.html`, `404.html` | 운영 사이트에 있던 Firebase 기본 페이지를 그대로 옮김 (아래 "기존 파일 보존") |
| `firebase.json`의 `hosting` | `hosting/public`을 배포하고, 배포 직전(`predeploy`)에 변환 스크립트를 실행 |

웹 주소와 앱 연결:

| 문서 | 주소 | 앱의 주소 상수 |
|------|------|---------------|
| 개인정보처리방침 | https://plant-82b09.web.app/privacy-policy.html | `LegalUrls.PRIVACY_POLICY` |
| 서비스 이용약관 | https://plant-82b09.web.app/terms.html | `LegalUrls.TERMS_OF_SERVICE` |
| 계정 삭제 안내 | https://plant-82b09.web.app/account-deletion.html | 없음 (앱에서 직접 열지 않음. 처리방침 4절·6절에서 링크) |

앱에서 여는 곳: 마이페이지(`MyPageViewModel`), 첫 로그인 닉네임 설정 다이얼로그의 "보기"(`SignInViewModel`, #661). 두 곳 모두 `core/util/LegalUrls.kt`의 상수를 쓴다.

계정 삭제 안내는 Play Console > 앱 콘텐츠 > 데이터 보안의 "계정 삭제 URL"에 등록하는 페이지다 (#632). 탈퇴 경로 문구·순서(`BottomBar.kt` "마이페이지", `MypageScreen.kt` "계정 관리", `AccountManageScreen.kt` 회원탈퇴·확인 창 문구·버튼, `DeleteAccountViewModel.kt` 완료 안내)나 탈퇴 처리(`functions/accountDeletion.js`의 `deleteUserData`), 처리방침 3절을 바꾸면 이 페이지도 함께 고친다. Play Console 답변 초안은 `docs/PLAY_CONSOLE.md`에 있다.

파일 이름(주소)을 바꾸거나 페이지를 추가하면 `hosting/build-legal.mjs`의 `PAGES`, 앱의 주소 상수, `.gitignore`(변환 결과 HTML)를 함께 고친다. `.gitignore`에 넣지 않으면 변환 결과가 커밋되어, 커밋된 HTML을 손으로 고쳐도 배포 때 md 내용으로 덮어써진다. 계정 삭제 안내 주소를 바꾸면 처리방침 4절·6절의 링크, 이 문서의 주소 표, `docs/PLAY_CONSOLE.md` 3절의 URL, Play Console에 등록한 URL, `docs/ACCOUNT_DELETION_REQUEST.md` 5절 회신 문구의 URL도 함께 바꾼다. 계정 삭제 안내 2절(메일 요청 방법·본인 확인)이나 처리방침 4절·6절의 본인 확인 문장을 바꾸면 `docs/ACCOUNT_DELETION_REQUEST.md` 3절(본인 확인)·5절(회신 문구)도 함께 고친다. 처리방침 주소를 바꾸면 계정 삭제 안내의 처리방침 링크(3절), 이 문서의 주소 표, `docs/PLAY_CONSOLE.md` 4절의 처리방침 URL, Play Console에 등록한 처리방침 URL, `LegalUrls` 주소를 확인하는 테스트(`MyPageViewModelTest` 등), Google 인증 플랫폼 브랜딩(OAuth 동의 화면)의 개인정보처리방침 링크와 그 값을 적은 [API_KEY_OAUTH.md](API_KEY_OAUTH.md#oauth-동의-화면-google-인증-플랫폼) 브랜딩 표도 함께 바꾼다. 이용약관 주소를 바꾸면 위의 파일 이름 변경 항목에 더해 이 문서의 주소 표, `MyPageViewModelTest`의 약관 주소, Google 인증 플랫폼 브랜딩의 서비스 약관 링크와 같은 브랜딩 표를 바꾼다. 호스팅 주소(`plant-82b09.web.app`)를 바꾸면 위 처리방침·이용약관·계정 삭제 안내 주소 변경 항목에 더해 Google 인증 플랫폼 브랜딩의 홈페이지·승인된 도메인과 [API_KEY_OAUTH.md](API_KEY_OAUTH.md#oauth-동의-화면-google-인증-플랫폼) 브랜딩 표의 같은 행도 바꾼다. 처리방침 12절 연락처를 바꾸면 계정 삭제 안내 4절 문의처와 이용약관 제16조 문의처도 함께 바꾼다.

## 문서 수정 절차

1. `docs/legal/*.md`를 고친다. 변환 스크립트가 처리하는 문법만 쓴다: 제목(`#`, `##`, `###`), 문단, 목록(`-`, `1.`), 표, `**굵게**`, `[링크](주소)`. 다른 문법(들여쓴 하위 목록, `>` 인용, `` ` `` 코드 등)을 쓰면 스크립트가 오류로 멈춘다
   - 문법이 아니어도 멈추는 경우: 본문 글자에 `_`, `[`, `]`가 있을 때(링크 문법 제외), 문단을 `**굵게**`로 시작할 때. 문장을 바꿔 쓴다 (목록 항목과 표 칸은 `**`로 시작해도 된다)
2. 내용을 바꾸면 맨 위 시행일을 바꾸고, 개인정보처리방침은 "11. 개인정보처리방침의 변경"의 개정 이력에 한 줄 추가한다
   - 시행일에는 **배포 예정일**을 적고, 그날 머지하고 바로 배포한다 (처리방침은 "게시 즉시 효력"이라 시행일과 배포일이 같아야 한다)
   - 머지·배포가 예정일보다 늦어지면, 배포 전에 시행일을 고치는 PR을 먼저 올린다. 바꿀 곳: `privacy.md` 맨 위 시행일과 개정 이력 날짜, `terms.md` 맨 위 시행일과 부칙, `account-deletion.md` 맨 위 시행일 (바꾼 문서만)
3. 이용약관을 바꾸면 제3조에 따라 시행일 7일 전(이용자에게 불리한 변경은 30일 전)부터 알린다
4. 앱 동작이 바뀌는 PR(수집 항목, 탈퇴 처리, 커뮤니티 공개 범위, 신고 사유·블라인드 기준, 아이템 이름, 데이터를 수집하는 SDK 추가·설정 변경(예: Analytics 광고 ID, Crashlytics) 등)은 같은 PR에서 처리방침·이용약관을 함께 고친다. 앱 탈퇴 화면 문구·순서, 탈퇴 처리, 처리방침 3절(수집 항목이 바뀌어 3절 가 목록이 바뀌는 경우와 보관 기간 포함)이 바뀌면 계정 삭제 안내(`account-deletion.md`)도 함께 고친다. Play Console 데이터 보안 양식·광고 ID 선언(답변 초안 `docs/PLAY_CONSOLE.md`)도 같이 확인한다. Google Analytics 관리 > 데이터 보관 값(현재 이벤트 2개월, 사용자 14개월·새 활동 시 재설정)을 바꾸면 처리방침 3절 다와 계정 삭제 안내 3절 다도 고친다. Firestore 백업·PITR 보관 기간(현재 7일, `docs/FIRESTORE_BACKUP.md`)을 늘리거나 매주 백업 일정을 추가할 때도 처리방침 3절 다와 계정 삭제 안내 3절 다("최대 7일"), `docs/ACCOUNT_DELETION_REQUEST.md` 5절 "삭제 완료" 회신 문구를 고친다
5. 로컬에서 미리 보기: `node hosting/build-legal.mjs` 실행 후 `hosting/public/*.html`을 브라우저로 연다

## 배포

develop에 머지된 뒤 **메인 폴더**에서 실행한다 (워크트리에서 배포하지 않는다).

배포 전 확인: `predeploy`는 그 순간 폴더에 있는 md 파일로 HTML을 만들므로, 커밋하지 않은 수정이 있으면 레포와 다른 내용이 올라간다.

```powershell
git switch develop
```

```powershell
git pull
```

```powershell
git status --short docs/legal hosting
```

- 마지막 명령의 출력이 비어 있어야 한다. 무언가 나오면 배포하지 않고 먼저 정리한다

배포:

```powershell
firebase deploy --only hosting --project plant-82b09
```

- `predeploy`가 `node hosting/build-legal.mjs`를 먼저 실행하므로, 배포되는 HTML은 그 시점 폴더의 md 파일과 같다 (위 확인을 거쳤다면 develop의 md 파일과 같다)
- 배포 후 확인: 브라우저에서 위 표의 세 주소를 열어 시행일·내용 확인 (계정 삭제 안내는 로그인하지 않은 브라우저에서도 열리는지 확인) → 앱 마이페이지의 "개인정보처리방침", "서비스 이용약관"을 눌러 같은 페이지가 열리는지 확인
  - 운영 응답 헤더가 `Cache-Control: max-age=3600`이라, 전에 연 적이 있는 브라우저는 배포 후 최대 1시간 동안 이전 페이지를 보여 줄 수 있다. 이전 내용이 보이면 새로고침한다
- 되돌리기: Firebase 콘솔 > Hosting의 출시 기록에서 이전 버전으로 롤백할 수 있다

### 기존 파일 보존

Hosting 배포는 사이트 전체를 `hosting/public` 내용으로 **바꾼다**. 폴더에 없는 파일은 운영 사이트에서 사라진다.

- 2026-11-20에 운영 사이트에서 확인한 파일: `privacy-policy.html`, `index.html`(Firebase 기본 "Welcome" 페이지), `404.html`(Firebase 기본 "Page Not Found" 페이지). 이 중 `index.html`·`404.html`은 운영 파일을 그대로 받아 `hosting/public`에 넣었다
- 확인 방법: 알려진 파일 이름을 하나씩 요청해 상태 코드를 봤다 (`terms.html`, `favicon.ico`, `robots.txt` 등은 404). 운영 버전의 전체 파일 목록은 조회하지 못했다. **첫 배포 전에 처음 배포했던 폴더(`firebase init hosting`으로 만든 `public` 폴더)를 찾아 파일 목록을 비교**하고, 빠진 파일이 있으면 `hosting/public`에 추가한다
- 같은 사이트에 새 페이지(예: #632 계정 삭제 안내)를 올릴 때도 `hosting/public`에 넣어 한 번에 배포한다

## 주의

- `firebase deploy`를 `--only` 없이 실행하면 이제 Hosting도 함께 배포된다. 규칙·함수만 배포할 때는 지금처럼 `--only`를 붙인다
- `firebase emulators:start`를 `--only` 없이 실행하면 Hosting 에뮬레이터(기본 포트 5000)도 함께 켜진다. 테스트 스크립트(`functions`, `rules-test`)는 `--only`를 쓰므로 영향 없음
