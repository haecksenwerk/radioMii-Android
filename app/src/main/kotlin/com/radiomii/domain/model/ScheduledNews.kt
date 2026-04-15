package com.radiomii.domain.model

import kotlinx.serialization.Serializable

enum class NewsInterval { HOURLY, HALF_HOURLY }

@Serializable
data class ScheduledNews(
    val enabled: Boolean = false,
    val stationId: String = "",
    val stationName: String = "",
    val interval: String = NewsInterval.HOURLY.name,   // serialized as String for DataStore
    val durationMinutes: Int = 5,  // 0 = play until manually stopped
    val showSkipButton: Boolean = false,
) {
    val intervalEnum: NewsInterval get() = NewsInterval.valueOf(interval)
}
