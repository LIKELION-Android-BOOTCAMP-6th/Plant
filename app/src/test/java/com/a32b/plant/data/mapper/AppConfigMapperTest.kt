package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.AppConfigDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigMapperTest {
    @Test
    fun integerMinVersionCodeIsRead() {
        // Firestore는 정수를 Long으로 돌려준다
        val dto = AppConfigDto(minVersionCode = 3L, message = "업데이트해 주세요")
        val policy = dto.toDomain()
        assertEquals(3L, policy.minVersionCode)
        assertEquals("업데이트해 주세요", policy.message)
        assertFalse(dto.hasInvalidMinVersionCode())
    }

    @Test
    fun wholeNumberDoubleIsReadAsInteger() {
        val dto = AppConfigDto(minVersionCode = 3.0)
        assertEquals(3L, dto.toDomain().minVersionCode)
        assertFalse(dto.hasInvalidMinVersionCode())
    }

    @Test
    fun nonIntegerMinVersionCodeIsNullAndInvalid() {
        for (value in listOf<Any>("3", 3.5, Double.NaN, Double.POSITIVE_INFINITY, true, listOf(3L), mapOf("v" to 3L))) {
            val dto = AppConfigDto(minVersionCode = value)
            assertNull("값: $value", dto.toDomain().minVersionCode)
            assertTrue("값: $value", dto.hasInvalidMinVersionCode())
        }
    }

    @Test
    fun missingMinVersionCodeIsNullButNotInvalid() {
        val dto = AppConfigDto(minVersionCode = null, message = "안내")
        assertNull(dto.toDomain().minVersionCode)
        assertFalse(dto.hasInvalidMinVersionCode())
    }

    @Test
    fun blankOrNonStringMessageIsNull() {
        for (value in listOf<Any?>(null, "", "   ", 3L, true)) {
            assertNull("값: $value", AppConfigDto(minVersionCode = 1L, message = value).toDomain().message)
        }
    }

    @Test
    fun messageIsTrimmed() {
        assertEquals("안내", AppConfigDto(message = "  안내 \n").toDomain().message)
    }
}
