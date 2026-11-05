// users/{uid}, 하위 pots/logs 규칙 테스트
const {describe, it, before, after, beforeEach} = require("node:test");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {doc, getDoc, setDoc, updateDoc} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, seed} = require("../helpers");

describe("users", () => {
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
      await setDoc(doc(db, "users", ALICE), {nickname: "앨리스", gold: 100});
      await setDoc(doc(db, "users", ALICE, "pots", "pot1"), {potName: "화분"});
      await setDoc(doc(db, "users", ALICE, "pots", "pot1", "logs", "log1"), {studyingTime: 60});
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();
  const asGuest = () => testEnv.unauthenticatedContext().firestore();

  it("본인 문서는 읽고 쓸 수 있다", async () => {
    await assertSucceeds(getDoc(doc(asAlice(), "users", ALICE)));
    await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {nickname: "새이름"}));
  });

  it("다른 사람 문서는 읽을 수도 쓸 수도 없다", async () => {
    await assertFails(getDoc(doc(asBob(), "users", ALICE)));
    await assertFails(updateDoc(doc(asBob(), "users", ALICE), {nickname: "해킹"}));
  });

  it("로그인하지 않으면 읽을 수 없다", async () => {
    await assertFails(getDoc(doc(asGuest(), "users", ALICE)));
  });

  it("pots·logs는 본인만 읽고 쓸 수 있다", async () => {
    await assertSucceeds(getDoc(doc(asAlice(), "users", ALICE, "pots", "pot1")));
    await assertSucceeds(setDoc(doc(asAlice(), "users", ALICE, "pots", "pot1", "logs", "log2"), {studyingTime: 30}));
    await assertFails(getDoc(doc(asBob(), "users", ALICE, "pots", "pot1")));
    await assertFails(getDoc(doc(asBob(), "users", ALICE, "pots", "pot1", "logs", "log1")));
    await assertFails(setDoc(doc(asBob(), "users", ALICE, "pots", "pot1", "logs", "log2"), {studyingTime: 30}));
  });

  // 알려진 구멍: 본인 문서는 모든 필드를 자유롭게 쓸 수 있다 (#453~#456에서 검증 규칙 추가, #532에서 잠금)
  // 잠금 작업 후에는 이 테스트를 assertFails로 바꾼다
  it("[현재 허용 - #532에서 잠금 예정] 본인이 gold를 임의 값으로 바꿀 수 있다", async () => {
    await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {gold: 999999}));
  });
});
