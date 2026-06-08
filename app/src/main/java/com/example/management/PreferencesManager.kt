package com.example.management

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    private val SORT_KEY = stringPreferencesKey("sort_by")

    val sortBy = context.dataStore.data.map { preferences ->
        preferences[SORT_KEY] ?: "Status"
    }

    suspend fun saveSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[SORT_KEY] = order
        }
    }
}
