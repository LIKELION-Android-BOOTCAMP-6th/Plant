# 플랜트 (Plant)

## 🌱 프로젝트 소개

> "학습 시간을 비료로, 나만의 성장을 꽃피우다"
>
> 학습 시간 측정과 식물 성장을 결합한 사용자 중심의 학습 관리 앱.

---

## 📖 서비스 배경

대부분의 학습 보조 앱은 개인의 기록에만 치중하여, 타인과의 소통을 통한 동기부여나 구체적인 학습 계획 공유에는 한계가 있습니다.

Plant는 학습 시간을 바탕으로 나만의 식물을 가꾸는 재미를 부여하고, 게시판을 통해 동료들과 학습 기록을 공유하며 꾸준히 공부할 수 있는 환경을 제공하고자 개발되었습니다.

---

## 🎯 대상 사용자

| 유형 | 설명 |
|------|------|
| 기록형 학습자 | 공부 시간을 측정하고, 내 정원에 학습 기록이 쌓여가는 과정을 시각적으로 확인하고 싶은 분 |
| 자극형 학습자 | 다른 유저들의 실시간 공부 현황과 학습 게시글을 보며 동기부여를 얻고 싶은 분 |
| 전략형 학습자 | 다른 유저들이 공유한 학습 계획과 노하우를 참고하여 나만의 학습 경로를 설계하고 싶은 분 |

---

## ✨ 핵심 기능
 
### 1. 학습 중 (Studying)
 
- **스톱워치**: 학습 시작 시 경과 시간을 측정하며, 일시 정지 및 재개 기능을 제공합니다.
- **실시간 학습 현황**: Firebase를 활용하여 현재 함께 공부 중인 유저들의 상태를 실시간으로 확인할 수 있습니다. 동료들의 존재를 통해 혼자 공부할 때보다 더 높은 소속감과 동기부여를 얻습니다.
 
### 2. 학습 기록 (Study Records)
 
- **화분 키우기**: 일정 시간 이상 학습을 완료하면 화분이 성장하고, 학습 성과가 내 정원에 식물로 쌓여갑니다.
- **개별 학습 기록 확인**: 날짜·과목별 학습 시간과 누적 기록을 시각적으로 확인하여 자신의 성장 과정을 한눈에 파악할 수 있습니다.
 
### 3. 공유 (Sharing & Community)
 
- **학습 기록 공유**: 자신의 학습 내용이나 사진을 게시글로 작성하여 꾸준한 공부 습관을 인증합니다.
- **다른 유저의 기록 탐색**: 다른 유저들이 공유한 학습 계획과 노하우를 참고하여 나만의 학습 경로를 설계할 수 있습니다.
- **커뮤니티 소통**: 게시글에 댓글·좋아요로 학습 정보를 나누고 자유롭게 소통합니다.

---

## 📁 프로젝트 문서

- **프로젝트**: [GitHub 링크]()
- **PPT**: [발표 자료]()

---

## 🖥️ 시연 영상

> 이미지 클릭 시 재생

[![시연 영상]()](유튜브 링크)

---

## 📆 개발 인원 및 기간

- **개발 기간**: 2026/03/12 ~ 2026/04/07
- **개발 인원**: Android 5명

---

## 🙌 팀원 소개

| 이름 | GitHub |
|------|--------|
| 장지은 | [zzingenius](https://github.com/zzingenius) |
| 손지희 | [starlightfjh](https://github.com/starlightfjh) |
| 김태환 | [taehwan-dev](https://github.com/taehwan-dev) |
| 김명준 | [jssmt247-crypto](https://github.com/jssmt247-crypto) |
| 송성호 | [sdfg7979-glitch](https://github.com/sdfg7979-glitch) |

---

## 📖 팀원 역할

| 코드 | 역할 | 담당 기능 |
|------|------|-----------|
| F1 | 인증·세션 | 회원가입, 로그인(이메일·Google), 로그아웃, 자동 로그인, 회원 탈퇴, 비밀번호 재설정 |
| F2 | 앱 골격·네비 | Bottom Navigation 구조 설계, Navigation Graph 구성, 화면 간 라우팅 처리 |
| F3 | 전체 UI 컨셉 | 앱 디자인 시스템 정의 (컬러·타이포·컴포넌트), Figma 화면 설계, 공통 UI 가이드라인 수립 |
| F4 | 홈 메인 | 홈 UI, 학습 현황 요약, 정원 표시, 식물 성장 로직 |
| F5 | 스터디 | 스톱워치(학습 시간 측정), 학습 기록 저장 |
| F6 | 커뮤니티 | 게시글 CRUD, 댓글·좋아요, 게시판 목록·상세 화면, 게시판 태그, 커뮤니티 활동 내역 |
| F7 | 마이페이지·설정 | 프로필 관리, 닉네임 설정, 다크모드 테마 설정 |
| F8 | QA | 기능별 테스트, 버그 리포트 및 추적, 릴리스 전 통합 테스트 수행 |

| 이름 | 담당 |
|------|------|
| 장지은 | F2, F3, F5, F6 |
| 김태환 | F1, F6, F8 |
| 손지희 | F3, F4, F6 |
| 김명준 | F7, F8 |
| 송성호 | F6 |

---

## 📄 협업 문서

- **기능 명세서**: [기능명세서](docs/FEATURE_SPEC.md)
- **화면 명세서**: [화면명세서](docs/SCREEN_SPEC.pdf)
- **ERD**: [ERD](docs/ERD.md)

---

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=Kotlin&logoColor=white"> |
| UI Framework | <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=Android&logoColor=white"> |
| Async | <img src="https://img.shields.io/badge/Coroutines%20%26%20Flow-7F52FF?style=flat-square&logo=Kotlin&logoColor=white"> |
| Architecture | <img src="https://img.shields.io/badge/MVVM-3DDC84?style=flat-square&logo=Android&logoColor=white"> <img src="https://img.shields.io/badge/Feature--based-3DDC84?style=flat-square&logo=Android&logoColor=white"> <img src="https://img.shields.io/badge/AppContainer%20%28Manual%20DI%29-3DDC84?style=flat-square&logo=Android&logoColor=white"> |
| Backend | <img src="https://img.shields.io/badge/Firebase%20Auth-FFCA28?style=flat-square&logo=Firebase&logoColor=white"> <img src="https://img.shields.io/badge/Cloud%20Firestore-FFCA28?style=flat-square&logo=Firebase&logoColor=white"> |
| Local Storage | <img src="https://img.shields.io/badge/Preferences%20DataStore-3DDC84?style=flat-square&logo=Android&logoColor=white"> |
| Design & Tools | <img src="https://img.shields.io/badge/Figma-F24E1E?style=flat-square&logo=Figma&logoColor=white"> <img src="https://img.shields.io/badge/Android%20Studio-3DDC84?style=flat-square&logo=android&logoColor=white"> <img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=GitHub&logoColor=white"> <img src="https://img.shields.io/badge/Notion-000000?style=flat-square&logo=Notion&logoColor=white"> |

---
 
## 🏗️ 앱 구조 / 아키텍처
 
Plant 프로젝트는 코드의 구조를 명확하게 나누고 유지보수를 쉽게 하기 위해 **MVVM(Model-View-ViewModel)** 아키텍처 패턴을 기반으로 설계되었습니다.
 
- **UI Layer**: Jetpack Compose를 사용해 구성하였으며, 화면에 필요한 데이터는 ViewModel에서 StateFlow를 통해 관리합니다.
- **ViewModel**: 앱의 상태를 관리하고 비즈니스 로직을 처리하는 역할을 수행하며, UI는 이 상태를 관찰하여 데이터가 변경될 때 자동으로 화면이 갱신되도록 구성하였습니다.
- **Repository**: Firebase(Firestore, Authentication)와 같은 외부 데이터 소스와의 통신을 담당합니다. 이를 통해 데이터 접근 로직을 분리하고 유지보수성을 향상시켰습니다.
- **AppContainer (수동 DI)**: 객체 간의 결합도를 낮추기 위해 AppContainer를 사용하여 필요한 객체들을 한 번만 생성하고 필요한 곳에서 사용하는 DI(Dependency Injection) 구조를 적용하여 각 계층에서 재사용할 수 있도록 설계하였습니다.
 
```
UI (Composable) → ViewModel → Repository → DataSource (Firebase)
```
 
## ⚙️ 환경 설정
 
- `google-services.json` 파일이 필요합니다.
- 로컬 키는 커밋하지 않고 별도 설정 파일로 관리합니다.
- Firestore 복합 인덱스가 필요한 쿼리가 있으며, 최초 실행 시 Logcat에 출력되는 링크를 통해 Firebase Console에서 인덱스를 등록해야 합니다.
 
---
 
## 🚀 실행 방법
 
- Android Studio Otter 이상 권장
- JDK 11
- Android SDK min 26 / target 36 / compile 36
- 프로젝트 오픈 후 Gradle Sync
- 에뮬레이터 또는 실기기에서 실행

