package com.radiomii.data.remote.dto

import com.radiomii.domain.model.Station
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StationDto(
    @SerialName("stationuuid") val stationuuid: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    @SerialName("favicon") val favicon: String = "",
    @SerialName("homepage") val homepage: String = "",
    @SerialName("votes") val votes: Int = 0,
    @SerialName("clickcount") val clickcount: Int = 0,
    @SerialName("codec") val codec: String = "",
    @SerialName("bitrate") val bitrate: Int = 0,
    @SerialName("tags") val tags: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("countrycode") val countrycode: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("language") val language: String = "",
)

/** Removes bracketed, parenthesised and pipe-separated meta-info from station names. */
private fun getPlainName(name: String): String {
    // Remove meta info in [] brackets, e.g. "Radio X [MP3 128]"
    var plain = name.replace(Regex(""" \[[^]]*]"""), "")
    // Remove meta info in () parentheses, e.g. "Radio X (Germany)"
    plain = plain.replace(Regex(""" \([^)]*\)"""), "")
    // Remove everything after the first '|', e.g. "RadioX | MP3 128 | POP"
    plain = plain.split("|")[0]
    return plain.trim()
}

fun StationDto.toDomain() = Station(
    stationuuid = stationuuid,
    name = getPlainName(name),
    url = url,
    urlResolved = urlResolved,
    favicon = favicon,
    homepage = homepage,
    votes = votes,
    clickcount = clickcount,
    codec = codec,
    bitrate = bitrate,
    tags = tags,
    country = country,
    countrycode = countrycode,
    state = state,
    language = language,
)
