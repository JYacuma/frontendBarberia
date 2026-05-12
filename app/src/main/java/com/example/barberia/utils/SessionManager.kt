package com.example.barberia.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Crea el DataStore una sola vez a nivel de app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {

    companion object {
        val KEY_TOKEN  = stringPreferencesKey("token")
        val KEY_ROL    = stringPreferencesKey("rol")
        val KEY_ID     = longPreferencesKey("id_usuario")
        val KEY_NOMBRE = stringPreferencesKey("nombre")
        val KEY_CORREO = stringPreferencesKey("correo")

        // Instancia única — reemplaza Hilt de forma simple
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Guarda toda la sesión después del login
    suspend fun guardarSesion(
        token: String,
        id: Long,
        nombre: String,
        correo: String,
        rol: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN]  = token
            prefs[KEY_ID]     = id
            prefs[KEY_NOMBRE] = nombre
            prefs[KEY_CORREO] = correo
            prefs[KEY_ROL]    = rol
        }
    }

    // Flujos reactivos — la UI los observa y se actualiza automáticamente
    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val rol: Flow<String?>   = context.dataStore.data.map { it[KEY_ROL] }
    val id: Flow<Long?>      = context.dataStore.data.map { it[KEY_ID] }
    val nombre: Flow<String?> = context.dataStore.data.map { it[KEY_NOMBRE] }

    // Borra todo al cerrar sesión
    suspend fun cerrarSesion() {
        context.dataStore.edit { it.clear() }
    }

    // Lectura rápida no reactiva — usada en RetrofitClient para el token
    suspend fun getToken(): String? {
        var result: String? = null
        context.dataStore.data.collect {
            result = it[KEY_TOKEN]
            return@collect
        }
        return result
    }
}