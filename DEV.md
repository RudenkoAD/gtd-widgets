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

- **`QuickJSLoader.init()` ОБЯЗАТЕЛЕН до первого `QuickJSContext.create()`.**
  Обёртка НЕ грузит `.so` автоматически: в `QuickJSContext` нет статического
  инициализатора, а `QuickJSLoader.init()` (пакет `com.whl.quickjs.android`)
  делает `System.loadLibrary("quickjs-android-wrapper")`. Без него конструктор
  `QuickJSContext` бросает `QuickJSException: The so library must be initialized
  before createContext! QuickJSLoader.init should be called…`, а сырой JNI-вызов —
  `UnsatisfiedLinkError: No implementation found for … createRuntime()`. Это был
  корень дефекта «креш по Собрать Превью» + «виджеты вечно Загрузка…» (JVM-тесты
  зелёные, т.к. QuickJS в них не вызывается). `QuickJsEngine.create()` вызывает
  `ensureNativeLoaded()` один раз и заворачивает ЛЮБОЙ сбой инициализации в
  `EngineException`, чтобы превью/виджеты показали текст ошибки, а не падали.
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

## 9. Проверка JNI-пути на эмуляторе (QuickJS вне JVM)

JVM-тесты НЕ исполняют нативный QuickJS (нет `.so`), поэтому дефекты уровня JNI
(например, незагруженная библиотека — см. §7) ловятся ТОЛЬКО на устройстве/эмуляторе.
Чистая логика границы движка вынесена и покрыта тестами (`EngineReplyTest`,
`WidgetErrorTextTest`, `WidgetBodyStateTest`); нативную часть проверяем так:

**Эмулятор без прав администратора (Windows 10).** Стоит устаревший, но рабочий
HAXM (`googlehaxm`/`IntelHaxm` — `Running`). Emulator 36 (из `sdkmanager`) HAXM НЕ
принимает (`FATAL: HAXM no longer supported`), а WHPX/AEHD требуют прав админа.
Решение — портативный emulator 33.1.24 (поддерживает HAXM), распакованный на `D:`:

```powershell
# один раз: система-образ и AVD (хранилище AVD — на D:, C: почти полон)
$env:JAVA_HOME='D:\gtd-toolchain\jdk-17'; $SDK='D:\gtd-toolchain\Android\Sdk'
$env:ANDROID_AVD_HOME='D:\gtd-toolchain\avd'
& "$SDK\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=$SDK "system-images;android-35;google_apis;x86_64"
'no' | & "$SDK\cmdline-tools\latest\bin\avdmanager.bat" create avd -n gtdtest -k "system-images;android-35;google_apis;x86_64" -d pixel_5 --force
# emulator 33.1.24 (HAXM): dl.google.com/android/repository/emulator-windows_x64-11237101.zip → D:\gtd-toolchain\emu33
# запуск (headless, HAXM):
$env:ANDROID_SDK_ROOT=$SDK; $env:ANDROID_HOME=$SDK
Start-Process 'D:\gtd-toolchain\emu33\emulator\emulator.exe' -ArgumentList `
  '-avd','gtdtest','-no-snapshot','-no-boot-anim','-no-audio','-gpu','swiftshader_indirect','-no-window' -WindowStyle Hidden
```

Ждать загрузки: `adb wait-for-device` + опрос `adb shell getprop sys.boot_completed`=`1`.

**Важно (Git Bash):** для `adb push`/аргументов вида `/sdcard/...` выставляй
`MSYS_NO_PATHCONV=1` — иначе MSYS ломает путь в `C:/Program Files/Git/sdcard/...`.

**Тестовый vault** (`adb push <vault>/. /sdcard/Documents/testvault`): каталог с
`.obsidian/plugins/gtd-flow/data.json` (`commonRoot`, `namespaces`),
`GTD/Inbox.md` (frontmatter `gtd-inbox: true` + задачи; задача с `📅 <сегодня>
HH:MM 📍 …` уходит в «Сегодня»), `Work/Events.md` (`gtd-events: true`;
`📅 <сегодня> HH:MM 📍 …` — разовое событие, `🔁 every monday at 10:00 📍 …` — серия).

**Доступ (SAF):** пикер проходится UI-автоматизацией — `adb shell uiautomator dump
/sdcard/uidump.xml`, разбор bounds, `adb shell input tap X Y`: внутреннее хранилище →
Documents → testvault → «USE THIS FOLDER» → «ALLOW». Персистентный грант переживает
`adb install -r`, поэтому настраивается один раз.

**Прогон:** запустить `adb shell am start -n com.gtdflow.widget/.ui.MainActivity`,
тап «Собрать превью» → на экране данные vault (не креш). Скрин:
`adb exec-out screencap -p > verify\preview.png`. Логи сбоев: `adb logcat -b crash -d`.

**Компоненты раунда 13** (проверять на эмуляторе — виджеты и оверлеи):

- Ресиверы виджетов: `.today.TodayWidgetReceiver`, `.agenda.AgendaWidgetReceiver`,
  `.inbox.InboxWidgetReceiver`. Конфиг-активити (запускаются при добавлении):
  `.inbox.InboxConfigActivity` (пространство, дефолт «Все» первым),
  `.agenda.AgendaConfigActivity` (7/14/30 дней).
- Оверлеи (translucent-тема `Theme.GtdFlow.BottomSheet`, прижаты к низу):
  `.ui.EventSheetActivity` (шторка события — тап по элементу «Сегодня»/«Агенды»),
  `.ui.InboxTaskSheetActivity` (правка входящей — тап по задаче),
  `.ui.CaptureActivity` (быстрый захват `＋`, автофокус+клавиатура, серийный ввод).
  Оверлеи открываются из виджетов через `actionStartActivity`. Точки риска для
  эмулятор-проверки: подъём листа над IME (`imePadding`, `WindowCompat.setDecorFitsSystemWindows(false)`
  + `windowSoftInputMode=adjustResize`) при `windowIsTranslucent`; показ клавиатуры
  в захвате (`FocusRequester`+`LocalSoftwareKeyboardController.show()`).
- Экшены (Glance `ActionCallback`): `.inbox.InboxToggleAction` (чекбокс),
  `.work.RefreshWidgetsAction` (тап по заголовку). Прямая проверка шторки без виджета:
  `adb shell am start -n com.gtdflow.widget/.ui.CaptureActivity --es namespace Все`.

## 10. Профилирование лага виджета (метки `GtdPerf`)

Путь «действие в виджете → обновление UI» размечен лёгкими метками `Log.d` под тегом
**`GtdPerf`** (класс `perf/Perf.kt`). Метки постоянные (Log.d дёшев) — это штатный
инструмент на реальном устройстве, где JVM-профайлер недоступен.

**Снять таймлайн одного действия:**

```powershell
$env:PATH += ';D:\gtd-toolchain\Android\Sdk\platform-tools'
adb logcat -c                       # очистить буфер
# ... тап по чекбоксу/захват/правка в виджете ...
adb logcat -d -s GtdPerf            # выгрузить метки
```

**Метки сценария «чекбокс Входящие»** (в порядке пути):

| Метка | Что мерит |
| --- | --- |
| `toggle.received` | экшен получен (тап пришёл в процесс) |
| `toggle.optimistic took=…` | оптимистичное удаление строки из кэша + `updateAll` — **видимый отклик** |
| `toggle.write took=…` | запись `- [x]` в файл (SAF read+write) |
| `refresh.start` / `refresh.done total=…` | границы полного пересчёта (инлайн) |
| `scan … enumMs=… readMs=… cacheHits=… cacheReads=…` | обход vault: перечисление / чтение содержимого / попадания кэша / реально прочитано файлов |
| `engine.compute took=… kind=…` | один вызов QuickJS (today/inbox/agenda) |
| `refresh.update took=…` | `updateAll` всех виджетов |
| `refresh.coalesced.skip` | повторный пересчёт слит с идущим (двойной тап/периодика) |

Дельту «тап → видимый отклик» считать как `toggle.optimistic` (≈ момент, когда строка
исчезает). Дельту «тап → честное состояние» — от `toggle.received` до `refresh.done`.

**Архитектура быстрого отклика (раунд оптимизации лага):**

- **Оптимистично + инлайн, без WorkManager.** Интерактивные действия
  (`InboxToggleAction`, `CaptureService`, `EditService`) сразу патчат кэш секции
  (`inbox/InboxOptimistic` + `work/OptimisticInbox`) и дёргают `updateAll`, затем
  запускают полный пересчёт ИНЛАЙН со слиянием (`WidgetService.refreshCoalesced`) в
  процессной `work/AppScope` (переживает закрытие оверлея/goAsync). WorkManager остался
  ТОЛЬКО в периодике (`RefreshScheduler.ensurePeriodic`); латентность его постановки
  (сотни мс) убрана с пути интеракции.
- **Кэш содержимого по mtime** (`vault/VaultContentCache`, процессный singleton
  `shared`): `VaultReader` перечисляет детей одним курсором с колонкой
  `COLUMN_LAST_MODIFIED` и перечитывает через SAF только изменённые файлы; неизменённые
  берёт из памяти. `VaultWriter` после нашей записи инвалидирует docId (не отдать старое
  при грубом mtime). Эффект виден в `scan`: `readMs` и `cacheReads` падают на порядок
  (полный скан ~150 файлов: холодный `readMs≈3000мс`, тёплый `readMs≈40мс`).
- **Слияние пересчётов** (`refreshCoalesced`, mutex + флаг `dirty`): двойной тап или
  тап+периодика не запускают второй тяжёлый проход — активный видит `dirty` и
  прогоняет ещё раз. Метка `refresh.coalesced.skip`.

Юнит-тесты чистой логики: `inbox/InboxOptimisticTest` (патчи remove/edit/prepend),
`vault/VaultContentCacheTest` (попадание/промах по mtime, инвалидация, сброс vault).
