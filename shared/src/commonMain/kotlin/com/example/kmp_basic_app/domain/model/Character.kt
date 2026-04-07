package com.example.kmp_basic_app.domain.model

data class Character(
    val id: String,
    val name: String,
    val status: CharacterStatus,
    val species: String,
    val gender: String,
    val origin: LocationBrief,
    val location: LocationBrief,
    val imageUrl: String,
    val episodeIds: List<String>,
    val isFavorite: Boolean = false
)

enum class CharacterStatus {
    ALIVE, DEAD, UNKNOWN;

    companion object {
        fun fromString(value: String): CharacterStatus = when (value.lowercase()) {
            "alive" -> ALIVE
            "dead" -> DEAD
            else -> UNKNOWN
        }
    }
}

data class LocationBrief(
    val id: String?,
    val name: String
)

data class LocationDetail(
    val id: String?,
    val name: String,
    val type: String,
    val dimension: String
)

data class Episode(
    val id: String,
    val name: String,
    val airDate: String,
    val episode: String
)

data class CharacterDetail(
    val character: Character,
    val origin: LocationDetail,
    val location: LocationDetail,
    val episodes: List<Episode>
)

data class CharacterPage(
    val info: PageInfo,
    val results: List<Character>
)

data class PageInfo(
    val count: Int,
    val pages: Int,
    val next: Int?
)
