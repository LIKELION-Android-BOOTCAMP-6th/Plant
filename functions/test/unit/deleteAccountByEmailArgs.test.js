// 운영자 스크립트의 인자·접속 대상 확인 유닛 테스트 (#778)
// 에뮬레이터 없이 실행된다: npm --prefix functions run test:unit
const {describe, it} = require("node:test");
const assert = require("node:assert");
const {
  describeTarget,
  parseArgs,
} = require("../../scripts/deleteAccountByEmail");

describe("parseArgs", () => {
  it("프로젝트·이메일을 읽고 앞뒤 공백을 지운다", () => {
    assert.deepStrictEqual(
        parseArgs(["--project", "demo-plant", "--email", " a@example.com "]),
        {project: "demo-plant", email: "a@example.com", dryRun: false},
    );
  });

  it("--dry-run은 순서와 상관없이 읽는다", () => {
    assert.deepStrictEqual(
        parseArgs(["--dry-run", "--email", "a@example.com", "--project", "p"]),
        {project: "p", email: "a@example.com", dryRun: true},
    );
  });

  it("프로젝트나 이메일이 없으면 실패한다", () => {
    assert.throws(() => parseArgs(["--email", "a@example.com"]), /--project/);
    assert.throws(() => parseArgs(["--project", "p"]), /--email/);
    assert.throws(() => parseArgs([]), /--project/);
  });

  it("값이 빠지거나 다음 인자가 값 자리에 오면 실패한다", () => {
    assert.throws(() => parseArgs(["--project", "p", "--email"]), /값이 없습니다/);
    assert.throws(
        () => parseArgs(["--project", "--email", "a@example.com"]),
        /값이 없습니다/,
    );
  });

  it("모르는 인자는 실패한다 (오타로 dry-run이 빠지는 것을 막음)", () => {
    assert.throws(
        () => parseArgs(["--project", "p", "--email", "a@b.c", "--dryrun"]),
        /알 수 없는 인자/,
    );
  });
});

describe("describeTarget", () => {
  it("에뮬레이터 주소가 둘 다 있으면 에뮬레이터", () => {
    const target = describeTarget({
      FIRESTORE_EMULATOR_HOST: "127.0.0.1:8080",
      FIREBASE_AUTH_EMULATOR_HOST: "127.0.0.1:9099",
    });
    assert.match(target, /^에뮬레이터/);
  });

  it("둘 다 없으면 운영 서버", () => {
    assert.strictEqual(describeTarget({}), "운영 서버");
  });

  it("하나만 있으면 실패한다 (Firestore와 Auth가 다른 곳을 가리킴)", () => {
    assert.throws(
        () => describeTarget({FIRESTORE_EMULATOR_HOST: "127.0.0.1:8080"}),
        /하나만/,
    );
    assert.throws(
        () => describeTarget({FIREBASE_AUTH_EMULATOR_HOST: "127.0.0.1:9099"}),
        /하나만/,
    );
  });
});
