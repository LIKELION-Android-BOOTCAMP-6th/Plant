package com.a32b.plant.presentation.mypage.viewmodel

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.auth.DeleteAccountUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeAuthRepository
import com.a32b.plant.fake.FakeStudyingRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {
    @Test
    fun unknownUserDuringReauthenticationNotifiesSessionExpiredWithoutToast() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val users = FakeUserRepository(User.create().copy(uid = "uid-1"))
            val auth = FakeAuthRepository(
                reauthenticationResult = Result.Failure(AppError.UnknownUser()),
                refreshResult = Result.Failure(AppError.Network())
            )
            val useCaseNotifier = CountingSessionExpiredNotifier()
            val viewModelNotifier = CountingSessionExpiredNotifier()
            val viewModel = DeleteAccountViewModel(
                deleteAccountUseCase = DeleteAccountUseCase(
                    authRepository = auth,
                    userRepository = users,
                    studyingRepository = FakeStudyingRepository(),
                    ensureCurrentUserUseCase = EnsureCurrentUserUseCase(users, useCaseNotifier)
                ),
                authRepository = auth,
                sessionExpiredNotifier = viewModelNotifier
            )
            val events = mutableListOf<DeleteAccountEvent>()
            backgroundScope.launch(dispatcher) { viewModel.events.toList(events) }

            viewModel.reauthenticateWithGoogleAndDelete("token")
            runCurrent()

            assertEquals(1, viewModelNotifier.calls)
            assertEquals(0, useCaseNotifier.calls)
            assertEquals(emptyList<DeleteAccountEvent>(), events)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // 이전 탈퇴 요청이 서버에서 끝났는데 응답만 받지 못한 뒤 다시 시도한 경우 (#457)
    @Test
    fun alreadyDeletedAccountDuringReauthenticationCompletesDeletion() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val users = FakeUserRepository(User.create().copy(uid = "uid-1"))
            val auth = FakeAuthRepository(
                reauthenticationResult = Result.Failure(AppError.AccountNotFound()),
                refreshResult = Result.Failure(AppError.AccountNotFound())
            )
            val notifier = CountingSessionExpiredNotifier()
            val viewModel = DeleteAccountViewModel(
                deleteAccountUseCase = DeleteAccountUseCase(
                    authRepository = auth,
                    userRepository = users,
                    studyingRepository = FakeStudyingRepository(),
                    ensureCurrentUserUseCase = EnsureCurrentUserUseCase(users, notifier)
                ),
                authRepository = auth,
                sessionExpiredNotifier = notifier
            )
            val events = mutableListOf<DeleteAccountEvent>()
            backgroundScope.launch(dispatcher) { viewModel.events.toList(events) }

            viewModel.reauthenticateWithGoogleAndDelete("token")
            runCurrent()

            assertEquals(
                listOf(
                    DeleteAccountEvent.ShowToast("회원탈퇴가 완료되었습니다."),
                    DeleteAccountEvent.NavigateToSignIn
                ),
                events
            )
            assertEquals(0, notifier.calls)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class CountingSessionExpiredNotifier : SessionExpiredNotifier {
        var calls = 0
        override fun notifySessionExpired(reason: SessionExpiredReason) {
            calls++
        }
    }
}
