package com.example.testaipokehit.widget

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверка диапазона id для виджета «Случайный покемон»: 1–1010
 * (от Bulbasaur до Iron Leaves), у всех есть официальный арт.
 */
class RandomPokemonIdsTest {

    @Test
    fun bounds_coverBulbasaurThroughIronLeaves() {
        assertEquals(1, RandomPokemonIds.MIN_ID)
        assertEquals(1010, RandomPokemonIds.MAX_ID)
    }

    @Test
    fun next_alwaysInsideRange() {
        repeat(2_000) {
            val id = RandomPokemonIds.next()
            assertTrue("id=$id вне диапазона", id in RandomPokemonIds.MIN_ID..RandomPokemonIds.MAX_ID)
        }
    }

    @Test
    fun next_withFixedSeed_isDeterministic() {
        val first = generator(Random(42))
        val second = generator(Random(42))
        assertEquals(first, second)
    }

    @Test
    fun next_withFixedSeed_givesVariety() {
        val ids = generator(Random(7))
        assertTrue("ожидались разные id, получено ${ids.toSet().size}", ids.toSet().size > 1)
    }

    @Test
    fun next_spansWholeRange() {
        val random = Random(2024)
        val ids = List(5_000) { RandomPokemonIds.next(random) }
        assertTrue("нижняя граница не достигнута", ids.any { it <= RandomPokemonIds.MIN_ID + 50 })
        assertTrue("верхняя граница не достигнута", ids.any { it >= RandomPokemonIds.MAX_ID - 50 })
    }

    private fun generator(random: Random): List<Int> = List(100) { RandomPokemonIds.next(random) }
}