// 화분 이름 검사 규칙 테스트 (#561, #532)
// 앱 기준: PotNameRule.MAX_LENGTH = 15 (String.length, UTF-16 단위), 저장 전 Kotlin trim() (#563),
//          화면에서 붙여넣은 줄바꿈('\n') 차단 (NewBornTreeScreen, StudyPlanDetailScreen)
// 앱이 저장하는 이름을 규칙이 거부하지 않고, 앱을 거치지 않은 이름만 거부하는지 확인한다
const {describe, it, before, after, beforeEach} = require("node:test");
const assert = require("node:assert");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {doc, getDoc, setDoc, updateDoc} = require("firebase/firestore");
const {ALICE, createTestEnv, seed, completePotBatch, levelUpBatch} = require("../helpers");

const HOUR = 60 * 60 * 1000;

// 앱이 저장할 수 있는 이름 (앱 기준을 통과)
const VALID_NAMES = {
  "한글 15자": "가".repeat(15),
  "영문 1자": "a",
  "가운데 공백": "새  싹",
  // 규칙 size()도 UTF-16 단위로 센다: 🌱(U+1F331)는 2
  "이모지 7개 (UTF-16 14)": "🌱".repeat(7),
  "한글 13자 + 이모지 (UTF-16 15)": "가".repeat(13) + "🌱",
  "가운데 \\r (앱은 \\n만 막음)": "이\r름",
  // Kotlin Char.isWhitespace가 아니라 trim()이 지우지 않는 문자는 앞뒤에 있어도 앱이 그대로 저장한다
  "앞에 \\u0001 (제어 문자)": "\u0001이름",
  "뒤에 \\u200B (폭 없는 공백, 서식 문자)": "이름​",
};

// 앱을 거치지 않은 이름 (앱 기준에서 거부되거나 trim으로 바뀌는 이름)
const INVALID_NAMES = {
  "빈 문자열": "",
  "한글 16자": "가".repeat(16),
  "이모지 8개 (UTF-16 16)": "🌱".repeat(8),
  "한글 14자 + 이모지 (UTF-16 16)": "가".repeat(14) + "🌱",
  "앞 공백": " 이름",
  "뒤 공백": "이름 ",
  "앞 탭": "\t이름",
  "뒤 \\r": "이름\r",
  "앞 전각 공백 U+3000": "　이름",
  "뒤 줄바꿈 없는 공백 U+00A0": "이름 ",
  "앞 \\u001F": "\u001F이름",
  "가운데 줄바꿈": "이\n름",
};

describe("화분 이름 (#561)", () => {
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
      await setDoc(doc(db, "users", ALICE), {nickname: "앨리스", item: {heart: 5}});
      await setDoc(doc(db, "users", ALICE, "pots", "pot1"), {
        name: "화분", potTotalStudyingTime: 0, potLevel: 0, isCompleted: false,
      });
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const newPot = (name) => ({name, potTotalStudyingTime: 0, potLevel: 0, isCompleted: false});

  describe("생성", () => {
    for (const [label, name] of Object.entries(VALID_NAMES)) {
      it(`허용: ${label}`, async () => {
        await assertSucceeds(setDoc(doc(asAlice(), "users", ALICE, "pots", "new"), newPot(name)));
      });
    }
    for (const [label, name] of Object.entries(INVALID_NAMES)) {
      it(`거부: ${label}`, async () => {
        await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "new"), newPot(name)));
      });
    }

    it("거부: 이름이 없거나 문자열이 아님", async () => {
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "new"), {potTotalStudyingTime: 0}));
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "new"), newPot(15)));
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "new"), newPot(null)));
    });
  });

  describe("이름 변경 (updatePotName): 생성과 같은 기준", () => {
    for (const [label, name] of Object.entries(VALID_NAMES)) {
      it(`허용: ${label}`, async () => {
        await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {name}));
      });
    }
    for (const [label, name] of Object.entries(INVALID_NAMES)) {
      it(`거부: ${label}`, async () => {
        await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {name}));
      });
    }
  });

  // 이름은 바꿀 때만 검사한다: 이미 기준에 맞지 않게 저장된 이름(규칙 이전 데이터)이 있어도 다른 쓰기는 막히지 않는다
  describe("이름을 바꾸지 않는 쓰기는 이름을 검사하지 않는다", () => {
    const STORED_INVALID = "가".repeat(20) + "\n줄바꿈";

    beforeEach(async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE, "pots", "pot1"), {
          name: STORED_INVALID, potTotalStudyingTime: 10 * HOUR, potLevel: 0, isCompleted: false,
        });
      });
    });

    it("레벨업 batch 허용 (#456)", async () => {
      await assertSucceeds(levelUpBatch(asAlice(), {newLevel: 1, items: {heart: 1}}));
    });

    it("화분 완료 batch 허용 (#455)", async () => {
      await assertSucceeds(completePotBatch(asAlice()));
    });

    it("바뀌는 것이 없는 쓰기(같은 이름 다시 쓰기) 허용", async () => {
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {name: STORED_INVALID}));
    });

    it("기준에 맞지 않는 다른 이름으로는 바꿀 수 없고, 기준에 맞는 이름으로는 바꿀 수 있다", async () => {
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {name: "가".repeat(19)}));
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {name: "가".repeat(15)}));
      let stored;
      await testEnv.withSecurityRulesDisabled(async (context) => {
        stored = (await getDoc(doc(context.firestore(), "users", ALICE, "pots", "pot1"))).data().name;
      });
      assert.strictEqual(stored, "가".repeat(15));
    });
  });
});
