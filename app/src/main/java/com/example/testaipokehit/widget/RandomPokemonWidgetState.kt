package com.example.testaipokehit.widget

import android.content.Context

/**
 * Состояние виджета в SharedPreferences: какой покемон показан и как он называется.
 *
 * Нужно, чтобы системная перерисовка (поворот экрана, изменение размера виджета,
 * periodic-обновление) не подменяла покемона — новый выбирается только по тапу.
 */
class RandomPokemonWidgetState(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun pokemonId(appWidgetId: Int): Int? =
        prefs.getInt(idKey(appWidgetId), 0).takeIf { it > 0 }

    fun pokemonName(appWidgetId: Int): String? =
        prefs.getString(nameKey(appWidgetId), null)

    /** Сохраняет показанного покемона; имя предыдущего сбрасывается. */
    fun savePokemon(appWidgetId: Int, pokemonId: Int) {
        prefs.edit()
            .putInt(idKey(appWidgetId), pokemonId)
            .remove(nameKey(appWidgetId))
            .apply()
    }

    fun saveName(appWidgetId: Int, name: String) {
        prefs.edit().putString(nameKey(appWidgetId), name).apply()
    }

    fun remove(appWidgetId: Int) {
        prefs.edit()
            .remove(idKey(appWidgetId))
            .remove(nameKey(appWidgetId))
            .apply()
    }

    private fun idKey(appWidgetId: Int) = "id_$appWidgetId"

    private fun nameKey(appWidgetId: Int) = "name_$appWidgetId"

    private companion object {
        const val PREFS_NAME = "random_pokemon_widget"
    }
}