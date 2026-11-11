## 📐 데이터 모델

> **최초 작성일**: 2026-09-09
> **최종 수정일**: 2026-11-12

### users
```
users
└── {uid}
    ├── nickname: String
    ├── profileImg: String               ← 고른 프로필 이미지 레벨 "0"~"6" (숫자 문자열). 가입 시 ""(선택 전)로만 생성. 바꿀 때만 규칙 검사: "0"~"6"이고 unlockedProfileLevel(없으면 0) 이하 (#602). 예전 형식 "lv.N"은 화면에서 읽기만 함. 게시글·댓글·공부 중 표시에 복사된 값은 해금 레벨을 검사하지 않음 (공부 중 표시는 문자열인지만, 게시글·댓글은 검사 없음)
    ├── unlockedProfileLevel: Int       ← 프로필 이미지 해금 최고 레벨 0~6. 가입 시 0, 필드가 없으면 0으로 읽음 (#475). 화분 레벨업 시 트리거(functions unlockProfileLevelOnPotLevelUp)가 min(max(기존 값, 새 레벨), 6)으로 갱신, 내려가지 않음 (#598). 앱은 수정 불가 (#532)
    ├── lastSelectedPotId: String
    ├── isFirstLogin: Boolean?
    ├── totalStudyTime: Long             ← 학습 기록 저장 batch에서만 증가 (#453), 기록 삭제·화분 삭제 batch에서만 감소 (#455)
    ├── lastStudyEndedAt: Timestamp?     ← 마지막 저장 세션의 startedAt + 학습 시간 (세션 겹침 검증, #453)
    ├── lastStudyPotId: String?          ← 마지막 저장 세션의 화분 ID (#453)
    ├── lastStudySessionId: String?      ← 마지막 저장 세션 ID (#453)
    ├── gold: Int                        ← 서버(보상·출석·박스 함수)만 씀, 앱은 가입 시 0으로 생성만 (#532)
    ├── completedPotsCount: Int          ← write-only. 화분 완료 batch에서 +1, 완료된 화분 삭제 batch에서 -1 (#455)
    ├── lastCompletedPotId: String?      ← 마지막으로 완료한 화분 ID (완료 수 +1 검증, #455)
    ├── lastDeletedPotId: String?        ← 마지막 기록 삭제·화분 삭제의 화분 ID (학습 시간 차감 검증, #455)
    ├── lastDeletedLogId: String?        ← 마지막 기록 삭제의 기록 ID, 화분 삭제면 "" (#455)
    ├── lastLevelUpPotId: String?        ← 마지막으로 레벨업한 화분 ID (아이템 차감 검증, #456)
    ├── dailyCheckThisMonth              ← 출석은 functions checkAttendance가 갱신, 앱은 가입 시 초기값 생성만 (#531), 규칙으로 앱 수정 거부 (#532)
    │   ├── count: Int                   ← 이번 달 출석 수 (0~28, 달이 바뀐 뒤 첫 출석에서 1로 다시 시작)
    │   ├── lastCheckedDate: String?     ← 마지막 출석일, 서버 시각 기준 KST "yyyy-MM-dd". "오늘 출석했나"·"같은 달인가"는 이 값으로 판단 (#531)
    │   └── lastCheckedAt: Timestamp?    ← 마지막 출석 시각 (기록용, 판단에는 쓰지 않음)
    ├── item                             ← 증가는 서버(보상·출석·박스 함수)만, heart~nutrient(성장 아이템) 감소는 화분 레벨업 batch에서만 (#456). 앱은 가입 시 0으로 생성, box는 수정 불가 (#532)
    │   ├── heart: Int
    │   ├── sun: Int
    │   ├── water: Int
    │   ├── fertilizer: Int
    │   ├── nutrient: Int
    │   └── box: Int
    │
    ├── boxOpenings                       ← 보너스 박스 열기 결과 (functions openBonusBoxes만 씀, 앱은 읽기·쓰기 불가, #454)
    │   └── {requestId}                   ← 앱이 보낸 요청 ID (같은 요청은 한 번만 추첨·지급)
    │       ├── count: Int                ← 연 박스 수
    │       ├── items: List<String>       ← 뽑힌 보상의 앱 ItemType 이름 (예: "HEART", "GOLD_300")
    │       └── createdAt: Timestamp
    │
    └── pots
        └── {potsId}
            ├── id: String
            ├── tag_id: String
            ├── tag_name: String
            ├── name: String                  ← 1~15자(UTF-16 단위), 앞뒤 공백·줄바꿈 없음 — 생성·이름 변경 시 규칙 검사 (#532). batch 검증 없이 앱이 바꿀 수 있는 화분 필드는 이것뿐
            ├── imageUrl: String
            ├── potTotalStudyingTime: Long    ← 학습 기록 저장 batch에서만 증가 (#453), 기록 삭제 batch에서만 감소 (#455)
            ├── createdAt: Timestamp?
            ├── completedAt: Timestamp?       ← 화분 완료 batch에서 서버 시각 (#455)
            ├── isCompleted: Boolean          ← 화분 완료 batch에서만 false → true (되돌릴 수 없음, #455)
            ├── lastStudiedAt: Timestamp?     ← 학습 기록 저장 시 서버 시각 (최근 학습순 정렬용)
            ├── lastStudySessionId: String?   ← 마지막 저장 세션 ID (#453)
            ├── lastDeletedLogId: String?     ← 마지막으로 삭제한 기록 ID (학습 시간 차감 검증, #455)
            ├── potLevel: Int                 ← 0으로 생성, 레벨업 batch에서만 +1 (최대 6, 조건 표는 LevelUpCondition.kt, #456)
            │
            └── logs                          ← 화분을 삭제하면 onPotDeleted 트리거가 함께 삭제 (#455)
                └── {logId}                   ← 학습 세션 ID (같은 세션은 한 번만 저장, #453)
                    ├── uid: String           ← #453부터 (쌓인 보상 collectionGroup 조회용)
                    ├── title: String
                    ├── contents: List<String>
                    ├── completedContents: List<Boolean>
                    ├── studyingTime: Long
                    ├── startedAt: Timestamp
                    ├── createAt: Timestamp
                    ├── rewardStatus: String  ← "pending" | "granted" | "none" — 앱은 "pending"으로만 생성 (#453)
                    ├── reward                ← 보상 트리거(functions grantStudyReward)만 씀
                    │   ├── items: List<String>   ← users.item 필드 키
                    │   └── box: Int
                    └── rewardSeen: Boolean   ← 앱이 기록에서 바꿀 수 있는 유일한 필드 (보상을 보여줬으면 true)
```
 
### activities
```
activities
└── {activityId}
    ├── uid: String
    ├── type: String              ← "게시물" | "댓글" | "좋아요" | "북마크"
    ├── title: String
    ├── comment: String?
    ├── commentId: String?
    ├── targetId: String
    └── createAt: Timestamp
```
 
### posts
```
posts
└── {postId}
    ├── author
    │   ├── id: String
    │   ├── nickname: String
    │   └── profileImg: String
    ├── title: String
    ├── content: String?
    ├── tag
    │   ├── id: String
    │   ├── name: String
    │   ├── parentId: String
    │   └── no: Int
    ├── commentCount: Int          ← 앱은 쓰지 않음, 댓글 생성·삭제 트리거(functions)가 댓글 수를 세어 저장 (#528)
    ├── likeCount: Int
    ├── likedBy: List<String>
    ├── bookmarkCount: Int
    ├── bookmarkedBy: List<String>
    ├── createdAt: Timestamp
    ├── activityId: String
    ├── isShared: Boolean?
    ├── studyLogs: List<StudyLog>?
    │
    └── comments
        └── {commentId}
            ├── user
            │   ├── uid: String
            │   ├── nickname: String
            │   └── profileImg: String
            ├── content: String
            ├── activityId: String
            └── createdAt: Timestamp
```
 
### studying
```
studying
└── {uid}
    ├── nickname: String
    ├── tag: String
    ├── studyingTime: Long
    ├── profileImg: String
    └── updatedAt: Timestamp       # 마지막 갱신 서버 시각 (#525)
```
- 앱이 타이머 시작·재개 시와 공부 중 1분마다 문서 전체를 다시 쓰고, 일시정지·종료하면 지운다 (학습 목표 입력 중처럼 타이머가 멈춘 동안에는 등록하지 않음)
- 예약 함수 `cleanUpStaleStudying`이 10분마다 `updatedAt`이 10분보다 오래된 문서(없는 문서 포함)를 지운다 (강제 종료 등으로 남은 문서 정리)
 
### Tags
```
Tags
└── {tagId}
    ├── id: String
    ├── name: String
    ├── parentId: String
    └── no: Int
```

### nicknames
```
nicknames
└── {nickname}
    └── nickname: String
```
