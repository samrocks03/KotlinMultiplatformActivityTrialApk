package com.example.kmp_basic_app.data.remote.mapper

import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.model.CharacterPage
import com.example.kmp_basic_app.domain.model.CharacterStatus
import com.example.kmp_basic_app.domain.model.Episode
import com.example.kmp_basic_app.domain.model.LocationBrief
import com.example.kmp_basic_app.domain.model.LocationDetail
import com.example.kmp_basic_app.domain.model.PageInfo
import com.example.kmp_basic_app.graphql.rickandmorty.GetCharacterDetailQuery
import com.example.kmp_basic_app.graphql.rickandmorty.GetCharactersQuery

fun GetCharactersQuery.Data.toDomain(): CharacterPage {
    val info = characters?.info
    val results = characters?.resultsFilterNotNull()?.map { result ->
        Character(
            id = result.id ?: "",
            name = result.name ?: "",
            status = CharacterStatus.fromString(result.status ?: "unknown"),
            species = result.species ?: "",
            gender = result.gender ?: "",
            origin = LocationBrief(
                id = result.origin?.id,
                name = result.origin?.name ?: "Unknown"
            ),
            location = LocationBrief(
                id = result.location?.id,
                name = result.location?.name ?: "Unknown"
            ),
            imageUrl = result.image ?: "",
            episodeIds = result.episodeFilterNotNull().mapNotNull { it.id }
        )
    } ?: emptyList()

    return CharacterPage(
        info = PageInfo(
            count = info?.count ?: 0,
            pages = info?.pages ?: 0,
            next = info?.next
        ),
        results = results
    )
}

fun GetCharacterDetailQuery.Data.toDomain(): CharacterDetail {
    val c = character ?: throw IllegalStateException("Character not found")
    return CharacterDetail(
        character = Character(
            id = c.id ?: "",
            name = c.name ?: "",
            status = CharacterStatus.fromString(c.status ?: "unknown"),
            species = c.species ?: "",
            gender = c.gender ?: "",
            origin = LocationBrief(
                id = c.origin?.id,
                name = c.origin?.name ?: "Unknown"
            ),
            location = LocationBrief(
                id = c.location?.id,
                name = c.location?.name ?: "Unknown"
            ),
            imageUrl = c.image ?: "",
            episodeIds = c.episodeFilterNotNull().mapNotNull { it.id }
        ),
        origin = LocationDetail(
            id = c.origin?.id,
            name = c.origin?.name ?: "Unknown",
            type = c.origin?.type ?: "",
            dimension = c.origin?.dimension ?: ""
        ),
        location = LocationDetail(
            id = c.location?.id,
            name = c.location?.name ?: "Unknown",
            type = c.location?.type ?: "",
            dimension = c.location?.dimension ?: ""
        ),
        episodes = c.episodeFilterNotNull().map { ep ->
            Episode(
                id = ep.id ?: "",
                name = ep.name ?: "",
                airDate = ep.air_date ?: "",
                episode = ep.episode ?: ""
            )
        }
    )
}
