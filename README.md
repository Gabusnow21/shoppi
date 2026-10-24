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
├── backend/    # Spring Boot (API REST + PostgreSQL) — Fase 1
├── android/    # Kotlin: Room + Retrofit + sync offline-first (sin UI todavía)
└── docs/       # Documentación local (NO versionada)
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

> Pendiente: el proyecto Spring Boot se inicializa en la Fase 1 (T-010).

```bash
cd backend
./mvnw spring-boot:run
# API disponible en http://localhost:8080
```

## Cómo correr la app Android

> Estado actual: está integrado el **núcleo de datos y sincronización**
> (Room + Retrofit + WorkManager), **sin pantallas**. La UI (Compose) se
> implementa en la Fase 2 del plan.

Requisitos: **JDK 17+**, **Android SDK 36.1** y un `android/local.properties` con
`sdk.dir=...` (no se versiona).

```bash
cd android
./gradlew :app:testDebugUnitTest   # pruebas unitarias del núcleo
./gradlew :app:assembleDebug       # compila el APK (sin UI todavía)
```

### Núcleo Android integrado

- **Room como fuente de verdad** (`data/model`, `data/local`): cada registro nace
  con `syncState = PENDING`, usa un UUID local y guarda el `remoteId` cuando la
  API confirma el alta. Las líneas de venta viajan como JSON (`itemsJson`).
- **DAO remoto con Retrofit** (`data/remote`): contrato de `api_specification.md`
  para auth, productos, clientes y ventas. La URL base por defecto es
  `http://10.0.2.2:8080/api/` (emulador) en `RetrofitClient`.
- **Sincronización offline-first** (`sync`): `SyncManager` empuja productos →
  clientes → ventas y luego hace pull resolviendo conflictos con `SyncPolicy`
  (last-write-wins por `updatedAt`; un cambio local pendiente nunca se pierde).
  `SyncErrors` distingue errores de red (siguen `PENDING`) de errores de
  validación (pasan a `FAILED`). `NetworkMonitor` expone la conectividad y
  WorkManager ejecuta la sincronización cada 15 min y tras cada mutación.
- **Utilidades** (`util`): validadores de auth/producto/cliente/venta, hash
  SHA-256 + salt para el login offline, `SessionManager` (JWT en
  SharedPreferences) y `VentaCalculator` (IVA 13 %).
- **Pruebas**: `android/app/src/test` cubre política de conflictos, detección de
  errores de red, mapeos DTO, convertidores, validadores, cálculo de totales y
  hash de contraseñas.

### Limitaciones conocidas

- No incluye inventario, reportes ni pantallas: solo la capa funcional de datos.
- El login offline requiere haber iniciado sesión antes en el dispositivo; la
  sesión offline no tiene JWT, así que para sincronizar hay que volver a iniciar
  sesión con red.
- El pull no reconcilia borrados hechos en otros dispositivos (la API de listado
  no expone tombstones).
- El descuento de stock de una venta es responsabilidad transaccional del backend;
  el cliente no lo duplica localmente.

## Seguridad

No se commitean secretos: `google-services.json`, `serviceAccount*.json`, `*.jks`,
`*.keystore` y `.env` (ver `.gitignore`).
