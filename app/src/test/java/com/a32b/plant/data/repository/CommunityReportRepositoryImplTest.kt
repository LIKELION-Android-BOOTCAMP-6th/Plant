package com.a32b.plant.data.repository

import com.a32b.plant.data.source.remote.community.CommunityReportRemoteDataSource
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ReportReason
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityReportRepositoryImplTest {

    @Test
    fun `권리 침해 사유는 copyright로 저장 요청된다 - 게시글`() = runTest {
        val remote = FakeCommunityReportRemoteDataSource()
        val repository = CommunityReportRepositoryImpl(remote)

        val result = repository.createReport(
            reporterUid = "uid-1",
            targetType = "post",
            targetId = "post-1",
            parentPostId = null,
            reason = ReportReason.COPYRIGHT,
            detail = "https://example.com/original"
        )

        assertEquals(Result.Success(Unit), result)
        assertEquals("copyright", remote.lastReason)
        assertEquals("https://example.com/original", remote.lastDetail)
        assertEquals("post", remote.lastTargetType)
    }

    @Test
    fun `권리 침해 사유는 copyright로 저장 요청된다 - 댓글`() = runTest {
        val remote = FakeCommunityReportRemoteDataSource()
        val repository = CommunityReportRepositoryImpl(remote)

        repository.createReport(
            reporterUid = "uid-1",
            targetType = "comment",
            targetId = "comment-1",
            parentPostId = "post-1",
            reason = ReportReason.COPYRIGHT,
            detail = null
        )

        assertEquals("copyright", remote.lastReason)
        assertEquals("comment", remote.lastTargetType)
        assertEquals("post-1", remote.lastParentPostId)
    }

    @Test
    fun `이미 신고한 대상이면 사유와 관계없이 실패한다`() = runTest {
        // 사유마다 Fake를 새로 만들어 앞 사유의 기록이 섞이지 않게 한다
        ReportReason.entries.forEach { reason ->
            val remote = FakeCommunityReportRemoteDataSource(reported = false)
            val repository = CommunityReportRepositoryImpl(remote)

            val result = repository.createReport(
                reporterUid = "uid-1",
                targetType = "post",
                targetId = "post-1",
                parentPostId = null,
                reason = reason,
                detail = null
            )

            assertEquals(reason.firestoreKey, remote.lastReason)
            assertEquals(
                "사유: $reason",
                "이미 신고한 콘텐츠예요.",
                (result as Result.Failure).error.message
            )
        }
    }

    private class FakeCommunityReportRemoteDataSource(
        private val reported: Boolean = true
    ) : CommunityReportRemoteDataSource {
        var lastReason: String? = null
        var lastDetail: String? = null
        var lastTargetType: String? = null
        var lastParentPostId: String? = null

        override suspend fun createReport(
            reporterUid: String,
            targetType: String,
            targetId: String,
            parentPostId: String?,
            reason: String,
            detail: String?
        ): Boolean {
            lastReason = reason
            lastDetail = detail
            lastTargetType = targetType
            lastParentPostId = parentPostId
            return reported
        }
    }
}
