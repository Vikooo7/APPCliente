# RutaLog Perú · App Cliente

**Sector:** Logística y transporte de carga
**Alcance:** Nacional (las 25 regiones del Perú)
**Rol de esta app:** Cliente remitente. La gestión de envíos y rutas está en otra app: **App Operador logístico / Administrador** (`V:\MIAPPadmin`).
**Tecnología:** Kotlin + Jetpack Compose + Material 3 · MVVM · **Room** (base de datos local) · Navigation Compose · un solo módulo (`:app`)
**Paquete:** `com.example.rutalogcliente` · **Base de datos:** `rutalog_cliente.db`

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

---

## 2. Historia de usuario

> Como **cliente remitente**, quiero iniciar sesión, registrar un envío guardado en Room y consultar su estado mediante su número de guía, para dar seguimiento simulado a mi carga entre sesiones.

---

## 3. Requerimientos y dónde se cumplen

### Autenticación (comunes a ambas apps)

| RF | Descripción | Implementación |
|---|---|---|
| RFA01 | Splash con identidad visual propia | `SplashScreen.kt`: logo, frase y camión que recorre la ruta; llega al final justo al pasar al Login |
| RFA02 | Registrar un usuario (nombre, correo, clave, rol) | `RegistroScreen.kt` → `AuthViewModel.registrar()` → `UsuarioDao.registrar()`. El rol queda fijo en `"cliente"` |
| RFA03 | Iniciar sesión contra la tabla `usuarios` | `LoginScreen.kt` → `AuthViewModel.login()` → `UsuarioDao.login(correo, clave)` |
| RFA04 | Error si las credenciales no coinciden o el usuario no existe | Mensajes: *"No existe una cuenta con ese correo"* y *"La contraseña es incorrecta"* |
| RFA05 | Ir a la pantalla principal del rol tras el login | `AppNavigation.kt`: Login → `HomeScreen` (Inicio del cliente) |

### Negocio (App Cliente)

| RF | Descripción | Implementación |
|---|---|---|
| RF06 | Registrar un nuevo envío indicando peso y ruta | `PantallaPrincipal()` en `MainActivity.kt` (2 campos + **Guardar**) → `EnvioViewModel.registrar()` → `EnvioDao.insertarConGuia()` |
| RF07 | Generar automáticamente el número de guía al insertar | `EnvioDao.insertarConGuia()`: inserta, toma el `id` de Room y guarda la guía en la misma transacción. Formato en `NumeroGuia.kt`: `RLP-AA-NNNNNN-D` |
| RF08 | Calcular el costo (`pesoKg × tarifaPorKg` según la ruta) | `CatalogoRutas.calcularCosto()` en `RutaTarifa.kt`; se guarda en `costoEnvio` |
| RF09 | Buscar un envío por número de guía | `EnvioDao.buscarPorGuia()`: consulta SQL que ignora guiones, espacios y mayúsculas. Disponible en Inicio y en Mis envíos |
| RF10 | Impedir registrar un envío con peso ≤ 0 | `EnvioViewModel.validar()`: muestra *"El peso debe ser mayor que 0 kg."* y no inserta nada |

---

## 4. Base de datos Room

### Tabla `usuarios`

| Campo | Tipo | Nota |
|---|---|---|
| `id` | Int (PK, autogenerado) | |
| `nombre` | String | Nombre o razón social |
| `correo` | String | Se guarda en minúsculas; no se permiten repetidos |
| `clave` | String | Texto plano (ver limitaciones) |
| `rol` | String | Siempre `"cliente"` en esta app |

### Tabla `envios`

Tiene el mismo nombre y los mismos campos en la App Cliente y en la App Operador.

| Campo | Tipo | Ejemplo |
|---|---|---|
| `id` | Int (PK, autogenerado) | `9` |
| `numeroGuia` | String | `RLP-26-100009-0` |
| `ruta` | String | `Lima → Trujillo` |
| `pesoKg` | Double | `12.5` |
| `costoEnvio` | Double | `23.75` (12.5 kg × S/ 1.90) |
| `estado` | String | `pendiente`, `recogido`, `en_transito`, `en_reparto`, `entregado` |
| `transportistaAsignado` | String? | `null` hasta que el operador lo asigne |

**Datos iniciales:** la primera vez que se crea la base (`AppDatabase` → `DatosIniciales`), se insertan la cuenta demo y 8 envíos de ejemplo en distintos estados.

**Tarifas por ruta (RF08):** hay 27 rutas hacia las 25 regiones, en `model/RutaTarifa.kt`. La tarifa va de S/ 0.70/kg (Lima → Callao) a S/ 4.80/kg (Lima → Iquitos, terrestre y fluvial).

---

## 5. Usuarios y cuenta demo

| Rol | Descripción | Pantalla principal |
|---|---|---|
| **Cliente remitente** | Empresa o persona que envía carga. Registra envíos y los rastrea por número de guía. | Inicio |

**Cuenta demo:** `cliente@rutalog.pe` · clave `1234`. El botón **"Usar cuenta demo"** del Login la completa automáticamente. También se puede crear una cuenta nueva con **"Regístrate"**.

El **operador logístico / administrador** usa su propia app, con su propio Login y su propia tabla `usuarios`.

---

## 6. Logo, ícono y colores

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

## 7. Flujo principal

```
Ícono → Splash → Login ──(¿no tienes cuenta?)──→ Registro → Login
                   │
                   └→ Inicio ─┬→ Registrar (PantallaPrincipal): Ruta + Peso → Guardar → guía generada
                              │                                  └→ lista de envíos → Seguimiento
                              ├→ Rastrear por guía ─────────────────────────────→ Seguimiento
                              └→ Mis envíos → Seguimiento ─┬→ Editar envío (FormScreen, solo si está pendiente)
                                                           └→ Eliminar envío (solo si está pendiente)
```

La barra inferior tiene tres pestañas: **Inicio · Registrar · Mis envíos**.

---

## 8. Estructura MVVM

```
com.example.rutalogcliente/
├── data/local/
│   ├── Usuario.kt            @Entity(tableName = "usuarios")
│   ├── UsuarioDao.kt         registrar(), login(), buscarPorCorreo()
│   ├── Envio.kt              @Entity(tableName = "envios")
│   ├── EnvioDao.kt           insertar, actualizar, eliminar, obtenerTodos (Flow),
│   │                         obtenerPorId, buscarPorGuia, insertarConGuia
│   └── AppDatabase.kt        @Database(entities = [Usuario, Envio]) + getDB() + datos iniciales
├── model/
│   ├── RolUsuario.kt         CLIENTE, ADMINISTRADOR, EMPLEADO
│   ├── EstadoEnvio.kt        pendiente → recogido → en tránsito → en reparto → entregado
│   ├── RutaTarifa.kt         27 rutas con tarifa por kg + calcularCosto()      (RF08)
│   └── NumeroGuia.kt         generar() y normalizar() del número de guía     (RF07)
├── ui/
│   ├── navigation/AppNavigation.kt   Splash → Login/Registro → Inicio del cliente
│   ├── screens/              SplashScreen, LoginScreen, RegistroScreen, HomeScreen,
│   │                         ListScreen, DetailScreen, FormScreen
│   ├── components/           AppLogo, InputField, ItemCard (+ AppScaffold, EstadoEnvioUi,
│   │                         Mensajes, Formato)
│   └── theme/                Color, Theme, Type
├── viewmodel/
│   ├── AuthViewModel.kt      registrar(), login(), estado de sesión
│   └── EnvioViewModel.kt     lista (Flow), registrar/actualizar/eliminar, costo, validación, búsqueda
└── MainActivity.kt           crea AppDatabase.getDB(this) e inicializa los ViewModel
                              + @Composable PantallaPrincipal(): 2 OutlinedTextField (ruta y peso)
                                + botón Guardar + LazyColumn con los envíos guardados
```

**Sobre `[EntidadOperacion]`:** la ficha de la App Cliente pide solo las tablas `usuarios` y `envios`. Aquí **`Envio` es a la vez la entidad principal y la operación de negocio** (registrar un envío), por eso no hay un archivo aparte.

### Pantallas

| Pantalla | Archivo | Qué hace |
|---|---|---|
| Splash | `SplashScreen.kt` | Animación de ~2.9 s sincronizada con el ingreso |
| Login | `LoginScreen.kt` | Correo, clave, cuenta demo y enlace a Registro |
| Registro | `RegistroScreen.kt` | Nombre, correo, clave y confirmación; rol fijo `cliente` |
| Inicio | `HomeScreen.kt` | Resumen, accesos rápidos, rastreo por guía y envíos recientes |
| Mis envíos | `ListScreen.kt` | Lista (`LazyColumn`) con búsqueda y filtro por estado |
| Registrar | `PantallaPrincipal()` en `MainActivity.kt` | Al estilo del ejemplo de clase: campos **Ruta** y **Peso (kg)**, costo en vivo, botón **Guardar** y `LazyColumn` con los envíos guardados |
| Seguimiento | `DetailScreen.kt` | Estado, línea de tiempo, datos, costo, editar y eliminar |
| Editar | `FormScreen.kt` | Cambiar la ruta o el peso de un envío pendiente |

---

## 9. Cómo probarlo

1. Abrir la app → Splash → Login → **Usar cuenta demo** → **Ingresar**.
2. **RF10:** pestaña **Registrar** → elegir una ruta → peso `0` → **Guardar** → aparece *"El peso debe ser mayor que 0 kg."* y no se guarda nada.
3. **RF06, RF07 y RF08:** cambiar el peso a `12.5` con la ruta Lima → Trujillo. Debajo del peso se ve *"Costo: 12.50 kg × S/ 1.90 = S/ 23.75"*; al tocar **Guardar** aparece la guía generada (por ejemplo `RLP-26-100009-0`) y el envío se suma arriba de la lista.
4. **RF09:** en Inicio, escribir la guía (con o sin guiones) → **Buscar envío** → se abre su seguimiento.
5. **Entre sesiones:** cerrar la app por completo y volver a abrirla → el envío sigue en **Mis envíos**.

---

### APK

El APK listo para instalar está en `apk/RutaLog-Cliente-v0.6.apk` (Android 10 o superior). Para generarlo de nuevo en Android Studio: **Build → Generate App Bundles or APKs → Generate APKs**.

---

## 10. Limitaciones

- **Contraseñas en texto plano:** por ser una práctica académica, la clave se guarda tal cual en Room. En un proyecto real se guardaría un *hash* seguro (por ejemplo, bcrypt o Argon2) y nunca la clave original.
- **Envíos compartidos entre cuentas:** la tabla `envios` definida para el caso no tiene un campo de usuario, así que todas las cuentas de este teléfono ven los mismos envíos.
- **Apps independientes:** la App Cliente y la App Operador tienen bases de datos separadas, así que un envío registrado aquí no aparece en la otra. El estado de los envíos de ejemplo simula lo que haría el operador.
- **Solo pendientes:** el cliente solo puede editar o eliminar un envío mientras siga *pendiente de recojo*.

---

## 11. Notas técnicas

- **Versiones:** Room 2.8.5, KSP 2.3.12, Navigation Compose 2.10.2, Compose BOM 2026.02.01, AGP 9.4.1, Kotlin 2.2.10, `minSdk` 29.
- **KSP desde `mavenLocal`:** en `settings.gradle.kts` se busca primero el archivo grande de KSP (`symbol-processing-aa-embeddable`, 81 MB) en `~/.m2`, porque la descarga por Gradle se cortaba con una conexión lenta. En otra PC, si ese archivo no está, Gradle lo descarga normalmente de Maven Central.
