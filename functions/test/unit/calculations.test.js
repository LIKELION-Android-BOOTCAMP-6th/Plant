// calculations.js 유닛 테스트 (#620)
// Firestore·에뮬레이터 없이 실행된다: npm --prefix functions run test:unit
// 1차 PR에서는 실행 구조 확인용으로 일부만 둔다. 이슈 #620 "계산 로직 유닛 테스트" 표는 2차 PR에서 채운다
const {describe, it} = require("node:test");
const assert = require("node:assert");
const {computeStudyReward, toKstDateKey} = require("../../calculations");

const HOUR_MS = 60 * 60 * 1000;

describe("computeStudyReward", () => {
  it("1시간 미만은 보상 없음, 1시간부터 1티어", () => {
    assert.strictEqual(computeStudyReward(HOUR_MS - 1000), null);
    assert.deepStrictEqual(
        computeStudyReward(HOUR_MS),
        {items: ["heart"], box: 1},
    );
  });
});

describe("toKstDateKey", () => {
  it("UTC 15:00(KST 00:00)부터 다음 날짜", () => {
    assert.strictEqual(
        toKstDateKey(Date.parse("2026-11-17T14:59:59.999Z")), "2026-11-17",
    );
    assert.strictEqual(
        toKstDateKey(Date.parse("2026-11-17T15:00:00.000Z")), "2026-11-18",
    );
  });
});
