package com.radiomii.data.remote.dto

import com.radiomii.domain.model.Country
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountryDto(
    @SerialName("name") val name: String = "",
    @SerialName("iso_3166_1") val iso31661: String = "",
    @SerialName("stationcount") val stationcount: Int = 0,
)

fun CountryDto.toDomain() = Country(name = name, iso31661 = iso31661, stationcount = stationcount)
