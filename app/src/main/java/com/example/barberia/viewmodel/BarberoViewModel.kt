package com.example.barberia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class BarberoUiState(
    val isLoading: Boolean = false,
    val citasHoy: List<CitaDTO> = emptyList(),
    val todasLasCitas: List<CitaDTO> = emptyList(),
    val citasConDetalle: List<CitaConDetalle> = emptyList(),
    val horarios: List<HorarioBarberoDTO> = emptyList(),
    val bloqueos: List<BloqueoHorarioDTO> = emptyList(),
    val resenas: List<ResenaDTO> = emptyList(),
    val resenasConCliente: List<Pair<ResenaDTO, String>> = emptyList(),
    val promedio: Double = 0.0,
    val notificacionesCount: Int = 0,
    val todosBloqueos: List<BloqueoHorarioDTO> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BarberoViewModel(
    private val apiService: ApiService,
    private val idBarbero: Long,
    private val idUsuario: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarberoUiState())
    val uiState: StateFlow<BarberoUiState> = _uiState

    var restBlock: String? = null
    var diaDescanso: String? = null

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

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val citasHoy  = apiService.getCitasByBarberoYFecha(idBarbero, fechaHoy)
                val resenas   = apiService.getResenasByBarbero(idBarbero)
                val bloqueos  = apiService.getBloqueosByBarbero(idBarbero)
                val horarios  = apiService.getHorariosByBarbero(idBarbero)

                val citas = if (citasHoy.isSuccessful) citasHoy.body() ?: emptyList() else emptyList()
                val resenasList = if (resenas.isSuccessful) resenas.body() ?: emptyList() else emptyList()
                val bloqueosList = if (bloqueos.isSuccessful) bloqueos.body() ?: emptyList() else emptyList()
                val horariosList = if (horarios.isSuccessful) horarios.body() ?: emptyList() else emptyList()

                val barberosResp = apiService.getBarberosActivos()
                val todosBarberos = if (barberosResp.isSuccessful) barberosResp.body() ?: emptyList() else emptyList()
                val todosBloqueosList = mutableListOf<BloqueoHorarioDTO>()
                for (b in todosBarberos) {
                    b.idBarbero?.let { id ->
                        try {
                            val bResp = apiService.getBloqueosByBarbero(id)
                            if (bResp.isSuccessful) {
                                todosBloqueosList.addAll(bResp.body() ?: emptyList())
                            }
                        } catch (_: Exception) { }
                    }
                }

                val prom = if (resenasList.isNotEmpty())
                    resenasList.mapNotNull { it.calificacion }.average() else 0.0

                val usuariosIdsResenas = resenasList.map { it.idUsuario }.distinct()
                val usuariosMap = mutableMapOf<Long, String>()
                for (uid in usuariosIdsResenas) {
                    val uResp = apiService.getUsuarioById(uid)
                    if (uResp.isSuccessful) {
                        uResp.body()?.let { usuariosMap[uid] = it.nombre }
                    }
                }
                val resConCliente = resenasList.map { r ->
                    r to (usuariosMap[r.idUsuario] ?: "Cliente #${r.idUsuario}")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    citasHoy = citas,
                    bloqueos = bloqueosList,
                    horarios = horariosList,
                    resenas = resenasList,
                    resenasConCliente = resConCliente,
                    promedio = prom,
                    notificacionesCount = citas.size,
                    todosBloqueos = todosBloqueosList.toList()
                )

                cargarCitasConDetalle()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sin conexión. Verifica tu internet."
                )
            }
        }
    }

    fun cargarCitasConDetalle() {
        viewModelScope.launch {
            try {
                val citas = _uiState.value.citasHoy
                if (citas.isEmpty()) return@launch

                val barberoResp = apiService.getBarberoById(idBarbero)
                val barberoNombre = if (barberoResp.isSuccessful)
                    barberoResp.body()?.nombre ?: "Barbero" else "Barbero"

                val serviciosIds = citas.map { it.idServicio }.distinct()
                val serviciosMap = mutableMapOf<Long, String>()
                for (sid in serviciosIds) {
                    val sResp = apiService.getServicioById(sid)
                    if (sResp.isSuccessful) {
                        sResp.body()?.let { serviciosMap[sid] = it.nombre }
                    }
                }

                val usuariosIds = citas.map { it.idUsuario }.distinct()
                val usuariosMap = mutableMapOf<Long, String>()
                for (uid in usuariosIds) {
                    val uResp = apiService.getUsuarioById(uid)
                    if (uResp.isSuccessful) {
                        uResp.body()?.let { usuariosMap[uid] = it.nombre }
                    }
                }

                val detalle = citas.map { cita ->
                    CitaConDetalle(
                        cita = cita,
                        clienteNombre = usuariosMap[cita.idUsuario] ?: "Cliente #${cita.idUsuario}",
                        barberoNombre = barberoNombre,
                        servicioNombre = serviciosMap[cita.idServicio] ?: "Servicio #${cita.idServicio}"
                    )
                }

                _uiState.value = _uiState.value.copy(citasConDetalle = detalle)
            } catch (_: Exception) { }
        }
    }

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

    fun guardarBloqueoDescanso(dia: String, bloque: String) {
        viewModelScope.launch {
            val days = listOf("DOMINGO", "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO")
            val targetIndex = days.indexOf(dia.uppercase())
            if (targetIndex < 0) return@launch
            val cal = java.util.Calendar.getInstance()
            val todayIndex = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
            var diff = targetIndex - todayIndex
            if (diff <= 0) diff += 7
            cal.add(java.util.Calendar.DAY_OF_MONTH, diff)
            val fecha = "%d-%02d-%02d".format(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            val parts = bloque.split("-")
            if (parts.size != 2) return@launch
            crearBloqueo("${fecha}T${parts[0]}:00", "${fecha}T${parts[1]}:00", "Descanso")
        }
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
