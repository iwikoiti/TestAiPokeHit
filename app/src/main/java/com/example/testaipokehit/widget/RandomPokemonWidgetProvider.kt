package com.example.testaipokehit.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import com.example.testaipokehit.MainActivity
import com.example.testaipokehit.PokeHitApplication
import com.example.testaipokehit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Виджет рабочего стола «Случайный покемон».
 *
 * - при добавлении и при периодическом обновлении показывает случайного покемона (id 1–1010);
 * - тап по виджету — новый случайный покемон;
 * - тап по иконке «i» — карточка этого покемона в приложении.
 *
 * Сеть используется только чтобы узнать имя покемона и скачать арт: текст и pokeball
 * рисуются сразу, чтобы отклик на тап не зависел от сети.
 */
class RandomPokemonWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        render(context, appWidgetManager, appWidgetIds.toList(), shuffle = false)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        appWidgetOptions: Bundle
    ) {
        render(context, appWidgetManager, listOf(appWidgetId), shuffle = false)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val state = RandomPokemonWidgetState(context)
        appWidgetIds.forEach(state::remove)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_SHUFFLE) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        render(
            context,
            AppWidgetManager.getInstance(context),
            listOf(appWidgetId),
            shuffle = true
        )
    }

    private fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: List<Int>,
        shuffle: Boolean
    ) {
        if (appWidgetIds.isEmpty()) return
        val appContext = context.applicationContext
        val state = RandomPokemonWidgetState(appContext)

        val targets = appWidgetIds.map { appWidgetId ->
            val currentId = state.pokemonId(appWidgetId)
            val pokemonId = if (shuffle || currentId == null) {
                RandomPokemonIds.next().also { state.savePokemon(appWidgetId, it) }
            } else {
                currentId
            }
            Target(
                appWidgetId = appWidgetId,
                pokemonId = pokemonId,
                options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            )
        }

        // Мгновенная отрисовка: то, что уже знаем, без ожидания сети.
        targets.forEach { target ->
            appWidgetManager.updateAppWidget(
                target.appWidgetId,
                buildViews(
                    context = appContext,
                    target = target,
                    name = state.pokemonName(target.appWidgetId) ?: "#${target.pokemonId}",
                    bitmap = null,
                    hintRes = R.string.widget_hint_shuffle
                )
            )
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            targets.forEach { target ->
                val loaded = runCatching {
                    withTimeout(LOAD_TIMEOUT_MS) { loadTarget(appContext, appWidgetManager, state, target) }
                }
                if (loaded.isFailure) {
                    appWidgetManager.updateAppWidget(
                        target.appWidgetId,
                        buildViews(
                            context = appContext,
                            target = target,
                            name = state.pokemonName(target.appWidgetId) ?: "#${target.pokemonId}",
                            bitmap = null,
                            hintRes = R.string.widget_error_offline
                        )
                    )
                }
            }
            pendingResult.finish()
        }
    }

    private suspend fun loadTarget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        state: RandomPokemonWidgetState,
        target: Target
    ) {
        val repository = (context as PokeHitApplication).container.pokemonRepository
        val summary = repository.getPokemonSummary(target.pokemonId)
        val name = summary?.name ?: "#${target.pokemonId}"
        val artworkUrl = summary?.artworkUrl ?: fallbackArtworkUrl(target.pokemonId)
        val bitmap = PokemonArtworkLoader.load(context, artworkUrl, target.imagePx(context))
        if (bitmap == null) throw IllegalStateException("Не удалось загрузить арт покемона ${target.pokemonId}")

        state.saveName(target.appWidgetId, name)
        appWidgetManager.updateAppWidget(
            target.appWidgetId,
            buildViews(
                context = context,
                target = target,
                name = name,
                bitmap = bitmap,
                hintRes = R.string.widget_hint_shuffle
            )
        )
    }

    private fun buildViews(
        context: Context,
        target: Target,
        name: String,
        bitmap: Bitmap?,
        hintRes: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_random_pokemon)
        views.setTextViewText(R.id.widget_name, name)
        views.setContentDescription(R.id.widget_artwork, name)
        views.setTextViewText(R.id.widget_hint, context.getString(hintRes))
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_artwork, bitmap)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_pokeball)
        }
        views.setOnClickPendingIntent(R.id.widget_root, shuffleIntent(context, target.appWidgetId))
        views.setOnClickPendingIntent(R.id.widget_details, detailIntent(context, target.pokemonId))
        return views
    }

    private fun shuffleIntent(context: Context, appWidgetId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            appWidgetId,
            Intent(context, RandomPokemonWidgetProvider::class.java).apply {
                action = ACTION_SHUFFLE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun detailIntent(context: Context, pokemonId: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            pokemonId,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_POKEMON_ID, pokemonId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private data class Target(
        val appWidgetId: Int,
        val pokemonId: Int,
        val options: Bundle
    ) {
        /**
         * Размер арта в пикселях: по меньшей стороне виджета. Верхняя граница — 320px,
         * потому что битмап уезжает в хост-процесс через binder (лимит транзакции ~1 МБ).
         */
        fun imagePx(context: Context): Int {
            val density = context.resources.displayMetrics.density
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, DEFAULT_MIN_WIDTH_DP)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, DEFAULT_MIN_HEIGHT_DP)
            return (minOf(widthDp, heightDp) * density).toInt().coerceIn(MIN_IMAGE_PX, MAX_IMAGE_PX)
        }
    }

    companion object {
        /** Тап по виджету: выбрать следующего покемона (заявлен также в манифесте). */
        const val ACTION_SHUFFLE = "com.example.testaipokehit.action.SHUFFLE_POKEMON"

        /** Меньше системного таймаута broadcast-приёмника (~10 c). */
        const val LOAD_TIMEOUT_MS = 8_000L

        const val DEFAULT_MIN_WIDTH_DP = 180
        const val DEFAULT_MIN_HEIGHT_DP = 110
        const val MIN_IMAGE_PX = 128
        const val MAX_IMAGE_PX = 320

        /** Запасной адрес арта на случай, если в ответе API нет `official-artwork`. */
        fun fallbackArtworkUrl(pokemonId: Int): String =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" +
                "other/official-artwork/$pokemonId.png"
    }
}