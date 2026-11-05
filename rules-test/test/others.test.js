// nicknames, feedbacks, studying, Tags 규칙 테스트
const {describe, it, before, after, beforeEach} = require("node:test");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {deleteDoc, doc, getDoc, serverTimestamp, setDoc} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, seed} = require("../helpers");

describe("기타 컬렉션", () => {
  let testEnv;
  before(async () => {
    testEnv = await createTestEnv();
  });
  after(async () => {
    await testEnv.cleanup();
  });
  beforeEach(async () => {
    await testEnv.clearFirestore();
    await seed(testEnv, async (db) => {
      await setDoc(doc(db, "nicknames", "앨리스"), {nickname: "앨리스", uid: ALICE});
      await setDoc(doc(db, "Tags", "tag1"), {name: "공부"});
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();
  const asGuest = () => testEnv.unauthenticatedContext().firestore();

  describe("nicknames", () => {
    it("빈 닉네임은 본인 uid로 선점할 수 있다", async () => {
      await assertSucceeds(setDoc(doc(asBob(), "nicknames", "밥"), {nickname: "밥", uid: BOB}));
    });

    it("이미 있는 닉네임은 덮어쓸 수 없다", async () => {
      await assertFails(setDoc(doc(asBob(), "nicknames", "앨리스"), {nickname: "앨리스", uid: BOB}));
    });

    it("다른 사람 uid로 선점하거나 문서 ID와 닉네임이 다르면 거부", async () => {
      await assertFails(setDoc(doc(asBob(), "nicknames", "밥"), {nickname: "밥", uid: ALICE}));
      await assertFails(setDoc(doc(asBob(), "nicknames", "밥"), {nickname: "다른이름", uid: BOB}));
    });

    it("소유자만 삭제할 수 있다", async () => {
      await assertFails(deleteDoc(doc(asBob(), "nicknames", "앨리스")));
      await assertSucceeds(deleteDoc(doc(asAlice(), "nicknames", "앨리스")));
    });
  });

  describe("feedbacks", () => {
    const feedback = (reporterId) => ({
      type: "bug", reporterId, category: "ui_bug", description: "버튼이 안 눌려요",
      appVersion: "1.0.0", createdAt: serverTimestamp(), status: "pending",
    });

    it("본인 명의 제보는 만들 수 있다", async () => {
      await assertSucceeds(setDoc(doc(asAlice(), "feedbacks", "f1"), feedback(ALICE)));
    });

    it("다른 사람 명의 제보는 거부", async () => {
      await assertFails(setDoc(doc(asAlice(), "feedbacks", "f1"), feedback(BOB)));
    });

    it("제보는 아무도 읽을 수 없다 (운영진은 콘솔에서 확인)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "feedbacks", "f1"), {...feedback(ALICE), createdAt: new Date()});
      });
      await assertFails(getDoc(doc(asAlice(), "feedbacks", "f1")));
    });
  });

  describe("studying", () => {
    it("본인 문서만 쓸 수 있고, 로그인하면 누구나 읽을 수 있다", async () => {
      await assertSucceeds(setDoc(doc(asAlice(), "studying", ALICE), {studyingTime: 0}));
      await assertFails(setDoc(doc(asBob(), "studying", ALICE), {studyingTime: 0}));
      await assertSucceeds(getDoc(doc(asBob(), "studying", ALICE)));
    });
  });

  describe("Tags", () => {
    it("로그인하면 읽기만 가능하고 쓰기는 불가", async () => {
      await assertSucceeds(getDoc(doc(asAlice(), "Tags", "tag1")));
      await assertFails(setDoc(doc(asAlice(), "Tags", "tag2"), {name: "새 태그"}));
      await assertFails(getDoc(doc(asGuest(), "Tags", "tag1")));
    });
  });
});
