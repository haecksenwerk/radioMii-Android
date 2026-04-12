package com.radiomii.domain.model

data class IcyMetadata(
    val artist: String = "",
    val title: String = "",
    val rawTitle: String = "",
) {
    val hasContent: Boolean get() = artist.isNotEmpty() || title.isNotEmpty()
    val display: String get() = when {
        artist.isNotEmpty() && title.isNotEmpty() -> "$artist – $title"
        title.isNotEmpty() -> title
        rawTitle.isNotEmpty() -> rawTitle
        else -> ""
    }
}

