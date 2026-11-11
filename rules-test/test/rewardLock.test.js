// 보상·재화·학습 시간 필드 잠금 테스트 (#532)
// 서버(Cloud Functions, Admin SDK라 규칙 무관)만 쓰는 필드와, 하위 이슈의 batch 검증으로만 바뀌는 필드를
// 앱(본인)이 직접 쓰면 거부되는지 확인한다. batch 검증 통과는 각 하위 이슈의 테스트에서 확인한다
//   - 학습 저장: studyLogs.test.js (#453)
//   - 화분 완료·기록 삭제·화분 삭제: potCompleteDelete.test.js (#455)
//   - 레벨업: potLevelUp.test.js (#456)
//   - 화분 이름: potName.test.js (#561)
const {describe, it, before, after, beforeEach} = require("node:test");
const assert = require("node:assert");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {
  Timestamp, deleteDoc, deleteField, doc, getDoc, increment, serverTimestamp, setDoc, updateDoc,
} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, seed} = require("../helpers");

const HOUR = 60 * 60 * 1000;
const ALL_ITEM_KEYS = ["heart", "sun", "water", "fertilizer", "nutrient", "box"];

/**
 * 앱 회원가입(UserRepositoryImpl.createUser)이 보내는 users 문서
 * User.create().toDto()를 Firestore가 직렬화한 값 (uid는 @DocumentId라 쓰지 않음)
 */
function appNewUser(overrides = {}) {
  return {
    nickname: "",
    profileImg: "",
    lastSelectedPotId: "",
    isFirstLogin: true,
    totalStudyTime: 0,
    gold: 0,
    dailyCheckThisMonth: {count: 0, lastCheckedAt: null, lastCheckedDate: null},
    item: {heart: 0, sun: 0, water: 0, fertilizer: 0, nutrient: 0, box: 0},
    activeSessionToken: null,
    unlockedProfileLevel: 0,
    ...overrides,
  };
}

describe("보상·재화·학습 시간 잠금 (#532)", () => {
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
      // 출석·보상을 받은 뒤의 유저 (서버가 쓴 값)
      await setDoc(doc(db, "users", ALICE), appNewUser({
        nickname: "앨리스",
        isFirstLogin: false,
        totalStudyTime: 3 * HOUR,
        gold: 100,
        item: {heart: 5, sun: 5, water: 5, fertilizer: 5, nutrient: 5, box: 2},
        dailyCheckThisMonth: {count: 3, lastCheckedAt: Timestamp.now(), lastCheckedDate: "2026-11-12"},
        unlockedProfileLevel: 1,
        completedPotsCount: 0,
      }));
      await setDoc(doc(db, "users", ALICE, "pots", "pot1"), {
        tag_id: "t1", tag_name: "태그", name: "화분", potTotalStudyingTime: 3 * HOUR,
        createdAt: null, completedAt: null, isCompleted: false, lastStudiedAt: null, potLevel: 1,
      });
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();
  const aliceRef = () => doc(asAlice(), "users", ALICE);
  const potRef = () => doc(asAlice(), "users", ALICE, "pots", "pot1");

  async function readAlice() {
    let data;
    await testEnv.withSecurityRulesDisabled(async (context) => {
      data = (await getDoc(doc(context.firestore(), "users", ALICE))).data();
    });
    return data;
  }

  describe("users 수정: 서버만 쓰는 필드", () => {
    it("gold를 늘리거나 줄이거나 임의 값으로 바꿀 수 없다", async () => {
      await assertFails(updateDoc(aliceRef(), {gold: increment(1)}));
      await assertFails(updateDoc(aliceRef(), {gold: increment(-1)}));
      await assertFails(updateDoc(aliceRef(), {gold: 999999}));
      await assertFails(updateDoc(aliceRef(), {gold: deleteField()}));
      assert.strictEqual((await readAlice()).gold, 100);
    });

    it("item.*(성장 아이템 5종 + box)를 늘리거나 줄일 수 없다 (레벨업 batch 제외, potLevelUp.test.js)", async () => {
      for (const key of ALL_ITEM_KEYS) {
        await assertFails(updateDoc(aliceRef(), {[`item.${key}`]: increment(1)}));
        await assertFails(updateDoc(aliceRef(), {[`item.${key}`]: increment(-1)}));
      }
      await assertFails(updateDoc(aliceRef(), {item: {heart: 99, sun: 99, water: 99, fertilizer: 99, nutrient: 99, box: 99}}));
      await assertFails(updateDoc(aliceRef(), {"item.gem": 1}));
      assert.deepStrictEqual((await readAlice()).item, {heart: 5, sun: 5, water: 5, fertilizer: 5, nutrient: 5, box: 2});
    });

    it("출석(dailyCheckThisMonth)을 바꿀 수 없다 (#531 서버 판정)", async () => {
      await assertFails(updateDoc(aliceRef(), {"dailyCheckThisMonth.count": increment(1)}));
      await assertFails(updateDoc(aliceRef(), {"dailyCheckThisMonth.lastCheckedDate": "2026-11-13"}));
      await assertFails(updateDoc(aliceRef(), {"dailyCheckThisMonth.lastCheckedAt": serverTimestamp()}));
      await assertFails(updateDoc(aliceRef(), {dailyCheckThisMonth: {count: 0, lastCheckedAt: null, lastCheckedDate: null}}));
      await assertFails(updateDoc(aliceRef(), {dailyCheckThisMonth: deleteField()}));
    });

    it("프로필 이미지 해금 레벨(unlockedProfileLevel)을 바꿀 수 없다 (#598 트리거)", async () => {
      await assertFails(updateDoc(aliceRef(), {unlockedProfileLevel: 6}));
      await assertFails(updateDoc(aliceRef(), {unlockedProfileLevel: increment(1)}));
      await assertFails(updateDoc(aliceRef(), {unlockedProfileLevel: deleteField()}));
    });

    it("학습 시간·완료 수·각 batch의 표시 필드는 batch 검증 없이 바꿀 수 없다", async () => {
      await assertFails(updateDoc(aliceRef(), {totalStudyTime: increment(HOUR)}));
      await assertFails(updateDoc(aliceRef(), {totalStudyTime: increment(-HOUR)}));
      await assertFails(updateDoc(aliceRef(), {lastStudyEndedAt: Timestamp.now()}));
      await assertFails(updateDoc(aliceRef(), {completedPotsCount: increment(1)}));
      for (const key of ["lastStudyPotId", "lastStudySessionId", "lastCompletedPotId",
        "lastDeletedPotId", "lastDeletedLogId", "lastLevelUpPotId"]) {
        await assertFails(updateDoc(aliceRef(), {[key]: "pot1"}));
      }
    });

    it("규칙에 없는 새 필드를 넣을 수 없다", async () => {
      await assertFails(updateDoc(aliceRef(), {isAdmin: true}));
    });
  });

  describe("users 수정: 앱이 쓰는 프로필 필드 (기존 동작 유지)", () => {
    // nickname 변경은 nicknames 문서 소유 확인이 함께 필요하다 (users.test.js, #535)
    // profileImg는 해금 레벨(시드 1) 이하로만 바꿀 수 있다 (profileImg.test.js, #602)
    it("profileImg, lastSelectedPotId, isFirstLogin, activeSessionToken은 바꿀 수 있다", async () => {
      await assertSucceeds(updateDoc(aliceRef(), {profileImg: "1"}));
      await assertSucceeds(updateDoc(aliceRef(), {lastSelectedPotId: "pot1"}));
      // 시드가 false라 실제로 바뀌는 값(true)으로 쓴다 (앱은 false만 쓰지만 이 테스트는 필드를 바꿀 수 있는지 확인)
      await assertSucceeds(updateDoc(aliceRef(), {isFirstLogin: true}));
      await assertSucceeds(updateDoc(aliceRef(), {activeSessionToken: "token"}));
      await assertSucceeds(updateDoc(aliceRef(), {activeSessionToken: null}));
    });

    it("본인 소유 닉네임으로는 바꿀 수 있다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "새이름"), {nickname: "새이름", uid: ALICE});
      });
      await assertSucceeds(updateDoc(aliceRef(), {nickname: "새이름", profileImg: "1"}));
    });

    it("프로필 필드와 잠긴 필드를 함께 쓰면 전체가 거부된다", async () => {
      await assertFails(updateDoc(aliceRef(), {profileImg: "1", gold: increment(100)}));
      await assertFails(updateDoc(aliceRef(), {lastSelectedPotId: "pot1", "item.box": increment(1)}));
      assert.strictEqual((await readAlice()).profileImg, "");
    });
  });

  describe("users 생성 (createUser): 초기값만", () => {
    beforeEach(async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await deleteDoc(doc(context.firestore(), "users", BOB));
      });
    });
    const bobRef = () => doc(asBob(), "users", BOB);

    it("앱 회원가입과 같은 문서는 만들 수 있다", async () => {
      await assertSucceeds(setDoc(bobRef(), appNewUser()));
    });

    it("값이 없는 필드는 0·없음으로 보므로 생략해도 만들 수 있다", async () => {
      await assertSucceeds(setDoc(bobRef(), {nickname: ""}));
    });

    it("gold·아이템·학습 시간·해금 레벨을 0이 아닌 값으로 만들 수 없다", async () => {
      await assertFails(setDoc(bobRef(), appNewUser({gold: 1})));
      await assertFails(setDoc(bobRef(), appNewUser({totalStudyTime: 1})));
      await assertFails(setDoc(bobRef(), appNewUser({unlockedProfileLevel: 1})));
      for (const key of ALL_ITEM_KEYS) {
        await assertFails(setDoc(bobRef(), appNewUser({item: {...appNewUser().item, [key]: 1}})));
      }
    });

    it("아이템·출석 값의 형식이 다르거나 모르는 키가 있으면 만들 수 없다", async () => {
      await assertFails(setDoc(bobRef(), appNewUser({item: 0})));
      await assertFails(setDoc(bobRef(), appNewUser({item: {...appNewUser().item, gem: 0}})));
      await assertFails(setDoc(bobRef(), appNewUser({dailyCheckThisMonth: 0})));
      await assertFails(setDoc(bobRef(), appNewUser({dailyCheckThisMonth: {count: 0, extra: 1}})));
    });

    it("출석 기록이 있는 상태로 만들 수 없다", async () => {
      await assertFails(setDoc(bobRef(), appNewUser({dailyCheckThisMonth: {count: 1, lastCheckedAt: null, lastCheckedDate: null}})));
      await assertFails(setDoc(bobRef(), appNewUser({
        dailyCheckThisMonth: {count: 0, lastCheckedAt: Timestamp.now(), lastCheckedDate: null},
      })));
      await assertFails(setDoc(bobRef(), appNewUser({
        dailyCheckThisMonth: {count: 0, lastCheckedAt: null, lastCheckedDate: "2026-11-12"},
      })));
    });

    it("완료 수·batch 표시 필드·모르는 필드를 넣어 만들 수 없다", async () => {
      await assertFails(setDoc(bobRef(), appNewUser({completedPotsCount: 0})));
      await assertFails(setDoc(bobRef(), appNewUser({lastStudySessionId: "s"})));
      await assertFails(setDoc(bobRef(), appNewUser({isAdmin: true})));
    });
  });

  describe("pots 수정: batch 검증이 없는 쓰기는 이름만", () => {
    it("학습 시간·최근 학습 시각·완료·레벨을 직접 바꿀 수 없다", async () => {
      await assertFails(updateDoc(potRef(), {potTotalStudyingTime: increment(HOUR)}));
      await assertFails(updateDoc(potRef(), {potTotalStudyingTime: increment(-HOUR)}));
      await assertFails(updateDoc(potRef(), {lastStudiedAt: serverTimestamp()}));
      await assertFails(updateDoc(potRef(), {isCompleted: true}));
      await assertFails(updateDoc(potRef(), {completedAt: serverTimestamp()}));
      await assertFails(updateDoc(potRef(), {potLevel: 6}));
      await assertFails(updateDoc(potRef(), {potLevel: increment(1)}));
    });

    it("태그 등 이름 외 필드나 새 필드도 바꿀 수 없다", async () => {
      await assertFails(updateDoc(potRef(), {tag_id: "t2", tag_name: "다른 태그"}));
      await assertFails(updateDoc(potRef(), {isAdmin: true}));
      await assertFails(updateDoc(potRef(), {name: "새 이름", potLevel: 6}));
    });

    it("이름 변경(updatePotName)은 허용", async () => {
      await assertSucceeds(updateDoc(potRef(), {name: "새 이름"}));
    });
  });

  describe("pots 생성: 초기값만", () => {
    const newPotRef = () => doc(asAlice(), "users", ALICE, "pots", "new");
    /** 앱 PotRemoteDataSourceImpl.addPot이 보내는 화분 (id는 @DocumentId라 쓰지 않음) */
    const appNewPot = (overrides = {}) => ({
      tag_id: "t1", tag_name: "태그", name: "새 화분", potTotalStudyingTime: 0,
      createdAt: null, completedAt: null, isCompleted: false, lastStudiedAt: null, potLevel: 0,
      ...overrides,
    });

    it("앱 화분 생성과 같은 문서는 만들 수 있다", async () => {
      await assertSucceeds(setDoc(newPotRef(), appNewPot()));
    });

    it("학습 시간·레벨·완료·최근 학습 시각을 초기값이 아닌 값으로 만들 수 없다", async () => {
      await assertFails(setDoc(newPotRef(), appNewPot({potTotalStudyingTime: HOUR})));
      await assertFails(setDoc(newPotRef(), appNewPot({potLevel: 1})));
      await assertFails(setDoc(newPotRef(), appNewPot({isCompleted: true})));
      await assertFails(setDoc(newPotRef(), appNewPot({completedAt: Timestamp.now()})));
      await assertFails(setDoc(newPotRef(), appNewPot({lastStudiedAt: Timestamp.now()})));
    });
  });
});
