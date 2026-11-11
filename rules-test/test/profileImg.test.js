// 프로필 이미지(users.profileImg) 해금 레벨 검사 테스트 (#602)
// 앱은 프로필 편집 후보를 0..unlockedProfileLevel로 보여주고(GetProfileImageLevelListUseCase),
// 고른 레벨을 "0"~"6" 숫자 문자열로 저장한다 (MypageScreen의 level.toString(), UserRemoteDataSourceImpl.updateProfile)
// 다른 문서에 복사된 profileImg(게시글·댓글·공부 중 표시)는 이 이슈 범위 밖이다 (#602 선택지 A)
const {describe, it, before, after, beforeEach} = require("node:test");
const assert = require("node:assert");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {deleteField, doc, getDoc, runTransaction, setDoc, updateDoc} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, seed} = require("../helpers");

describe("프로필 이미지 해금 레벨 검사 (#602)", () => {
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
      // 해금 레벨 3까지 올린 유저 (서버 트리거가 쓴 값)
      await setDoc(doc(db, "users", ALICE), {
        nickname: "앨리스", profileImg: "1", isFirstLogin: false, unlockedProfileLevel: 3,
      });
      await setDoc(doc(db, "nicknames", "앨리스"), {nickname: "앨리스", uid: ALICE});
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();
  const aliceRef = () => doc(asAlice(), "users", ALICE);

  async function seedAlice(fields) {
    await seed(testEnv, async (db) => {
      await setDoc(doc(db, "users", ALICE), {nickname: "앨리스", isFirstLogin: false, ...fields});
    });
  }

  async function readProfileImg() {
    let value;
    await testEnv.withSecurityRulesDisabled(async (context) => {
      value = (await getDoc(doc(context.firestore(), "users", ALICE))).data().profileImg;
    });
    return value;
  }

  // 앱의 updateProfile 트랜잭션 (UserRemoteDataSourceImpl.updateProfile)
  function updateProfileTxn(db, {nickname, profileImg}) {
    return runTransaction(db, async (txn) => {
      const userRef = doc(db, "users", ALICE);
      const current = (await txn.get(userRef)).data();
      if (current.nickname === nickname) {
        txn.update(userRef, {profileImg});
        return;
      }
      const newRef = doc(db, "nicknames", nickname);
      await txn.get(newRef);
      txn.set(newRef, {nickname, uid: ALICE});
      if (current.nickname) txn.delete(doc(db, "nicknames", current.nickname));
      txn.update(userRef, {nickname, profileImg});
    });
  }

  describe("profileImg 변경", () => {
    it("해금 레벨 이하의 숫자로 바꿀 수 있다 (해금 3: \"0\"~\"3\")", async () => {
      for (const value of ["0", "2", "3"]) {
        await assertSucceeds(updateDoc(aliceRef(), {profileImg: value}));
        assert.strictEqual(await readProfileImg(), value);
      }
    });

    it("해금 레벨보다 높은 숫자로는 바꿀 수 없다 (해금 3: \"4\"~\"6\")", async () => {
      for (const value of ["4", "5", "6"]) {
        await assertFails(updateDoc(aliceRef(), {profileImg: value}));
      }
      assert.strictEqual(await readProfileImg(), "1");
    });

    it("unlockedProfileLevel 필드가 없는 문서는 0으로 보고 \"0\"만 허용", async () => {
      await seedAlice({profileImg: ""});
      await assertFails(updateDoc(aliceRef(), {profileImg: "1"}));
      await assertSucceeds(updateDoc(aliceRef(), {profileImg: "0"}));
    });

    it("해금 레벨 6이면 \"6\"까지 허용", async () => {
      await seedAlice({profileImg: "", unlockedProfileLevel: 6});
      await assertSucceeds(updateDoc(aliceRef(), {profileImg: "6"}));
    });

    it("해금 레벨이 6보다 큰 비정상 값이어도 \"7\" 이상은 형식 검사에서 거부", async () => {
      await seedAlice({profileImg: "", unlockedProfileLevel: 99});
      await assertSucceeds(updateDoc(aliceRef(), {profileImg: "6"}));
      await assertFails(updateDoc(aliceRef(), {profileImg: "7"}));
      await assertFails(updateDoc(aliceRef(), {profileImg: "10"}));
    });

    it("형식이 아닌 값으로는 바꿀 수 없다 (앱은 \"0\"~\"6\"만 저장)", async () => {
      for (const value of ["", "lv.3", "lv.0", "7", "-1", "03", "a", " 3", "3 ", "3\n"]) {
        await assertFails(updateDoc(aliceRef(), {profileImg: value}), `profileImg: ${JSON.stringify(value)}`);
      }
      // 문자열이 아닌 타입
      for (const value of [3, 0, null, true]) {
        await assertFails(updateDoc(aliceRef(), {profileImg: value}), `profileImg: ${JSON.stringify(value)}`);
      }
      assert.strictEqual(await readProfileImg(), "1");
    });

    it("profileImg 필드를 지울 수 없다", async () => {
      await assertFails(updateDoc(aliceRef(), {profileImg: deleteField()}));
    });

    it("다른 사람 문서의 profileImg는 해금 레벨 이하여도 바꿀 수 없다", async () => {
      await assertFails(updateDoc(doc(asBob(), "users", ALICE), {profileImg: "0"}));
    });
  });

  describe("profileImg를 바꾸지 않는 쓰기", () => {
    // 저장된 값이 형식 밖(회원가입 값 "", 예전 형식 "lv.N", 해금 레벨보다 높은 예전 값)이어도 다른 쓰기는 막지 않는다
    for (const stored of ["", "lv.3", "6"]) {
      it(`저장된 값이 ${JSON.stringify(stored)}이어도 세션 토큰·대표 화분·첫 로그인 쓰기는 허용`, async () => {
        // isFirstLogin을 true로 두어 아래 첫 로그인 쓰기(false)가 실제로 바뀌는 쓰기가 되게 한다
        await seedAlice({profileImg: stored, unlockedProfileLevel: 0, isFirstLogin: true});
        await assertSucceeds(updateDoc(aliceRef(), {activeSessionToken: "token"}));
        await assertSucceeds(updateDoc(aliceRef(), {lastSelectedPotId: "pot1"}));
        await assertSucceeds(updateDoc(aliceRef(), {isFirstLogin: false}));
        // 같은 값을 다시 써도 바뀐 필드가 아니므로 검사하지 않는다
        await assertSucceeds(updateDoc(aliceRef(), {profileImg: stored}));
        assert.strictEqual(await readProfileImg(), stored);
      });
    }

    it("저장된 값이 \"lv.3\"이어도 닉네임만 바꾸는 프로필 편집은 허용 (이미지를 고르지 않으면 앱은 저장된 이미지 값을 그대로 다시 보냄)", async () => {
      await seedAlice({profileImg: "lv.3", unlockedProfileLevel: 3});
      await assertSucceeds(updateProfileTxn(asAlice(), {nickname: "새이름", profileImg: "lv.3"}));
      assert.strictEqual(await readProfileImg(), "lv.3");
    });

    it("첫 로그인(completeFirstLogin): 회원가입 값 \"\" 그대로 닉네임 설정 허용", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", BOB), {nickname: "", profileImg: "", isFirstLogin: true, unlockedProfileLevel: 0});
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
  });

  describe("프로필 편집 트랜잭션 (updateProfile)", () => {
    it("이미지만 바꾸기: 해금 레벨 이하면 허용, 넘으면 거부", async () => {
      await assertSucceeds(updateProfileTxn(asAlice(), {nickname: "앨리스", profileImg: "3"}));
      await assertFails(updateProfileTxn(asAlice(), {nickname: "앨리스", profileImg: "4"}));
      assert.strictEqual(await readProfileImg(), "3");
    });

    it("닉네임 + 이미지 함께 바꾸기: 둘 다 통과하면 허용", async () => {
      await assertSucceeds(updateProfileTxn(asAlice(), {nickname: "새이름", profileImg: "2"}));
      assert.strictEqual(await readProfileImg(), "2");
    });

    it("닉네임 + 이미지 함께 바꾸기: 이미지가 해금 레벨을 넘으면 닉네임 변경도 함께 거부", async () => {
      await assertFails(updateProfileTxn(asAlice(), {nickname: "새이름", profileImg: "4"}));
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const admin = context.firestore();
        assert.strictEqual((await getDoc(doc(admin, "users", ALICE))).data().nickname, "앨리스");
        assert.strictEqual((await getDoc(doc(admin, "nicknames", "새이름"))).exists(), false);
        assert.strictEqual((await getDoc(doc(admin, "nicknames", "앨리스"))).exists(), true);
      });
    });

    it("닉네임 + 이미지를 직접 함께 쓸 때: 이미지가 통과해도 닉네임 소유 검사(#535)에 걸리면 거부 (앱 트랜잭션은 남의 닉네임이면 쓰기 전에 멈춤)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "밥"), {nickname: "밥", uid: BOB});
      });
      await assertFails(updateDoc(aliceRef(), {nickname: "밥", profileImg: "2"}));
      assert.strictEqual(await readProfileImg(), "1");
    });
  });

  describe("회원가입 (createUser)", () => {
    const bobRef = () => doc(asBob(), "users", BOB);

    it("profileImg가 \"\"이면 만들 수 있다 (앱의 User.create())", async () => {
      await assertSucceeds(setDoc(bobRef(), {nickname: "", profileImg: "", isFirstLogin: true}));
    });

    it("profileImg가 없으면 \"\"로 보고 만들 수 있다", async () => {
      await assertSucceeds(setDoc(bobRef(), {nickname: "", isFirstLogin: true}));
    });

    it("profileImg를 \"\"가 아닌 값으로 만들 수 없다 (\"0\" 포함)", async () => {
      for (const value of ["0", "1", "6", "lv.0", 0, null]) {
        await assertFails(setDoc(bobRef(), {nickname: "", profileImg: value, isFirstLogin: true}),
          `profileImg: ${JSON.stringify(value)}`);
      }
    });
  });
});
