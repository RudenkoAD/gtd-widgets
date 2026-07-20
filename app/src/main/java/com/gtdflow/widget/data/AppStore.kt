package com.gtdflow.widget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Единый app-level DataStore: конфиг vault + кэш ленты «Сегодня» (не зависит от пространства). */
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "gtd_widget")

/**
 * Хранилище конфигурации приложения и кэша данных виджета «Сегодня».
 *
 * Здесь живёт то, что общо для всего приложения: SAF-URI выбранного vault, его
 * отображаемое имя (для deep-link) и последний посчитанный снимок «сегодня»
 * (JSON секции today + метка обновления + ошибки). Данные виджета «Входящие»
 * пер-виджетные — они в Glance-состоянии (см. InboxWidget), не тут.
 */
object AppStore {
    private val KEY_VAULT_URI = stringPreferencesKey("vault_uri")
    private val KEY_VAULT_NAME = stringPreferencesKey("vault_name")
    private val KEY_TODAY_JSON = stringPreferencesKey("today_json")
    private val KEY_TODAY_UPDATED = stringPreferencesKey("today_updated")
    private val KEY_TODAY_ERROR = stringPreferencesKey("today_error")
    private val KEY_NAMESPACES = stringPreferencesKey("namespaces_json")

    data class VaultConfig(val treeUri: String?, val vaultName: String?) {
        val isConfigured: Boolean get() = !treeUri.isNullOrBlank()
    }

    /**
     * Снимок кэша «сегодня»: сырой JSON секции today (WidgetData.today), метка
     * «обновлено HH:mm» и текст последней ошибки расчёта (null, если последний
     * расчёт успешен — успех очищает ошибку).
     */
    data class TodayCache(val todayJson: String?, val updatedHhmm: String?, val error: String?)

    fun vaultConfigFlow(context: Context): Flow<VaultConfig> =
        context.appDataStore.data.map { p ->
            VaultConfig(p[KEY_VAULT_URI], p[KEY_VAULT_NAME])
        }

    suspend fun vaultConfig(context: Context): VaultConfig =
        vaultConfigFlow(context).first()

    suspend fun saveVault(context: Context, treeUri: String, vaultName: String) {
        context.appDataStore.edit { p ->
            p[KEY_VAULT_URI] = treeUri
            p[KEY_VAULT_NAME] = vaultName
        }
    }

    fun todayCacheFlow(context: Context): Flow<TodayCache> =
        context.appDataStore.data.map { p ->
            TodayCache(p[KEY_TODAY_JSON], p[KEY_TODAY_UPDATED], p[KEY_TODAY_ERROR])
        }

    suspend fun todayCache(context: Context): TodayCache =
        todayCacheFlow(context).first()

    /** Успешный расчёт: пишем ленту + метку и ОЧИЩАЕМ ошибку. */
    suspend fun saveTodayCache(context: Context, todayJson: String, updatedHhmm: String) {
        context.appDataStore.edit { p ->
            p[KEY_TODAY_JSON] = todayJson
            p[KEY_TODAY_UPDATED] = updatedHhmm
            p.remove(KEY_TODAY_ERROR)
        }
    }

    /**
     * Сбой расчёта: сохраняем текст ошибки; ленту и метку «обновлено» НЕ трогаем —
     * метка отражает последний УСПЕШНЫЙ расчёт (иначе виджет врал бы о свежести кэша).
     */
    suspend fun saveTodayError(context: Context, error: String) {
        context.appDataStore.edit { p ->
            p[KEY_TODAY_ERROR] = error
        }
    }

    /** Кэш списка пользовательских пространств (JSON-массив [{name,root}]) для конфигуратора. */
    suspend fun namespacesJson(context: Context): String? =
        context.appDataStore.data.map { it[KEY_NAMESPACES] }.first()

    suspend fun saveNamespacesJson(context: Context, json: String) {
        context.appDataStore.edit { p -> p[KEY_NAMESPACES] = json }
    }
}
