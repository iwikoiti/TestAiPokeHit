package com.example.testaipokehit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.testaipokehit.PokeHitApplication

class HomeViewModelFactory(
    private val application: PokeHitApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(application.container.pokemonRepository) as T
}

class FavoritesViewModelFactory(
    private val application: PokeHitApplication
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FavoritesViewModel(application.container.pokemonRepository) as T
}

class DetailViewModelFactory(
    private val application: PokeHitApplication,
    private val pokemonId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DetailViewModel(application.container.pokemonRepository, pokemonId) as T
}