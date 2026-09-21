package com.example.testaipokehit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testaipokehit.mvi.FavoritesIntent
import com.example.testaipokehit.mvi.FavoritesState
import com.example.testaipokehit.repository.PokemonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: PokemonRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFavorites()
                .distinctUntilChanged()
                .collect { ids ->
                    _state.update { it.copy(isLoading = true) }
                    if (ids.isEmpty()) {
                        _state.update {
                            it.copy(pokemon = emptyList(), favorites = emptySet(), isLoading = false)
                        }
                    } else {
                        val items = coroutineScope {
                            ids
                                .map { id -> async { repository.getPokemonDetails(id) } }
                                .awaitAll()
                                .filterNotNull()
                        }
                        _state.update {
                            it.copy(
                                pokemon = items,
                                favorites = ids.toSet(),
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    fun onIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.ToggleFavorite -> viewModelScope.launch {
                repository.toggleFavorite(intent.id)
            }
        }
    }
}