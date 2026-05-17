package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estado completo de la pantalla del cliente
// La UI observa este objeto y se redibuja automáticamente cuando cambia
data class ClienteUiState(
    val isLoading: Boolean = false,
    val barberos: List<BarberoDTO> = emptyList(),
    val servicios: List<ServicioDTO> = emptyList(),
    val citas: List<CitaDTO> = emptyList(),
    val horasDisponibles: List<String> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ClienteViewModel(
    private val apiService: ApiService,
    private val idUsuario: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClienteUiState())
    val uiState: StateFlow<ClienteUiState> = _uiState

    // Al crear el ViewModel carga todo inmediatamente
    init {
        cargarDatosIniciales()
    }

    // Carga barberos activos, servicios y citas del cliente en paralelo
    // Se llama al arrancar y después de agendar/cancelar para refrescar
    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val barberos  = apiService.getBarberosActivos()
                val servicios = apiService.getServicios()
                val citas     = apiService.getCitasByUsuario(idUsuario)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    barberos  = if (barberos.isSuccessful)  barberos.body()  ?: emptyList() else emptyList(),
                    servicios = if (servicios.isSuccessful) servicios.body() ?: emptyList() else emptyList(),
                    citas     = if (citas.isSuccessful)     citas.body()     ?: emptyList() else emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

    // Consulta las horas disponibles para un barbero en una fecha
    // Se llama cuando el usuario selecciona barbero + fecha en la tab Agendar
    fun cargarDisponibilidad(idBarbero: Long, fecha: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getDisponibilidad(idBarbero, fecha)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        horasDisponibles = response.body() ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        horasDisponibles = emptyList(),
                        errorMessage = "No se pudo cargar disponibilidad"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al cargar disponibilidad"
                )
            }
        }
    }

    // Limpia las horas disponibles cuando el usuario cambia de barbero o fecha
    fun limpiarDisponibilidad() {
        _uiState.value = _uiState.value.copy(horasDisponibles = emptyList())
    }

    // POST /api/citas — crea la cita y recarga la lista
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
                        successMessage = "¡Cita agendada exitosamente!",
                        horasDisponibles = emptyList()
                    )
                    cargarDatosIniciales() // refresca la lista de citas
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = when (response.code()) {
                            400  -> "Horario no disponible, elige otro"
                            403  -> "No tienes permiso para agendar"
                            else -> "Error al agendar (${response.code()})"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión."
                )
            }
        }
    }

    // PATCH /api/citas/{id}/cancelar — cancela una cita PENDIENTE
    fun cancelarCita(idCita: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.cancelarCita(idCita)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "Cita cancelada correctamente"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Error al cancelar la cita"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión."
                )
            }
        }
    }

    // POST /api/resenas — envía reseña para una cita FINALIZADA
    // calificacion: 1 a 5 estrellas
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
                    _uiState.value = _uiState.value.copy(
                        successMessage = "¡Reseña enviada! Gracias."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al enviar reseña"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Sin conexión."
                )
            }
        }
    }

    // Limpia mensajes de éxito/error después de mostrarlos en el Snackbar
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage   = null,
            successMessage = null
        )
    }

    // Factory — le dice a Android cómo crear este ViewModel con sus dependencias
    // sin necesitar Hilt
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