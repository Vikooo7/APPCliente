# RutaLog Perú · App Cliente (remitente)

App del **remitente** del caso 14 (proyecto final): registra envíos con o sin cobro contraentrega, paga el envío con
QR de Banco Andino, sigue la guía (eventos, intentos, avisos, foto y firma de la entrega, **llegada estimada** con
ML-02) y ve sus cobros y liquidaciones. Funciona sin conexión y sincroniza al volver.

```
App Cliente (Compose · MVVM · Room) → API RutaLog (Vercel) → Neon (PostgreSQL) · Banco Andino (por la API)
```

La documentación completa está en [app/src/main/java/com/example/rutalogcliente/README.md](app/src/main/java/com/example/rutalogcliente/README.md).

## Cómo probarla

**Instalar (lo más rápido):** descargar `RutaLog-Cliente.apk` desde **Releases** (columna derecha de esta página) en
un celular con Android 10 o superior, permitir "instalar apps de origen desconocido" y abrirla. Con este APK funciona
todo, incluido **Entrar con Google**.

**O compilarla:** abrir esta carpeta en Android Studio y pulsar **Run**. Incluye `app/google-services.json`
(proyecto Firebase `rutalog-peru`). En un APK compilado en otra PC, *Entrar con Google* necesita que la huella
SHA-1 de esa PC se registre en Firebase; el ingreso con correo y clave (y *Crear cuenta*) funciona siempre.

La app usa la API publicada (`https://app-api-rutalog.vercel.app`): no hace falta levantar ningún servidor.

## Cuentas de prueba

| App | Usuario | Clave | Rol |
|---|---|---|---|
| Cliente | `cliente@rutalog.pe` | `1234` | Remitente: Distribuidora Andina SAC |
| Cliente | `cliente2@rutalog.pe` | `1234` | Remitente: Textiles Gamarra EIRL |
| Admin | `supervisor@rutalog.pe` | `1234` | Operador |
| Admin | `operador@rutalog.pe` | `1234` | Administrador (aprueba liquidaciones, Mi crédito) |
| Courier | `courier1@rutalog.pe` … `courier10@rutalog.pe` | `1234` | Courier (cada uno ve lo que se le asignó) |
| Billetera Banco Andino | `andina` | `banco2026` | Paga los envíos de `cliente@rutalog.pe` y recibe su liquidación |
| Billetera Banco Andino | `cliente1` | `banco2026` | Destinatario: paga el contraentrega en la puerta |
| Web del comité | `https://app-api-rutalog.vercel.app/banco/comite` | `comite2026` | Aprueba o rechaza Mi crédito |

## Repositorios del proyecto

`APPApi` (API y núcleo de Banco Andino) · `APPCliente` · `APPAdmin` · `AppCourier` · `BancoAndino` (billetera).
