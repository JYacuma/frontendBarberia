package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoading: Boolean = false,
    val todasLasCitas: List<CitaDTO> = emptyList(),
    val citasHoy: List<CitaDTO> = emptyList(),
    val citasConDetalles: List<CitaConDetalle> = emptyList(),
    val barberos: List<BarberoDTO> = emptyList(),
    val servicios: List<ServicioDTO> = emptyList(),
    val serviciosConDetalle: List<ServicioConDetalle> = emptyList(),
    val horarios: List<HorarioBarberoDTO> = emptyList(),
    val barberoSeleccionado: BarberoDTO? = null,
    val resenas: List<ResenaDTO> = emptyList(),
    val usuariosPorRol: List<UsuarioDTO> = emptyList(),
    val usuariosBarbero: List<UsuarioDTO> = emptyList(),
    val todosUsuarios: List<UsuarioDTO> = emptyList(),
    val servicioEditando: ServicioDTO? = null,
    val horarioEditando: HorarioBarberoDTO? = null,
    val notificacionesCount: Int = 0,
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

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val citasResponse = apiService.getCitas()
                val barberosResponse = apiService.getBarberos()
                val serviciosResponse = apiService.getServicios()
                val barberosUsersResponse = apiService.getUsuariosByRol("BARBERO")
                val usuariosResponse = apiService.getUsuarios()

                val todasLasCitas = if (citasResponse.isSuccessful) citasResponse.body() ?: emptyList() else emptyList()
                val barberos = if (barberosResponse.isSuccessful) barberosResponse.body() ?: emptyList() else emptyList()
                val servicios = if (serviciosResponse.isSuccessful) serviciosResponse.body() ?: emptyList() else emptyList()
                val usuariosBarbero = if (barberosUsersResponse.isSuccessful) barberosUsersResponse.body() ?: emptyList() else emptyList()
                val todosUsuarios = if (usuariosResponse.isSuccessful) usuariosResponse.body() ?: emptyList() else emptyList()

                val citasConDetalles = todasLasCitas.map { cita ->
                    val barbero = barberos.find { it.idBarbero == cita.idBarbero }
                    val servicio = servicios.find { it.idServicio == cita.idServicio }
                    val usuario = todosUsuarios.find { it.idUsuario == cita.idUsuario }
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
                        clienteDTOs = todosUsuarios.filter { it.idUsuario in clientesIds }
                    )
                }

                val citasHoyList = todasLasCitas.filter { it.fecha == fechaHoy }

                _uiState.value = _uiState.value.copy(
                    isLoading            = false,
                    todasLasCitas        = todasLasCitas,
                    citasHoy             = citasHoyList,
                    citasConDetalles     = citasConDetalles,
                    barberos             = barberos,
                    servicios            = servicios,
                    serviciosConDetalle  = serviciosConDetalle,
                    usuariosBarbero      = usuariosBarbero,
                    todosUsuarios        = todosUsuarios,
                    notificacionesCount  = citasHoyList.size
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

    fun cargarServiciosConDetalle() {
        val state = _uiState.value
        val detalle = state.servicios.map { servicio ->
            val citasDelServicio = state.todasLasCitas.filter { it.idServicio == servicio.idServicio }
            val barberosIds = citasDelServicio.map { it.idBarbero }.distinct()
            val clientesIds = citasDelServicio.map { it.idUsuario }.distinct()
            ServicioConDetalle(
                servicio = servicio,
                barberoDTOs = state.barberos.filter { it.idBarbero in barberosIds },
                clienteDTOs = state.todosUsuarios.filter { it.idUsuario in clientesIds }
            )
        }
        _uiState.value = state.copy(serviciosConDetalle = detalle)
    }

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

    fun crearBarbero(nombre: String, especialidad: String, telefono: String, idUsuarioVinculado: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.createBarbero(
                    BarberoDTO(
                        idBarbero    = null,
                        nombre       = nombre,
                        especialidad = especialidad.ifBlank { null },
                        telefono     = telefono.ifBlank { null },
                        activo       = true,
                        idUsuario    = idUsuarioVinculado
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

    fun editarBarbero(id: Long, nombre: String, especialidad: String, telefono: String, idUsuario: Long?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = apiService.updateBarbero(
                    id,
                    BarberoDTO(
                        idBarbero    = id,
                        nombre       = nombre,
                        especialidad = especialidad.ifBlank { null },
                        telefono     = telefono.ifBlank { null },
                        activo       = true,
                        idUsuario    = idUsuario
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading      = false,
                        successMessage = "Barbero '$nombre' actualizado correctamente"
                    )
                    cargarDatosIniciales()
                } else if (response.code() == 409) {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Este teléfono ya está registrado, usa otro"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading    = false,
                        errorMessage = "Error al actualizar barbero (${response.code()})"
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

    fun crearServicio(nombre: String, descripcion: String, precio: Double, duracionMinutos: Int) {
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

    fun seleccionarBarbero(barbero: BarberoDTO?) {
        _uiState.value = _uiState.value.copy(barberoSeleccionado = barbero)
        if (barbero != null) cargarHorarios(barbero.idBarbero!!)
    }

    fun cargarHorarios(idBarbero: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getHorariosByBarbero(idBarbero)
                _uiState.value = _uiState.value.copy(
                    horarios = if (response.isSuccessful)
                        response.body() ?: emptyList() else emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Error al cargar horarios")
            }
        }
    }

    fun crearHorario(idBarbero: Long, diaSemana: DiaSemanaEnum, horaInicio: String, horaFin: String) {
        viewModelScope.launch {
            try {
                val response = apiService.createHorario(
                    HorarioBarberoDTO(
                        idHorario  = null,
                        idBarbero  = idBarbero,
                        diaSemana  = diaSemana,
                        horaInicio = horaInicio,
                        horaFin    = horaFin
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Horario agregado correctamente"
                    )
                    cargarHorarios(idBarbero)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al crear horario (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun eliminarHorario(idHorario: Long, idBarbero: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteHorario(idHorario)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Horario eliminado"
                    )
                    cargarHorarios(idBarbero)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al eliminar horario"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun iniciarEdicionServicio(servicio: ServicioDTO) {
        _uiState.value = _uiState.value.copy(servicioEditando = servicio)
    }

    fun cancelarEdicionServicio() {
        _uiState.value = _uiState.value.copy(servicioEditando = null)
    }

    fun actualizarServicio(servicio: ServicioDTO) {
        viewModelScope.launch {
            try {
                val response = apiService.updateServicio(servicio.idServicio!!, servicio)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        servicioEditando = null,
                        successMessage = "Servicio actualizado"
                    )
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al actualizar (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun iniciarEdicionHorario(horario: HorarioBarberoDTO) {
        _uiState.value = _uiState.value.copy(horarioEditando = horario)
    }

    fun cancelarEdicionHorario() {
        _uiState.value = _uiState.value.copy(horarioEditando = null)
    }

    fun actualizarHorario(horario: HorarioBarberoDTO) {
        viewModelScope.launch {
            try {
                val response = apiService.updateHorario(horario.idHorario!!, horario)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        horarioEditando = null,
                        successMessage = "Horario actualizado"
                    )
                    cargarHorarios(horario.idBarbero)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al actualizar (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sin conexión.")
            }
        }
    }

    fun cargarResenas() {
        viewModelScope.launch {
            try {
                val todas = mutableListOf<ResenaDTO>()
                uiState.value.barberos.forEach { barbero ->
                    val res = apiService.getResenasByBarbero(barbero.idBarbero!!)
                    if (res.isSuccessful) {
                        todas.addAll(res.body() ?: emptyList())
                    }
                }
                _uiState.value = _uiState.value.copy(resenas = todas)
            } catch (_: Exception) { }
        }
    }

    fun eliminarResena(idResena: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteResena(idResena)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "Reseña eliminada")
                    cargarResenas()
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Error al eliminar reseña")
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
                    return AdminViewModel(apiService) as T
                }
            }
        }
    }
}
