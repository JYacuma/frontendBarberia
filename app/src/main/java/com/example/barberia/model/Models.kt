package com.example.barberia.model

import java.text.Normalizer

fun getInitials(name: String): String {
    return name.split(" ").take(2).joinToString("") { it.take(1).uppercase() }
}

// ── AUTH ───────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val type: String,
    val idUsuario: Long,
    val nombre: String,
    val correo: String,
    val rol: RolEnum
)

data class RegisterRequest(
    val nombre: String,
    val correoOTelefono: String,
    val password: String,
    val telefono: String? = null
)

data class RegisterResponse(
    val idUsuario: Long,
    val nombre: String,
    val correo: String,
    val rol: RolEnum,
    val fechaRegistro: String
)

// ── USUARIO ────────────────────────────────────────────────────────────────

data class UsuarioDTO(
    val idUsuario: Long? = null,
    val nombre: String,
    val correo: String,
    val telefono: String? = null,
    val password: String? = null,  // SE AGREGO ESTA LÍNEA
    val rol: RolEnum,
    val activo: Boolean? = true
)
// ── BARBERO ────────────────────────────────────────────────────────────────

data class BarberoDTO(
    val idBarbero: Long? = null,
    val nombre: String,
    val especialidad: String? = null,
    val telefono: String? = null,
    val activo: Boolean? = true,
    val idUsuario: Long? = null
)

// ── SERVICIO ───────────────────────────────────────────────────────────────

data class ServicioDTO(
    val idServicio: Long? = null,
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,
    val duracionMinutos: Int
)

// ── CITA ───────────────────────────────────────────────────────────────────

data class CitaRequest(
    val idUsuario: Long,
    val idBarbero: Long,
    val idServicio: Long,
    val fecha: String,
    val horaInicio: String
)

data class CitaDTO(
    val idCita: Long? = null,
    val idUsuario: Long,
    val idBarbero: Long,
    val idServicio: Long,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String? = null,
    val estado: EstadoCitaEnum? = EstadoCitaEnum.PENDIENTE
)

// ── HORARIO ────────────────────────────────────────────────────────────────

data class HorarioBarberoDTO(
    val idHorario: Long? = null,
    val idBarbero: Long,
    val diaSemana: DiaSemanaEnum,
    val horaInicio: String,
    val horaFin: String
)

// ── BLOQUEO ────────────────────────────────────────────────────────────────

data class BloqueoHorarioDTO(
    val idBloqueo: Long? = null,
    val idBarbero: Long,
    val fechaInicio: String,
    val fechaFin: String,
    val motivo: String? = null
)

// ── RESEÑA ─────────────────────────────────────────────────────────────────

data class ResenaRequest(
    val idCita: Long,
    val idUsuario: Long,
    val idBarbero: Long,
    val calificacion: Int,
    val comentario: String? = null,
    val fecha: String? = null
)

data class ResenaDTO(
    val idResena: Long? = null,
    val idCita: Long,
    val idUsuario: Long,
    val idBarbero: Long,
    val calificacion: Int,
    val comentario: String? = null,
    val fecha: String? = null
)

// ── PROMEDIO ───────────────────────────────────────────────────────────────

data class PromedioClienteDTO(
    val idPromedio: Long? = null,
    val idUsuario: Long,
    val idBarbero: Long,
    val duracionPromedioMinutos: Double,
    val totalCitas: Int
)

// ── ERROR ──────────────────────────────────────────────────────────────────

data class ErrorResponse(
    val status: Int,
    val error: String,
    val mensaje: String
)

// ── NOTIFICACIÓN ──────────────────────────────────────────────────────────

data class NotificacionDTO(
    val idNotificacion: Long? = null,
    val idCita: Long,
    val tipo: TipoNotificacionEnum,
    val mensaje: String? = null,
    val enviado: Boolean? = false
)

// ── DETALLE COMPUESTO ──────────────────────────────────────────────────────

data class CitaConDetalle(
    val cita: CitaDTO,
    val clienteNombre: String = "",
    val barberoNombre: String = "",
    val servicioNombre: String = "",
    val precio: Double = 0.0
)

data class ServicioConDetalle(
    val servicio: ServicioDTO,
    val barberos: List<String> = emptyList(),
    val clientes: List<String> = emptyList(),
    val barberoDTOs: List<BarberoDTO> = emptyList(),
    val clienteDTOs: List<UsuarioDTO> = emptyList()
)