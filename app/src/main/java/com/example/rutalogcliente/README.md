# RutaLog Perú · App Cliente

**Sector:** Logística y transporte de carga
**Alcance:** Nacional (las 25 regiones del Perú)
**Rol de esta app:** Cliente remitente. La gestión de envíos y rutas está en otra app: **App Operador logístico / Administrador**.
**Tecnología:** Kotlin + Jetpack Compose + Material 3 · MVVM con Repository · **Room** (base local) · **Retrofit** (API REST) · **WorkManager** (sincronización) · Navigation Compose · un solo módulo (`:app`)
**Paquete:** `com.example.rutalogcliente` · **Base local:** `rutalog_cliente.db`
**API REST:** `https://app-api-rutalog.vercel.app` (Node.js + Express en Vercel) · **Base remota:** PostgreSQL en Neon · Código de la API: repositorio `APPApi`

---

## 1. Problema que resuelve

### 

> Los remitentes no saben dónde está su carga y los operadores gestionan envíos y rutas de todo el Perú sin una vista central. RutaLog permite registrar, rastrear por número de guía y controlar las rutas activas en las 25 regiones.

### 

En el Perú, la carga que sale de Lima hacia las 25 regiones recorre rutas largas y muy distintas: costa, sierra y selva. Hay destinos, como Iquitos, a los que solo se llega combinando carretera y río. Muchas empresas pequeñas y medianas de transporte siguen coordinando sus envíos por llamadas, WhatsApp y hojas de cálculo.

**Para el cliente remitente:**
- Cuando entrega su carga, no sabe en qué etapa está: si ya la recogieron, si va en camino o si ya salió a reparto.
- No tiene un número de guía para consultar por su cuenta; tiene que llamar a la agencia.
- No sabe cuándo llegará, así que no puede avisar a su destinatario.

**Para el operador logístico:**
- No tiene una vista central de todos los envíos por estado, zona o región.
- Asignar transportistas y actualizar estados es manual, y se pierde información entre áreas.
- No sabe con facilidad qué rutas están activas, cuánta carga llevan ni la hora estimada de llegada.

**En resumen:** hay poca visibilidad y trazabilidad de la carga, tanto para quien envía como para quien opera.

### Cómo lo resuelve la App Cliente

| Problema del cliente | Función en la app | RF |
|---|---|---|
| No puede registrar su envío por su cuenta | Registrar envío indicando ruta y peso | RF06 |
| No tiene un número para consultar | Número de guía generado automáticamente al guardar | RF07 |
| No sabe cuánto le costará | Costo = peso × tarifa por kg de la ruta, calculado en vivo | RF08 |
| No sabe dónde está su carga | Búsqueda por número de guía y línea de tiempo del estado | RF09 |
| Se registran datos erróneos | Se rechaza cualquier envío con peso menor o igual a 0 | RF10 |
| En ruta o en almacén no siempre hay señal | La app funciona sin conexión y sincroniza sola al recuperarla | RF11 a RF15 |

---

## 2. Historia de usuario

> Como **cliente remitente**, quiero iniciar sesión, registrar mis envíos aunque no tenga conexión y consultar su estado por número de guía, para dar seguimiento a mi carga y que mis registros lleguen al servidor en cuanto vuelva la señal.

---

## 3. Arquitectura

```
Pantallas (Compose) → ViewModel → Repository ─┬→ Room      (fuente de datos de la interfaz)
                                              └→ Retrofit  (API REST → PostgreSQL en Neon)
```

- **La interfaz solo lee de Room.** Por eso la app muestra los datos con o sin conexión.
- **Cada escritura se guarda primero en Room** junto con una operación pendiente, y después se envía a la API.
- **La app nunca se conecta directo a la base remota.** Todo pasa por la API REST, que valida la sesión, el rol y los datos.
- **`di/AppContainer`** crea una sola vez la base, el cliente Retrofit y los repositorios; `MainActivity` los entrega a los ViewModel.

---

## 4. Requerimientos y dónde se cumplen

### Autenticación y sesión

| Regla | Implementación |
|---|---|
| Autenticar mediante la API | `LoginScreen` → `AuthViewModel.login()` → `AuthRepository.login()` → `POST /api/auth/login` |
| Guardar la sesión de forma segura | `SesionSegura.kt`: el token se guarda cifrado con `EncryptedSharedPreferences` |
| Acceso sin conexión a quien ya inició sesión | `SplashScreen` → si hay token guardado entra directo a Inicio, aunque no haya red |
| `UsuarioEntity` sin contraseña en texto plano | La tabla `usuarios` guarda solo `id`, `nombre`, `correo` y `rol` |
| Mensajes de error del login | Los devuelve la API: *"No existe una cuenta con ese correo."*, *"La contraseña es incorrecta."*; sin red: *"Sin conexión. Necesitas internet para iniciar sesión."* |

### Negocio (App Cliente)

| RF | Descripción | Implementación |
|---|---|---|
| RF06 | Registrar un envío indicando peso y ruta | `PantallaPrincipal()` en `MainActivity.kt` (2 campos + **Guardar**) → `EnvioViewModel.registrar()` → `EnvioRepository.registrar()` |
| RF07 | Generar el número de guía al insertar | `EnvioRepository.registrar()` genera la guía con `NumeroGuia.generar()` (formato `RLP-AA-NNNNNN-D`). Si el servidor ya tiene esa guía, asigna otra y la app la actualiza |
| RF08 | Calcular el costo (`pesoKg × tarifaPorKg`) | `CatalogoRutas.calcularCosto()` en `RutaTarifa.kt`. La API lo vuelve a calcular con su tabla `rutas` |
| RF09 | Buscar un envío por número de guía | `EnvioDao.buscarPorGuia()`: ignora guiones, espacios y mayúsculas. Funciona sin conexión porque busca en Room |
| RF10 | Impedir peso ≤ 0 | `EnvioViewModel.validar()` en la app y `PESO_INVALIDO` (422) en la API |
| RF11 | Actualizar Room con los datos de la API | `SyncRepository.guardarDescarga()`: `GET /api/envios` → Room, sin sobrescribir registros con cambios pendientes |
| RF12 | Escritura + operación pendiente en una transacción | `EnvioRepository.registrar()`, `actualizar()` y `eliminar()` usan `db.withTransaction { … }` |
| RF13 | WorkManager y botón manual | `SyncWorker.kt` (solo corre con red) y botón **Sincronizar ahora** en `SyncScreen.kt` |
| RF14 | Mostrar pendientes, errores y última sincronización | `SyncScreen.kt` + `SyncViewModel`; la pestaña **Sincronizar** muestra un contador |
| RF15 | Evitar duplicados por UUID y mostrar conflictos | Cada operación lleva `uuidOperacion`; la API la registra en `operaciones_procesadas`. Los conflictos quedan como **Rechazado** con su motivo |

---

## 5. Base de datos local (Room)

### `usuarios`

| Campo | Tipo | Nota |
|---|---|---|
| `id` | Int (PK) | El mismo identificador que tiene en el servidor |
| `nombre` | String | Nombre o razón social |
| `correo` | String | |
| `rol` | String | `cliente` |

No se guarda la contraseña.

### `envios`

| Campo | Tipo | Nota |
|---|---|---|
| `id` | Int (PK, autogenerado) | Identificador **local** |
| `idRemoto` | Int? | Identificador en el servidor; `null` hasta que la API acepta el envío |
| `uuid` | String (único) | Lo genera la app al crear el envío; identifica al mismo envío en el teléfono y en el servidor |
| `numeroGuia` | String | `RLP-26-100009-0` |
| `ruta` | String | `Lima → Trujillo` |
| `pesoKg` | Double | `12.5` |
| `costoEnvio` | Double | `23.75` (12.5 kg × S/ 1.90) |
| `estado` | String | `pendiente`, `recogido`, `en_transito`, `en_reparto`, `entregado` |
| `transportistaAsignado` | String? | Lo asigna el operador |
| `version` | Int | Aumenta con cada cambio en el servidor; sirve para detectar conflictos |
| `estadoSync` | String | `PENDIENTE`, `ENVIANDO`, `SINCRONIZADO` o `ERROR` |
| `eliminadoLocal` | Boolean | `true` si se eliminó sin conexión y falta confirmarlo en el servidor |
| `mensajeError` | String? | Motivo del rechazo, si lo hubo |

### `operaciones_pendientes`

Cola de escrituras por enviar. Se conserva aunque se cierre la app.

| Campo | Tipo | Nota |
|---|---|---|
| `id` | Int (PK, autogenerado) | |
| `uuidOperacion` | String | Identificador único de la operación; evita duplicados al reenviar |
| `entidad` | String | `envios` |
| `idEntidadLocal` | Int | `id` local del envío afectado |
| `tipoOperacion` | String | `CREAR`, `ACTUALIZAR` o `ELIMINAR` |
| `payload` | String | Datos de la operación en JSON |
| `fechaRegistro` | Long | Fecha en milisegundos |
| `estado` | String | `PENDIENTE`, `ENVIANDO` o `ERROR` |
| `intentos` | Int | Veces que se intentó enviar |
| `mensajeError` | String? | Último error recibido |

### `sync_metadata`

| Campo | Tipo | Nota |
|---|---|---|
| `recurso` | String (PK) | `envios` |
| `ultimaSincronizacionExitosa` | Long? | Fecha de la última descarga correcta |
| `cursorRemoto` | String? | Hora del servidor en esa descarga |

**Tarifas por ruta (RF08):** hay 27 rutas hacia las 25 regiones, en `model/RutaTarifa.kt`. La tarifa va de S/ 0.70/kg (Lima → Callao) a S/ 4.80/kg (Lima → Iquitos, terrestre y fluvial).

---

## 6. API REST y base remota

**URL base:** `https://app-api-rutalog.vercel.app`

| Método | Endpoint | Uso en la app |
|---|---|---|
| POST | `/api/auth/login` | Inicia sesión. Devuelve el token y el perfil |
| GET | `/api/envios` | Lista los envíos del cliente con sesión iniciada |
| GET | `/api/envios/{id}` | Consulta un envío |
| POST | `/api/envios` | Registra un envío (`uuidOperacion`, `uuid`, `numeroGuia`, `ruta`, `pesoKg`) |
| PUT | `/api/envios/{id}` | Modifica ruta o peso de un envío pendiente (`uuidOperacion`, `version`) |
| DELETE | `/api/envios/{id}` | Elimina un envío pendiente (`uuidOperacion`) |

Todos los endpoints de envíos exigen el token (`Authorization: Bearer …`). El cliente solo ve y modifica sus propios envíos.

**Respuestas de error que la app distingue**

| Código | Significado | Qué hace la app |
|---|---|---|
| 401 | Sesión inválida | Detiene la sincronización; la operación sigue pendiente |
| 409 `ENVIO_NO_EDITABLE` | El envío ya no está pendiente en el servidor | Conflicto: queda **Rechazado** con el motivo |
| 409 `VERSION_DESACTUALIZADA` | Otro cambio llegó antes al servidor | Conflicto: queda **Rechazado** con el motivo |
| 422 `PESO_INVALIDO` / `RUTA_INVALIDA` | Dato inválido | Queda **Rechazado** con el motivo |
| 503 o sin respuesta | Servidor caído o sin red | La operación sigue **Pendiente** y se reintenta |

**Tablas remotas (PostgreSQL en Neon):** `usuarios` (clave con hash bcrypt), `rutas` (tarifa por kg), `envios` (con `uuid`, `version`, `usuario_id` y `eliminado`) y `operaciones_procesadas` (UUID de cada operación ya aplicada). El esquema completo está en `db/esquema.sql` del repositorio de la API.

---

## 7. Sincronización

### Estados de un registro

| Estado | En pantalla | Significado |
|---|---|---|
| `PENDIENTE` | Pendiente de enviar | Guardado en el teléfono; falta enviarlo |
| `ENVIANDO` | Enviando | Se está enviando a la API |
| `SINCRONIZADO` | Sincronizado | El servidor lo aceptó y tiene `idRemoto` |
| `ERROR` | Rechazado | El servidor lo rechazó; se conserva con el motivo |

### Pasos de `SyncRepository.sincronizar()`

1. **Envía la cola** en el orden en que se registró. Cada operación viaja con su `uuidOperacion`.
   - Si la API la acepta, se borra de la cola y el envío queda **Sincronizado** con su `idRemoto`.
   - Si la API la rechaza (validación o conflicto), queda como **Rechazado**. No se pierde el dato.
   - Si no hay red o el servidor falla, la sincronización se detiene y la operación sigue **Pendiente**.
2. **Descarga** los envíos del cliente y actualiza Room. Los registros con cambios locales por enviar no se tocan.
3. **Guarda** la fecha de la sincronización en `sync_metadata`.

### Cuándo se sincroniza

- Al iniciar sesión y al abrir la app con sesión guardada.
- Después de cada registro, edición o eliminación: `SyncWorker` (WorkManager) con la condición `NetworkType.CONNECTED` y reintentos con espera creciente.
- Al recuperar la conexión, detectada por `ConnectivityObserver`.
- Con el botón **Sincronizar ahora**.

### Duplicados y conflictos

- **Sin duplicados:** si una operación se reenvía, la API reconoce su UUID y devuelve la respuesta guardada sin aplicarla otra vez.
- **Conflicto:** ocurre cuando el cliente edita o elimina sin conexión un envío que el operador ya cambió en el servidor. La operación queda **Rechazado** y el usuario elige **Reintentar** o **Descartar mi cambio** (acepta los datos del servidor).
- **Error del servidor:** el interruptor **Simular error del servidor** de la pestaña Sincronizar hace que la API responda 503, para demostrar que los datos quedan pendientes y no se pierden.

---

## 8. Permisos y seguridad

| Permiso | Para qué |
|---|---|
| `INTERNET` | Llamar a la API REST |
| `ACCESS_NETWORK_STATE` | Saber si hay conexión y mostrar la franja "Sin conexión" |

- La comunicación con la API es por **HTTPS**.
- El token se guarda cifrado y la contraseña nunca se guarda en el teléfono.
- `android:allowBackup="false"`: los datos de la app no se copian a respaldos del sistema.
- Si otra persona inicia sesión en el mismo teléfono, se borran los envíos y la cola de la cuenta anterior.

---

## 9. Usuarios y cuenta demo

| Rol | Descripción | Pantalla principal |
|---|---|---|
| **Cliente remitente** | Empresa o persona que envía carga. Registra envíos y los rastrea por número de guía. | Inicio |

**Cuenta demo:** `cliente@rutalog.pe` · clave `1234`. El botón **"Usar cuenta demo"** del Login la completa automáticamente.

Las cuentas se crean en el servidor; la app no tiene pantalla de registro. Una cuenta de operador no puede entrar a esta app.

---

## 10. Logo, ícono y colores

- **Ícono de la app:** el emblema del logo (mapa del Perú, caja, flecha y pin) sobre fondo blanco. Es un ícono adaptativo con versión monocromática.
  Archivos: `res/mipmap-*/ic_launcher_foreground.png`, `ic_launcher_monochrome.png`
- **Logo completo:** `res/drawable-nodpi/logo_rutalog.png`, que se usa en el Splash y el Login (`AppLogo.kt`).

| Color | Hex | Uso |
|---|---|---|
| Azul marino | `#0B2F5B` | Color primario: barra superior, cabeceras y texto de marca |
| Azul claro | `#1B4F8A` | Degradados de las cabeceras |
| Rojo | `#E3192A` | Color secundario: acentos, camión del Splash y barra de avance |
| Verde | `#2E7D32` | Estado "Entregado" y confirmaciones |

---

## 11. Flujo principal

```
Ícono → Splash ─┬→ (sin sesión) Login → Inicio
                └→ (con sesión guardada) Inicio, aunque no haya conexión

Inicio ─┬→ Registrar (PantallaPrincipal): Ruta + Peso → Guardar → guía generada
        ├→ Rastrear por guía ──────────────────────────→ Seguimiento
        ├→ Mis envíos → Seguimiento ─┬→ Editar envío (solo si está pendiente)
        │                            └→ Eliminar envío (solo si está pendiente)
        └→ Sincronizar: estado de la conexión, pendientes, rechazados y última sincronización
```

La barra inferior tiene cuatro pestañas: **Inicio · Registrar · Mis envíos · Sincronizar**.

---

## 12. Estructura del proyecto

```
com.example.rutalogcliente/
├── data/
│   ├── local/
│   │   ├── entities/         UsuarioEntity, EnvioEntity, OperacionPendienteEntity, SyncMetadataEntity
│   │   ├── dao/              UsuarioDao, EnvioDao, OperacionPendienteDao
│   │   ├── AppDatabase.kt    @Database con las 4 tablas + getDB()
│   │   └── SesionSegura.kt   token cifrado (EncryptedSharedPreferences)
│   ├── remote/
│   │   ├── api/ApiService.kt endpoints de Retrofit
│   │   ├── dto/EnvioDto.kt   cuerpos de petición y respuesta
│   │   └── RetrofitClient.kt cliente HTTP, token en cada petición y lectura de errores
│   ├── mapper/EnvioMapper.kt DTO ↔ Entity ↔ modelo
│   └── repository/           AuthRepository, EnvioRepository, SyncRepository
├── model/                    Envio, Usuario, EstadoEnvio, EstadoSincronizacion,
│                             RutaTarifa (RF08), NumeroGuia (RF07), RolUsuario
├── viewmodel/                AuthViewModel, EnvioViewModel, SyncViewModel
├── ui/
│   ├── navigation/AppNavigation.kt
│   ├── screens/              SplashScreen, LoginScreen, HomeScreen, ListScreen,
│   │                         DetailScreen, FormScreen, SyncScreen
│   ├── components/           ConnectivityBanner, SyncStatusIndicator, AppScaffold, ItemCard,
│   │                         InputField, AppLogo, EstadoEnvioUi, Mensajes, Formato
│   └── theme/                Color, Theme, Type
├── worker/SyncWorker.kt      sincronización en segundo plano (WorkManager)
├── util/ConnectivityObserver.kt   detecta si hay conexión
├── di/AppContainer.kt        crea la base, la API y los repositorios
├── RutaLogApplication.kt     inicia el contenedor
└── MainActivity.kt           inicializa los ViewModel
                              + @Composable PantallaPrincipal(): 2 OutlinedTextField (ruta y peso)
                                + botón Guardar + LazyColumn con los envíos guardados
```

### Pantallas

| Pantalla | Archivo | Qué hace |
|---|---|---|
| Splash | `SplashScreen.kt` | Animación de ~2.9 s; al terminar va a Inicio o al Login según haya sesión |
| Login | `LoginScreen.kt` | Correo, clave y cuenta demo; valida contra la API |
| Inicio | `HomeScreen.kt` | Resumen, accesos rápidos, rastreo por guía y envíos recientes |
| Mis envíos | `ListScreen.kt` | Lista (`LazyColumn`) con búsqueda y filtro por estado |
| Registrar | `PantallaPrincipal()` en `MainActivity.kt` | Campos **Ruta** y **Peso (kg)**, costo en vivo, botón **Guardar** y `LazyColumn` con los envíos guardados |
| Seguimiento | `DetailScreen.kt` | Estado, línea de tiempo, datos, costo, sincronización, editar y eliminar |
| Editar | `FormScreen.kt` | Cambiar la ruta o el peso de un envío pendiente |
| Sincronizar | `SyncScreen.kt` | Conexión, última sincronización, contadores, cola de operaciones y botón **Sincronizar ahora** |

Las pantallas con barra inferior y el Login muestran la franja **"Sin conexión"** cuando no hay red, y cada envío muestra su estado de sincronización.

---

## 13. Cómo probarlo

### Prueba online

1. Abrir la app → Splash → Login → **Usar cuenta demo** → **Ingresar**. Se descargan los envíos del cliente y aparecen como **Sincronizado** (RF11).
2. **RF10:** pestaña **Registrar** → elegir una ruta → peso `0` → **Guardar** → aparece *"El peso debe ser mayor que 0 kg."* y no se guarda nada.
3. **RF06, RF07 y RF08:** peso `12.5` con la ruta Lima → Trujillo. Se ve *"Costo: 12.50 kg × S/ 1.90 = S/ 23.75"*; al tocar **Guardar** aparece la guía y en unos segundos el envío pasa a **Sincronizado**.
4. **RF09:** en Inicio, escribir la guía (con o sin guiones) → **Buscar envío** → se abre su seguimiento.

### Prueba offline y recuperación

5. Activar el modo avión: aparece la franja **"Sin conexión"**.
6. Registrar un envío: queda **Pendiente de enviar** y la pestaña **Sincronizar** muestra 1 pendiente (RF12, RF14).
7. Cerrar la app por completo y abrirla sin conexión: entra sin pedir login y el envío sigue pendiente.
8. Quitar el modo avión: se sincroniza sola, o con **Sincronizar ahora**. El envío pasa a **Sincronizado** y se actualiza la fecha de última sincronización (RF13).
9. Tocar **Sincronizar ahora** varias veces: el total de envíos no cambia (RF15).
10. **Error del servidor:** activar **Simular error del servidor**, registrar un envío y sincronizar. La app avisa del error y el envío sigue pendiente. Al apagar el interruptor y sincronizar, pasa a **Sincronizado**.
11. **Conflicto:** en modo avión, editar el peso de un envío pendiente. Cambiar su estado en el servidor (con la cuenta de operador). Reconectar y sincronizar: la operación queda **Rechazado** con el motivo, el cambio local se conserva y se puede **Reintentar** o **Descartar mi cambio**.

---

### APK

El APK listo para instalar está en la carpeta `apk/` (Android 10 o superior). Para generarlo de nuevo en Android Studio: **Build → Generate App Bundles or APKs → Generate APKs**.

---

## 14. Limitaciones

- **El primer inicio de sesión necesita internet.** Como la contraseña no se guarda en el teléfono, sin conexión solo entra quien ya había iniciado sesión.
- **Conflictos resueltos por el usuario:** la app no mezcla cambios automáticamente; muestra el motivo y el usuario decide entre reintentar o descartar su cambio.
- **La guía puede cambiar al sincronizar:** si el número generado sin conexión ya existe en el servidor, este asigna otro y la app lo actualiza.
- **Tarifas en la app:** las 27 rutas y sus tarifas están en `RutaTarifa.kt`, con los mismos valores que la tabla `rutas` del servidor. El costo definitivo es el que calcula la API.
- **Solo pendientes:** el cliente solo puede editar o eliminar un envío mientras siga *pendiente de recojo*.

---

## 15. Notas técnicas

- **Versiones:** Room 2.8.5, Retrofit 2.11.0, WorkManager 2.10.0, Security Crypto 1.0.0, KSP 2.3.12, Navigation Compose 2.10.2, Compose BOM 2026.02.01, AGP 9.4.1, Kotlin 2.2.10, `minSdk` 29.
- **Cambio de esquema local:** la base está en la versión 2 con `fallbackToDestructiveMigration`. Al actualizar desde la versión anterior se recrean las tablas y los envíos se vuelven a descargar del servidor.
- **KSP desde `mavenLocal`:** en `settings.gradle.kts` se busca primero el archivo grande de KSP (`symbol-processing-aa-embeddable`, 81 MB) en `~/.m2`, porque la descarga por Gradle se cortaba con una conexión lenta. En otra PC, si ese archivo no está, Gradle lo descarga normalmente de Maven Central.
