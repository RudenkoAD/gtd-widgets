# Версия движка (widget-core.js)

Ассет `app/src/main/assets/widget-core.js` — это IIFE-бандл ядра (глобал
`GtdWidgetCore`), собранный из исходников плагина GTD Flow.

| Параметр | Значение |
| --- | --- |
| Плагин-источник | `D:\projects\claude_home\calendar_app` (GTD Flow) |
| Версия плагина | **0.8.0** (`manifest.json`) |
| Коммит-источник | `4d9fca4869841b5060a6fcc12d75ce28f9d825b5` (ветка `master`) + незакоммиченные правки движка «раунд 13» в `src/widget` (agenda-дни, `itemKind`/`recurrenceText`/`namespace` в элементах, синхронный `buildEditedLine`) + фикс классификации `buildEditedLine`: повторяющаяся ЗАДАЧА (🔁 + поле-дата 📅/⏳/🛫) правится как задача, а не серия |
| Команда сборки | `npm run build:widget-core` → `node esbuild.widget.mjs` |
| Артефакт | `widget-core.js`, 85268 байт (UTF-8) |
| SHA-256 | `afb881a2d85062a28bcfc5dec034a7769b114c42a007d6aef6c2ce7ca34b9473` |

## Обновление ассета

1. В репозитории плагина: `npm run build:widget-core` (пересобирает `widget-core.js`).
2. В этом репозитории: `gradlew :app:vendorWidgetCore`
   (копирует `widget-core.js` из плагина в `app/src/main/assets/`).
   При нестандартном пути: `gradlew :app:vendorWidgetCore -Pgtd.widgetCoreSrc=<путь>`.
3. Обновите таблицу выше (версия/коммит/размер/хеш).

Мост Kotlin↔ядро — `app/src/main/assets/widget-bridge.js` (обёртка над
`computeWidgetData`/`buildCaptureLine`/`captureTargetPath`/`buildEditedLine`), он в
репозитории и рукописный — при смене контракта ядра правится вручную.
