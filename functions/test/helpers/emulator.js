// 에뮬레이터 통합 테스트 공통 준비 (#620)
// - npm test(firebase emulators:exec)로 실행된다. 에뮬레이터 주소는 emulators:exec가 넣어 주는
//   환경 변수(FIRESTORE_EMULATOR_HOST 등)를 쓰고, 없으면 firebase.json의 포트를 쓴다
// - 프로젝트 ID는 demo-plant. demo-로 시작하는 ID는 에뮬레이터 전용이라 실제 서버에 접속하지 않는다
// - 테스트 쪽 Admin SDK는 함수와 같은 Firestore 에뮬레이터에 붙으므로 보안 규칙 없이 읽고 쓴다

// Admin SDK(google-auth-library)가 Google 클라우드 안에서 실행 중인지 메타데이터 서버에 묻지 않게 한다.
// 물으면 응답을 기다리느라 테스트 파일마다 첫 요청이 늦어지고 MetadataLookupWarning이 찍힌다
// (Windows PC에서 파일별 첫 테스트 시간 측정: 넣기 전 9~23초 → 넣은 뒤 4~11초, 경고 없음)
// firebase-admin을 require하기 전에 넣어야 한다
if (!process.env.METADATA_SERVER_DETECTION) {
  process.env.METADATA_SERVER_DETECTION = "none";
}
const {randomUUID} = require("node:crypto");
const {initializeApp, getApps} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
const {getFirestore} = require("firebase-admin/firestore");

const PROJECT_ID = "demo-plant";
// index.js의 setGlobalOptions region과 같아야 Callable 주소가 맞는다
const REGION = "asia-northeast3";

const FIRESTORE_HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";
const AUTH_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || "127.0.0.1:9099";
const FUNCTIONS_HOST =
  process.env.FUNCTIONS_EMULATOR_HOST || "127.0.0.1:5001";

// 에뮬레이터 밖(npm run test:unit 등)에서 실수로 불러도 실서버에 붙지 않도록 주소를 직접 넣는다
process.env.FIRESTORE_EMULATOR_HOST = FIRESTORE_HOST;
process.env.FIREBASE_AUTH_EMULATOR_HOST = AUTH_HOST;

const app = getApps()[0] || initializeApp({projectId: PROJECT_ID});
const db = getFirestore(app);
const auth = getAuth(app);

/**
 * 테스트마다 겹치지 않는 ID를 만든다.
 * 앞 테스트의 트리거가 늦게 끝나도 다음 테스트 문서에 영향을 주지 않게 uid·문서 ID에 쓴다.
 * @param {string} prefix
 * @return {string}
 */
function uniqueId(prefix) {
  return `${prefix}-${randomUUID().slice(0, 8)}`;
}

/**
 * Firestore 에뮬레이터의 모든 문서를 지운다.
 * @return {Promise<void>}
 */
async function clearFirestore() {
  const url = `http://${FIRESTORE_HOST}/emulator/v1/projects/${PROJECT_ID}` +
    "/databases/(default)/documents";
  const response = await fetch(url, {method: "DELETE"});
  if (!response.ok) {
    throw new Error(`Firestore 초기화 실패: ${response.status}`);
  }
}

/**
 * Auth 에뮬레이터의 모든 계정을 지운다.
 * @return {Promise<void>}
 */
async function clearAuth() {
  const url = `http://${AUTH_HOST}/emulator/v1/projects/${PROJECT_ID}/accounts`;
  const response = await fetch(url, {method: "DELETE"});
  if (!response.ok) {
    throw new Error(`Auth 초기화 실패: ${response.status}`);
  }
}

// ---------------------------------------------------------------------------
// 트리거 결과 대기
// ---------------------------------------------------------------------------
// 트리거는 문서를 쓴 뒤 따로(비동기로) 실행되므로, 쓰고 바로 읽으면 아직 반영되지 않았을 수 있다.
// → 조건이 맞을 때까지 일정 간격으로 다시 확인한다.
// 처음 실행되는 함수는 에뮬레이터가 코드를 불러오느라 몇 초 걸릴 수 있어 제한 시간을 넉넉히 둔다.
// ⚠️ "아무것도 바뀌지 않음"(두 번째 실행은 지급하지 않음 등)은 이 방식으로 끝을 알 수 없다.
//    그런 테스트는 트리거가 끝난 표시(rewardStatus 등)를 먼저 기다린 뒤 값을 비교한다.
const DEFAULT_WAIT_TIMEOUT_MS = 20000;
const DEFAULT_WAIT_INTERVAL_MS = 200;

/**
 * check()가 참인 값(truthy)을 돌려줄 때까지 기다려 그 값을 돌려준다.
 * 제한 시간 안에 맞지 않으면 마지막으로 본 값을 담아 실패한다.
 * 0·""도 "아직"으로 보므로, 개수·값 비교는 boolean으로 돌려준다
 * (예: async () => (await query.get()).size === 2).
 * @template T
 * @param {function(): Promise<T>} check
 * @param {Object} [options]
 * @param {number} [options.timeoutMs]
 * @param {number} [options.intervalMs]
 * @param {string} [options.message] 실패 메시지 앞에 붙일 설명
 * @param {function(*): string} [options.describe] 실패 메시지에 넣을 마지막 상태
 * @return {Promise<T>}
 */
async function waitFor(check, {
  timeoutMs = DEFAULT_WAIT_TIMEOUT_MS,
  intervalMs = DEFAULT_WAIT_INTERVAL_MS,
  message = "조건",
  describe = (last) => `마지막 값: ${JSON.stringify(last)}`,
} = {}) {
  const deadline = Date.now() + timeoutMs;
  let last;
  for (;;) {
    last = await check();
    if (last) return last;
    if (Date.now() >= deadline) {
      throw new Error(
          `${message}이(가) ${timeoutMs}ms 안에 맞지 않음 (${describe(last)})`,
      );
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }
}

/**
 * 문서가 predicate(데이터)를 만족할 때까지 기다려 그 데이터를 돌려준다.
 * 문서가 없으면 predicate에 undefined를 넘긴다 (삭제를 기다릴 때: (data) => data === undefined).
 * @param {FirebaseFirestore.DocumentReference} ref
 * @param {function((Object|undefined)): boolean} predicate
 * @param {Object} [options] waitFor의 옵션
 * @return {Promise<Object | undefined>}
 */
async function waitForDoc(ref, predicate, options = {}) {
  let data;
  await waitFor(async () => {
    data = (await ref.get()).data();
    return predicate(data);
  }, {
    message: `${ref.path} 문서`,
    describe: () => `마지막 문서: ${JSON.stringify(data)}`,
    ...options,
  });
  return data;
}

// ---------------------------------------------------------------------------
// Callable 호출
// ---------------------------------------------------------------------------
// 앱의 Functions SDK처럼 HTTP로 호출한다: POST {data} → 성공 {result} / 실패 {error}
// 로그인 상태는 Auth 에뮬레이터에서 받은 ID 토큰을 Authorization 헤더에 넣어 만든다

/**
 * Auth 에뮬레이터에서 uid로 로그인한 ID 토큰을 받는다. 계정이 없으면 새로 만들어진다.
 * 토큰의 auth_time은 이 호출 시각이다 (deleteAccount의 재인증 5분 조건 확인에 쓴다).
 * @param {string} uid
 * @param {Object} [claims] 토큰에 넣을 추가 값
 * @return {Promise<string>}
 */
async function signIn(uid, claims) {
  // Auth 에뮬레이터를 쓸 때 Admin SDK는 서명 없는 커스텀 토큰을 만든다 (서비스 계정 불필요)
  const customToken = await auth.createCustomToken(uid, claims);
  const url = `http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/` +
    "accounts:signInWithCustomToken?key=fake-api-key";
  const response = await fetch(url, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({token: customToken, returnSecureToken: true}),
  });
  const body = await response.json();
  if (!response.ok) {
    throw new Error(`에뮬레이터 로그인 실패: ${JSON.stringify(body)}`);
  }
  return body.idToken;
}

/**
 * Callable 함수가 HttpsError로 실패했을 때 던지는 오류.
 * code는 HttpsError를 만들 때 쓴 값과 같은 형태("not-found", "unauthenticated" 등)다.
 */
class CallableError extends Error {
  /**
   * @param {string} code
   * @param {string} message
   * @param {*} details
   */
  constructor(code, message, details) {
    super(`${code}: ${message}`);
    this.name = "CallableError";
    this.code = code;
    this.details = details;
  }
}

/**
 * Callable 함수를 호출해 result를 돌려준다. 실패하면 CallableError를 던진다.
 * ⚠️ uid로 부르면 호출할 때마다 signIn을 하고, Auth 에뮬레이터는 계정이 없으면 새로 만든다.
 *    계정 삭제를 확인하거나 삭제 뒤 다시 부르는 테스트(deleteAccount 등)는
 *    signIn으로 한 번 받은 idToken을 재사용한다 (uid로 다시 부르면 지운 계정이 되살아남).
 * @param {string} name index.js의 exports 이름 (예: "checkAttendance")
 * @param {*} [data] request.data로 받을 값
 * @param {Object} [options]
 * @param {string} [options.uid] 이 uid로 로그인해 호출. 없으면 로그인하지 않은 호출
 * @param {string} [options.idToken] 이미 받은 토큰으로 호출 (uid보다 우선)
 * @return {Promise<*>}
 */
async function callFunction(name, data = {}, {uid, idToken} = {}) {
  const token = idToken || (uid ? await signIn(uid) : null);
  const headers = {"Content-Type": "application/json"};
  if (token) headers.Authorization = `Bearer ${token}`;

  const url = `http://${FUNCTIONS_HOST}/${PROJECT_ID}/${REGION}/${name}`;
  const response = await fetch(url, {
    method: "POST",
    headers,
    body: JSON.stringify({data}),
  });
  const text = await response.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch (error) {
    // 함수 이름·지역이 틀리면 JSON이 아닌 응답이 온다
    throw new Error(`${name} 호출 응답이 JSON이 아님 (${response.status}): ${text}`);
  }
  if (body.error) {
    // status는 "NOT_FOUND"처럼 오므로 HttpsError 코드 형태("not-found")로 바꾼다
    const code = String(body.error.status).toLowerCase().replace(/_/g, "-");
    throw new CallableError(code, body.error.message, body.error.details);
  }
  return body.result;
}

module.exports = {
  PROJECT_ID,
  db,
  auth,
  uniqueId,
  clearFirestore,
  clearAuth,
  waitFor,
  waitForDoc,
  signIn,
  CallableError,
  callFunction,
};
