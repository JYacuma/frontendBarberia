package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


data class ClienteUiState(
    val isLoading: Boolean = false,
    val barberos: List<BarberoDTO> = emptyList(),
    val servicios: List<ServicioDTO> = emptyList(),
    val citas: List<CitaDTO> = emptyList(),
    val horasDisponibles: List<String> = emptyList(),
    val perfil: UsuarioDTO? = null,
    val isLoadingPerfil: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ClienteViewModel(
    private val apiService: ApiService,
    private val idUsuario: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClienteUiState())
    val uiState: StateFlow<ClienteUiState> = _uiState

    init { cargarDatosIniciales() }

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val barberos  = apiService.getBarberosActivos()
                val servicios = apiService.getServicios()
                val citas     = apiService.getCitasByUsuario(idUsuario)
                val perfil    = apiService.getUsuarioById(idUsuario)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    barberos  = if (barberos.isSuccessful)
                        barberos.body() ?: emptyList() else emptyList(),
                    servicios = if (servicios.isSuccessful)
                        servicios.body() ?: emptyList() else emptyList(),
                    citas     = if (citas.isSuccessful)
                        citas.body() ?: emptyList() else emptyList(),
                    perfil    = if (perfil.isSuccessful) perfil.body() else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

    // PUT /api/usuarios/{id} — actualiza nombre y teléfono
    // El correo no se edita por seguridad
    fun actualizarPerfil(nombre: String, telefono: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPerfil = true)
            try {
                val perfilActual = _uiState.value.perfil ?: return@launch
                val actualizado  = perfilActual.copy(
                    nombre   = nombre.ifBlank { perfilActual.nombre ?: "" },
                    telefono = telefono.ifBlank { null }
                )
                val response = apiService.updateUsuario(idUsuario, actualizado)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPerfil = false,
                        perfil          = response.body(),
                        successMessage  = "Perfil actualizado correctamente"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPerfil = false,
                        errorMessage    = when (response.code()) {
                            400  -> "Datos inválidos"
                            409  -> "El teléfono ya está en uso"
                            else -> "Error al actualizar (${response.code()})"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPerfil = false,
                    errorMessage    = "Sin conexión."
                )
            }
        }
    }

    fun cargarDisponibilidad(idBarbero: Long, fecha: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getDisponibilidad(idBarbero, fecha)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        horasDisponibles = response.body() ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al cargar disponibilidad"
                )
            }
        }
    }

    fun limpiarDisponibilidad() {
        _uiState.value = _uiState.value.copy(horasDisponibles = emptyList())
    }

    fun agendarCita(idBarbero: Long, idServicio: Long, fecha: String, horaInicio: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = apiService.createCita(
                    CitaRequest(
                        idUsuario  = idUsuario,
                        idBarbero  = idBarbero,
                        idServicio = idServicio,
                        fecha      = fecha,
                        horaInicio = horaInicio
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "¡Cita agendada exitosamente!"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = when (response.code()) {
                            400  -> "Horario no disponible, elige otro"
                            else -> "Error al agendar (${response.code()})"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = "Sin conexión.")
            }
        }
    }

    fun cancelarCita(idCita: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.cancelarCita(idCita)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Cita cancelada")
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al cancelar")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Error al cancelar")
            }
        }
    }

    fun enviarResena(idCita: Long, idBarbero: Long, calificacion: Int, comentario: String) {
        viewModelScope.launch {
            try {
                val response = apiService.createResena(
                    ResenaRequest(
                        idCita       = idCita,
                        idUsuario    = idUsuario,
                        idBarbero    = idBarbero,
                        calificacion = calificacion,
                        comentario   = comentario.ifBlank { null }
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "¡Reseña enviada!")
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al enviar reseña (${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Error al enviar reseña")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    companion object {
        fun factory(apiService: ApiService, idUsuario: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ClienteViewModel(apiService, idUsuario) as T
                }
            }
        }
    }
}