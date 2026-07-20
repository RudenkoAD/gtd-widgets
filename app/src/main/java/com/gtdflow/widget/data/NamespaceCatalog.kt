package com.gtdflow.widget.data

import android.content.Context
import android.net.Uri
import com.gtdflow.widget.engine.NamespaceDef
import com.gtdflow.widget.engine.WidgetJson
import com.gtdflow.widget.vault.VaultManager
import com.gtdflow.widget.vault.VaultReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Каталог пользовательских пространств для конфигуратора виджета «Входящие».
 *
 * Источник — кэш AppStore (заполняется при каждом пересчёте виджетов). Если кэша ещё
 * нет (виджет добавляют раньше первого пересчёта), читаем ТОЛЬКО data.json vault и
 * разбираем массив namespaces напрямую (без обхода дерева .md). Встроенные метки
 * «Общее»/«Все» здесь НЕ входят — их добавляет UI как варианты по умолчанию.
 */
object NamespaceCatalog {

    private val nsSer = ListSerializer(NamespaceDef.serializer())

    suspend fun userNamespaces(context: Context): List<NamespaceDef> {
        AppStore.namespacesJson(context)?.let { json ->
            runCatching { WidgetJson.decodeFromString(nsSer, json) }.getOrNull()?.let { return it }
        }
        val treeUri: Uri = VaultManager.treeUri(context) ?: return emptyList()
        if (!VaultManager.hasAccess(context, treeUri)) return emptyList()
        val dataJson = withContext(Dispatchers.IO) {
            VaultReader.readDataJsonOnly(context, treeUri)
        } ?: return emptyList()
        return parseNamespaces(dataJson)
    }

    /** Достать [{name,root}] из сырого data.json (терпимо к отсутствию/битому полю). */
    private fun parseNamespaces(dataJson: String): List<NamespaceDef> = runCatching {
        val root = WidgetJson.parseToJsonElement(dataJson).jsonObject
        val arr = root["namespaces"]?.jsonArray ?: return emptyList()
        arr.mapNotNull { el ->
            val obj = el.jsonObject
            val name = obj["name"]?.jsonPrimitive?.contentOrNull
            val nsRoot = obj["root"]?.jsonPrimitive?.contentOrNull
            if (name != null && nsRoot != null) NamespaceDef(name, nsRoot) else null
        }
    }.getOrDefault(emptyList())
}
