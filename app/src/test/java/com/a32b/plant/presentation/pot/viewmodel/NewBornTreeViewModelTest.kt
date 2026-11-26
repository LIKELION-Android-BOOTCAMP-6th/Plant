package com.a32b.plant.presentation.pot.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.result.Result
import com.a32b.plant.fake.FakePotRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
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

/** 새 화분 만들기 완료 버튼 연타 (#672) */
@OptIn(ExperimentalCoroutinesApi::class)
class NewBornTreeViewModelTest {

    @Test
    fun `저장을 기다리는 중 다시 눌러도 화분은 한 번만 만든다`() = runPotTest { repository, viewModel ->
        val gate = CompletableDeferred<Unit>()
        repository.addPotGate = gate
        val notices = mutableListOf<String?>()

        viewModel.createPot(TAG, "화분") { notices += it }
        viewModel.createPot(TAG, "화분") { notices += it }   // 저장 대기 중 (게이트 미완료)
        runCurrent()
        assertTrue(viewModel.isUploading.value)
        gate.complete(Unit)
        runCurrent()

        assertEquals(listOf(Triple(UID, TAG, "화분")), repository.addedPots)
        assertEquals(listOf<String?>(null), notices)
        assertFalse(viewModel.isUploading.value)
    }

    @Test
    fun `저장이 실패하면 다시 눌러 재시도할 수 있다`() = runPotTest { repository, viewModel ->
        repository.addPotResult = Result.Failure(AppError.Network())
        val notices = mutableListOf<String?>()

        viewModel.createPot(TAG, "화분") { notices += it }
        runCurrent()
        repository.addPotResult = Result.Success(SaveResult.Saved)
        viewModel.createPot(TAG, "화분") { notices += it }
        runCurrent()

        assertEquals(2, repository.addedPots.size)
        assertEquals(listOf<String?>(null), notices)
    }

    private fun runPotTest(
        block: suspend TestScope.(FakePotRepository, NewBornTreeViewModel) -> Unit
    ) = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            // NewBornTreeViewModel은 아직 전역 CurrentUser에서 uid를 읽는다 (#629)
            CurrentUser.set(UserModel(uid = UID))
            val repository = FakePotRepository(
                tagsFlow = flowOf(listOf(TAG)),
                addPotResult = Result.Success(SaveResult.Saved)
            )
            val viewModel = NewBornTreeViewModel(repository)
            runCurrent()   // 태그 구독

            block(repository, viewModel)

            viewModel.viewModelScope.cancel()
        } finally {
            CurrentUser.clear()
            Dispatchers.resetMain()
        }
    }

    private companion object {
        const val UID = "uid-1"
        val TAG = Tag(id = "tag1", name = "공부")
    }
}
