package com.example.testaipokehit.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.testaipokehit.R
import com.example.testaipokehit.mvi.FavoritesIntent
import com.example.testaipokehit.screens.common.PokemonListItem
import com.example.testaipokehit.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onOpenDetail: (Int) -> Unit
) {
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading && state.pokemon.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        state.pokemon.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.favorites_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp)
            )
        }
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.pokemon, key = { it.id }) { pokemon ->
                PokemonListItem(
                    pokemon = pokemon,
                    isFavorite = pokemon.id in state.favorites,
                    onToggleFavorite = { viewModel.onIntent(FavoritesIntent.ToggleFavorite(it)) },
                    onClick = { onOpenDetail(pokemon.id) }
                )
            }
        }
    }
}