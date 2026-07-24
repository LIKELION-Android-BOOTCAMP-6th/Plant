package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.Result
import com.a32b.plant.origin.OldUserRepository
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class UpdateDarkModeUseCase @Inject constructor(
    // TODO: UserRepository의 다크모드 갱신 API가 확정되면
    // OldUserRepository, CurrentUser 의존성을 제거한다.
    private val userRepository: OldUserRepository
) {
    suspend operator fun invoke(isDarkMode: Boolean): Result<Unit> {
        return try {
            userRepository.updateIsDarkMode(
                uid = CurrentUser.uid,
                state = isDarkMode
            )
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseNetworkException) {
            Result.Failure(AppError.Network())
        } catch (_: Exception) {
            Result.Failure(AppError.Custom("다크모드 설정 변경에 실패했습니다."))
        }
    }
}
