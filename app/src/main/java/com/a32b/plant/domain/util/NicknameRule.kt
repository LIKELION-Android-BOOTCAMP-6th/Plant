package com.a32b.plant.domain.util

/**
 * 닉네임 입력 규칙 — 로그인(닉네임 설정)과 마이페이지(프로필 편집)가 함께 사용한다.
 *
 * 닉네임은 Firestore 문서 ID(`nicknames/{nickname}`)로 쓰이므로,
 * 문서 ID로 사용할 수 없는 값은 입력 단계에서 걸러 입력칸 에러로 안내한다.
 * (걸러지지 않으면 저장 단계에서 예외가 나 상황 에러(토스트)로 오인된다)
 */
object NicknameRule {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 10
    const val LENGTH_MESSAGE = "${MIN_LENGTH}~${MAX_LENGTH}자로 입력해주세요."
    const val UNUSABLE_MESSAGE = "사용할 수 없는 닉네임입니다."

    fun isValidLength(nickname: String): Boolean =
        nickname.length in MIN_LENGTH..MAX_LENGTH

    private val reservedIdPattern = Regex("^__.*__$")

    fun isUsable(nickname: String): Boolean =
        '/' !in nickname &&
                nickname != "." &&
                nickname != ".." &&
                !reservedIdPattern.matches(nickname)
}
