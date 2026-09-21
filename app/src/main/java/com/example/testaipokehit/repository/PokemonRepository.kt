package com.example.testaipokehit.repository

import com.example.testaipokehit.api.PokeApi
import com.example.testaipokehit.api.dto.NamedResource
import com.example.testaipokehit.api.dto.PokemonDto
import com.example.testaipokehit.api.dto.SpeciesDto
import com.example.testaipokehit.database.FavoriteDao
import com.example.testaipokehit.database.FavoriteEntity
import com.example.testaipokehit.model.Pokemon
import com.example.testaipokehit.model.PokemonStat
import com.example.testaipokehit.model.PokemonType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Единый источник данных: объединяет API и БД, кэширует имена, сведения по типам
 * и полные детали покемонов в памяти.
 */
class PokemonRepository(
    private val api: PokeApi,
    private val favoriteDao: FavoriteDao
) {

    private val namesMutex = Mutex()
    private var namesByName: Map<String, Int> = emptyMap()

    private val typesMutex = Mutex()
    private val typeMembersCache = mutableMapOf<PokemonType, List<String>>()

    private val detailsCache = mutableMapOf<Int, Pokemon>()

    /** Загружает полный список имён покемонов (~1300) порциями. */
    private suspend fun ensureNamesLoaded() = namesMutex.withLock {
        if (namesByName.isNotEmpty()) return
        val all = mutableListOf<NamedResource>()
        var offset = 0
        val pageSize = 200
        while (true) {
            val page = api.getPokemonPage(limit = pageSize, offset = offset)
            all += page.results
            offset += pageSize
            if (page.results.size < pageSize || offset > 2000) break
        }
        namesByName = all.associate { it.name to it.idFromUrl() }
    }

    suspend fun getPokemonDetailsByName(name: String): Pokemon? {
        ensureNamesLoaded()
        val id = namesByName[name] ?: return null
        return getPokemonDetails(id)
    }

    suspend fun getPokemonDetails(id: Int): Pokemon? {
        detailsCache[id]?.let { return it }
        return runCatching {
            val dto = api.getPokemon(id)
            val species = api.getPokemonSpecies(id)
            val pokemon = dto.toModel(species.englishDescription())
            detailsCache[id] = pokemon
            pokemon
        }.getOrNull()
    }

    /** Имена покемонов под поисковый запрос и пересечение выбранных типов. */
    suspend fun searchNames(query: String, types: Set<PokemonType>): List<String> {
        ensureNamesLoaded()
        val q = query.trim().lowercase()
        val names = when {
            types.isEmpty() -> namesByName.keys.toList()
            else -> namesMatchingTypes(types)
        }
        return names
            .asSequence()
            .filter { q.isEmpty() || it.contains(q) }
            .sorted()
            .toList()
    }

    private suspend fun namesMatchingTypes(types: Set<PokemonType>): List<String> {
        val sets = types.map { typeMembers(it) }.map { it.toSet() }
        var result = sets.first()
        for (set in sets.drop(1)) {
            result = result.intersect(set)
        }
        return result.toList()
    }

    private suspend fun typeMembers(type: PokemonType): List<String> = typesMutex.withLock {
        typeMembersCache[type]?.let { return it }
        val members = api.getType(type.apiName).pokemon.map { it.pokemon.name }
        typeMembersCache[type] = members
        members
    }

    // ---- Избранное ----

    fun observeFavorites(): Flow<List<Int>> = favoriteDao.observeFavoriteIds()

    suspend fun isFavorite(id: Int): Boolean =
        favoriteDao.observeFavoriteIds().first().contains(id)

    suspend fun toggleFavorite(id: Int) {
        if (isFavorite(id)) {
            favoriteDao.delete(id)
        } else {
            favoriteDao.insert(FavoriteEntity(id = id))
        }
    }

    // ---- Маппинг ----

    private fun PokemonDto.toModel(description: String?): Pokemon {
        val typeList = types.mapNotNull { PokemonType.fromApiName(it.type.name) }
        val officialArtwork = sprites.other?.officialArtwork?.frontDefault
        val gallery = buildList {
            sprites.other?.home?.frontDefault?.let(::add)
            sprites.other?.home?.frontShiny?.let(::add)
            sprites.frontShiny?.let(::add)
            sprites.other?.officialArtwork?.frontShiny?.let(::add)
        }
        return Pokemon(
            id = id,
            name = name.toTitleCase(),
            types = typeList,
            officialArtworkUrl = officialArtwork,
            galleryUrls = gallery,
            height = height / 10.0,
            weight = weight / 10.0,
            baseExperience = baseExperience,
            stats = stats.map { PokemonStat(name = it.stat.name, base = it.baseStat) },
            abilities = abilities.map { it.ability.name.toTitleCase() },
            description = description
        )
    }

    private fun SpeciesDto.englishDescription(): String? =
        flavorTextEntries
            .firstOrNull { it.language.name == "en" }
            ?.flavorText
            ?.replace("\n", " ")
            ?.replace("\r", " ")
            ?.replace("\u000C", " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun NamedResource.idFromUrl(): Int =
        url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 0

    private fun String.toTitleCase(): String =
        split(' ', '-').joinToString(" ") { word ->
            if (word.isEmpty()) "" else word.replaceFirstChar { it.uppercase() }
        }
}