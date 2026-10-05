package com.example.testaipokehit.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.testaipokehit.PokeHitApplication
import com.example.testaipokehit.R
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Инструментальная проверка виджета на устройстве: разметка должна надуваться
 * RemoteViews, а тап (broadcast [RandomPokemonWidgetProvider.ACTION_SHUFFLE]) — реально
 * сменить покемона и скачать его арт.
 *
 * Тест не может стать «настоящим» виджетом (нужна системная привилегия BIND_APPWIDGET),
 * поэтому тап имитируется broadcast'ом с несуществующим id: провайдер всё равно
 * отрабатывает целиком — выбор покемона, запрос в API, загрузка и декодирование картинки.
 */
@RunWith(AndroidJUnit4::class)
class RandomPokemonWidgetProviderTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val fakeAppWidgetId = 4242

    @Test
    fun layout_isInflatableByRemoteViews() {
        val views = RemoteViews(context.packageName, R.layout.widget_random_pokemon)
        views.setTextViewText(R.id.widget_name, "Pikachu")
        views.setTextViewText(R.id.widget_hint, context.getString(R.string.widget_hint_shuffle))
        views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_pokeball)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, RandomPokemonWidgetProvider::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val root = views.apply(context, FrameLayout(context))

        listOf(
            R.id.widget_root,
            R.id.widget_title,
            R.id.widget_details,
            R.id.widget_artwork,
            R.id.widget_name,
            R.id.widget_hint
        ).forEach { viewId ->
            assertNotNull("view $viewId не найден в разметке виджета", root.findViewById<View>(viewId))
        }
        assertEquals("Pikachu", root.findViewById<TextView>(R.id.widget_name).text.toString())
    }

    @Test
    fun shuffle_replacesPokemonAndDownloadsArtwork() {
        val state = RandomPokemonWidgetState(context)

        var pokemonId = 0
        var replaced = false
        for (attempt in 1..MAX_SHUFFLE_ATTEMPTS) {
            // Состояние очищаем, иначе await увидит прежнего покемона и не дождётся нового.
            state.remove(fakeAppWidgetId)
            shuffle()

            pokemonId = awaitValue("id покемона") { state.pokemonId(fakeAppWidgetId) }
            val name = awaitValue("имя покемона") { state.pokemonName(fakeAppWidgetId) }

            assertTrue("id=$pokemonId вне 1..1010", pokemonId in RandomPokemonIds.MIN_ID..RandomPokemonIds.MAX_ID)
            assertTrue("имя не получено из API: $name", !name.startsWith("#"))
            // Совпадение с маркером возможно (1 шанс из 1010) — тогда повторяем тап.
            if (pokemonId != SENTINEL_ID) {
                replaced = true
                break
            }
        }
        assertTrue("тап не сменил покемона: id остался $pokemonId", replaced)

        val artworkFile = awaitValue("файл арта в кэше") { cachedArtwork() }
        assertTrue("файл арта пустой", artworkFile.length() > 1_000)
    }

    @Test
    fun widgetLayout_rendersArtworkToBitmap() {
        val state = RandomPokemonWidgetState(context)
        val id = state.pokemonId(fakeAppWidgetId) ?: run {
            shuffle()
            awaitValue("id покемона") { state.pokemonId(fakeAppWidgetId) }
        }
        val name = state.pokemonName(fakeAppWidgetId) ?: "#$id"
        val artworkUrl = runBlocking {
            (context.applicationContext as PokeHitApplication).container.pokemonRepository
                .getPokemonSummary(id)
                ?.artworkUrl
        } ?: RandomPokemonWidgetProvider.fallbackArtworkUrl(id)
        val bitmap = runBlocking { PokemonArtworkLoader.load(context, artworkUrl, MAX_PREVIEW_PX) }
        assertNotNull("арт не загрузился", bitmap)

        // Тот же набор RemoteViews, что собирает провайдер, только отрисованный офлайн.
        val views = RemoteViews(context.packageName, R.layout.widget_random_pokemon)
        views.setTextViewText(R.id.widget_name, name)
        views.setTextViewText(R.id.widget_hint, context.getString(R.string.widget_hint_shuffle))
        views.setImageViewBitmap(R.id.widget_artwork, bitmap)
        val root = views.apply(context, FrameLayout(context))

        val size = (PREVIEW_DP * context.resources.displayMetrics.density).toInt()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        assertTrue("виджет не измерился", root.measuredWidth > 0 && root.measuredHeight > 0)
        assertEquals(name, root.findViewById<TextView>(R.id.widget_name).text.toString())

        val shot = Bitmap.createBitmap(root.measuredWidth, root.measuredHeight, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(shot))

        val distinctColors = mutableSetOf<Int>()
        for (x in 0 until shot.width step 4) {
            for (y in 0 until shot.height step 4) {
                distinctColors += shot.getPixel(x, y)
            }
        }
        assertTrue(
            "превью выглядит пустым: ${distinctColors.size} цветов",
            distinctColors.size > MIN_PREVIEW_COLORS
        )

        val file = File(context.getExternalFilesDir(null), PREVIEW_FILE)
        file.outputStream().use { shot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("превью не записано", file.length() > 0)
    }

    private fun shuffle() {
        context.sendBroadcast(
            Intent(context, RandomPokemonWidgetProvider::class.java).apply {
                action = RandomPokemonWidgetProvider.ACTION_SHUFFLE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, fakeAppWidgetId)
            }
        )
    }

    private fun cachedArtwork(): File? =
        context.cacheDir
            .resolve("widget_artwork")
            .listFiles()
            ?.firstOrNull { it.length() > 0 }

    private fun <T> awaitValue(what: String, timeoutMs: Long = 25_000, block: () -> T?): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            block()?.let { return it }
            Thread.sleep(200)
        }
        throw AssertionError("Не дождались: $what")
    }

    private companion object {
        /** Совпадение id возможно, поэтому тап повторяется, пока покемон не сменится. */
        const val MAX_SHUFFLE_ATTEMPTS = 5
        const val SENTINEL_ID = 999
        const val PREVIEW_DP = 180
        const val MAX_PREVIEW_PX = 320
        const val MIN_PREVIEW_COLORS = 8
        const val PREVIEW_FILE = "widget_preview.png"
    }
}