package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SuperAdminUiState(
    val isLoading: Boolean = false,
    val usuarios: List<UsuarioDTO> = emptyList(),
    val usuariosPorRol: List<UsuarioDTO> = emptyList(),
    val barberos: List<BarberoDTO> = emptyList(),
    val servicios: List<ServicioDTO> = emptyList(),
    val serviciosConDetalle: List<ServicioConDetalle> = emptyList(),
    val todasLasCitas: List<CitaDTO> = emptyList(),
    val citasHoy: List<CitaDTO> = emptyList(),
    val citasConDetalles: List<CitaConDetalle> = emptyList(),
    val promediosBarbero: Map<Long, Double> = emptyMap(),
    val notificaciones: List<NotificacionDTO> = emptyList(),
    val notificacionesCount: Int = 0,
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

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val usuariosResponse   = apiService.getUsuarios()
                val barberosResponse   = apiService.getBarberos()
                val serviciosResponse  = apiService.getServicios()
                val citasResponse      = apiService.getCitas()

                val usuarios  = if (usuariosResponse.isSuccessful) usuariosResponse.body() ?: emptyList() else emptyList()
                val barberos  = if (barberosResponse.isSuccessful) barberosResponse.body() ?: emptyList() else emptyList()
                val servicios = if (serviciosResponse.isSuccessful) serviciosResponse.body() ?: emptyList() else emptyList()
                val todasLasCitas = if (citasResponse.isSuccessful) citasResponse.body() ?: emptyList() else emptyList()

                val citasConDetalles = todasLasCitas.map { cita ->
                    val barbero = barberos.find { it.idBarbero == cita.idBarbero }
                    val servicio = servicios.find { it.idServicio == cita.idServicio }
                    val usuario = usuarios.find { it.idUsuario == cita.idUsuario }
                    CitaConDetalle(
                        cita = cita,
                        clienteNombre = usuario?.nombre ?: "Desconocido",
                        barberoNombre = barbero?.nombre ?: "Desconocido",
                        servicioNombre = servicio?.nombre ?: "Desconocido",
                        precio = servicio?.precio ?: 0.0
                    )
                }

                val serviciosConDetalle = servicios.map { servicio ->
                    val citasDelServicio = todasLasCitas.filter { it.idServicio == servicio.idServicio }
                    val barberosIds = citasDelServicio.map { it.idBarbero }.distinct()
                    val clientesIds = citasDelServicio.map { it.idUsuario }.distinct()
                    ServicioConDetalle(
                        servicio = servicio,
                        barberoDTOs = barberos.filter { it.idBarbero in barberosIds },
                        clienteDTOs = usuarios.filter { it.idUsuario in clientesIds }
                    )
                }

                val resenas = mutableListOf<ResenaDTO>()
                barberos.forEach { barbero ->
                    val res = apiService.getResenasByBarbero(barbero.idBarbero!!)
                    if (res.isSuccessful) resenas.addAll(res.body() ?: emptyList())
                }
                val promediosBarbero = barberos.associate { barbero ->
                    val r = resenas.filter { it.idBarbero == barbero.idBarbero }
                    barbero.idBarbero!! to (if (r.isEmpty()) 0.0 else r.map { it.calificacion }.average())
                }

                val citasHoyList = todasLasCitas.filter { it.fecha == fechaHoy }

                var notificacionesReales = 0
                for (cita in todasLasCitas.take(50)) {
                    cita.idCita?.let { id ->
                        try {
                            val r = apiService.getNotificacionesByCita(id)
                            if (r.isSuccessful) notificacionesReales += (r.body()?.size ?: 0)
                        } catch (_: Exception) { }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading            = false,
                    usuarios             = usuarios,
                    barberos             = barberos,
                    servicios            = servicios,
                    serviciosConDetalle  = serviciosConDetalle,
                    todasLasCitas        = todasLasCitas,
                    citasHoy             = citasHoyList,
                    citasConDetalles     = citasConDetalles,
                    promediosBarbero     = promediosBarbero,
                    notificacionesCount  = notificacionesReales
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

    // ── Gestión de Usuarios ────────────────────────────────────────────────

    fun crearUsuario(nombre: String, correo: String, password: String, rol: RolEnum) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.register(
                    RegisterRequest(nombre, correo, password, rol = rol.name)
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "Usuario '$nombre' creado con rol ${rol.name}"
                    )
                    cargarDatosIniciales()
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = when (response.code()) {
                            409 -> "El correo o teléfono ya está registrado. Verifica los datos."
                            400 -> "Datos inválidos. Revisa que el correo tenga formato correcto."
                            else -> "Error al crear usuario (${response.code()}): $errorBody"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Sin conexión.")
            }
        }
    }

    fun toggleActivoUsuario(usuario: UsuarioDTO) {
        viewModelScope.launch {
            try {
                val actualizado = usuario.copy(activo = !(usuario.activo ?: true))
                val response = apiService.updateUsuario(usuario.idUsuario!!, actualizado)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = if (actualizado.activo == true) "${usuario.nombre} activado"
                        else "${usuario.nombre} desactivado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al actualizar usuario (${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun eliminarUsuario(idUsuario: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteUsuario(idUsuario)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Usuario eliminado")
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al eliminar usuario (${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun editarUsuario(id: Long, nombre: String, telefono: String?, rol: RolEnum, activo: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val original = _uiState.value.usuarios.find { it.idUsuario == id }
                val dto = UsuarioDTO(
                    idUsuario = id,
                    nombre    = nombre,
                    correo    = original?.correo ?: "",
                    telefono  = telefono,
                    rol       = rol,
                    activo    = activo
                )
                val response = apiService.updateUsuario(id, dto)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "Usuario '$nombre' actualizado"
                    )
                    cargarDatosIniciales()
                } else if (response.code() == 409) {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Este correo o teléfono ya está en uso"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Error al actualizar usuario (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Sin conexión.")
            }
        }
    }

    // ── Gestión de Barberos ────────────────────────────────────────────────

    fun crearBarbero(nombre: String, especialidad: String, telefono: String, idUsuarioVinculado: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createBarbero(
                    BarberoDTO(null, nombre, especialidad.ifBlank { null }, telefono.ifBlank { null }, true, idUsuarioVinculado)
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
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Sin conexión.")
            }
        }
    }

    fun editarBarbero(id: Long, nombre: String, especialidad: String, telefono: String, idUsuario: Long?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.updateBarbero(
                    id,
                    BarberoDTO(idBarbero = id, nombre = nombre, especialidad = especialidad.ifBlank { null },
                        telefono = telefono.ifBlank { null }, activo = true, idUsuario = idUsuario)
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Barbero '$nombre' actualizado"
                    )
                    cargarDatosIniciales()
                } else if (response.code() == 409) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Este teléfono ya está registrado, usa otro"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al actualizar barbero (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Sin conexión.")
            }
        }
    }

    fun eliminarBarbero(idBarbero: Long) {
        viewModelScope.launch {
            try {
                val pendientes = _uiState.value.todasLasCitas.filter {
                    it.idBarbero == idBarbero && it.estado == EstadoCitaEnum.PENDIENTE
                }
                pendientes.forEach { cita ->
                    apiService.cancelarCita(cita.idCita!!)
                }
                val response = apiService.deleteBarbero(idBarbero)
                if (response.isSuccessful) {
                    val msg = if (pendientes.isNotEmpty()) {
                        "Barbero eliminado. ${pendientes.size} cita(s) cancelada(s). Se notificó a los clientes."
                    } else {
                        "Barbero eliminado"
                    }
                    _uiState.value = _uiState.value.copy(successMessage = msg)
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al eliminar barbero")
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
                        successMessage = if (actualizado.activo == true) "${barbero.nombre} activado"
                        else "${barbero.nombre} desactivado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al actualizar barbero")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    // ── Gestión de Servicios ──────────────────────────────────────────────

    fun crearServicio(nombre: String, descripcion: String, precio: Double, duracionMinutos: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createServicio(
                    ServicioDTO(null, nombre, descripcion.ifBlank { null }, precio, duracionMinutos)
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
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Sin conexión.")
            }
        }
    }

    fun actualizarServicio(servicio: ServicioDTO) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.updateServicio(servicio.idServicio!!, servicio)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Servicio actualizado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al actualizar servicio (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Sin conexión.")
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
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al eliminar servicio")
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
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al cancelar la cita")
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
                    usuariosPorRol = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
                )
            } catch (_: Exception) { }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
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
