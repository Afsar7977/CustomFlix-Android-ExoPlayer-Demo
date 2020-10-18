@file:Suppress("PackageName")

package com.afsar.customflix.Utilities

import android.content.Context
import androidx.datastore.DataStore
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.preferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ApplicationData(context: Context) {
    private val applicationContext = context.applicationContext
    private val dataStore: DataStore<Preferences>

    init {
        dataStore = applicationContext.createDataStore(
            name = "learnfhome"
        )
    }

    val userId: Flow<String?>
        get() = dataStore.data.map { preferences ->
            preferences[KEY_USERID]
        }

    suspend fun saveUserID(emp_code: String, cid: String) {
        dataStore.edit { preferences ->
            preferences[KEY_USERID] = emp_code
        }
    }

    companion object {
        val KEY_USERID = preferencesKey<String>("logged")
    }
}