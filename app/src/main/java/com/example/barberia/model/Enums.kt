package com.example.barberia.model

enum class RolEnum {
    SUPERADMIN, ADMINISTRADOR, BARBERO, CLIENTE
}

enum class EstadoCitaEnum {
    PENDIENTE, EN_CURSO, FINALIZADA, CANCELADA, NO_PRESENTADO
}

enum class DiaSemanaEnum {
    LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO
}

enum class TipoNotificacionEnum {
    CITA_AGENDADA, CITA_CANCELADA
}
