// users/{uid}/blocks 사용자 차단 규칙 테스트 (#660)
// 앱의 쓰기: BlockRemoteDataSourceImpl.blockUser(set) / unblockUser(delete)
const {describe, it, before, after, beforeEach} = require("node:test");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {
  collection, deleteDoc, doc, getDoc, getDocs, serverTimestamp, setDoc, Timestamp,
} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, seed} = require("../helpers");

describe("users/{uid}/blocks", () => {
  let testEnv;
  before(async () => {
    testEnv = await createTestEnv();
  });
  after(async () => {
    await testEnv.cleanup();
  });
  beforeEach(async () => {
    await testEnv.clearFirestore();
    // 앨리스가 이미 캐롤을 차단해 둔 상태
    // 규칙은 차단 대상의 users 문서가 있어야 차단을 허용한다 (#779) → 앨리스·밥·캐롤의 users 문서를 만들어 둔다
    // (앨리스 문서: 자기 자신 차단이 대상 문서가 없어서가 아니라 자기 자신이라서 거부되는지 보기 위함)
    await seed(testEnv, async (db) => {
      await setDoc(doc(db, "users", ALICE), {nickname: "앨리스"});
      await setDoc(doc(db, "users", BOB), {nickname: "밥"});
      await setDoc(doc(db, "users", "carol"), {nickname: "캐롤"});
      await setDoc(doc(db, "users", ALICE, "blocks", "carol"), {
        blockedUid: "carol", nickname: "캐롤", profileImg: "1", createdAt: Timestamp.now(),
      });
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();
  const asGuest = () => testEnv.unauthenticatedContext().firestore();

  // 앱과 같은 차단 문서 (blockedUid는 문서 ID와 같은 값, #779)
  const blockData = (overrides = {}) => ({
    blockedUid: BOB, nickname: "밥", profileImg: "2", createdAt: serverTimestamp(), ...overrides,
  });
  const block = (db, ownerUid, blockedUid, data = blockData({blockedUid})) =>
    setDoc(doc(db, "users", ownerUid, "blocks", blockedUid), data);

  describe("읽기", () => {
    it("본인의 차단 목록은 읽을 수 있다", async () => {
      await assertSucceeds(getDocs(collection(asAlice(), "users", ALICE, "blocks")));
      await assertSucceeds(getDoc(doc(asAlice(), "users", ALICE, "blocks", "carol")));
    });

    it("다른 사람·로그인하지 않은 사용자는 읽을 수 없다", async () => {
      await assertFails(getDocs(collection(asBob(), "users", ALICE, "blocks")));
      await assertFails(getDoc(doc(asBob(), "users", ALICE, "blocks", "carol")));
      await assertFails(getDocs(collection(asGuest(), "users", ALICE, "blocks")));
    });
  });

  describe("차단", () => {
    it("본인 목록에 다른 사용자를 차단할 수 있다", async () => {
      await assertSucceeds(block(asAlice(), ALICE, BOB));
    });

    it("이미 차단한 사용자를 다시 차단하면 같은 문서에 덮어쓴다", async () => {
      await assertSucceeds(block(asAlice(), ALICE, "carol", blockData({blockedUid: "carol", nickname: "캐롤2"})));
    });

    it("다른 사람의 목록에는 쓸 수 없다", async () => {
      await assertFails(block(asBob(), ALICE, BOB));
      await assertFails(block(asGuest(), ALICE, BOB));
    });

    it("자기 자신은 차단할 수 없다", async () => {
      await assertFails(block(asAlice(), ALICE, ALICE));
    });

    it("정해진 필드 외의 필드가 있거나 필드가 빠지면 거부", async () => {
      await assertFails(block(asAlice(), ALICE, BOB, blockData({uid: "dave"})));
      await assertFails(block(asAlice(), ALICE, BOB, {blockedUid: BOB, nickname: "밥", profileImg: "2"}));
      await assertFails(block(asAlice(), ALICE, BOB, {blockedUid: BOB, profileImg: "2", createdAt: serverTimestamp()}));
    });

    // 탈퇴할 때 서버가 이 값으로 남의 차단 목록에서 지운다 (#779)
    it("blockedUid가 빠지면 거부 (예전 앱 형식)", async () => {
      await assertFails(block(asAlice(), ALICE, BOB, {nickname: "밥", profileImg: "2", createdAt: serverTimestamp()}));
    });

    it("blockedUid가 문서 ID와 다르거나 문자열이 아니면 거부", async () => {
      await assertFails(block(asAlice(), ALICE, BOB, blockData({blockedUid: "carol"})));
      await assertFails(block(asAlice(), ALICE, BOB, blockData({blockedUid: ""})));
      await assertFails(block(asAlice(), ALICE, BOB, blockData({blockedUid: 1})));
      await assertFails(block(asAlice(), ALICE, BOB, blockData({blockedUid: null})));
    });

    it("차단 대상의 users 문서가 없으면(탈퇴함·없는 uid) 거부", async () => {
      await assertFails(block(asAlice(), ALICE, "dave"));
      // 이미 차단해 둔 상대가 탈퇴한 뒤 다시 쓰는 경우(덮어쓰기)도 거부
      await seed(testEnv, async (db) => {
        await deleteDoc(doc(db, "users", "carol"));
      });
      await assertFails(block(asAlice(), ALICE, "carol"));
    });

    it("createdAt이 서버 시각이 아니면 거부", async () => {
      await assertFails(block(asAlice(), ALICE, BOB, blockData({createdAt: Timestamp.fromMillis(0)})));
    });

    it("닉네임·프로필이 문자열이 아니거나 너무 길면 거부", async () => {
      await assertFails(block(asAlice(), ALICE, BOB, blockData({nickname: 1})));
      await assertFails(block(asAlice(), ALICE, BOB, blockData({profileImg: null})));
      await assertFails(block(asAlice(), ALICE, BOB, blockData({nickname: "가".repeat(51)})));
      await assertFails(block(asAlice(), ALICE, BOB, blockData({profileImg: "1".repeat(11)})));
    });

    it("예전 형식 값(빈 닉네임, lv.N 프로필)도 받는다", async () => {
      await assertSucceeds(block(asAlice(), ALICE, BOB, blockData({nickname: "", profileImg: "lv.3"})));
    });
  });

  describe("차단 해제", () => {
    it("본인 목록에서 해제할 수 있다", async () => {
      await assertSucceeds(deleteDoc(doc(asAlice(), "users", ALICE, "blocks", "carol")));
    });

    it("다른 사람의 목록에서는 해제할 수 없다", async () => {
      await assertFails(deleteDoc(doc(asBob(), "users", ALICE, "blocks", "carol")));
      await assertFails(deleteDoc(doc(asGuest(), "users", ALICE, "blocks", "carol")));
    });
  });
});
