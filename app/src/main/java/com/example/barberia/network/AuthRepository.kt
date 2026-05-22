package com.example.barberia.network

import com.example.barberia.model.LoginRequest
import com.example.barberia.model.LoginResponse
import com.example.barberia.model.RegisterRequest
import com.example.barberia.model.RegisterResponse
import com.example.barberia.utils.SessionManager
import com.example.barberia.model.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
        // Reintento automático para cuando Render está dormido (cold start)
        val maxIntentos = 2
        var ultimoError: Exception? = null

        for (intento in 1..maxIntentos) {
            try {
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
                        } catch (_: Exception) { }
                    }

                    return Result.Success(body)
                } else {
                    return Result.Error(
                        message = when (response.code()) {
                            401  -> "Correo o contraseña incorrectos"
                            400  -> "Datos inválidos"
                            else -> "Error del servidor (${response.code()})"
                        },
                        code = response.code()
                    )
                }
            } catch (e: Exception) {
                ultimoError = e
                if (intento < maxIntentos) {
                    // Render puede tardar hasta 60s en despertar — esperamos y reintentamos
                    kotlinx.coroutines.delay(2000)
                }
            }
        }

        return Result.Error(
            message = when (ultimoError) {
                is SocketTimeoutException -> "El servidor está dormido. Reintenta en unos segundos."
                is UnknownHostException -> "Sin conexión a internet. Verifica tu red."
                else -> "Error de conexión. Verifica tu internet e intenta de nuevo."
            }
        )
    }

    suspend fun register(
        nombre: String,
        correoOTelefono: String,
        password: String,
        telefono: String? = null
    ): Result<RegisterResponse> {
        val maxIntentos = 2
        var ultimoError: Exception? = null

        for (intento in 1..maxIntentos) {
            try {
                val response = apiService.register(
                    RegisterRequest(nombre, correoOTelefono, password, telefono)
                )
                if (response.isSuccessful) {
                    return Result.Success(response.body()!!)
                } else {
                    return Result.Error(
                        message = when (response.code()) {
                            400  -> "El correo ya está registrado"
                            else -> "Error al registrarse (${response.code()})"
                        },
                        code = response.code()
                    )
                }
            } catch (e: Exception) {
                ultimoError = e
                if (intento < maxIntentos) {
                    kotlinx.coroutines.delay(2000)
                }
            }
        }

        return Result.Error(
            message = when (ultimoError) {
                is SocketTimeoutException -> "El servidor está dormido. Reintenta en unos segundos."
                is UnknownHostException -> "Sin conexión a internet. Verifica tu red."
                else -> "Error de conexión. Verifica tu internet e intenta de nuevo."
            }
        )
    }

    suspend fun logout() {
        sessionManager.cerrarSesion()
    }
}