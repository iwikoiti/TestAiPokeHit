package com.example.testaipokehit.model

/**
 * Краткая карточка покемона: всё, что нужно показать на рабочем столе без
 * открытия приложения (имя и официальный арт).
 */
data class PokemonSummary(
    val id: Int,
    val name: String,
    val artworkUrl: String?
)