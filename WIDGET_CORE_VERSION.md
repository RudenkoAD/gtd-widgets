# Версия движка (widget-core.js)

Ассет `app/src/main/assets/widget-core.js` — это IIFE-бандл ядра (глобал
`GtdWidgetCore`), собранный из исходников плагина GTD Flow.

| Параметр | Значение |
| --- | --- |
| Плагин-источник | `RudenkoAD/gtd-flow` (GTD Flow; канонический репозиторий плагина) |
| Версия плагина | **0.10.0** (`manifest.json`) |
| Коммит-источник | `bf77c45` (ветка `master`) — вдобавок к `every!` грамматика повторов читает Tasks-синтаксис «when done» как алиас `every` (повтор от даты выполнения) |
| Команда сборки | `npm run build:widget-core` → `node esbuild.widget.mjs` |
| Артефакт | `widget-core.js`, 88973 байт (UTF-8) |
| SHA-256 | `7c896c9c0bbcfb31d0b46d525587d2ab8775aa612f40cbfea9f06e70d462944a` |

## Обновление ассета

1. В репозитории плагина: `npm run build:widget-core` (пересобирает `widget-core.js`).
2. В этом репозитории: `gradlew :app:vendorWidgetCore`
   (копирует `widget-core.js` из плагина в `app/src/main/assets/`).
   При нестандартном пути: `gradlew :app:vendorWidgetCore -Pgtd.widgetCoreSrc=<путь>`.
3. Обновите таблицу выше (версия/коммит/размер/хеш).

Мост Kotlin↔ядро — `app/src/main/assets/widget-bridge.js` (обёртка над
`computeWidgetData`/`buildCaptureLine`/`captureTargetPath`/`buildEditedLine`), он в
репозитории и рукописный — при смене контракта ядра правится вручную.
