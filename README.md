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
