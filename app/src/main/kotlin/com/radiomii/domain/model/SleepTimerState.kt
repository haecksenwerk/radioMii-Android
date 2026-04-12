package com.radiomii.domain.model

data class SleepTimerState(
    val isActive: Boolean = false,
    val endTimeMs: Long = 0L,
    val selectedMinutes: Int = 30,
    val cancelOnStop: Boolean = false,
    // Stored explicitly so StateFlow emits on every tick (computed properties
    // change silently without emitting, as StateFlow uses structural equality).
    val remainingMs: Long = 0L,
) {
    val remainingMinutes: Int get() = (remainingMs / 60_000).toInt()
    val progress: Float get() = if (!isActive || selectedMinutes <= 0) 0f
        else remainingMs / (selectedMinutes * 60_000f)
}
