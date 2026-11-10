// 학습 기록 저장 검증 규칙 테스트 (#453)
// 앱(StudyingRemoteDataSourceImpl.saveStudySession)과 같은 batch를 만들어 허용·거부를 확인한다
const {describe, it, before, after, beforeEach} = require("node:test");
const assert = require("node:assert");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {
  Timestamp, collectionGroup, deleteDoc, doc, getDoc, getDocs, increment, query,
  serverTimestamp, setDoc, updateDoc, where, writeBatch,
} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, deleteStudyLogBatch, seed} = require("../helpers");

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

describe("studyLogs", () => {
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
      await setDoc(doc(db, "users", ALICE), {nickname: "앨리스", totalStudyTime: 0});
      await setDoc(doc(db, "users", ALICE, "pots", "pot1"), {name: "화분", potTotalStudyingTime: 0, isCompleted: false});
      await setDoc(doc(db, "users", ALICE, "pots", "done"), {name: "완료 화분", potTotalStudyingTime: 0, isCompleted: true});
      await setDoc(doc(db, "users", BOB), {nickname: "밥", totalStudyTime: 0});
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();

  /**
   * 앱과 같은 학습 저장 batch. 일부 값만 바꿔 거부 케이스를 만든다
   * - startedAt: 시작 시각(ms). 기본은 지금으로부터 time + 1분 전 (방금 끝난 세션)
   * - potDelta / userDelta: 화분·유저 학습 시간 증가량 (기본 time)
   * - endedAt: users.lastStudyEndedAt (기본 startedAt + time)
   * - log: 기록 문서 필드 덮어쓰기
   * - skipPot / skipUser: 화분·유저 갱신을 빼고 보냄
   */
  function studyBatch(db, {
    uid = ALICE, potId = "pot1", sessionId = "s1", time = HOUR, startedAt,
    potDelta, userDelta, endedAt, log = {}, skipPot = false, skipUser = false,
  } = {}) {
    const start = startedAt ?? Date.now() - time - MINUTE;
    const batch = writeBatch(db);
    const userRef = doc(db, "users", uid);
    const potRef = doc(db, "users", uid, "pots", potId);
    batch.set(doc(potRef, "logs", sessionId), {
      uid,
      title: "2026년 11월 10일 10:00 ~ 11:00",
      contents: ["목표 1", "목표 2"],
      completedContents: [true, false],
      studyingTime: time,
      createAt: Timestamp.now(),
      startedAt: Timestamp.fromMillis(start),
      rewardStatus: "pending",
      rewardSeen: false,
      ...log,
    });
    if (!skipPot) {
      batch.update(potRef, {
        potTotalStudyingTime: increment(potDelta ?? time),
        lastStudiedAt: serverTimestamp(),
        lastStudySessionId: sessionId,
      });
    }
    if (!skipUser) {
      batch.update(userRef, {
        totalStudyTime: increment(userDelta ?? time),
        lastStudyEndedAt: Timestamp.fromMillis(endedAt ?? start + time),
        lastStudyPotId: potId,
        lastStudySessionId: sessionId,
      });
    }
    return batch.commit();
  }

  async function totals() {
    let result;
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      const user = await getDoc(doc(db, "users", ALICE));
      const pot = await getDoc(doc(db, "users", ALICE, "pots", "pot1"));
      result = {user: user.get("totalStudyTime"), pot: pot.get("potTotalStudyingTime")};
    });
    return result;
  }

  describe("정상", () => {
    it("기록 생성 + 화분·유저 학습 시간 누적을 batch 하나로 저장할 수 있다", async () => {
      await assertSucceeds(studyBatch(asAlice()));
      assert.deepStrictEqual(await totals(), {user: HOUR, pot: HOUR});
    });

    it("일시정지 후 재개한 세션: startedAt + 학습 시간이 실제 종료보다 일러도 통과", async () => {
      await assertSucceeds(studyBatch(asAlice(), {startedAt: Date.now() - 3 * HOUR, time: HOUR}));
    });

    it("오프라인으로 며칠 지나 동기화한 기록도 저장된다 (시작 24시간 이내 검증 없음)", async () => {
      await assertSucceeds(studyBatch(asAlice(), {startedAt: Date.now() - 5 * DAY, time: 2 * HOUR}));
    });

    it("이전 세션이 끝난 시각 이후에 시작한 세션은 이어서 저장된다", async () => {
      const first = Date.now() - 5 * HOUR;
      await assertSucceeds(studyBatch(asAlice(), {sessionId: "s1", startedAt: first, time: HOUR}));
      // 이전 세션 종료 시각과 같은 시각에 시작해도 겹침이 아니다
      await assertSucceeds(studyBatch(asAlice(), {sessionId: "s2", startedAt: first + HOUR, time: HOUR}));
      assert.deepStrictEqual(await totals(), {user: 2 * HOUR, pot: 2 * HOUR});
    });

    it("목표 없는 세션, 목표 10개(각 100자) 세션 모두 저장된다", async () => {
      await assertSucceeds(studyBatch(asAlice(), {sessionId: "s1", startedAt: Date.now() - 4 * HOUR, time: MINUTE,
        log: {contents: [], completedContents: []}}));
      await assertSucceeds(studyBatch(asAlice(), {sessionId: "s2", startedAt: Date.now() - 2 * HOUR, time: MINUTE,
        log: {contents: Array(10).fill("가".repeat(100)), completedContents: Array(10).fill(true)}}));
    });

    it("기기 시계가 서버보다 조금(5분 이내) 빨라도 저장된다", async () => {
      await assertSucceeds(studyBatch(asAlice(), {startedAt: Date.now() - HOUR + 3 * MINUTE, time: HOUR}));
    });
  });

  describe("멱등 (같은 세션 재전송)", () => {
    it("같은 sessionId로 다시 보내면 batch 전체가 거부되어 학습 시간이 한 번만 늘어난다", async () => {
      const startedAt = Date.now() - 2 * HOUR;
      await assertSucceeds(studyBatch(asAlice(), {startedAt}));
      await assertFails(studyBatch(asAlice(), {startedAt}));
      assert.deepStrictEqual(await totals(), {user: HOUR, pot: HOUR});
    });

    it("기록을 지운 뒤 같은 세션을 다시 보내도 이전 세션과 겹쳐 거부된다", async () => {
      const startedAt = Date.now() - 2 * HOUR;
      await assertSucceeds(studyBatch(asAlice(), {startedAt}));
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {logId: "s1", time: HOUR}));
      await assertFails(studyBatch(asAlice(), {startedAt}));
    });
  });

  describe("검증 실패 (거부)", () => {
    it("학습 시간이 0 이하이거나 24시간을 넘으면 거부", async () => {
      await assertFails(studyBatch(asAlice(), {time: 0}));
      await assertFails(studyBatch(asAlice(), {time: -MINUTE}));
      await assertFails(studyBatch(asAlice(), {time: DAY + 1, startedAt: Date.now() - 2 * DAY}));
    });

    it("24시간 세션은 허용", async () => {
      await assertSucceeds(studyBatch(asAlice(), {time: DAY, startedAt: Date.now() - 2 * DAY}));
    });

    it("종료 시각(startedAt + 학습 시간)이 서버 시각 + 허용 오차(5분)보다 늦으면 거부", async () => {
      await assertFails(studyBatch(asAlice(), {startedAt: Date.now() - HOUR + 10 * MINUTE, time: HOUR}));
      await assertFails(studyBatch(asAlice(), {startedAt: Date.now() + DAY, time: HOUR}));
    });

    it("이전 세션과 겹치면 거부", async () => {
      const first = Date.now() - 5 * HOUR;
      await assertSucceeds(studyBatch(asAlice(), {sessionId: "s1", startedAt: first, time: 2 * HOUR}));
      await assertFails(studyBatch(asAlice(), {sessionId: "s2", startedAt: first + HOUR, time: HOUR}));
    });

    it("lastStudyEndedAt이 startedAt + 학습 시간과 다르면 거부", async () => {
      const startedAt = Date.now() - 3 * HOUR;
      await assertFails(studyBatch(asAlice(), {startedAt, endedAt: startedAt}));
      await assertFails(studyBatch(asAlice(), {startedAt, endedAt: startedAt + 2 * HOUR}));
    });

    it("화분·유저 학습 시간 증가량이 기록의 학습 시간과 다르면 거부", async () => {
      await assertFails(studyBatch(asAlice(), {potDelta: 2 * HOUR}));
      await assertFails(studyBatch(asAlice(), {userDelta: 2 * HOUR}));
      await assertFails(studyBatch(asAlice(), {potDelta: 0}));
    });

    it("화분·유저 갱신 없이 기록만 만들거나, 기록 없이 시간만 늘리면 거부", async () => {
      await assertFails(studyBatch(asAlice(), {skipPot: true}));
      await assertFails(studyBatch(asAlice(), {skipUser: true}));
      await assertFails(studyBatch(asAlice(), {skipPot: true, skipUser: true}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {totalStudyTime: increment(HOUR)}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {potTotalStudyingTime: increment(HOUR)}));
    });

    it("없는 화분, 완료된 화분, 다른 사람 화분에는 저장할 수 없다", async () => {
      await assertFails(studyBatch(asAlice(), {potId: "missing"}));
      await assertFails(studyBatch(asAlice(), {potId: "done"}));
      await assertFails(studyBatch(asBob(), {uid: ALICE}));
      assert.deepStrictEqual(await totals(), {user: 0, pot: 0});
    });

    it("앱이 보상 필드를 넣어 만들면 거부", async () => {
      await assertFails(studyBatch(asAlice(), {log: {rewardStatus: "granted"}}));
      await assertFails(studyBatch(asAlice(), {log: {reward: {items: ["heart"], box: 1}}}));
      await assertFails(studyBatch(asAlice(), {log: {rewardSeen: true}}));
    });

    it("필수 필드가 빠지거나 uid가 다르면 거부", async () => {
      await assertFails(studyBatch(asAlice(), {log: {uid: BOB}}));
      await assertFails(studyBatch(asAlice(), {log: {rewardStatus: null}}));
      await assertFails(studyBatch(asAlice(), {log: {startedAt: Date.now()}}));
    });

    it("제목·목표 길이와 개수가 넘치거나 완료 상태 개수가 다르면 거부", async () => {
      await assertFails(studyBatch(asAlice(), {log: {title: "가".repeat(101)}}));
      await assertFails(studyBatch(asAlice(), {log: {contents: Array(11).fill("목표"), completedContents: Array(11).fill(false)}}));
      await assertFails(studyBatch(asAlice(), {log: {contents: ["가".repeat(101)], completedContents: [false]}}));
      await assertFails(studyBatch(asAlice(), {log: {contents: ["목표 1", "목표 2"], completedContents: [true]}}));
      await assertFails(studyBatch(asAlice(), {log: {contents: ["목표 1"], completedContents: ["true"]}}));
    });
  });

  describe("학습 저장이 아닌 쓰기", () => {
    // 차감량 검증(저장된 값과 다르면 거부 등)은 potCompleteDelete.test.js (#455)
    it("저장한 기록은 기록 삭제 batch(기록 삭제 + 화분·유저 학습 시간 감소)로 지울 수 있다", async () => {
      await assertSucceeds(studyBatch(asAlice(), {startedAt: Date.now() - 2 * HOUR}));
      await assertSucceeds(deleteStudyLogBatch(asAlice(), {logId: "s1", time: HOUR}));
      assert.deepStrictEqual(await totals(), {user: 0, pot: 0});
    });

    it("학습 저장 필드(lastStudyEndedAt 등)는 따로 바꿀 수 없다", async () => {
      await assertSucceeds(studyBatch(asAlice(), {startedAt: Date.now() - 2 * HOUR}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {lastStudyEndedAt: Timestamp.fromMillis(0)}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE), {lastStudySessionId: "other"}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {lastStudySessionId: "other"}));
      await assertFails(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {lastStudiedAt: serverTimestamp()}));
    });

    // 화분 완료는 완료 수와 함께 검증한다 (potCompleteDelete.test.js, #455)
    it("화분 이름 변경·레벨업은 학습 저장 검증과 무관하게 허용", async () => {
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {name: "새 이름"}));
      await assertSucceeds(updateDoc(doc(asAlice(), "users", ALICE, "pots", "pot1"), {potLevel: 1}));
    });

    it("화분과 유저는 학습 시간 0으로만 만들 수 있다", async () => {
      await assertSucceeds(setDoc(doc(asAlice(), "users", ALICE, "pots", "pot2"), {name: "새 화분", potTotalStudyingTime: 0, lastStudiedAt: null}));
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "pot3"), {name: "새 화분", potTotalStudyingTime: HOUR}));
      await assertFails(setDoc(doc(asAlice(), "users", ALICE, "pots", "pot4"), {name: "새 화분", potTotalStudyingTime: 0, lastStudySessionId: "s1"}));
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await deleteDoc(doc(context.firestore(), "users", BOB));
      });
      await assertFails(setDoc(doc(asBob(), "users", BOB), {nickname: "", totalStudyTime: HOUR}));
      await assertFails(setDoc(doc(asBob(), "users", BOB), {nickname: "", lastStudyEndedAt: Timestamp.now()}));
      await assertSucceeds(setDoc(doc(asBob(), "users", BOB), {nickname: "", totalStudyTime: 0}));
    });
  });

  describe("보상 확인 표시 (rewardSeen)", () => {
    beforeEach(async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE, "pots", "pot1", "logs", "granted"), {
          uid: ALICE, studyingTime: HOUR, rewardStatus: "granted", reward: {items: ["heart"], box: 1}, rewardSeen: false,
        });
      });
    });
    const grantedRef = (db) => doc(db, "users", ALICE, "pots", "pot1", "logs", "granted");

    it("rewardSeen을 true로 바꾸는 것만 허용", async () => {
      await assertSucceeds(updateDoc(grantedRef(asAlice()), {rewardSeen: true}));
    });

    it("보상 필드나 다른 필드를 바꾸거나 rewardSeen을 false로 되돌릴 수 없다", async () => {
      await assertFails(updateDoc(grantedRef(asAlice()), {rewardSeen: true, rewardStatus: "pending"}));
      await assertFails(updateDoc(grantedRef(asAlice()), {reward: {items: ["nutrient"], box: 9}}));
      await assertFails(updateDoc(grantedRef(asAlice()), {studyingTime: 9 * HOUR}));
      await assertSucceeds(updateDoc(grantedRef(asAlice()), {rewardSeen: true}));
      await assertFails(updateDoc(grantedRef(asAlice()), {rewardSeen: false}));
    });

    it("다른 사람은 바꿀 수 없다", async () => {
      await assertFails(updateDoc(grantedRef(asBob()), {rewardSeen: true}));
    });

    it("쌓인 보상 조회: 본인 uid로 거른 collectionGroup 조회만 허용", async () => {
      const unseen = (db, uid) => getDocs(query(collectionGroup(db, "logs"),
          where("uid", "==", uid), where("rewardStatus", "==", "granted"), where("rewardSeen", "==", false)));
      const snapshot = await assertSucceeds(unseen(asAlice(), ALICE));
      assert.deepStrictEqual(snapshot.docs.map((d) => d.id), ["granted"]);
      await assertFails(unseen(asBob(), ALICE));
      await assertFails(getDocs(query(collectionGroup(asAlice(), "logs"), where("rewardSeen", "==", false))));
    });
  });
});
