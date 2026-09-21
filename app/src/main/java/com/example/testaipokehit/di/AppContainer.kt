package com.example.testaipokehit.di

import android.content.Context
import com.example.testaipokehit.api.RetrofitClient
import com.example.testaipokehit.database.AppDatabase
import com.example.testaipokehit.repository.PokemonRepository

/**
 * Ручной DI-контейнер: единственный источник зависимостей для ViewModel.
 */
class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)

    val pokemonRepository: PokemonRepository =
        PokemonRepository(api = RetrofitClient.api, favoriteDao = database.favoriteDao())
}