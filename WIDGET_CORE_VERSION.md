# Версия движка (widget-core.js)

Ассет `app/src/main/assets/widget-core.js` — это IIFE-бандл ядра (глобал
`GtdWidgetCore`), собранный из исходников плагина GTD Flow.

| Параметр | Значение |
| --- | --- |
| Плагин-источник | `RudenkoAD/gtd-flow` (GTD Flow; канонический репозиторий плагина) |
| Версия плагина | **0.9.1** (`manifest.json`) |
| Коммит-источник | `bd3d88b` (ветка `master`) — уже включает правки «раунда 13» и widget-core v2 (коммит `0b8504d`) |
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
