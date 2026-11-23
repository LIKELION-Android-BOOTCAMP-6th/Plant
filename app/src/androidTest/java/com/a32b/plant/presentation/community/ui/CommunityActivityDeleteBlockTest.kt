package com.a32b.plant.presentation.community.ui

import android.graphics.Color as AndroidColor
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.domain.community.PostUpdate
import com.a32b.plant.domain.community.PostUpdateNotifier
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.domain.usecase.community.DeleteActivitiesUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.presentation.community.viewmodel.CommunityActivityViewModel
import com.a32b.plant.presentation.core.component.LOADING_SCRIM_ALPHA
import com.a32b.plant.presentation.theme.DarkColorScheme
import com.a32b.plant.presentation.theme.LightColorScheme
import com.a32b.plant.presentation.theme.PlantTheme
import java.lang.reflect.Proxy
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 내 활동 선택 삭제 중 화면 조작 막기 확인 (#753)
 *
 * 게시글 삭제 응답을 gate로 붙잡아 "삭제 중" 상태를 만들고, 실제 터치(performClick)로 누른다.
 * - 상단바 "취소"·"삭제(n)", 태그가 눌려도 아무 일이 일어나지 않는지, 삭제가 끝나면 다시 눌리는지
 * - 시스템 뒤로가기는 막히지 않는지 (#748: 처음엔 선택 모드 해제, 다음엔 화면 나감)
 * - 덮개가 상단바까지 scrim 한 겹으로 덮는지, 로딩 표시가 하나뿐인지 (라이트·다크)
 */
@RunWith(AndroidJUnit4::class)
class CommunityActivityDeleteBlockTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val gate = CompletableDeferred<Unit>()
    private val repository = GatedCommunityRepository(gate)
    private val viewModel = CommunityActivityViewModel(
        repository = repository,
        deleteActivitiesUseCase = DeleteActivitiesUseCase(
            repository = repository,
            // 게시글 삭제 경로는 현재 유저를 조회하지 않는다
            ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
                unused<UserRepository>(),
                object : SessionExpiredNotifier {
                    override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
                }
            ),
            postUpdateNotifier = object : PostUpdateNotifier {
                override fun notifyPostUpdated(update: PostUpdate) = Unit
                override fun notifyPostCreated(postId: String) = Unit
                override fun notifyPostDeleted(postId: String) = Unit
            }
        )
    )

    @After
    fun tearDown() {
        gate.complete(Unit)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun topBarAndTagsDoNothingWhileDeletingAndWorkAfter() {
        showScreen(darkTheme = false)
        startDeleting()

        // 하나가 눌리면 뒤 버튼이 사라질 수 있어(취소 → 삭제(n) 숨김) 누를 때마다 바로 확인한다
        composeRule.onNode(hasText("(1)")).performClick()
        composeRule.waitForIdle()
        assertFalse("삭제(n)이 눌려 확인 창이 뜸", viewModel.uiState.value.isDeleteDialogShown)

        composeRule.onNodeWithText("취소").performClick()
        composeRule.waitForIdle()
        assertTrue("취소가 눌려 선택 모드가 풀림", viewModel.uiState.value.isSelectionMode)

        composeRule.onNodeWithText(ActivityType.COMMENT).performClick()
        composeRule.waitForIdle()
        assertEquals("태그가 눌려 탭이 바뀜", ActivityType.POST, viewModel.uiState.value.selected)

        assertTrue("삭제 중이 아님", viewModel.uiState.value.isDeleting)
        assertEquals("삭제 요청이 한 번이 아님", listOf("post-1"), repository.deletedPostIds)

        gate.complete(Unit)
        waitUntilDeleteEnds()
        composeRule.waitForIdle()

        // 반증: 같은 방법으로 누르면 삭제가 끝난 뒤에는 동작한다
        composeRule.onNodeWithText(ActivityType.COMMENT).performClick()
        composeRule.waitForIdle()
        assertEquals("삭제 후 태그가 눌리지 않음", ActivityType.COMMENT, viewModel.uiState.value.selected)
    }

    @Test
    fun systemBackStillWorksWhileDeleting() {
        showScreen(darkTheme = false)
        startDeleting()

        Espresso.pressBack()
        composeRule.waitForIdle()
        assertFalse("첫 뒤로가기에 선택 모드가 풀리지 않음", viewModel.uiState.value.isSelectionMode)
        assertTrue("뒤로가기로 삭제 상태가 풀림", viewModel.uiState.value.isDeleting)
        assertFalse("첫 뒤로가기에 화면이 닫힘", composeRule.activity.isFinishing)

        // 화면이 닫힌 뒤에는 composeRule.activity로 읽을 수 없어 미리 잡아 둔다
        val activity = composeRule.activity
        Espresso.pressBackUnconditionally()
        assertTrue("두 번째 뒤로가기에 화면이 닫히지 않음", activity.isFinishing || activity.isDestroyed)

        gate.complete(Unit)
        waitUntilDeleteEnds()
        assertEquals(listOf("post-1"), repository.deletedPostIds)
    }

    @Test
    fun lightThemeOverlayCoversTopBarWithOneSpinner() {
        assertOverlayCoversTopBar(darkTheme = false)
    }

    @Test
    fun darkThemeOverlayCoversTopBarWithOneSpinner() {
        assertOverlayCoversTopBar(darkTheme = true)
    }

    private fun assertOverlayCoversTopBar(darkTheme: Boolean) {
        showScreen(darkTheme)
        enterSelection()
        val before = topBarPixel()

        startDeleting(alreadySelected = true)
        val during = topBarPixel()

        val scrim = (if (darkTheme) DarkColorScheme else LightColorScheme).scrim.toArgb()
        val expected = overlay(before, scrim)
        assertTrue("상단바 색: 덮기 전 ${hex(before)}, 기대 ${hex(expected)}, 실제 ${hex(during)}", sameColor(expected, during))
        // 목록 자리 덮개와 화면 전체 덮개가 겹치지 않는다
        composeRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(1)
    }

    // ---------- 도우미 ----------

    private fun showScreen(darkTheme: Boolean) {
        composeRule.setContent {
            PlantTheme(darkTheme = darkTheme) {
                CommunityActivityScreen(navController = rememberNavController(), viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    private fun enterSelection() {
        composeRule.onNodeWithText("선택").performClick()
        composeRule.onNodeWithText("제목").performClick()
        composeRule.waitForIdle()
    }

    /** "선택" → 항목 선택 → "삭제(1)" → 확인 창 "삭제"까지 화면에서 누르고, 응답 대기 중인지 확인 */
    private fun startDeleting(alreadySelected: Boolean = false) {
        if (!alreadySelected) enterSelection()
        composeRule.onNode(hasText("(1)")).performClick()
        composeRule.onNode(hasText("삭제") and hasAnyAncestor(isDialog())).performClick()
        composeRule.waitForIdle()
        assertTrue("삭제 응답 대기 상태가 아님", viewModel.uiState.value.isDeleting)
    }

    // 삭제 결과 처리는 메인 스레드 코루틴에서 돈다. 화면을 나간 뒤에도 쓸 수 있게 Compose 대기 대신 상태를 직접 기다린다
    private fun waitUntilDeleteEnds() {
        val deadline = SystemClock.uptimeMillis() + 3_000
        while (viewModel.uiState.value.isDeleting) {
            if (SystemClock.uptimeMillis() > deadline) throw AssertionError("삭제가 끝나지 않음")
            SystemClock.sleep(50)
        }
    }

    /** 상단바에서 뒤로가기 버튼과 제목 사이 빈 곳의 색 */
    private fun topBarPixel(): Int {
        val back = composeRule.onNode(hasContentDescription("뒤로가기")).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithText("내 활동").fetchSemanticsNode().boundsInRoot
        val x = ((back.right + title.left) / 2).roundToInt()
        val y = title.center.y.roundToInt()
        return composeRule.onRoot().captureToImage().asAndroidBitmap().getPixel(x, y)
    }

    /** 배경 위에 scrim을 [LOADING_SCRIM_ALPHA]로 한 번 덮은 색 */
    private fun overlay(background: Int, scrim: Int): Int {
        fun blend(bg: Int, fg: Int) = (fg * LOADING_SCRIM_ALPHA + bg * (1 - LOADING_SCRIM_ALPHA)).roundToInt()
        return AndroidColor.rgb(
            blend(AndroidColor.red(background), AndroidColor.red(scrim)),
            blend(AndroidColor.green(background), AndroidColor.green(scrim)),
            blend(AndroidColor.blue(background), AndroidColor.blue(scrim))
        )
    }

    // 합성·반올림 차이 허용 (채널당 2)
    private fun sameColor(a: Int, b: Int) =
        abs(AndroidColor.red(a) - AndroidColor.red(b)) <= 2 &&
            abs(AndroidColor.green(a) - AndroidColor.green(b)) <= 2 &&
            abs(AndroidColor.blue(a) - AndroidColor.blue(b)) <= 2

    private fun hex(color: Int) = String.format("#%06X", color and 0xFFFFFF)

    /** 게시글 1개짜리 '게시물' 탭. 게시글 삭제는 gate가 열릴 때까지 응답하지 않는다 */
    private class GatedCommunityRepository(
        private val gate: CompletableDeferred<Unit>
    ) : CommunityRepository by unused() {
        private val activities = MutableStateFlow(
            listOf(
                CommunityActivity(
                    id = "a1", uid = "uid-1", type = ActivityType.POST, title = "제목",
                    targetId = "post-1", comment = null, commentId = null, createAt = null
                )
            )
        )
        val deletedPostIds = mutableListOf<String>()

        override fun observeActivity(selected: String): Flow<List<CommunityActivity>> =
            if (selected == ActivityType.POST) activities else MutableStateFlow(emptyList())

        override suspend fun isPostExist(postId: String): Result<Boolean> = Result.Success(true)

        override suspend fun deletePost(postId: String, activityId: String): Result<Unit> {
            deletedPostIds += postId
            gate.await()
            return Result.Success(Unit)
        }
    }
}

/** 이 테스트에서 부르지 않는 메서드만 있는 저장소. 불리면 바로 실패시킨다 */
private inline fun <reified T : Any> unused(): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->
        when (method.name) {
            "toString" -> "unused ${T::class.java.simpleName}"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            else -> throw UnsupportedOperationException("테스트에서 쓰지 않는 메서드: ${method.name}")
        }
    } as T
