package com.example.testaipokehit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testaipokehit.mvi.HomeIntent
import com.example.testaipokehit.mvi.HomeState
import com.example.testaipokehit.repository.PokemonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: PokemonRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var page = 0
    private val pageSize = 30

    init {
        viewModelScope.launch {
            repository.observeFavorites().collect { ids ->
                _state.update { it.copy(favorites = ids.toSet()) }
            }
        }
        reload(reset = true)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Search -> {
                _state.update { it.copy(query = intent.query) }
                reload(reset = true)
            }
            is HomeIntent.ApplyFilter -> {
                _state.update { it.copy(selectedTypes = intent.types) }
                reload(reset = true)
            }
            HomeIntent.ResetFilter -> {
                _state.update { it.copy(selectedTypes = emptySet()) }
                reload(reset = true)
            }
            HomeIntent.Reload -> reload(reset = true)
            is HomeIntent.ToggleFavorite -> viewModelScope.launch {
                repository.toggleFavorite(intent.id)
            }
            HomeIntent.LoadMore -> {
                val s = _state.value
                if (!s.isLoading && !s.loadingMore && s.hasMore) {
                    reload(reset = false)
                }
            }
        }
    }

    private fun reload(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (reset) {
                _state.update { it.copy(isLoading = true, error = null) }
            } else {
                _state.update { it.copy(loadingMore = true, error = null) }
            }
            try {
                loadPage(reset)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadingMore = false,
                        error = e.message ?: "load error"
                    )
                }
            }
        }
    }

    private suspend fun loadPage(reset: Boolean) {
        val current = _state.value
        val all = repository.searchNames(current.query, current.selectedTypes)
        val start = if (reset) 0 else page * pageSize
        if (!reset && start >= all.size) {
            _state.update { it.copy(loadingMore = false, hasMore = false) }
            return
        }
        page = if (reset) 1 else page + 1
        val slice = all.drop(start).take(pageSize)
        val hasMore = start + slice.size < all.size

        val items = coroutineScope {
            slice
                .map { name -> async { repository.getPokemonDetailsByName(name) } }
                .awaitAll()
                .filterNotNull()
        }

        _state.update {
            it.copy(
                pokemon = if (reset) items else it.pokemon + items,
                isLoading = false,
                loadingMore = false,
                hasMore = hasMore,
                error = null
            )
        }
    }
}