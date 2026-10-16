# Shoppi

Aplicación móvil de punto de venta e inventario para pequeños comercios: app Android
(Kotlin + Jetpack Compose) con soporte offline y backend API REST (Java + Spring Boot)
sobre PostgreSQL.

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
