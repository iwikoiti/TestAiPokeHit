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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.testaipokehit.R
import com.example.testaipokehit.mvi.DetailIntent
import com.example.testaipokehit.model.Pokemon
import com.example.testaipokehit.model.PokemonStat
import com.example.testaipokehit.screens.common.TypeChip
import com.example.testaipokehit.viewmodel.DetailViewModel

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val pokemon = state.pokemon

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (pokemon != null) {
                IconButton(onClick = { viewModel.onIntent(DetailIntent.ToggleFavorite) }) {
                    Icon(
                        imageVector = if (state.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Filled.FavoriteBorder
                        },
                        contentDescription = stringResource(
                            if (state.isFavorite) R.string.remove_favorite else R.string.add_favorite
                        ),
                        tint = if (state.isFavorite) Color(0xFFE53935) else LocalContentColor.current
                    )
                }
            }
        }

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            pokemon != null -> DetailContent(pokemon = pokemon)
            else -> Column(
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
                Button(onClick = { viewModel.onIntent(DetailIntent.Reload) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun DetailContent(pokemon: Pokemon) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AsyncImage(
            model = pokemon.officialArtworkUrl,
            contentDescription = pokemon.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(8.dp))
        Text(text = pokemon.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            pokemon.types.forEach { TypeChip(type = it) }
        }
        Spacer(Modifier.height(16.dp))

        SectionTitle(stringResource(R.string.section_physical))
        DetailInfoRow(
            label = stringResource(R.string.height_label),
            value = "%.1f %s".format(pokemon.height, stringResource(R.string.unit_meters))
        )
        DetailInfoRow(
            label = stringResource(R.string.weight_label),
            value = "%.1f %s".format(pokemon.weight, stringResource(R.string.unit_kilograms))
        )
        pokemon.baseExperience?.let {
            DetailInfoRow(
                label = stringResource(R.string.base_experience_label),
                value = it.toString()
            )
        }

        SectionTitle(stringResource(R.string.section_stats))
        pokemon.stats.forEach { stat -> StatBar(stat = stat) }

        if (pokemon.abilities.isNotEmpty()) {
            SectionTitle(stringResource(R.string.section_abilities))
            pokemon.abilities.forEach { ability ->
                Text(text = "\u2022 $ability", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (pokemon.galleryUrls.isNotEmpty()) {
            SectionTitle(stringResource(R.string.section_gallery))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pokemon.galleryUrls) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.size(140.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        if (!pokemon.description.isNullOrBlank()) {
            SectionTitle(stringResource(R.string.section_description))
            Text(text = pokemon.description, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatBar(stat: PokemonStat) {
    val progress = stat.base.coerceIn(0, 255) / 255f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stat.displayName(),
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stat.base.toString(),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun PokemonStat.displayName(): String {
    if (name == "hp") return "HP"
    return name
        .split('-')
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}