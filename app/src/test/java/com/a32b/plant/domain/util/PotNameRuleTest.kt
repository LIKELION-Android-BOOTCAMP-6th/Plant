package com.a32b.plant.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PotNameRuleTest {

    @Test
    fun normalizeRemovesLeadingAndTrailingWhitespace() {
        assertEquals("새싹", PotNameRule.normalize("  새싹  "))
        assertEquals("새싹", PotNameRule.normalize("\t새싹\n"))
    }

    // 가운데 공백은 이번 범위(#563)에서 정리하지 않는다
    @Test
    fun normalizeKeepsInnerWhitespace() {
        assertEquals("새  싹", PotNameRule.normalize(" 새  싹 "))
    }

    @Test
    fun normalizeTurnsBlankIntoEmpty() {
        assertEquals("", PotNameRule.normalize("   "))
    }
}
