# Версия движка (widget-core.js)

Ассет `app/src/main/assets/widget-core.js` — это IIFE-бандл ядра (глобал
`GtdWidgetCore`), собранный из исходников плагина GTD Flow.

| Параметр | Значение |
| --- | --- |
| Плагин-источник | `D:\projects\claude_home\calendar_app` (GTD Flow) |
| Версия плагина | **0.8.0** (`manifest.json`) |
| Коммит-источник | `4d9fca4869841b5060a6fcc12d75ce28f9d825b5` (ветка `master`, «widget-core: JS-бандл ядра для Android-виджетов») |
| Команда сборки | `npm run build:widget-core` → `node esbuild.widget.mjs` |
| Артефакт | `widget-core.js`, 73852 байта |
| SHA-256 | `100211e74a27323a86d00491fe12b7c40293ec9cfb66815cd882f9243bf953af` |

## Обновление ассета

1. В репозитории плагина: `npm run build:widget-core` (пересобирает `widget-core.js`).
2. В этом репозитории: `gradlew :app:vendorWidgetCore`
   (копирует `widget-core.js` из плагина в `app/src/main/assets/`).
   При нестандартном пути: `gradlew :app:vendorWidgetCore -Pgtd.widgetCoreSrc=<путь>`.
3. Обновите таблицу выше (версия/коммит/размер/хеш).

Мост Kotlin↔ядро — `app/src/main/assets/widget-bridge.js` (обёртка над
`computeWidgetData`/`buildCaptureLine`/`captureTargetPath`), он в репозитории и
рукописный — при смене контракта ядра правится вручную.
