package com.example.barberia.network

import com.example.barberia.model.LoginRequest
import com.example.barberia.model.LoginResponse
import com.example.barberia.model.RegisterRequest
import com.example.barberia.model.RegisterResponse
import com.example.barberia.utils.SessionManager

// Resultado genérico para cualquier llamada al backend

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = 0) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    // POST /api/auth/login
    // Si el backend responde 200 guarda el token en disco automáticamente
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                // Guarda JWT + datos del usuario en DataStore
                sessionManager.guardarSesion(
                    token  = body.token,
                    id     = body.idUsuario,
                    nombre = body.nombre,
                    correo = body.correo,
                    rol    = body.rol.name
                )
                Result.Success(body)
            } else {
                Result.Error(
                    message = when (response.code()) {
                        401  -> "Correo o contraseña incorrectos"
                        400  -> "Datos inválidos"
                        else -> "Error del servidor (${response.code()})"
                    },
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            // Sin internet, timeout de Render, etc.
            Result.Error("Sin conexión. Verifica tu internet e intenta de nuevo.")
        }
    }

    // POST /api/auth/register
    suspend fun register(
        nombre: String,
        correoOTelefono: String,
        password: String
    ): Result<RegisterResponse> {
        return try {
            val response = apiService.register(
                RegisterRequest(nombre, correoOTelefono, password)
            )
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(
                    message = when (response.code()) {
                        400  -> "El correo ya está registrado"
                        else -> "Error al registrarse (${response.code()})"
                    },
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            Result.Error("Sin conexión. Verifica tu internet e intenta de nuevo.")
        }
    }

    // Borra el token y datos del disco
    suspend fun logout() {
        sessionManager.cerrarSesion()
    }
}