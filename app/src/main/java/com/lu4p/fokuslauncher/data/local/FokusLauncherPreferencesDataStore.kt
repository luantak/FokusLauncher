package com.lu4p.fokuslauncher.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore instance for launcher preferences; do not add a second delegate for this name. */
val Context.fokusLauncherPreferencesDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "fokus_launcher_prefs")
