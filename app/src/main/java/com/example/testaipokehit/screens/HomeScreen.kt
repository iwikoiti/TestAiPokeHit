package com.example.testaipokehit.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.testaipokehit.R
import com.example.testaipokehit.mvi.HomeIntent
import com.example.testaipokehit.screens.common.PokemonListItem
import com.example.testaipokehit.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDetail: (Int) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showFilter by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.onIntent(HomeIntent.Search(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onIntent(HomeIntent.Search("")) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear_search)
                            )
                        }
                    }
                }
            )
            IconButton(onClick = { showFilter = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.filter)
                )
            }
        }

        when {
            state.error != null && state.pokemon.isEmpty() -> ErrorMessage(
                onRetry = { viewModel.onIntent(HomeIntent.Reload) }
            )
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
                Text(stringResource(R.string.no_results))
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.pokemon, key = { it.id }) { pokemon ->
                    PokemonListItem(
                        pokemon = pokemon,
                        isFavorite = pokemon.id in state.favorites,
                        onToggleFavorite = { viewModel.onIntent(HomeIntent.ToggleFavorite(it)) },
                        onClick = { onOpenDetail(pokemon.id) }
                    )
                }
                if (state.hasMore) {
                    item(key = "loadMore") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.loadingMore) {
                                CircularProgressIndicator()
                            }
                        }
                        LaunchedEffect(Unit) {
                            viewModel.onIntent(HomeIntent.LoadMore)
                        }
                    }
                }
            }
        }
    }

    if (showFilter) {
        FilterSheet(
            selected = state.selectedTypes,
            onApply = { viewModel.onIntent(HomeIntent.ApplyFilter(it)) },
            onDismiss = { showFilter = false }
        )
    }
}

@Composable
private fun ErrorMessage(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_loading),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}