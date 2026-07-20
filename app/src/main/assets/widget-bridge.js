/*
 * Мост между Kotlin (QuickJS wrapper) и GtdWidgetCore.
 *
 * computeWidgetData АСИНХРОННА, но внутри ждёт ТОЛЬКО микрозадачи (chunkSize=∞,
 * NOOP-события, без setTimeout — см. handoff JS-зоны). QuickJS-обёртка
 * (wang.harlon.quickjs) после КАЖДОГО call()/evaluate() прогоняет
 * JS_ExecutePendingJob в цикле до опустошения очереди заданий. Значит: вызываем
 * __gtdRunCompute(json) — обёртка сама доводит промис до резолва и наш .then
 * успевает записать результат в глобал ДО возврата из call(). Затем синхронно
 * читаем __gtdReadCompute(): 'OK:' + JSON | 'ERR:' + сообщение.
 *
 * Синхронные экспорты (buildCaptureLine/captureTargetPath) зовём напрямую —
 * возвращают строку сразу; пустой текст захвата бросает исключение (ловим в Kotlin).
 */
// Минимальный console-шим: контекст QuickJS (wang.harlon.quickjs) без
// QuickJSLoader.initConsoleLog НЕ имеет глобала console. Ядро зовёт console.error
// только на аварийных ветках (например, сбой первичного скана), но если такой
// вызов случится без шима — это ReferenceError, который замаскирует настоящую
// причину. Определяем no-op console ДО первого вызова ядра.
if (typeof console === "undefined") {
    var console = {
        log: function () {},
        info: function () {},
        warn: function () {},
        error: function () {},
        debug: function () {},
    };
}

var __gtdResult = null;
var __gtdError = null;

function __gtdRunCompute(inputJson) {
    __gtdResult = null;
    __gtdError = null;
    var input;
    try {
        input = JSON.parse(inputJson);
    } catch (e) {
        __gtdError = "bad input json: " + e;
        return;
    }
    GtdWidgetCore.computeWidgetData(input).then(
        function (s) {
            __gtdResult = s;
        },
        function (e) {
            __gtdError = e && e.stack ? String(e.stack) : String(e);
        },
    );
}

function __gtdReadCompute() {
    if (__gtdError !== null) return "ERR:" + __gtdError;
    if (__gtdResult === null) return "ERR:compute did not settle";
    return "OK:" + __gtdResult;
}

function __gtdBuildCapture(text, location) {
    return GtdWidgetCore.buildCaptureLine(text, location == null ? null : location);
}

function __gtdCaptureTarget(dataJson, namespace) {
    return GtdWidgetCore.captureTargetPath(
        dataJson == null ? null : dataJson,
        namespace == null ? null : namespace,
    );
}

/*
 * Синхронная правка строки задачи/события для «шторки деталей». editsJson —
 * JSON-объект {title?,date?,timeRange?,location?} (Kotlin строит его так, чтобы
 * ПРИСУТСТВИЕ ключа означало «применить», а null — «снять поле»; отсутствие ключа —
 * «не трогать»). Ядро возвращает уже готовую JSON-строку {ok:true,line}|{ok:false,error} —
 * прокидываем её как есть (Kotlin разбирает). Битый editsJson не роняет вызов:
 * возвращаем тот же контракт с ok:false, чтобы сторона Kotlin показала текст ошибки.
 */
function __gtdBuildEditedLine(rawLine, editsJson) {
    var edits;
    try {
        edits = editsJson == null || editsJson === "" ? {} : JSON.parse(editsJson);
    } catch (e) {
        return JSON.stringify({ ok: false, error: "bad edits json: " + e });
    }
    return GtdWidgetCore.buildEditedLine(rawLine, edits);
}
