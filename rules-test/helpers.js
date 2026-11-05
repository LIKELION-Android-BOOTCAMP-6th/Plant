// 규칙 테스트 공통 준비 (#452)
// - demo- 로 시작하는 프로젝트 ID는 에뮬레이터 전용이라 실제 서버에 절대 접속하지 않는다
// - 규칙은 레포 루트의 firestore.rules(실제 배포되는 파일)를 그대로 읽는다
const fs = require("node:fs");
const path = require("node:path");
const {initializeTestEnvironment} = require("@firebase/rules-unit-testing");
const {setLogLevel} = require("firebase/firestore");

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

module.exports = {ALICE, BOB, createTestEnv, seed};
