// 화분 완료·학습 기록 삭제·화분 삭제 검증 규칙 테스트 (#455)
// 앱과 같은 batch(helpers.js의 completePotBatch·deleteStudyLogBatch·deletePotBatch)로 허용·거부를 확인한다
const {describe, it, before, after, beforeEach} = require("node:test");
const assert = require("node:assert");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {
  Timestamp, deleteDoc, doc, getDoc, increment, setDoc, updateDoc,
} = require("firebase/firestore");
const {
  ALICE, BOB, createTestEnv, seed,
  completePotBatch, deleteStudyLogBatch, deletePotBatch,
} = require("../helpers");

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;

describe("화분 완료·기록 삭제·화분 삭제 (#455)", () => {
  let testEnv;
  before(async () => {
    testEnv = await createTestEnv();
  });
  after(async () => {
    await testEnv.cleanup();
  });

  // alice: 진행 중 화분 pot1(기록 l1 1시간 + l2 2시간 = 3시간), 완료된 화분 done(기록 d1 30분)
  // users.totalStudyTime = 3시간 30분, completedPotsCount = 1
  beforeEach(async () => {
    await testEnv.clearFirestore();
    await seed(testEnv, async (db) => {
      await setDoc(doc(db, "users", ALICE), {
        nickname: "앨리스", totalStudyTime: 3 * HOUR + 30 * MINUTE, completedPotsCount: 1,
      });
      await setDoc(doc(db, "users", ALICE, "pots", "pot1"), {
        name: "화분", potTotalStudyingTime: 3 * HOUR, isCompleted: false, completedAt: null,
      });
      await setDoc(doc(db, "users", ALICE, "pots", "pot1", "logs", "l1"), {uid: ALICE, studyingTime: HOUR});
      await setDoc(doc(db, "users", ALICE, "pots", "pot1", "logs", "l2"), {uid: ALICE, studyingTime: 2 * HOUR});
      await setDoc(doc(db, "users", ALICE, "pots", "done"), {
        name: "완료 화분", potTotalStudyingTime: 30 * MINUTE, isCompleted: true, completedAt: Timestamp.now(),
      });
      await setDoc(doc(db, "users", ALICE, "pots", "done", "logs", "d1"), {uid: ALICE, studyingTime: 30 * MINUTE});
      await setDoc(doc(db, "users", BOB), {nickname: "밥", totalStudyTime: 0});
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();

  /** 규칙을 끄고 현재 값을 읽는다 */
  async function read(...path) {
    let snapshot;
    await testEnv.withSecurityRulesDisabled(async (context) => {
      snapshot = await getDoc(doc(context.firestore(), ...path));
    });
    return snapshot;
  }

  async function user() {
    const snapshot = await read("users", ALICE);
    return {total: snapshot.get("totalStudyTime"), completed: snapshot.get("completedPotsCount")};
  }

  async function potTotal(potId) {
    return (await read("users", ALICE, "pots", potId)).get("potTotalStudyingTime");
  }

  async function exists(...path) {
    return (await read(...path)).exists();
  }

  describe("화분 완료", () => {
    it("진행 중 화분을 완료하면 완료 상태·완료 시각과 완료 수 +1이 함께 저장된다", async () => {
      await assertSucceeds(completePotBatch(asAlice()));
      const pot = await read("users", ALICE, "pots", "pot1");
      assert.strictEqual(pot.get("isCompleted"), true);
      assert.ok(pot.get("completedAt") instanceof Timestamp);
      assert.deepStrictEqual(await user(), {total: 3 * HOUR + 30 * MINUTE, completed: 2});
    });

    it("완료 수 필드가 없는 예전 유저 문서도 완료할 수 있다 (0에서 1로)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE), {nickname: "앨리스", totalStudyTime: 0});
      });
      await assertSucceeds(completePotBatch(asAlice()));
      assert.strictEqual((await user()).completed, 1);
    });

    it("같은 완료가 두 번 도착하면(연타·재전송) 두 번째는 거부되어 완료 수는 1번만 오른다", async () => {
      await assertSucceeds(completePotBatch(asAlice()));
      await assertFails(completePotBatch(asAlice()));
      assert.strictEqual((await user()).completed, 2);
    });

    it("이미 완료된 화분은 다시 완료할 수 없다", async () => {
      await assertFails(completePotBatch(asAlice(), {potId: "done"}));
    });

    it("완료 수를 2 이상 올리거나 줄이면 거부", async () => {
      await assertFails(completePotBatch(asAlice(), {countDelta: 2}));
      await assertFails(completePotBatch(asAlice(), {countDelta: 0}));
      await assertFails(completePotBatch(asAlice(), {countDelta: -1}));
    });

    it("완료하지 않고 완료 수만 올리거나, 완료 수 없이 완료만 하면 거부", async () => {
      await assertFails(completePotBatch(asAlice(), {skipPot: true}));
      await assertFails(completePotBatch(asAlice(), {skipUser: true}));
    });

    it("완료 표시가 다른 화분이면 거부 (완료된 화분 ID로 완료 수 올리기 포함)", async () => {
      await assertFails(completePotBatch(asAlice(), {markedPotId: "done"}));
      await assertFails(completePotBatch(asAlice(), {skipPot: true, markedPotId: "done"}));
    });

    it("완료 시각이 서버 시각이 아니거나 다른 필드를 함께 바꾸면 거부", async () => {
      await assertFails(completePotBatch(asAlice(), {pot: {completedAt: Timestamp.fromMillis(0)}}));
      await assertFails(completePotBatch(asAlice(), {pot: {potTotalStudyingTime: 0}}));
    });

    it("없는 화분, 다른 사람 화분은 완료할 수 없다", async () => {
      await assertFails(completePotBatch(asAlice(), {potId: "none"}));
      await assertFails(completePotBatch(asBob(), {uid: ALICE}));
      await assertFails(completePotBatch(asAlice(), {skipPot: true, markedPotId: "none"}));
    });

    it("완료 상태·완료 수는 따로 바꿀 수 없다 (완료 되돌리기 포함)", async () => {
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "done"), {isCompleted: false}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {isCompleted: true}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "done"), {completedAt: Timestamp.now()}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {completedPotsCount: 99}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {lastCompletedPotId: "pot1"}));
    });
    // 완료된 화분으로의 학습 저장 거부는 studyLogs.test.js (#453)
  });

  describe("학습 기록 삭제", () => {
    it("기록을 지우면 저장된 학습 시간만큼 화분·유저 학습 시간이 줄어든다", async () => {
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR}));
      assert.strictEqual(await exists("users", ALICE, "pots", "pot1", "logs", "l1"), false);
      assert.strictEqual(await potTotal("pot1"), 2 * HOUR);
      assert.deepStrictEqual(await user(), {total: 2 * HOUR + 30 * MINUTE, completed: 1});
    });

    it("같은 화분의 기록을 이어서 지울 수 있고, 완료된 화분의 기록도 지울 수 있다", async () => {
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR}));
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {logId: "l2", time: 2 * HOUR}));
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {potId: "done", logId: "d1", time: 30 * MINUTE}));
      assert.strictEqual(await potTotal("pot1"), 0);
      assert.strictEqual(await potTotal("done"), 0);
      assert.deepStrictEqual(await user(), {total: 0, completed: 1});
    });

    it("같은 삭제가 두 번 도착하면(연타·재전송) 두 번째는 거부되어 학습 시간은 1번만 줄어든다", async () => {
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR}));
      assert.strictEqual(await potTotal("pot1"), 2 * HOUR);
      assert.strictEqual((await user()).total, 2 * HOUR + 30 * MINUTE);
    });

    it("차감량이 저장된 학습 시간과 다르면 거부 (덜 빼기, 더 빼기, 음수 차감)", async () => {
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: 30 * MINUTE}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: 2 * HOUR}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: -HOUR}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR, potDelta: 0}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR, userDelta: 2 * HOUR}));
    });

    it("기록 삭제·화분 차감·유저 차감 중 하나라도 빠지면 거부", async () => {
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR, skipLog: true}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR, skipPot: true}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR, skipUser: true}));
      await assertFails(deleteDoc(doc(asAlice(), "users", ALICE, "pots", "pot1", "logs", "l1")));
    });

    it("표시 필드가 지우는 기록과 다르면 거부", async () => {
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR, markedLogId: "l2"}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR, markedPotId: "done"}));
    });

    it("없는 기록, 다른 사람 기록은 지울 수 없다", async () => {
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "none", time: 0}));
      await assertFails(deleteStudyLogBatch(asBob(), {uid: ALICE, logId: "l1", time: HOUR}));
    });

    it("차감 결과가 음수여도(예전 데이터 불일치) 차감량이 저장된 값과 같으면 허용", async () => {
      await seed(testEnv, async (db) => {
        await updateDoc(doc(db, "users", ALICE), {totalStudyTime: 10 * MINUTE});
        await updateDoc(doc(db, "users", ALICE, "pots", "pot1"), {potTotalStudyingTime: 0});
      });
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR}));
      assert.strictEqual(await potTotal("pot1"), -HOUR);
      assert.strictEqual((await user()).total, 10 * MINUTE - HOUR);
    });

    it("학습 시간을 따로 줄이거나 표시 필드만 바꿀 수 없다", async () => {
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {totalStudyTime: 0}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {potTotalStudyingTime: 0}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {lastDeletedPotId: "pot1", lastDeletedLogId: "l1"}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {lastDeletedLogId: "l1"}));
    });
  });

  describe("화분 삭제", () => {
    it("미완료 화분을 지우면 저장된 학습 시간만큼 유저 학습 시간이 줄고 완료 수는 그대로다", async () => {
      await assertSucceeds(deletePotBatch(asAlice(), {time: 3 * HOUR}));
      assert.strictEqual(await exists("users", ALICE, "pots", "pot1"), false);
      assert.deepStrictEqual(await user(), {total: 30 * MINUTE, completed: 1});
    });

    it("완료된 화분을 지우면 완료 수도 1 줄어든다", async () => {
      await assertSucceeds(deletePotBatch(asAlice(), {potId: "done", time: 30 * MINUTE, completed: true}));
      assert.deepStrictEqual(await user(), {total: 3 * HOUR, completed: 0});
    });

    it("완료된 화분인데 완료 수를 줄이지 않거나, 미완료 화분인데 완료 수를 줄이면 거부", async () => {
      await assertFails(deletePotBatch(asAlice(), {potId: "done", time: 30 * MINUTE}));
      await assertFails(deletePotBatch(asAlice(), {potId: "done", time: 30 * MINUTE, countDelta: -2}));
      await assertFails(deletePotBatch(asAlice(), {time: 3 * HOUR, completed: true}));
      await assertFails(deletePotBatch(asAlice(), {time: 3 * HOUR, countDelta: 1}));
    });

    it("같은 삭제가 두 번 도착하면(연타·재전송) 두 번째는 거부되어 학습 시간·완료 수는 1번만 줄어든다", async () => {
      await assertSucceeds(deletePotBatch(asAlice(), {potId: "done", time: 30 * MINUTE, completed: true}));
      await assertFails(deletePotBatch(asAlice(), {potId: "done", time: 30 * MINUTE, completed: true}));
      assert.deepStrictEqual(await user(), {total: 3 * HOUR, completed: 0});
    });

    it("차감량이 저장된 화분 학습 시간과 다르면 거부 (음수 차감 포함)", async () => {
      await assertFails(deletePotBatch(asAlice(), {time: HOUR}));
      await assertFails(deletePotBatch(asAlice(), {time: 0}));
      await assertFails(deletePotBatch(asAlice(), {time: -3 * HOUR}));
    });

    it("화분 삭제·유저 차감 중 하나라도 빠지면 거부", async () => {
      await assertFails(deletePotBatch(asAlice(), {time: 3 * HOUR, skipUser: true}));
      await assertFails(deletePotBatch(asAlice(), {time: 3 * HOUR, skipPot: true}));
      await assertFails(deleteDoc(doc(asAlice(), "users", ALICE, "pots", "pot1")));
    });

    it("표시 필드가 지우는 화분과 다르거나 기록 ID가 비어 있지 않으면 거부", async () => {
      await assertFails(deletePotBatch(asAlice(), {time: 3 * HOUR, markedPotId: "done"}));
      await assertFails(deletePotBatch(asAlice(), {time: 3 * HOUR, markedLogId: "l1"}));
    });

    it("기록을 지운 직후 같은 화분을 지울 수 있다 (줄어든 학습 시간 기준)", async () => {
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR}));
      await assertSucceeds(deletePotBatch(asAlice(), {time: 2 * HOUR}));
      assert.deepStrictEqual(await user(), {total: 30 * MINUTE, completed: 1});
    });

    it("학습 시간 필드가 없는 예전 화분은 0으로 보고 지운다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE, "pots", "old"), {name: "예전 화분"});
      });
      await assertSucceeds(deletePotBatch(asAlice(), {potId: "old", time: 0}));
      assert.strictEqual((await user()).total, 3 * HOUR + 30 * MINUTE);
    });

    it("없는 화분, 다른 사람 화분은 지울 수 없다", async () => {
      await assertFails(deletePotBatch(asAlice(), {potId: "none", time: 0}));
      await assertFails(deletePotBatch(asBob(), {uid: ALICE, time: 3 * HOUR}));
    });

    it("화분 삭제 후 남은 기록은 앱이 지울 수 없다 (트리거가 정리)", async () => {
      await assertSucceeds(deletePotBatch(asAlice(), {time: 3 * HOUR}));
      await assertFails(deleteStudyLogBatch(asAlice(), {logId: "l1", time: HOUR}));
    });
  });

  describe("그 외 쓰기", () => {
    it("화분 이름 변경·레벨업, 유저의 일반 필드 쓰기는 그대로 허용", async () => {
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {name: "새 이름"}));
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {potLevel: 1}));
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE), {"item.heart": increment(-1), "lastSelectedPotId": "pot1"}));
    });

    it("새 화분은 미완료로, 새 유저는 완료 수 0으로만 만들 수 있고 표시 필드는 넣을 수 없다", async () => {
      await assertSucceeds(setDoc(doc(asAlice(), "users", ALICE, "pots", "new"), {
        name: "새 화분", potTotalStudyingTime: 0, lastStudiedAt: null, isCompleted: false, completedAt: null,
      }));
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "new2"), {name: "새 화분", isCompleted: true}));
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "new3"), {name: "새 화분", completedAt: Timestamp.now()}));
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "new4"), {name: "새 화분", lastDeletedLogId: "l1"}));

      await testEnv.withSecurityRulesDisabled(async (context) => {
        await deleteDoc(doc(context.firestore(), "users", BOB));
      });
      await assertFails(setDoc(doc(asBob(), "users", BOB), {nickname: "", completedPotsCount: 1}));
      await assertFails(setDoc(doc(asBob(), "users", BOB), {nickname: "", lastCompletedPotId: "p"}));
      await assertFails(setDoc(doc(asBob(), "users", BOB), {nickname: "", lastDeletedPotId: "p"}));
      await assertFails(setDoc(doc(asBob(), "users", BOB), {nickname: "", lastDeletedLogId: "l"}));
      await assertSucceeds(setDoc(doc(asBob(), "users", BOB), {nickname: "", totalStudyTime: 0}));
    });
  });
});
