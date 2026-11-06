package com.a32b.plant.domain.util

import java.text.Normalizer

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
     * 입력값을 검사·저장할 형태로 바꾼다. 검사([isValidLength], [isUsable]) 전에 반드시 이 값으로 바꿔서 넘긴다.
     * - 앞뒤 공백 제거
     * - NFC 정규화: 자모로 나뉜 한글(맥에서 복사한 글 등, 예: U+1109 U+1162 U+110A U+1161 U+11A8)을
     *   완성형("새싹", U+C0C8 U+C2F9)으로 합쳐, 같은 모습의 닉네임이 다른 값으로 등록되지 않게 한다.
     *   (NFKC는 쓰지 않는다. 키보드로 입력한 "ㅋ"(U+314B) 같은 자모까지 분리형 자모로 바꿔 [hasLookAlikeChar]에 걸림)
     */
    fun normalize(input: String): String = Normalizer.normalize(input.trim(), Normalizer.Form.NFC)

    /**
     * 사용할 수 있는 닉네임인지 확인한다.
     * - [normalize]를 거치지 않은 값 (호출하는 쪽에서 정규화를 빠뜨려도 걸러지도록 여기서도 확인)
     * - Firestore 문서 ID로 쓸 수 없는 값
     * - 화면에 보이지 않는 문자가 섞여 다른 닉네임과 똑같아 보이는 값 ([hasLookAlikeChar])
     */
    fun isUsable(nickname: String): Boolean =
        nickname == normalize(nickname) &&
                '/' !in nickname &&
                nickname != "." &&
                nickname != ".." &&
                !reservedIdPattern.matches(nickname) &&
                !hasLookAlikeChar(nickname)

    private const val SPACE = ' '.code
    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val COMBINING_KEYCAP = 0x20E3
    private const val BRAILLE_BLANK = 0x2800

    // 유니코드 표준이 '기본적으로 화면에 표시하지 않는 문자'로 정한 범위 (Default_Ignorable_Code_Point)
    // 한글 채움 문자(U+115F, U+1160, U+3164, U+FFA0), 폭 없는 공백(U+200B), 방향 제어 문자(U+202A~202E),
    // 결합 문자(U+200D), 모양 선택 문자(U+FE00~FE0F), 태그 문자(U+E0000~E007F), 아직 배정되지 않은 예약 문자 등
    private val defaultIgnorables = listOf(
        0x00AD..0x00AD, 0x034F..0x034F, 0x061C..0x061C, 0x115F..0x1160, 0x17B4..0x17B5,
        0x180B..0x180F, 0x200B..0x200F, 0x202A..0x202E, 0x2060..0x206F, 0x3164..0x3164,
        0xFE00..0xFE0F, 0xFEFF..0xFEFF, 0xFFA0..0xFFA0, 0xFFF0..0xFFF8, 0x1BCA0..0x1BCA3,
        0x1D173..0x1D17A, 0xE0000..0xE0FFF
    )

    // 분리형 한글 자모 (초성·중성·종성, 옛한글 확장 포함)
    // [normalize] 후에도 남아 있다면 완성형으로 합쳐지지 않는 조각이다.
    // 혼자 있는 초성 U+1100은 키보드로 입력하는 "ㄱ"(U+3131)과 거의 똑같이 보이므로 거부한다.
    // (한국어 키보드는 완성형 글자와 U+3131~318E 자모만 입력하므로 일반 입력에는 영향 없음)
    private val conjoiningJamo = listOf(0x1100..0x11FF, 0xA960..0xA97F, 0xD7B0..0xD7FF)

    // 서식·제어 문자, 짝이 맞지 않는 반쪽 문자(서로게이트), 기기 글꼴마다 다르게 보이는 사용자 정의 영역 문자
    private val blockedTypes = setOf(
        Character.FORMAT, Character.CONTROL, Character.SURROGATE, Character.PRIVATE_USE
    )

    /**
     * 보이지 않는 문자나 다른 공백 문자가 섞여, 다른 닉네임과 똑같아 보일 수 있는지 확인한다.
     * 예: "새"+U+200B+"싹", U+3164+"새싹" → "새싹"처럼 보임 / "새"+U+00A0+"싹", "새"+공백 2칸+"싹" → "새 싹"처럼 보임
     *     / "새"+U+1100 → "새ㄱ"처럼 보임 (정규화 후에도 남은 분리형 자모)
     *
     * 이모지 닉네임은 허용해야 하므로, 이모지를 만드는 문자는 [defaultIgnorables]보다 먼저 검사해 이모지 사이에 있을 때만 허용한다.
     * (순서가 바뀌면 두 문자가 [defaultIgnorables]에 걸려 모든 이모지 조합이 막힘)
     * - 결합 문자(ZWJ, U+200D, 가족 이모지처럼 이모지를 이어 붙임): 앞이 이모지(또는 모양 선택 문자)이고 뒤가 이모지일 때
     * - 모양 선택 문자(VS, U+FE00~FE0F 등, 하트를 컬러 이모지로 표시): 이모지 뒤, 또는 키캡(숫자+VS+U+20E3) 안
     *
     * 알려진 한계
     * - 태그 문자를 쓰는 일부 국기(잉글랜드·스코틀랜드·웨일스)는 함께 막힌다.
     * - "°"+ZWJ+"°"처럼 이모지가 아닌 기타 기호 사이의 결합 문자는 통과한다.
     *   정확히 막으려면 문자별 이모지 속성 정보가 필요하고, 좁은 목록으로 막으면 정상 이모지 조합이 깨지므로 두지 않는다.
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
                cp == BRAILLE_BLANK -> true
                // 불타는 하트처럼 모양 선택 문자 뒤에 결합 문자가 오는 경우도 허용 (그 모양 선택 문자는 자기 차례에 따로 검사됨)
                cp == ZERO_WIDTH_JOINER ->
                    !((prev.isEmoji() || prev?.isVariationSelector() == true) && next.isEmoji())
                cp.isVariationSelector() ->
                    !(prev.isEmoji() || (prev.isKeycapBase() && next == COMBINING_KEYCAP))
                defaultIgnorables.any { cp in it } -> true
                conjoiningJamo.any { cp in it } -> true
                else -> Character.getType(cp).toByte() in blockedTypes
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

    // 이모지 본체: 기타 기호(U+2764 하트, U+2640 여성 기호, U+00A9 저작권 기호 등) 또는 그림 이모지 영역
    // (수식 기호까지 넓히면 `^` 같은 ASCII 기호 사이에 숨긴 결합 문자까지 통과하므로 넣지 않음)
    private fun Int?.isEmoji(): Boolean =
        this != null && (Character.getType(this) == Character.OTHER_SYMBOL.toInt() || this in emojiBlocks)

    private fun Int?.isKeycapBase(): Boolean =
        this != null && (this in '0'.code..'9'.code || this == '#'.code || this == '*'.code)
}
