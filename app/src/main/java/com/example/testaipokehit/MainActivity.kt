package com.example.testaipokehit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.testaipokehit.screens.PokeHitApp
import com.example.testaipokehit.ui.theme.TestAiPokeHitTheme

class MainActivity : ComponentActivity() {

    /** Покемон, запрошенный виджетом: 0 — открывать обычный список. */
    private var requestedPokemonId by mutableStateOf(NO_POKEMON_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedPokemonId = pokemonIdFromIntent(intent)
        setContent {
            TestAiPokeHitTheme {
                PokeHitApp(initialPokemonId = requestedPokemonId.takeIf { it > 0 })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedPokemonId = pokemonIdFromIntent(intent)
    }

    private fun pokemonIdFromIntent(intent: Intent?): Int =
        intent?.getIntExtra(EXTRA_POKEMON_ID, NO_POKEMON_ID) ?: NO_POKEMON_ID

    companion object {
        const val EXTRA_POKEMON_ID = "com.example.testaipokehit.extra.POKEMON_ID"
        private const val NO_POKEMON_ID = 0
    }
}