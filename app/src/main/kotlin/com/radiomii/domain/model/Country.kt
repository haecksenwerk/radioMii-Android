package com.radiomii.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val name: String,
    val iso31661: String,
    val stationcount: Int,
)
