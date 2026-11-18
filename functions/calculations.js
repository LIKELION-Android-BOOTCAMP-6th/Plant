// Firestore를 읽거나 쓰지 않고 값만 계산하는 함수 모음 (#620)
// - index.js의 함수들이 require해서 사용한다. 여기 있는 함수는 같은 입력이면 항상 같은 결과를 돌려준다
// - 에뮬레이터 없이 유닛 테스트(test/unit/)로 바로 확인할 수 있도록 index.js에서 분리했다
//   (index.js는 불러오는 순간 initializeApp()과 함수 등록을 하므로 테스트에서 직접 require하지 않는다)
const {Timestamp} = require("firebase-admin/firestore");

const HOUR_MS = 60 * 60 * 1000;

// ---------------------------------------------------------------------------
// 학습 보상 (#453)
// ---------------------------------------------------------------------------

// 보상 티어 표 (원본은 서버. 앱은 "1시간 이상이면 보상" 같은 표시용 기준만 가진다)
// items: users.item의 필드 키, box: 보너스 박스 개수. 시간이 긴 티어부터 확인한다
const STUDY_REWARD_TIERS = [
  {
    hours: 9,
    items: ["heart", "sun", "water", "fertilizer", "nutrient"],
    box: 5,
  },
  {hours: 7, items: ["heart", "sun", "water", "fertilizer"], box: 4},
  {hours: 5, items: ["heart", "sun", "water"], box: 3},
  {hours: 3, items: ["heart", "sun"], box: 2},
  {hours: 1, items: ["heart"], box: 1},
];

/**
 * 학습 시간(ms)에 맞는 보상을 찾는다. 1시간 미만이면 null.
 * @param {*} studyingTime 기록의 studyingTime
 * @return {{items: string[], box: number} | null}
 */
function computeStudyReward(studyingTime) {
  if (typeof studyingTime !== "number" || !Number.isFinite(studyingTime)) {
    return null;
  }
  const hours = Math.floor(studyingTime / HOUR_MS);
  const tier = STUDY_REWARD_TIERS.find((t) => hours >= t.hours);
  return tier ? {items: tier.items, box: tier.box} : null;
}

// ---------------------------------------------------------------------------
// 보너스 박스 열기 (#454)
// ---------------------------------------------------------------------------

/**
 * 뽑힌 목록으로 users/{uid}에 더할 값(필드 경로 → 증감량)을 만든다.
 * 연 박스 수만큼 item.box를 줄이고, 순증감이 0인 필드는 넣지 않는다.
 * @param {Object[]} drops BONUS_BOX_DROP_TABLE의 항목 목록
 * @param {number} openedCount
 * @return {Object<string, number>} 예: {"item.box": -2, "gold": 600}
 */
function computeBonusBoxDeltas(drops, openedCount) {
  const itemCounts = {box: -openedCount};
  let gold = 0;
  drops.forEach((drop) => {
    if (drop.field) {
      itemCounts[drop.field] = (itemCounts[drop.field] || 0) + 1;
    }
    if (drop.gold) gold += drop.gold;
  });

  const deltas = {};
  Object.entries(itemCounts).forEach(([field, count]) => {
    // 박스를 연 만큼 다시 뽑으면 순증감 0 → 쓰지 않는다
    if (count !== 0) deltas[`item.${field}`] = count;
  });
  if (gold > 0) deltas.gold = gold;
  return deltas;
}

// ---------------------------------------------------------------------------
// 출석 체크 (#531)
// ---------------------------------------------------------------------------

const MONTHLY_ATTENDANCE_LIMIT = 28;
const KST_OFFSET_MS = 9 * HOUR_MS;
const ATTENDANCE_STATUS = {
  SUCCESS: "SUCCESS",
  ALREADY_CHECKED: "ALREADY_CHECKED",
  MONTH_COMPLETED: "MONTH_COMPLETED",
};

/**
 * 시각(ms)을 KST 날짜 키("yyyy-MM-dd")로 바꾼다. 실행 환경의 시간대와 무관하다.
 * @param {number} nowMs
 * @return {string}
 */
function toKstDateKey(nowMs) {
  return new Date(nowMs + KST_OFFSET_MS).toISOString().slice(0, 10);
}

/**
 * 저장된 출석 정보와 오늘 날짜 키로 이번 출석 결과를 정한다.
 * @param {*} monthCheck users.dailyCheckThisMonth 값
 * @param {string} todayKey toKstDateKey 결과
 * @return {Object} {status, newCount} (newCount는 SUCCESS일 때만)
 */
function decideAttendance(monthCheck, todayKey) {
  const saved = monthCheck || {};
  const lastKey = typeof saved.lastCheckedDate === "string" ?
    saved.lastCheckedDate : null;
  const isSameMonth = lastKey !== null &&
    lastKey.slice(0, 7) === todayKey.slice(0, 7);
  const savedCount = Number.isInteger(saved.count) ? saved.count : 0;
  // 달이 바뀌었으면 지난달 횟수는 보지 않는다 (이번 출석이 1회차)
  const count = isSameMonth ? savedCount : 0;

  if (count >= MONTHLY_ATTENDANCE_LIMIT) {
    return {status: ATTENDANCE_STATUS.MONTH_COMPLETED};
  }
  if (lastKey === todayKey) {
    return {status: ATTENDANCE_STATUS.ALREADY_CHECKED};
  }
  return {status: ATTENDANCE_STATUS.SUCCESS, newCount: count + 1};
}

// ---------------------------------------------------------------------------
// 프로필 이미지 해금 레벨 (#598)
// ---------------------------------------------------------------------------

// 화분 최고 레벨. firestore.rules의 maxPotLevel()과
// 앱 domain/model/LevelUpCondition.kt의 PotLevelConditions.MAX_LEVEL과 같은 값이어야 한다
const MAX_POT_LEVEL = 6;

/**
 * 저장된 해금 레벨과 화분의 새 레벨로 저장할 해금 레벨을 정한다.
 * 새 값 = min(max(기존 값, 새 레벨), 최고 레벨) → 값은 올라가기만 한다.
 * @param {*} stored users.unlockedProfileLevel 값 (없거나 정수가 아니면 0으로 본다)
 * @param {*} afterLevel 수정 후 potLevel
 * @return {number | null} 저장할 값. 정수가 아닌 레벨이거나 값이 그대로면 null(쓰지 않음)
 */
function computeUnlockedProfileLevel(stored, afterLevel) {
  if (!Number.isInteger(afterLevel)) return null;
  // 필드가 없거나 정수가 아니면 0으로 본다
  const current = Number.isInteger(stored) ? stored : 0;
  const next = Math.min(Math.max(current, afterLevel), MAX_POT_LEVEL);
  return stored === next ? null : next;
}

// ---------------------------------------------------------------------------
// 오래된 "공부 중" 표시(studying) 정리 (#525)
// ---------------------------------------------------------------------------

/**
 * studying 문서가 오래됐는지 (updatedAt이 없거나 Timestamp가 아니어도 오래된 것으로 본다).
 * @param {*} updatedAt 문서의 updatedAt 값
 * @param {number} cutoffMs 이 시각(ms)보다 이전이면 오래된 것
 * @return {boolean}
 */
function isStaleStudying(updatedAt, cutoffMs) {
  if (!(updatedAt instanceof Timestamp)) return true;
  return updatedAt.toMillis() < cutoffMs;
}

module.exports = {
  STUDY_REWARD_TIERS,
  computeStudyReward,
  computeBonusBoxDeltas,
  MONTHLY_ATTENDANCE_LIMIT,
  ATTENDANCE_STATUS,
  toKstDateKey,
  decideAttendance,
  MAX_POT_LEVEL,
  computeUnlockedProfileLevel,
  isStaleStudying,
};
