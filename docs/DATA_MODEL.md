## 📐 데이터 모델

> **최초 작성일**: 2026-09-09
> **최종 수정일**: 2026-11-10

### users
```
users
└── {uid}
    ├── nickname: String
    ├── profileImg: String
    ├── lastSelectedPotId: String
    ├── isFirstLogin: Boolean?
    ├── totalStudyTime: Long             ← 학습 기록 저장 batch에서만 증가 (#453), 기록 삭제·화분 삭제 batch에서만 감소 (#455)
    ├── lastStudyEndedAt: Timestamp?     ← 마지막 저장 세션의 startedAt + 학습 시간 (세션 겹침 검증, #453)
    ├── lastStudyPotId: String?          ← 마지막 저장 세션의 화분 ID (#453)
    ├── lastStudySessionId: String?      ← 마지막 저장 세션 ID (#453)
    ├── gold: Int
    ├── completedPotsCount: Int          ← write-only. 화분 완료 batch에서 +1, 완료된 화분 삭제 batch에서 -1 (#455)
    ├── lastCompletedPotId: String?      ← 마지막으로 완료한 화분 ID (완료 수 +1 검증, #455)
    ├── lastDeletedPotId: String?        ← 마지막 기록 삭제·화분 삭제의 화분 ID (학습 시간 차감 검증, #455)
    ├── lastDeletedLogId: String?        ← 마지막 기록 삭제의 기록 ID, 화분 삭제면 "" (#455)
    ├── dailyCheckThisMonth
    │   ├── count: Int
    │   └── lastCheckedAt: Timestamp?
    ├── item
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
            ├── name: String
            ├── imageUrl: String
            ├── potTotalStudyingTime: Long    ← 학습 기록 저장 batch에서만 증가 (#453), 기록 삭제 batch에서만 감소 (#455)
            ├── createdAt: Timestamp?
            ├── completedAt: Timestamp?       ← 화분 완료 batch에서 서버 시각 (#455)
            ├── isCompleted: Boolean          ← 화분 완료 batch에서만 false → true (되돌릴 수 없음, #455)
            ├── lastStudiedAt: Timestamp?     ← 학습 기록 저장 시 서버 시각 (최근 학습순 정렬용)
            ├── lastStudySessionId: String?   ← 마지막 저장 세션 ID (#453)
            ├── lastDeletedLogId: String?     ← 마지막으로 삭제한 기록 ID (학습 시간 차감 검증, #455)
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
