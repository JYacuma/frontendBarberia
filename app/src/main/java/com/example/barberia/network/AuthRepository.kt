package com.example.barberia.network

import com.example.barberia.model.LoginRequest
import com.example.barberia.model.LoginResponse
import com.example.barberia.model.RegisterRequest
import com.example.barberia.model.RegisterResponse
import com.example.barberia.utils.SessionManager
import com.example.barberia.model.*


sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = 0) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                sessionManager.guardarSesion(
                    token  = body.token,
                    id     = body.idUsuario,
                    nombre = body.nombre,
                    correo = body.correo,
                    rol    = body.rol.name
                )

                // Si el rol es BARBERO, buscamos el idBarbero real en la tabla barbero
                // filtrando por idUsuario — así BarberoScreen usa el id correcto
                if (body.rol == RolEnum.BARBERO) {
                    try {
                        val barberos = apiService.getBarberos()
                        if (barberos.isSuccessful) {
                            val barbero = barberos.body()
                                ?.firstOrNull { it.idUsuario == body.idUsuario }
                            if (barbero?.idBarbero != null) {
                                sessionManager.guardarIdBarbero(barbero.idBarbero)
                            }
                        }
                    } catch (e: Exception) {
                        // Si falla la búsqueda del barbero no bloqueamos el login
                        // BarberoScreen mostrará un error al cargar sus datos
                    }
                }

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
            Result.Error("Sin conexión. Verifica tu internet e intenta de nuevo.")
        }
    }

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

    suspend fun logout() {
        sessionManager.cerrarSesion()
    }
}