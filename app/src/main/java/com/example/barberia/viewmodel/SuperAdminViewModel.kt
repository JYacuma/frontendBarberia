package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estado completo de la pantalla del SuperAdmin
// Tiene todo lo del Admin + gestión completa de usuarios
data class SuperAdminUiState(
    val isLoading: Boolean = false,
    val usuarios: List<UsuarioDTO> = emptyList(),
    val usuariosPorRol: List<UsuarioDTO> = emptyList(),
    val barberos: List<BarberoDTO> = emptyList(),
    val servicios: List<ServicioDTO> = emptyList(),
    val todasLasCitas: List<CitaDTO> = emptyList(),
    val citasHoy: List<CitaDTO> = emptyList(),
    val notificaciones: List<NotificacionDTO> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SuperAdminViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuperAdminUiState())
    val uiState: StateFlow<SuperAdminUiState> = _uiState

    private val fechaHoy: String
        get() = java.util.Calendar.getInstance().let {
            "%d-%02d-%02d".format(
                it.get(java.util.Calendar.YEAR),
                it.get(java.util.Calendar.MONTH) + 1,
                it.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }

    init { cargarDatosIniciales() }

    // Carga todo — usuarios, barberos, servicios, citas y notificaciones
    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val usuarios  = apiService.getUsuarios()
                val barberos  = apiService.getBarberos()
                val servicios = apiService.getServicios()
                val citas     = apiService.getCitas()

                val todasLasCitas = if (citas.isSuccessful) citas.body() ?: emptyList()
                else emptyList()

                _uiState.value = _uiState.value.copy(
                    isLoading     = false,
                    usuarios      = if (usuarios.isSuccessful)
                        usuarios.body() ?: emptyList() else emptyList(),
                    barberos      = if (barberos.isSuccessful)
                        barberos.body() ?: emptyList() else emptyList(),
                    servicios     = if (servicios.isSuccessful)
                        servicios.body() ?: emptyList() else emptyList(),
                    todasLasCitas = todasLasCitas,
                    citasHoy      = todasLasCitas.filter { it.fecha == fechaHoy }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

    // ── Gestión de Usuarios (exclusivo SuperAdmin) ────────────────────────

    // POST /api/usuarios — crea cualquier tipo de usuario con cualquier rol
    fun crearUsuario(
        nombre: String, correo: String,
        password: String, rol: RolEnum
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createUsuario(
                    UsuarioDTO(
                        idUsuario = null,
                        nombre    = nombre,
                        correo    = correo,
                        telefono  = null,
                        password  = password,  // ⭐ AHORA SÍ FUNCIONA
                        rol       = rol,
                        activo    = true
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "Usuario '$nombre' creado con rol ${rol.name}"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Error al crear usuario (${response.code()})"
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

    // PUT /api/usuarios/{id} — activa o desactiva un usuario
    fun toggleActivoUsuario(usuario: UsuarioDTO) {
        viewModelScope.launch {
            try {
                val actualizado = usuario.copy(activo = !(usuario.activo ?: true))
                val response = apiService.updateUsuario(usuario.idUsuario!!, actualizado)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = if (actualizado.activo == true)
                            "${usuario.nombre} activado"
                        else
                            "${usuario.nombre} desactivado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al actualizar usuario (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // DELETE /api/usuarios/{id} — elimina un usuario permanentemente
    // Solo SuperAdmin puede hacer esto
    fun eliminarUsuario(idUsuario: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteUsuario(idUsuario)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Usuario eliminado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al eliminar usuario (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // ── Gestión de Barberos ───────────────────────────────────────────────

    fun crearBarbero(nombre: String, especialidad: String, telefono: String, idUsuarioVinculado: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createBarbero(
                    BarberoDTO(null, nombre,
                        especialidad.ifBlank { null },
                        telefono.ifBlank { null }, true, idUsuarioVinculado)
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Barbero '$nombre' creado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al crear barbero (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = "Sin conexión.")
            }
        }
    }

    fun eliminarBarbero(idBarbero: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteBarbero(idBarbero)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Barbero eliminado")
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al eliminar barbero")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun toggleActivoBarbero(barbero: BarberoDTO) {
        viewModelScope.launch {
            try {
                val actualizado = barbero.copy(activo = !(barbero.activo ?: true))
                val response = apiService.updateBarbero(barbero.idBarbero!!, actualizado)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = if (actualizado.activo == true)
                            "${barbero.nombre} activado" else "${barbero.nombre} desactivado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al actualizar barbero")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // ── Gestión de Servicios ──────────────────────────────────────────────

    fun crearServicio(
        nombre: String, descripcion: String,
        precio: Double, duracionMinutos: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createServicio(
                    ServicioDTO(null, nombre,
                        descripcion.ifBlank { null }, precio, duracionMinutos)
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Servicio '$nombre' creado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al crear servicio (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = "Sin conexión.")
            }
        }
    }

    fun eliminarServicio(idServicio: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteServicio(idServicio)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Servicio eliminado")
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al eliminar servicio")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // ── Citas ─────────────────────────────────────────────────────────────

    fun cancelarCita(idCita: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.cancelarCita(idCita)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Cita cancelada")
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al cancelar la cita")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun cargarUsuariosPorRol(rol: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getUsuariosByRol(rol)
                _uiState.value = _uiState.value.copy(
                    usuariosPorRol = if (response.isSuccessful)
                        response.body() ?: emptyList() else emptyList()
                )
            } catch (_: Exception) { }
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
                    return SuperAdminViewModel(apiService) as T
                }
            }
        }
    }
}