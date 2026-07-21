# Версия движка (widget-core.js)

Ассет `app/src/main/assets/widget-core.js` — это IIFE-бандл ядра (глобал
`GtdWidgetCore`), собранный из исходников плагина GTD Flow.

| Параметр | Значение |
| --- | --- |
| Плагин-источник | `RudenkoAD/gtd-flow` (GTD Flow; канонический репозиторий плагина) |
| Версия плагина | **0.10.0** (`manifest.json`) |
| Коммит-источник | `3581887` (ветка `master`) — включает `every!` (from-completion recurrence, отказ серий-событий `series-completion-not-allowed`) и «гигиену» PR #1–#2 |
| Команда сборки | `npm run build:widget-core` → `node esbuild.widget.mjs` |
| Артефакт | `widget-core.js`, 88499 байт (UTF-8) |
| SHA-256 | `d0aeec188319c452e80cda6569d056054110124fce5860ff1e981f380ca7e22c` |

## Обновление ассета

1. В репозитории плагина: `npm run build:widget-core` (пересобирает `widget-core.js`).
2. В этом репозитории: `gradlew :app:vendorWidgetCore`
   (копирует `widget-core.js` из плагина в `app/src/main/assets/`).
   При нестандартном пути: `gradlew :app:vendorWidgetCore -Pgtd.widgetCoreSrc=<путь>`.
3. Обновите таблицу выше (версия/коммит/размер/хеш).

Мост Kotlin↔ядро — `app/src/main/assets/widget-bridge.js` (обёртка над
`computeWidgetData`/`buildCaptureLine`/`captureTargetPath`/`buildEditedLine`), он в
репозитории и рукописный — при смене контракта ядра правится вручную.
