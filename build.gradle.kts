// Корневой build-скрипт: плагины объявляем без применения (apply false),
// применяются в модуле :app. Версии — из каталога gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
