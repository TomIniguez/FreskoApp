# FreskoApp
Aplicación Android para controlar los vencimientos de los alimentos del hogar. Registrás lo que comprás (escaneando el código de barras o a mano) y Fresko te avisa antes de que venza, para que consumas primero lo que corresponde y tires menos comida.

> Trabajo Práctico Obligatorio · Desarrollo de Aplicaciones I · Facultad de Ingeniería y Cs. Exactas · UADE

---

## Índice

1. [El problema](#el-problema)
2. [Qué hace la app](#qué-hace-la-app)
3. [Capturas y diseño](#capturas-y-diseño)
4. [Tecnologías](#tecnologías)
5. [Arquitectura](#arquitectura)
6. [Estrategia Offline First](#estrategia-offline-first)
7. [Estructura del proyecto](#estructura-del-proyecto)
8. [Cómo ejecutarla](#cómo-ejecutarla)
9. [Permisos](#permisos)
10. [Pruebas](#pruebas)
11. [Estado del proyecto](#estado-del-proyecto)
12. [Decisiones relevantes](#decisiones-relevantes)
13. [Limitaciones conocidas](#limitaciones-conocidas)
14. [Equipo y forma de trabajo](#equipo-y-forma-de-trabajo)

---

## El problema

En los hogares se tiran alimentos que se vencieron sin que nadie lo notara. Las causas más comunes son el olvido y la falta de visibilidad de lo que hay en la heladera, el freezer y la alacena. Hoy se resuelve "de memoria", revisando fechas a mano o con notas, y eso falla justo cuando la persona está ocupada.

**Problema → Usuario → Solución → Valor**

| | |
|---|---|
| **Problema** | Los alimentos se vencen por olvido y falta de visibilidad. |
| **Usuario** | Persona que hace las compras del hogar: poco tiempo, manos ocupadas, a veces sin señal. |
| **Solución** | Registro rápido, lista única ordenada por vencimiento y alertas locales. |
| **Valor** | Menos comida tirada, ahorro y menos carga mental. |

## Qué hace la app

| Requisito | Funcionalidad |
|---|---|
| **RF01** | Registrar un producto: nombre, fecha de vencimiento, cantidad y ubicación (Heladera / Freezer / Alacena). Opcionalmente se escanea el código de barras para autocompletar el nombre. |
| **RF02** | Ver el inventario en una lista única ordenada por fecha de vencimiento, con filtros por estado (Vencidos / Por vencer / Vigentes) y por ubicación. |
| **RF03** | Recibir una notificación local antes del vencimiento. Los días de anticipación son configurables. |
| **RF04** | Marcar un producto como consumido o descartado, con opción de deshacer. |

Fuera de alcance en esta versión: iOS, lectura de fechas por OCR, cuentas y nube, inventarios compartidos, listas de compras, recetas e integración con supermercados.

## Capturas y diseño

El diseño de la experiencia (UI / UX / CX) está en Figma, con sistema de diseño, 19 pantallas con sus estados (carga, contenido, vacío, error, sin conexión) y prototipo navegable:

**[Ver diseño en Figma](https://www.figma.com/design/3IFurF1Q1iGGSB3DIcqF2v)**

<!-- TODO: agregar capturas de la app real en /docs/screenshots y enlazarlas acá -->

## Tecnologías

| Tecnología | Para qué se usa |
|---|---|
| **Kotlin** | Lenguaje principal. |
| **Jetpack Compose** | Interfaz declarativa. Las listas usan `LazyColumn`. |
| **Navigation Compose** | Navegación entre pantallas. |
| **ViewModel + StateFlow** | Estado de cada pantalla, expuesto de forma inmutable. |
| **Coroutines / Flow** | Operaciones asíncronas y datos reactivos. |
| **Room** | Base de datos local; es la única fuente de verdad de la app. |
| **DataStore** | Preferencias del usuario (días de aviso, hora del aviso, onboarding). |
| **Retrofit** | Consulta a la API de Open Food Facts. |
| **WorkManager** | Verificación diaria de vencimientos y notificaciones locales. |
| **CameraX + ML Kit (barcode)** | Escaneo de códigos de barras en el dispositivo. |

<!-- TODO: si se usa inyección de dependencias (Hilt, Koin o manual), agregarla acá con su justificación -->

Las versiones exactas están en `gradle/libs.versions.toml`.

## Arquitectura

La app sigue **MVVM** y principios de **Clean Architecture**, con tres capas y dependencias que apuntan hacia el dominio:

```
UI (Compose) → ViewModel → Caso de uso → Repositorio → Fuente local (Room) / remota (Retrofit)
```

| Capa | Responsabilidad |
|---|---|
| **Presentación** | Pantallas Compose, navegación, `UiState` y ViewModels. Sin reglas de negocio. |
| **Dominio** | Modelos, reglas del negocio (estado de vencimiento), casos de uso e interfaces de repositorio. Kotlin puro, sin dependencias de Android. |
| **Datos** | Implementación de los repositorios, Room, Retrofit, DataStore y *mappers*. |

**Alertas en segundo plano.** Un `Worker` de WorkManager corre una vez por día, consulta en Room los productos activos que vencen dentro del plazo configurado y publica una notificación local. Al tocarla se abre el inventario filtrado por "Por vencer".

**Infraestructura.** No hay servidor propio. Todo corre en el dispositivo y la única dependencia externa es la API pública de Open Food Facts, usada solo para autocompletar el nombre de un producto escaneado.

<!-- TODO: agregar diagrama de arquitectura (flujo de datos y topología) en /docs y enlazarlo acá -->

## Estrategia Offline First

| Situación | Comportamiento |
|---|---|
| **Con conexión** | Al escanear un código se consulta Open Food Facts y se autocompleta el nombre (editable). |
| **Sin conexión** | Todo funciona: registrar, consultar, dar de baja y recibir alertas. Solo falta el autocompletado; se avisa y se completa a mano. |
| **Recupera la conexión** | No hay sincronización pendiente, porque los datos del usuario nunca dependen de la red. |
| **API lenta o sin resultado** | Tiempo máximo de espera de 5 segundos; se informa y se permite reintentar o cargar a mano. |

Los datos del usuario se generan y se guardan solo en el teléfono (Room). Open Food Facts se consulta únicamente para sugerir nombres y su resultado no se guarda como dato maestro.

## Estructura del proyecto

```
app/src/main/java/<paquete>/
├── presentation/
│   ├── navigation/        # Navigation Compose
│   ├── theme/             # Colores, tipografía, tema (Fresko)
│   ├── inventory/         # Lista, filtros y estados
│   ├── addproduct/        # Método de carga, escáner y formulario
│   ├── detail/            # Detalle y baja del producto
│   ├── settings/          # Ajustes
│   └── onboarding/        # Primer uso y permiso de notificaciones
├── domain/
│   ├── model/             # Product, ExpirationStatus, Location...
│   ├── usecase/           # Casos de uso
│   └── repository/        # Interfaces
├── data/
│   ├── local/             # Room: base, DAO, entidades
│   ├── remote/            # Retrofit: API y DTO de Open Food Facts
│   ├── preferences/       # DataStore
│   ├── mapper/            # Entidad / DTO ↔ dominio
│   └── repository/        # Implementaciones
└── work/                  # WorkManager y notificaciones
```

<!-- TODO: ajustar esta estructura a la que realmente tenga el repositorio -->

## Cómo ejecutarla

### Requisitos

- Android Studio (versión reciente) con el SDK de Android instalado.
- JDK compatible con el proyecto (el incluido en Android Studio alcanza).
- Dispositivo o emulador con **Android 8.0 (API 26)** o superior. Para probar el escaneo conviene un dispositivo real con cámara.

### Pasos

1. Clonar el repositorio:
   ```bash
   git clone <URL-DEL-REPOSITORIO>
   cd <carpeta-del-repositorio>
   ```
2. Abrir la carpeta con Android Studio y esperar a que termine la sincronización de Gradle.
3. Elegir un dispositivo o emulador.
4. Ejecutar la configuración **app** (▶).

También desde la terminal:

```bash
./gradlew assembleDebug        # compila
./gradlew installDebug         # instala en el dispositivo conectado
```

No hace falta configurar claves ni variables de entorno: la API de Open Food Facts es pública.

## Permisos

| Permiso | Motivo | Cuándo se pide |
|---|---|---|
| `CAMERA` | Escanear códigos de barras. | Al entrar al escáner, no al iniciar la app. |
| `POST_NOTIFICATIONS` (Android 13+) | Mostrar los avisos de vencimiento. | En el onboarding, con una explicación previa. |
| `INTERNET` | Consultar Open Food Facts. | No requiere confirmación del usuario. |

Si el usuario rechaza un permiso, la app sigue siendo usable: sin cámara se carga a mano, y sin notificaciones se informa en Ajustes cómo activarlas.

## Pruebas

```bash
./gradlew test                 # pruebas unitarias
./gradlew connectedAndroidTest # pruebas instrumentadas (requiere dispositivo)
```

Prioridad de cobertura: la regla que calcula el estado de vencimiento, los casos de uso con repositorios falsos y los ViewModels.

## Estado del proyecto

- [x] Etapa 1: análisis y diseño (problema, requisitos, diseño en Figma, arquitectura)
- [ ] RF01 · Registrar producto
- [ ] RF02 · Inventario con filtros
- [ ] RF03 · Alertas de vencimiento
- [ ] RF04 · Baja con "Deshacer"
- [ ] Estados de error, vacío y sin conexión
- [ ] Pruebas unitarias del dominio

<!-- TODO: actualizar la lista a medida que se implemente -->

## Decisiones relevantes

- **Room como única fuente de verdad:** la interfaz siempre refleja lo guardado y la app funciona sin red.
- **Lista única por vencimiento** en lugar de agrupar por ubicación: responde a la pregunta "¿qué consumo primero?".
- **Baja lógica** (estado consumido / descartado) en vez de borrar: hace posible el "Deshacer".
- **Sin backend propio:** el problema no requiere cuentas ni sincronización, y mejora la privacidad.
- **Estado de vencimiento con color, ícono y texto**, nunca solo color (accesibilidad).
- **Carga manual de la fecha**, sin OCR en esta versión.

## Limitaciones conocidas

- Los datos viven solo en un dispositivo: no hay respaldo ni sincronización entre teléfonos.
- La fecha de vencimiento se ingresa a mano.
- El autocompletado depende de que el producto exista en Open Food Facts.
- Solo Android.

## Equipo y forma de trabajo

### Integrantes

| Nombre | Rol principal | Responsabilidades |
|---|---|---|
| _Nombre y apellido_ | _Ej.: Arquitectura_ | _Ej.: capas, repositorios, casos de uso_ |
| _Nombre y apellido_ | _Ej.: UI / UX_ | _Ej.: Compose, navegación, accesibilidad_ |
| _Nombre y apellido_ | _Ej.: Persistencia_ | _Ej.: Room, DataStore, WorkManager_ |

Los roles no son exclusivos: todos los integrantes conocen el funcionamiento general de la app.

### Ramas

- `main`: versión estable y entregable.
- `develop`: integración del trabajo en curso.
- `feature/<descripcion-corta>`: una rama por funcionalidad o tarea.

### Commits y pull requests

- Commits chicos y frecuentes, con mensajes que explican el cambio. Ejemplo: `feat: agregar filtro por ubicación en el inventario`.
- Prefijos sugeridos: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`.
- Todo cambio entra a `develop` mediante pull request con al menos una revisión de otro integrante.
- No se suben claves, archivos de build ni configuración personal del IDE.

### Uso de inteligencia artificial

Se usaron asistentes de IA como apoyo de consulta, revisión y borradores. Todo el código y la documentación incorporados fueron revisados, adaptados y son comprendidos y defendibles por el equipo.

---

Proyecto académico · UADE · 2026

