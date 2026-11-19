// 회원탈퇴 시 신고 익명화 (#606)
// index.js의 deleteUserData가 쓰고, 테스트가 에뮬레이터 Firestore에 직접 불러 경쟁 상황을 확인한다.
// 에뮬레이터는 같은 함수 요청을 하나씩 실행해서 Callable 호출로는 두 탈퇴 호출이 겹치는 상황을
// 만들 수 없기 때문에 따로 뺐다 → Firestore 인스턴스(db)는 인자로 받는다 (initializeApp 하지 않음)

// 신고·버그 제보·개선 제안은 지우지 않고 작성자를 비운다
const ANONYMOUS_REPORTER_ID = "";

// batch 하나는 쓰기 500건까지. 신고 1건을 옮길 때 create + delete 2건을 쓴다
const REPORT_MOVE_BATCH_SIZE = 250;

/**
 * 신고 문서를 자동 생성 ID의 새 문서로 옮기고 reporterId를 비운다.
 * - 문서 ID {uid}_{대상ID}에 uid가 들어 있어 필드만 비울 수 없으므로 새 ID로 옮긴다
 * - 사본 생성과 원본 삭제를 같은 batch에 넣는다 → 둘 중 하나만 반영되지 않아,
 *   재시도해도 사본이 두 개 생기지 않는다 (운영진이 기각할 때 reportCount가 두 번 줄지 않게)
 * - 원본 삭제는 조회한 시점의 문서일 때만 한다(lastUpdateTime 조건)
 *   → 그 사이 운영진이 status를 바꿨거나, 겹쳐 실행된 다른 탈퇴 호출이 먼저 옮겼으면
 *     그 batch가 통째로 실패한다. 다시 호출하면 남은 원본을 새로 조회해 이어서 옮긴다
 * - 묶음(batch)은 순서대로 반영되므로, 중간 묶음이 실패하면 앞 묶음까지만 옮겨진 상태로 끝난다
 * @param {FirebaseFirestore.Firestore} db
 * @param {FirebaseFirestore.QueryDocumentSnapshot[]} reportDocs
 * @return {Promise<void>}
 */
async function moveReportsAnonymously(db, reportDocs) {
  const reports = db.collection("reports");
  for (let i = 0; i < reportDocs.length; i += REPORT_MOVE_BATCH_SIZE) {
    const batch = db.batch();
    for (const doc of reportDocs.slice(i, i + REPORT_MOVE_BATCH_SIZE)) {
      batch.create(reports.doc(), {
        ...doc.data(),
        reporterId: ANONYMOUS_REPORTER_ID,
      });
      batch.delete(doc.ref, {lastUpdateTime: doc.updateTime});
    }
    await batch.commit();
  }
}

module.exports = {
  ANONYMOUS_REPORTER_ID,
  REPORT_MOVE_BATCH_SIZE,
  moveReportsAnonymously,
};
