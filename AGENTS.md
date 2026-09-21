# AGENTS.md

Проект **PokeHit** — энциклопедия покемонов для Android (Kotlin, Jetpack Compose). Данные — с https://pokeapi.co/ по API (Retrofit2), избранное — в локальной SQLite (Room), изображения — Coil (кэшируются на диск). UI на Kotlin/Compose, реактивное обновление через ViewModel + StateFlow.

## Текущее состояние

Репозиторий пока содержит стандартный шаблон Android Studio («Hello Android»): один модуль `:app`, пакет `com.example.testaipokehit`, ничего из функциональности PokeHit не реализовано. Приложение предстоит переписать по ТЗ ниже (шаблонный код `MainActivity`/темы заменить). Git-репозиторий ещё не инициализирован (план: GitHub).

## Целевой стек (по ТЗ)

- Kotlin 2.1.21 (в каталоге версий сейчас 2.2.10, AGP 9.4.1, Gradle 9.6.0)
- minSdk 27 (Android 8.1), targetSdk 36
- MVI + ViewModel + StateFlow; Kotlin Coroutines 1.10.2
- Retrofit2 + OkHttp 3.0.0
- Room 2.8.4
- Coil 3.3.0
- Jetpack Compose + Navigation 2.9.7
- Новые зависимости добавлять только в `gradle/libs.versions.toml` и подключать через `libs.*` (версии в build-файлах не хардкодить).

## Архитектура — слои в `app/src/main/java/com/example/testaipokehit/`

- `api/` — Retrofit-клиент, интерфейсы PokeAPI, DTO для парсинга JSON
- `model/` — доменные модели данных
- `database/` — Room: сущности, DAO, база данных (хранится ТОЛЬКО id избранных покемонов — остальные данные всегда актуальны с API; так экономим место)
- `repository/` — единый источник данных: объединяет API и БД, кэширование
- `mvi/` — Intent и State для каждого экрана
- `viewmodel/` — ViewModel с бизнес-логикой (StateFlow)
- `screens/` — UI на Jetpack Compose

## Функциональные требования (что реализовать)

- **Главный экран**: список всех покемонов. Элемент: изображение (official-artwork), имя, типы с цветовой кодировкой, чекбокс избранного. Сверху — строка поиска по имени и кнопка фильтра. Снизу — BottomNavigationBar с вкладками «Главная» и «Избранное».
- **Детальный экран** (по клику на элемент): official-artwork, имя, типы, вес, рост, базовый опыт, базовые статы (HP, attack, defense, special-attack, special-defense, speed), способности, галерея (shiny, home), описание. В верхней панели — иконка сердечка (избранное) и кнопка «Назад».
- **Экран «Избранное»**: как главный, но только избранные; автоматически синхронизируется с главным при добавлении/удалении.
- **Фильтр**: BottomSheet с 18 типами (множественный выбор), кнопки «Сбросить всё» и «Применить».

## Сборка и тесты (Windows)

- Все команды — через `.\gradlew.bat <task>` (скрипт `./gradlew` на этом хосте не работает).
- Debug APK: `.\gradlew.bat :app:assembleDebug`
- Unit-тесты (JUnit4, `app/src/test`): `.\gradlew.bat :app:testDebugUnitTest`
- Инструментальные тесты (`app/src/androidTest`) требуют устройство/эмулятор: `.\gradlew.bat :app:connectedDebugAndroidTest`

## Особенности сборки (AGP 9.4 / Gradle 9.6, не менять без причины)

- `compileSdk` — новый DSL AGP 9: `compileSdk { version = release(...) }`.
- R8 keep-правила — в `app/src/main/keepRules/` (AGP авто-объединяет файлы), а не в legacy `proguard-rules.pro`.
- Release-сборка: оптимизация/R8 отключена (`optimization { enable = false }`).
- `org.gradle.configuration-cache=true` — никаких side-effect'ов на этапе конфигурации в build-скриптах.
- Гредл-демон работает на авто-провиженном JDK 25 (foojay в `settings.gradle.kts`); код приложения компилируется под Java 11.

## Соглашения

- Все ресурсные строки UI — в `app/src/main/res/values/strings.xml`.
- Тема — `TestAiPokeHitTheme` в `ui/theme/` (Material3, динамический цвет на Android 12+).