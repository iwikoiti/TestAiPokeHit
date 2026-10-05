# AGENTS.md

Проект **PokeHit** — энциклопедия покемонов для Android (Kotlin, Jetpack Compose). Данные — с https://pokeapi.co/ по API (Retrofit2), избранное — в локальной SQLite (Room, только id), изображения — Coil (кэш на диске). UI на Kotlin/Compose, реактивное обновление через ViewModel + StateFlow (MVI).

## Текущее состояние

Приложение **реализовано полностью** по ТЗ ниже, плюс виджет рабочего стола «Случайный покемон» (пакет `widget/`). Git-репозиторий инициализирован, первый коммит сделан. GitHub-remote не добавлен (требует учётку пользователя). Все слои в пакете `com.example.testaipokehit`: `api/`, `model/`, `database/`, `repository/`, `mvi/`, `viewmodel/`, `screens/`, `widget/`, а также `di/` (ручной DI-контейнер `AppContainer`, держится в `PokeHitApplication`).

## Стек (факт, проверено сборкой)

- Kotlin 2.2.10, AGP 9.4.1, Gradle 9.6.0, Compose BOM 2026.02.01
- minSdk 27, targetSdk 36, compileSdk release(37)
- MVI + ViewModel + StateFlow; Kotlin Coroutines 1.10.2
- Retrofit2 2.11.0 + Gson (`converter-gson`), OkHttp 4.12.0 (в ТЗ было «3.0.0» — устаревшая версия 2015 г., несовместима с Retrofit; используем 4.12.0)
- Room 2.8.4 (компилятор через KSP: плагин `com.google.devtools.ksp` версии `2.2.0-2.0.2` — это последний KSP для Kotlin 2.2.x)
- Coil 3.2.0 (`coil-compose` + `coil-network-okhttp`; версии 3.3.0 из ТЗ не существует на Maven Central)
- Jetpack Compose + Navigation 2.9.7; иконки — `material-icons-core` 1.7.8 (не extended)
- Gson для JSON, описания (flavor text) — на английском языке
- Новые зависимости добавлять только в `gradle/libs.versions.toml` и подключать через `libs.*` (версии в build-файлах не хардкодить)

## Архитектура и данные

- Пагинация: полный список имён (~1300) грузится один раз порциями по 200, детали подгружаются лениво окнами по 30 (по мере скролла) и кэшируются в памяти репозитория.
- Фильтр по типам: множественное пересечение списков покемонов из `/type/{name}` (18 классических типов, без unknown/shadow/stellar).
- Поиск: клиентский по имени (lowercase-contains) на предзагруженном списке.
- `PokemonRepository` — единый источник: кэши имена, членов типов и `Pokemon` (detail + species-описание одним маппингом).
- Избранное: `FavoriteDao.observeFavoriteIds()` → `Flow<List<Int>>`, обновление на главном/детальном/избранном экранах через стандартный коллектор (инструментальный сброс `distinctUntilChanged` — на экране «Избранное»).
- Детали: официальный арт (official-artwork) + галерея (home, shiny), статы, способности, рост в «м», вес в «кг».

## Виджет «Случайный покемон» (`widget/`)

- `RandomPokemonWidgetProvider` (AppWidgetProvider + RemoteViews, без Compose/Glance): id выбирается из диапазона **1–1010** (`RandomPokemonIds`) — от Bulbasaur до Iron Leaves; у всех есть `official-artwork`.
- Тап по виджету → broadcast `com.example.testaipokehit.action.SHUFFLE_POKEMON` → новый случайный покемон; тап по иконке «i» → `MainActivity` с extra `EXTRA_POKEMON_ID` → сразу карточка покемона (`PokeHitApp(initialPokemonId)`).
- Состояние виджета (id + имя) — в SharedPreferences `random_pokemon_widget`, чтобы перерисовка при повороте/изменении размера не меняла покемона; очищается в `onDeleted`.
- Данные для виджета — `PokemonRepository.getPokemonSummary(id)` (один запрос `/pokemon/{id}`, без species) → `model/PokemonSummary`.
- Картинку виджет грузит не Coil, а `PokemonArtworkLoader`: OkHttp + `LruCache` (8 МБ) + PNG в `cacheDir/widget_artwork` (максимум 24 файла, подрезка по `lastModified`). Запасной URL арта — `sprites/pokemon/other/official-artwork/{id}.png` (без сегмента `other/` PokeAPI отдаёт 404).
- Отрисовка двухфазная: сначала мгновенно текст + pokeball-плейсхолдер, затем `goAsync()` + корутина с `withTimeout(8 c)` грузит имя и арт (таймаут меньше системного ~10 c у broadcast). Размер битмапа ограничен 320 px — лимит binder-транзакции при передаче в хост-процесс.
- Палитра виджета — своя (`res/values/colors.xml` + `values-night/colors.xml`), т.к. RemoteViews не читает тему Compose.

## Сборка и тесты (Windows)

- `JAVA_HOME` на хосте не задан — задавайте вручную: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'` (JBR Android Studio) перед `.\gradlew.bat`.
- Debug APK: `.\gradlew.bat :app:assembleDebug`
- Unit-тесты (JUnit4, `app/src/test`): `.\gradlew.bat :app:testDebugUnitTest`
- Инструментальные тесты (`app/src/androidTest`) требуют устройство/эмулятор: `.\gradlew.bat :app:connectedDebugAndroidTest`
- На этом хосте задача `connectedDebugAndroidTest` может упасть с `INSTALL_FAILED_USER_RESTRICTED` (тестовый APK помечен `testOnly`). Обход: `adb install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk` и `adb shell am instrument -w -r -e class <FQCN> com.example.testaipokehit.test/androidx.test.runner.AndroidJUnitRunner`.
- `adb shell uiautomator dump` на подключённом MIUI-устройстве падает с «could not get idle state» — для проверки UI используйте OkHttp-логи (`adb logcat --pid=<pid>`, тег `okhttp.OkHttpClient`) либо инструментальные тесты.

## Особенности сборки (AGP 9.4 / Gradle 9.6, не менять без причины)

- `compileSdk` — новый DSL AGP 9: `compileSdk { version = release(37) }`.
- R8 keep-правила — в `app/src/main/keepRules/`, а не в legacy `proguard-rules.pro`.
- Release-сборка: оптимизация/R8 отключена (`optimization { enable = false }`).
- `org.gradle.configuration-cache=true` — никаких side-effect'ов на этапе конфигурации в build-скриптах.
- `android.disallowKotlinSourceSets=false` в `gradle.properties` — обязательный обходной флаг: KSP 2.2.x добавляет исходники старым DSL, что запрещено built-in Kotlin в AGP 9 (без флага сборка падает).
- Гредл-демон работает на авто-провиженном JDK 25 (foojay в `settings.gradle.kts`); Kotlin пока не знает JVM target 25, падает на JVM_24 — это безвредно, код компилируется под Java 11.

## Соглашения

- Все ресурсные строки UI — в `app/src/main/res/values/strings.xml`. Язык UI — русский; имена покемонов, типов и способностей — английские (канонические, как в PokeAPI).
- Тема — `TestAiPokeHitTheme` в `ui/theme/` (Material3, динамический цвет на Android 12+).
- Новые экраны: Intent/State в `mvi/`, ViewModel в `viewmodel/` + Factory в `AppViewModelFactories.kt`, экран в `screens/`; общие UI-компоненты — в `screens/common/`.
- Виджеты: разметка — только RemoteViews-совместимые вьюхи (`LinearLayout`/`ImageView`/`TextView`), вся логика — в `widget/`; вьюхи не поддерживают тему Compose, поэтому цвета виджета задаются ресурсами.
- Room-схему менять через версию БД и миграцию (`app/src/main/java/.../database/AppDatabase.kt`).