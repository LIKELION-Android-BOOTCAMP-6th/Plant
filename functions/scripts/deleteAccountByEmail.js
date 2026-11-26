// 메일로 받은 계정 삭제 요청을 처리하는 운영자용 스크립트 (#778)
// 앱 탈퇴(deleteAccount)와 같은 함수(accountDeletion.js)로 같은 범위를 지운다.
// 절차(본인 확인·회신 포함)는 docs/ACCOUNT_DELETION_REQUEST.md를 따른다.
//
// 실행 (functions 폴더에서):
//   node scripts/deleteAccountByEmail.js \
//     --project <프로젝트ID> --email <주소> [--dry-run]
// - --dry-run: 대상 계정만 보여 주고 아무것도 지우지 않는다
// - 실제 삭제는 대상 uid를 그대로 입력해야 진행된다 (다른 값을 넣으면 취소)
// - 운영 접속: 서비스 계정 키 파일 경로를 GOOGLE_APPLICATION_CREDENTIALS에 넣고 실행한다
// - 에뮬레이터 접속: FIRESTORE_EMULATOR_HOST와 FIREBASE_AUTH_EMULATOR_HOST를 둘 다 넣는다
//   (하나만 넣으면 Firestore와 Auth가 서로 다른 곳을 가리키므로 실행하지 않는다)
//
// 종료 코드: 0 삭제 완료·dry-run, 1 오류, 2 계정 없음, 3 취소
const readline = require("node:readline/promises");
const {initializeApp} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
const {getFirestore} = require("firebase-admin/firestore");
const {deleteAuthUser, deleteUserData} = require("../accountDeletion");

const EXIT_CODE = {
  OK: 0,
  ERROR: 1,
  NOT_FOUND: 2,
  CANCELLED: 3,
};

const USAGE = "사용법: node scripts/deleteAccountByEmail.js " +
  "--project <프로젝트ID> --email <주소> [--dry-run]";

/**
 * 명령줄 인자를 읽는다. 잘못되면 예외를 던진다.
 * @param {string[]} argv process.argv.slice(2)
 * @return {{project: string, email: string, dryRun: boolean}}
 */
function parseArgs(argv) {
  const options = {project: "", email: "", dryRun: false};
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === "--dry-run") {
      options.dryRun = true;
    } else if (arg === "--project" || arg === "--email") {
      const value = argv[i + 1];
      if (!value || value.startsWith("--")) {
        throw new Error(`${arg} 다음에 값이 없습니다.\n${USAGE}`);
      }
      options[arg.slice(2)] = value.trim();
      i++;
    } else {
      throw new Error(`알 수 없는 인자: ${arg}\n${USAGE}`);
    }
  }
  if (!options.project || !options.email) {
    throw new Error(`--project와 --email은 꼭 넣어야 합니다.\n${USAGE}`);
  }
  return options;
}

/**
 * 접속 대상(운영/에뮬레이터)을 설명한다. 에뮬레이터 주소가 하나만 있으면 예외를 던진다.
 * @param {Object<string, string>} env process.env
 * @return {string}
 */
function describeTarget(env) {
  const firestore = env.FIRESTORE_EMULATOR_HOST;
  const auth = env.FIREBASE_AUTH_EMULATOR_HOST;
  if (firestore && auth) {
    return `에뮬레이터 (Firestore ${firestore}, Auth ${auth})`;
  }
  if (firestore || auth) {
    throw new Error(
        "FIRESTORE_EMULATOR_HOST와 FIREBASE_AUTH_EMULATOR_HOST 중 하나만 있습니다. " +
        "둘 다 넣거나 둘 다 지운 뒤 실행하세요.",
    );
  }
  return "운영 서버";
}

/**
 * 이메일로 계정을 찾아 확인용 정보를 모은다. 없으면 null.
 * users/{uid}가 없거나(가입을 마치지 않음) 일부 필드만 있어도 오류 없이 돌려준다.
 * @param {FirebaseFirestore.Firestore} db
 * @param {Object} auth Admin SDK의 Auth
 * @param {string} email
 * @return {Promise<Object|null>}
 */
async function findAccount(db, auth, email) {
  let user;
  try {
    user = await auth.getUserByEmail(email);
  } catch (error) {
    if (error.code === "auth/user-not-found") return null;
    throw error;
  }
  const uid = user.uid;
  const userSnapshot = await db.collection("users").doc(uid).get();
  const [pots, posts, comments] = await Promise.all([
    db.collection("users").doc(uid).collection("pots").count().get(),
    db.collection("posts").where("author.id", "==", uid).count().get(),
    db.collectionGroup("comments").where("user.uid", "==", uid).count().get(),
  ]);
  return {
    uid,
    email: user.email || "",
    providers: user.providerData.map((provider) => provider.providerId),
    createdAt: user.metadata.creationTime || "",
    lastSignInAt: user.metadata.lastSignInTime || "",
    userDocExists: userSnapshot.exists,
    nickname: userSnapshot.get("nickname") || "",
    isFirstLogin: userSnapshot.get("isFirstLogin"),
    potCount: pots.data().count,
    postCount: posts.data().count,
    commentCount: comments.data().count,
  };
}

/**
 * 확인용 정보를 사람이 읽을 수 있는 줄로 만든다.
 * @param {Object} account findAccount의 결과
 * @return {string[]}
 */
function describeAccount(account) {
  let signUp;
  if (!account.userDocExists) {
    signUp = "users 문서 없음 (가입을 마치지 않음)";
  } else if (account.isFirstLogin === false) {
    signUp = "가입 완료";
  } else {
    signUp = "users 문서는 있으나 닉네임 설정 전 (가입을 마치지 않음)";
  }
  return [
    `  uid          : ${account.uid}`,
    `  이메일       : ${account.email}`,
    `  로그인 방식  : ${account.providers.join(", ") || "(없음)"}`,
    `  계정 생성    : ${account.createdAt}`,
    `  최근 로그인  : ${account.lastSignInAt}`,
    `  가입 상태    : ${signUp}`,
    `  닉네임       : ${account.nickname || "(없음)"}`,
    `  화분 ${account.potCount}개 · 게시글 ${account.postCount}개 · ` +
      `댓글 ${account.commentCount}개`,
  ];
}

/**
 * 이메일로 찾은 계정을 앱 탈퇴와 같은 범위로 지운다.
 * - 계정이 없으면 아무것도 지우지 않고 "not-found"
 * - dryRun이면 대상만 보여 주고 "dry-run"
 * - confirm()이 돌려준 값이 uid와 다르면 지우지 않고 "cancelled"
 * - 지운 뒤 Auth 계정·users 문서가 남지 않았는지 다시 확인한다
 * 여러 번 실행해도 안전하다: 중간에 실패해도 Auth 계정은 마지막에 지우므로
 * 같은 이메일로 다시 찾아 남은 단계부터 이어서 처리된다
 * @param {Object} params
 * @param {FirebaseFirestore.Firestore} params.db
 * @param {Object} params.auth Admin SDK의 Auth
 * @param {string} params.email
 * @param {boolean} [params.dryRun]
 * @param {function(Object): Promise<string>} params.confirm 대상 계정을 받아 입력값을 돌려준다
 * @param {function(string): void} [params.print]
 * @return {Promise<{status: string, uid: (string|undefined)}>}
 */
async function deleteAccountByEmail({
  db, auth, email, dryRun = false, confirm, print = console.log,
}) {
  const account = await findAccount(db, auth, email);
  if (!account) {
    print(`계정 없음: ${email} 로 가입한 계정을 찾지 못했습니다. 아무것도 지우지 않았습니다.`);
    return {status: "not-found", uid: undefined};
  }

  print("삭제 대상 계정:");
  describeAccount(account).forEach((line) => print(line));

  if (dryRun) {
    print("dry-run: 아무것도 지우지 않았습니다.");
    return {status: "dry-run", uid: account.uid};
  }

  const answer = (await confirm(account)).trim();
  if (answer !== account.uid) {
    print("입력한 값이 uid와 달라 취소했습니다. 아무것도 지우지 않았습니다.");
    return {status: "cancelled", uid: account.uid};
  }

  await deleteUserData(db, account.uid);
  await deleteAuthUser(auth, account.uid);

  // 지운 결과 확인 (Auth 계정과 users 문서가 남아 있으면 실패로 본다)
  const userSnapshot = await db.collection("users").doc(account.uid).get();
  let authRemains = true;
  try {
    await auth.getUser(account.uid);
  } catch (error) {
    if (error.code !== "auth/user-not-found") throw error;
    authRemains = false;
  }
  if (userSnapshot.exists || authRemains) {
    throw new Error(
        `삭제 후에도 남아 있음 (users 문서: ${userSnapshot.exists}, ` +
        `Auth 계정: ${authRemains}). 같은 명령을 다시 실행하세요.`,
    );
  }
  print(`삭제 완료: ${account.uid} (Auth 계정·users 문서 없음 확인)`);
  return {status: "deleted", uid: account.uid};
}

/**
 * 터미널에서 uid 입력을 받는다.
 * @param {string} target describeTarget의 결과
 * @param {string} project
 * @return {function(Object): Promise<string>}
 */
function promptForUid(target, project) {
  return async (account) => {
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout,
    });
    try {
      return await rl.question(
          `\n[${target} / ${project}] 위 계정을 삭제합니다. 되돌릴 수 없습니다.\n` +
          `계속하려면 uid(${account.uid})를 그대로 입력하세요: `,
      );
    } finally {
      rl.close();
    }
  };
}

/**
 * 명령줄 실행 진입점.
 * @return {Promise<number>} 종료 코드
 */
async function main() {
  const options = parseArgs(process.argv.slice(2));
  const target = describeTarget(process.env);
  console.log(`접속 대상: ${target} / 프로젝트: ${options.project}`);

  const app = initializeApp({projectId: options.project});
  const result = await deleteAccountByEmail({
    db: getFirestore(app),
    auth: getAuth(app),
    email: options.email,
    dryRun: options.dryRun,
    confirm: promptForUid(target, options.project),
  });
  if (result.status === "not-found") return EXIT_CODE.NOT_FOUND;
  if (result.status === "cancelled") return EXIT_CODE.CANCELLED;
  return EXIT_CODE.OK;
}

if (require.main === module) {
  main().then(
      (code) => {
        process.exitCode = code;
      },
      (error) => {
        console.error("실패:", error.message);
        if (error.code) console.error("오류 코드:", error.code);
        process.exitCode = EXIT_CODE.ERROR;
      },
  );
}

module.exports = {
  parseArgs,
  describeTarget,
  findAccount,
  deleteAccountByEmail,
};
