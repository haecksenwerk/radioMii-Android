package com.radiomii.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteResultDto(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("message") val message: String = "",
)

