package com.example.testaipokehit.mvi

import com.example.testaipokehit.model.Pokemon
import com.example.testaipokehit.model.PokemonType

data class HomeState(
    val pokemon: List<Pokemon> = emptyList(),
    val query: String = "",
    val selectedTypes: Set<PokemonType> = emptySet(),
    val favorites: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true
)

sealed interface HomeIntent {
    data class Search(val query: String) : HomeIntent
    data class ApplyFilter(val types: Set<PokemonType>) : HomeIntent
    data object ResetFilter : HomeIntent
    data object Reload : HomeIntent
    data class ToggleFavorite(val id: Int) : HomeIntent
    data object LoadMore : HomeIntent
}