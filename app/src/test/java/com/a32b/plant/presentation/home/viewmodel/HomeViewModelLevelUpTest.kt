package com.a32b.plant.presentation.home.viewmodel

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
            studyingTimerServiceController = StudyingTimerServiceController(ContextWrapper(null)),
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
    fun 쓰기_전에_UseCase가_실패하면_다이얼로그를_열어_둔_채_토스트() = runTest(dispatcher) {
        val env = createEnv()
        env.viewModel.onLevelUpClick()
        // Fake에서 UseCase의 화분 조회 결과만 null로 바꿔 요청 전 실패를 만든다
        // (실제로 화분이 사라지면 화분 목록 구독으로 다이얼로그도 닫힌다)
        env.pots.cachedPot = null

        env.viewModel.onLevelUpConfirm()

        assertTrue(env.pots.levelUps.isEmpty())
        assertEquals(listOf(HomeEvent.ShowToast(LevelUpPotUseCase.POT_NOT_FOUND_MESSAGE)), env.events)
        assertNotNull(env.viewModel.uiState.value.levelUpDialog)
        assertFalse(env.viewModel.uiState.value.isLoading)
    }

    @Test
    fun 캐시_반영_뒤_서버가_거부하면_다이얼로그는_닫힌_채_토스트만_뜨고_되돌아가도_다시_열리지_않는다() = runTest(dispatcher) {
        val rejection = AppError.Custom("레벨업 거부")
        val env = createEnv(levelUpResult = Result.Failure(rejection))
        // 레벨업 batch가 기기 캐시에 먼저 반영되어 화분 레벨이 오른 상황
        env.pots.onLevelUp = { env.potList.value = listOf(lv1Pot.copy(potLevel = 2)) }
        env.viewModel.onLevelUpClick()

        env.viewModel.onLevelUpConfirm()

        assertNull(env.viewModel.uiState.value.levelUpDialog)
        assertEquals(listOf(HomeEvent.ShowToast(rejection.message)), env.events)
        assertFalse(env.viewModel.uiState.value.isLoading)

        // 서버 거부로 캐시가 원래 레벨로 되돌아감
        env.potList.value = listOf(lv1Pot)
        assertNull(env.viewModel.uiState.value.levelUpDialog)
    }

    @Test
    fun 처리_중에는_연타와_닫기와_다시_열기를_막는다() = runTest(dispatcher) {
        val env = createEnv()
        val gate = CompletableDeferred<Unit>()
        env.pots.levelUpGate = gate
        env.viewModel.onLevelUpClick()

        env.viewModel.onLevelUpConfirm()
        env.viewModel.onLevelUpConfirm()
        env.viewModel.onLevelUpDialogDismiss()

        assertTrue(env.viewModel.uiState.value.isLoading)
        assertEquals(1, env.pots.levelUps.size)
        assertNotNull(env.viewModel.uiState.value.levelUpDialog)

        // 처리 중 레벨이 바뀌어 다이얼로그가 닫힌 뒤 홈 버튼을 눌러도 다음 레벨 다이얼로그를 열지 않는다
        env.potList.value = listOf(lv1Pot.copy(potLevel = 2))
        assertNull(env.viewModel.uiState.value.levelUpDialog)
        env.viewModel.onLevelUpClick()
        assertNull(env.viewModel.uiState.value.levelUpDialog)

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

    private companion object {
        const val HOUR = 60 * 60 * 1000L
    }
}
