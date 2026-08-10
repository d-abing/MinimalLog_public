package com.aube.minimallog.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

object LocalePrefs {
    private val KEY_LOCALE = stringPreferencesKey("app_locale")

    fun flow(context: Context) = context.dataStore.data.map { it[KEY_LOCALE].orEmpty() }

    suspend fun set(context: Context, tag: String) {
        context.dataStore.edit { it[KEY_LOCALE] = tag }
    }
}

object LogViewPrefs {
    private val KEY_PHOTO_ONLY = booleanPreferencesKey("log_photo_only")

    fun flow(context: Context) = context.dataStore.data.map { preferences ->
        preferences[KEY_PHOTO_ONLY] ?: false
    }

    suspend fun setPhotoOnly(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PHOTO_ONLY] = enabled
        }
    }
}
