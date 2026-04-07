## 📐 ERD
 
### users
```
users
└── {uid}
    ├── nickname: String?
    ├── profileImg: String?
    ├── lastSelectedPotId: String?
    ├── isFirstLogin: Boolean?
    ├── isDarkMode: Boolean?
    ├── totalStudyTime: Long?
    ├── completedPotsCount: Int?
    │
    └── pots
        └── {potsId}
            ├── id: String?
            ├── tag_id: String?
            ├── tag_name: String?
            ├── name: String?
            ├── imageUrl: String?
            ├── potTotalStudyingTime: Long?
            ├── createdAt: Timestamp
            ├── completedAt: Timestamp?
            ├── isCompleted: Boolean
            │
            └── logs
                └── {logId}
                    ├── id: String
                    ├── title: String
                    ├── contents: List<String>
                    ├── studyingTime: Long
                    └── createAt: Timestamp
```
 
### activities
```
activities
└── {activityId}
    ├── uid: String
    ├── type: String
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
    ├── commentCount: Int
    ├── likeCount: Int
    ├── likedBy: List<String>
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
└── {studyingId}
    ├── uid: String
    ├── nickname: String
    ├── tag: String
    ├── studyingTime: Long
    └── profileImg: String
```
 
### nicknames
```
nicknames
└── {nickname}
    └── nickname: String
```
 
