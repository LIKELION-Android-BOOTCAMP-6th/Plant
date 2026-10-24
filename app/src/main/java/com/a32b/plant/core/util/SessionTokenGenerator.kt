package com.a32b.plant.core.util

import java.util.UUID

object SessionTokenGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}
