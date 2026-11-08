// users/{uid}, 하위 pots/logs 규칙 테스트
const {describe, it, before, after, beforeEach} = require("node:test");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {deleteDoc, deleteField, doc, getDoc, increment, runTransaction, setDoc, updateDoc} = require("firebase/firestore");
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
    await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {profileImg: "3"}));
  });

  // 닉네임 변경은 nicknames 문서 소유 확인 (#535)
  describe("nickname", () => {
    it("닉네임 외 필드 쓰기는 검사하지 않는다 (출석, 아이템, 세션 토큰, 화분 선택 등)", async () => {
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {
        "activeSessionToken": "token",
        "lastSelectedPotId": "pot1",
        "item.water": increment(1),
        "dailyCheckThisMonth.count": increment(1),
        "totalStudyTime": increment(60),
      }));
    });

    it("같은 닉네임을 그대로 다시 쓰는 것은 변경이 아니므로 허용", async () => {
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {nickname: "앨리스", profileImg: "2"}));
    });

    it("이미 본인 소유인 닉네임 문서가 있으면 그 닉네임으로 바꿀 수 있다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "새이름"), {nickname: "새이름", uid: ALICE});
      });
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {nickname: "새이름", profileImg: "2"}));
    });

    it("nicknames 문서가 없거나 다른 사람 소유인 닉네임으로는 바꿀 수 없다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "밥"), {nickname: "밥", uid: BOB});
      });
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {nickname: "없는이름"}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {nickname: "밥"}));
    });

    it("닉네임을 빈 값·경로로 쓸 수 없는 값으로 바꾸거나 필드를 지울 수 없다", async () => {
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {nickname: ""}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {nickname: "a/b"}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {nickname: deleteField()}));
    });

    it("첫 로그인: nicknames 생성과 users 수정을 한 트랜잭션에서 하면 허용 (completeFirstLogin)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", BOB), {nickname: "", isFirstLogin: true});
      });
      const db = asBob();
      await assertSucceeds(runTransaction(db, async (txn) => {
        const userRef = doc(db, "users", BOB);
        const nicknameRef = doc(db, "nicknames", "밥");
        await txn.get(userRef);
        await txn.get(nicknameRef);
        txn.set(nicknameRef, {nickname: "밥", uid: BOB});
        txn.update(userRef, {nickname: "밥", isFirstLogin: false});
      }));
    });

    it("첫 로그인: 같은 쓰기에 nicknames 생성이 없으면 거부", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", BOB), {nickname: "", isFirstLogin: true});
      });
      await assertFails(updateDoc(doc(asBob(), "users", BOB), {nickname: "밥", isFirstLogin: false}));
    });

    it("빈 닉네임으로 문서를 만들 수 있다 (createUser)", async () => {
      await assertSucceeds(setDoc(doc(asBob(), "users", BOB), {nickname: "", profileImg: "1", isFirstLogin: true}));
    });

    it("본인이 선점한 닉네임으로는 문서를 만들 수 있다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "밥"), {nickname: "밥", uid: BOB});
      });
      await assertSucceeds(setDoc(doc(asBob(), "users", BOB), {nickname: "밥"}));
    });

    it("문서가 없을 때 다른 사람 닉네임으로 만들 수 없다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "밥"), {nickname: "밥", uid: BOB});
        await deleteDoc(doc(db, "users", ALICE));
      });
      await assertFails(setDoc(doc(asAlice(), "users", ALICE), {nickname: "밥"}));
    });
  });

  // 회원탈퇴는 deleteAccount 함수(Admin SDK)가 처리하므로 앱에서 users 문서를 지울 수 없다 (#457)
  it("본인 문서도 삭제할 수 없다", async () => {
    await assertFails(deleteDoc(doc(asAlice(), "users", ALICE)));
  });

  it("본인 화분·기록은 삭제할 수 있다 (화분 삭제·기록 삭제는 앱에서 처리)", async () => {
    await assertSucceeds(deleteDoc(doc(asAlice(), "users", ALICE, "pots", "pot1", "logs", "log1")));
    await assertSucceeds(deleteDoc(doc(asAlice(), "users", ALICE, "pots", "pot1")));
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
