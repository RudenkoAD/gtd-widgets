/*
GENERATED/BUNDLED FILE BY ESBUILD — GTD Flow widget core (QuickJS).
Source: src/widget (see the plugin's GitHub repository).
No node/DOM/npm runtime deps: pure core + services with input-provided files/time.
*/
"use strict";
var GtdWidgetCore = (() => {
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, { get: all[name], enumerable: true });
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

  // src/widget/widgetCore.ts
  var widgetCore_exports = {};
  __export(widgetCore_exports, {
    DEFAULT_NS: () => DEFAULT_NS,
    buildCaptureLine: () => buildCaptureLine,
    buildEditedLine: () => buildEditedLine,
    captureTargetPath: () => captureTargetPath,
    computeWidgetData: () => computeWidgetData
  });

  // src/core/namespace/namespace.ts
  var DEFAULT_NS = String.fromCharCode(0) + "default";
  var ALL_NS = String.fromCharCode(0) + "all";
  function normalizeNsPath(path) {
    let s = path.trim();
    while (s.length > 0 && s.endsWith("/")) s = s.slice(0, -1);
    return s;
  }
  function normalizeNsName(raw) {
    if (typeof raw !== "string") return null;
    const s = raw.trim();
    return s === "" ? null : s;
  }
  function resolveNamespace(filePath, nsOverride, defs) {
    const override = normalizeNsName(nsOverride);
    if (override !== null) return override;
    const path = normalizeNsPath(filePath);
    let bestName = null;
    let bestLen = -1;
    for (const d of defs) {
      const root = normalizeNsPath(d.root);
      if (root === "") continue;
      if (path === root || path.startsWith(root + "/")) {
        if (root.length > bestLen) {
          bestLen = root.length;
          bestName = d.name;
        }
      }
    }
    return bestName ?? DEFAULT_NS;
  }
  function nsRoot(name, defs) {
    for (const d of defs) if (d.name === name) return normalizeNsPath(d.root);
    return null;
  }
  function inNamespace(filePath, nsOverride, filter) {
    if (filter.defs.length === 0) return true;
    if (filter.active === ALL_NS) return true;
    return resolveNamespace(filePath, nsOverride, filter.defs) === filter.active;
  }
  function normalizeActiveNamespace(active, defs, allowAll = false) {
    if (allowAll && active === ALL_NS) return ALL_NS;
    if (active === DEFAULT_NS) return DEFAULT_NS;
    return defs.some((d) => d.name === active) ? active : DEFAULT_NS;
  }
  var NS_CONVENTION = {
    /** Фолбэк-цель захвата и spawn-target именованного пространства. */
    inbox: "\u0412\u0445\u043E\u0434\u044F\u0449\u0438\u0435.md",
    /** Файл серий-событий именованного пространства. */
    events: "\u0421\u043E\u0431\u044B\u0442\u0438\u044F.md",
    /** Файл-приёмник архивирования именованного пространства. */
    archive: "\u0410\u0440\u0445\u0438\u0432.md",
    /** Фолбэк-файл шаблонов регулярных именованного пространства. */
    recurring: "\u0420\u0435\u0433\u0443\u043B\u044F\u0440\u043D\u044B\u0435.md",
    /** Каталог новых досок именованного пространства. */
    boardsDir: "\u0414\u043E\u0441\u043A\u0438",
    /** Каталог новых проектов именованного пространства. */
    projectsDir: "\u041F\u0440\u043E\u0435\u043A\u0442\u044B"
  };
  function nsCommonTarget(name, defs, suffix, commonRoot) {
    const named = nsRoot(name, defs);
    const root = named === null || named === "" ? normalizeNsPath(commonRoot) : named;
    return root === "" ? suffix : `${root}/${suffix}`;
  }

  // src/core/parser/emoji.ts
  var DATE_FIELD_EMOJI = {
    due: "\u{1F4C5}",
    scheduled: "\u23F3",
    start: "\u{1F6EB}",
    created: "\u2795",
    done: "\u2705",
    cancelled: "\u274C",
    nextSpawn: "\u{1F51C}"
  };
  var VALUE_FIELD_EMOJI = {
    recurrence: "\u{1F501}",
    // текст правила до следующего поля
    id: "\u{1F194}",
    // один токен
    dependsOn: "\u26D4",
    // список id через запятую без пробелов
    spawnedFrom: "\u{1F9EC}",
    // один токен
    excludedDates: "\u{1F6AB}",
    // список дат-исключений вхождений серии через запятую без пробелов
    location: "\u{1F4CD}"
    // свободный текст места/адреса до следующего поля (как 🔁)
  };
  var PRIORITY_EMOJI = {
    highest: "\u{1F53A}",
    high: "\u23EB",
    medium: "\u{1F53C}",
    low: "\u{1F53D}",
    lowest: "\u23EC"
  };
  var EMOJI_TO_PRIORITY = new Map(
    Object.entries(PRIORITY_EMOJI).map(([p, e]) => [e, p])
  );
  var ALL_FIELD_EMOJI = [
    ...Object.values(DATE_FIELD_EMOJI),
    ...Object.values(VALUE_FIELD_EMOJI),
    ...Object.values(PRIORITY_EMOJI)
  ];

  // src/core/parser/tokenizer.ts
  var TIMED_DATE_FIELDS = /* @__PURE__ */ new Set(["due", "scheduled", "start"]);
  function isTimedDateField(field) {
    return TIMED_DATE_FIELDS.has(field);
  }
  var TIME_RE = /^([01]\d|2[0-3]):[0-5]\d$/;
  var DATE_SHAPE_RE = /^\d{4}-\d{2}-\d{2}$/;
  var HEAD_RE = /^([ \t]*)([-*+])([ \t]+)\[(.)\](?=\s|$)/u;
  var BLOCK_REF_RE = /(\s+)(\^[A-Za-z0-9-]+)(\s*)$/;
  var FIELD_EMOJI_DESC = [...ALL_FIELD_EMOJI].sort(
    (a, b) => b.length - a.length
  );
  var FIELD_OF_EMOJI = (() => {
    const m = /* @__PURE__ */ new Map();
    for (const [name, e] of Object.entries(DATE_FIELD_EMOJI))
      m.set(e, name);
    for (const [name, e] of Object.entries(VALUE_FIELD_EMOJI))
      m.set(e, name);
    for (const e of Object.values(PRIORITY_EMOJI)) m.set(e, "priority");
    return m;
  })();
  function isWs(ch) {
    return ch !== "" && /\s/.test(ch);
  }
  function matchFieldEmoji(s, i) {
    for (const e of FIELD_EMOJI_DESC) {
      if (s.startsWith(e, i)) {
        const emoji = s.charCodeAt(i + e.length) === 65039 ? s.slice(i, i + e.length + 1) : e;
        const field = FIELD_OF_EMOJI.get(e);
        if (field !== void 0) return { emoji, field };
      }
    }
    return null;
  }
  function scanToken(s, from) {
    let j = from;
    while (j < s.length && !isWs(s.charAt(j)) && s.charAt(j) !== "," && !matchFieldEmoji(s, j)) j++;
    return j;
  }
  function tagStartsAt(s, j) {
    if (s.charAt(j) !== "#") return false;
    const prev = j > 0 ? s.charAt(j - 1) : "";
    if (prev === "#" || isTagChar(prev)) return false;
    let k = j + 1;
    while (k < s.length && isTagChar(s.charAt(k))) k++;
    const body = s.slice(j + 1, k);
    return body !== "" && /[^0-9]/.test(body);
  }
  function scanLocationPayload(s, from) {
    let j = from;
    while (j < s.length && matchFieldEmoji(s, j) === null && !tagStartsAt(s, j)) j++;
    while (j > from && isWs(s.charAt(j - 1))) j--;
    return j;
  }
  function isLeadingLocation(rest, i) {
    return rest.slice(0, i).trim() === "";
  }
  function scanDateTimeToken(s, from) {
    const dateEnd = scanToken(s, from);
    if (!DATE_SHAPE_RE.test(s.slice(from, dateEnd))) return dateEnd;
    let k = dateEnd;
    while (k < s.length && isWs(s.charAt(k))) k++;
    if (k === dateEnd) return dateEnd;
    const tokEnd = scanToken(s, k);
    if (tokEnd === k) return dateEnd;
    const tok = s.slice(k, tokEnd);
    if (TIME_RE.test(tok)) return tokEnd;
    const startPart = tok.slice(0, 5);
    if (!TIME_RE.test(startPart) || tok.charAt(5) !== "-") return dateEnd;
    const endPart = tok.slice(6);
    if (TIME_RE.test(endPart) && endPart > startPart) return tokEnd;
    return k + 5;
  }
  function scanCommaList(s, from) {
    let j = scanToken(s, from);
    if (j === from) return j;
    for (; ; ) {
      let k = j;
      while (k < s.length && isWs(s.charAt(k))) k++;
      if (s.charAt(k) !== ",") return j;
      k++;
      while (k < s.length && isWs(s.charAt(k))) k++;
      const e = scanToken(s, k);
      if (e === k) return j;
      j = e;
    }
  }
  function tokenizeSegments(rest, opts = {}) {
    const parseLocation = opts.location !== false;
    const segs = [];
    let textStart = 0;
    let i = 0;
    const flushText = (end) => {
      if (end > textStart) segs.push({ kind: "text", text: rest.slice(textStart, end) });
    };
    while (i < rest.length) {
      const m = matchFieldEmoji(rest, i);
      if (m === null || m.field === "location" && (!parseLocation || isLeadingLocation(rest, i))) {
        i++;
        continue;
      }
      flushText(i);
      i += m.emoji.length;
      if (m.field === "priority") {
        segs.push({ kind: "field", field: "priority", emoji: m.emoji, gap: "", payload: "" });
        textStart = i;
        continue;
      }
      let g = i;
      while (g < rest.length && isWs(rest.charAt(g))) g++;
      const gap = rest.slice(i, g);
      i = g;
      let payloadEnd;
      if (m.field === "recurrence") {
        let j = i;
        while (j < rest.length && matchFieldEmoji(rest, j) === null) j++;
        while (j > i && isWs(rest.charAt(j - 1))) j--;
        payloadEnd = j;
      } else if (m.field === "location") {
        payloadEnd = scanLocationPayload(rest, i);
      } else if (m.field === "dependsOn" || m.field === "excludedDates") {
        payloadEnd = scanCommaList(rest, i);
      } else if (isTimedDateField(m.field)) {
        payloadEnd = scanDateTimeToken(rest, i);
      } else {
        payloadEnd = scanToken(rest, i);
      }
      segs.push({
        kind: "field",
        field: m.field,
        emoji: m.emoji,
        gap,
        payload: rest.slice(i, payloadEnd)
      });
      i = payloadEnd;
      textStart = i;
    }
    flushText(rest.length);
    return segs;
  }
  function tokenizeTaskLine(rawLine, opts = {}) {
    if (rawLine.includes("\n")) return null;
    const trailingCr = rawLine.endsWith("\r") ? "\r" : "";
    const line = trailingCr === "" ? rawLine : rawLine.slice(0, -1);
    const h = HEAD_RE.exec(line);
    if (h === null) return null;
    let rest = line.slice(h[0].length);
    let blockRef = null;
    const b = BLOCK_REF_RE.exec(rest);
    if (b !== null) {
      blockRef = { spacing: b[1], ref: b[2], trailing: b[3] };
      rest = rest.slice(0, b.index);
    }
    return {
      indent: h[1],
      bullet: h[2],
      afterBullet: h[3],
      statusChar: h[4],
      segments: tokenizeSegments(rest, opts),
      blockRef,
      trailingCr
    };
  }
  function serializeTokens(t) {
    let s = `${t.indent}${t.bullet}${t.afterBullet}[${t.statusChar}]`;
    for (const seg of t.segments) {
      s += seg.kind === "text" ? seg.text : seg.emoji + seg.gap + seg.payload;
    }
    if (t.blockRef !== null) s += t.blockRef.spacing + t.blockRef.ref + t.blockRef.trailing;
    return s + t.trailingCr;
  }
  function isTagChar(ch) {
    if (ch === "") return false;
    if (/[0-9A-Za-z_/-]/.test(ch)) return true;
    return ch.charCodeAt(0) > 127 && !/\s/.test(ch);
  }
  function extractTags(text) {
    const out = [];
    let i = 0;
    while (i < text.length) {
      if (text.charAt(i) !== "#") {
        i++;
        continue;
      }
      const prev = i > 0 ? text.charAt(i - 1) : "";
      if (prev === "#" || isTagChar(prev)) {
        i++;
        continue;
      }
      let j = i + 1;
      while (j < text.length && isTagChar(text.charAt(j))) j++;
      const body = text.slice(i + 1, j);
      if (body !== "" && /[^0-9]/.test(body)) out.push(text.slice(i, j));
      i = j > i + 1 ? j : i + 1;
    }
    return out;
  }

  // src/core/parser/taskKey.ts
  function fnv1a(text) {
    let h = 2166136261;
    for (const ch of text) {
      const cp = ch.codePointAt(0);
      if (cp < 128) {
        h = mix(h, cp);
      } else if (cp < 2048) {
        h = mix(h, 192 | cp >> 6);
        h = mix(h, 128 | cp & 63);
      } else if (cp < 65536) {
        h = mix(h, 224 | cp >> 12);
        h = mix(h, 128 | cp >> 6 & 63);
        h = mix(h, 128 | cp & 63);
      } else {
        h = mix(h, 240 | cp >> 18);
        h = mix(h, 128 | cp >> 12 & 63);
        h = mix(h, 128 | cp >> 6 & 63);
        h = mix(h, 128 | cp & 63);
      }
    }
    return h >>> 0;
  }
  function mix(h, byte) {
    return Math.imul(h ^ byte, 16777619);
  }
  function normalizeDescription(input) {
    const line = tokenizeTaskLine(input);
    const segments = line !== null ? line.segments : tokenizeSegments(input);
    let text = "";
    for (const s of segments) if (s.kind === "text") text += s.text;
    return text.replace(/\s+/g, " ").trim();
  }
  function computeKey(src, occurrenceIndex = 0) {
    if (src.taskId !== null && src.taskId !== "") return `id:${src.taskId}`;
    const hash = fnv1a(normalizeDescription(src.description)).toString(16).padStart(8, "0");
    return `${src.filePath}#${hash}#${occurrenceIndex}`;
  }

  // src/core/parser/parseTaskLine.ts
  var ISO_DATE_RE = /^(\d{4})-(\d{2})-(\d{2})$/;
  var OFFSET_RE = /^([+-])(\d{1,3})d$/;
  function daysInMonth(y, m) {
    if (m === 2) return y % 4 === 0 && y % 100 !== 0 || y % 400 === 0 ? 29 : 28;
    return [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m - 1];
  }
  function parseDatePayload(payload) {
    if (payload === "") return { kind: "empty" };
    const d = ISO_DATE_RE.exec(payload);
    if (d !== null) {
      const year = Number(d[1]);
      const month = Number(d[2]);
      const day = Number(d[3]);
      if (month >= 1 && month <= 12 && day >= 1 && day <= daysInMonth(year, month))
        return { kind: "date", date: payload };
      return { kind: "invalid", raw: payload };
    }
    const o = OFFSET_RE.exec(payload);
    if (o !== null) {
      return { kind: "offset", offset: { sign: o[1] === "-" ? -1 : 1, days: Number(o[2]) } };
    }
    return { kind: "invalid", raw: payload };
  }
  function splitDateTimePayload(payload) {
    const m = /^(\S+)\s+(\S+)$/.exec(payload);
    if (m !== null) {
      const tok = m[2];
      if (TIME_RE.test(tok)) return { datePart: m[1], time: tok, timeEnd: null };
      const startPart = tok.slice(0, 5);
      const endPart = tok.slice(6);
      if (tok.charAt(5) === "-" && TIME_RE.test(startPart) && TIME_RE.test(endPart) && endPart > startPart) {
        return { datePart: m[1], time: startPart, timeEnd: endPart };
      }
    }
    return { datePart: payload, time: null, timeEnd: null };
  }
  function parseExcludedDates(payload) {
    const out = [];
    for (const part of payload.split(",")) {
      const s = part.trim();
      if (s === "") continue;
      if (parseDatePayload(s).kind === "date") out.push(s);
    }
    return out;
  }
  function stripVariationSelector(emoji) {
    let out = "";
    for (let i = 0; i < emoji.length; i++) {
      if (emoji.charCodeAt(i) !== 65039) out += emoji.charAt(i);
    }
    return out;
  }
  function parseTaskLine(rawLine, ctx) {
    const tok = tokenizeTaskLine(rawLine);
    if (tok === null) return null;
    const dates = {
      due: null,
      scheduled: null,
      start: null,
      created: null,
      done: null,
      cancelled: null,
      nextSpawn: null
    };
    const times = {
      due: null,
      scheduled: null,
      start: null
    };
    const timeEnds = {
      due: null,
      scheduled: null,
      start: null
    };
    let recurrence = null;
    let taskId = null;
    let spawnedFrom = null;
    let dependsOn = [];
    let excludedDates = [];
    let location = null;
    let priority = "none";
    const textParts = [];
    const tags = [];
    for (const seg of tok.segments) {
      if (seg.kind === "text") {
        textParts.push(seg.text);
        for (const t of extractTags(seg.text)) if (!tags.includes(t)) tags.push(t);
        continue;
      }
      switch (seg.field) {
        case "priority": {
          const p = EMOJI_TO_PRIORITY.get(stripVariationSelector(seg.emoji));
          if (p !== void 0) priority = p;
          break;
        }
        case "recurrence": {
          const rule = seg.payload.trim();
          recurrence = rule === "" ? null : rule;
          break;
        }
        case "id": {
          taskId = seg.payload === "" ? null : seg.payload;
          break;
        }
        case "spawnedFrom": {
          spawnedFrom = seg.payload === "" ? null : seg.payload;
          break;
        }
        case "dependsOn": {
          dependsOn = seg.payload.split(",").map((s) => s.trim()).filter((s) => s !== "");
          break;
        }
        case "excludedDates": {
          excludedDates = parseExcludedDates(seg.payload);
          break;
        }
        case "location": {
          const loc = seg.payload.trim();
          location = loc === "" ? null : loc;
          break;
        }
        default: {
          if (isTimedDateField(seg.field)) {
            const { datePart, time, timeEnd } = splitDateTimePayload(seg.payload);
            const parsed = parseDatePayload(datePart);
            const ok = parsed.kind === "date";
            dates[seg.field] = ok ? parsed.date : null;
            times[seg.field] = ok ? time : null;
            timeEnds[seg.field] = ok ? timeEnd : null;
          } else {
            const parsed = parseDatePayload(seg.payload);
            dates[seg.field] = parsed.kind === "date" ? parsed.date : null;
          }
        }
      }
    }
    const description = textParts.join("").replace(/\s+/g, " ").trim();
    return {
      // occurrenceIndex здесь всегда 0 — дизамбигуацию одинаковых строк
      // в одном файле делает индексатор, пересчитывая key через computeKey
      key: computeKey({ taskId, filePath: ctx.filePath, description }, 0),
      taskId,
      filePath: ctx.filePath,
      lineStart: ctx.lineStart,
      lineEnd: ctx.lineStart,
      parentLine: ctx.parentLine,
      heading: ctx.heading,
      description,
      rawLine,
      statusChar: tok.statusChar,
      due: dates.due,
      scheduled: dates.scheduled,
      start: dates.start,
      created: dates.created,
      done: dates.done,
      cancelled: dates.cancelled,
      dueTime: times.due,
      scheduledTime: times.scheduled,
      startTime: times.start,
      dueTimeEnd: timeEnds.due,
      scheduledTimeEnd: timeEnds.scheduled,
      startTimeEnd: timeEnds.start,
      recurrence,
      nextSpawn: dates.nextSpawn,
      spawnedFrom,
      priority,
      dependsOn,
      excludedDates,
      location,
      tags,
      container: ctx.container,
      projectActive: ctx.projectActive,
      nsOverride: ctx.nsOverride ?? null
    };
  }

  // src/core/parser/serializeTaskLine.ts
  function mustTokenize(rawLine, opts) {
    const t = tokenizeTaskLine(rawLine, opts);
    if (t === null) {
      throw new Error(`serializeTaskLine: \u0441\u0442\u0440\u043E\u043A\u0430 \u043D\u0435 \u044F\u0432\u043B\u044F\u0435\u0442\u0441\u044F \u0437\u0430\u0434\u0430\u0447\u0435\u0439: ${JSON.stringify(rawLine)}`);
    }
    return t;
  }
  function assertToken(value, what) {
    if (value === "" || /\s/.test(value) || value.includes(",")) {
      throw new Error(`serializeTaskLine: \u043D\u0435\u0434\u043E\u043F\u0443\u0441\u0442\u0438\u043C\u043E\u0435 \u0437\u043D\u0430\u0447\u0435\u043D\u0438\u0435 ${what}: ${JSON.stringify(value)}`);
    }
  }
  function assertLocation(value) {
    const v = value.replace(/\s+/g, " ").trim();
    if (v === "") {
      throw new Error("serializeTaskLine: \u043F\u0443\u0441\u0442\u0430\u044F \u043B\u043E\u043A\u0430\u0446\u0438\u044F \u{1F4CD} (\u0434\u043B\u044F \u0441\u043D\u044F\u0442\u0438\u044F \u043F\u043E\u043B\u044F \u043F\u0435\u0440\u0435\u0434\u0430\u0439\u0442\u0435 null)");
    }
    for (const e of ALL_FIELD_EMOJI) {
      if (v.includes(e)) {
        throw new Error(`serializeTaskLine: \u044D\u043C\u043E\u0434\u0437\u0438 \u043F\u043E\u043B\u044F \u0432 \u043B\u043E\u043A\u0430\u0446\u0438\u0438 \u{1F4CD}: ${JSON.stringify(value)}`);
      }
    }
    if (extractTags(` ${v}`).length > 0) {
      throw new Error(`serializeTaskLine: #\u0442\u0435\u0433 \u0432 \u043B\u043E\u043A\u0430\u0446\u0438\u0438 \u{1F4CD}: ${JSON.stringify(value)}`);
    }
    return v;
  }
  function fieldIndices(segs, field) {
    const out = [];
    for (let i = 0; i < segs.length; i++) {
      const s = segs[i];
      if (s.kind === "field" && s.field === field) out.push(i);
    }
    return out;
  }
  function removeSegmentAt(segs, idx) {
    const prev = segs[idx - 1];
    const next = segs[idx + 1];
    if (prev !== void 0 && prev.kind === "text" && /\s$/.test(prev.text)) {
      prev.text = prev.text.slice(0, -1);
    } else if (next !== void 0 && next.kind === "text" && /^\s/.test(next.text)) {
      next.text = next.text.slice(1);
    }
    segs.splice(idx, 1);
  }
  function ensureHeadSeparator(segs) {
    const first = segs[0];
    if (first === void 0) return;
    if (first.kind === "text") {
      if (!/^\s/.test(first.text)) first.text = ` ${first.text}`;
    } else {
      segs.unshift({ kind: "text", text: " " });
    }
  }
  function coalesceText(segs) {
    for (let i = segs.length - 1; i >= 0; i--) {
      const s = segs[i];
      if (s.kind !== "text") continue;
      if (s.text === "") {
        segs.splice(i, 1);
        continue;
      }
      const prev = segs[i - 1];
      if (prev !== void 0 && prev.kind === "text") {
        prev.text += s.text;
        segs.splice(i, 1);
      }
    }
  }
  function setPayloadField(rawLine, field, emoji, payload) {
    const t = mustTokenize(rawLine);
    const idxs = fieldIndices(t.segments, field);
    if (payload === null) {
      if (idxs.length === 0) return rawLine;
      for (let k = idxs.length - 1; k >= 0; k--) removeSegmentAt(t.segments, idxs[k]);
      coalesceText(t.segments);
      ensureHeadSeparator(t.segments);
      return serializeTokens(t);
    }
    if (idxs.length > 0) {
      const tok = t.segments[idxs[idxs.length - 1]];
      if (tok.gap === "" && tok.payload === "") tok.gap = " ";
      tok.payload = payload;
      return serializeTokens(t);
    }
    t.segments.push({ kind: "text", text: " " }, { kind: "field", field, emoji, gap: " ", payload });
    return serializeTokens(t);
  }
  function existingFieldTimes(t, field) {
    const idxs = fieldIndices(t.segments, field);
    if (idxs.length === 0) return { time: null, timeEnd: null };
    const tok = t.segments[idxs[idxs.length - 1]];
    const { time, timeEnd } = splitDateTimePayload(tok.payload);
    return { time, timeEnd };
  }
  function setField(rawLine, field, value, time, timeEnd) {
    if (value !== null && parseDatePayload(value).kind !== "date") {
      throw new Error(`serializeTaskLine: \u043D\u0435 ISO-\u0434\u0430\u0442\u0430: ${JSON.stringify(value)}`);
    }
    if (time !== void 0) {
      if (!isTimedDateField(field)) {
        throw new Error(`serializeTaskLine: \u043F\u043E\u043B\u0435 ${field} \u043D\u0435 \u0438\u043C\u0435\u0435\u0442 \u0432\u0440\u0435\u043C\u0435\u043D\u0438`);
      }
      if (time !== null && !TIME_RE.test(time)) {
        throw new Error(`serializeTaskLine: \u043D\u0435 \u0432\u0440\u0435\u043C\u044F HH:mm: ${JSON.stringify(time)}`);
      }
      if (time !== null && value === null) {
        throw new Error(`serializeTaskLine: \u0432\u0440\u0435\u043C\u044F \u0431\u0435\u0437 \u0434\u0430\u0442\u044B: ${JSON.stringify(time)}`);
      }
    }
    if (timeEnd !== void 0) {
      if (!isTimedDateField(field)) {
        throw new Error(`serializeTaskLine: \u043F\u043E\u043B\u0435 ${field} \u043D\u0435 \u0438\u043C\u0435\u0435\u0442 \u0432\u0440\u0435\u043C\u0435\u043D\u0438`);
      }
      if (timeEnd !== null && !TIME_RE.test(timeEnd)) {
        throw new Error(`serializeTaskLine: \u043D\u0435 \u0432\u0440\u0435\u043C\u044F HH:mm: ${JSON.stringify(timeEnd)}`);
      }
      if (timeEnd !== null && value === null) {
        throw new Error(`serializeTaskLine: \u0432\u0440\u0435\u043C\u044F \u0431\u0435\u0437 \u0434\u0430\u0442\u044B: ${JSON.stringify(timeEnd)}`);
      }
    }
    if (value === null) {
      return setPayloadField(rawLine, field, DATE_FIELD_EMOJI[field], null);
    }
    let effTime = time ?? null;
    let effTimeEnd = timeEnd ?? null;
    if (isTimedDateField(field) && (time === void 0 || timeEnd === void 0)) {
      const existing = existingFieldTimes(mustTokenize(rawLine), field);
      if (time === void 0) effTime = existing.time;
      if (timeEnd === void 0) effTimeEnd = time === null ? null : existing.timeEnd;
    }
    if (effTimeEnd !== null) {
      if (effTime === null) {
        throw new Error(
          `serializeTaskLine: \u043A\u043E\u043D\u0435\u0446 \u0438\u043D\u0442\u0435\u0440\u0432\u0430\u043B\u0430 \u0431\u0435\u0437 \u0432\u0440\u0435\u043C\u0435\u043D\u0438 \u043D\u0430\u0447\u0430\u043B\u0430: ${JSON.stringify(effTimeEnd)}`
        );
      }
      if (effTimeEnd <= effTime) {
        throw new Error(
          `serializeTaskLine: \u043A\u043E\u043D\u0435\u0446 \u0438\u043D\u0442\u0435\u0440\u0432\u0430\u043B\u0430 \u043D\u0435 \u043F\u043E\u0437\u0436\u0435 \u043D\u0430\u0447\u0430\u043B\u0430: ${JSON.stringify(`${effTime}-${effTimeEnd}`)}`
        );
      }
    }
    const payload = effTime === null ? value : effTimeEnd === null ? `${value} ${effTime}` : `${value} ${effTime}-${effTimeEnd}`;
    return setPayloadField(rawLine, field, DATE_FIELD_EMOJI[field], payload);
  }
  function setValueField(rawLine, field, value) {
    let payload = value;
    if (value !== null) {
      if (field === "location") payload = assertLocation(value);
      else assertToken(value, field);
    }
    return setPayloadField(rawLine, field, VALUE_FIELD_EMOJI[field], payload);
  }
  function setDescription(rawLine, text) {
    const canon = text.replace(/\s+/g, " ").trim();
    const loc = VALUE_FIELD_EMOJI.location;
    for (const e of ALL_FIELD_EMOJI) {
      if (e === loc) {
        for (let idx = canon.indexOf(loc); idx !== -1; idx = canon.indexOf(loc, idx + loc.length)) {
          if (!isLeadingLocation(canon, idx)) {
            throw new Error(
              `serializeTaskLine: \u044D\u043C\u043E\u0434\u0437\u0438 \u043F\u043E\u043B\u044F \u0432 \u0442\u0435\u043A\u0441\u0442\u0435 \u043E\u043F\u0438\u0441\u0430\u043D\u0438\u044F: ${JSON.stringify(text)}`
            );
          }
        }
        continue;
      }
      if (canon.includes(e)) {
        throw new Error(
          `serializeTaskLine: \u044D\u043C\u043E\u0434\u0437\u0438 \u043F\u043E\u043B\u044F \u0432 \u0442\u0435\u043A\u0441\u0442\u0435 \u043E\u043F\u0438\u0441\u0430\u043D\u0438\u044F: ${JSON.stringify(text)}`
        );
      }
    }
    const t = mustTokenize(rawLine);
    const fieldToks = t.segments.filter((s) => s.kind === "field");
    const segs = [];
    if (canon !== "") segs.push({ kind: "text", text: ` ${canon}` });
    for (const f of fieldToks) segs.push({ kind: "text", text: " " }, f);
    t.segments = segs;
    coalesceText(t.segments);
    return serializeTokens(t);
  }

  // src/core/model/gtdState.ts
  function isDone(t) {
    return t.statusChar === "x" || t.statusChar === "X";
  }
  function isCancelled(t) {
    return t.statusChar === "-";
  }
  function isDeferred(t, today) {
    return t.start !== null && t.start > today;
  }
  function isTemplate(t) {
    return t.container === "recurring";
  }
  function isDetail(t) {
    return t.container === "card";
  }
  function isEvent(t) {
    return t.container === "events";
  }
  function isArchived(t) {
    return t.container === "archive";
  }
  function isActive(t, today) {
    return !isDone(t) && !isCancelled(t) && !isDeferred(t, today) && !isTemplate(t) && !isDetail(t) && !isEvent(t) && !isArchived(t);
  }
  var WAITING_TAG = "#waiting";
  function hasWaitingTag(t) {
    return t.tags.includes(WAITING_TAG);
  }
  function eligible(t, today) {
    return isActive(t, today) && !hasWaitingTag(t);
  }
  function depSatisfied(id, resolveDep) {
    const carriers = resolveDep(id);
    if (carriers.length === 0) return false;
    return carriers.every((c) => isDone(c) || isCancelled(c));
  }
  function depsMet(t, resolveDep) {
    return t.dependsOn.every((d) => depSatisfied(d, resolveDep));
  }
  function ready(t, today, resolveDep) {
    return eligible(t, today) && depsMet(t, resolveDep);
  }

  // src/core/model/projections.ts
  function taskToCalendarEvent(task, placement) {
    for (const field of placement) {
      const date = task[field];
      if (date !== null) return { date, field };
    }
    return null;
  }

  // src/core/query/QueryEngine.ts
  function nsPredicate(ns) {
    if (ns === void 0 || ns.defs.length === 0) return () => true;
    return (t) => inNamespace(t.filePath, t.nsOverride ?? null, ns);
  }
  function isInInbox(t, ctx) {
    if (!isActive(t, ctx.today)) return false;
    const bits = ctx.settingsBits;
    if (bits.hasBoardTag(t) || bits.hasDue(t)) return false;
    if (t.container === "project") return t.projectActive && ready(t, ctx.today, ctx.resolveDep);
    if (t.container === "plain") return bits.includePlain;
    return true;
  }
  function isInTickler(t, today) {
    if (isTemplate(t) || isDetail(t) || isEvent(t) || isArchived(t)) return false;
    return !isDone(t) && !isCancelled(t) && t.start !== null && t.start > today;
  }
  function evaluate(spec, ctx) {
    const inNs = nsPredicate(ctx.namespace);
    switch (spec.kind) {
      case "inbox": {
        const out = collect(ctx.tasks, (t) => inNs(t) && isInInbox(t, ctx));
        out.sort(cmpInbox);
        return out;
      }
      case "tickler": {
        const out = collect(ctx.tasks, (t) => inNs(t) && isInTickler(t, ctx.today));
        out.sort(cmpTickler);
        return out;
      }
      case "active": {
        const out = collect(ctx.tasks, (t) => isActive(t, ctx.today));
        out.sort(cmpLocation);
        return out;
      }
      case "all-templates": {
        const out = collect(ctx.tasks, (t) => inNs(t) && isTemplate(t));
        out.sort(cmpLocation);
        return out;
      }
      case "project-members": {
        const out = collect(ctx.tasks, (t) => t.filePath === spec.path);
        out.sort(cmpLocation);
        return out;
      }
      case "calendar-range": {
        const placed = [];
        for (const t of ctx.tasks) {
          if (!inNs(t)) continue;
          if (isTemplate(t) || isDetail(t) || isEvent(t) || isArchived(t)) continue;
          const ev = taskToCalendarEvent(t, spec.placement);
          if (ev === null) continue;
          if (ev.date < spec.fromIso || ev.date > spec.toIso) continue;
          placed.push({ t, date: ev.date });
        }
        placed.sort(
          (a, b) => a.date !== b.date ? a.date < b.date ? -1 : 1 : cmpLocation(a.t, b.t)
        );
        return placed.map((p) => p.t);
      }
    }
  }
  var PRIORITY_RANK = {
    highest: 0,
    high: 1,
    medium: 2,
    low: 3,
    lowest: 4,
    none: 5
  };
  function cmpInbox(a, b) {
    const pr = PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority];
    if (pr !== 0) return pr;
    const cr = cmpNullableIsoAsc(a.created, b.created);
    if (cr !== 0) return cr;
    return cmpLocation(a, b);
  }
  function cmpTickler(a, b) {
    const st = cmpNullableIsoAsc(a.start, b.start);
    if (st !== 0) return st;
    return cmpLocation(a, b);
  }
  function cmpNullableIsoAsc(a, b) {
    if (a === b) return 0;
    if (a === null) return 1;
    if (b === null) return -1;
    return a < b ? -1 : 1;
  }
  function cmpLocation(a, b) {
    if (a.filePath !== b.filePath) return a.filePath < b.filePath ? -1 : 1;
    return a.lineStart - b.lineStart;
  }
  function collect(tasks, pred) {
    const out = [];
    for (const t of tasks) if (pred(t)) out.push(t);
    return out;
  }

  // src/core/query/querySpec.ts
  function defaultHasBoardTag(t) {
    return t.container === "board" || t.tags.some((tag) => tag.startsWith("#kanban/"));
  }
  function defaultHasDue(t) {
    return t.due !== null;
  }
  function defaultInboxConfig(includePlain = false) {
    return { hasBoardTag: defaultHasBoardTag, hasDue: defaultHasDue, includePlain };
  }

  // src/core/recurrence/dateMath.ts
  function isLeap(y) {
    return y % 4 === 0 && y % 100 !== 0 || y % 400 === 0;
  }
  var MONTH_LENGTHS = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  function daysInMonth2(y, m) {
    const base = MONTH_LENGTHS[m - 1];
    if (base === void 0) throw new RangeError(`month out of range: ${m}`);
    return m === 2 && isLeap(y) ? 29 : base;
  }
  function clampDay(y, m, d) {
    const max = daysInMonth2(y, m);
    return d > max ? max : d;
  }
  function toParts(date) {
    return {
      y: parseInt(date.slice(0, 4), 10),
      m: parseInt(date.slice(5, 7), 10),
      d: parseInt(date.slice(8, 10), 10)
    };
  }
  function fromParts(p) {
    const yy = String(p.y).padStart(4, "0");
    const mm = String(p.m).padStart(2, "0");
    const dd = String(p.d).padStart(2, "0");
    return `${yy}-${mm}-${dd}`;
  }
  function compare(a, b) {
    return a < b ? -1 : a > b ? 1 : 0;
  }
  function isValidIsoDate(s) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
    const { y, m, d } = toParts(s);
    return m >= 1 && m <= 12 && d >= 1 && d <= daysInMonth2(y, m);
  }
  function toEpochDays(date) {
    const { y, m, d } = toParts(date);
    const yy = m <= 2 ? y - 1 : y;
    const era = Math.floor(yy / 400);
    const yoe = yy - era * 400;
    const doy = Math.floor((153 * (m + (m > 2 ? -3 : 9)) + 2) / 5) + d - 1;
    const doe = yoe * 365 + Math.floor(yoe / 4) - Math.floor(yoe / 100) + doy;
    return era * 146097 + doe - 719468;
  }
  function fromEpochDays(z) {
    const zz = z + 719468;
    const era = Math.floor(zz / 146097);
    const doe = zz - era * 146097;
    const yoe = Math.floor(
      (doe - Math.floor(doe / 1460) + Math.floor(doe / 36524) - Math.floor(doe / 146096)) / 365
    );
    const y = yoe + era * 400;
    const doy = doe - (365 * yoe + Math.floor(yoe / 4) - Math.floor(yoe / 100));
    const mp = Math.floor((5 * doy + 2) / 153);
    const d = doy - Math.floor((153 * mp + 2) / 5) + 1;
    const m = mp < 10 ? mp + 3 : mp - 9;
    return fromParts({ y: m <= 2 ? y + 1 : y, m, d });
  }
  function addDays(date, days) {
    return fromEpochDays(toEpochDays(date) + days);
  }
  function dayOfWeek(date) {
    const z = toEpochDays(date);
    return ((z % 7 + 7) % 7 + 3) % 7;
  }
  function weeksBetween(a, b) {
    const mondayA = toEpochDays(a) - dayOfWeek(a);
    const mondayB = toEpochDays(b) - dayOfWeek(b);
    return (mondayA - mondayB) / 7;
  }

  // src/core/recurrence/grammar.ts
  function isParseError(r) {
    return "error" in r;
  }
  var WEEKDAY_NAMES = /* @__PURE__ */ new Map([
    ["monday", 0],
    ["mon", 0],
    ["tuesday", 1],
    ["tue", 1],
    ["wednesday", 2],
    ["wed", 2],
    ["thursday", 3],
    ["thu", 3],
    ["friday", 4],
    ["fri", 4],
    ["saturday", 5],
    ["sat", 5],
    ["sunday", 6],
    ["sun", 6]
  ]);
  var MONTH_NAMES = /* @__PURE__ */ new Map([
    ["january", 1],
    ["jan", 1],
    ["february", 2],
    ["feb", 2],
    ["march", 3],
    ["mar", 3],
    ["april", 4],
    ["apr", 4],
    ["may", 5],
    ["june", 6],
    ["jun", 6],
    ["july", 7],
    ["jul", 7],
    ["august", 8],
    ["aug", 8],
    ["september", 9],
    ["sep", 9],
    ["october", 10],
    ["oct", 10],
    ["november", 11],
    ["nov", 11],
    ["december", 12],
    ["dec", 12]
  ]);
  var YEARLY_MAX_DAY = [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  function lookupWeekday(tok) {
    const direct = WEEKDAY_NAMES.get(tok);
    if (direct !== void 0) return direct;
    if (tok.endsWith("s")) {
      const stripped = WEEKDAY_NAMES.get(tok.slice(0, -1));
      if (stripped !== void 0) return stripped;
    }
    return null;
  }
  var ORDINAL_RE = /^(\d+)(st|nd|rd|th)?$/;
  var TIME_RE2 = /^([01]\d|2[0-3]):[0-5]\d$/;
  function parseRule(text) {
    const tokens = text.trim().toLowerCase().split(/[\s,]+/).filter((t) => t.length > 0);
    let i = 0;
    let fromCompletion = false;
    if (tokens[i] === "every!") {
      fromCompletion = true;
      i++;
    } else if (tokens[i] === "every") {
      i++;
    } else {
      return { error: "rule must start with 'every' (or 'every!' for from-completion)" };
    }
    let n = 1;
    let hasN = false;
    const nTok = tokens[i];
    if (nTok !== void 0 && /^\d+$/.test(nTok)) {
      n = parseInt(nTok, 10);
      hasN = true;
      if (n < 1) return { error: "interval must be at least 1" };
      i++;
    }
    const unitTok = tokens[i];
    if (unitTok === void 0) return { error: "expected a unit after 'every'" };
    i++;
    let kind;
    let unitWeekday = -1;
    if (unitTok === "day" || unitTok === "days") kind = "daily";
    else if (unitTok === "week" || unitTok === "weeks") kind = "weekly";
    else if (unitTok === "month" || unitTok === "months") kind = "monthly";
    else if (unitTok === "year" || unitTok === "years") kind = "yearly";
    else if (unitTok === "weekday" || unitTok === "weekdays") kind = "weekdays";
    else {
      const wd = lookupWeekday(unitTok);
      if (wd === null) return { error: `unknown unit '${unitTok}'` };
      kind = "weekday-name";
      unitWeekday = wd;
    }
    let from;
    let until;
    let onWeekdays = null;
    let onMonthDay = null;
    let onDate = null;
    let eventTime;
    let eventTimeEnd;
    while (i < tokens.length) {
      const t = tokens[i];
      if (t === "at") {
        i++;
        const spec = tokens[i];
        if (spec === void 0) return { error: "expected a time after 'at'" };
        if (eventTime !== void 0) return { error: "duplicate 'at'" };
        const dash = spec.indexOf("-");
        if (dash === -1) {
          if (!TIME_RE2.test(spec)) return { error: `invalid time '${spec}' after 'at'` };
          eventTime = spec;
        } else {
          const startPart = spec.slice(0, dash);
          const endPart = spec.slice(dash + 1);
          if (!TIME_RE2.test(startPart) || !TIME_RE2.test(endPart)) {
            return { error: `invalid time range '${spec}' after 'at'` };
          }
          if (endPart <= startPart) return { error: "'at' end time must be after start time" };
          eventTime = startPart;
          eventTimeEnd = endPart;
        }
        i++;
        continue;
      }
      if (t === "from") {
        i++;
        const dt = tokens[i];
        if (dt === void 0 || !isValidIsoDate(dt)) {
          return { error: "expected a valid YYYY-MM-DD date after 'from'" };
        }
        if (from !== void 0) return { error: "duplicate 'from'" };
        from = dt;
        i++;
        continue;
      }
      if (t === "until") {
        i++;
        const dt = tokens[i];
        if (dt === void 0 || !isValidIsoDate(dt)) {
          return { error: "expected a valid YYYY-MM-DD date after 'until'" };
        }
        if (until !== void 0) return { error: "duplicate 'until'" };
        until = dt;
        i++;
        continue;
      }
      if (t !== "on") return { error: `unexpected token '${t}'` };
      i++;
      const t2 = tokens[i];
      if (t2 === void 0) return { error: "expected a target after 'on'" };
      if (t2 === "the") {
        i++;
        const t3 = tokens[i];
        if (t3 === "last") {
          i++;
          if (tokens[i] !== "day") return { error: "expected 'day' after 'on the last'" };
          i++;
          if (onMonthDay !== null) return { error: "duplicate day-of-month clause" };
          onMonthDay = "last";
        } else {
          const m = t3 !== void 0 ? ORDINAL_RE.exec(t3) : null;
          if (!m || m[1] === void 0) {
            return { error: "expected a day number or 'last day' after 'on the'" };
          }
          const dnum = parseInt(m[1], 10);
          if (dnum < 1 || dnum > 31) return { error: `day of month out of range: ${dnum}` };
          if (onMonthDay !== null) return { error: "duplicate day-of-month clause" };
          onMonthDay = dnum;
          i++;
        }
        continue;
      }
      const monthNum = MONTH_NAMES.get(t2);
      if (monthNum !== void 0) {
        i++;
        const dTok = tokens[i];
        const m = dTok !== void 0 ? ORDINAL_RE.exec(dTok) : null;
        if (!m || m[1] === void 0) {
          return { error: `expected a day number after month name '${t2}'` };
        }
        const dnum = parseInt(m[1], 10);
        const maxDay = YEARLY_MAX_DAY[monthNum - 1];
        if (maxDay === void 0 || dnum < 1 || dnum > maxDay) {
          return { error: `'${t2} ${dnum}' is not a valid date` };
        }
        if (onDate !== null) return { error: "duplicate month-day clause" };
        onDate = { month: monthNum, day: dnum };
        i++;
        continue;
      }
      const list = [];
      while (i < tokens.length) {
        const wTok = tokens[i];
        const wd = wTok !== void 0 ? lookupWeekday(wTok) : null;
        if (wd === null) break;
        list.push(wd);
        i++;
      }
      if (list.length === 0) return { error: `cannot parse 'on ${t2}'` };
      if (onWeekdays !== null) return { error: "duplicate weekday clause" };
      onWeekdays = [...new Set(list)].sort((a, b) => a - b);
    }
    if (from !== void 0 && until !== void 0 && compare(from, until) > 0) {
      return { error: "'from' must not be after 'until'" };
    }
    const withTail = (r) => {
      const out = { ...r };
      if (from !== void 0) out.from = from;
      if (until !== void 0) out.until = until;
      if (eventTime !== void 0) out.eventTime = eventTime;
      if (eventTimeEnd !== void 0) out.eventTimeEnd = eventTimeEnd;
      if (fromCompletion) out.fromCompletion = true;
      return out;
    };
    switch (kind) {
      case "daily":
        if (onWeekdays !== null || onMonthDay !== null || onDate !== null) {
          return { error: "daily rules do not take an 'on' clause" };
        }
        return withTail({ freq: "daily", n });
      case "weekdays":
        if (hasN) return { error: "'every weekday' does not take an interval" };
        if (onWeekdays !== null || onMonthDay !== null || onDate !== null) {
          return { error: "'every weekday' does not take an 'on' clause" };
        }
        return withTail({ freq: "weekdays" });
      case "weekday-name":
        if (fromCompletion) {
          return {
            error: `'every! ${unitTok}' is ambiguous \u2014 from-completion has no fixed weekday; use 'every! week' or 'every! N weeks'`
          };
        }
        if (onWeekdays !== null || onMonthDay !== null || onDate !== null) {
          return { error: `'every ${unitTok}' does not take an 'on' clause` };
        }
        return withTail({ freq: "weekly", n, byDay: [unitWeekday] });
      case "weekly":
        if (onMonthDay !== null || onDate !== null) {
          return { error: "weekly rules take only 'on <weekday, ...>'" };
        }
        if (fromCompletion && onWeekdays !== null) {
          return {
            error: "'every! week on <weekday>' is contradictory \u2014 from-completion has no fixed weekday; drop the 'on' clause"
          };
        }
        return withTail({ freq: "weekly", n, byDay: onWeekdays ?? [] });
      case "monthly":
        if (onWeekdays !== null || onDate !== null) {
          return { error: "monthly rules take only 'on the <day>' / 'on the last day'" };
        }
        if (fromCompletion) {
          if (onMonthDay !== null) {
            return {
              error: "'every! month on the <day>' is contradictory \u2014 from-completion counts the day from the completion date; drop the 'on' clause"
            };
          }
          return withTail({ freq: "monthly", n });
        }
        if (onMonthDay === null) {
          return { error: "monthly rule requires 'on the <day>' or 'on the last day'" };
        }
        return withTail({ freq: "monthly", n, day: onMonthDay });
      case "yearly":
        if (onWeekdays !== null || onMonthDay !== null) {
          return { error: "yearly rules take only 'on <month-name> <day>'" };
        }
        if (fromCompletion) {
          if (onDate !== null) {
            return {
              error: "'every! year on <month> <day>' is contradictory \u2014 from-completion counts the date from the completion date; drop the 'on' clause"
            };
          }
          return withTail({ freq: "yearly", n });
        }
        if (onDate === null) return { error: "yearly rule requires 'on <month-name> <day>'" };
        return withTail({ freq: "yearly", n, month: onDate.month, day: onDate.day });
    }
  }

  // src/core/recurrence/nextOccurrence.ts
  var MAX_ITERATIONS = 1e3;
  function snapWeekAnchor(anchor, days) {
    const first = days[0];
    if (first === void 0) return anchor;
    const dow = dayOfWeek(anchor);
    const weekStart = addDays(anchor, -dow);
    for (const wd of days) {
      if (wd >= dow) return addDays(weekStart, wd);
    }
    return addDays(weekStart, 7 + first);
  }
  function capUntil(cand, until) {
    if (until !== void 0 && compare(cand, until) > 0) return null;
    return cand;
  }
  function nextInPhase(anchorDate, step, after) {
    const anc = toEpochDays(anchorDate);
    const delta = toEpochDays(after) - anc;
    return fromEpochDays(anc + (Math.floor(delta / step) + 1) * step);
  }
  function nextOccurrence(rule, after, anchor) {
    if (rule.fromCompletion) return null;
    if (rule.from !== void 0 && compare(after, addDays(rule.from, -1)) < 0) {
      after = addDays(rule.from, -1);
    }
    switch (rule.freq) {
      case "daily": {
        const dailyAnchor = rule.from ?? anchor;
        if (dailyAnchor !== void 0 && rule.n > 1) {
          return capUntil(nextInPhase(dailyAnchor, rule.n, after), rule.until);
        }
        return capUntil(addDays(after, rule.n), rule.until);
      }
      case "weekdays": {
        let d = addDays(after, 1);
        for (let iter = 0; iter < MAX_ITERATIONS; iter++) {
          if (dayOfWeek(d) <= 4) return capUntil(d, rule.until);
          d = addDays(d, 1);
        }
        return null;
      }
      case "weekly": {
        if (rule.byDay.length === 0) {
          const weeklyAnchor = rule.from ?? anchor;
          if (weeklyAnchor !== void 0 && rule.n > 1) {
            return capUntil(nextInPhase(weeklyAnchor, 7 * rule.n, after), rule.until);
          }
          return capUntil(addDays(after, 7 * rule.n), rule.until);
        }
        const days = [...rule.byDay].sort((a, b) => a - b);
        const dow = dayOfWeek(after);
        const weekStart = addDays(after, -dow);
        const first = days[0];
        if (first === void 0) return null;
        if (anchor !== void 0 && rule.n > 1) {
          const eff = snapWeekAnchor(anchor, days);
          const off = (weeksBetween(after, eff) % rule.n + rule.n) % rule.n;
          if (off === 0) {
            for (const wd of days) {
              if (wd > dow) return capUntil(addDays(weekStart, wd), rule.until);
            }
            return capUntil(addDays(weekStart, 7 * rule.n + first), rule.until);
          }
          return capUntil(addDays(weekStart, 7 * (rule.n - off) + first), rule.until);
        }
        for (const wd of days) {
          if (wd > dow) return capUntil(addDays(weekStart, wd), rule.until);
        }
        const stride = days.includes(dow) ? 7 * rule.n : 7;
        return capUntil(addDays(weekStart, stride + first), rule.until);
      }
      case "monthly": {
        if (rule.day === void 0) return null;
        const ruleDay = rule.day;
        const p = toParts(after);
        let y = p.y;
        let m = p.m;
        for (let iter = 0; iter < MAX_ITERATIONS; iter++) {
          const dom = ruleDay === "last" ? daysInMonth2(y, m) : clampDay(y, m, ruleDay);
          const cand = fromParts({ y, m, d: dom });
          if (compare(cand, after) > 0) return capUntil(cand, rule.until);
          m += rule.n;
          y += Math.floor((m - 1) / 12);
          m = (m - 1) % 12 + 1;
        }
        return null;
      }
      case "yearly": {
        if (rule.month === void 0 || rule.day === void 0) return null;
        const { month, day } = rule;
        let y = toParts(after).y;
        for (let iter = 0; iter < MAX_ITERATIONS; iter++) {
          const cand = fromParts({ y, m: month, d: clampDay(y, month, day) });
          if (compare(cand, after) > 0) return capUntil(cand, rule.until);
          y += rule.n;
        }
        return null;
      }
    }
  }

  // src/core/recurrence/occurrences.ts
  var DEFAULT_OCCURRENCE_CAP = 500;
  var WEEK_PARITY_EPOCH = "1970-01-05";
  function effectiveAnchor(rule, anchor) {
    let anc;
    if (rule.from !== void 0) anc = rule.from;
    else if (anchor !== void 0) anc = anchor;
    else if ((rule.freq === "weekly" || rule.freq === "daily") && rule.n > 1) {
      anc = WEEK_PARITY_EPOCH;
    } else return void 0;
    if (rule.freq === "weekly" && rule.byDay.length > 0) return snapWeekAnchor(anc, rule.byDay);
    return anc;
  }
  function expandOccurrences(rule, fromIso, toIso, cap = DEFAULT_OCCURRENCE_CAP, exclude, anchor) {
    const out = [];
    if (compare(fromIso, toIso) > 0 || cap <= 0) return out;
    if (rule.fromCompletion) return out;
    const anc = effectiveAnchor(rule, anchor);
    let cur = nextOccurrence(rule, addDays(fromIso, -1), anc);
    while (cur !== null && compare(cur, toIso) <= 0 && out.length < cap) {
      if (exclude === void 0 || !exclude.has(cur)) out.push(cur);
      cur = nextOccurrence(rule, cur, anc);
    }
    return out;
  }

  // src/views/common/dates.ts
  var DAY_MS = 864e5;
  function addDaysIso(date, days) {
    return new Date(Date.parse(date + "T00:00:00Z") + days * DAY_MS).toISOString().slice(0, 10);
  }

  // src/views/calendar/timeGrid.ts
  var MINUTES_PER_DAY = 24 * 60;
  var DEFAULT_SCROLL_MIN = 8 * 60;
  var TIME_RE3 = /^([01]\d|2[0-3]):([0-5]\d)$/;
  function timeToMinutes(time) {
    const m = TIME_RE3.exec(time);
    if (m === null) return null;
    return Number(m[1]) * 60 + Number(m[2]);
  }
  function minutesToTime(min) {
    const clamped = Math.min(Math.max(Math.trunc(min), 0), MINUTES_PER_DAY - 1);
    const h = Math.trunc(clamped / 60);
    const m = clamped % 60;
    return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
  }

  // src/views/calendar/calendarLogic.ts
  var PRIORITY_RANK2 = {
    highest: 0,
    high: 1,
    medium: 2,
    low: 3,
    lowest: 4,
    none: 5
  };
  function placedTime(task, field) {
    switch (field) {
      case "due":
        return task.dueTime;
      case "scheduled":
        return task.scheduledTime;
      case "start":
        return task.startTime;
    }
  }
  function placedTimeEnd(task, field) {
    switch (field) {
      case "due":
        return task.dueTimeEnd;
      case "scheduled":
        return task.scheduledTimeEnd;
      case "start":
        return task.startTimeEnd;
    }
  }
  function placeEvents(tasks, placement) {
    const out = /* @__PURE__ */ new Map();
    for (const task of tasks) {
      const ev = taskToCalendarEvent(task, placement);
      if (ev === null) continue;
      let list = out.get(ev.date);
      if (list === void 0) {
        list = [];
        out.set(ev.date, list);
      }
      list.push({ task, field: ev.field });
    }
    for (const list of out.values()) {
      list.sort((a, b) => {
        const ta = placedTime(a.task, a.field);
        const tb = placedTime(b.task, b.field);
        if (ta !== null || tb !== null) {
          if (ta === null) return 1;
          if (tb === null) return -1;
          if (ta !== tb) return ta < tb ? -1 : 1;
        }
        const pr = PRIORITY_RANK2[a.task.priority] - PRIORITY_RANK2[b.task.priority];
        if (pr !== 0) return pr;
        const da = a.task.description;
        const db = b.task.description;
        return da < db ? -1 : da > db ? 1 : 0;
      });
    }
    return out;
  }
  function expandEventOccurrences(events, from, to) {
    const out = /* @__PURE__ */ new Map();
    const push = (occ) => {
      let list = out.get(occ.date);
      if (list === void 0) {
        list = [];
        out.set(occ.date, list);
      }
      list.push(occ);
    };
    for (const task of events) {
      if (task.recurrence !== null) {
        const rule = parseRule(task.recurrence);
        if (isParseError(rule)) continue;
        const time = rule.eventTime ?? null;
        const timeEnd = rule.eventTimeEnd ?? null;
        const exclude = task.excludedDates.length > 0 ? new Set(task.excludedDates) : void 0;
        for (const date of expandOccurrences(rule, from, to, void 0, exclude)) {
          push({
            task,
            kind: "series",
            date,
            title: task.description,
            time,
            timeEnd,
            location: task.location
          });
        }
      } else if (task.due !== null && task.due >= from && task.due <= to) {
        push({
          task,
          kind: "single",
          date: task.due,
          title: task.description,
          time: task.dueTime,
          timeEnd: task.dueTimeEnd,
          location: task.location
        });
      }
    }
    for (const list of out.values()) {
      list.sort((a, b) => {
        if (a.time !== null || b.time !== null) {
          if (a.time === null) return 1;
          if (b.time === null) return -1;
          if (a.time !== b.time) return a.time < b.time ? -1 : 1;
        }
        return a.title < b.title ? -1 : a.title > b.title ? 1 : 0;
      });
    }
    return out;
  }

  // src/views/common/taskActions.ts
  function quickCaptureLine(text) {
    const collapsed = text.replace(/\s+/g, " ").trim();
    if (collapsed === "") return null;
    const body = collapsed.replace(/^[-*+]\s+\[.\]\s*/, "").trim();
    if (body === "") return null;
    return `- [ ] ${body}`;
  }

  // src/core/index/TaskIndex.ts
  function addToSetMap(map, mk, v) {
    const set = map.get(mk);
    if (set) set.add(v);
    else map.set(mk, /* @__PURE__ */ new Set([v]));
  }
  function deleteFromSetMap(map, mk, v) {
    const set = map.get(mk);
    if (!set) return;
    set.delete(v);
    if (set.size === 0) map.delete(mk);
  }
  var TaskIndex = class {
    constructor() {
      this.primary = /* @__PURE__ */ new Map();
      this.byFile = /* @__PURE__ */ new Map();
      this.byId = /* @__PURE__ */ new Map();
      /** Объединённый бакет по due/scheduled/start (календарю нужен union). */
      this.byDate = /* @__PURE__ */ new Map();
      this.byTag = /* @__PURE__ */ new Map();
      this.epochCounter = 0;
    }
    /** Инкрементируется при КАЖДОЙ мутации — ключ мемоизации запросов. */
    get epoch() {
      return this.epochCounter;
    }
    /** Полная замена задач файла (единица инкрементального обновления). */
    replaceFile(path, tasks) {
      this.clearFile(path);
      for (const t of tasks) this.insert(path, t);
      this.epochCounter++;
    }
    removeFile(path) {
      this.clearFile(path);
      this.epochCounter++;
    }
    /**
     * Переименование файла: content-ключи включают путь и перезаписываются
     * на новый путь; "id:<🆔>" от пути не зависит и стабилен.
     */
    renameFile(oldPath, newPath) {
      const keys = this.byFile.get(oldPath);
      const moved = [];
      if (keys) {
        for (const sk of keys) {
          const t = this.primary.get(sk);
          if (t) moved.push(t);
        }
      }
      this.clearFile(oldPath);
      for (const t of moved) {
        const relocated = { ...t, filePath: newPath };
        const key = t.taskId === null && t.key.startsWith(oldPath + "#") ? newPath + t.key.slice(oldPath.length) : computeKey(relocated);
        this.insert(newPath, { ...relocated, key });
      }
      this.epochCounter++;
    }
    get(key) {
      return this.primary.get(key);
    }
    /** ВСЕ носители id — для depsMet (fail-closed при дублях) и дедупа. */
    resolveDep(id) {
      const keys = this.byId.get(id);
      if (!keys) return [];
      const out = [];
      for (const sk of keys) {
        const t = this.primary.get(sk);
        if (t) out.push(t);
      }
      return out;
    }
    all() {
      return this.primary.values();
    }
    /** 🆔 с более чем одним носителем → их ключи (для lint-бейджей и дедупа §8). */
    duplicateIds() {
      const dup = /* @__PURE__ */ new Map();
      for (const [id, keys] of this.byId) {
        if (keys.length > 1) dup.set(id, [...keys]);
      }
      return dup;
    }
    // --- дополнительные аксессоры (нужны сервисам и видам) ---
    fileTasks(path) {
      return this.collect(this.byFile.get(path));
    }
    dateTasks(date) {
      return this.collect(this.byDate.get(date));
    }
    tagTasks(tag) {
      return this.collect(this.byTag.get(tag));
    }
    // --- внутренности ---
    collect(keys) {
      if (!keys) return [];
      const out = [];
      for (const sk of keys) {
        const t = this.primary.get(sk);
        if (t) out.push(t);
      }
      return out;
    }
    insert(path, task) {
      let sk = task.key;
      let n = 1;
      while (this.primary.has(sk)) sk = task.key + "" + n++;
      const stored = sk === task.key ? task : { ...task, key: sk };
      this.primary.set(sk, stored);
      addToSetMap(this.byFile, path, sk);
      if (stored.taskId !== null) {
        const list = this.byId.get(stored.taskId);
        if (list) list.push(sk);
        else this.byId.set(stored.taskId, [sk]);
      }
      for (const d of [stored.due, stored.scheduled, stored.start]) {
        if (d !== null) addToSetMap(this.byDate, d, sk);
      }
      for (const tag of stored.tags) addToSetMap(this.byTag, tag, sk);
    }
    clearFile(path) {
      const keys = this.byFile.get(path);
      if (!keys) return;
      this.byFile.delete(path);
      for (const sk of keys) this.removeStorageKey(sk);
    }
    removeStorageKey(sk) {
      const task = this.primary.get(sk);
      if (!task) return;
      this.primary.delete(sk);
      if (task.taskId !== null) {
        const list = this.byId.get(task.taskId);
        if (list) {
          const filtered = list.filter((k) => k !== sk);
          if (filtered.length > 0) this.byId.set(task.taskId, filtered);
          else this.byId.delete(task.taskId);
        }
      }
      for (const d of [task.due, task.scheduled, task.start]) {
        if (d !== null) deleteFromSetMap(this.byDate, d, sk);
      }
      for (const tag of task.tags) deleteFromSetMap(this.byTag, tag, sk);
    }
  };

  // src/services/IndexerService.ts
  var DEFAULT_CHUNK_SIZE = 50;
  var IndexerService = class {
    constructor(deps) {
      this.deps = deps;
      this.index = new TaskIndex();
      /** Эпохи событий вне индекса (смена дня) — суммируются с index.epoch. */
      this.rolloverEpoch = 0;
      this.listeners = /* @__PURE__ */ new Set();
      this.unsubscribers = [];
      this.timers = /* @__PURE__ */ new Map();
      this.pendingSnaps = /* @__PURE__ */ new Map();
      this.disposed = false;
      this.started = false;
      this.unsubscribers.push(
        deps.events.onChanged((snap) => this.scheduleReindex(snap)),
        deps.events.onDeleted((path) => this.handleDeleted(path)),
        deps.events.onRenamed((oldPath, snap) => this.handleRenamed(oldPath, snap)),
        deps.clock.onDayRollover(() => {
          this.rolloverEpoch++;
          this.notify();
        })
      );
    }
    // --- IndexFeed ---
    getIndex() {
      return this.index;
    }
    getEpoch() {
      return this.index.epoch + this.rolloverEpoch;
    }
    today() {
      return this.deps.clock.todayIso();
    }
    onChange(cb) {
      this.listeners.add(cb);
      return () => {
        this.listeners.delete(cb);
      };
    }
    // --- жизненный цикл ---
    /** Первичное наполнение: чанками, между чанками уступаем макротаске,
     *  чтобы не замораживать UI на большом хранилище. */
    async start() {
      if (this.started) return;
      this.started = true;
      const chunkSize = this.deps.chunkSize ?? DEFAULT_CHUNK_SIZE;
      let filled = 0;
      try {
        for await (const snap of this.deps.initialScan()) {
          if (this.disposed) return;
          this.index.replaceFile(snap.path, this.parseSnapshot(snap));
          if (++filled % chunkSize === 0) {
            this.notify();
            await yieldToMacrotask();
            if (this.disposed) return;
          }
        }
      } catch (e) {
        console.error("GTD Flow: \u043F\u0435\u0440\u0432\u0438\u0447\u043D\u044B\u0439 \u0441\u043A\u0430\u043D \u043F\u0440\u0435\u0440\u0432\u0430\u043D, \u0438\u043D\u0434\u0435\u043A\u0441 \u043C\u043E\u0436\u0435\u0442 \u0431\u044B\u0442\u044C \u043D\u0435\u043F\u043E\u043B\u043D\u044B\u043C", e);
      }
      if (this.disposed) return;
      this.notify();
      this.deps.onReady?.();
    }
    dispose() {
      this.disposed = true;
      for (const unsub of this.unsubscribers) unsub();
      this.unsubscribers.length = 0;
      for (const timer of this.timers.values()) clearTimeout(timer);
      this.timers.clear();
      this.pendingSnaps.clear();
      this.listeners.clear();
    }
    // --- обработчики событий ---
    scheduleReindex(snap) {
      if (this.disposed) return;
      this.pendingSnaps.set(snap.path, snap);
      const prev = this.timers.get(snap.path);
      if (prev !== void 0) clearTimeout(prev);
      this.timers.set(
        snap.path,
        setTimeout(() => {
          this.timers.delete(snap.path);
          const pending = this.pendingSnaps.get(snap.path);
          this.pendingSnaps.delete(snap.path);
          if (pending !== void 0) this.indexSnapshot(pending);
        }, this.deps.debounceMs)
      );
    }
    handleDeleted(path) {
      if (this.disposed) return;
      this.cancelPending(path);
      this.index.removeFile(path);
      this.notify();
    }
    handleRenamed(oldPath, snap) {
      if (this.disposed) return;
      this.cancelPending(oldPath);
      this.index.renameFile(oldPath, snap.path);
      this.indexSnapshot(snap);
    }
    cancelPending(path) {
      const timer = this.timers.get(path);
      if (timer !== void 0) clearTimeout(timer);
      this.timers.delete(path);
      this.pendingSnaps.delete(path);
    }
    // --- индексация ---
    indexSnapshot(snap) {
      this.index.replaceFile(snap.path, this.parseSnapshot(snap));
      this.notify();
    }
    parseSnapshot(snap) {
      const lines = snap.content.split("\n");
      const projectActive = snap.context.container !== "project" || (snap.context.projectStatus ?? "active") === "active";
      const parsed = [];
      for (const item of snap.listItems) {
        if (item.taskChar === null) continue;
        const rawLine = lines[item.lineStart];
        if (rawLine === void 0) continue;
        const task = parseTaskLine(rawLine, {
          filePath: snap.path,
          lineStart: item.lineStart,
          parentLine: item.parentLine,
          heading: item.heading,
          container: snap.context.container,
          projectActive,
          // перебивка пространства (frontmatter gtd-namespace) — без прокидки
          // override не доехал бы до Task и фича была бы мертва (ревью)
          nsOverride: snap.context.nsOverride ?? null
        });
        if (task === null) continue;
        parsed.push(task.lineEnd === item.lineEnd ? task : { ...task, lineEnd: item.lineEnd });
      }
      return assignOccurrenceIndexes(parsed);
    }
    notify() {
      for (const cb of [...this.listeners]) cb();
    }
  };
  function assignOccurrenceIndexes(tasks) {
    const seen = /* @__PURE__ */ new Map();
    return tasks.map((t) => {
      if (t.taskId !== null) return t;
      const base = t.key;
      const n = seen.get(base) ?? 0;
      seen.set(base, n + 1);
      return { ...t, key: n === 0 ? t.key : computeKey(t, n), occurrenceIndex: n };
    });
  }
  function yieldToMacrotask() {
    return new Promise((resolve) => setTimeout(resolve, 0));
  }

  // src/services/snapshotHelpers.ts
  function nearestHeadingAbove(headings, line) {
    let found = null;
    for (const h of headings) {
      if (h.position.start.line <= line) found = h.heading;
      else break;
    }
    return found;
  }
  function snapshotListItems(items, headings) {
    return items.map((it) => ({
      lineStart: it.position.start.line,
      lineEnd: it.position.end.line,
      taskChar: it.task ?? null,
      parentLine: it.parent >= 0 ? it.parent : null,
      heading: nearestHeadingAbove(headings, it.position.start.line)
    }));
  }
  var PROJECT_STATUSES = /* @__PURE__ */ new Set(["active", "on-hold", "done", "archived"]);
  function fileContextFromFrontmatter(path, fm) {
    const base = resolveContainer(path, fm);
    const nsOverride = frontmatterNamespace(fm);
    return nsOverride === null ? base : { ...base, nsOverride };
  }
  function frontmatterNamespace(fm) {
    if (fm === null || fm === void 0) return null;
    const raw = fm["gtd-namespace"];
    if (typeof raw !== "string") return null;
    const s = raw.trim();
    return s === "" ? null : s;
  }
  function resolveContainer(path, fm) {
    if (fm === null || fm === void 0) return { path, container: "plain" };
    if (fm["gtd-recurring"] === true) return { path, container: "recurring" };
    if (fm["gtd-events"] === true) return { path, container: "events" };
    const cardOf = fm["gtd-card-of"];
    if (cardOf !== null && cardOf !== void 0 && cardOf !== false && String(cardOf).trim() !== "")
      return { path, container: "card" };
    if (fm["gtd-project"] === true) {
      const status = normalizeProjectStatus(fm["status"]);
      return status === void 0 ? { path, container: "project" } : { path, container: "project", projectStatus: status };
    }
    if (fm["gtd-board"] === true) return { path, container: "board" };
    if (fm["gtd-archive"] === true) return { path, container: "archive" };
    if (fm["gtd-inbox"] === true) return { path, container: "inbox" };
    return { path, container: "plain" };
  }
  function normalizeProjectStatus(raw) {
    if (raw === null || raw === void 0) return void 0;
    const s = String(raw).trim();
    if (s === "") return void 0;
    return PROJECT_STATUSES.has(s) ? s : "on-hold";
  }

  // src/mcp/scanFile.ts
  var TASK_RE = /^[ \t]*[-*+][ \t]+\[(.)\](?=\s|$)/u;
  var LIST_RE = /^([ \t]*)(?:[-*+]|\d+[.)])[ \t]+/;
  var HEADING_RE = /^#{1,6}\s/;
  var FENCE_RE = /^[ \t]*(```+|~~~+)/;
  var BLOCKQUOTE_RE = /^((?:[ \t]{0,3}>[ \t]?)+)/;
  function indentWidth(ws) {
    let w = 0;
    for (const ch of ws) w += ch === "	" ? 4 - w % 4 : 1;
    return w;
  }
  function scanSnapshotListItems(content) {
    const lines = content.split("\n");
    const items = [];
    const headings = [];
    const stack = [];
    let stackBqDepth = 0;
    let prevNonBlankIsList = false;
    let inFrontmatter = false;
    let inFence = false;
    let fenceMarker = "";
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].replace(/\r$/, "");
      if (i === 0 && line.trim() === "---") {
        inFrontmatter = true;
        continue;
      }
      if (inFrontmatter) {
        if (line.trim() === "---") inFrontmatter = false;
        continue;
      }
      const bq = BLOCKQUOTE_RE.exec(line);
      const bqDepth = bq !== null ? (bq[1].match(/>/g) ?? []).length : 0;
      const rest = bq !== null ? line.slice(bq[1].length) : line;
      const fence = FENCE_RE.exec(rest);
      if (inFence) {
        if (fence !== null && rest.trim().startsWith(fenceMarker)) inFence = false;
        if (line.trim() !== "") prevNonBlankIsList = false;
        continue;
      }
      if (fence !== null) {
        inFence = true;
        fenceMarker = fence[1];
        prevNonBlankIsList = false;
        continue;
      }
      if (HEADING_RE.test(rest)) {
        if (bqDepth === 0) {
          headings.push({
            position: { start: { line: i }, end: { line: i } },
            heading: rest.replace(/^#{1,6}\s+/, "").trim()
          });
        }
        stack.length = 0;
        prevNonBlankIsList = false;
        continue;
      }
      const lm = LIST_RE.exec(rest);
      if (lm !== null) {
        if (bqDepth === 0 && indentWidth(lm[1]) >= 4 && !prevNonBlankIsList) {
          prevNonBlankIsList = false;
          continue;
        }
        if (bqDepth !== stackBqDepth) {
          stack.length = 0;
          stackBqDepth = bqDepth;
        }
        const indent = lm[1].length;
        while (stack.length > 0 && stack[stack.length - 1].indent >= indent) stack.pop();
        const parent = stack.length > 0 ? stack[stack.length - 1].line : -1;
        stack.push({ line: i, indent });
        const tm = TASK_RE.exec(rest);
        items.push({
          position: { start: { line: i }, end: { line: i } },
          task: tm !== null ? tm[1] : void 0,
          parent
        });
        prevNonBlankIsList = true;
        continue;
      }
      if (rest.trim() === "" || !/^[ \t]/.test(rest)) stack.length = 0;
      if (line.trim() !== "") prevNonBlankIsList = false;
    }
    return snapshotListItems(items, headings);
  }

  // src/widget/widgetFrontmatter.ts
  var FRONTMATTER_RE = /^---\r?\n([\s\S]*?)\r?\n---[ \t]*(?:\r?\n|$)/;
  var KEY_RE = /^([A-Za-z0-9_][A-Za-z0-9_-]*):[ \t]*(.*)$/;
  function unquote(raw) {
    const v = raw.trim();
    if (v.length >= 2) {
      const first = v.charAt(0);
      const last = v.charAt(v.length - 1);
      if (first === '"' && last === '"' || first === "'" && last === "'") {
        return v.slice(1, -1);
      }
    }
    return v;
  }
  function parseWidgetFrontmatter(content) {
    const m = FRONTMATTER_RE.exec(content);
    if (m === null) return null;
    const body = m[1] ?? "";
    const out = {};
    let found = false;
    for (const rawLine of body.split("\n")) {
      const line = rawLine.replace(/\r$/, "");
      if (line.trim() === "" || line.trimStart().startsWith("#")) continue;
      if (/^[ \t]/.test(line)) continue;
      const km = KEY_RE.exec(line);
      if (km === null) continue;
      const key = km[1];
      const valueRaw = km[2] ?? "";
      const lower = valueRaw.trim().toLowerCase();
      let value;
      if (lower === "true") value = true;
      else if (lower === "false") value = false;
      else if (valueRaw.trim() === "") value = "";
      else value = unquote(valueRaw);
      out[key] = value;
      found = true;
    }
    return found ? out : null;
  }

  // src/widget/widgetIndex.ts
  var NOOP_EVENTS = {
    onChanged: () => () => void 0,
    onDeleted: () => () => void 0,
    onRenamed: () => () => void 0
  };
  async function buildWidgetIndex(files, today, errors) {
    const snapshots = [];
    for (const path of Object.keys(files)) {
      const content = files[path];
      try {
        if (typeof content !== "string") {
          throw new Error("\u0441\u043E\u0434\u0435\u0440\u0436\u0438\u043C\u043E\u0435 \u0444\u0430\u0439\u043B\u0430 \u043D\u0435 \u0441\u0442\u0440\u043E\u043A\u0430");
        }
        const data = parseWidgetFrontmatter(content);
        const context = fileContextFromFrontmatter(path, data);
        snapshots.push({
          path,
          content,
          listItems: scanSnapshotListItems(content),
          context
        });
      } catch (e) {
        errors.push(`file '${path}': ${errorMessage(e)}`);
      }
    }
    const clock = { todayIso: () => today, onDayRollover: () => () => void 0 };
    const indexer = new IndexerService({
      events: NOOP_EVENTS,
      clock,
      // eslint-disable-next-line @typescript-eslint/require-await
      initialScan: async function* () {
        for (const snap of snapshots) yield snap;
      },
      debounceMs: 0,
      chunkSize: Number.MAX_SAFE_INTEGER
      // без уступок макротаске: setTimeout не зовётся
    });
    await indexer.start();
    return { feed: indexer, allTasks: [...indexer.getIndex().all()] };
  }
  function errorMessage(e) {
    return e instanceof Error ? e.message : String(e);
  }

  // src/settings/Settings.ts
  var DEFAULT_SETTINGS = {
    commonRoot: "GTD",
    inboxIncludePlain: false,
    projectStrategy: "tag",
    projectTagPrefix: "#project/",
    calendarPlacement: ["due", "scheduled", "start"],
    deferPresets: [
      { label: "\u0417\u0430\u0432\u0442\u0440\u0430", offsetDays: 1 },
      { label: "+3 \u0434\u043D\u044F", offsetDays: 3 },
      { label: "\u0427\u0435\u0440\u0435\u0437 \u043D\u0435\u0434\u0435\u043B\u044E", offsetDays: 7 }
    ],
    firstDayOfWeek: 1,
    statusMap: {},
    defaultBoardPath: "",
    autoInjectId: true,
    debounceMs: { fileReindex: 150, queryRecompute: 50 },
    virtualizeThreshold: 100,
    // Дефолт "inbox" (фидбек пользователя): когда 🛫 наступает сама, задача
    // приходит именно во «Входящие» своего пространства, а не остаётся на месте.
    promoteTo: "inbox",
    promoteLastRun: null,
    recurring: {
      spawnTarget: "GTD/Inbox.md",
      catchUp: "latest",
      catchUpCap: 30
    },
    cardsFolder: "GTD/Cards",
    cardLinkInLine: true,
    eventsFile: "GTD/Events.md",
    archiveFile: "GTD/Archive.md",
    dayStatusFile: "GTD/DayStatus.md",
    onboarded: false,
    namespaces: [],
    activeNamespace: DEFAULT_NS,
    lastQuickAddKind: "task"
  };

  // src/settings/mergeSettings.ts
  function mergeSettings(defaults, loaded) {
    const data = asObject(loaded);
    return {
      ...defaults,
      ...data,
      debounceMs: { ...defaults.debounceMs, ...asObject(data.debounceMs) },
      recurring: { ...defaults.recurring, ...asObject(data.recurring) }
    };
  }
  function asObject(v) {
    return typeof v === "object" && v !== null && !Array.isArray(v) ? v : {};
  }

  // src/widget/widgetSettings.ts
  var COMMON_LABEL = "\u041E\u0431\u0449\u0435\u0435";
  var ALL_LABEL = "\u0412\u0441\u0435";
  function loadWidgetSettings(dataJson) {
    let loaded = null;
    let error = null;
    if (dataJson !== null && dataJson !== void 0) {
      try {
        loaded = JSON.parse(dataJson);
      } catch (e) {
        error = `invalid data.json: ${e instanceof Error ? e.message : String(e)}`;
        loaded = null;
      }
    }
    const merged = mergeSettings(DEFAULT_SETTINGS, loaded);
    merged.activeNamespace = normalizeActiveNamespace(merged.activeNamespace, merged.namespaces);
    return { settings: merged, error };
  }
  function nsLabel(active) {
    if (active === DEFAULT_NS) return COMMON_LABEL;
    if (active === ALL_NS) return ALL_LABEL;
    return active;
  }
  function fileNsLabel(filePath, nsOverride, defs) {
    return nsLabel(resolveNamespace(filePath, nsOverride, defs));
  }
  function resolveWidgetActive(arg, settings, errors, allowAll = true) {
    if (arg === null || arg === void 0 || arg.trim() === "") return DEFAULT_NS;
    const a = arg.trim();
    const lower = a.toLowerCase();
    if (a === ALL_LABEL || lower === "all" || a === "*") {
      return allowAll ? ALL_NS : DEFAULT_NS;
    }
    if (a === COMMON_LABEL || lower === "common" || lower === "default") return DEFAULT_NS;
    if (settings.namespaces.some((d) => d.name === a)) return a;
    errors.push(`unknown namespace '${a}' \u2014 falling back to '${COMMON_LABEL}'`);
    return DEFAULT_NS;
  }
  function widgetFilter(active, settings) {
    return { active, defs: settings.namespaces };
  }

  // src/widget/widgetEditLine.ts
  var ISO_DATE_SHAPE_RE = /^\d{4}-\d{2}-\d{2}$/;
  var DATE_FIELD_ORDER = ["due", "scheduled", "start"];
  function err(code) {
    return { ok: false, error: code };
  }
  function hasField(t, field) {
    return t.segments.some((s) => s.kind === "field" && s.field === field);
  }
  function firstDateField(t) {
    for (const f of DATE_FIELD_ORDER) if (hasField(t, f)) return f;
    return null;
  }
  function fieldDate(t, field) {
    let payload = null;
    for (const s of t.segments) if (s.kind === "field" && s.field === field) payload = s.payload;
    if (payload === null) return null;
    const m = /^(\d{4}-\d{2}-\d{2})/.exec(payload);
    return m ? m[1] : null;
  }
  function recurrenceToken(t) {
    let tok = null;
    for (const s of t.segments) if (s.kind === "field" && s.field === "recurrence") tok = s;
    return tok;
  }
  function stripAtTail(ruleText) {
    const m = /^(.*?)\s+at\s+\S+\s*$/i.exec(ruleText.trim());
    return m && m[1] !== void 0 ? m[1].trim() : ruleText.trim();
  }
  function parseTimeRange(input) {
    const s = input.trim();
    if (s === "") return null;
    const dash = s.indexOf("-");
    if (dash === -1) return TIME_RE.test(s) ? { time: s, timeEnd: null } : null;
    const a = s.slice(0, dash).trim();
    const b = s.slice(dash + 1).trim();
    if (!TIME_RE.test(a) || !TIME_RE.test(b) || b <= a) return null;
    return { time: a, timeEnd: b };
  }
  function applySeriesTime(line, timeRange) {
    const t = tokenizeTaskLine(line);
    if (t === null) return err("not-a-task");
    const tok = recurrenceToken(t);
    if (tok === null) return err("not-a-series");
    const base = stripAtTail(tok.payload);
    let newRule;
    if (timeRange === null) {
      newRule = base;
    } else {
      if (typeof timeRange !== "string") return err("invalid-time-range");
      const pr = parseTimeRange(timeRange);
      if (pr === null) return err("invalid-time-range");
      const tail = pr.timeEnd !== null ? `${pr.time}-${pr.timeEnd}` : pr.time;
      newRule = `${base} at ${tail}`;
    }
    const parsed = parseRule(newRule);
    if (isParseError(parsed)) return err("invalid-rule");
    if (parsed.fromCompletion) return err("series-completion-not-allowed");
    if (tok.gap === "" && tok.payload === "") tok.gap = " ";
    tok.payload = newRule;
    return { ok: true, line: serializeTokens(t) };
  }
  function applyDateTime(line, dateEdit, timeEdit) {
    const t = tokenizeTaskLine(line);
    if (t === null) return err("not-a-task");
    const field = firstDateField(t) ?? "due";
    const existingDate = fieldDate(t, field);
    let newDate;
    if (dateEdit === void 0) newDate = existingDate;
    else if (dateEdit === null) newDate = null;
    else {
      if (typeof dateEdit !== "string" || !ISO_DATE_SHAPE_RE.test(dateEdit)) return err("invalid-date");
      newDate = dateEdit;
    }
    let timeArg;
    let timeEndArg;
    if (timeEdit === void 0) {
      timeArg = void 0;
      timeEndArg = void 0;
    } else if (timeEdit === null) {
      timeArg = null;
      timeEndArg = null;
    } else {
      if (typeof timeEdit !== "string") return err("invalid-time-range");
      const pr = parseTimeRange(timeEdit);
      if (pr === null) return err("invalid-time-range");
      timeArg = pr.time;
      timeEndArg = pr.timeEnd;
    }
    if (newDate === null && typeof timeArg === "string") return err("time-without-date");
    try {
      return { ok: true, line: setField(line, field, newDate, timeArg, timeEndArg) };
    } catch {
      return err("invalid-date");
    }
  }
  function editLine(rawLine, editsRaw) {
    if (typeof rawLine !== "string") return err("not-a-task");
    const t0 = tokenizeTaskLine(rawLine);
    if (t0 === null) return err("not-a-task");
    const edits = editsRaw !== null && typeof editsRaw === "object" ? editsRaw : {};
    const isSeries = hasField(t0, "recurrence") && firstDateField(t0) === null;
    if (isSeries && edits.date !== void 0) return err("series-date-not-editable");
    let line = rawLine;
    if (edits.title !== void 0) {
      if (typeof edits.title !== "string") return err("invalid-title");
      const canon = edits.title.replace(/\s+/g, " ").trim();
      if (canon === "") return err("empty-title");
      try {
        line = setDescription(line, canon);
      } catch {
        return err("invalid-title");
      }
    }
    if (edits.location !== void 0) {
      if (edits.location !== null && typeof edits.location !== "string") {
        return err("invalid-location");
      }
      const loc = edits.location === null ? "" : edits.location.trim();
      try {
        line = setValueField(line, "location", loc === "" ? null : loc);
      } catch {
        return err("invalid-location");
      }
    }
    if (isSeries) {
      if (edits.timeRange !== void 0) {
        const r = applySeriesTime(line, edits.timeRange);
        if (!r.ok) return r;
        line = r.line;
      }
    } else if (edits.date !== void 0 || edits.timeRange !== void 0) {
      const r = applyDateTime(line, edits.date, edits.timeRange);
      if (!r.ok) return r;
      line = r.line;
    }
    return { ok: true, line };
  }
  function buildEditedLine(rawLine, edits) {
    try {
      return JSON.stringify(editLine(rawLine, edits));
    } catch (e) {
      return JSON.stringify({ ok: false, error: e instanceof Error ? e.message : String(e) });
    }
  }

  // src/widget/widgetCore.ts
  async function computeWidgetData(input) {
    const errors = [];
    const files = input && typeof input.files === "object" && input.files !== null ? input.files : {};
    const todayIso = input && typeof input.todayIso === "string" ? input.todayIso : "";
    const nowMinutes = input && typeof input.nowMinutes === "number" && Number.isFinite(input.nowMinutes) ? input.nowMinutes : 0;
    const dataJson = input && typeof input.dataJson === "string" ? input.dataJson : null;
    const { settings, error: settingsError } = loadWidgetSettings(dataJson);
    if (settingsError !== null) errors.push(settingsError);
    const defs = settings.namespaces;
    let allTasks = [];
    let resolveDep = () => [];
    try {
      const idx = await buildWidgetIndex(files, todayIso, errors);
      allTasks = idx.allTasks;
      const built = idx.feed;
      resolveDep = (id) => built.getIndex().resolveDep(id);
    } catch (e) {
      errors.push(`index build failed: ${errorMessage(e)}`);
    }
    const agendaDaysRaw = input && typeof input.agendaDays === "number" && Number.isFinite(input.agendaDays) ? Math.trunc(input.agendaDays) : 0;
    const agendaDays = Math.max(0, Math.min(30, agendaDaysRaw));
    const validToday = /^\d{4}-\d{2}-\d{2}$/.test(todayIso);
    const rangeDates = [todayIso];
    if (validToday) for (let i = 1; i < agendaDays; i++) rangeDates.push(addDaysIso(todayIso, i));
    const lastIso = rangeDates[rangeDates.length - 1];
    const todayItems = [];
    const agendaByDate = /* @__PURE__ */ new Map();
    try {
      const events = allTasks.filter((t) => t.container === "events");
      const occMap = expandEventOccurrences(events, todayIso, lastIso);
      const placement = settings.calendarPlacement;
      const candidates = allTasks.filter(
        (t) => !isTemplate(t) && !isDetail(t) && !isEvent(t) && !isArchived(t) && !isDone(t) && !isCancelled(t)
      );
      const placedMap = placeEvents(candidates, placement);
      const buildDay = (dateIso) => {
        const items = [];
        for (const o of occMap.get(dateIso) ?? []) {
          const startMinutes = o.time !== null ? timeToMinutes(o.time) : null;
          const endMinutes = o.timeEnd !== null ? timeToMinutes(o.timeEnd) : null;
          items.push({
            kind: "event",
            itemKind: o.kind === "series" ? "series-occurrence" : "single-event",
            title: o.title,
            startMinutes,
            endMinutes,
            allDay: startMinutes === null,
            location: o.location,
            file: o.task.filePath,
            line: o.task.lineStart + 1,
            namespace: fileNsLabel(o.task.filePath, o.task.nsOverride, defs),
            rawLine: o.task.rawLine,
            recurrenceText: o.kind === "series" ? o.task.recurrence : null
          });
        }
        for (const pe of placedMap.get(dateIso) ?? []) {
          const t = pe.task;
          const time = placedTime(t, pe.field);
          const timeEnd = placedTimeEnd(t, pe.field);
          const startMinutes = time !== null ? timeToMinutes(time) : null;
          const endMinutes = timeEnd !== null ? timeToMinutes(timeEnd) : null;
          items.push({
            kind: "task",
            itemKind: "task",
            title: t.description,
            startMinutes,
            endMinutes,
            allDay: startMinutes === null,
            location: t.location,
            file: t.filePath,
            line: t.lineStart + 1,
            namespace: fileNsLabel(t.filePath, t.nsOverride, defs),
            rawLine: t.rawLine,
            recurrenceText: null
          });
        }
        sortTodayItems(items);
        return items;
      };
      const todayBuilt = buildDay(todayIso);
      todayItems.push(...todayBuilt);
      for (const d of rangeDates) agendaByDate.set(d, d === todayIso ? todayBuilt : buildDay(d));
    } catch (e) {
      errors.push(`today failed: ${errorMessage(e)}`);
    }
    const agendaDaysList = agendaDays > 0 ? rangeDates.map((d) => ({ date: d, items: agendaByDate.get(d) ?? [] })) : [];
    const inboxActive = resolveWidgetActive(input?.inboxNamespace ?? null, settings, errors, true);
    const inboxItems = [];
    try {
      const ctx = {
        tasks: allTasks,
        today: todayIso,
        resolveDep,
        settingsBits: defaultInboxConfig(settings.inboxIncludePlain),
        namespace: widgetFilter(inboxActive, settings)
      };
      for (const t of evaluate({ kind: "inbox" }, ctx)) {
        inboxItems.push({
          title: t.description,
          file: t.filePath,
          line: t.lineStart + 1,
          id: t.taskId,
          location: t.location,
          namespace: fileNsLabel(t.filePath, t.nsOverride, defs)
        });
      }
    } catch (e) {
      errors.push(`inbox failed: ${errorMessage(e)}`);
    }
    const data = {
      today: {
        date: todayIso,
        items: todayItems,
        generatedAt: `${todayIso}T${minutesToTime(nowMinutes)}`
      },
      agenda: { days: agendaDaysList },
      inbox: { namespace: nsLabel(inboxActive), items: inboxItems },
      namespaces: defs.map((d) => ({ name: d.name, root: d.root })),
      errors
    };
    return JSON.stringify(data);
  }
  function sortTodayItems(items) {
    items.sort((a, b) => {
      if (a.allDay !== b.allDay) return a.allDay ? -1 : 1;
      if (!a.allDay) {
        const sa = a.startMinutes ?? 0;
        const sb = b.startMinutes ?? 0;
        if (sa !== sb) return sa - sb;
      }
      if (a.kind !== b.kind) return a.kind === "event" ? -1 : 1;
      if (a.title !== b.title) return a.title < b.title ? -1 : 1;
      if (a.file !== b.file) return a.file < b.file ? -1 : 1;
      return a.line - b.line;
    });
  }
  function buildCaptureLine(text, location) {
    const line = quickCaptureLine(typeof text === "string" ? text : "");
    if (line === null) throw new Error("empty capture text");
    const loc = typeof location === "string" ? location.trim() : "";
    if (loc === "") return line;
    try {
      return setValueField(line, "location", loc);
    } catch {
      return line;
    }
  }
  function captureTargetPath(dataJson, namespace) {
    const { settings } = loadWidgetSettings(typeof dataJson === "string" ? dataJson : null);
    const errors = [];
    const active = resolveWidgetActive(namespace ?? null, settings, errors, false);
    return nsCommonTarget(active, settings.namespaces, NS_CONVENTION.inbox, settings.commonRoot);
  }
  return __toCommonJS(widgetCore_exports);
})();
