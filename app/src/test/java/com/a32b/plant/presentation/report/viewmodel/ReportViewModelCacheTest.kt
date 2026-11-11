package com.a32b.plant.presentation.report.viewmodel

import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.ReportPeriodData
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.core.util.NETWORK_SLOW_READ_MESSAGE
import com.a32b.plant.domain.repository.ReportRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.report.GetDailyReportUseCase
import com.a32b.plant.domain.usecase.report.GetMonthlyReportUseCase
import com.a32b.plant.domain.usecase.report.GetReportRecordsUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelCacheTest {
    @Test
    fun `cache based reports are exposed by ui state`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel = createViewModel(isFromCache = true)

            assertTrue(viewModel.uiState.value.isFromCache)
            viewModel.selectTab(ReportTab.MONTHLY)
            assertTrue(viewModel.uiState.value.isFromCache)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `server based reports are not marked as cache based`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel = createViewModel(isFromCache = false)

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.dailyReport)
            assertFalse(viewModel.uiState.value.isFromCache)
            viewModel.selectTab(ReportTab.MONTHLY)
            assertNotNull(viewModel.uiState.value.monthlyReport)
            assertFalse(viewModel.uiState.value.isFromCache)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `daily cache notice includes cached monthly calendar after date selection`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            lateinit var repository: FakeReportRepository
            val viewModel = createViewModel(isFromCache = true) { repository = it }

            repository.isFromCache = false
            viewModel.selectDate(viewModel.uiState.value.today)

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.dailyReport)
            assertTrue(viewModel.uiState.value.isFromCache)
            viewModel.selectTab(ReportTab.MONTHLY)
            assertTrue(viewModel.uiState.value.isFromCache)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `daily cache notice remains when only daily report is cached after date selection`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            lateinit var repository: FakeReportRepository
            val viewModel = createViewModel(isFromCache = false) { repository = it }

            repository.isFromCache = true
            viewModel.selectDate(viewModel.uiState.value.today)

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.dailyReport)
            assertTrue(viewModel.uiState.value.isFromCache)
            viewModel.selectTab(ReportTab.MONTHLY)
            assertNotNull(viewModel.uiState.value.monthlyReport)
            assertFalse(viewModel.uiState.value.isFromCache)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `server reports remain without cache notice after date selection`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel = createViewModel(isFromCache = false)

            viewModel.selectDate(viewModel.uiState.value.today)

            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.dailyReport)
            assertFalse(viewModel.uiState.value.isFromCache)
            viewModel.selectTab(ReportTab.MONTHLY)
            assertNotNull(viewModel.uiState.value.monthlyReport)
            assertFalse(viewModel.uiState.value.isFromCache)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `parallel slow report reads show one notice`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel = createViewModel(isFromCache = false, delayMillis = 3_000L)
            val events = mutableListOf<ReportEvent>()
            val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.event.toList(events)
            }

            advanceTimeBy(2_001L)
            assertEquals(listOf(ReportEvent.ShowToast(NETWORK_SLOW_READ_MESSAGE)), events)

            advanceUntilIdle()
            eventJob.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(
        isFromCache: Boolean,
        delayMillis: Long = 0L,
        onRepositoryCreated: (FakeReportRepository) -> Unit = {}
    ): ReportViewModel {
        val users = FakeUserRepository()
        val ensureCurrentUser = EnsureCurrentUserUseCase(
            userRepository = users,
            notifier = object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
            }
        )
        val repository = FakeReportRepository(isFromCache, delayMillis)
        onRepositoryCreated(repository)
        val records = GetReportRecordsUseCase(repository)
        return ReportViewModel(
            ensureCurrentUserUseCase = ensureCurrentUser,
            getDailyReportUseCase = GetDailyReportUseCase(records),
            getMonthlyReportUseCase = GetMonthlyReportUseCase(records)
        )
    }

    private class FakeReportRepository(var isFromCache: Boolean, private val delayMillis: Long) : ReportRepository {
        override suspend fun getPeriodData(
            uid: String,
            startInclusive: Long,
            endExclusive: Long
        ): Result<ReportPeriodData> {
            delay(delayMillis)
            return Result.Success(ReportPeriodData(emptyList(), isFromCache))
        }
    }

    private class FakeUserRepository : UserRepository {
        override val currentUser: StateFlow<User?> = MutableStateFlow(User.create().copy(uid = "uid"))
        override fun observeDarkMode(): Flow<Boolean> = emptyFlow()
        override fun startUserSession(user: User) = Unit
        override fun endUserSession() = Unit
        override fun takeExpiredSessionReason(): SessionExpiredReason? = null
        override suspend fun updateLastSelectedPot(uid: String, potId: String) = Unit
        override suspend fun getUser(uid: String): Result<User?> = error("Unused")
        override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = error("Unused")
        override suspend fun createUser(uid: String): Result<User> = error("Unused")
        override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> = error("Unused")
        override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> = error("Unused")
        override suspend fun updateProfile(user: User): Result<Unit> = error("Unused")
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = error("Unused")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Unused")
    }
}
