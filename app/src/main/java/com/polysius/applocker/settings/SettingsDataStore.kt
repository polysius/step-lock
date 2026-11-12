package com.polysius.applocker.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** The single DataStore file holding every user setting. */
val Context.settingsDataStore by preferencesDataStore(name = "settings")
