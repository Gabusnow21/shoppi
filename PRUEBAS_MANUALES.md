# Pruebas manuales — Shoppi

Guía para validar el flujo completo desde Android Studio, con la API levantándose
automáticamente al ejecutar la app. Pensada para hacerla más tarde o para que la
ejecute el tutor.

Fecha de referencia: 2026-10-25.

---

## 1. Requisitos previos

- [ ] JDK 17 o superior.
- [ ] Android SDK 36.1 y `android/local.properties` con `sdk.dir=...`.
- [ ] PostgreSQL corriendo y base `shoppi` creada.
  - En esta máquina escucha en el puerto **5433** (el script también prueba 5432).
  - Usuario / contraseña / base: `shoppi` / `shoppi` / `shoppi`.
- [ ] Teléfono Android con depuración USB habilitada y aceptado en la PC.
- [ ] Cable USB conectado (se usa `adb reverse`, no hace falta la IP de la PC).

Verificar el dispositivo:

```bash
adb devices -l
```

Debe aparecer el teléfono en estado `device`.

---

## 2. Arranque desde Android Studio

1. Abrir la carpeta **`android/`** en Android Studio.
2. Seleccionar el dispositivo físico en la barra superior.
3. Presionar **Run** (▶).

Qué debe pasar automáticamente:

- [ ] Gradle ejecuta `prepareDevEnvironment`.
- [ ] Si la API no está arriba, la inicia con `backend/mvnw spring-boot:run`.
- [ ] Detecta PostgreSQL en `5433` (o `5432`).
- [ ] Configura `adb reverse tcp:8080 tcp:8080`.
- [ ] Compila e instala el APK debug.
- [ ] La app abre en el teléfono.

Comprobar la API desde la PC:

```bash
curl http://localhost:8080/api/ping
```

Respuesta esperada:

```json
{"status":"UP","service":"shoppi","timestamp":"..."}
```

Log de la API: `/tmp/shoppi-backend.log`.

---

## 3. Datos de acceso

Usuario de prueba ya creado en la base:

| Campo      | Valor                        |
|------------|------------------------------|
| Correo     | `tutor20261025@shoppi.com`   |
| Contraseña | `shoppi1234`                 |
| Nombre     | `TutorShoppi`                |

Si la base se reinició y el usuario no existe, usar **Regístrate** en la pantalla
de login y crear una cuenta nueva.

---

## 4. Casos de prueba

Marcar cada casilla al completarlo.

### CP-01 — Login

1. Abrir la app.
2. Ingresar el correo y la contraseña de la tabla anterior.
3. Tocar **Iniciar sesión**.

Resultado esperado:

- [ ] Entra al dashboard y saluda con `Hola, TutorShoppi`.
- [ ] Se ven las pestañas Inicio, Productos, Vender, Clientes y Reportes.
- [ ] No aparece error de red ni de credenciales.

### CP-02 — Registro (opcional)

1. Cerrar sesión.
2. Tocar **Regístrate**.
3. Completar nombre, correo nuevo, contraseña y confirmación.
4. Tocar **Registrarme**.

Resultado esperado:

- [ ] Crea la cuenta y permite iniciar sesión con ella.
- [ ] Si el correo ya existe, muestra un mensaje claro.

### CP-03 — Crear producto

1. Ir a **Productos**.
2. Tocar el botón **+** (abajo a la derecha).
3. Completar:
   - Nombre: `Café de prueba`
   - Precio: `5.50`
   - Stock actual: `10`
   - Stock mínimo: `3`
   - Categoría: `Bebidas`
4. Tocar **Crear producto**.

Resultado esperado:

- [ ] El producto aparece en la lista con precio `$5.50` y `Stock: 10`.
- [ ] Si la API está arriba, el badge de sincronización desaparece en segundos.
- [ ] En la base, el producto existe:

```bash
PGPASSWORD=shoppi psql -h localhost -p 5433 -U shoppi -d shoppi \
  -c "select id,nombre,precio,stock_actual from productos order by id desc limit 5;"
```

### CP-04 — Crear cliente

1. Ir a **Clientes**.
2. Tocar el botón **+**.
3. Completar nombre, teléfono, correo y dirección.
4. Tocar **Crear cliente**.

Resultado esperado:

- [ ] El cliente aparece en la lista.
- [ ] Se sincroniza con el backend sin quedar en estado pendiente.

### CP-05 — Movimiento de inventario

1. Ir a **Productos**.
2. Tocar el icono de inventario (arriba a la derecha).
3. Tocar **Movimiento**.
4. Elegir el producto, tipo **Entrada**, cantidad `2` y guardar.

Resultado esperado:

- [ ] El movimiento aparece en el historial.
- [ ] El stock del producto sube a `12` en la app y en la base.
- [ ] Si se hace una **Salida** mayor al stock, se rechaza con un mensaje.

### CP-06 — Venta online (flujo completo)

1. Ir a **Vender**.
2. Tocar el producto para agregarlo al carrito.
3. Tocar **Ver carrito**.
4. Seleccionar un cliente (opcional).
5. Tocar **Confirmar venta**.

Resultado esperado:

- [ ] La venta se confirma con un mensaje de éxito.
- [ ] El carrito queda vacío.
- [ ] El stock baja en la app y en la base.
- [ ] En la base aparece la venta:

```bash
PGPASSWORD=shoppi psql -h localhost -p 5433 -U shoppi -d shoppi \
  -c "select id,total,metodo_pago,cliente_id from ventas order by id desc limit 5;"
```

### CP-07 — Reportes

1. Ir a **Reportes**.
2. Revisar el resumen de ventas del día.

Resultado esperado:

- [ ] La venta recién registrada aparece en el total y en el listado.
- [ ] Los filtros Diario / Semanal / Mensual responden.

### CP-08 — Venta offline y sincronización (opcional)

Objetivo: comprobar que se puede vender sin conexión y que luego se sincroniza.

Opción A (con cable conectado):

1. Quitar el túnel de la API sin desconectar el teléfono:

```bash
adb reverse --remove tcp:8080
```

2. En la app, registrar una venta normalmente.
3. Verificar que la venta se guarda localmente (mensaje de éxito, carrito vacío).
4. Restaurar el túnel:

```bash
adb reverse tcp:8080 tcp:8080
```

5. Provocar una sincronización: editar cualquier producto y guardar, o esperar
   hasta 15 minutos (WorkManager periódico).

Opción B (más realista):

1. Desconectar el cable USB.
2. Registrar la venta en el teléfono.
3. Volver a conectar el cable y presionar **Run** en Android Studio, o ejecutar:

```bash
adb reverse tcp:8080 tcp:8080
```

4. Provocar la sincronización como en la opción A.

Resultado esperado:

- [ ] La venta se registra localmente aunque la API no responda.
- [ ] Al volver la conexión, la venta y el descuento de stock llegan al backend.
- [ ] No se duplica la venta en la base.

### CP-09 — Segundo Run (robustez)

1. Con la app ya abierta y la API corriendo, presionar **Run** otra vez.

Resultado esperado:

- [ ] El build termina rápido.
- [ ] El log muestra `La API ya está arriba`.
- [ ] No se queda colgado esperando el lock del backend.
- [ ] La app se reinstala/abre sin errores.

### CP-10 — Detener y volver a levantar la API

1. En una terminal:

```bash
cd android
./gradlew stopDevBackend
```

2. Verificar que la API bajó:

```bash
curl http://localhost:8080/api/ping
```

Debe fallar la conexión.

3. Presionar **Run** en Android Studio o ejecutar:

```bash
./gradlew :app:installDebug
```

Resultado esperado:

- [ ] La API se vuelve a levantar automáticamente.
- [ ] `adb reverse` queda configurado de nuevo.
- [ ] La app funciona sin pasos manuales adicionales.

---

## 5. Tabla de resultados

| Caso | Descripción                         | Resultado | Observaciones |
|------|-------------------------------------|-----------|---------------|
| CP-01 | Login                              | OK (10/11) | Email/clave real; sesión persistida y logout/login probados |
| CP-02 | Registro                           |           |               |
| CP-03 | Crear producto                     | PARCIAL   | Sin imagen: bucket de Storage no existe (ver §7.4) |
| CP-04 | Crear cliente                      |           |               |
| CP-05 | Movimiento de inventario           | OK (indirecto) | Stock descontado por el servidor tras cada venta |
| CP-06 | Venta online                       | OK (10/11) | Ventas #5, #6 y #7 sincronizadas; push y ticket verificados |
| CP-07 | Reportes                           |           |               |
| CP-08 | Venta offline + sincronización     |           |               |
| CP-09 | Segundo Run sin bloqueo            |           |               |
| CP-10 | Detener/levantar API               |           |               |

---

## 6. Comandos útiles

```bash
# Estado de la API
curl http://localhost:8080/api/ping

# Log del backend
tail -f /tmp/shoppi-backend.log

# PID del backend iniciado por el script
cat /tmp/shoppi-backend.pid

# Túnel del teléfono a la PC
adb reverse --list

# Volver a crear el túnel manualmente
adb reverse tcp:8080 tcp:8080

# Detener la API iniciada por el script
cd android && ./gradlew stopDevBackend

# Reinstalar el APK debug sin Android Studio
cd android && ./gradlew :app:installDebug

# Pruebas unitarias
cd android && ./gradlew :app:testDebugUnitTest

# Base de datos
PGPASSWORD=shoppi psql -h localhost -p 5433 -U shoppi -d shoppi
```

---

## 7. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---------|----------------|----------|
| La app muestra error de red | No hay `adb reverse` | `adb reverse tcp:8080 tcp:8080` o volver a presionar Run. |
| `curl http://localhost:8080/api/ping` falla | API detenida | Presionar Run o `./gradlew :app:installDebug`. |
| La API no conecta a PostgreSQL | Puerto incorrecto | Verificar `pg_isready -h localhost -p 5433`; el script detecta 5433/5432. |
| El segundo Run queda colgado | Lock heredado por el backend | Ya corregido en `prepare-dev.sh`; si pasa, `./gradlew stopDevBackend` y reintentar. |
| El catálogo de ventas aparece vacío | Sesión creada después del ViewModel | Ya corregido: los repositorios reaccionan a cambios de sesión. |
| No aparece “Ver carrito” | Barra tapada por la lista | Ya corregido en `PuntoVentaScreen.kt`. |
| El teléfono no aparece en `adb devices` | Depuración USB o cable | Revisar autorización USB y probar otro cable/puerto. |
| Se desconectó el cable durante la prueba | `adb reverse` se pierde | Volver a conectar y ejecutar `adb reverse tcp:8080 tcp:8080`. |

---

## 8. Notas técnicas

- La app **debug** usa `http://127.0.0.1:8080/api/` y `adb reverse`; funciona igual
  en emulador y en teléfono físico.
- El tráfico HTTP en claro está permitido **solo en debug**
  (`app/src/debug/AndroidManifest.xml`). Un build release debe usar HTTPS.
- La URL se puede sobreescribir en `android/local.properties`:

```properties
shoppi.apiBaseUrl=http://192.168.1.13:8080/api/
```

  En ese caso hay que abrir el puerto `8080` en el firewall de la PC.
- Para saltar el arranque automático de la API (por ejemplo en CI):

```bash
SHOPPI_SKIP_DEV_BACKEND=1 ./gradlew :app:assembleDebug
```

- La API y el túnel `adb reverse` los configura
  `android/scripts/prepare-dev.sh`, que se ejecuta como dependencia de
  `assembleDebug` e `installDebug`.

---

## 9. Prueba de Crashlytics (solo debug)

Requiere que `android/app/google-services.json` exista y que el teléfono tenga
internet real (Crashlytics no pasa por `adb reverse`).

1. Instalar el APK debug y ejecutar:

   ```bash
   adb shell am start -n com.shoppi/.CrashTestActivity
   ```

2. Pulsar **“Provocar crash”**. La app se cerrará.
3. Volver a abrir la app (para que Crashlytics procese el reporte).
4. En Firebase Console → **Calidad → Crashlytics**, esperar unos minutos.
5. Verificar que aparezca el stack de `CrashTestActivity` con el mensaje
   `Crash de prueba de Crashlytics`.

`CrashTestActivity` vive solo en `app/src/debug/`, por lo que no se incluye en
builds release.


---

## 10. Prueba de push FCM (end-to-end)

Requiere internet real en el teléfono (FCM no pasa por `adb reverse`) y un
token FCM registrado.

1. Instalar el APK debug y asegurarse de que la API esté arriba.
2. Iniciar sesión con el usuario de prueba.
3. Obtener un token FCM desde la app (o con `firebase.messaging().getToken()`)
   y registrarlo:

   ```bash
   curl -X POST http://localhost:8080/api/dispositivos/fcm-token \
     -H "Authorization: Bearer <JWT>" \
     -H "Content-Type: application/json" \
     -d '{"token":"<FCM_TOKEN>","deviceId":"<DEVICE_ID>","plataforma":"ANDROID"}'
   ```

   Respuesta esperada: `{"ok":true}`.

4. Crear una venta (`POST /api/ventas` o desde la app). El backend envía:
   - push **"Venta confirmada"** con data `{type: VENTA_CONFIRMADA, ventaId}`.
   - si algún producto queda con stock ≤ mínimo, push **"Stock bajo"** con
     data `{type: STOCK_BAJO, productoId}`.

5. Verificar que el push llegue al dispositivo y que la notificación tenga los
   canales `ventas` / `stock`.

6. Al cerrar sesión, borrar el token:

   ```bash
   curl -X DELETE http://localhost:8080/api/dispositivos/fcm-token \
     -H "Authorization: Bearer <JWT>" \
     -H "Content-Type: application/json" \
     -d '{"token":"<FCM_TOKEN>"}'
   ```

   Respuesta esperada: `204 No Content`.

---

## 7. Verificación Firebase 3B en dispositivo físico — 10/11/2026

Dispositivo: `23049RAD8C` (Xiaomi, Android 14), paquete `com.shoppi` debug.

### 7.1 Remote Config — OK
- Plantilla publicada: versión **2** con `api_base_url=http://127.0.0.1:8080/api/`,
  `sync_enabled=true`, `maintenance_mode=false`, `fcm_enabled=true`, `storage_max_mb=5`
  (verificado por API con la cuenta de servicio).
- En el dispositivo: `frc_..._firebase_settings.xml` muestra `last_template_version=2`,
  `last_fetch_status=-1` (éxito) y `minimum_fetch_interval_in_seconds=0` (debug).
- `RetrofitClient` tomó `api_base_url` y la app habló con la API sin recompilar.

### 7.2 Firebase Auth (custom-token) — OK
- Logcat: `FirebaseAuth: Notifying id token listeners about user ( user-1 )`.
- El backend emite el custom-token con `uid = user-{id}` a partir del JWT propio.

### 7.3 FCM — OK (requirió fix de backend)
- Token registrado en `dispositivos_fcm` (usuario 1, device `23049RAD8C`).
- Bug encontrado: `FirebaseMessaging.sendAll` usa el endpoint **batch legacy**
  (`https://fcm.googleapis.com/batch`) que responde **404** → ningún push se entregaba.
  Corregido en `f3f1f58` usando `sendEach` (API v1 `messages:send`).
- Tras el fix, al confirmar la venta #7 el backend logueó `Push FCM a usuario 1: 1 ok / 0 fallos`
  y el dispositivo mostró la notificación:
  - canal `ventas`, título **"Venta confirmada"**, texto **"Venta #7 registrada por 1.13."**
- Deep link verificado (frío y con app cerrada):
  - `push_type=VENTA_CONFIRMADA` + `ventaId=7` → abre **Ticket (T-154)**.
  - `push_type=STOCK_BAJO` → abre el **historial de inventario**.

### 7.4 Storage — BLOQUEADO (bucket inexistente)
- `FirebaseAuth` OK (`user-1`), pero `putFile` falla con
  `StorageException: Object does not exist at location. Code: -13010 HttpResult: 404`.
- Causa: el proyecto `shoppi-5215c` **no tiene ningún bucket** de Cloud Storage
  (verificado con la API de GCS: `storage#buckets` vacío y 404 para
  `shoppi-5215c.firebasestorage.app` / `shoppi-5215c.appspot.com`).
- Falta el paso manual: **Firebase Console → Storage → Comenzar** (crear bucket + reglas).
  Mientras no exista, la app mantiene el placeholder de imagen.

### 7.5 Crashlytics — OK
- `CrashTestActivity` provocó `RuntimeException: Crash de prueba de Crashlytics`.
- `FirebaseCrashlytics 19.0.3` inicializó y subió el reporte:
  `CctTransportBackend: Status Code: 200` a `crashlyticsreports-pa.googleapis.com`.
- `setUserId("user-1")` se ejecuta en el login (helper `Observabilidad`).
- Pendiente visual: confirmar el issue en Console → Crashlytics (puede tardar minutos).

### 7.6 Analytics — OK (eventos registrados)
- SDK inicializado (`FA: App measurement initialized`), sesión creada y
  `FA: Logging telemetry for logEvent from database` al confirmar ventas.
- Eventos del código: `login`, `venta_confirmada`, `sync_completed`.
- Para verlos en vivo: Console → Analytics → DebugView con
  `adb shell setprop debug.firebase.analytics.app com.shoppi` (ya aplicado en el equipo).
