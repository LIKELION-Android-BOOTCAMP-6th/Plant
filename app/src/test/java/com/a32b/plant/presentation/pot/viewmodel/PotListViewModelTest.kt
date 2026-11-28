package com.a32b.plant.presentation.pot.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.PotSortType
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakePotRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 내 화분 정렬 기준 유지 (#803) */
@OptIn(ExperimentalCoroutinesApi::class)
class PotListViewModelTest {

    @Test
    fun `저장된 정렬을 읽은 뒤에 목록을 구독하고 로딩을 푼다`() = runPotListTest(
        readSortTypeResult = Result.Success(PotSortType.TOTAL_STUDY_TIME),
        readGate = CompletableDeferred()
    ) { repository, viewModel, potsSubscribed ->
        // 읽기 전: 로딩 화면 그대로, 목록 구독 전 → "생성일" 순서가 화면에 나오지 않는다
        assertTrue(viewModel.isLoading.value)
        assertFalse(potsSubscribed())

        repository.readSortTypeGate!!.complete(Unit)
        runCurrent()

        assertFalse(viewModel.isLoading.value)
        assertEquals(PotSortType.TOTAL_STUDY_TIME, viewModel.sortType.value)
        assertEquals(listOf("많이", "적게"), viewModel.studyingPots.value.map { it.name })
    }

    @Test
    fun `저장된 정렬은 공부 중과 기른 화분에 같이 적용된다`() = runPotListTest(
        readSortTypeResult = Result.Success(PotSortType.TOTAL_STUDY_TIME)
    ) { _, viewModel, _ ->
        assertEquals(listOf("많이", "적게"), viewModel.studyingPots.value.map { it.name })
        assertEquals(listOf("완료 많이", "완료 적게"), viewModel.completedPots.value.map { it.name })
    }

    @Test
    fun `정렬 읽기에 실패하면 생성일로 목록을 보여 준다`() = runPotListTest(
        readSortTypeResult = Result.Failure(AppError.Local("불러오기 실패"))
    ) { _, viewModel, _ ->
        assertFalse(viewModel.isLoading.value)
        assertEquals(PotSortType.CREATED_DATE, viewModel.sortType.value)
        assertEquals(listOf("적게", "많이"), viewModel.studyingPots.value.map { it.name })
    }

    @Test
    fun `정렬을 고르면 기기에 저장한다`() = runPotListTest { repository, viewModel, _ ->
        viewModel.setSortType(PotSortType.LEVEL)
        viewModel.setSortType(PotSortType.NAME)
        runCurrent()

        assertEquals(listOf(PotSortType.LEVEL, PotSortType.NAME), repository.savedSortTypes)
        assertEquals(PotSortType.NAME, viewModel.sortType.value)
    }

    @Test
    fun `지금과 같은 정렬을 고르면 다시 저장하지 않는다`() = runPotListTest(
        readSortTypeResult = Result.Success(PotSortType.LEVEL)
    ) { repository, viewModel, _ ->
        viewModel.setSortType(PotSortType.LEVEL)
        runCurrent()

        assertEquals(emptyList<PotSortType>(), repository.savedSortTypes)
    }

    @Test
    fun `저장에 실패해도 고른 정렬을 그대로 보여 준다`() = runPotListTest { repository, viewModel, _ ->
        repository.saveSortTypeResult = Result.Failure(AppError.Local())

        viewModel.setSortType(PotSortType.TOTAL_STUDY_TIME)
        runCurrent()

        assertEquals(PotSortType.TOTAL_STUDY_TIME, viewModel.sortType.value)
        assertEquals(listOf("많이", "적게"), viewModel.studyingPots.value.map { it.name })
    }

    @Test
    fun `세션이 없어도 저장된 정렬은 읽고 로딩을 푼다`() = runPotListTest(
        readSortTypeResult = Result.Success(PotSortType.LEVEL),
        user = null
    ) { _, viewModel, potsSubscribed ->
        assertFalse(viewModel.isLoading.value)
        assertFalse(potsSubscribed())
        assertEquals(PotSortType.LEVEL, viewModel.sortType.value)
    }

    private fun runPotListTest(
        readSortTypeResult: Result<PotSortType> = Result.Success(PotSortType.CREATED_DATE),
        readGate: CompletableDeferred<Unit>? = null,
        user: User? = User.create().copy(uid = UID),
        block: suspend TestScope.(FakePotRepository, PotListViewModel, potsSubscribed: () -> Boolean) -> Unit
    ) = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            var subscribed = false
            val repository = FakePotRepository(
                pots = MutableStateFlow(POTS).onStart { subscribed = true }
            ).apply {
                this.readSortTypeResult = readSortTypeResult
                readSortTypeGate = readGate
            }
            val viewModel = PotListViewModel(
                getPotListUseCase = GetPotListUseCase(repository),
                potRepository = repository,
                ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
                    FakeUserRepository(user),
                    object : SessionExpiredNotifier {
                        override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
                    }
                )
            )
            // 화면처럼 두 목록을 구독한다 (WhileSubscribed)
            backgroundScope.launch { viewModel.studyingPots.collect {} }
            backgroundScope.launch { viewModel.completedPots.collect {} }
            runCurrent()

            block(repository, viewModel) { subscribed }

            viewModel.viewModelScope.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private companion object {
        const val UID = "uid-1"

        // 생성일 순(최신 먼저)과 누적 학습 시간 순(많은 것 먼저)이 반대가 되게 만든다
        val POTS = listOf(
            pot("많이", createdAt = 1_000L, time = 600_000L, completed = false),
            pot("적게", createdAt = 2_000L, time = 60_000L, completed = false),
            pot("완료 많이", createdAt = 3_000L, time = 900_000L, completed = true),
            pot("완료 적게", createdAt = 4_000L, time = 90_000L, completed = true)
        )

        fun pot(name: String, createdAt: Long, time: Long, completed: Boolean) = Pot.EMPTY.copy(
            id = name,
            name = name,
            createdAt = createdAt,
            potTotalStudyingTime = time,
            isCompleted = completed
        )
    }
}
