package com.example.barberia.network

import com.example.barberia.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── AUTH ──────────────────────────────────────────────────────────────
    // Estos no necesitan token — el backend los tiene en lista blanca

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<LoginResponse>

    // ── BARBEROS ──────────────────────────────────────────────────────────

    @GET("api/barberos")
    suspend fun getBarberos(): Response<List<BarberoDTO>>

    @GET("api/barberos/activos")
    suspend fun getBarberosActivos(): Response<List<BarberoDTO>>

    @GET("api/barberos/mas-solicitados")
    suspend fun getBarberosMasSolicitados(): Response<List<BarberoDTO>>

    @GET("api/barberos/{id}")
    suspend fun getBarberoById(@Path("id") id: Long): Response<BarberoDTO>

    @POST("api/barberos")
    suspend fun createBarbero(@Body dto: BarberoDTO): Response<BarberoDTO>

    @PUT("api/barberos/{id}")
    suspend fun updateBarbero(@Path("id") id: Long, @Body dto: BarberoDTO): Response<BarberoDTO>

    @DELETE("api/barberos/{id}")
    suspend fun deleteBarbero(@Path("id") id: Long): Response<Unit>

    // ── SERVICIOS ─────────────────────────────────────────────────────────

    @GET("api/servicios")
    suspend fun getServicios(): Response<List<ServicioDTO>>

    @GET("api/servicios/{id}")
    suspend fun getServicioById(@Path("id") id: Long): Response<ServicioDTO>

    @POST("api/servicios")
    suspend fun createServicio(@Body dto: ServicioDTO): Response<ServicioDTO>

    @PUT("api/servicios/{id}")
    suspend fun updateServicio(@Path("id") id: Long, @Body dto: ServicioDTO): Response<ServicioDTO>

    @DELETE("api/servicios/{id}")
    suspend fun deleteServicio(@Path("id") id: Long): Response<Unit>

    // ── CITAS ─────────────────────────────────────────────────────────────

    @GET("api/citas")
    suspend fun getCitas(): Response<List<CitaDTO>>

    @GET("api/citas/{id}")
    suspend fun getCitaById(@Path("id") id: Long): Response<CitaDTO>

    @GET("api/citas/usuario/{idUsuario}")
    suspend fun getCitasByUsuario(@Path("idUsuario") idUsuario: Long): Response<List<CitaDTO>>

    @GET("api/citas/barbero/{idBarbero}")
    suspend fun getCitasByBarberoYFecha(
        @Path("idBarbero") idBarbero: Long,
        @Query("fecha") fecha: String
    ): Response<List<CitaDTO>>

    @GET("api/citas/disponibilidad")
    suspend fun getDisponibilidad(
        @Query("idBarbero") idBarbero: Long,
        @Query("fecha") fecha: String
    ): Response<List<String>>

    @POST("api/citas")
    suspend fun createCita(@Body dto: CitaRequest): Response<CitaDTO>

    @PATCH("api/citas/{id}/cancelar")
    suspend fun cancelarCita(@Path("id") id: Long): Response<CitaDTO>

    @PATCH("api/citas/{id}/iniciar")
    suspend fun iniciarCita(@Path("id") id: Long): Response<CitaDTO>

    @PATCH("api/citas/{id}/finalizar")
    suspend fun finalizarCita(@Path("id") id: Long): Response<CitaDTO>

    @PATCH("api/citas/{id}/no-presento")
    suspend fun noPresento(@Path("id") id: Long): Response<CitaDTO>

    // ── HORARIOS ──────────────────────────────────────────────────────────

    @GET("api/horarios/barbero/{idBarbero}")
    suspend fun getHorariosByBarbero(@Path("idBarbero") idBarbero: Long): Response<List<HorarioBarberoDTO>>

    @GET("api/horarios/barbero/{idBarbero}/dia/{dia}")
    suspend fun getHorariosByBarberoYDia(
        @Path("idBarbero") idBarbero: Long,
        @Path("dia") dia: String
    ): Response<List<HorarioBarberoDTO>>

    @POST("api/horarios")
    suspend fun createHorario(@Body dto: HorarioBarberoDTO): Response<HorarioBarberoDTO>

    @PUT("api/horarios/{id}")
    suspend fun updateHorario(@Path("id") id: Long, @Body dto: HorarioBarberoDTO): Response<HorarioBarberoDTO>

    @DELETE("api/horarios/{id}")
    suspend fun deleteHorario(@Path("id") id: Long): Response<Unit>

    // ── BLOQUEOS ──────────────────────────────────────────────────────────

    @GET("api/bloqueos/barbero/{idBarbero}")
    suspend fun getBloqueosByBarbero(@Path("idBarbero") idBarbero: Long): Response<List<BloqueoHorarioDTO>>

    @POST("api/bloqueos")
    suspend fun createBloqueo(@Body dto: BloqueoHorarioDTO): Response<BloqueoHorarioDTO>

    @DELETE("api/bloqueos/{id}")
    suspend fun deleteBloqueo(@Path("id") id: Long): Response<Unit>

    // ── RESEÑAS ───────────────────────────────────────────────────────────

    @GET("api/resenas/barbero/{idBarbero}")
    suspend fun getResenasByBarbero(@Path("idBarbero") idBarbero: Long): Response<List<ResenaDTO>>

    @POST("api/resenas")
    suspend fun createResena(@Body dto: ResenaRequest): Response<ResenaDTO>

    @DELETE("api/resenas/{id}")
    suspend fun deleteResena(@Path("id") id: Long): Response<Unit>

    // ── USUARIOS ──────────────────────────────────────────────────────────

    @GET("api/usuarios")
    suspend fun getUsuarios(): Response<List<UsuarioDTO>>

    @POST("api/usuarios")
    suspend fun createUsuario(@Body dto: UsuarioDTO): Response<UsuarioDTO>

    @GET("api/usuarios/{id}")
    suspend fun getUsuarioById(@Path("id") id: Long): Response<UsuarioDTO>

    @PUT("api/usuarios/{id}")
    suspend fun updateUsuario(@Path("id") id: Long, @Body dto: UsuarioDTO): Response<UsuarioDTO>

    @DELETE("api/usuarios/{id}")
    suspend fun deleteUsuario(@Path("id") id: Long): Response<Unit>

    @GET("api/usuarios/rol/{rol}")
    suspend fun getUsuariosByRol(@Path("rol") rol: String): Response<List<UsuarioDTO>>

    // ── PROMEDIOS ─────────────────────────────────────────────────────────

    @GET("api/promedios/usuario/{idUsuario}/barbero/{idBarbero}")
    suspend fun getPromedioByUsuarioYBarbero(
        @Path("idUsuario") idUsuario: Long,
        @Path("idBarbero") idBarbero: Long
    ): Response<PromedioClienteDTO>

    // ── NOTIFICACIONES ────────────────────────────────────────────────────

    @GET("api/notificaciones/cita/{idCita}")
    suspend fun getNotificacionesByCita(@Path("idCita") idCita: Long): Response<List<NotificacionDTO>>
}