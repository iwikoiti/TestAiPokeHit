package com.example.testaipokehit.mvi

import com.example.testaipokehit.model.Pokemon

data class FavoritesState(
    val pokemon: List<Pokemon> = emptyList(),
    val favorites: Set<Int> = emptySet(),
    val isLoading: Boolean = true
)

sealed interface FavoritesIntent {
    data class ToggleFavorite(val id: Int) : FavoritesIntent
}