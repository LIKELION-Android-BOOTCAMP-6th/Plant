package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.UserDto
import com.a32b.plant.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserMapperTest {
    @Test
    fun missingUnlockedProfileLevelMapsToZero() {
        val user = UserDto(unlockedProfileLevel = null).toDomain()

        assertEquals(0, user.unlockedProfileLevel)
    }

    @Test
    fun storedUnlockedProfileLevelIsPreserved() {
        val user = UserDto(unlockedProfileLevel = 3).toDomain()

        assertEquals(3, user.unlockedProfileLevel)
    }

    @Test
    fun newUserIsSavedWithUnlockedProfileLevelZero() {
        val dto = User.create().toDto()

        assertEquals(0, dto.unlockedProfileLevel)
    }

    @Test
    fun updatedUserUnlockedProfileLevelIsPreservedWhenSaved() {
        val dto = User.create().copy(unlockedProfileLevel = 3).toDto()

        assertEquals(3, dto.unlockedProfileLevel)
    }
}
