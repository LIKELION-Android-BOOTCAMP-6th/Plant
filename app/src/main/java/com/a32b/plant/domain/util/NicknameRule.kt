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

    /**
     * 사용할 수 있는 닉네임인지 확인한다. 앞뒤 공백을 잘라낸 값을 넘겨야 한다.
     * - Firestore 문서 ID로 쓸 수 없는 값
     * - 화면에 보이지 않는 문자가 섞여 다른 닉네임과 똑같아 보이는 값 ([hasLookAlikeChar])
     */
    fun isUsable(nickname: String): Boolean =
        '/' !in nickname &&
                nickname != "." &&
                nickname != ".." &&
                !reservedIdPattern.matches(nickname) &&
                !hasLookAlikeChar(nickname)

    private const val SPACE = ' '.code
    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val COMBINING_KEYCAP = 0x20E3
    private const val COMBINING_GRAPHEME_JOINER = 0x034F
    private const val BRAILLE_BLANK = 0x2800

    // 글자 모양이 없는 한글 채움 문자 (초성·중성 채움, 호환용 채움, 반각 채움)
    private val hangulFillers = setOf(0x115F, 0x1160, 0x3164, 0xFFA0)
    private val mongolianVariationSelectors = 0x180B..0x180F

    /**
     * 보이지 않는 문자나 다른 공백 문자가 섞여, 다른 닉네임과 똑같아 보일 수 있는지 확인한다.
     * (`"ㅤ새싹"`, `"새​싹"`, `"새 싹"`, `"새  싹"` 등이 `"새싹"`, `"새 싹"`처럼 보이는 것을 막음)
     *
     * 이모지 닉네임은 허용해야 하므로 이모지를 만드는 문자는 이모지 사이에 있을 때만 허용한다.
     * - 결합 문자(ZWJ, 👨‍👩‍👧): 양옆이 모두 이모지일 때
     * - 모양 선택 문자(VS, ❤️): 이모지 뒤, 또는 키캡(1️⃣) 앞
     *
     * 태그 문자를 쓰는 일부 국기(잉글랜드·스코틀랜드·웨일스)는 서식 문자라 함께 막힌다.
     */
    private fun hasLookAlikeChar(nickname: String): Boolean {
        val codePoints = nickname.toCodePoints()
        return codePoints.indices.any { i ->
            val cp = codePoints[i]
            val prev = codePoints.getOrNull(i - 1)
            val next = codePoints.getOrNull(i + 1)
            when {
                cp == SPACE -> prev == SPACE
                cp.isSpace() -> true
                cp in hangulFillers || cp == BRAILLE_BLANK -> true
                cp == COMBINING_GRAPHEME_JOINER || cp in mongolianVariationSelectors -> true
                // ❤️‍🔥처럼 모양 선택 문자 뒤에 결합 문자가 오는 경우도 허용 (그 모양 선택 문자는 자기 차례에 따로 검사됨)
                cp == ZERO_WIDTH_JOINER ->
                    !((prev.isEmoji() || prev?.isVariationSelector() == true) && next.isEmoji())
                cp.isVariationSelector() ->
                    !(prev.isEmoji() || (prev.isKeycapBase() && next == COMBINING_KEYCAP))
                else -> Character.getType(cp).let {
                    it == Character.FORMAT.toInt() || it == Character.CONTROL.toInt()
                }
            }
        }
    }

    private fun String.toCodePoints(): List<Int> {
        val result = mutableListOf<Int>()
        var index = 0
        while (index < length) {
            val cp = codePointAt(index)
            result += cp
            index += Character.charCount(cp)
        }
        return result
    }

    private fun Int.isSpace(): Boolean = Character.isWhitespace(this) || Character.isSpaceChar(this)

    private fun Int.isVariationSelector(): Boolean = this in 0xFE00..0xFE0F || this in 0xE0100..0xE01EF

    // 그림 이모지 전용 영역. 피부색 변경 문자(U+1F3FB~1F3FF)도 여기 포함된다.
    // 기기의 유니코드 버전보다 새로 나온 이모지는 '미지정 문자'로 판정되므로, 문자 종류 대신 영역으로 확인한다.
    private val emojiBlocks = 0x1F000..0x1FAFF

    // 이모지 본체: 기타 기호(❤ ♀ © 등) 또는 그림 이모지 영역
    // (수식 기호까지 넓히면 `^` 같은 ASCII 기호 사이에 숨긴 결합 문자까지 통과하므로 넣지 않음)
    private fun Int?.isEmoji(): Boolean =
        this != null && (Character.getType(this) == Character.OTHER_SYMBOL.toInt() || this in emojiBlocks)

    private fun Int?.isKeycapBase(): Boolean =
        this != null && (this in '0'.code..'9'.code || this == '#'.code || this == '*'.code)
}
