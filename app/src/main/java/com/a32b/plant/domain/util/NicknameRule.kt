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

    /**
     * 입력칸에서 [MAX_LENGTH]를 넘는 입력을 막는다.
     * - 이미 꽉 찬 상태에서 더 입력하면 이전 값 유지
     *   (한글 조합 중 `각`+`ㅏ` → `가가`처럼 넘칠 때 잘라내면 받침이 사라지므로 잘라내지 않음)
     * - 여유가 있을 때 붙여넣기 등으로 넘치면 통째로 무시하지 않고 앞에서부터 [MAX_LENGTH]자까지 반영
     */
    fun limitInput(previous: String, input: String): String {
        if (input.length <= MAX_LENGTH) return input
        if (previous.length >= MAX_LENGTH) return previous

        // 이모지 같은 2칸짜리 문자가 경계에 걸리면 반쪽만 남지 않게 그 앞에서 자른다
        val end = if (input[MAX_LENGTH - 1].isHighSurrogate()) MAX_LENGTH - 1 else MAX_LENGTH
        return input.take(end)
    }

    private val reservedIdPattern = Regex("^__.*__$")

    fun isUsable(nickname: String): Boolean =
        '/' !in nickname &&
                nickname != "." &&
                nickname != ".." &&
                !reservedIdPattern.matches(nickname)
}
