# Generar documento Word - Sistema de Gestión de Barbería (RUP + APA)
param(
    [string]$outputPath = "$env:USERPROFILE\Downloads\Documentacion_RUP_Barberia.docx"
)

Write-Host "Generando documento Word en: $outputPath" -ForegroundColor Green

try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $doc = $word.Documents.Add()
    $sel = $word.Selection

    # ── Configuración APA ──
    # Márgenes 1 pulgada (2.54 cm)
    $doc.PageSetup.TopMargin    = [float]72  # 1 inch in points
    $doc.PageSetup.BottomMargin = [float]72
    $doc.PageSetup.LeftMargin   = [float]72
    $doc.PageSetup.RightMargin  = [float]72

    # Interlineado doble
    $sel.ParagraphFormat.LineSpacingRule = 4  # wdLineSpaceDouble
    $sel.ParagraphFormat.SpaceBefore = 0
    $sel.ParagraphFormat.SpaceAfter = 0

    # Fuente Times New Roman 12pt
    $sel.Font.Name = "Times New Roman"
    $sel.Font.Size = 12

    # ── FUNCIONES AYUDA ──
    function Add-ApaTitlePage {
        param($doc, $sel)
        # Centrar verticalmente
        $doc.PageSetup.VerticalAlignment = 1  # wdAlignVerticalCenter

        $sel.ParagraphFormat.Alignment = 1  # wdAlignParagraphCenter

        # Título (negrita, 12pt)
        $sel.Font.Bold = $true
        $sel.Font.Size = 12
        $sel.TypeText("Sistema de Gestión de Barbería")
        $sel.TypeParagraph()
        $sel.TypeText("Aplicación Móvil Android para Administración de Barberías")
        $sel.TypeParagraph()
        $sel.TypeParagraph()

        $sel.Font.Bold = $false
        $sel.Font.Size = 12
        $sel.TypeText("[Nombre del Autor]")
        $sel.TypeParagraph()
        $sel.TypeText("[Nombre de la Institución]")
        $sel.TypeParagraph()
        $sel.TypeText("Mayo 2026")
        $sel.TypeParagraph()

        # Restaurar alineación
        $sel.ParagraphFormat.Alignment = 3  # wdAlignParagraphJustify
        $doc.PageSetup.VerticalAlignment = 0  # wdAlignVerticalTop

        # Salto de página
        $sel.InsertBreak(7)  # wdPageBreak
    }

    function Add-Heading {
        param($sel, [string]$text, [int]$level)
        $sel.Font.Bold = $true
        $sel.Font.Size = 12
        if ($level -eq 1) {
            $sel.ParagraphFormat.Alignment = 1  # Centrado
        } else {
            $sel.ParagraphFormat.Alignment = 3  # Justificado
        }
        $sel.TypeText($text)
        $sel.TypeParagraph()
        $sel.Font.Bold = $false
        $sel.Font.Size = 12
        $sel.ParagraphFormat.Alignment = 3
    }

    function Add-Paragraph {
        param($sel, [string]$text)
        $sel.Font.Bold = $false
        $sel.Font.Size = 12
        $sel.TypeText($text)
        $sel.TypeParagraph()
    }

    function Add-Bullet {
        param($sel, [string]$text, [int]$indent = 0)
        $sel.Font.Bold = $false
        $sel.Font.Size = 12
        $listFormat = $sel.Range.ListFormat
        $listFormat.ApplyBulletDefault()
        if ($indent -gt 0) {
            $listFormat.ListIndent()
            $listFormat.ListIndent()
        }
        $sel.TypeText($text)
        $sel.TypeParagraph()
        $listFormat.RemoveNumbers(3)  # wdNumberParagraph
    }

    # ── PORTADA ──
    Add-ApaTitlePage -doc $doc -sel $sel

    # ── TABLA DE CONTENIDOS ──
    Add-Heading -sel $sel -text "Tabla de Contenidos" -level 1
    Add-Paragraph -sel $sel -text "(Inserte aquí la tabla de contenidos automática en Word: Referencias > Tabla de contenido)"
    $sel.InsertBreak(7)

    # ── RESUMEN ──
    Add-Heading -sel $sel -text "Resumen" -level 1
    Add-Paragraph -sel $sel -text "El presente documento describe el desarrollo del Sistema de Gestión de Barbería, una aplicación móvil Android desarrollada con Jetpack Compose que permite la administración integral de una barbería. El sistema contempla cuatro roles de usuario: SuperAdministrador, Administrador, Barbero y Cliente, cada uno con funcionalidades específicas adaptadas a sus necesidades. La aplicación se comunica con un backend RESTful desplegado en Render, utilizando autenticación basada en JWT y almacenamiento de sesión mediante DataStore. El documento sigue la metodología RUP (Rational Unified Process) dividido en sus cuatro fases: Inicio, Elaboración, Construcción y Transición, con las disciplinas de Modelado de Negocio, Requisitos, Análisis y Diseño, Implementación, Pruebas y Despliegue. Se emplearon normas APA para la presentación del documento."
    Add-Paragraph -sel $sel -text ""
    $sel.Font.Italic = $true
    Add-Paragraph -sel $sel -text "Palabras clave: barbería, gestión de citas, aplicación Android, Jetpack Compose, RUP, metodología ágil."
    $sel.Font.Italic = $false
    $sel.InsertBreak(7)

    # ── 1. INTRODUCCIÓN ──
    Add-Heading -sel $sel -text "1. Introducción" -level 1
    Add-Paragraph -sel $sel -text "El presente documento tiene como objetivo documentar el proceso de desarrollo del Sistema de Gestión de Barbería, una aplicación móvil Android diseñada para optimizar la administración de barberías, permitiendo la gestión de citas, usuarios, servicios, horarios y reseñas. La aplicación fue desarrollada utilizando Kotlin y Jetpack Compose para el frontend, con un backend RESTful implementado en Spring Boot."
    Add-Paragraph -sel $sel -text "La metodología RUP (Rational Unified Process) fue seleccionada por su enfoque iterativo e incremental, que permite la entrega temprana de funcionalidades y la adaptación continua a los requisitos del negocio. RUP se estructura en cuatro fases (Inicio, Elaboración, Construcción y Transición) y seis disciplinas técnicas (Modelado de Negocio, Requisitos, Análisis y Diseño, Implementación, Pruebas y Despliegue)."

    # ── 2. FASE DE INICIO ──
    Add-Heading -sel $sel -text "2. Fase de Inicio" -level 1

    # 2.1 Modelado de Negocio
    Add-Heading -sel $sel -text "2.1. Modelado de Negocio" -level 2
    Add-Heading -sel $sel -text "2.1.1. Descripción del Negocio" -level 3
    Add-Paragraph -sel $sel -text "La barbería requiere un sistema digital que permita gestionar las operaciones diarias, incluyendo la programación de citas, el registro de clientes, la administración de barberos y servicios, y la gestión de horarios. Actualmente, muchos establecimientos manejan estos procesos de forma manual o con herramientas genéricas no especializadas."

    Add-Heading -sel $sel -text "2.1.2. Procesos de Negocio Identificados" -level 3
    Add-Paragraph -sel $sel -text "1. Registro y autenticación de usuarios: Clientes, barberos y administradores deben poder registrarse e iniciar sesión en el sistema."
    Add-Paragraph -sel $sel -text "2. Gestión de citas: Los clientes pueden agendar, consultar y cancelar citas. Los barberos pueden ver su agenda y actualizar el estado de las citas."
    Add-Paragraph -sel $sel -text "3. Administración de servicios: El administrador puede crear, editar y eliminar servicios ofrecidos por la barbería."
    Add-Paragraph -sel $sel -text "4. Gestión de horarios: Los barberos pueden definir sus horarios de trabajo y períodos de descanso."
    Add-Paragraph -sel $sel -text "5. Sistema de reseñas: Los clientes pueden calificar y comentar sobre los servicios recibidos."
    Add-Paragraph -sel $sel -text "6. Notificaciones: El sistema notifica a los usuarios sobre cambios en el estado de sus citas."
    Add-Paragraph -sel $sel -text "7. Administración de usuarios: El superadministrador puede gestionar todos los usuarios del sistema."

    Add-Heading -sel $sel -text "2.1.3. Actores del Sistema" -level 3
    Add-Paragraph -sel $sel -text "Cliente: Usuario que agenda citas y deja reseñas."
    Add-Paragraph -sel $sel -text "Barbero: Profesional que atiende citas y gestiona su horario."
    Add-Paragraph -sel $sel -text "Administrador: Encargado de gestionar servicios, barberos y horarios."
    Add-Paragraph -sel $sel -text "SuperAdministrador: Administrador global con control total sobre usuarios y configuraciones."

    # 2.2 Requisitos
    Add-Heading -sel $sel -text "2.2. Requisitos del Sistema" -level 2
    Add-Heading -sel $sel -text "2.2.1. Requisitos Funcionales" -level 3
    Add-Paragraph -sel $sel -text "RF-01: El sistema debe permitir el registro de usuarios con nombre, correo electrónico, teléfono y contraseña."
    Add-Paragraph -sel $sel -text "RF-02: El sistema debe permitir el inicio de sesión mediante correo electrónico y contraseña."
    Add-Paragraph -sel $sel -text "RF-03: El sistema debe autenticar a los usuarios mediante JWT."
    Add-Paragraph -sel $sel -text "RF-04: El sistema debe mostrar una pantalla de inicio personalizada según el rol del usuario."
    Add-Paragraph -sel $sel -text "RF-05: El cliente debe poder agendar citas seleccionando barbero, servicio, fecha y hora."
    Add-Paragraph -sel $sel -text "RF-06: El cliente debe poder ver el historial de sus citas."
    Add-Paragraph -sel $sel -text "RF-07: El cliente debe poder cancelar citas pendientes."
    Add-Paragraph -sel $sel -text "RF-08: El cliente debe poder dejar reseñas con calificación del 1 al 5 y comentario."
    Add-Paragraph -sel $sel -text "RF-09: El barbero debe poder ver sus citas del día."
    Add-Paragraph -sel $sel -text "RF-10: El barbero debe poder cambiar el estado de las citas."
    Add-Paragraph -sel $sel -text "RF-11: El barbero debe poder gestionar sus horarios de trabajo por día de la semana."
    Add-Paragraph -sel $sel -text "RF-12: El barbero debe poder establecer bloques de descanso en su horario."
    Add-Paragraph -sel $sel -text "RF-13: El administrador debe poder gestionar los servicios."
    Add-Paragraph -sel $sel -text "RF-14: El administrador debe poder gestionar los barberos."
    Add-Paragraph -sel $sel -text "RF-15: El administrador debe poder ver todas las citas y filtrarlas por estado."
    Add-Paragraph -sel $sel -text "RF-16: El administrador debe poder ver las reseñas de todos los barberos."
    Add-Paragraph -sel $sel -text "RF-17: El superadministrador debe poder gestionar todos los usuarios del sistema."
    Add-Paragraph -sel $sel -text "RF-18: El sistema debe soportar modo oscuro y modo claro."
    Add-Paragraph -sel $sel -text "RF-19: El sistema debe mostrar notificaciones sobre cambios en las citas."
    Add-Paragraph -sel $sel -text "RF-20: El sistema debe permitir la actualización del perfil del usuario."

    Add-Heading -sel $sel -text "2.2.2. Requisitos No Funcionales" -level 3
    Add-Paragraph -sel $sel -text "RNF-01: La aplicación debe desarrollarse en Kotlin con Jetpack Compose. RNF-02: El backend debe ser una API RESTful. RNF-03: La comunicación debe ser mediante HTTPS. RNF-04: La aplicación debe funcionar en dispositivos con Android 7.0 o superior. RNF-05: Los datos de sesión deben persistirse localmente mediante DataStore. RNF-06: La aplicación debe manejar tiempos de espera de hasta 90 segundos. RNF-07: Las contraseñas deben almacenarse de forma segura en el backend. RNF-08: La interfaz debe ser intuitiva y responsiva."

    # 2.3 Alcance
    Add-Heading -sel $sel -text "2.3. Alcance y Objetivos" -level 2
    Add-Heading -sel $sel -text "2.3.1. Objetivo General" -level 3
    Add-Paragraph -sel $sel -text "Desarrollar una aplicación móvil Android para la gestión integral de una barbería que permita la administración de citas, usuarios, servicios, horarios y reseñas, mejorando la eficiencia operativa y la experiencia del cliente."

    Add-Heading -sel $sel -text "2.3.2. Objetivos Específicos" -level 3
    Add-Paragraph -sel $sel -text "1. Implementar un sistema de autenticación seguro basado en roles. 2. Desarrollar un módulo de agendamiento de citas con verificación de disponibilidad. 3. Implementar un sistema de gestión de horarios para barberos. 4. Desarrollar un módulo de reseñas y calificaciones. 5. Implementar un panel de administración con métricas y gestión de usuarios. 6. Desplegar el backend en un entorno de producción accesible."

    # ── 3. FASE DE ELABORACIÓN ──
    Add-Heading -sel $sel -text "3. Fase de Elaboración" -level 1

    Add-Heading -sel $sel -text "3.1. Análisis de Requisitos" -level 2
    Add-Paragraph -sel $sel -text "A continuación se presentan los casos de uso identificados para el sistema:"
    Add-Paragraph -sel $sel -text "CU-01: Registrar usuario (Actor: Todos). El usuario se registra en el sistema con sus datos personales."
    Add-Paragraph -sel $sel -text "CU-02: Iniciar sesión (Actor: Todos). El usuario inicia sesión con sus credenciales."
    Add-Paragraph -sel $sel -text "CU-03: Agendar cita (Actor: Cliente). El cliente selecciona barbero, servicio, fecha y hora."
    Add-Paragraph -sel $sel -text "CU-04: Cancelar cita (Actor: Cliente). El cliente cancela una cita pendiente."
    Add-Paragraph -sel $sel -text "CU-05: Dejar reseña (Actor: Cliente). El cliente califica y comenta un servicio recibido."
    Add-Paragraph -sel $sel -text "CU-06: Gestionar agenda (Actor: Barbero). El barbero visualiza y actualiza el estado de sus citas."
    Add-Paragraph -sel $sel -text "CU-07: Gestionar horario (Actor: Barbero). El barbero define sus horarios de trabajo y descanso."
    Add-Paragraph -sel $sel -text "CU-08: Gestionar servicios (Actor: Administrador). El administrador crea, edita o elimina servicios."
    Add-Paragraph -sel $sel -text "CU-09: Gestionar barberos (Actor: Administrador). El administrador gestiona los barberos del sistema."
    Add-Paragraph -sel $sel -text "CU-10: Ver reseñas (Actor: Administrador). El administrador consulta las reseñas de todos los barberos."
    Add-Paragraph -sel $sel -text "CU-11: Gestionar usuarios (Actor: SuperAdmin). El superadministrador gestiona todos los usuarios."
    Add-Paragraph -sel $sel -text "CU-12: Ver estadísticas (Actor: SuperAdmin). El superadministrador visualiza estadísticas del sistema."

    # 3.2 Arquitectura
    Add-Heading -sel $sel -text "3.2. Arquitectura del Sistema" -level 2
    Add-Heading -sel $sel -text "3.2.1. Arquitectura General" -level 3
    Add-Paragraph -sel $sel -text "El sistema sigue una arquitectura Cliente-Servidor. El cliente es una aplicación Android nativa desarrollada en Kotlin con Jetpack Compose. El servidor es una API RESTful desarrollada en Spring Boot desplegada en Render. La base de datos es PostgreSQL gestionada mediante JPA/Hibernate. La autenticación utiliza JWT con almacenamiento local en DataStore."

    Add-Heading -sel $sel -text "3.2.2. Arquitectura del Frontend" -level 3
    Add-Paragraph -sel $sel -text "La aplicación Android sigue una arquitectura MVVM (Model-View-ViewModel). Los modelos (Model) son clases DTO en el paquete model. Las vistas (View) son composables Jetpack Compose en el paquete ui. Los ViewModels gestionan el estado en el paquete viewmodel. La capa de red utiliza Retrofit con Moshi en el paquete network. La sesión se gestiona con DataStore en el paquete utils."

    Add-Heading -sel $sel -text "3.2.3. Patrones de Diseño" -level 3
    Add-Paragraph -sel $sel -text "MVVM: Separación de la lógica de negocio de la interfaz de usuario. Repository: Capa de abstracción para el acceso a datos (AuthRepository). Singleton: RetrofitClient, SessionManager, TemaManager. Factory Method: Fábricas estáticas para los ViewModels. StateFlow: Flujo reactivo de estado para la UI. Observer: La UI observa los cambios en los StateFlows de los ViewModels."

    # ── 4. FASE DE CONSTRUCCIÓN ──
    Add-Heading -sel $sel -text "4. Fase de Construcción" -level 1

    Add-Heading -sel $sel -text "4.1. Implementación del Frontend" -level 2
    Add-Paragraph -sel $sel -text "La aplicación se desarrolló utilizando Kotlin 2.2.10 con Jetpack Compose (BOM 2026.02.01) y Material 3. La comunicación HTTP se realiza mediante Retrofit 2.11.0 con Moshi 1.15.0 como serializador JSON, sobre OkHttp 4.12.0. La programación asíncrona utiliza Coroutines 1.10.2. El almacenamiento local de sesión se implementó con DataStore Preferences 1.1.4. La navegación entre pantallas utiliza Navigation Compose 2.9.0 y la gestión de estado emplea ViewModel Compose 2.9.1."

    Add-Heading -sel $sel -text "4.1.1. Estructura del Proyecto" -level 3
    Add-Paragraph -sel $sel -text "El proyecto se organiza en los siguientes paquetes: model (Modelos de datos y DTOs), network (ApiService, RetrofitClient, AuthRepository), utils (SessionManager), viewmodel (AuthViewModel, AdminViewModel, BarberoViewModel, ClienteViewModel, SuperAdminViewModel), ui/auth (LoginScreen, RegisterScreen, ClienteScreen), ui/screens (AdminScreen, BarberoScreen, SuperAdminScreen, InfoBarberoScreen, NotificacionesScreen, PerfilScreen, HistorialResenasScreen) y ui/theme (Color, Theme, Type)."

    Add-Heading -sel $sel -text "4.1.2. Roles y Pantallas" -level 3
    Add-Paragraph -sel $sel -text "El sistema cuenta con cuatro roles: Cliente (pantallas: Inicio, Agendar, Mis Citas), Barbero (Hoy, Agenda, Reseñas, Horarios), Administrador (Inicio, Citas, Usuarios, Servicios, Horarios, Reseñas) y SuperAdministrador (Inicio, Usuarios, Barberos, Servicios, Citas). Cada rol tiene una barra de navegación inferior con sus respectivas pestañas y un menú lateral con acceso a perfil, cambio de tema y cierre de sesión."

    Add-Heading -sel $sel -text "4.2. Módulos del Sistema" -level 2
    Add-Paragraph -sel $sel -text "Módulo de Autenticación: Maneja el registro e inicio de sesión con JWT. El token se almacena en DataStore y se envía en cada solicitud HTTP."
    Add-Paragraph -sel $sel -text "Módulo de Gestión de Citas: Permite agendar, consultar y cancelar citas con verificación de disponibilidad en tiempo real."
    Add-Paragraph -sel $sel -text "Módulo de Horarios: Los barberos definen su horario semanal con bloques de 30 minutos y períodos de descanso."
    Add-Paragraph -sel $sel -text "Módulo de Reseñas: Los clientes califican del 1 al 5 con comentario. El sistema calcula promedios por barbero y global."
    Add-Paragraph -sel $sel -text "Módulo de Notificaciones: Genera notificaciones automáticas al crear o cancelar citas."
    Add-Paragraph -sel $sel -text "Módulo de Temas: Soporta modo claro, oscuro y seguimiento del sistema. Cada rol tiene colores distintivos."

    Add-Heading -sel $sel -text "4.3. Backend" -level 2
    Add-Paragraph -sel $sel -text "El backend es una API RESTful desplegada en Render (URL: https://backendbarberia-ga9f.onrender.com/). Está desarrollada con Spring Boot y JPA/Hibernate, utilizando PostgreSQL como base de datos. Incluye endpoints para autenticación, gestión de usuarios, barberos, servicios, citas, horarios, reseñas, bloqueos y notificaciones."

    # ── 5. FASE DE TRANSICIÓN ──
    Add-Heading -sel $sel -text "5. Fase de Transición" -level 1

    Add-Heading -sel $sel -text "5.1. Pruebas" -level 2
    Add-Paragraph -sel $sel -text "Se realizaron pruebas unitarias en los ViewModels para verificar la lógica de negocio: validación de campos, cálculo de promedios, filtrado de citas y transformación de datos. Las pruebas de integración verificaron la comunicación con el backend mediante Retrofit, la serialización con Moshi y el almacenamiento en DataStore. Las pruebas de interfaz de usuario verificaron la navegación por tabs, pull-to-refresh, diálogos de confirmación y menú lateral."

    Add-Heading -sel $sel -text "5.2. Despliegue" -level 2
    Add-Paragraph -sel $sel -text "El backend se despliega en Render como servicio web Java con despliegue automático desde GitHub y SSL/TLS automático. La aplicación Android se distribuye como APK firmado con requisitos mínimos de Android 7.0 (API 24), conexión a internet y 50 MB de espacio libre."

    Add-Heading -sel $sel -text "5.3. Manejo de Errores" -level 2
    Add-Paragraph -sel $sel -text "El sistema maneja los siguientes casos de error: Error 409 (conflicto) al registrar correo o teléfono duplicado con mensaje descriptivo. Error 400 (solicitud inválida). Timeout por cold start del backend en Render (hasta 90 segundos de espera). Token expirado con redirección a pantalla de login. Estado vacío en listas y búsquedas con mensajes informativos."

    # ── 6. CONCLUSIONES ──
    Add-Heading -sel $sel -text "6. Conclusiones" -level 1
    Add-Paragraph -sel $sel -text "El desarrollo del Sistema de Gestión de Barbería bajo la metodología RUP permitió abordar de manera estructurada las necesidades de digitalización de una barbería, entregando una solución completa que abarca desde el registro de usuarios hasta la generación de reportes estadísticos. La implementación con Jetpack Compose y Kotlin proporcionó una base moderna y reactiva para la interfaz de usuario, mientras que la arquitectura MVVM facilitó la separación de responsabilidades y la mantenibilidad del código. La división por roles permitió adaptar las funcionalidades a las necesidades específicas de cada tipo de usuario, mejorando la experiencia de uso y la eficiencia operativa. La integración con un backend RESTful desplegado en Render demostró la viabilidad de una arquitectura cliente-servidor para aplicaciones móviles de gestión empresarial."

    # ── 7. REFERENCIAS ──
    Add-Heading -sel $sel -text "7. Referencias" -level 1
    $sel.ParagraphFormat.LeftIndent = [float]36
    $sel.ParagraphFormat.FirstLineIndent = [float]-36
    Add-Paragraph -sel $sel -text "Kruchten, P. (2000). The Rational Unified Process: An Introduction (2nd ed.). Addison-Wesley Professional."
    Add-Paragraph -sel $sel -text "Jacobson, I., Booch, G., & Rumbaugh, J. (1999). The Unified Software Development Process. Addison-Wesley."
    Add-Paragraph -sel $sel -text "American Psychological Association. (2020). Publication Manual of the American Psychological Association (7th ed.). https://doi.org/10.1037/0000165-000"
    Add-Paragraph -sel $sel -text "Google. (2024). Jetpack Compose Documentation. Android Developers. https://developer.android.com/jetpack/compose/documentation"
    Add-Paragraph -sel $sel -text "Square. (2024). Retrofit Documentation. Square Open Source. https://square.github.io/retrofit/"
    Add-Paragraph -sel $sel -text "Square. (2024). Moshi Documentation. Square Open Source. https://github.com/square/moshi"
    Add-Paragraph -sel $sel -text "Render. (2024). Render Documentation. https://render.com/docs"

    # ── GUARDAR ──
    $doc.SaveAs2([ref]$outputPath, [ref]16)  # 16 = wdFormatDocumentDefault (docx)
    $doc.Close()
    $word.Quit()

    Write-Host "Documento generado exitosamente." -ForegroundColor Green
    Write-Host "Ruta: $outputPath" -ForegroundColor Cyan

} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    if ($word) { $word.Quit() }
}
