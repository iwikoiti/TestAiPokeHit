package com.example.testaipokehit.model

/**
 * Доменная модель покемона. Высота — в метрах, вес — в килограммах.
 */
data class Pokemon(
    val id: Int,
    val name: String,
    val types: List<PokemonType>,
    val officialArtworkUrl: String?,
    val galleryUrls: List<String>,
    val height: Double,
    val weight: Double,
    val baseExperience: Int?,
    val stats: List<PokemonStat>,
    val abilities: List<String>,
    val description: String?
)

data class PokemonStat(
    val name: String,
    val base: Int
)