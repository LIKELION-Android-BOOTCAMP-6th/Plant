package com.a32b.plant.data.source.remote.community

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityReportRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : CommunityReportRemoteDataSource {

    override suspend fun createReport(
        reporterUid: String,
        targetType: String,
        targetId: String,
        parentPostId: String?,
        reason: String,
        detail: String?
    ): Boolean {
        // 신고 문서 ID를 "신고자uid_대상id"로 고정 → 같은 대상 중복 신고는 문서 존재 여부로 판단
        // (보안 규칙도 이 ID 형식을 강제하고, 이미 있는 문서는 다시 만들 수 없다)
        val reportRef = db.collection("reports").document("${reporterUid}_$targetId")
        if (reportRef.get().await().exists()) return false

        val reportData = buildMap<String, Any?> {
            put("targetType", targetType)
            put("targetId", targetId)
            put("parentPostId", parentPostId)
            put("reporterId", reporterUid)
            put("reason", reason)
            put("detail", detail)
            put("createdAt", FieldValue.serverTimestamp())
            put("status", "pending")
        }

        val targetRef = when (targetType) {
            "post" -> db.collection("posts").document(targetId)
            "comment" -> parentPostId?.let { postId ->
                db.collection("posts").document(postId)
                    .collection("comments").document(targetId)
            }
            else -> null
        }

        // 신고 문서 생성과 reportCount +1을 한 batch로 묶음
        // → 보안 규칙이 "이번 쓰기에서 신고 문서가 새로 생겼는지"로 +1을 검증하고,
        //   대상이 삭제됐으면 둘 다 반영되지 않아 신고 문서만 남는 일이 없다
        db.runBatch { batch ->
            batch.set(reportRef, reportData)
            targetRef?.let { batch.update(it, "reportCount", FieldValue.increment(1)) }
        }.await()

        return true
    }
}
