# Documentación del Sistema de Gestión de Barbería

## Aplicación Móvil Android para Administración de Barberías

---

**Autores:** Equipo de Desarrollo

**Institución:** [Nombre de la Institución]

**Fecha:** Mayo 2026

---

## Resumen

El presente documento describe el desarrollo del Sistema de Gestión de Barbería, una aplicación móvil Android desarrollada con Jetpack Compose que permite la administración integral de una barbería. El sistema contempla cuatro roles de usuario: SuperAdministrador, Administrador, Barbero y Cliente, cada uno con funcionalidades específicas adaptadas a sus necesidades. La aplicación se comunica con un backend RESTful desplegado en Render, utilizando autenticación basada en JWT y almacenamiento de sesión mediante DataStore. El documento sigue la metodología RUP (Rational Unified Process) dividido en sus cuatro fases: Inicio, Elaboración, Construcción y Transición, con las disciplinas de Modelado de Negocio, Requisitos, Análisis y Diseño, Implementación, Pruebas y Despliegue. Se emplearon normas APA para la presentación del documento.

*Palabras clave:* barbería, gestión de citas, aplicación Android, Jetpack Compose, RUP, metodología ágil.

---

## Tabla de Contenidos

1. Introducción
2. Fase de Inicio
   2.1. Modelado de Negocio
   2.2. Requisitos del Sistema
   2.3. Alcance y Objetivos
3. Fase de Elaboración
   3.1. Análisis de Requisitos
   3.2. Arquitectura del Sistema
   3.3. Diagramas UML
4. Fase de Construcción
   4.1. Implementación del Frontend
   4.2. Implementación del Backend
   4.3. Módulos del Sistema
5. Fase de Transición
   5.1. Pruebas
   5.2. Despliegue
   5.3. Manual de Usuario
6. Conclusiones
7. Referencias

---

## 1. Introducción

El presente documento tiene como objetivo documentar el proceso de desarrollo del Sistema de Gestión de Barbería, una aplicación móvil Android diseñada para optimizar la administración de barberías, permitiendo la gestión de citas, usuarios, servicios, horarios y reseñas. La aplicación fue desarrollada utilizando Kotlin y Jetpack Compose para el frontend, con un backend RESTful implementado en Spring Boot.

La metodología RUP (Rational Unified Process) fue seleccionada por su enfoque iterativo e incremental, que permite la entrega temprana de funcionalidades y la adaptación continua a los requisitos del negocio. RUP se estructura en cuatro fases (Inicio, Elaboración, Construcción y Transición) y seis disciplinas técnicas (Modelado de Negocio, Requisitos, Análisis y Diseño, Implementación, Pruebas y Despliegue).

---

## 2. Fase de Inicio

### 2.1. Modelado de Negocio

#### 2.1.1. Descripción del Negocio

La barbería requiere un sistema digital que permita gestionar las operaciones diarias, incluyendo la programación de citas, el registro de clientes, la administración de barberos y servicios, y la gestión de horarios. Actualmente, muchos establecimientos manejan estos procesos de forma manual o con herramientas genéricas no especializadas.

#### 2.1.2. Procesos de Negocio Identificados

1. **Registro y autenticación de usuarios**: Clientes, barberos y administradores deben poder registrarse e iniciar sesión en el sistema.
2. **Gestión de citas**: Los clientes pueden agendar, consultar y cancelar citas. Los barberos pueden ver su agenda y actualizar el estado de las citas.
3. **Administración de servicios**: El administrador puede crear, editar y eliminar servicios ofrecidos por la barbería.
4. **Gestión de horarios**: Los barberos pueden definir sus horarios de trabajo y períodos de descanso.
5. **Sistema de reseñas**: Los clientes pueden calificar y comentar sobre los servicios recibidos.
6. **Notificaciones**: El sistema notifica a los usuarios sobre cambios en el estado de sus citas.
7. **Administración de usuarios**: El superadministrador puede gestionar todos los usuarios del sistema.

#### 2.1.3. Actores del Sistema

| Actor | Descripción |
|-------|-------------|
| Cliente | Usuario que agenda citas y deja reseñas |
| Barbero | Profesional que atiende citas y gestiona su horario |
| Administrador | Encargado de gestionar servicios, barberos y horarios |
| SuperAdministrador | Administrador global con control total sobre usuarios y configuraciones |

### 2.2. Requisitos del Sistema

#### 2.2.1. Requisitos Funcionales

**RF-01**: El sistema debe permitir el registro de usuarios con nombre, correo electrónico, teléfono y contraseña.
**RF-02**: El sistema debe permitir el inicio de sesión mediante correo electrónico y contraseña.
**RF-03**: El sistema debe autenticar a los usuarios mediante JWT (JSON Web Token).
**RF-04**: El sistema debe mostrar una pantalla de inicio personalizada según el rol del usuario.
**RF-05**: El cliente debe poder agendar citas seleccionando barbero, servicio, fecha y hora.
**RF-06**: El cliente debe poder ver el historial de sus citas.
**RF-07**: El cliente debe poder cancelar citas pendientes.
**RF-08**: El cliente debe poder dejar reseñas con calificación del 1 al 5 y comentario.
**RF-09**: El barbero debe poder ver sus citas del día.
**RF-10**: El barbero debe poder cambiar el estado de las citas (iniciar, finalizar, cancelar, marcar como no presentado).
**RF-11**: El barbero debe poder gestionar sus horarios de trabajo por día de la semana.
**RF-12**: El barbero debe poder establecer bloques de descanso en su horario.
**RF-13**: El administrador debe poder gestionar los servicios (crear, editar, eliminar).
**RF-14**: El administrador debe poder gestionar los barberos (crear, editar, eliminar, activar/desactivar).
**RF-15**: El administrador debe poder ver todas las citas y filtrarlas por estado.
**RF-16**: El administrador debe poder ver las reseñas de todos los barberos.
**RF-17**: El superadministrador debe poder gestionar todos los usuarios del sistema.
**RF-18**: El sistema debe soportar modo oscuro y modo claro.
**RF-19**: El sistema debe mostrar notificaciones sobre cambios en las citas.
**RF-20**: El sistema debe permitir la actualización del perfil del usuario.

#### 2.2.2. Requisitos No Funcionales

**RNF-01**: La aplicación debe desarrollarse en Kotlin con Jetpack Compose.
**RNF-02**: El backend debe ser una API RESTful.
**RNF-03**: La comunicación debe ser mediante HTTPS.
**RNF-04**: La aplicación debe funcionar en dispositivos con Android 7.0 (API 24) o superior.
**RNF-05**: Los datos de sesión deben persistirse localmente mediante DataStore.
**RNF-06**: La aplicación debe manejar tiempos de espera de hasta 90 segundos debido al cold start del backend en Render.
**RNF-07**: Las contraseñas deben almacenarse de forma segura en el backend.
**RNF-08**: La interfaz debe ser intuitiva y responsiva.

### 2.3. Alcance y Objetivos

#### 2.3.1. Objetivo General

Desarrollar una aplicación móvil Android para la gestión integral de una barbería que permita la administración de citas, usuarios, servicios, horarios y reseñas, mejorando la eficiencia operativa y la experiencia del cliente.

#### 2.3.2. Objetivos Específicos

1. Implementar un sistema de autenticación seguro basado en roles.
2. Desarrollar un módulo de agendamiento de citas con verificación de disponibilidad.
3. Implementar un sistema de gestión de horarios para barberos.
4. Desarrollar un módulo de reseñas y calificaciones.
5. Implementar un panel de administración con métricas y gestión de usuarios.
6. Desplegar el backend en un entorno de producción accesible.

#### 2.3.3. Alcance

El sistema abarca la gestión completa de una barbería, incluyendo el registro de usuarios, agendamiento de citas, administración de servicios y barberos, gestión de horarios, sistema de reseñas y notificaciones. Queda fuera del alcance el procesamiento de pagos en línea y la integración con calendarios externos.

---

## 3. Fase de Elaboración

### 3.1. Análisis de Requisitos

#### 3.1.1. Casos de Uso

| ID | Caso de Uso | Actor | Descripción |
|----|-------------|-------|-------------|
| CU-01 | Registrar usuario | Todos | El usuario se registra en el sistema con sus datos personales |
| CU-02 | Iniciar sesión | Todos | El usuario inicia sesión con sus credenciales |
| CU-03 | Agendar cita | Cliente | El cliente selecciona barbero, servicio, fecha y hora |
| CU-04 | Cancelar cita | Cliente | El cliente cancela una cita pendiente |
| CU-05 | Dejar reseña | Cliente | El cliente califica y comenta un servicio recibido |
| CU-06 | Gestionar agenda | Barbero | El barbero visualiza y actualiza el estado de sus citas |
| CU-07 | Gestionar horario | Barbero | El barbero define sus horarios de trabajo y descanso |
| CU-08 | Gestionar servicios | Administrador | El administrador crea, edita o elimina servicios |
| CU-09 | Gestionar barberos | Administrador | El administrador gestiona los barberos del sistema |
| CU-10 | Ver reseñas | Administrador | El administrador consulta las reseñas de todos los barberos |
| CU-11 | Gestionar usuarios | SuperAdmin | El superadministrador gestiona todos los usuarios |
| CU-12 | Ver estadísticas | SuperAdmin | El superadministrador visualiza estadísticas del sistema |

### 3.2. Arquitectura del Sistema

#### 3.2.1. Arquitectura General

El sistema sigue una arquitectura Cliente-Servidor con los siguientes componentes:

- **Cliente Android**: Aplicación nativa desarrollada en Kotlin con Jetpack Compose.
- **Servidor Backend**: API RESTful desarrollada en Spring Boot, desplegada en Render.
- **Base de Datos**: PostgreSQL gestionada por el backend mediante JPA/Hibernate.
- **Autenticación**: JWT (JSON Web Token) con almacenamiento local en DataStore.

```
[Dispositivo Android] ←→ HTTPS → [API REST (Render)] ←→ [PostgreSQL]
       ↓
[DataStore Local]
```

#### 3.2.2. Arquitectura del Frontend

La aplicación Android sigue una arquitectura MVVM (Model-View-ViewModel):

- **Model**: Clases DTO en `com.example.barberia.model`
- **View**: Composables Jetpack Compose en `com.example.barberia.ui`
- **ViewModel**: Clases ViewModel en `com.example.barberia.viewmodel`
- **Network**: Retrofit + Moshi en `com.example.barberia.network`
- **Session**: DataStore en `com.example.barberia.utils`

```
View (Composable) → ViewModel → ApiService (Retrofit) → Backend
                       ↓
                  UiState (StateFlow)
```

#### 3.2.3. Patrones de Diseño Utilizados

- **MVVM**: Separación de la lógica de negocio de la interfaz de usuario.
- **Repository**: Capa de abstracción para el acceso a datos (AuthRepository).
- **Singleton**: RetrofitClient, SessionManager, TemaManager.
- **Factory Method**: Fábricas estáticas para los ViewModels.
- **StateFlow**: Flujo reactivo de estado para la UI.
- **Observer**: La UI observa los cambios en los StateFlows de los ViewModels.

### 3.3. Diagramas UML

#### 3.3.1. Diagrama de Clases (Modelo de Datos)

```
+----------------+       +----------------+       +----------------+
|   UsuarioDTO   |       |  BarberoDTO    |       |  ServicioDTO   |
+----------------+       +----------------+       +----------------+
| idUsuario      |       | idBarbero      |       | idServicio     |
| nombre         |       | nombre         |       | nombre         |
| correo         |       | especialidad   |       | descripcion    |
| telefono       |       | telefono       |       | precio         |
| password       |       | activo         |       | duracionMinutos|
| rol            |       | idUsuario      |       +----------------+
| activo         |       +----------------+
+----------------+              

+----------------+       +----------------+       +----------------+
|   CitaDTO      |       | HorarioBarbero |       |   ResenaDTO    |
+----------------+       +----------------+       +----------------+
| idCita         |       | idHorario      |       | idResena       |
| idUsuario      |       | idBarbero      |       | idCita         |
| idBarbero      |       | diaSemana      |       | idUsuario      |
| idServicio     |       | horaInicio     |       | idBarbero      |
| fecha          |       | horaFin        |       | calificacion   |
| horaInicio     |       +----------------+       | comentario     |
| horaFin        |                                | fecha          |
| estado         |                                +----------------+
+----------------+
```

#### 3.3.2. Diagrama de Secuencia (Agendamiento de Cita)

```
Cliente        Frontend          ViewModel            Backend         Base Datos
   |               |                 |                    |               |
   |--Agendar----->|                 |                    |               |
   |               |--crearCita----->|                    |               |
   |               |                 |--POST /api/citas-->|               |
   |               |                 |                    |--INSERT------>|
   |               |                 |                    |<--OK----------|
   |               |                 |<--201 Created------|               |
   |               |<--actualizar----|                    |               |
   |<--Confirmar---|                 |                    |               |
```

---

## 4. Fase de Construcción

### 4.1. Implementación del Frontend

#### 4.1.1. Estructura del Proyecto

```
com.example.barberia/
├── MainActivity.kt          # Punto de entrada y navegación
├── model/
│   ├── Models.kt            # DTOs y clases de datos
│   └── Enums.kt             # Enumeraciones del sistema
├── network/
│   ├── ApiService.kt        # Interfaz Retrofit (30+ endpoints)
│   ├── RetrofitClient.kt    # Configuración de Retrofit + Moshi
│   └── AuthRepository.kt    # Repositorio de autenticación
├── utils/
│   └── SessionManager.kt    # Gestión de sesión con DataStore
├── viewmodel/
│   ├── AuthViewModel.kt     # Autenticación
│   ├── AdminViewModel.kt    # Dashboard administrador
│   ├── BarberoViewModel.kt  # Dashboard barbero
│   ├── ClienteViewModel.kt  # Dashboard cliente
│   └── SuperAdminViewModel.kt # Dashboard superadmin
└── ui/
    ├── auth/
    │   ├── LoginScreen.kt   # Pantalla de inicio de sesión
    │   ├── RegisterScreen.kt # Pantalla de registro
    │   └── ClienteScreen.kt # Pantalla principal del cliente
    ├── screens/
    │   ├── AdminScreen.kt   # Panel administrador (6 tabs)
    │   ├── BarberoScreen.kt # Panel barbero (4 tabs)
    │   ├── SuperAdminScreen.kt # Panel superadmin (5 tabs)
    │   ├── InfoBarberoScreen.kt # Detalle de barbero
    │   ├── NotificacionesScreen.kt # Notificaciones
    │   ├── PerfilScreen.kt  # Edición de perfil
    │   └── HistorialResenasScreen.kt # Historial de reseñas
    └── theme/
        ├── Color.kt         # Paleta de colores
        ├── Theme.kt         # Temas oscuro/claro
        └── Type.kt          # Tipografía
```

#### 4.1.2. Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| Kotlin | 2.2.10 | Lenguaje de programación |
| Jetpack Compose | BOM 2026.02.01 | UI declarativa |
| Material 3 | — | Componentes de diseño |
| Retrofit | 2.11.0 | Cliente HTTP |
| Moshi | 1.15.0 | Serialización JSON |
| OkHttp | 4.12.0 | Capa de red |
| Coroutines | 1.10.2 | Programación asíncrona |
| DataStore | 1.1.4 | Almacenamiento local |
| Navigation Compose | 2.9.0 | Navegación entre pantallas |
| ViewModel Compose | 2.9.1 | Gestión de estado |
| Coil | 2.7.0 | Carga de imágenes |

#### 4.1.3. Roles y sus Pantallas

**Cliente:**
- Inicio: Barberos populares, servicios destacados, acceso rápido
- Agendar: Selección de barbero, servicio, fecha y hora disponible
- Mis Citas: Historial de citas con estados y opción de cancelar
- Reseña: Diálogo modal con calificación de 1 a 5 estrellas

**Barbero:**
- Hoy: Citas del día con filtros por estado (pendiente, en curso, finalizado)
- Agenda: Vista semanal con selector de día (lunes a domingo)
- Reseñas: Calificaciones recibidas con promedio y distribución
- Horarios: Gestión de bloques horarios y descansos

**Administrador:**
- Inicio: Tarjetas estadísticas (citas hoy, barberos activos, cancelaciones)
- Citas: Todas las citas con filtro por estado
- Usuarios: Sub-tabs Clientes/Barberos con CRUD completo
- Servicios: CRUD de servicios con historial de citas
- Horarios: Gestión de horarios por barbero
- Reseñas: Promedio general, promedio por barbero, reseñas individuales

**SuperAdministrador:**
- Inicio: Estadísticas globales del sistema
- Usuarios: CRUD completo con roles y estados
- Barberos: Gestión de barberos vinculados a usuarios
- Servicios: CRUD de servicios
- Citas: Vista global de todas las citas agrupadas por fecha

### 4.2. Implementación del Backend

El backend es una API RESTful desplegada en Render (URL: `https://backendbarberia-ga9f.onrender.com/`). Está desarrollada con Spring Boot y JPA/Hibernate, utilizando PostgreSQL como base de datos.

#### 4.2.1. Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Inicio de sesión |
| POST | `/api/auth/register` | Registro de usuario |
| GET | `/api/auth/me` | Obtener usuario actual |
| GET | `/api/barberos` | Listar barberos |
| POST | `/api/barberos` | Crear barbero |
| GET | `/api/servicios` | Listar servicios |
| POST | `/api/servicios` | Crear servicio |
| GET | `/api/citas` | Listar citas |
| POST | `/api/citas` | Crear cita |
| PATCH | `/api/citas/{id}/cancelar` | Cancelar cita |
| GET | `/api/horarios/barbero/{id}` | Horarios de barbero |
| POST | `/api/horarios` | Crear horario |
| GET | `/api/resenas/barbero/{id}` | Reseñas de barbero |
| POST | `/api/resenas` | Crear reseña |

### 4.3. Módulos del Sistema

#### 4.3.1. Módulo de Autenticación

El módulo de autenticación maneja el registro e inicio de sesión de usuarios. Utiliza JWT para la autenticación sin estado (stateless). El token se almacena localmente en DataStore y se envía en el encabezado `Authorization: Bearer <token>` en cada solicitud.

El flujo de autenticación incluye:
1. Validación de campos en el frontend
2. Envío de credenciales al backend
3. Recepción del token JWT y datos del usuario
4. Almacenamiento de sesión en DataStore
5. Redirección a la pantalla principal según el rol

#### 4.3.2. Módulo de Gestión de Citas

El módulo de citas permite a los clientes agendar citas verificando la disponibilidad del barbero en tiempo real. Los barberos pueden actualizar el estado de las citas a través de un flujo de trabajo: Pendiente → En Curso → Finalizada, o alternativamente Cancelada o No Presentado.

#### 4.3.3. Módulo de Horarios

Los barberos pueden definir su horario semanal con bloques de 30 minutos. El sistema permite establecer períodos de descanso dentro del horario laboral. La disponibilidad se calcula dinámicamente basándose en los horarios definidos y las citas existentes.

#### 4.3.4. Módulo de Reseñas

Los clientes pueden calificar los servicios recibidos en una escala del 1 al 5, con comentario opcional. El sistema calcula automáticamente el promedio de calificaciones por barbero y global. Los administradores pueden visualizar todas las reseñas y eliminarlas si es necesario.

#### 4.3.5. Módulo de Notificaciones

El sistema genera notificaciones automáticas cuando se crea o cancela una cita. Las notificaciones se muestran en una pantalla dedicada y se indican mediante un badge en el ícono de notificaciones.

#### 4.3.6. Módulo de Temas

La aplicación soporta tres modos de visualización: claro, oscuro y seguimiento del sistema. Cada rol tiene una paleta de colores distintiva: rojo para cliente, azul para administrador/barbero, y dorado para superadministrador.

---

## 5. Fase de Transición

### 5.1. Pruebas

#### 5.1.1. Pruebas Unitarias

Se realizaron pruebas unitarias en los ViewModels para verificar la lógica de negocio:
- Validación de campos en el formulario de registro
- Cálculo de promedios de calificaciones
- Filtrado de citas por estado y fecha
- Transformación de datos a formatos de visualización

#### 5.1.2. Pruebas de Integración

Se verificaron las siguientes integraciones:
- Comunicación correcta con el backend mediante Retrofit
- Serialización/deserialización correcta con Moshi
- Almacenamiento y recuperación de sesión en DataStore
- Navegación entre pantallas con argumentos

#### 5.1.3. Pruebas de Interfaz de Usuario

Se verificó el correcto funcionamiento de:
- Navegación por tabs en cada rol
- Pull-to-refresh en todas las pantallas
- Diálogos de confirmación y formularios
- Menú lateral de navegación (ModalNavigationDrawer)
- BottomSheets para selección y detalle

#### 5.1.4. Casos de Error Manejados

- Error 409 (conflicto) al registrar correo o teléfono duplicado
- Error 400 (solicitud inválida) con mensajes descriptivos
- Timeout por cold start del backend en Render (hasta 90 segundos)
- Token expirado (redirección a pantalla de login)
- Estado vacío en listas y búsquedas

### 5.2. Despliegue

#### 5.2.1. Backend

El backend se despliega en Render (https://render.com) como un servicio web Java. Utiliza el plan gratuito que incluye:
- Despliegue automático desde GitHub
- SSL/TLS automático
- Cold start en el primer acceso tras inactividad

#### 5.2.2. Frontend

La aplicación Android se distribuye como APK firmado. Los requisitos mínimos son:
- Android 7.0 (API 24) o superior
- Conexión a internet
- 50 MB de espacio libre

#### 5.2.3. Configuración de Conexión

La URL del backend se configura en `RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "https://backendbarberia-ga9f.onrender.com/"
```

### 5.3. Manual de Usuario

#### 5.3.1. Pantalla de Inicio de Sesión

La aplicación inicia en la pantalla de inicio de sesión. El usuario debe ingresar su correo electrónico y contraseña. Existe un enlace a la pantalla de registro para nuevos usuarios. Se incluye un selector de tema (claro/oscuro/sistema) y una animación del logo.

#### 5.3.2. Pantalla de Registro

El formulario de registro solicita: nombre completo, correo electrónico, contraseña (con indicador de fortaleza), confirmación de contraseña y teléfono (opcional). Incluye una barra de progreso animada de tres pasos.

#### 5.3.3. Navegación General

Cada rol tiene una pantalla principal con pestañas horizontales y un menú lateral (hamburguesa) que permite acceder al perfil, cambiar el tema y cerrar sesión. La barra inferior muestra las pestañas disponibles para cada rol.

#### 5.3.4. Funcionalidades por Rol

**Cliente:**
- **Inicio**: Ver barberos populares, servicios destacados y accesos rápidos para agendar cita.
- **Agendar**: Seleccionar barbero, servicio, fecha y hora disponible. Confirmar la cita.
- **Mis Citas**: Ver historial de citas con estado. Cancelar citas pendientes. Dejar reseña al finalizar.

**Barbero:**
- **Hoy**: Ver citas del día con filtros. Iniciar, finalizar o cancelar citas.
- **Agenda**: Navegar por día de la semana. Ver detalle de cada cita.
- **Reseñas**: Ver calificaciones recibidas y comentarios de clientes.
- **Horarios**: Definir horario semanal y establecer descansos.

**Administrador:**
- **Inicio**: Estadísticas rápidas (citas hoy, barberos activos, cancelaciones).
- **Citas**: Ver y filtrar todas las citas del sistema.
- **Usuarios**: Gestionar clientes y barberos (crear, editar, eliminar).
- **Servicios**: Gestionar servicios ofrecidos (crear, editar, eliminar, ver historial).
- **Horarios**: Gestionar horarios de todos los barberos.
- **Reseñas**: Ver promedio general y por barbero, revisar reseñas individuales.

**SuperAdministrador:**
- **Inicio**: Métricas globales del sistema.
- **Usuarios**: CRUD completo de usuarios con asignación de roles.
- **Barberos**: Gestión completa de barberos.
- **Servicios**: CRUD de servicios.
- **Citas**: Vista global de citas agrupadas por fecha.

---

## 6. Conclusiones

El desarrollo del Sistema de Gestión de Barbería bajo la metodología RUP permitió abordar de manera estructurada las necesidades de digitalización de una barbería, entregando una solución completa que abarca desde el registro de usuarios hasta la generación de reportes estadísticos.

La implementación con Jetpack Compose y Kotlin proporcionó una base moderna y reactiva para la interfaz de usuario, mientras que la arquitectura MVVM facilitó la separación de responsabilidades y la mantenibilidad del código.

La división por roles (Cliente, Barbero, Administrador, SuperAdministrador) permitió adaptar las funcionalidades a las necesidades específicas de cada tipo de usuario, mejorando la experiencia de uso y la eficiencia operativa.

La integración con un backend RESTful desplegado en Render demostró la viabilidad de una arquitectura cliente-servidor para aplicaciones móviles de gestión empresarial, con manejo adecuado de tiempos de espera y errores de red.

---

## 7. Referencias

Kruchten, P. (2000). *The Rational Unified Process: An Introduction* (2nd ed.). Addison-Wesley Professional.

Jacobson, I., Booch, G., & Rumbaugh, J. (1999). *The Unified Software Development Process*. Addison-Wesley.

Google. (2024). *Jetpack Compose Documentation*. Android Developers. https://developer.android.com/jetpack/compose/documentation

Square. (2024). *Retrofit Documentation*. Square Open Source. https://square.github.io/retrofit/

Square. (2024). *Moshi Documentation*. Square Open Source. https://github.com/square/moshi

American Psychological Association. (2020). *Publication Manual of the American Psychological Association* (7th ed.). https://doi.org/10.1037/0000165-000

Render. (2024). *Render Documentation*. Render. https://render.com/docs

Spring. (2024). *Spring Boot Reference Documentation*. Spring. https://docs.spring.io/spring-boot/docs/current/reference/html/
