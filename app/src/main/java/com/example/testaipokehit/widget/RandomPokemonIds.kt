package com.example.testaipokehit.widget

import kotlin.random.Random

/**
 * Диапазон id для «случайного» покемона.
 *
 * 1–1010 — от Bulbasaur до Iron Leaves (DLC «The Indigo Disk»); у всех покемонов
 * этого диапазона PokeAPI отдаёт официальный арт (`official-artwork`).
 */
object RandomPokemonIds {

    const val MIN_ID = 1
    const val MAX_ID = 1010

    /** Случайный id в диапазоне [MIN_ID]..[MAX_ID] включительно. */
    fun next(random: Random = Random.Default): Int = random.nextInt(MIN_ID, MAX_ID + 1)
}