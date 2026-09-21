package com.example.testaipokehit.api.dto

import com.google.gson.annotations.SerializedName

data class NamedResource(
    val name: String,
    val url: String
)

data class PokemonListResponse(
    val count: Int,
    val results: List<NamedResource>
)

data class PokemonDto(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    @SerializedName("base_experience") val baseExperience: Int?,
    val sprites: SpritesDto,
    val stats: List<StatDto>,
    val types: List<TypeSlotDto>,
    val abilities: List<AbilitySlotDto>
)

data class SpritesDto(
    @SerializedName("front_shiny") val frontShiny: String?,
    val other: OtherSpritesDto?
)

data class OtherSpritesDto(
    @SerializedName("official-artwork") val officialArtwork: ArtworkImagesDto?,
    @SerializedName("home") val home: ArtworkImagesDto?
)

data class ArtworkImagesDto(
    @SerializedName("front_default") val frontDefault: String?,
    @SerializedName("front_shiny") val frontShiny: String?
)

data class StatDto(
    @SerializedName("base_stat") val baseStat: Int,
    val stat: NamedResource
)

data class TypeSlotDto(
    val slot: Int,
    val type: NamedResource
)

data class AbilitySlotDto(
    val ability: NamedResource,
    @SerializedName("is_hidden") val isHidden: Boolean
)

data class SpeciesDto(
    @SerializedName("flavor_text_entries") val flavorTextEntries: List<FlavorTextEntryDto>
)

data class FlavorTextEntryDto(
    @SerializedName("flavor_text") val flavorText: String,
    val language: NamedResource
)

data class TypeDetailDto(
    val pokemon: List<TypePokemonDto>
)

data class TypePokemonDto(
    val pokemon: NamedResource
)