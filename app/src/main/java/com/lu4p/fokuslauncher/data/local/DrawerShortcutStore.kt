package com.lu4p.fokuslauncher.data.local

import android.content.Context
import com.lu4p.fokuslauncher.data.model.AppShortcutAction
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class DrawerShortcutStore(context: Context) {
    private val preferences = context.getSharedPreferences("drawer_shortcuts", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val shortcuts = state.asStateFlow()

    @Synchronized
    fun add(action: AppShortcutAction) {
        if (action.target !is ShortcutTarget.LauncherShortcut || state.value.any { it.id == action.id }) return
        save(state.value + action.copy(icon = null))
    }

    @Synchronized
    fun remove(action: AppShortcutAction) {
        save(state.value.filterNot { it.id == action.id })
    }

    private fun save(actions: List<AppShortcutAction>) {
        val json = JSONArray()
        actions.forEach {
            json.put(JSONObject().put("target", ShortcutTarget.encode(it.target))
                    .put("profile", it.profileKey).put("appLabel", it.appLabel)
                    .put("actionLabel", it.actionLabel))
        }
        preferences.edit().putString("entries", json.toString()).apply()
        state.value = actions
    }

    private fun read(): List<AppShortcutAction> = runCatching {
        val json = JSONArray(preferences.getString("entries", "[]") ?: "[]")
        (0 until json.length()).mapNotNull { index ->
            runCatching {
                val entry = json.getJSONObject(index)
                val target = ShortcutTarget.decode(entry.getString("target")) as? ShortcutTarget.LauncherShortcut
                        ?: return@runCatching null
                AppShortcutAction(entry.getString("appLabel"), entry.getString("actionLabel"),
                        target, entry.getString("profile"))
            }.getOrNull()
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())
}
