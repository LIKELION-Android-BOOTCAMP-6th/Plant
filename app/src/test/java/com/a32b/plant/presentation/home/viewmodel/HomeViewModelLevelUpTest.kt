package com.a32b.plant.presentation.home.viewmodel

import android.content.Context
import android.content.ContextWrapper
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.network.NetworkMonitor
import com.a32b.plant.domain.repository.ItemRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.item.OpenBonusBoxUseCase
import com.a32b.plant.domain.usecase.pot.CheckLevelUpConditionUseCase
import com.a32b.plant.domain.usecase.pot.GetActivePotUseCase
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.pot.GetTodayTotalStudyTimeUseCase
import com.a32b.plant.domain.usecase.pot.LevelUpPotUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studying.FinishStudyingUseCase
import com.a32b.plant.domain.usecase.studying.GetRecoverySessionUseCase
import com.a32b.plant.domain.usecase.studying.RecoverStudySessionUseCase
import com.a32b.plant.fake.FakePotRepository
import com.a32b.plant.fake.FakeStudyingRepository
import com.a32b.plant.fake.FakeUserRepository
import com.a32b.plant.presentation.studying.service.StudyingTimerServiceController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 홈 레벨업 확인 다이얼로그·성공 문구·보너스 박스 클릭 (#678) */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelLevelUpTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // Lv.1 → 2 조건: 5시간, 햇빛 3·물 1·정성 5
    private val readyItems = Item(heart = 5, sun = 3, water = 1, fertilizer = 0, nutrient = 0, box = 0)
    private val lv1Pot = Pot.EMPTY.copy(id = "pot-1", potLevel = 1, potTotalStudyingTime = 5 * HOUR)

    private class Env(
        val viewModel: HomeViewModel,
        val pots: FakePotRepository,
        val potList: MutableStateFlow<List<Pot>>,
        val users: FakeUserRepository,
        val events: MutableList<HomeEvent>
    )

    private fun kotlinx.coroutines.test.TestScope.createEnv(
        pot: Pot = lv1Pot,
        items: Item = readyItems,
        levelUpResult: Result<SaveResult> = Result.Success(SaveResult.Saved)
    ): Env {
        val potList = MutableStateFlow(listOf(pot))
        val pots = FakePotRepository(
            cachedPot = pot,
            levelUpResult = levelUpResult,
            pots = potList,
            todayStudyLogsResult = Result.Success(emptyList())
        )
        val users = FakeUserRepository(User.create().copy(uid = "uid-1", item = items))
        val studying = FakeStudyingRepository(unseenStudyRewards = flowOf(emptyList()))
        val ensureUser = EnsureCurrentUserUseCase(users, object : SessionExpiredNotifier {
            override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
        })
        val viewModel = HomeViewModel(
            getActivePotUseCase = GetActivePotUseCase(pots),
            getPotListUseCase = GetPotListUseCase(pots),
            userRepository = users,
            ensureCurrentUserUseCase = ensureUser,
            getTodayTotalStudyTimeUseCase = GetTodayTotalStudyTimeUseCase(pots),
            studyingRepository = studying,
            getRecoverySessionUseCase = GetRecoverySessionUseCase(studying, ensureUser),
            recoverStudySessionUseCase = RecoverStudySessionUseCase(FinishStudyingUseCase(studying, ensureUser), studying),
            openBonusBoxUseCase = OpenBonusBoxUseCase(object : ItemRepository {
                override suspend fun openBonusBoxes(boxCount: Int, requestId: String) = error("Unused")
            }, ensureUser),
            checkLevelUpConditionUseCase = CheckLevelUpConditionUseCase(),
            levelUpPotUseCase = LevelUpPotUseCase(pots, ensureUser),
            // 이 테스트에서는 타이머 서비스를 멈추는 onHomeResumed를 부르지 않으므로 Context는 쓰이지 않는다
            // (단위 테스트의 android.jar는 생성자가 "Stub!" 예외를 던지므로 생성자 없이 만든다)
            studyingTimerServiceController = StudyingTimerServiceController(unusedContext()),
            networkMonitor = object : NetworkMonitor {
                override fun isOnline() = true
                override fun observeIsOnline() = flowOf(true)
            }
        )
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
        return Env(viewModel, pots, potList, users, events)
    }

    @Test
    fun 레벨업_버튼은_요청하지_않고_확인_다이얼로그를_연다() = runTest(dispatcher) {
        val env = createEnv()

        env.viewModel.onLevelUpClick()

        val dialog = env.viewModel.uiState.value.levelUpDialog
        assertNotNull(dialog)
        assertEquals("pot-1", dialog!!.potId)
        assertEquals(2, dialog.newLevel)
        assertTrue(dialog.isReady)
        assertTrue(env.pots.levelUps.isEmpty())
        assertTrue(env.events.isEmpty())
    }

    @Test
    fun 최고_레벨은_다이얼로그_대신_토스트() = runTest(dispatcher) {
        val env = createEnv(pot = lv1Pot.copy(potLevel = 6))

        env.viewModel.onLevelUpClick()

        assertNull(env.viewModel.uiState.value.levelUpDialog)
        assertEquals(listOf(HomeEvent.ShowToast(LevelUpPotUseCase.MAX_LEVEL_MESSAGE)), env.events)
    }

    @Test
    fun 조건이_부족하면_요청하지_않고_부족한_내용을_토스트로_알리고_다이얼로그는_유지() = runTest(dispatcher) {
        val env = createEnv(items = readyItems.copy(sun = 1, water = 0))
        env.viewModel.onLevelUpClick()

        env.viewModel.onLevelUpConfirm()

        assertTrue(env.pots.levelUps.isEmpty())
        assertEquals(listOf(HomeEvent.ShowToast("재료가 부족해요: 햇빛 2개, 물 1개")), env.events)
        assertNotNull(env.viewModel.uiState.value.levelUpDialog)
        assertFalse(env.viewModel.uiState.value.isLoading)
    }

    @Test
    fun 조건을_채우면_요청하고_새_레벨을_담은_성공_이벤트_후_다이얼로그를_닫는다() = runTest(dispatcher) {
        val env = createEnv()
        env.viewModel.onLevelUpClick()

        env.viewModel.onLevelUpConfirm()

        assertEquals(1, env.pots.levelUps.size)
        assertEquals(1, env.pots.levelUps.single().currentLevel)
        val success = env.events.single() as HomeEvent.LevelUpSuccess
        assertEquals(HomeEvent.LevelUpSuccess(newLevel = 2, isPending = false), success)
        assertEquals("Lv.2로 성장했어요!", success.message)
        assertNull(env.viewModel.uiState.value.levelUpDialog)
        assertFalse(env.viewModel.uiState.value.isLoading)
    }

    @Test
    fun 오프라인_대기_저장이면_자동_저장_안내가_붙은_성공_문구() = runTest(dispatcher) {
        val env = createEnv(levelUpResult = Result.Success(SaveResult.Pending))
        env.viewModel.onLevelUpClick()

        env.viewModel.onLevelUpConfirm()

        assertEquals(
            "Lv.2로 성장했어요! 네트워크에 연결되면 자동으로 저장돼요.",
            (env.events.single() as HomeEvent.LevelUpSuccess).message
        )
    }

    @Test
    fun 요청이_실패하면_다이얼로그를_열어_둔_채_토스트() = runTest(dispatcher) {
        val env = createEnv(levelUpResult = Result.Failure(AppError.Network()))
        env.viewModel.onLevelUpClick()

        env.viewModel.onLevelUpConfirm()

        assertEquals(listOf(HomeEvent.ShowToast(AppError.Network().message)), env.events)
        assertNotNull(env.viewModel.uiState.value.levelUpDialog)
        assertFalse(env.viewModel.uiState.value.isLoading)
    }

    @Test
    fun 처리_중에는_연타와_닫기를_막는다() = runTest(dispatcher) {
        val env = createEnv()
        val gate = CompletableDeferred<Unit>()
        env.pots.levelUpGate = gate
        env.viewModel.onLevelUpClick()

        env.viewModel.onLevelUpConfirm()
        env.viewModel.onLevelUpConfirm()
        env.viewModel.onLevelUpDialogDismiss()
        env.viewModel.onLevelUpClick()

        assertTrue(env.viewModel.uiState.value.isLoading)
        assertEquals(1, env.pots.levelUps.size)
        assertNotNull(env.viewModel.uiState.value.levelUpDialog)

        gate.complete(Unit)
        assertFalse(env.viewModel.uiState.value.isLoading)
        assertEquals(1, env.events.filterIsInstance<HomeEvent.LevelUpSuccess>().size)
    }

    @Test
    fun 다이얼로그가_열린_사이_아이템_수가_바뀌면_최신_보유_개수로_다시_만든다() = runTest(dispatcher) {
        val env = createEnv(items = readyItems.copy(sun = 0))
        env.viewModel.onLevelUpClick()
        assertFalse(env.viewModel.uiState.value.levelUpDialog!!.isReady)

        env.users.currentUser.value = env.users.currentUser.value!!.copy(item = readyItems)

        val dialog = env.viewModel.uiState.value.levelUpDialog!!
        assertEquals(3, dialog.items.single { it.type == ItemType.SUN }.owned)
        assertTrue(dialog.isReady)
    }

    @Test
    fun 다이얼로그가_열린_사이_화분_레벨이_바뀌거나_화분이_없어지면_닫는다() = runTest(dispatcher) {
        val env = createEnv()
        env.viewModel.onLevelUpClick()
        env.potList.value = listOf(lv1Pot.copy(potLevel = 2))
        assertNull(env.viewModel.uiState.value.levelUpDialog)

        // 다시 열면 바뀐 레벨 기준으로 열린다
        env.viewModel.onLevelUpClick()
        assertEquals(2, env.viewModel.uiState.value.levelUpDialog!!.currentLevel)

        env.potList.value = emptyList()
        assertNull(env.viewModel.uiState.value.levelUpDialog)
    }

    @Test
    fun 닫기를_누르면_다이얼로그가_닫힌다() = runTest(dispatcher) {
        val env = createEnv()
        env.viewModel.onLevelUpClick()

        env.viewModel.onLevelUpDialogDismiss()

        assertNull(env.viewModel.uiState.value.levelUpDialog)
    }

    @Test
    fun 보너스_박스가_없으면_토스트_있으면_개수_선택_다이얼로그_이벤트() = runTest(dispatcher) {
        val empty = createEnv(items = readyItems.copy(box = 0))
        empty.viewModel.onBonusBoxClick()
        assertEquals(listOf(HomeEvent.ShowToast(NO_BONUS_BOX_MESSAGE)), empty.events)

        val owned = createEnv(items = readyItems.copy(box = 2))
        owned.viewModel.onBonusBoxClick()
        assertEquals(listOf(HomeEvent.OpenBonusBoxDialog), owned.events)
    }

    // 생성자를 부르지 않고 객체만 만든다 (JVM의 sun.misc.Unsafe — 컴파일 클래스패스가 android.jar라 리플렉션으로 찾는다)
    private fun unusedContext(): Context {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
        return unsafeClass.getMethod("allocateInstance", Class::class.java)
            .invoke(unsafe, ContextWrapper::class.java) as Context
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
    }
}
