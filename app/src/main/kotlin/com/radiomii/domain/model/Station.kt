package com.radiomii.domain.model

import kotlinx.serialization.Serializable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Transient

@Immutable
@Serializable
data class Station(
    val stationuuid: String = "",
    val name: String = "",
    val url: String = "",
    val urlResolved: String = "",
    val favicon: String = "",
    val homepage: String = "",
    val votes: Int = 0,
    val clickcount: Int = 0,
    val codec: String = "",
    val bitrate: Int = 0,
    val tags: String = "",        // comma-separated
    val country: String = "",
    val countrycode: String = "",
    val state: String = "",
    val language: String = "",
    val isCustom: Boolean = false,
) {
    val streamUrl: String get() = urlResolved.ifBlank { url }

    @Transient
    val tagList: ImmutableList<String> = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toImmutableList()
}
