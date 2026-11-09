package com.a32b.plant.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 닉네임 규칙 테스트.
 *
 * 보안 규칙(firestore.rules의 isValidNickname)은 "앱이 허용하는 닉네임은 거부하지 않는다"는 전제로 작성되어 있다 (#535).
 * 그래서 허용·거부 값은 규칙 테스트(rules-test/test/others.test.js의 nicknames)와 **같은 값**을 쓴다.
 * 한쪽 값을 바꾸면 다른 쪽도 함께 바꿀 것.
 *
 * 유닛 테스트는 JVM(JDK)의 유니코드 데이터로, 실제 앱은 기기(Android ICU)의 유니코드 데이터로 문자 종류를 판단한다.
 * 버전에 따라 분류가 바뀌는 최근 추가 문자는 테스트 값에 넣지 않는다.
 */
class NicknameRuleTest {

    // rules-test: "정상 닉네임(공백 1칸, 키보드 자모, 이모지 조합 포함)은 선점할 수 있다"와 같은 값
    private val allowed = listOf(
        "새싹",
        "새 싹",
        "ㅋㅋ", // 키보드 자모 U+3131–318E
        "Plant_01",
        "🌱새싹",
        "❤️하트", // 모양 선택 문자 U+FE0F
        "1️⃣번", // 키캡 (숫자 + U+FE0F + U+20E3)
        "👍🏻좋아", // 피부색 변경 문자
        "👨‍👩‍👧가족", // 결합 문자 ZWJ U+200D
        "❤️‍🔥불꽃", // 모양 선택 문자 뒤 ZWJ
    )

    @Test
    fun `nicknames allowed by the security rules are usable`() {
        allowed.forEach { nickname ->
            assertTrue("허용되어야 함: ${nickname.codePointsHex()}", NicknameRule.isUsable(nickname))
        }
    }

    // rules-test: "앞뒤 공백·연속 공백이 있으면 거부"와 같은 값
    @Test
    fun `leading, trailing and double spaces are not usable`() {
        listOf("새싹 ", " 새싹", "새  싹", "\t새싹").forEach { nickname ->
            assertFalse("거부되어야 함: ${nickname.codePointsHex()}", NicknameRule.isUsable(nickname))
        }
    }

    // rules-test: "보이지 않는 문자·일반 공백 외 공백 문자가 있으면 거부"와 같은 값
    @Test
    fun `invisible characters and non-ascii spaces are not usable`() {
        listOf(
            "ㅤ새싹", // 한글 채움 문자
            "ᅟ새싹", // 한글 초성 채움 문자
            "새​싹", // 폭 없는 공백
            "새⁠싹", // 단어 결합 문자
            "‮새싹", // 방향 제어 문자
            "﻿새싹", // BOM
            "새­싹", // 소프트 하이픈
            "새 싹", // 줄바꿈 없는 공백
            "새　싹", // 전각 공백
            "새⠀싹", // 점자 공백
            "새\n싹", // 제어 문자
            "새\u0007싹", // 제어 문자
            "새싹", // 사용자 정의 영역
            "새${String(Character.toChars(0xF0000))}싹", // 보조 사용자 정의 영역
            "새${String(Character.toChars(0xE0041))}싹", // 태그 문자
            "새${String(Character.toChars(0x1D173))}싹", // 음악 서식 문자
        ).forEach { nickname ->
            assertFalse("거부되어야 함: ${nickname.codePointsHex()}", NicknameRule.isUsable(nickname))
        }
    }

    // rules-test: "분리형 한글 자모가 있으면 거부"와 같은 값
    @Test
    fun `conjoining jamo left after normalization are not usable`() {
        listOf(
            "새싹", // 자모로 나뉜 "새싹" (normalize를 거치지 않은 값)
            "새ᄀ", // 혼자 있는 초성 — "새ㄱ"처럼 보임
            "새ꥠ싹", // 옛한글 초성 확장
            "새ힰ싹", // 옛한글 중성 확장
        ).forEach { nickname ->
            assertFalse("거부되어야 함: ${nickname.codePointsHex()}", NicknameRule.isUsable(nickname))
        }
    }

    // rules-test의 "[알려진 한계 - 앱이 막음]" 테스트가 기대는 전제.
    // 규칙은 결합 문자·모양 선택 문자의 위치를 판단하지 않으므로 이 값들은 앱에서만 막힌다
    @Test
    fun `joiner and variation selector between non-emoji letters are not usable`() {
        listOf("새‍싹", "새️싹").forEach { nickname ->
            assertFalse("거부되어야 함: ${nickname.codePointsHex()}", NicknameRule.isUsable(nickname))
        }
    }

    @Test
    fun `normalize trims and composes conjoining jamo but keeps keyboard jamo`() {
        assertEquals("새싹", NicknameRule.normalize(" 새싹 "))
        assertEquals("새싹", NicknameRule.normalize("새싹"))
        // NFKC를 쓰면 키보드 자모 "ㅋ"(U+314B)가 분리형 자모로 바뀌어 거부되므로 그대로 남아야 한다
        assertEquals("ㅋㅋ", NicknameRule.normalize("ㅋㅋ"))
    }

    @Test
    fun `normalized decomposed hangul becomes usable`() {
        assertTrue(NicknameRule.isUsable(NicknameRule.normalize("새싹")))
    }

    @Test
    fun `validate returns length message for too short or too long nickname`() {
        listOf("a", "가나다라마바사아자차카", "").forEach { nickname ->
            assertEquals(NicknameRule.LENGTH_MESSAGE, NicknameRule.validate(nickname))
        }
    }

    @Test
    fun `validate returns unusable message for unusable nickname`() {
        // 길이는 맞지만 문서 ID로 쓸 수 없거나(`/`, `..`, `__xx__`), 보이지 않는 문자(U+200B)가 섞인 값
        listOf("새/싹", "..", "__ab__", "새​싹").forEach { nickname ->
            assertEquals(NicknameRule.UNUSABLE_MESSAGE, NicknameRule.validate(nickname))
        }
    }

    @Test
    fun `validate returns null for valid nickname`() {
        allowed.forEach { nickname ->
            assertNull("통과해야 함: ${nickname.codePointsHex()}", NicknameRule.validate(nickname))
        }
    }

    @Test
    fun `validate checks length before usability`() {
        // "/"는 사용할 수 없는 값이면서 1글자라 길이에도 걸린다 → 길이 메시지가 먼저
        assertEquals(NicknameRule.LENGTH_MESSAGE, NicknameRule.validate("/"))
        // "."도 마찬가지
        assertEquals(NicknameRule.LENGTH_MESSAGE, NicknameRule.validate("."))
    }

    private fun String.codePointsHex(): String =
        codePoints().toArray().joinToString(" ") { "U+%04X".format(it) }
}
