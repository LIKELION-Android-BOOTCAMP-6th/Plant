// posts/{postId}, 하위 comments 규칙 테스트
const {describe, it, before, after, beforeEach} = require("node:test");
const {assertFails, assertSucceeds} = require("@firebase/rules-unit-testing");
const {
  arrayUnion, deleteDoc, doc, getDoc, increment, runTransaction, serverTimestamp, setDoc, updateDoc, writeBatch,
} = require("firebase/firestore");
const {ALICE, BOB, createTestEnv, seed} = require("../helpers");

// 앱이 새 게시글을 만들 때 보내는 초기값
function newPost(authorId, overrides = {}) {
  return {
    author: {id: authorId},
    title: "제목",
    content: "내용",
    likeCount: 0,
    commentCount: 0,
    bookmarkCount: 0,
    reportCount: 0,
    isHidden: false,
    likedBy: [],
    bookmarkedBy: [],
    ...overrides,
  };
}

describe("posts", () => {
  let testEnv;
  before(async () => {
    testEnv = await createTestEnv();
  });
  after(async () => {
    await testEnv.cleanup();
  });
  beforeEach(async () => {
    await testEnv.clearFirestore();
    // 앨리스가 쓴 게시글 + 밥이 그 게시글에 단 댓글
    await seed(testEnv, async (db) => {
      await setDoc(doc(db, "posts", "post1"), newPost(ALICE));
      await setDoc(doc(db, "posts", "post1", "comments", "c1"), {
        user: {uid: BOB}, content: "댓글", reportCount: 0, isHidden: false,
      });
    });
  });

  const asAlice = () => testEnv.authenticatedContext(ALICE).firestore();
  const asBob = () => testEnv.authenticatedContext(BOB).firestore();
  const asGuest = () => testEnv.unauthenticatedContext().firestore();

  describe("게시글", () => {
    it("로그인하면 읽을 수 있고, 비로그인은 읽을 수 없다", async () => {
      await assertSucceeds(getDoc(doc(asBob(), "posts", "post1")));
      await assertFails(getDoc(doc(asGuest(), "posts", "post1")));
    });

    it("초기값으로 본인 명의 게시글을 만들 수 있다", async () => {
      await assertSucceeds(setDoc(doc(asBob(), "posts", "post2"), newPost(BOB)));
    });

    it("다른 사람 명의나 조작된 초기값으로는 만들 수 없다", async () => {
      await assertFails(setDoc(doc(asBob(), "posts", "post2"), newPost(ALICE)));
      await assertFails(setDoc(doc(asBob(), "posts", "post2"), newPost(BOB, {likeCount: 100})));
      await assertFails(setDoc(doc(asBob(), "posts", "post2"), newPost(BOB, {isHidden: true})));
    });

    it("작성자만 본문을 수정·삭제할 수 있다", async () => {
      await assertSucceeds(updateDoc(doc(asAlice(), "posts", "post1"), {title: "수정"}));
      await assertFails(updateDoc(doc(asBob(), "posts", "post1"), {title: "남의 글 수정"}));
      await assertFails(deleteDoc(doc(asBob(), "posts", "post1")));
      await assertSucceeds(deleteDoc(doc(asAlice(), "posts", "post1")));
    });

    it("작성자도 본문 외 필드(likeCount 등)는 직접 바꿀 수 없다", async () => {
      await assertFails(updateDoc(doc(asAlice(), "posts", "post1"), {title: "수정", likeCount: 50}));
    });
  });

  describe("좋아요", () => {
    it("다른 사람 글에 본인 uid 추가 + 1 증가는 허용", async () => {
      await assertSucceeds(updateDoc(doc(asBob(), "posts", "post1"), {
        likedBy: arrayUnion(BOB), likeCount: increment(1),
      }));
    });

    it("카운트를 1보다 크게 올리거나 남의 uid를 넣으면 거부", async () => {
      await assertFails(updateDoc(doc(asBob(), "posts", "post1"), {
        likedBy: arrayUnion(BOB), likeCount: increment(2),
      }));
      await assertFails(updateDoc(doc(asBob(), "posts", "post1"), {
        likedBy: arrayUnion("someone"), likeCount: increment(1),
      }));
    });

    it("본인 게시글에는 좋아요를 누를 수 없다", async () => {
      await assertFails(updateDoc(doc(asAlice(), "posts", "post1"), {
        likedBy: arrayUnion(ALICE), likeCount: increment(1),
      }));
    });
  });

  describe("신고", () => {
    function reportDoc(reporterId, targetId) {
      return {
        targetType: "post", targetId, parentPostId: targetId, reporterId,
        reason: "spam", detail: "", createdAt: serverTimestamp(), status: "pending",
      };
    }

    it("신고 문서 생성과 함께 reportCount +1은 허용 (batch)", async () => {
      const db = asBob();
      const batch = writeBatch(db);
      batch.set(doc(db, "reports", `${BOB}_post1`), reportDoc(BOB, "post1"));
      batch.update(doc(db, "posts", "post1"), {reportCount: increment(1)});
      await assertSucceeds(batch.commit());
    });

    it("신고 문서 없이 reportCount만 올리면 거부", async () => {
      await assertFails(updateDoc(doc(asBob(), "posts", "post1"), {reportCount: increment(1)}));
    });

    it("신고 문서 ID는 {신고자uid}_{대상id} 형식이어야 한다", async () => {
      await assertFails(setDoc(doc(asBob(), "reports", "random-id"), reportDoc(BOB, "post1")));
    });

    it("다른 사람의 신고 문서는 읽을 수 없다", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "reports", `${BOB}_post1`), {...reportDoc(BOB, "post1"), createdAt: new Date()});
      });
      await assertSucceeds(getDoc(doc(asBob(), "reports", `${BOB}_post1`)));
      await assertFails(getDoc(doc(asAlice(), "reports", `${BOB}_post1`)));
    });
  });

  describe("댓글", () => {
    it("본인 명의 댓글은 만들 수 있고, 다른 사람 명의는 거부", async () => {
      const comment = (uid) => ({user: {uid}, content: "안녕", reportCount: 0, isHidden: false});
      await assertSucceeds(setDoc(doc(asAlice(), "posts", "post1", "comments", "c2"), comment(ALICE)));
      await assertFails(setDoc(doc(asAlice(), "posts", "post1", "comments", "c3"), comment(BOB)));
    });

    it("댓글 작성자는 내용만 수정할 수 있다", async () => {
      await assertSucceeds(updateDoc(doc(asBob(), "posts", "post1", "comments", "c1"), {content: "수정"}));
      await assertFails(updateDoc(doc(asBob(), "posts", "post1", "comments", "c1"), {isHidden: true}));
      await assertFails(updateDoc(doc(asAlice(), "posts", "post1", "comments", "c1"), {content: "남의 댓글 수정"}));
    });

    // 회원탈퇴 시 익명화는 deleteAccount 함수(Admin SDK)가 처리한다 → 앱 예외 제거 (#457, #431)
    it("댓글 작성자도 익명화 형태(content + user)로는 수정할 수 없다", async () => {
      await assertFails(updateDoc(doc(asBob(), "posts", "post1", "comments", "c1"), {
        content: "- 삭제된 댓글입니다. -",
        user: {uid: "", nickname: "(알 수 없음)", profileImg: ""},
      }));
      await assertFails(updateDoc(doc(asBob(), "posts", "post1", "comments", "c1"), {
        user: {uid: ALICE},
      }));
    });

    it("댓글 작성자는 본인 댓글을 삭제할 수 있다", async () => {
      await assertSucceeds(deleteDoc(doc(asBob(), "posts", "post1", "comments", "c1")));
    });

    // 게시글 삭제(deletePost) 시 남의 댓글까지 지우는 데 필요 (#457에서도 유지)
    it("게시글 작성자는 남의 댓글을 삭제할 수 있다", async () => {
      await assertSucceeds(deleteDoc(doc(asAlice(), "posts", "post1", "comments", "c1")));
    });

    it("제3자는 댓글을 삭제할 수 없다", async () => {
      const carol = testEnv.authenticatedContext("carol").firestore();
      await assertFails(deleteDoc(doc(carol, "posts", "post1", "comments", "c1")));
    });

    // 앱이 실제로 보내는 쓰기(CommunityRemoteDataSourceImpl)가 새 규칙에서 막히지 않는지 (#528)
    function commentActivity(uid, commentId) {
      return {
        id: "", uid, type: "댓글", title: "제목", targetId: "post1",
        comment: "안녕", commentId, createAt: serverTimestamp(),
      };
    }

    it("앱의 댓글 작성 batch(댓글 + activity, commentCount 없음)는 허용", async () => {
      const db = asBob();
      const batch = writeBatch(db);
      batch.set(doc(db, "posts", "post1", "comments", "c2"), {
        user: {uid: BOB}, content: "안녕", activityId: "a2", commentId: "c2",
        createdAt: serverTimestamp(), reportCount: 0, isHidden: false,
      });
      batch.set(doc(db, "activities", "a2"), commentActivity(BOB, "c2"));
      await assertSucceeds(batch.commit());
    });

    it("앱의 댓글 삭제 트랜잭션(댓글 + activity 삭제, commentCount 없음)은 허용", async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "activities", "a1"), {...commentActivity(BOB, "c1"), createAt: new Date()});
      });
      const db = asBob();
      await assertSucceeds(runTransaction(db, async (txn) => {
        const commentRef = doc(db, "posts", "post1", "comments", "c1");
        const snapshot = await txn.get(commentRef);
        if (!snapshot.exists()) throw new Error("댓글이 없음");
        txn.delete(commentRef);
        txn.delete(doc(db, "activities", "a1"));
      }));
    });

    it("삭제된(없는) 게시글에는 댓글을 만들 수 없다", async () => {
      const comment = {user: {uid: BOB}, content: "안녕", reportCount: 0, isHidden: false};
      await assertFails(setDoc(doc(asBob(), "posts", "no-post", "comments", "c2"), comment));
    });

    // commentCount는 댓글 생성·삭제 트리거(Admin SDK)만 쓴다 (#528)
    it("commentCount는 누구도 직접 바꿀 수 없다 (+1/-1, 작성자 포함)", async () => {
      await assertFails(updateDoc(doc(asBob(), "posts", "post1"), {commentCount: increment(1)}));
      await assertFails(updateDoc(doc(asBob(), "posts", "post1"), {commentCount: increment(-1)}));
      await assertFails(updateDoc(doc(asAlice(), "posts", "post1"), {commentCount: increment(1)}));
    });

    it("댓글 작성 batch에 commentCount +1을 함께 넣으면 거부 (예전 앱 방식)", async () => {
      const db = asBob();
      const batch = writeBatch(db);
      batch.set(doc(db, "posts", "post1", "comments", "c2"), {
        user: {uid: BOB}, content: "안녕", reportCount: 0, isHidden: false,
      });
      batch.update(doc(db, "posts", "post1"), {commentCount: increment(1)});
      await assertFails(batch.commit());
    });
  });

  // likes 하위 컬렉션(예전 좋아요 방식)은 앱이 쓰지 않아 규칙을 제거함 (#457)
  describe("likes", () => {
    beforeEach(async () => {
      await seed(testEnv, async (db) => {
        await setDoc(doc(db, "posts", "post1", "likes", BOB), {uid: BOB});
      });
    });

    it("본인 문서·게시글 작성자 모두 삭제할 수 없다", async () => {
      await assertFails(deleteDoc(doc(asBob(), "posts", "post1", "likes", BOB)));
      await assertFails(deleteDoc(doc(asAlice(), "posts", "post1", "likes", BOB)));
    });

    it("읽기·생성도 거부", async () => {
      await assertFails(getDoc(doc(asBob(), "posts", "post1", "likes", BOB)));
      await assertFails(setDoc(doc(asAlice(), "posts", "post1", "likes", ALICE), {uid: ALICE}));
    });
  });
});
