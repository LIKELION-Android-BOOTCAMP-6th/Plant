## 🐛 [화분 도메인] PotDto `@DocumentId` 충돌로 앱 실행 불가

> 작성일: 2026-07-21 / 작성자: 김태환 (인증 담당)
> 발견 경위: 인증 CA 전환 작업 후 실기 테스트 중 발견. **인증 도메인과 무관한 별개 버그**라 수정하지 않고 인계합니다.

---

### 📌 한 줄 요약

`PotDto.id`의 `@DocumentId` 어노테이션과 Firestore 문서에 실제 저장된 `id` 필드가 충돌해서, **화분이 1개라도 있는 계정은 앱이 즉시 강제 종료됩니다.**

---

### 증상

- 앱 아이콘을 눌러도 켜지지 않음 (스플래시 잠깐 뜨고 바로 종료)
- 로그인·자동로그인까지는 정상 통과하고, **홈 화면이 화분 목록을 읽는 순간** 크래시
- `develop` 브랜치에 이미 존재하는 버그 (특정 작업 브랜치의 문제가 아님)

### 재현 조건

**화분 데이터가 1개 이상 있는 계정으로 로그인하면 100% 재현됩니다.**

반대로 화분이 없는 신규 계정에서는 재현되지 않습니다. 그래서 지금까지 눈에 안 띄었을 가능성이 높습니다.
(실제로 에뮬레이터 신규 계정에서는 안 나다가, 화분이 생긴 뒤부터 나타났습니다.)

### 크래시 로그

```
FATAL EXCEPTION: main
java.lang.RuntimeException: 'id' was found from document
  users/{uid}/pots/{potId}, cannot apply @DocumentId on this property
  for class com.a32b.plant.data.model.PotDto

  at CustomClassMapper$BeanMapper.populateDocumentIdProperties(CustomClassMapper.java:868)
  at com.google.firebase.firestore.QuerySnapshot.toObjects(QuerySnapshot.java:178)
  at com.a32b.plant.data.datasource.pot.PotRemoteDataSourceImpl$getPots$1
      .invokeSuspend$lambda$0(PotRemoteDataSourceImpl.kt:35)
```

갤럭시 노트20 울트라(SM-N986N) 실기기 / 에뮬레이터(Pixel_3a) 양쪽 모두에서 동일하게 확인했습니다.

---

### 원인

Firestore의 `@DocumentId`는 **"이 필드는 문서 데이터가 아니라 문서 ID(경로)에서 채워라"**는 의미입니다.
규칙상 이 필드는 **문서 데이터 안에 실제로 존재하면 안 되고**, 존재하면 SDK가 어느 쪽을 써야 할지 몰라 예외를 던집니다.

그런데 현재 코드는 `id`를 문서 바디에도 함께 저장하고 있습니다.

**`data/model/PotDto.kt`**
```kotlin
data class PotDto(
    @DocumentId
    var id: String = "",   // ← 문서 데이터에 있으면 안 되는 필드
    ...
)
```

**`data/datasource/pot/PotRemoteDataSourceImpl.kt` (addPot)**
```kotlin
val newDocRef = db.collection("users").document(uid).collection("pots").document()
val newPotDto = PotDto(
    id = newDocRef.id,   // ← 이 값이 문서 바디에 같이 write 됨
    ...
)
```

**`docs/ERD.md`에도 `id`가 정식 필드로 설계되어 있습니다:**
```
pots
└── {potsId}
    ├── id: String?      ← 여기
    ├── tag_id: String?
```

즉 **설계상으로는 `id`를 문서 필드로 저장하기로 되어 있는데, DTO에는 `@DocumentId`가 붙어 있어** 둘이 모순된 상태입니다. 이미 저장된 기존 문서들도 전부 `id` 필드를 갖고 있습니다.

> 참고: 같은 로그에서 `No setter/field for level found on class PotDto` 경고도 나옵니다.
> 실제 문서에는 `level` 필드가 있는데 `PotDto`에는 없습니다. 크래시 원인은 아니지만 함께 점검하면 좋겠습니다.
> (`UserDto` 쪽도 `completedPotsCount`, `currentPot`, `potList` 필드가 동일하게 누락 경고가 뜹니다.)

---

### 수정 방향 (택 1)

#### ✅ 방안 A — `@DocumentId` 제거 + 문서 ID를 명시적으로 주입 (추천)

기존 데이터·신규 데이터 **양쪽 모두 안전**하고, 데이터 마이그레이션이 필요 없습니다.
이미 `OldUserRepository`에서 쓰고 있는 패턴과 동일합니다.

```kotlin
// PotDto.kt — @DocumentId 어노테이션만 제거 (필드는 유지)
data class PotDto(
    var id: String = "",
    ...
)

// PotRemoteDataSourceImpl.kt getPots()
val pots = snapshot?.documents?.mapNotNull { doc ->
    doc.toObject(PotDto::class.java)?.copy(id = doc.id)
} ?: emptyList()
```

`toObjects()` → `documents.mapNotNull { }` 로 바꿔서 문서 ID를 직접 채워주는 방식입니다.
`getPots` 외에 `PotDto`를 역직렬화하는 다른 지점이 있다면 동일하게 적용이 필요합니다.

#### 방안 B — `@DocumentId` 유지 + 저장 시 `id` 미기록 + 기존 데이터 정리

`@DocumentId`의 원래 의도를 살리는 방향이지만,
**이미 저장된 모든 화분 문서에서 `id` 필드를 제거하는 마이그레이션이 필요**합니다.
운영 데이터가 있다면 부담이 크고, ERD 문서도 함께 수정해야 합니다.

---

### 확인 부탁드릴 것

1. **ERD 설계 의도** — `pots.{potsId}.id`를 문서 필드로 유지할 것인지, 문서 ID만 쓸 것인지 결정 필요 (방안 A/B 선택의 기준)
2. **`PotDto`에 `level` 필드 누락** — 실제 문서에는 존재. 의도된 것인지 확인 필요
3. **`UserDto` 누락 필드** — `completedPotsCount`, `currentPot`, `potList`. `potList`는 매퍼에서 별도 주입하는 구조라 의도된 것으로 보이나 나머지는 확인 필요
4. `PotDto`를 역직렬화하는 **다른 호출 지점**이 더 있는지 (있다면 같은 수정 필요)

---

### 왜 인증 PR에 포함하지 않았나

- `PotDto.kt` / `PotRemoteDataSourceImpl.kt`는 인증 작업에서 **한 줄도 건드리지 않은 파일**입니다
- `origin/develop`에도 동일한 코드가 있어 **기존부터 존재하던 버그**임을 확인했습니다
- 화분 도메인의 데이터 모델 설계 결정(위 1번)이 필요한 사안이라 임의로 수정하지 않았습니다

인증 PR 리뷰 시 이 크래시 때문에 앱 실행이 안 될 수 있으니, **화분 없는 신규 계정으로 테스트**하시거나 이 이슈를 먼저 처리해주시면 좋겠습니다.
