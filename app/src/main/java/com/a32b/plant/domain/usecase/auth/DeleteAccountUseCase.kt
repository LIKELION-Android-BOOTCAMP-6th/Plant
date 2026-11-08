package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

/**
 * 회원탈퇴. Google 재인증이 끝난 뒤 호출한다.
 * 데이터·닉네임·Auth 계정 삭제는 서버 함수(deleteAccount)가 처리하고, 앱은 함수 호출과 로컬 정리만 한다 (#457).
 */
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val studyingRepository: StudyingRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(): Result<Unit> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return result // UnknownUser — 세션 만료 이벤트 이미 발송됨
        }

        // 서버는 최근에 재인증했는지(토큰의 auth_time)를 확인한다.
        // 재인증 직후에도 기존 토큰이 그대로 쓰일 수 있으므로 새 토큰을 받은 뒤 호출한다.
        when (val refreshResult = authRepository.refreshIdToken()) {
            is Result.Success -> Unit
            is Result.Failure -> {
                // 이전 요청에서 서버가 Auth 계정까지 지웠는데 응답만 받지 못한 경우 → 탈퇴 완료로 처리
                if (refreshResult.error is AppError.AccountNotFound) return finishLocally()
                return refreshResult
            }
        }

        // 서버가 users/{uid}를 지우는 동안 실시간 구독이 살아 있으면 문서 삭제를 세션 만료로 받아들이므로
        // 함수 호출 전에 구독을 끊는다. 탈퇴가 끝나지 않으면(실패·취소) 세션을 다시 시작한다.
        userRepository.endUserSession()
        var deleted = false
        try {
            val deleteResult = authRepository.deleteAccount()
            if (deleteResult is Result.Failure) return deleteResult
            deleted = true
        } finally {
            // 서버가 users/{uid}까지 지운 뒤 실패했다면, 다시 시작한 구독이 문서 없음을 받아
            // 세션 만료(로그인 화면)로 이어진다.
            if (!deleted) userRepository.startUserSession(user)
        }

        return finishLocally()
    }

    /** 서버에서 탈퇴가 끝난 뒤 기기에 남은 로그인 정보와 설정을 정리한다. */
    private suspend fun finishLocally(): Result<Unit> {
        userRepository.endUserSession()
        // TODO: CurrentUser 전역 싱글톤 제거 대상.
        //  아직 CurrentUser를 직접 읽는 화면이 남아 있어 과도기 동안 함께 초기화한다.
        //  모든 화면이 UserRepository로 전환되면 이 줄과 di/CurrentUser.kt를 함께 삭제할 것.
        CurrentUser.clear()
        // 서버에서 Auth 계정을 지워도 기기에는 로그인 상태가 남아 있으므로 직접 로그아웃한다.
        authRepository.signOut()

        // 저장 실패는 Repository에서 로그를 남기며, 완료된 회원탈퇴 결과는 유지한다.
        userRepository.updateDarkMode(false)
        studyingRepository.clearLocalSession()

        return Result.Success(Unit)
    }
}
