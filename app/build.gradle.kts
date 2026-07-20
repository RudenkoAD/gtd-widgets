import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// --- Подпись релиза: keystore.properties (в .gitignore) описывает путь/пароли ---
// Файл создаёт scripts/make-keystore.ps1 (keystore лежит ВНЕ репо, в %LOCALAPPDATA%).
// Если файла нет — release собирается с debug-подписью (сборка НИКОГДА не падает из-за
// его отсутствия). CI подменяет keystore.properties из секретов (см. зону CI).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasKeystore = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasKeystore) FileInputStream(keystorePropertiesFile).use { load(it) }
}

android {
    namespace = "com.gtdflow.widget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gtdflow.widget"
        minSdk = 26 // java.time.LocalDate/LocalTime без десугаринга; Glance поддерживает 23+
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 включён; движок QuickJS и модели сериализации защищены proguard-rules.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Есть keystore.properties → подписываем release-ключом; иначе — debug-подпись
            // (assembleRelease всё равно даёт устанавливаемый APK, сборка не падает).
            signingConfig = if (hasKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // JVM-юнит-тесты (src/test) видят ассеты/ресурсы для фикстур.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.google.material) // XML-темы Material3 для окон Activity

    // Compose — экраны настройки (MainActivity, CaptureActivity)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Glance — виджеты «Сегодня» и «Входящие»
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Инфраструктура: обновление в фоне, per-widget конфиг, SAF-доступ к vault
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)

    // Движок ядра (widget-core.js в QuickJS) + разбор его JSON-вывода
    implementation(libs.quickjs.wrapper)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}

// --- Обновление движка: копирует свежий widget-core.js из репозитория плагина. ---
// Запускать вручную при обновлении ядра: gradlew :app:vendorWidgetCore
// Путь-источник можно переопределить: -Pgtd.widgetCoreSrc=<путь к widget-core.js>.
tasks.register<Copy>("vendorWidgetCore") {
    group = "gtd"
    description = "Копирует widget-core.js из репозитория плагина в app/src/main/assets."
    val srcPath = providers.gradleProperty("gtd.widgetCoreSrc")
        .getOrElse("D:/projects/claude_home/calendar_app/widget-core.js")
    val srcFile = file(srcPath)
    onlyIf { srcFile.exists() }
    from(srcFile)
    into(layout.projectDirectory.dir("src/main/assets"))
}
