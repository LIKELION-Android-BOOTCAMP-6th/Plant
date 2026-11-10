// 화분 레벨업 검증 규칙 테스트 (#456)
// 앱과 같은 batch(helpers.js의 levelUpBatch)로 허용·거부를 확인한다
// 조건 표는 앱 파일(LevelUpCondition.kt)을 직접 읽어 레벨마다 경계값을 만든다 → 규칙의 표와 어긋나면 실패한다
const {describe, it, before, after, beforeEach} = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {
  Timestamp, deleteDoc, doc, getDoc, increment, serverTimestamp, setDoc, updateDoc, writeBatch,
} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, seed, levelUpBatch} = require("../helpers");

const HOUR = 60 * 60 * 1000;
const ITEM_KEYS = ["heart", "sun", "water", "fertilizer", "nutrient"];
const MAX_LEVEL = 6;

const CONDITION_PATH = path.resolve(
  __dirname, "../../app/src/main/java/com/a32b/plant/domain/model/LevelUpCondition.kt",
);

/**
 * 앱의 PotLevelConditions.table을 읽는다
 * @return {Map<number, {minHours: number, items: Object<string, number>}>}
 */
function readAppConditionTable() {
  const source = fs.readFileSync(CONDITION_PATH, "utf8");
  const entryPattern =
    /(\d+)\s+to\s+LevelUpCondition\(\s*minHoursForLevelUp\s*=\s*(\d+)L,[\s\S]*?requiredItems\s*=\s*mapOf\(([\s\S]*?)\)\s*\)/g;
  const table = new Map();
  for (const [, level, minHours, itemsSource] of source.matchAll(entryPattern)) {
    const items = {};
    // ItemType.HEART → users.item.heart (ItemType.fieldKey는 이름의 소문자)
    for (const [, type, count] of itemsSource.matchAll(/ItemType\.(\w+)\s+to\s+(\d+)/g)) {
      items[type.toLowerCase()] = Number(count);
    }
    table.set(Number(level), {minHours: Number(minHours), items});
  }
  return table;
}

const CONDITIONS = readAppConditionTable();

/** 모든 성장 아이템을 count개로 채운 아이템 맵 */
function itemsOf(count, overrides = {}) {
  return {...Object.fromEntries(ITEM_KEYS.map((key) => [key, count])), ...overrides};
}

describe("화분 레벨업 (#456)", () => {
  let testEnv;
  before(async () => {
    testEnv = await createTestEnv();
  });
  after(async () => {
    await testEnv.cleanup();
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();

  /** alice의 화분 pot1과 아이템을 준비한다 */
  async function setup({level = 0, time = 0, items = itemsOf(0), pot = {}} = {}) {
    await seed(testEnv, async (db) => {
      await setDoc(doc(db, "users", ALICE), {nickname: "앨리스", item: {...items, box: 0}});
      await setDoc(doc(db, "users", ALICE, "pots", "pot1"), {
        name: "화분", potTotalStudyingTime: time, potLevel: level, isCompleted: false, ...pot,
      });
      await setDoc(doc(db, "users", BOB), {nickname: "밥", item: itemsOf(100)});
    });
  }

  /** 규칙을 끄고 현재 값을 읽는다 */
  async function read(...docPath) {
    let snapshot;
    await testEnv.withSecurityRulesDisabled(async (context) => {
      snapshot = await getDoc(doc(context.firestore(), ...docPath));
    });
    return snapshot.data();
  }

  describe("조건 표 (앱 PotLevelConditions와 같은지 경계값으로 확인)", () => {
    it("앱 조건 표를 읽었다 (LV0→1 ~ LV5→6)", () => {
      assert.deepStrictEqual([...CONDITIONS.keys()], [0, 1, 2, 3, 4, 5]);
      for (const {items} of CONDITIONS.values()) assert.ok(Object.keys(items).length > 0);
    });

    for (const [level, {minHours, items}] of CONDITIONS) {
      const exactTime = minHours * HOUR;
      const exactItems = itemsOf(0, items);

      describe(`LV${level} → LV${level + 1} (${minHours}시간, ${JSON.stringify(items)})`, () => {
        it("조건 딱 맞음 → 허용, 아이템이 필요 개수만큼 줄어 0이 된다", async () => {
          await setup({level, time: exactTime, items: exactItems});
          await assertSucceeds(levelUpBatch(asAlice(), {newLevel: level + 1, items}));
          assert.strictEqual((await read("users", ALICE, "pots", "pot1")).potLevel, level + 1);
          assert.deepStrictEqual((await read("users", ALICE)).item, {...itemsOf(0), box: 0});
        });

        it("학습 시간 1ms 부족 → 거부", async () => {
          await setup({level, time: exactTime - 1, items: exactItems});
          await assertFails(levelUpBatch(asAlice(), {newLevel: level + 1, items}));
        });

        for (const key of Object.keys(items)) {
          it(`${key} 1개 부족 → 거부 (차감 후 음수)`, async () => {
            await setup({level, time: exactTime, items: {...exactItems, [key]: items[key] - 1}});
            await assertFails(levelUpBatch(asAlice(), {newLevel: level + 1, items}));
          });

          it(`${key}을(를) 1개 덜 줄임 → 거부`, async () => {
            await setup({level, time: exactTime, items: itemsOf(100)});
            await assertFails(levelUpBatch(asAlice(), {
              newLevel: level + 1, items: {...items, [key]: items[key] - 1},
            }));
          });
        }
      });
    }
  });

  describe("레벨업 batch", () => {
    // LV0 → LV1: 1시간, 정성 1개
    const LV0 = {newLevel: 1, items: {heart: 1}};

    it("아이템을 줄이지 않고 레벨만 올림 → 거부", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100)});
      await assertFails(levelUpBatch(asAlice(), {...LV0, skipUser: true}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {potLevel: 1}));
    });

    it("필요한 것보다 많이 줄이거나, 필요 없는 아이템을 함께 줄임 → 거부", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100)});
      await assertFails(levelUpBatch(asAlice(), {newLevel: 1, items: {heart: 2}}));
      await assertFails(levelUpBatch(asAlice(), {newLevel: 1, items: {heart: 1, sun: 1}}));
    });

    it("레벨을 2 올리거나 낮춤 → 거부", async () => {
      await setup({level: 1, time: 100 * HOUR, items: itemsOf(100)});
      await assertFails(levelUpBatch(asAlice(), {newLevel: 3, items: CONDITIONS.get(1).items}));
      await assertFails(levelUpBatch(asAlice(), {newLevel: 0, items: CONDITIONS.get(1).items}));
      await assertFails(levelUpBatch(asAlice(), {newLevel: 1, items: CONDITIONS.get(1).items}));
    });

    it("같은 레벨업이 두 번 도착(연타·재전송) → 두 번째는 거부, 레벨·아이템은 한 번만 바뀐다", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100)});
      await assertSucceeds(levelUpBatch(asAlice(), LV0));
      await assertFails(levelUpBatch(asAlice(), LV0));
      assert.strictEqual((await read("users", ALICE, "pots", "pot1")).potLevel, 1);
      assert.strictEqual((await read("users", ALICE)).item.heart, 99);
    });

    it("최고 레벨(LV6) → 거부", async () => {
      await setup({level: MAX_LEVEL, time: 1000 * HOUR, items: itemsOf(100)});
      await assertFails(levelUpBatch(asAlice(), {newLevel: MAX_LEVEL + 1, items: {heart: 1}}));
      await assertFails(levelUpBatch(asAlice(), {newLevel: MAX_LEVEL + 1, items: {}}));
    });

    it("완료된 화분 → 거부", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100), pot: {isCompleted: true}});
      await assertFails(levelUpBatch(asAlice(), LV0));
    });

    it("남의 화분·없는 화분 → 거부", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100)});
      await assertFails(levelUpBatch(asBob(), {...LV0, uid: ALICE}));
      await assertFails(levelUpBatch(asAlice(), {...LV0, potId: "none"}));
    });

    it("레벨업 batch에서 학습 시간·이름 등 다른 화분 필드를 함께 바꿈 → 거부", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100)});
      await assertFails(levelUpBatch(asAlice(), {...LV0, pot: {potTotalStudyingTime: 100 * HOUR}}));
      await assertFails(levelUpBatch(asAlice(), {...LV0, pot: {name: "새 이름"}}));
    });

    it("예전 화분(potLevel·학습 시간 필드 없음)은 LV0으로 보고 검증한다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE), {nickname: "앨리스", item: itemsOf(100)});
        await setDoc(doc(db, "users", ALICE, "pots", "old"), {name: "예전 화분", potTotalStudyingTime: 2 * HOUR});
        await setDoc(doc(db, "users", ALICE, "pots", "empty"), {name: "빈 화분"});
      });
      await assertSucceeds(levelUpBatch(asAlice(), {...LV0, potId: "old"}));
      await assertFails(levelUpBatch(asAlice(), {...LV0, potId: "empty"}));
    });

    // 오프라인에서 학습 저장 → 레벨업: 대기열은 보낸 순서대로 도착하므로 학습 시간이 먼저 반영된 뒤 레벨업이 검증된다
    // (학습 저장 batch는 앱의 StudyingRemoteDataSourceImpl과 같은 쓰기, studyLogs.test.js의 studyBatch 참고)
    function studyBatch(db, {sessionId, time}) {
      const start = Date.now() - time - 60 * 1000;
      const batch = writeBatch(db);
      const potRef = doc(db, "users", ALICE, "pots", "pot1");
      batch.set(doc(potRef, "logs", sessionId), {
        uid: ALICE, title: "학습", contents: [], completedContents: [], studyingTime: time,
        createAt: Timestamp.now(), startedAt: Timestamp.fromMillis(start),
        rewardStatus: "pending", rewardSeen: false,
      });
      batch.update(potRef, {
        potTotalStudyingTime: increment(time), lastStudiedAt: serverTimestamp(), lastStudySessionId: sessionId,
      });
      batch.update(doc(db, "users", ALICE), {
        totalStudyTime: increment(time), lastStudyEndedAt: Timestamp.fromMillis(start + time),
        lastStudyPotId: "pot1", lastStudySessionId: sessionId,
      });
      return batch.commit();
    }

    it("학습 저장(59분 → 61분) 뒤에 도착한 레벨업은 허용, 학습 저장보다 먼저 도착하면 거부", async () => {
      await setup({time: 59 * 60 * 1000, items: itemsOf(100)});
      await assertFails(levelUpBatch(asAlice(), LV0));
      await assertSucceeds(studyBatch(asAlice(), {sessionId: "s1", time: 2 * 60 * 1000}));
      await assertSucceeds(levelUpBatch(asAlice(), LV0));
      assert.strictEqual((await read("users", ALICE, "pots", "pot1")).potLevel, 1);
    });

    it("새 화분은 레벨 0으로만 만들 수 있다", async () => {
      await setup();
      await assertSucceeds(setDoc(doc(asAlice(), "users", ALICE, "pots", "new"), {name: "새 화분", potLevel: 0}));
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "new2"), {name: "새 화분", potLevel: 3}));
    });
  });

  describe("유저 아이템 차감 (레벨업과 함께여야 함)", () => {
    const LV0 = {newLevel: 1, items: {heart: 1}};

    it("레벨업 없이 성장 아이템만 줄임 → 거부, 늘리거나 보너스 박스를 줄이는 쓰기는 허용", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100)});
      await assertFails(levelUpBatch(asAlice(), {...LV0, skipPot: true}));
      for (const key of ITEM_KEYS) {
        await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {[`item.${key}`]: increment(-1)}));
      }
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {"item.heart": increment(1)}));
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {"item.box": increment(1)}));
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {"item.box": increment(-1)}));
    });

    it("화분 레벨을 그대로 두고(이미 오른 레벨) 아이템만 줄임 → 거부", async () => {
      await setup({level: 1, time: 10 * HOUR, items: itemsOf(100)});
      await assertFails(levelUpBatch(asAlice(), {newLevel: 1, items: {heart: 1}}));
    });

    it("표시한 화분이 레벨업한 화분과 다름 / 표시 없음 → 거부", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100)});
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE, "pots", "pot2"), {name: "화분2", potTotalStudyingTime: 0, potLevel: 0});
      });
      await assertFails(levelUpBatch(asAlice(), {...LV0, markedPotId: "pot2"}));
      await assertFails(levelUpBatch(asAlice(), {...LV0, markedPotId: ""}));
    });

    it("같은 레벨 화분 여러 개를 아이템 한 번 차감으로 올림 → 거부 (batch 하나에 표시한 화분 1개만)", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(1)});
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE, "pots", "pot2"), {name: "화분2", potTotalStudyingTime: 10 * HOUR, potLevel: 0});
      });
      const db = asAlice();
      const batch = writeBatch(db);
      batch.update(doc(db, "users", ALICE, "pots", "pot1"), {potLevel: 1});
      batch.update(doc(db, "users", ALICE, "pots", "pot2"), {potLevel: 1});
      batch.update(doc(db, "users", ALICE), {"item.heart": increment(-1), lastLevelUpPotId: "pot1"});
      await assertFails(batch.commit());
      assert.strictEqual((await read("users", ALICE, "pots", "pot1")).potLevel, 0);
      assert.strictEqual((await read("users", ALICE, "pots", "pot2")).potLevel, 0);
      assert.strictEqual((await read("users", ALICE)).item.heart, 1);
    });

    it("같은 화분을 연속으로 레벨업하면 표시 값이 같아도 허용", async () => {
      await setup({time: 10 * HOUR, items: itemsOf(100)});
      await assertSucceeds(levelUpBatch(asAlice(), LV0));
      await assertSucceeds(levelUpBatch(asAlice(), {newLevel: 2, items: CONDITIONS.get(1).items}));
      assert.strictEqual((await read("users", ALICE, "pots", "pot1")).potLevel, 2);
    });

    it("레벨업 표시 필드는 따로 바꾸거나 새 유저에 넣을 수 없다", async () => {
      await setup();
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {lastLevelUpPotId: "pot1"}));
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await deleteDoc(doc(context.firestore(), "users", BOB));
      });
      await assertFails(setDoc(doc(asBob(), "users", BOB), {nickname: "", lastLevelUpPotId: "p"}));
      await assertSucceeds(setDoc(doc(asBob(), "users", BOB), {nickname: ""}));
    });
  });
});
