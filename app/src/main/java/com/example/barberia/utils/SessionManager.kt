package com.example.barberia.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {

    companion object {
        val KEY_TOKEN     = stringPreferencesKey("token")
        val KEY_ROL       = stringPreferencesKey("rol")
        val KEY_ID        = longPreferencesKey("id_usuario")
        val KEY_NOMBRE    = stringPreferencesKey("nombre")
        val KEY_CORREO    = stringPreferencesKey("correo")
        // idBarbero real de la tabla barbero — diferente al idUsuario
        // Solo se guarda cuando el rol es BARBERO
        val KEY_ID_BARBERO = longPreferencesKey("id_barbero")

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Guarda la sesión completa después del login
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

    // Guarda el idBarbero real después de buscarlo por idUsuario
    // Se llama desde AuthRepository justo después del login de un BARBERO
    suspend fun guardarIdBarbero(idBarbero: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ID_BARBERO] = idBarbero
        }
    }

    // Flujos reactivos — la UI los observa
    val token: Flow<String?>  = context.dataStore.data.map { it[KEY_TOKEN] }
    val rol: Flow<String?>    = context.dataStore.data.map { it[KEY_ROL] }
    val id: Flow<Long?>       = context.dataStore.data.map { it[KEY_ID] }
    val nombre: Flow<String?> = context.dataStore.data.map { it[KEY_NOMBRE] }
    // 0L = no encontrado aún, se resuelve después del login
    val idBarbero: Flow<Long?> = context.dataStore.data.map { it[KEY_ID_BARBERO] }

    // Borra todo al cerrar sesión — incluyendo el idBarbero
    suspend fun cerrarSesion() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun getToken(): String? {
        var result: String? = null
        context.dataStore.data.collect {
            result = it[KEY_TOKEN]
            return@collect
        }
        return result
    }
}