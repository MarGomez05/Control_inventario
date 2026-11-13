# TAMACHIHUA-ERP

Sistema de gestión para ferretería y agroservicio. Actualmente cubre el módulo de **inventario** y está en proceso de escalar a un **ERP comercial** con clientes, ventas, facturación y pagos.

> **Alcance del cliente:** únicamente **Angular web**. No se usa Vue ni Ionic ni aplicación móvil.

## Estado de la nueva etapa

- Se adopta **Flyway** para versionar el esquema de base de datos.
- Se agregan tablas y relaciones para clientes, ventas, detalle de venta, facturación, pagos y secuencias de documentos.
- Se agrega imagen por presentación de producto mediante URL.
- Se retiran los endpoints `/api/app/**` y el encabezado `X-Client-Interface`.
- El frontend Angular se ubicará en `web/`.

El plan completo está en [`Implementacion.md`](Implementacion.md). Las decisiones, convenciones y pendientes están en [`MEMORY.md`](MEMORY.md).

## Estructura del proyecto

```text
Control_inventario/
├── AGENTS.md              # Instrucciones para agentes
├── MEMORY.md              # Memoria viva del proyecto
├── Implementacion.md      # Plan de la nueva etapa
├── README.md
├── Api/
│   ├── README.md          # Documentación del backend
│   └── api/               # Proyecto Spring Boot (pom.xml aquí)
├── Base_datos/            # Esquema anterior (referencia para el baseline Flyway)
├── Documentacion/         # Contrato de API, diagramas y logo
└── web/                   # Angular (futuro)
```

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.2, Maven Wrapper |
| Persistencia | PostgreSQL 16+, Spring Data JPA, Hibernate `validate`, Flyway |
| Seguridad | Spring Security, JWT HS256, BCrypt |
| Documentación API | springdoc OpenAPI |
| Frontend | Angular (standalone, signals) |
| Pruebas | JUnit 5, Testcontainers, MockMvc |
| CI/CD | GitHub Actions |

## Arranque rápido del backend

Requisitos: JDK 21, Docker (para pruebas) y PostgreSQL 16+.

```bash
cd Api/api
./mvnw spring-boot:run
```

Variables de entorno necesarias:

```text
DB_URL=jdbc:postgresql://localhost:5432/tamachihua_erp
DB_USERNAME=<usuario>
DB_PASSWORD=<contraseña>
JWT_SECRET=<base64 de al menos 32 bytes>
```

Comandos útiles:

```bash
cd Api/api
./mvnw -q -DskipTests compile
./mvnw test
./mvnw verify
```

Documentación interactiva: `http://localhost:8080/swagger-ui/index.html`.

## Documentación

- [`Implementacion.md`](Implementacion.md): plan de migración y nuevas funcionalidades.
- [`MEMORY.md`](MEMORY.md): estado, decisiones, convenciones y pendientes.
- [`Api/README.md`](Api/README.md): configuración y guía del backend.
- [`Documentacion/Diagramas/Endpoints-API.md`](Documentacion/Diagramas/Endpoints-API.md): contrato REST v2.0.
- [`AGENTS.md`](AGENTS.md): reglas para agentes y desarrolladores.

## Reglas generales

- No hay `DELETE`: la baja lógica se hace con `PATCH /{id}/status`.
- El `userId` sale siempre del JWT, nunca del body.
- Dinero con `BigDecimal`; nunca `double` ni `float`.
- Cambios de esquema solo mediante migraciones Flyway.
- `ddl-auto=validate` en todos los entornos.
- No commitear secretos, `target/`, `.idea/` ni archivos generados.
