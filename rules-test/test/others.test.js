// nicknames, feedbacks, studying, Tags 규칙 테스트
const {describe, it, before, after, beforeEach} = require("node:test");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {deleteDoc, doc, getDoc, runTransaction, serverTimestamp, setDoc, updateDoc, writeBatch} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, seed} = require("../helpers");

describe("기타 컬렉션", () => {
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
      await setDoc(doc(db, "nicknames", "앨리스"), {nickname: "앨리스", uid: ALICE});
      await setDoc(doc(db, "Tags", "tag1"), {name: "공부"});
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();
  const asGuest = () => testEnv.unauthenticatedContext().firestore();

  describe("nicknames", () => {
    it("빈 닉네임은 본인 uid로 선점할 수 있다", async () => {
      await assertSucceeds(setDoc(doc(asBob(), "nicknames", "밥"), {nickname: "밥", uid: BOB}));
    });

    it("이미 있는 닉네임은 덮어쓸 수 없다", async () => {
      await assertFails(setDoc(doc(asBob(), "nicknames", "앨리스"), {nickname: "앨리스", uid: BOB}));
    });

    it("다른 사람 uid로 선점하거나 문서 ID와 닉네임이 다르면 거부", async () => {
      await assertFails(setDoc(doc(asBob(), "nicknames", "밥"), {nickname: "밥", uid: ALICE}));
      await assertFails(setDoc(doc(asBob(), "nicknames", "밥"), {nickname: "다른이름", uid: BOB}));
    });

    it("소유자만 삭제할 수 있다", async () => {
      await assertFails(deleteDoc(doc(asBob(), "nicknames", "앨리스")));
      await assertSucceeds(deleteDoc(doc(asAlice(), "nicknames", "앨리스")));
    });

    // 닉네임 형식 검사 (#535) — 앱(NicknameRule)이 허용하는 닉네임은 규칙도 허용해야 한다
    // 허용·거부 값은 앱 유닛 테스트(app/src/test/.../domain/util/NicknameRuleTest.kt)와 같은 값을 쓴다. 한쪽을 바꾸면 함께 바꿀 것
    const register = (nickname) =>
      setDoc(doc(asBob(), "nicknames", nickname), {nickname, uid: BOB});

    it("정상 닉네임(공백 1칸, 키보드 자모, 이모지 조합 포함)은 선점할 수 있다", async () => {
      const allowed = [
        "새싹",
        "새 싹",
        "ㅋㅋ", // 키보드 자모 U+3131–318E
        "Plant_01",
        "🌱새싹",
        "❤️하트", // 모양 선택 문자 U+FE0F
        "1️⃣번", // 키캡 (숫자 + U+FE0F + U+20E3)
        "👍🏻좋아", // 피부색 변경 문자
        "👨‍👩‍👧가족", // 결합 문자 ZWJ U+200D
        "❤️‍🔥불꽃", // 모양 선택 문자 뒤 ZWJ
      ];
      for (const nickname of allowed) {
        await assertSucceeds(register(nickname));
      }
    });

    it("앞뒤 공백·연속 공백이 있으면 거부", async () => {
      for (const nickname of ["새싹 ", " 새싹", "새  싹", "\t새싹"]) {
        await assertFails(register(nickname));
      }
    });

    it("보이지 않는 문자·일반 공백 외 공백 문자가 있으면 거부", async () => {
      const blocked = [
        "ㅤ새싹", // 한글 채움 문자
        "ᅟ새싹", // 한글 초성 채움 문자
        "새​싹", // 폭 없는 공백
        "새⁠싹", // 단어 결합 문자
        "‮새싹", // 방향 제어 문자
        "﻿새싹", // BOM
        "새­싹", // 소프트 하이픈
        "새 싹", // 줄바꿈 없는 공백
        "새　싹", // 전각 공백
        "새⠀싹", // 점자 공백
        "새\n싹", // 제어 문자
        "새\u0007싹", // 제어 문자
        "새싹", // 사용자 정의 영역
        "새\u{F0000}싹", // 보조 사용자 정의 영역
        "새\u{E0041}싹", // 태그 문자
        "새\u{1D173}싹", // 음악 서식 문자
      ];
      for (const nickname of blocked) {
        await assertFails(register(nickname));
      }
    });

    it("분리형 한글 자모가 있으면 거부 (앱은 NFC로 완성형으로 합친 뒤 저장)", async () => {
      const blocked = [
        "새싹", // 자모로 나뉜 "새싹"
        "새ᄀ", // 혼자 있는 초성 — "새ㄱ"처럼 보임
        "새ꥠ싹", // 옛한글 초성 확장
        "새ힰ싹", // 옛한글 중성 확장
      ];
      for (const nickname of blocked) {
        await assertFails(register(nickname));
      }
    });

    // 알려진 한계: 결합 문자(ZWJ)·모양 선택 문자(VS)가 이모지 사이에 있는지는 규칙에서 판단하지 않는다
    // (막으면 앱이 허용한 이모지 닉네임이 서버에서 거부됨). 이모지가 아닌 글자 사이의 ZWJ·VS는 앱(NicknameRule)이 막는다
    it("[알려진 한계 - 앱이 막음] 이모지가 아닌 글자 사이의 ZWJ·VS는 규칙에서 통과", async () => {
      await assertSucceeds(register("새‍싹"));
      await assertSucceeds(register("새️싹"));
    });

    it("규칙 이전에 만들어진 형식 위반 닉네임 문서도 소유자는 삭제할 수 있다 (users 문서 없음)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "앨리스 "), {nickname: "앨리스 ", uid: ALICE});
      });
      await assertSucceeds(deleteDoc(doc(asAlice(), "nicknames", "앨리스 ")));
    });
  });

  // 삭제 후 내 users.nickname이 아직 그 닉네임이면 거부 — 다른 사람이 같은 닉네임을 등록하지 못하게 (#549)
  // 닉네임 변경 트랜잭션 자체(새 닉네임 생성 + 이전 닉네임 삭제 + users 수정)는 users.test.js의 updateProfile 테스트에서 함께 확인
  describe("nicknames 삭제 시 users.nickname 확인", () => {
    beforeEach(async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE), {nickname: "앨리스", profileImg: "1", unlockedProfileLevel: 3});
      });
    });

    it("users.nickname이 아직 그 닉네임이면 닉네임 문서만 지울 수 없다", async () => {
      await assertFails(deleteDoc(doc(asAlice(), "nicknames", "앨리스")));
    });

    it("같은 트랜잭션에서 users.nickname을 바꾸지 않고 이전 닉네임만 지우면 거부", async () => {
      const db = asAlice();
      await assertFails(runTransaction(db, async (txn) => {
        const userRef = doc(db, "users", ALICE);
        await txn.get(userRef);
        txn.delete(doc(db, "nicknames", "앨리스"));
        txn.update(userRef, {profileImg: "2"});
      }));
    });

    it("같은 배치에서 users.nickname을 본인 소유가 아닌 닉네임으로 바꾸며 지우는 우회는 거부 (users 규칙)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "밥"), {nickname: "밥", uid: BOB});
      });
      const db = asAlice();
      for (const nickname of ["밥", "없는이름"]) {
        const batch = writeBatch(db);
        batch.delete(doc(db, "nicknames", "앨리스"));
        batch.update(doc(db, "users", ALICE), {nickname});
        await assertFails(batch.commit());
      }
    });

    it("users 문서가 없으면 지울 수 있다 (회원탈퇴 후 남은 문서 정리)", async () => {
      await seed(testEnv, async (db) => {
        await deleteDoc(doc(db, "users", ALICE));
      });
      await assertSucceeds(deleteDoc(doc(asAlice(), "nicknames", "앨리스")));
    });

    it("데이터 어긋남: users.nickname이 다른 값이면 주인 없는 내 닉네임 문서를 정리할 수 있다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE), {nickname: "새이름", profileImg: "1"});
      });
      await assertSucceeds(deleteDoc(doc(asAlice(), "nicknames", "앨리스")));
    });

    it("데이터 어긋남: users에 nickname 필드가 없어도 지울 수 있다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE), {profileImg: "1"});
      });
      await assertSucceeds(deleteDoc(doc(asAlice(), "nicknames", "앨리스")));
    });

    it("없는 닉네임 문서는 users.nickname이 그 닉네임이어도 지울 수 있다 (기존처럼 허용)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "users", ALICE), {nickname: "유령", profileImg: "1"});
      });
      await assertSucceeds(deleteDoc(doc(asAlice(), "nicknames", "유령")));
    });

    it("다른 사람 소유 닉네임 문서는 내 users.nickname과 상관없이 거부", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "밥"), {nickname: "밥", uid: BOB});
        await setDoc(doc(db, "users", BOB), {nickname: "다른이름"});
      });
      await assertFails(deleteDoc(doc(asAlice(), "nicknames", "밥")));
    });
  });

  describe("feedbacks", () => {
    const feedback = (reporterId) => ({
      type: "bug", reporterId, category: "ui_bug", description: "버튼이 안 눌려요",
      appVersion: "1.0.0", createdAt: serverTimestamp(), status: "pending",
    });

    it("본인 명의 제보는 만들 수 있다", async () => {
      await assertSucceeds(setDoc(doc(asAlice(), "feedbacks", "f1"), feedback(ALICE)));
    });

    it("다른 사람 명의 제보는 거부", async () => {
      await assertFails(setDoc(doc(asAlice(), "feedbacks", "f1"), feedback(BOB)));
    });

    it("제보는 아무도 읽을 수 없다 (운영진은 콘솔에서 확인)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "feedbacks", "f1"), {...feedback(ALICE), createdAt: new Date()});
      });
      await assertFails(getDoc(doc(asAlice(), "feedbacks", "f1")));
    });
  });

  describe("studying", () => {
    // 앱(StudyingRemoteDataSourceImpl.saveStudyingUser)이 보내는 필드와 같게 둔다 (#525)
    const studyingUser = (overrides = {}) => ({
      nickname: "앨리스", profileImg: "", tag: "공부", studyingTime: 60000,
      updatedAt: serverTimestamp(), ...overrides,
    });
    const aliceStudying = () => doc(asAlice(), "studying", ALICE);

    it("본인 문서만 쓸 수 있고, 로그인하면 누구나 읽을 수 있다", async () => {
      await assertSucceeds(setDoc(aliceStudying(), studyingUser()));
      await assertFails(setDoc(doc(asBob(), "studying", ALICE), studyingUser()));
      await assertSucceeds(getDoc(doc(asBob(), "studying", ALICE)));
      await assertFails(getDoc(doc(asGuest(), "studying", ALICE)));
    });

    it("이미 있는 문서도 set으로 다시 쓸 수 있다 (1분마다 갱신, updatedAt 없는 예전 문서 덮어쓰기)", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "studying", ALICE), {nickname: "앨리스", profileImg: "", tag: "공부", studyingTime: 0});
      });
      await assertSucceeds(setDoc(aliceStudying(), studyingUser({studyingTime: 120000})));
    });

    it("updatedAt이 없거나 서버 시각이 아니면 거부", async () => {
      const {updatedAt, ...withoutUpdatedAt} = studyingUser();
      await assertFails(setDoc(aliceStudying(), withoutUpdatedAt));
      await assertFails(setDoc(aliceStudying(), studyingUser({updatedAt: new Date()})));
    });

    it("예전 앱의 update(studyingTime, tag만)는 updatedAt이 없어 거부", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "studying", ALICE), {nickname: "앨리스", profileImg: "", tag: "공부", studyingTime: 0});
      });
      await assertFails(updateDoc(aliceStudying(), {studyingTime: 60000, tag: "공부"}));
    });

    it("허용하지 않은 필드가 있거나 필드가 빠지면 거부", async () => {
      await assertFails(setDoc(aliceStudying(), studyingUser({extra: true})));
      const {nickname, ...withoutNickname} = studyingUser();
      await assertFails(setDoc(aliceStudying(), withoutNickname));
    });

    it("필드 타입이 맞지 않거나 학습 시간이 음수면 거부", async () => {
      await assertFails(setDoc(aliceStudying(), studyingUser({studyingTime: -1})));
      await assertFails(setDoc(aliceStudying(), studyingUser({studyingTime: "60000"})));
      await assertFails(setDoc(aliceStudying(), studyingUser({tag: 1})));
    });

    it("본인 문서만 지울 수 있다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "studying", ALICE), {...studyingUser(), updatedAt: new Date()});
      });
      await assertFails(deleteDoc(doc(asBob(), "studying", ALICE)));
      await assertSucceeds(deleteDoc(aliceStudying()));
    });
  });

  describe("Tags", () => {
    it("로그인하면 읽기만 가능하고 쓰기는 불가", async () => {
      await assertSucceeds(getDoc(doc(asAlice(), "Tags", "tag1")));
      await assertFails(setDoc(doc(asAlice(), "Tags", "tag2"), {name: "새 태그"}));
      await assertFails(getDoc(doc(asGuest(), "Tags", "tag1")));
    });
  });
});
