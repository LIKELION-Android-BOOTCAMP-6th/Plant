// 탈퇴 시 남의 차단 목록에서 내 문서를 지우는 단계 통합 테스트 (#779)
// 에뮬레이터에서 실행된다: npm --prefix functions test
// deleteUserData를 에뮬레이터 Firestore로 직접 부른다 (Auth 계정 삭제는 이 단계와 무관)
// 차단 문서는 앱(BlockRemoteDataSourceImpl.blockUser)과 같은 모양으로 만든다
const {describe, it, before} = require("node:test");
const assert = require("node:assert");
const {Timestamp} = require("firebase-admin/firestore");
const {clearFirestore, db, uniqueId, waitFor} = require("../helpers/emulator");
const {deleteUserData} = require("../../accountDeletion");

const users = db.collection("users");

/**
 * owner가 blocked를 차단한 문서를 앱과 같은 모양으로 만든다.
 * @param {string} owner 차단한 사용자
 * @param {string} blocked 차단당한 사용자
 * @return {Promise<FirebaseFirestore.DocumentReference>}
 */
async function block(owner, blocked) {
  const ref = users.doc(owner).collection("blocks").doc(blocked);
  await ref.set({
    blockedUid: blocked,
    nickname: `nick-${blocked}`,
    profileImg: "1",
    createdAt: Timestamp.now(),
  });
  return ref;
}

/**
 * 가입을 마친 사용자 문서를 만든다.
 * @param {string} uid
 * @return {Promise<void>}
 */
async function createUser(uid) {
  await users.doc(uid).set({nickname: `nick-${uid}`, isFirstLogin: false});
}

/**
 * 문서가 남아 있는지 경로별로 돌려준다.
 * @param {FirebaseFirestore.DocumentReference[]} refs
 * @return {Promise<string[]>} 남아 있는 문서 경로
 */
async function remaining(refs) {
  const snapshots = await Promise.all(refs.map((ref) => ref.get()));
  return snapshots.filter((s) => s.exists).map((s) => s.ref.path);
}

describe("deleteUserData — 남의 차단 목록 속 나 (#779)", () => {
  before(async () => {
    await clearFirestore();
  });

  it("나를 차단한 사용자가 여러 명이면 모두의 목록에서 지운다", async () => {
    const me = uniqueId("me");
    const blockers = [uniqueId("a"), uniqueId("b"), uniqueId("c")];
    const third = uniqueId("third");
    await Promise.all([me, ...blockers, third].map(createUser));
    const blockedMe = await Promise.all(blockers.map((uid) => block(uid, me)));
    // 남끼리의 차단, 내가 남을 차단한 문서
    const othersBlock = await block(blockers[0], third);
    const myBlock = await block(me, third);

    await deleteUserData(db, me);

    assert.deepStrictEqual(await remaining([...blockedMe, myBlock]), []);
    // 남의 차단 목록에 있는 다른 사람·차단한 사람의 users 문서는 그대로
    assert.deepStrictEqual(await remaining([othersBlock]), [othersBlock.path]);
    assert.strictEqual((await users.doc(blockers[0]).get()).exists, true);
  });

  it("나를 차단한 사용자가 없어도 오류 없이 끝난다", async () => {
    const me = uniqueId("me");
    await createUser(me);

    await deleteUserData(db, me);

    assert.strictEqual((await users.doc(me).get()).exists, false);
  });

  it("users 문서를 지운 뒤 실패했어도 다시 부르면 남은 차단 문서를 지운다", async () => {
    // 11단계(users/{uid} 삭제)까지 끝나고 12단계 전에 실패한 상태
    const me = uniqueId("me");
    const blocker = uniqueId("a");
    await createUser(blocker);
    const blockedMe = await block(blocker, me);

    await deleteUserData(db, me);
    assert.deepStrictEqual(await remaining([blockedMe]), []);

    // 이미 모두 지운 뒤 한 번 더 불러도 오류 없음
    await deleteUserData(db, me);
  });

  it("상대가 차단 목록을 구독 중이면 오류 없이 항목이 사라진다", async () => {
    const me = uniqueId("me");
    const blocker = uniqueId("a");
    const third = uniqueId("third");
    await Promise.all([me, blocker, third].map(createUser));
    await block(blocker, me);
    await block(blocker, third);

    // 앱의 observeBlockedUsers와 같이 users/{blocker}/blocks 전체를 구독
    const snapshots = [];
    const errors = [];
    const unsubscribe = users.doc(blocker).collection("blocks").onSnapshot(
        (snapshot) => snapshots.push(snapshot.docs.map((doc) => doc.id)),
        (error) => errors.push(error),
    );
    try {
      await waitFor(async () => snapshots.length > 0, {message: "첫 스냅샷"});

      await deleteUserData(db, me);

      await waitFor(
          async () => {
            const last = snapshots[snapshots.length - 1];
            return last.length === 1 && last[0] === third;
          },
          {message: "삭제 반영", describe: () => JSON.stringify(snapshots)},
      );
      assert.deepStrictEqual(errors, []);
    } finally {
      unsubscribe();
    }
  });

  it("[알려진 한계] blockedUid가 없는 예전 형식 문서는 지우지 못한다", async () => {
    // 비공개 테스트 전 데이터 초기화로 예전 문서가 없어지므로 이행하지 않는다 (#779)
    const me = uniqueId("me");
    const blocker = uniqueId("a");
    await Promise.all([me, blocker].map(createUser));
    const oldFormat = users.doc(blocker).collection("blocks").doc(me);
    await oldFormat.set({
      nickname: `nick-${me}`, profileImg: "1", createdAt: Timestamp.now(),
    });

    await deleteUserData(db, me);

    assert.deepStrictEqual(await remaining([oldFormat]), [oldFormat.path]);
  });
});
