package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estado completo de la pantalla del administrador
data class AdminUiState(
    val isLoading: Boolean = false,
    val todasLasCitas: List<CitaDTO> = emptyList(),
    val citasHoy: List<CitaDTO> = emptyList(),
    val barberos: List<BarberoDTO> = emptyList(),
    val servicios: List<ServicioDTO> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    private val fechaHoy: String
        get() = java.util.Calendar.getInstance().let {
            "%d-%02d-%02d".format(
                it.get(java.util.Calendar.YEAR),
                it.get(java.util.Calendar.MONTH) + 1,
                it.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }

    init { cargarDatosIniciales() }

    // Carga todas las citas, barberos y servicios en paralelo
    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val citas = apiService.getCitas()
                val barberos = apiService.getBarberos()
                val servicios = apiService.getServicios()

                val todasLasCitas = if (citas.isSuccessful) citas.body() ?: emptyList()
                else emptyList()

                _uiState.value = _uiState.value.copy(
                    isLoading      = false,
                    todasLasCitas  = todasLasCitas,
                    citasHoy       = todasLasCitas.filter { it.fecha == fechaHoy },
                    barberos       = if (barberos.isSuccessful)
                        barberos.body() ?: emptyList() else emptyList(),
                    servicios      = if (servicios.isSuccessful)
                        servicios.body() ?: emptyList() else emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

    // ── Gestión de Barberos ───────────────────────────────────────────────

    // PUT /api/barberos/{id} — activa o desactiva un barbero
    // Al desactivar un barbero el backend cancela sus citas pendientes
    fun toggleActivoBarbero(barbero: BarberoDTO) {
        viewModelScope.launch {
            try {
                val actualizado = barbero.copy(activo = !(barbero.activo ?: true))
                val response = apiService.updateBarbero(barbero.idBarbero!!, actualizado)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = if (actualizado.activo == true)
                            "${barbero.nombre} activado"
                        else
                            "${barbero.nombre} desactivado — citas canceladas"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al actualizar barbero (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // POST /api/barberos — crea un nuevo barbero
    fun crearBarbero(nombre: String, especialidad: String, telefono: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createBarbero(
                    BarberoDTO(
                        idBarbero   = null,
                        nombre      = nombre,
                        especialidad = especialidad.ifBlank { null },
                        telefono    = telefono.ifBlank { null },
                        activo      = true,
                        idUsuario   = null
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "Barbero '$nombre' creado correctamente"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Error al crear barbero (${response.code()})"
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

    // DELETE /api/barberos/{id} — elimina un barbero
    fun eliminarBarbero(idBarbero: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteBarbero(idBarbero)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Barbero eliminado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al eliminar barbero"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // ── Gestión de Servicios ──────────────────────────────────────────────

    // POST /api/servicios — crea un nuevo servicio
    fun crearServicio(
        nombre: String, descripcion: String,
        precio: Double, duracionMinutos: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createServicio(
                    ServicioDTO(
                        idServicio      = null,
                        nombre          = nombre,
                        descripcion     = descripcion.ifBlank { null },
                        precio          = precio,
                        duracionMinutos = duracionMinutos
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "Servicio '$nombre' creado correctamente"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Error al crear servicio (${response.code()})"
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

    // DELETE /api/servicios/{id} — elimina un servicio
    fun eliminarServicio(idServicio: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteServicio(idServicio)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Servicio eliminado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al eliminar servicio"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // PATCH /api/citas/{id}/cancelar — el admin puede cancelar cualquier cita
    fun cancelarCita(idCita: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.cancelarCita(idCita)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Cita cancelada"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al cancelar la cita"
                    )
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
        fun factory(apiService: ApiService): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return AdminViewModel(apiService) as T
                }
            }
        }
    }
}