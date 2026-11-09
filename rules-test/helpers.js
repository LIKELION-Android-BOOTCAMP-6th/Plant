// 규칙 테스트 공통 준비 (#452)
// - demo- 로 시작하는 프로젝트 ID는 에뮬레이터 전용이라 실제 서버에 절대 접속하지 않는다
// - 규칙은 레포 루트의 firestore.rules(실제 배포되는 파일)를 그대로 읽는다
const fs = require("node:fs");
const path = require("node:path");
const {initializeTestEnvironment} = require("@firebase/rules-unit-testing");
const {
  doc, increment, serverTimestamp, setLogLevel, writeBatch,
} = require("firebase/firestore");

// 거부가 정상인 테스트마다 SDK가 PERMISSION_DENIED 경고를 찍어 결과가 묻히므로 끈다
setLogLevel("silent");

const PROJECT_ID = "demo-plant";
const RULES_PATH = path.resolve(__dirname, "../firestore.rules");

// 테스트에 쓰는 사용자
const ALICE = "alice";
const BOB = "bob";

async function createTestEnv() {
  return initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync(RULES_PATH, "utf8"),
      // emulators:exec가 FIRESTORE_EMULATOR_HOST를 넣어 주므로 host/port는 생략
    },
  });
}

/** 규칙을 끈 상태로 테스트 데이터를 미리 넣는다 (관리자 권한처럼 동작) */
async function seed(testEnv, writer) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await writer(context.firestore());
  });
}

// ---------------------------------------------------------------------------
// 화분 완료·학습 기록 삭제·화분 삭제 batch (#455)
// 앱과 같은 쓰기를 만든다. 옵션으로 일부 값만 바꿔 거부 케이스를 만든다
// - 화분 완료: PotRemoteDataSourceImpl.completePot
// - 기록 삭제: StudyLogRemoteDataSourceImpl.deleteStudyLog
// - 화분 삭제: PotRemoteDataSourceImpl.deletePot
// ---------------------------------------------------------------------------

/**
 * 화분 완료 batch
 * - countDelta: completedPotsCount 증가량 (기본 1)
 * - markedPotId: users.lastCompletedPotId (기본 potId)
 * - pot: 화분 문서 필드 덮어쓰기
 * - skipPot / skipUser: 화분·유저 갱신을 빼고 보냄
 */
function completePotBatch(db, {
  uid = ALICE, potId = "pot1", countDelta = 1, markedPotId, pot = {},
  skipPot = false, skipUser = false,
} = {}) {
  const batch = writeBatch(db);
  if (!skipPot) {
    batch.update(doc(db, "users", uid, "pots", potId), {
      isCompleted: true,
      completedAt: serverTimestamp(),
      ...pot,
    });
  }
  if (!skipUser) {
    batch.update(doc(db, "users", uid), {
      completedPotsCount: increment(countDelta),
      lastCompletedPotId: markedPotId ?? potId,
    });
  }
  return batch.commit();
}

/**
 * 학습 기록 삭제 batch
 * - time: 앱이 보내는 차감량 (기본은 저장된 학습 시간과 같다고 가정한 값 — 호출부에서 넘긴다)
 * - potDelta / userDelta: 화분·유저 차감량 (기본 time)
 * - markedLogId / markedPotId: 표시 필드 값 (기본 logId / potId)
 * - skipLog / skipPot / skipUser: 기록 삭제·화분 갱신·유저 갱신을 빼고 보냄
 */
function deleteStudyLogBatch(db, {
  uid = ALICE, potId = "pot1", logId, time, potDelta, userDelta,
  markedLogId, markedPotId, skipLog = false, skipPot = false, skipUser = false,
} = {}) {
  const batch = writeBatch(db);
  const potRef = doc(db, "users", uid, "pots", potId);
  if (!skipLog) batch.delete(doc(potRef, "logs", logId));
  if (!skipPot) {
    batch.update(potRef, {
      potTotalStudyingTime: increment(-(potDelta ?? time)),
      lastDeletedLogId: markedLogId ?? logId,
    });
  }
  if (!skipUser) {
    batch.update(doc(db, "users", uid), {
      totalStudyTime: increment(-(userDelta ?? time)),
      lastDeletedPotId: markedPotId ?? potId,
      lastDeletedLogId: markedLogId ?? logId,
    });
  }
  return batch.commit();
}

/**
 * 화분 삭제 batch
 * - time: 유저 학습 시간 차감량 (화분의 potTotalStudyingTime)
 * - completed: true면 completedPotsCount -1을 함께 보냄 (앱은 캐시상 완료된 화분일 때 보냄)
 * - countDelta: completedPotsCount 증가량을 직접 지정 (completed보다 우선)
 * - markedPotId / markedLogId: 표시 필드 값 (기본 potId / '')
 * - skipPot / skipUser: 화분 삭제·유저 갱신을 빼고 보냄
 */
function deletePotBatch(db, {
  uid = ALICE, potId = "pot1", time, completed = false, countDelta,
  markedPotId, markedLogId = "", skipPot = false, skipUser = false,
} = {}) {
  const batch = writeBatch(db);
  if (!skipPot) batch.delete(doc(db, "users", uid, "pots", potId));
  if (!skipUser) {
    const update = {
      totalStudyTime: increment(-time),
      lastDeletedPotId: markedPotId ?? potId,
      lastDeletedLogId: markedLogId,
    };
    const delta = countDelta ?? (completed ? -1 : undefined);
    if (delta !== undefined) update.completedPotsCount = increment(delta);
    batch.update(doc(db, "users", uid), update);
  }
  return batch.commit();
}

module.exports = {
  ALICE, BOB, createTestEnv, seed,
  completePotBatch, deleteStudyLogBatch, deletePotBatch,
};
