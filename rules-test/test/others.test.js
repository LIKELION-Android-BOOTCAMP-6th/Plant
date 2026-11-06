// nicknames, feedbacks, studying, Tags 규칙 테스트
const {describe, it, before, after, beforeEach} = require("node:test");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {deleteDoc, doc, getDoc, serverTimestamp, setDoc} = require("firebase/firestore");
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

    it("규칙 이전에 만들어진 형식 위반 닉네임 문서도 소유자는 삭제할 수 있다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "nicknames", "앨리스 "), {nickname: "앨리스 ", uid: ALICE});
      });
      await assertSucceeds(deleteDoc(doc(asAlice(), "nicknames", "앨리스 ")));
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
    it("본인 문서만 쓸 수 있고, 로그인하면 누구나 읽을 수 있다", async () => {
      await assertSucceeds(setDoc(doc(asAlice(), "studying", ALICE), {studyingTime: 0}));
      await assertFails(setDoc(doc(asBob(), "studying", ALICE), {studyingTime: 0}));
      await assertSucceeds(getDoc(doc(asBob(), "studying", ALICE)));
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
