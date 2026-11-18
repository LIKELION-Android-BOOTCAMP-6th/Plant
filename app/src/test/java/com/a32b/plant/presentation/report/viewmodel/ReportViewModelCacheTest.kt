package com.a32b.plant.presentation.report.viewmodel

import com.a32b.plant.core.util.NETWORK_SLOW_READ_MESSAGE
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.report.GetDailyReportUseCase
import com.a32b.plant.domain.usecase.report.GetMonthlyReportUseCase
import com.a32b.plant.domain.usecase.report.GetReportRecordsUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeReportRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
        val users = FakeUserRepository(User.create().copy(uid = "uid"))
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
}
