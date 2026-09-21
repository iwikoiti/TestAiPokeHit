package com.example.testaipokehit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testaipokehit.mvi.DetailIntent
import com.example.testaipokehit.mvi.DetailState
import com.example.testaipokehit.repository.PokemonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: PokemonRepository,
    private val pokemonId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFavorites().collect { ids ->
                _state.update { it.copy(isFavorite = pokemonId in ids) }
            }
        }
        load()
    }

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.ToggleFavorite -> viewModelScope.launch {
                repository.toggleFavorite(pokemonId)
            }
            DetailIntent.Reload -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val pokemon = repository.getPokemonDetails(pokemonId)
                _state.update { it.copy(pokemon = pokemon, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "load error"
                    )
                }
            }
        }
    }
}