# DEV — окружение и сборка «GTD Flow Виджеты»

Воспроизводимые шаги для сборки Android-приложения (Windows 10, без прав администратора).

> **Важно про диск.** На системном диске `C:` было мало места (~2 ГБ), поэтому весь
> тулчейн (JDK, Android SDK, кэш Gradle) вынесен на диск `D:` в `D:\gtd-toolchain\`
> (ВНЕ репозитория). На машине, где хватает места на `C:`, можно ставить в
> `%LOCALAPPDATA%` — тогда скорректируйте пути ниже и `local.properties`.
> Keystore (крошечный) лежит в `%LOCALAPPDATA%\gtd-widgets\` (см. «Подпись»).

## 0. Итоговые версии

| Компонент | Версия / путь |
| --- | --- |
| JDK | Temurin (Eclipse Adoptium) **17.0.19+10**, `D:\gtd-toolchain\jdk-17` |
| Gradle | **8.9** (wrapper), дистрибутив тянется автоматически |
| Android Gradle Plugin | **8.5.2** |
| Kotlin | **2.0.20** |
| compileSdk / targetSdk | **35** / **35** (build-tools 35.0.0, platform android-35) |
| minSdk | **26** |
| Glance | 1.1.0, WorkManager 2.9.1, DataStore 1.1.1, DocumentFile 1.0.1 |
| QuickJS | `wang.harlon.quickjs:wrapper-android:3.2.3` (Maven Central) |
| Android SDK | `D:\gtd-toolchain\Android\Sdk` |
| GRADLE_USER_HOME | `D:\gtd-toolchain\gradle-home` (кэш вне `C:`) |

## 1. JDK 17 (без прав администратора)

Скачан zip Temurin 17 и распакован в `D:\gtd-toolchain\jdk-17`:

```bash
curl -L -o jdk17.zip "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
# распаковать, переименовать каталог jdk-17.x -> D:\gtd-toolchain\jdk-17
```

Проверка: `D:\gtd-toolchain\jdk-17\bin\java.exe -version` → `openjdk 17.0.19`.

## 2. Android SDK (cmdline-tools + пакеты)

```bash
# cmdline-tools -> D:\gtd-toolchain\Android\Sdk\cmdline-tools\latest\
curl -L -o cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
# распаковать так, чтобы получилось .../cmdline-tools/latest/bin/sdkmanager.bat
```

Установка пакетов и лицензий (JAVA_HOME обязателен):

```powershell
$env:JAVA_HOME = 'D:\gtd-toolchain\jdk-17'
$SDK = 'D:\gtd-toolchain\Android\Sdk'
$SDKM = "$SDK\cmdline-tools\latest\bin\sdkmanager.bat"
# лицензии (ответить y на все):
& $SDKM --sdk_root=$SDK --licenses
& $SDKM --sdk_root=$SDK "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

## 3. local.properties (в .gitignore)

Путь к SDK для Gradle (обратные слэши экранируются):

```
sdk.dir=D\:\\gtd-toolchain\\Android\\Sdk
```

## 4. Gradle wrapper

В репозитории лежат `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`
и `...properties` (Gradle 8.9). Первый запуск скачает дистрибутив 8.9 в
`GRADLE_USER_HOME`. Регенерация при необходимости:

```bash
# в пустом каталоге с settings.gradle.kts:
gradle wrapper --gradle-version 8.9 --distribution-type bin
# затем скопировать gradlew, gradlew.bat, gradle/wrapper/* в репозиторий
```

## 5. Сборка и тесты

Задать окружение (в этой машине — обязательно оба, чтобы кэш не забивал `C:`):

```powershell
$env:JAVA_HOME = 'D:\gtd-toolchain\jdk-17'
$env:GRADLE_USER_HOME = 'D:\gtd-toolchain\gradle-home'
cd D:\projects\claude_home\gtd_widgets
.\gradlew.bat --no-daemon assembleDebug assembleRelease test
```

Артефакты:
- `app/build/outputs/apk/debug/app-debug.apk` (debug-подпись).
- `app/build/outputs/apk/release/app-release.apk` (R8 + shrinkResources;
  подпись — release-ключом при наличии `keystore.properties`, иначе debug).

Юнит-тесты — чистый JVM (`app/src/test`, без Robolectric): модели/сериализация,
`TimeUtil`, `DeepLink`, `InboxFileText`, `TaskLineToggle`.

## 6. Подпись (секреты — вне git)

- `scripts/make-keystore.ps1` генерирует keystore в
  `%LOCALAPPDATA%\gtd-widgets\release.keystore` (ВНЕ репо) и пишет
  `keystore.properties` в корень репо (в `.gitignore`).
- `app/build.gradle.kts` читает `keystore.properties`; если он есть — release
  подписывается release-ключом, если нет — debug-подписью (сборка НЕ падает).
- Запуск: `powershell -ExecutionPolicy Bypass -File scripts\make-keystore.ps1`.
  (Скрипт в UTF-8 **с BOM** — иначе Windows PowerShell 5.1 портит кириллицу.)
- Проверка подписи:
  `build-tools\35.0.0\apksigner.bat verify --print-certs app-release.apk`.
- CI подписывает из GitHub Secrets (KEYSTORE_B64/KEYSTORE_PASSWORD) — зона CI.

**Никогда не коммитить:** `keystore.properties`, `*.keystore`, `local.properties`
(все в `.gitignore`).

## 7. Движок ядра (QuickJS) — API-приёмы

Библиотека `wang.harlon.quickjs:wrapper-android:3.2.3`, пакет
`com.whl.quickjs.wrapper`. Нативные `.so` в aar для `armeabi-v7a`, `arm64-v8a`,
`x86`, `x86_64` (в APK попадают все четыре).

Обёртка — `engine/QuickJsEngine.kt`, мост — `assets/widget-bridge.js`. Приёмы:

- `val ctx = QuickJSContext.create()` — создать контекст (привязан к потоку,
  НЕ потокобезопасен; см. `EngineRunner` — выделенный однопоточный диспетчер).
- `ctx.evaluate(scriptString)` — выполнить JS. Обёртка после каждого
  `evaluate()/call()` сама прогоняет очередь заданий (`JS_ExecutePendingJob`),
  поэтому промисы ядра, ждущие только микрозадачи, к возврату уже разрешены.
- Загрузка: `ctx.evaluate(widget-core.js)`, затем `ctx.evaluate(widget-bridge.js)`.
- Вызов функции: `ctx.globalObject.getJSFunction("__gtdRunCompute")` → `JSFunction`;
  `f.call(argString)`; результат-строку читаем через `f.call(...) as String`.
  Полученные `JSObject`/`JSFunction` освобождать `.release()`.
- Строки передаются как обычные Java `String` (аргументы и возврат).
- Завершение: `ctx.destroy()`.

Контракт моста: `__gtdRunCompute(json)` (запускает `computeWidgetData`, кладёт
результат в глобал), `__gtdReadCompute()` → `"OK:"+JSON | "ERR:"+msg`,
`__gtdBuildCapture(text, location)`, `__gtdCaptureTarget(dataJson, namespace)`.

## 8. Обновление widget-core.js

См. `WIDGET_CORE_VERSION.md`. Кратко: в репо плагина
`npm run build:widget-core`, затем в этом репо `gradlew :app:vendorWidgetCore`.
