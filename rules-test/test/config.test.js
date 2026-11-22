// config/app 규칙 테스트 (#662) — 최소 지원 버전 설정
// 스플래시가 로그인 확인 전에 읽으므로 비로그인도 읽을 수 있어야 하고, 앱에서는 아무도 쓸 수 없어야 한다
const {describe, it, before, after, beforeEach} = require("node:test");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {deleteDoc, doc, getDoc, setDoc, updateDoc} = require("firebase/firestore");
const {ALICE, createTestEnv, seed} = require("../helpers");

describe("config", () => {
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
      await setDoc(doc(db, "config", "app"), {minVersionCode: 1, message: "업데이트해 주세요"});
      await setDoc(doc(db, "config", "other"), {secret: "x"});
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asGuest = () => testEnv.unauthenticatedContext().firestore();

  it("로그인하지 않아도 config/app을 읽을 수 있다", async () => {
    await assertSucceeds(getDoc(doc(asGuest(), "config", "app")));
  });

  it("로그인한 사용자도 config/app을 읽을 수 있다", async () => {
    await assertSucceeds(getDoc(doc(asAlice(), "config", "app")));
  });

  it("config/app이 없어도 읽기는 허용 (문서 없음 = 통과 처리)", async () => {
    await seed(testEnv, async (db) => {
      await deleteDoc(doc(db, "config", "app"));
    });
    await assertSucceeds(getDoc(doc(asGuest(), "config", "app")));
  });

  it("config/app 외 다른 config 문서는 읽을 수 없다", async () => {
    await assertFails(getDoc(doc(asGuest(), "config", "other")));
    await assertFails(getDoc(doc(asAlice(), "config", "other")));
  });

  it("config/app은 로그인 여부와 관계없이 만들기·고치기·지우기가 거부된다", async () => {
    for (const db of [asGuest(), asAlice()]) {
      const ref = doc(db, "config", "app");
      await assertFails(setDoc(ref, {minVersionCode: 0, message: ""}));
      await assertFails(updateDoc(ref, {minVersionCode: 0}));
      await assertFails(deleteDoc(ref));
    }
  });

  it("config/app이 없을 때 새로 만드는 것도 거부된다", async () => {
    await seed(testEnv, async (db) => {
      await deleteDoc(doc(db, "config", "app"));
    });
    await assertFails(setDoc(doc(asAlice(), "config", "app"), {minVersionCode: 999}));
    await assertFails(setDoc(doc(asGuest(), "config", "app"), {minVersionCode: 999}));
  });
});
