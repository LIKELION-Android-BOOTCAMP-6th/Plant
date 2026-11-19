# 개인정보처리방침·이용약관 웹 페이지 관리 (#607)

개인정보처리방침과 이용약관은 **레포의 md 파일이 원본**이다. 웹 페이지(Firebase Hosting)는 배포할 때 md 파일로 HTML을 만들어 올린다.

## 구조

| 파일 | 역할 |
|------|------|
| `docs/legal/privacy.md` | 개인정보처리방침 원본 |
| `docs/legal/terms.md` | 서비스 이용약관 원본 |
| `hosting/build-legal.mjs` | md → HTML 변환 스크립트 (외부 패키지 없음) |
| `hosting/public/privacy-policy.html` | 변환 결과. 커밋하지 않음 (`.gitignore`) |
| `hosting/public/terms.html` | 변환 결과. 커밋하지 않음 (`.gitignore`) |
| `hosting/public/index.html`, `404.html` | 운영 사이트에 있던 Firebase 기본 페이지를 그대로 옮김 (아래 "기존 파일 보존") |
| `firebase.json`의 `hosting` | `hosting/public`을 배포하고, 배포 직전(`predeploy`)에 변환 스크립트를 실행 |

웹 주소와 앱 연결:

| 문서 | 주소 | 앱에서 여는 곳 |
|------|------|---------------|
| 개인정보처리방침 | https://plant-82b09.web.app/privacy-policy.html | `MyPageViewModel.PRIVACY_POLICY_URL` |
| 서비스 이용약관 | https://plant-82b09.web.app/terms.html | `MyPageViewModel.TERMS_OF_SERVICE_URL` |

파일 이름(주소)을 바꾸면 `hosting/build-legal.mjs`의 `PAGES`와 앱의 주소 상수를 함께 고친다.

## 문서 수정 절차

1. `docs/legal/*.md`를 고친다. 변환 스크립트가 처리하는 문법만 쓴다: 제목(`#`, `##`, `###`), 문단, 목록(`-`, `1.`), 표, `**굵게**`, `[링크](주소)`. 다른 문법을 쓰면 스크립트가 오류로 멈춘다
2. 내용을 바꾸면 맨 위 시행일을 바꾸고, 개인정보처리방침은 "11. 개인정보처리방침의 변경"의 개정 이력에 한 줄 추가한다. 시행일은 실제 배포하는 날로 맞춘다
3. 이용약관을 바꾸면 제3조에 따라 시행일 7일 전(이용자에게 불리한 변경은 30일 전)부터 알린다
4. 앱 동작이 바뀌는 PR(수집 항목, 탈퇴 처리, 커뮤니티 공개 범위 등)은 같은 PR에서 두 문서를 함께 고친다
5. 로컬에서 미리 보기: `node hosting/build-legal.mjs` 실행 후 `hosting/public/*.html`을 브라우저로 연다

## 배포

develop에 머지된 뒤 **메인 폴더**에서 실행한다 (워크트리에서 배포하지 않는다).

```powershell
firebase deploy --only hosting --project plant-82b09
```

- `predeploy`가 `node hosting/build-legal.mjs`를 먼저 실행하므로, 배포되는 HTML은 항상 그 시점의 md 파일과 같다
- 배포 후 확인: 브라우저에서 두 주소를 열어 시행일·내용 확인 → 앱 마이페이지의 "개인정보처리방침", "서비스 이용약관"을 눌러 같은 페이지가 열리는지 확인
- 되돌리기: Firebase 콘솔 > Hosting의 출시 기록에서 이전 버전으로 롤백할 수 있다

### 기존 파일 보존

Hosting 배포는 사이트 전체를 `hosting/public` 내용으로 **바꾼다**. 폴더에 없는 파일은 운영 사이트에서 사라진다.

- 2026-11-20에 운영 사이트에서 확인한 파일: `privacy-policy.html`, `index.html`(Firebase 기본 "Welcome" 페이지), `404.html`(Firebase 기본 "Page Not Found" 페이지). 이 중 `index.html`·`404.html`은 운영 파일을 그대로 받아 `hosting/public`에 넣었다
- 확인 방법: 알려진 파일 이름을 하나씩 요청해 상태 코드를 봤다 (`terms.html`, `favicon.ico`, `robots.txt` 등은 404). 운영 버전의 전체 파일 목록은 조회하지 못했다. **첫 배포 전에 처음 배포했던 폴더(`firebase init hosting`으로 만든 `public` 폴더)를 찾아 파일 목록을 비교**하고, 빠진 파일이 있으면 `hosting/public`에 추가한다
- 같은 사이트에 새 페이지(예: #632 계정 삭제 안내)를 올릴 때도 `hosting/public`에 넣어 한 번에 배포한다

## 주의

- `firebase deploy`를 `--only` 없이 실행하면 이제 Hosting도 함께 배포된다. 규칙·함수만 배포할 때는 지금처럼 `--only`를 붙인다
- `firebase emulators:start`를 `--only` 없이 실행하면 Hosting 에뮬레이터(기본 포트 5000)도 함께 켜진다. 테스트 스크립트(`functions`, `rules-test`)는 `--only`를 쓰므로 영향 없음
