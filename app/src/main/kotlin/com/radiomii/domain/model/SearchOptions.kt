package com.radiomii.domain.model

enum class SearchMode { TAG, NAME }
enum class TagShortcutMode { CARDS, BUTTONS }

enum class SortOrder(val apiValue: String) {
    CLICK_COUNT("clickcount"),
    VOTES("votes"),
    COUNTRY("country"),
    BITRATE("bitrate"),
}

val DEFAULT_TAG_LIST = listOf(
    "Rock", "Hard Rock", "Metal", "Alternative", "Indie", "Punk",
    "Funk", "Soul", "Reggae", "Hiphop", "House", "Electro", "Progressive",
    "Jazz", "Blues", "Salsa", "Latino", "Pop", "Disco", "Smooth Jazz",
    "Chillout", "Ambient", "World Music", "African Music", "Oldies",
    "60s", "70s", "80s", "90s", "Country", "Classical", "News",
)

data class SearchOptions(
    val searchMode: SearchMode = SearchMode.TAG,
    val country: String = "",
    val sortOrder: SortOrder = SortOrder.CLICK_COUNT,
    val bitrateMin: Int = 96,
    val reverse: Boolean = true,
    val hidebroken: Boolean = true,
    val isHttpsOnly: Boolean = false,
    val showTagButtons: Boolean = true,
    val tagOrder: List<String> = emptyList(),
    val tagShortcutMode: TagShortcutMode = TagShortcutMode.CARDS,
)
