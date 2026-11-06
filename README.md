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

Además de lo planificado originalmente, la app incluye:

- **Agrupación por lote:** si se carga un producto con el mismo nombre, vencimiento y ubicación que uno activo, se suma a ese registro en lugar de duplicarlo.
- **Edición de productos** desde el detalle.
- **Baja parcial:** se puede indicar cuántas unidades se consumieron o descartaron; el resto sigue en el inventario.
- **Atajos de fecha** en el formulario (+3 días, +1 sem., +1 mes, +3 meses).
- **Onboarding** a pantalla completa en el primer uso, que explica los avisos antes de pedir el permiso de notificaciones.
- **Hora del aviso configurable** en Ajustes.
- **Métricas del piloto:** registro local de eventos (altas, bajas, tiempo de carga, uso de notificaciones) con resumen en Ajustes y exportación manual a CSV.

Fuera de alcance en esta versión: iOS, lectura de fechas por OCR, cuentas y nube, inventarios compartidos, listas de compras, recetas e integración con supermercados.

## Capturas y diseño

El diseño de la experiencia (UI / UX / CX) está en Figma, con sistema de diseño, 19 pantallas con sus estados (carga, contenido, vacío, error, sin conexión) y prototipo navegable:

**[Ver diseño en Figma](https://www.figma.com/design/3IFurF1Q1iGGSB3DIcqF2v)**

El diseño ya está implementado en la app: inventario, alta, detalle, escáner, ajustes y onboarding.


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
| **DI manual vía FreskoApplication** | Sin framework: las fábricas de ViewModel y el Worker obtienen sus dependencias de la clase `Application`. Alcanza para el tamaño actual de la app. |

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

**Alertas en segundo plano.** Un `Worker` de WorkManager corre una vez por día, a la hora elegida en Ajustes, consulta en Room los productos activos que vencen dentro del plazo configurado y publica una notificación local. Al tocarla se abre el inventario filtrado por "Por vencer".

**Registro de eventos.** Las altas, bajas y ediciones generan un `ProductEvent` en una tabla propia de Room. El cambio del producto y su evento se guardan en la misma transacción, así nunca quedan desfasados. Con esos eventos se calcula el resumen del piloto que muestra Ajustes.

**Infraestructura.** No hay servidor propio. Todo corre en el dispositivo y la única dependencia externa es la API pública de Open Food Facts, usada solo para autocompletar el nombre de un producto escaneado.

[**Ver Diagrama.**](https://app.diagrams.net/#G1alkpbZdyGYsolXA0AIvvIxT5shZUgdLr#%7B%22pageId%22%3A%22f0CJgA2T5WTjUYc3Ftwf%22%7D)

## Estrategia Offline First

| Situación | Comportamiento |
|---|---|
| **Con conexión** | Al escanear un código se consulta Open Food Facts y se autocompleta el nombre (editable). |
| **Sin conexión** | Todo funciona: registrar, consultar, dar de baja y recibir alertas. Solo falta el autocompletado; se avisa y se completa a mano. |
| **Recupera la conexión** | No hay sincronización pendiente, porque los datos del usuario nunca dependen de la red. |
| **API lenta o sin resultado** | Tiempo máximo de espera de 5 segundos; se informa y se permite reintentar o cargar a mano. |

Los datos del usuario se generan y se guardan solo en el teléfono (Room). Open Food Facts se consulta únicamente para sugerir nombres y su resultado no se guarda como dato maestro.

**Datos de productos:** Open Food Facts contributors, bajo licencia [ODbL](https://opendatacommons.org/licenses/odbl/1-0/). El crédito también aparece en Ajustes.

## Estructura del proyecto

```
app/
├── schemas/                   # Esquemas exportados de Room (versiones 2 a 4)
└── src/main/java/com/example/freskoapp/
    ├── FreskoApplication.kt   # DI manual
    ├── MainActivity.kt
    ├── presentation/
    │   ├── navigation/        # Navigation Compose, rutas y deep links
    │   ├── theme/             # Colores, tipografía, formas y tema (Fresko)
    │   ├── common/            # Componentes y textos compartidos
    │   ├── inventory/         # Lista, filtros, estados y onboarding
    │   ├── addproduct/        # Método de carga y formulario (alta y edición)
    │   ├── scanner/           # Escáner de códigos (CameraX + ML Kit)
    │   ├── detail/            # Detalle y baja del producto
    │   └── settings/          # Ajustes y datos del piloto
    ├── domain/
    │   ├── model/             # Product, ExpirationStatus, Location, ProductEvent...
    │   ├── usecase/           # Casos de uso
    │   └── repository/        # Interfaces
    ├── data/
    │   ├── local/             # Room: base, DAO, entidades y migraciones
    │   ├── remote/            # Retrofit: API y DTO de Open Food Facts
    │   ├── preferences/       # DataStore
    │   ├── mapper/            # Entidad / DTO ↔ dominio
    │   └── repository/        # Implementaciones
    └── work/                  # WorkManager y notificaciones
docs/
└── plans/                     # Planes de implementación (RF03, RF04)
```

## Cómo ejecutarla

### Requisitos

- Android Studio (versión reciente) con el SDK de Android instalado.
- JDK compatible con el proyecto (el incluido en Android Studio alcanza).
- Dispositivo o emulador con **Android 8.0 (API 26)** o superior. Para probar el escaneo conviene un dispositivo real con cámara.

### Pasos

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/TomIniguez/FreskoApp.git
   cd FreskoApp
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
| `CAMERA` | Escanear códigos de barras. | Al elegir "Escanear código de barras", no al iniciar la app. |
| `POST_NOTIFICATIONS` (Android 13+) | Mostrar los avisos de vencimiento. | En el onboarding del primer uso, después de explicar los avisos. Si se rechaza, se puede activar desde Ajustes. |
| `INTERNET` | Consultar Open Food Facts. | No requiere confirmación del usuario. |

Si el usuario rechaza un permiso, la app sigue siendo usable: sin cámara se carga a mano, y sin notificaciones se informa en Ajustes cómo activarlas.

## Pruebas

```bash
./gradlew test                 # pruebas unitarias
./gradlew connectedAndroidTest # pruebas instrumentadas (requiere dispositivo)
```

Las pruebas unitarias (`app/src/test`) cubren:

- **Dominio:** la regla de estado de vencimiento, las reglas de `Product` (lotes, baja parcial), el resumen del piloto y los casos de uso, con repositorios falsos.
- **Presentación:** los ViewModels de inventario, alta, detalle y ajustes, los textos de vencimiento y el contraste de colores del tema.
- **Datos:** los *mappers*, el cliente de Open Food Facts (con MockWebServer) y las preferencias en DataStore.
- **Alertas:** la programación del aviso diario y el texto de la notificación.

Las pruebas instrumentadas todavía son solo el ejemplo que genera Android Studio.

## Estado del proyecto

| Planificado en la idea original | Estado |
|---|---|
| Etapa 1: análisis y diseño (problema, requisitos, diseño en Figma, arquitectura) | ✅ Hecho |
| RF01 · Registrar producto (manual y por escaneo) | ✅ Hecho |
| RF02 · Inventario con filtros | ✅ Hecho |
| RF03 · Alertas de vencimiento | ✅ Hecho |
| RF04 · Baja con "Deshacer" | ✅ Hecho |
| Estados de carga, error, vacío, sin resultados y sin conexión | ✅ Hecho |
| Pruebas unitarias del dominio | ✅ Hecho |
| Pruebas instrumentadas | ✅ Hecho |

| Agregado durante el desarrollo | Estado |
|---|---|
| Agrupación por lote al registrar | ✅ Hecho |
| Edición de productos | ✅ Hecho |
| Baja parcial por unidades | ✅ Hecho |
| Onboarding de avisos a pantalla completa | ✅ Hecho |
| Hora del aviso configurable | ✅ Hecho |
| Métricas del piloto con exportación CSV | ✅ Hecho |
| Implementación del diseño de Figma | ✅ Hecho |

## Decisiones relevantes

- **Room como única fuente de verdad:** la interfaz siempre refleja lo guardado y la app funciona sin red.
- **Lista única por vencimiento** en lugar de agrupar por ubicación: responde a la pregunta "¿qué consumo primero?".
- **Baja lógica** (estado consumido / descartado) en vez de borrar: hace posible el "Deshacer".
- **Sin backend propio:** el problema no requiere cuentas ni sincronización, y mejora la privacidad.
- **Estado de vencimiento con color, ícono y texto**, nunca solo color (accesibilidad).
- **Carga manual de la fecha**, sin OCR en esta versión.
- **Un solo umbral de aviso** (`warning_days` en DataStore, por defecto 3, rango 1–7) para el estado "Por vencer", el filtro y la notificación. El día exacto del vencimiento cuenta como "Por vencer", no como "Vencido"; "Vencido" es desde el día siguiente.
- **Regla de lote:** dos productos activos son el mismo lote si coinciden el nombre (sin distinguir mayúsculas ni espacios de más), la fecha de vencimiento y la ubicación.
- **Baja parcial sin cambiar el esquema:** las unidades viven en `quantity`; dar de baja una parte la descuenta, y dar de baja todas cierra el registro.
- **Métricas del piloto solo locales:** los eventos no salen del teléfono salvo que el usuario exporte el CSV a mano.

## Limitaciones conocidas

- Los datos viven solo en un dispositivo: no hay respaldo ni sincronización entre teléfonos.
- La fecha de vencimiento se ingresa a mano.
- El autocompletado depende de que el producto exista en Open Food Facts.
- Los códigos UPC-E se consultan en su forma comprimida de 8 dígitos, así que probablemente no se encuentren; en ese caso se carga a mano.
- Los campos del formulario no sobreviven si Android cierra la app en segundo plano para liberar memoria: al volver, el formulario empieza vacío.
- Los avisos se envían una vez por día a la hora elegida en Ajustes (por defecto 09:00) y no son exactos: Android puede demorarlos por el ahorro de batería (Doze).
- El resumen del piloto evalúa todos los eventos con los días de aviso actuales: si se cambian durante el piloto, los eventos anteriores se juzgan con el valor nuevo.
- La regla de lote distingue acentos ("leche" y "léche" son productos distintos).
- Solo Android.

## Equipo y forma de trabajo

### Integrantes

| Nombre | Rol principal | Responsabilidades |
|---|---|---|
| Tomas Iñiguez Guerrisi | PMO | Desarrollo de todo |


### Ramas

- `main`: versión estable y entregable.
- `develop`: integración del trabajo en curso.
- `feature/<descripcion-corta>`: una rama por funcionalidad o tarea.

### Commits y pull requests

- Commits chicos y frecuentes, con mensajes que explican el cambio. Ejemplo: `feat: agregar filtro por ubicación en el inventario`.
- Prefijos sugeridos: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`.
- Todo cambio entra a `develop` mediante pull request. Como el equipo tiene un solo integrante, la revisión la hace el mismo autor antes de integrar.
- No se suben claves, archivos de build ni configuración personal del IDE.

### Uso de inteligencia artificial

Se usaron asistentes de IA como apoyo de consulta, revisión y borradores. Todo el código y la documentación incorporados fueron revisados, adaptados y son comprendidos y defendibles por el equipo.

---

Proyecto académico · UADE · 2026
