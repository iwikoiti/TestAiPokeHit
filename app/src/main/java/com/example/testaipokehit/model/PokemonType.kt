package com.example.testaipokehit.model

import androidx.compose.ui.graphics.Color

/**
 * 18 классических типов покемонов с цветовой кодировкой (официальная палитра).
 */
enum class PokemonType(
    val apiName: String,
    val displayName: String,
    val color: Color
) {
    NORMAL("normal", "Normal", Color(0xFFA8A77A)),
    FIGHTING("fighting", "Fighting", Color(0xFFC22E28)),
    FLYING("flying", "Flying", Color(0xFFA98FF3)),
    POISON("poison", "Poison", Color(0xFFA33EA1)),
    GROUND("ground", "Ground", Color(0xFFE2BF65)),
    ROCK("rock", "Rock", Color(0xFFB6A136)),
    BUG("bug", "Bug", Color(0xFFA6B91A)),
    GHOST("ghost", "Ghost", Color(0xFF735797)),
    STEEL("steel", "Steel", Color(0xFFB7B7CE)),
    FIRE("fire", "Fire", Color(0xFFEE8130)),
    WATER("water", "Water", Color(0xFF6390F0)),
    GRASS("grass", "Grass", Color(0xFF7AC74C)),
    ELECTRIC("electric", "Electric", Color(0xFFF7D02C)),
    PSYCHIC("psychic", "Psychic", Color(0xFFF95587)),
    ICE("ice", "Ice", Color(0xFF96D9D6)),
    DRAGON("dragon", "Dragon", Color(0xFF6F35FC)),
    DARK("dark", "Dark", Color(0xFF705746)),
    FAIRY("fairy", "Fairy", Color(0xFFD685AD));

    companion object {
        fun fromApiName(name: String): PokemonType? =
            entries.firstOrNull { it.apiName == name }
    }
}