package com.example.barberia.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object ClienteBarberoPreseleccion {
    var barbero: BarberoDTO? by mutableStateOf(null)
}

data class ClienteUiState(
    val isLoading: Boolean = false,
    val barberos: List<BarberoDTO> = emptyList(),
    val barberosMasSolicitados: List<BarberoDTO> = emptyList(),
    val servicios: List<ServicioDTO> = emptyList(),
    val citas: List<CitaDTO> = emptyList(),
    val horasDisponibles: List<String> = emptyList(),
    val horarioDia: List<HorarioBarberoDTO> = emptyList(),
    val perfil: UsuarioDTO? = null,
    val isLoadingPerfil: Boolean = false,
    val promedioCliente: PromedioClienteDTO? = null,
    val citaDetalle: CitaDTO? = null,
    val notificacionesCita: List<NotificacionDTO> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val popularBarberos: List<BarberoDTO> = emptyList(),
    val popularServicios: List<ServicioDTO> = emptyList(),
    val notificaciones: List<NotificacionDTO> = emptyList(),
    val citasConDetalles: List<CitaConDetalle> = emptyList(),
    val barberoHorarios: List<HorarioBarberoDTO> = emptyList(),
    val barberoAgendarId: Long = 0L,
    val barberoPreseleccionado: BarberoDTO? = null
)

class ClienteViewModel(
    private val apiService: ApiService,
    private val idUsuario: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClienteUiState())
    val uiState: StateFlow<ClienteUiState> = _uiState

    init { if (idUsuario != 0L) cargarDatosIniciales() }

    fun cargarSiUsuarioValido(id: Long) {
        if (id != 0L) cargarDatosIniciales()
    }

    fun cargarDatosIniciales() {
        if (idUsuario == 0L) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val barberosDeferred  = async { apiService.getBarberosActivos() }
                val serviciosDeferred = async { apiService.getServicios() }
                val citasDeferred     = async { apiService.getCitasByUsuario(idUsuario) }
                val perfilDeferred    = async { apiService.getUsuarioById(idUsuario) }

                val barberos  = barberosDeferred.await()
                val servicios = serviciosDeferred.await()
                val citas     = citasDeferred.await()
                val perfil    = perfilDeferred.await()

                val barberosList = if (barberos.isSuccessful) barberos.body() ?: emptyList() else emptyList()
                val serviciosList = if (servicios.isSuccessful) servicios.body() ?: emptyList() else emptyList()
                val citasList = if (citas.isSuccessful) citas.body() ?: emptyList() else emptyList()

                val masSolicitadosDeferred = async { apiService.getBarberosMasSolicitados() }
                val promedioDeferred = async { apiService.getPromedioByUsuarioYBarbero(idUsuario, 0) }

                val masSolicitados = masSolicitadosDeferred.await()
                val masSolicitadosList = if (masSolicitados.isSuccessful) masSolicitados.body() ?: emptyList() else emptyList()

                val barberosMap = barberosList.associateBy { it.idBarbero }
                val serviciosMap = serviciosList.associateBy { it.idServicio }
                val citasConDetalles = citasList.map { cita ->
                    CitaConDetalle(
                        cita = cita,
                        barberoNombre = barberosMap[cita.idBarbero]?.nombre ?: "Barbero #${cita.idBarbero}",
                        servicioNombre = serviciosMap[cita.idServicio]?.nombre ?: "Servicio #${cita.idServicio}"
                    )
                }

                val allNotificaciones = mutableListOf<NotificacionDTO>()
                for (cita in citasList) {
                    cita.idCita?.let { idCita ->
                        try {
                            val notisResp = apiService.getNotificacionesByCita(idCita)
                            if (notisResp.isSuccessful) {
                                allNotificaciones.addAll(notisResp.body() ?: emptyList())
                            }
                        } catch (_: Exception) { }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading              = false,
                    barberos               = barberosList,
                    barberosMasSolicitados = masSolicitadosList,
                    popularBarberos        = masSolicitadosList,
                    servicios              = serviciosList,
                    popularServicios       = serviciosList.sortedBy { it.precio },
                    citas                  = citasList,
                    citasConDetalles       = citasConDetalles,
                    perfil                 = if (perfil.isSuccessful) perfil.body() else null,
                    notificaciones         = allNotificaciones
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

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
        if (idUsuario == 0L) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sesión no válida. Inicia sesión de nuevo.")
            return
        }
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

    fun cancelarCitaConMotivo(idCita: Long, motivo: String) {
        viewModelScope.launch {
            try {
                Log.d("ClienteVM", "Cancelando cita $idCita, motivo: $motivo")
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
        if (idUsuario == 0L) return
        viewModelScope.launch {
            try {
                val ahora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                val response = apiService.createResena(
                    ResenaRequest(
                        idCita       = idCita,
                        idUsuario    = idUsuario,
                        idBarbero    = idBarbero,
                        calificacion = calificacion,
                        comentario   = comentario.ifBlank { null },
                        fecha        = ahora
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(successMessage = "¡Reseña enviada!")
                    cargarDatosIniciales()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Error al enviar reseña (${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Error al enviar reseña")
            }
        }
    }

    fun cargarHorariosBarbero(idBarbero: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getHorariosByBarbero(idBarbero)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        barberoHorarios = response.body() ?: emptyList()
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun cargarHorarioDia(idBarbero: Long, dia: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getHorariosByBarberoYDia(idBarbero, dia)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        horarioDia = response.body() ?: emptyList()
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun cargarDetalleCita(idCita: Long) {
        viewModelScope.launch {
            try {
                val cita = apiService.getCitaById(idCita)
                val notis = apiService.getNotificacionesByCita(idCita)
                _uiState.value = _uiState.value.copy(
                    citaDetalle        = if (cita.isSuccessful) cita.body() else null,
                    notificacionesCita = if (notis.isSuccessful)
                        notis.body() ?: emptyList() else emptyList()
                )
            } catch (_: Exception) { }
        }
    }

    fun limpiarDetalleCita() {
        _uiState.value = _uiState.value.copy(citaDetalle = null, notificacionesCita = emptyList())
    }

    fun cargarPromedio(idBarbero: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getPromedioByUsuarioYBarbero(idUsuario, idBarbero)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        promedioCliente = response.body()
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun cargarPopularBarberos() {
        viewModelScope.launch {
            try {
                val response = apiService.getBarberosMasSolicitados()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        popularBarberos = response.body() ?: emptyList()
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun cargarPopularServicios() {
        viewModelScope.launch {
            try {
                val response = apiService.getServicios()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        popularServicios = (response.body() ?: emptyList()).sortedBy { it.precio }
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun cargarNotificacionesPorCita(idCita: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.getNotificacionesByCita(idCita)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        notificaciones = response.body() ?: emptyList()
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun preseleccionarBarbero(barbero: BarberoDTO?) {
        _uiState.value = _uiState.value.copy(barberoPreseleccionado = barbero)
    }

    fun preseleccionarBarbero(id: Long) {
        _uiState.value = _uiState.value.copy(barberoAgendarId = id)
        if (id != 0L) cargarHorariosBarbero(id)
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
