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
├── android/    # App Kotlin (Room + Retrofit + Compose) — Fase 2
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

> Pendiente: el proyecto Android se inicializa en la Fase 2 (T-100).

1. Abrir la carpeta `android/` en Android Studio.
2. Sincronizar Gradle y ejecutar en emulador o dispositivo físico:

```bash
cd android
./gradlew assembleDebug
```

## Seguridad

No se commitean secretos: `google-services.json`, `serviceAccount*.json`, `*.jks`,
`*.keystore` y `.env` (ver `.gitignore`).
