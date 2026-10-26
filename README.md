# Shoppi

Aplicación móvil de punto de venta e inventario para pequeños comercios: app Android
(Kotlin + Jetpack Compose) con soporte offline y backend API REST (Java + Spring Boot)
sobre PostgreSQL.

## Stack

| Capa | Tecnología |
|------|------------|
| App móvil | Kotlin + Jetpack Compose (Navigation, Room, Retrofit, Coroutines) |
| BD local | SQLite (Room) |
| Backend | Java + Spring Boot (API REST, JWT propio) |
| BD remota | PostgreSQL (source of truth) |
| Auxiliar | Firebase (FCM, Storage, Remote Config, Crashlytics/Analytics) |

## Estructura del repositorio

```
shoppi/
├── backend/    # Spring Boot (API REST + PostgreSQL)
├── android/    # App Kotlin (Room + Retrofit + Compose) — Fase 2
```

> `docs/` (plan de desarrollo, contrato API, modelo de datos, guías) vive solo en local
> y está excluida del repositorio.

## Ramas

| Rama | Propósito | Protección |
|------|-----------|------------|
| `main` | Producción / entrega final. Solo merge desde `develop` vía PR. | No push directo, requiere PR aprobado |
| `develop` | Integración. Rama base para todo el desarrollo diario. | No push directo, requiere PR |
| `feature/*` | Funcionalidad nueva. Ej: `feature/backend-auth`, `feature/android-login` | Push libre |
| `fix/*` | Corrección de bug sobre `develop`. Ej: `fix/backend-calculo-total` | Push libre |
| `hotfix/*` | Fix urgente sobre `main`. Se mergea a `main` Y a `develop` | Push libre |
| `docs/*` | Solo documentación. Ej: `docs/actualizar-readme` | Push libre |

**Regla de oro:** 1 tarea = 1 rama `feature/*` = 1 PR = 1+ commits granulares.
Nunca se hace push directo a `main` ni a `develop`.

## Cómo correr el backend

Requisitos: **JDK 17+** y **PostgreSQL** en ejecución.

1. Crear la base de datos y el usuario (una vez):

```bash
psql -U postgres -c "CREATE USER shoppi WITH PASSWORD 'shoppi';"
psql -U postgres -c "CREATE DATABASE shoppi OWNER shoppi;"
```

2. Configurar credenciales por variables de entorno (tienen defaults de desarrollo):

```bash
export DB_URL="jdbc:postgresql://localhost:5432/shoppi"   # default
export DB_USER="shoppi"                                    # default
export DB_PASS="shoppi"                                    # default
export JWT_SECRET="una-clave-larga-y-secreta-de-al-menos-32-caracteres"
```

3. Arrancar la API:

```bash
cd backend
./mvnw spring-boot:run
# API disponible en http://localhost:8080
```

4. Verificar que responde:

```bash
curl http://localhost:8080/api/ping          # {"status":"UP","service":"shoppi",...}
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

> Perfil de producción: `SPRING_PROFILES_ACTIVE=prod` (exige `DB_URL`, `DB_USER`, `DB_PASS` y `JWT_SECRET` por entorno y valida el schema con `ddl-auto=validate`).

## Cómo correr la app Android

> Estado actual: **núcleo de datos y sincronización** (Room + Retrofit +
> WorkManager) y **UI Compose** de login, registro, productos, clientes, punto
> de venta, inventario y reportes. El resto de pantallas se implementa en las
> siguientes fases del plan.

Requisitos: **JDK 17+**, **Android SDK 36.1** y un `android/local.properties` con
`sdk.dir=...` (no se versiona).

```bash
cd android
./gradlew :app:testDebugUnitTest   # pruebas unitarias
./gradlew :app:assembleDebug       # compila el APK
```

### UI Android integrada

- **Login y registro** (`ui/login`): formulario con validaciones, visibilidad de
  contraseña, chip "recordar sesión" y navegación al dashboard. Usa el login
  offline de `UsuarioRepository` cuando no hay red.
- **Productos** (`ui/productos`): lista en grid con búsqueda, badge de stock,
  estado de sincronización, alta/edición y borrado lógico con confirmación.
- **Clientes** (`ui/clientes`): lista con búsqueda por nombre, teléfono o correo,
  alta/edición con validaciones y borrado lógico con confirmación.
- **Punto de venta** (`ui/ventas`): catálogo con búsqueda y badge de stock que
  agrega al carrito al tocar; el carrito ajusta cantidades (tope = stock actual),
  permite elegir cliente opcional y método de pago, y muestra subtotal + IVA 13 %
  antes de confirmar. La venta se guarda en Room con `syncState = PENDING` y la
  sincroniza el `SyncManager`; el stock lo descuenta el backend.
- **Inventario** (`ui/inventario`): historial de entradas y salidas filtrable por
  producto y por rango de fechas (hoy / 7 días / 30 días), con resumen de
  movimientos y diálogo para registrar un movimiento nuevo. El stock lo aplica
  el servidor en la misma transacción que guarda el movimiento.
- **Reportes** (`ui/reportes`): tres pestañas — ventas por período (diario /
  semanal / mensual) con totales y serie diaria, ranking de productos más
  vendidos y estado del inventario (productos, stock bajo y valor). Se consultan
  en vivo a la API y se pueden recargar.
- **Tema y navegación** (`ui/theme`, `ui/navigation`): Material 3 dual-theme,
  BottomBar de 5 destinos y rutas base para las siguientes pantallas.

### Núcleo Android integrado

- **Room como fuente de verdad** (`data/model`, `data/local`): cada registro nace
  con `syncState = PENDING`, usa un UUID local y guarda el `remoteId` cuando la
  API confirma el alta. Las líneas de venta viajan como JSON (`itemsJson`).
- **DAO remoto con Retrofit** (`data/remote`): contrato de `api_specification.md`
  para auth, productos, clientes, ventas e inventario. La URL base por defecto es
  `http://10.0.2.2:8080/api/` (emulador) en `RetrofitClient`.
- **Sincronización offline-first** (`sync`): `SyncManager` empuja productos →
  clientes → ventas → movimientos de inventario y luego hace pull resolviendo
  conflictos con `SyncPolicy` (last-write-wins por `updatedAt`; un cambio local
  pendiente nunca se pierde; el inventario es append-only y solo se agregan).
  `SyncErrors` distingue errores de red (siguen `PENDING`) de errores de
  validación (pasan a `FAILED`). `NetworkMonitor` expone la conectividad y
  WorkManager ejecuta la sincronización cada 15 min y tras cada mutación.
- **Utilidades** (`util`): validadores de auth/producto/cliente/venta, hash
  SHA-256 + salt para el login offline, `SessionManager` (JWT en
  SharedPreferences) y `VentaCalculator` (IVA 13 %).
- **Pruebas**: `android/app/src/test` cubre política de conflictos, detección de
  errores de red, mapeos DTO (inventario y reportes), convertidores, validadores,
  cálculo de totales, rangos de fecha y hash de contraseñas.

### Limitaciones conocidas

- Los reportes se consultan en vivo: no se guardan en el dispositivo, así que
  sin conexión no hay datos que mostrar (solo el aviso para recargar).
- La pantalla de detalle de producto se implementa en una tarea posterior; por
  ahora tocar una tarjeta abre el formulario de edición.
- El login offline requiere haber iniciado sesión antes en el dispositivo; la
  sesión offline no tiene JWT, así que para sincronizar hay que volver a iniciar
  sesión con red.
- El pull no reconcilia borrados hechos en otros dispositivos (la API de listado
  no expone tombstones).
- El descuento de stock de una venta es responsabilidad transaccional del backend;
  el cliente no lo duplica localmente.
- El historial de inventario es de solo lectura: ni la app ni la API permiten
  editar ni borrar un movimiento ya registrado.
- La API se llama por `http://` sobre `10.0.2.2` (solo el emulador). Android 9+
  bloquea el tráfico en claro, así que para probar online todavía hay que
  permitirlo en el build de debug y, desde un teléfono físico, cambiar la URL
  base por la IP de la PC.

## Seguridad

No se commitean secretos: `google-services.json`, `serviceAccount*.json`, `*.jks`,
`*.keystore` y `.env` (ver `.gitignore`).
