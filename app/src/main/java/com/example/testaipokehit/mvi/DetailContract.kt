package com.example.testaipokehit.mvi

import com.example.testaipokehit.model.Pokemon

data class DetailState(
    val pokemon: Pokemon? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface DetailIntent {
    data object ToggleFavorite : DetailIntent
    data object Reload : DetailIntent
}