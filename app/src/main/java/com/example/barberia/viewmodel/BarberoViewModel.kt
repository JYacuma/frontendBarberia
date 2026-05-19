package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estado completo de la pantalla del barbero
// La UI observa este objeto y se redibuja automáticamente cuando cambia
data class BarberoUiState(
    val isLoading: Boolean = false,
    val citasHoy: List<CitaDTO> = emptyList(),       // citas del día actual
    val todasLasCitas: List<CitaDTO> = emptyList(),  // citas de la semana
    val horarios: List<HorarioBarberoDTO> = emptyList(),
    val bloqueos: List<BloqueoHorarioDTO> = emptyList(),
    val resenas: List<ResenaDTO> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BarberoViewModel(
    private val apiService: ApiService,
    private val idBarbero: Long,       // id del barbero en tabla barbero
    private val idUsuario: Long        // id del usuario vinculado al barbero
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarberoUiState())
    val uiState: StateFlow<BarberoUiState> = _uiState

    // Fecha de hoy en formato "YYYY-MM-DD" sin java.time (minSdk 24)
    private val fechaHoy: String
        get() = java.util.Calendar.getInstance().let {
            "%d-%02d-%02d".format(
                it.get(java.util.Calendar.YEAR),
                it.get(java.util.Calendar.MONTH) + 1,
                it.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }

    init {
        cargarDatosIniciales()
    }

    // Carga citas de hoy, reseñas y bloqueos del barbero
    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val citasHoy  = apiService.getCitasByBarberoYFecha(idBarbero, fechaHoy)
                val resenas   = apiService.getResenasByBarbero(idBarbero)
                val bloqueos  = apiService.getBloqueosByBarbero(idBarbero)
                val horarios  = apiService.getHorariosByBarbero(idBarbero)

                _uiState.value = _uiState.value.copy(
                    isLoading     = false,
                    citasHoy      = if (citasHoy.isSuccessful)
                        citasHoy.body() ?: emptyList() else emptyList(),
                    resenas       = if (resenas.isSuccessful)
                        resenas.body() ?: emptyList() else emptyList(),
                    bloqueos      = if (bloqueos.isSuccessful)
                        bloqueos.body() ?: emptyList() else emptyList(),
                    horarios      = if (horarios.isSuccessful)
                        horarios.body() ?: emptyList() else emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

    // PATCH /api/citas/{id}/iniciar
    // El barbero confirma que la cita está comenzando
    // Cambia el estado de PENDIENTE → EN_CURSO
    fun iniciarCita(idCita: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.iniciarCita(idCita)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Cita iniciada"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al iniciar la cita (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // PATCH /api/citas/{id}/finalizar
    // El barbero confirma que terminó el servicio
    // Cambia EN_CURSO → FINALIZADA y actualiza el promedio del cliente
    fun finalizarCita(idCita: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.finalizarCita(idCita)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Cita finalizada correctamente"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al finalizar la cita (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // PATCH /api/citas/{id}/no-presento
    // El barbero marca que el cliente no se presentó
    // Cambia PENDIENTE → NO_PRESENTADO
    fun marcarNoPresento(idCita: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.noPresento(idCita)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Marcado como no presentado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al marcar la cita"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // POST /api/bloqueos
    // El barbero bloquea un rango de tiempo (vacaciones, descanso, etc.)
    // fechaInicio y fechaFin en formato "YYYY-MM-DDTHH:mm:ss"
    fun crearBloqueo(fechaInicio: String, fechaFin: String, motivo: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createBloqueo(
                    BloqueoHorarioDTO(
                        idBloqueo   = null,
                        idBarbero   = idBarbero,
                        fechaInicio = fechaInicio,
                        fechaFin    = fechaFin,
                        motivo      = motivo.ifBlank { "Sin motivo" }
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "Horario bloqueado correctamente"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Error al bloquear el horario (${response.code()})"
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

    // DELETE /api/bloqueos/{id}
    // Elimina un bloqueo existente
    fun eliminarBloqueo(idBloqueo: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteBloqueo(idBloqueo)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Bloqueo eliminado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al eliminar el bloqueo"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun eliminarResena(idResena: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteResena(idResena)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Reseña eliminada")
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al eliminar reseña")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage   = null,
            successMessage = null
        )
    }

    companion object {
        fun factory(
            apiService: ApiService,
            idBarbero: Long,
            idUsuario: Long
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return BarberoViewModel(apiService, idBarbero, idUsuario) as T
                }
            }
        }
    }
}