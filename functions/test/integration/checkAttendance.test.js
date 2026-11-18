// checkAttendance Callable 통합 테스트 (#620)
// 에뮬레이터에서 실행된다: npm --prefix functions test
// 1차 PR에서는 Callable 호출 헬퍼(callFunction) 동작 확인용으로 성공 1건·실패 1건만 둔다
const {describe, it, before} = require("node:test");
const assert = require("node:assert");
const {toKstDateKey} = require("../../calculations");
const {
  CallableError, callFunction, clearAuth, clearFirestore, db, uniqueId,
} = require("../helpers/emulator");

describe("checkAttendance", () => {
  before(async () => {
    await Promise.all([clearFirestore(), clearAuth()]);
  });

  it("첫 출석은 1회차 SUCCESS이고 오늘 KST 날짜를 저장한다", async () => {
    const uid = uniqueId("user");
    const userRef = db.collection("users").doc(uid);
    await userRef.set({nickname: "출석"});
    // 호출 중 KST 자정이 지날 수 있어 전후 날짜를 모두 허용한다
    const keyBefore = toKstDateKey(Date.now());

    const result = await callFunction("checkAttendance", {}, {uid});

    const keyAfter = toKstDateKey(Date.now());
    assert.deepStrictEqual(
        result, {status: "SUCCESS", newCount: 1, reward: null},
    );
    const monthCheck = (await userRef.get()).get("dailyCheckThisMonth");
    assert.strictEqual(monthCheck.count, 1);
    assert.ok(
        [keyBefore, keyAfter].includes(monthCheck.lastCheckedDate),
        `lastCheckedDate: ${monthCheck.lastCheckedDate}`,
    );
  });

  it("로그인하지 않으면 unauthenticated", async () => {
    await assert.rejects(
        callFunction("checkAttendance"),
        (error) => error instanceof CallableError &&
          error.code === "unauthenticated",
    );
  });
});
