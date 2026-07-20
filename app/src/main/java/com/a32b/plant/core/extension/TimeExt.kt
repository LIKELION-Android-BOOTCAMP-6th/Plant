package com.a32b.plant.core.extension

import com.google.firebase.Timestamp


// Timestamp -> Long
fun Timestamp?.toLong() : Long = this?.toDate()?.time ?: 0L

// Long -> Timestamp
fun Long?.toTimestamp(): Timestamp {
    return this?.let {
        Timestamp(it / 1000, ((it % 1000) * 1000000).toInt())
    } ?: Timestamp.now()
}
