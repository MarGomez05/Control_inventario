# TAMACHIHUA-ERP · Backend

**Responsable:** Marvin Gómez · **Apoyo:** Ali Velásquez

Backend REST del ERP, construido con Java 21 y Spring Boot. Centraliza la lógica de negocio, la seguridad y el acceso a PostgreSQL.

> **Alcance del cliente:** únicamente **Angular web**. No se usa Vue, Ionic ni aplicación móvil. Los endpoints `/api/app/**` y el encabezado `X-Client-Interface` se retiran en la migración.

## 1. Ubicación y estructura

El proyecto Maven está en `Api/api/`. Todos los comandos se ejecutan desde ahí.

```text
Api/
├── README.md          # Este documento
└── api/
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/
        ├── main/
        │   ├── java/com/tamachihua_erp/api/
        │   │   ├── ApiApplication.java
        │   │   ├── config/        # Seguridad, OpenAPI, CORS
        │   │   ├── security/      # JWT, filtros, user details
        │   │   ├── controller/    # Endpoints REST actuales
        │   │   ├── service/       # Interfaces + impl
        │   │   ├── repository/    # Spring Data JPA
        │   │   ├── entity/        # Entidades JPA
        │   │   ├── mapper/        # Mapeo manual entidad ↔ DTO
        │   │   ├── dto/           # Records de entrada y salida
        │   │   ├── exception/     # GlobalExceptionHandler
        │   │   └── util/
        │   └── resources/
        │       ├── application.properties
        │       └── db/migration/  # Migraciones Flyway (nuevo)
        └── test/
```

### Estructura objetivo por funcionalidad

Los módulos nuevos se organizan por funcionalidad, no por capa global:

```text
com.tamachihua_erp.api
├── config/
├── security/
├── shared/
├── catalog/       # Catálogos, productos, presentaciones, proveedores
├── inventory/     # Lotes, precios, stock, dashboard, reportes
├── customer/      # Clientes
├── sale/          # Ventas y detalle de venta
└── billing/       # Facturas, notas de crédito, pagos
```

Cada módulo replica `controller`, `dto`, `entity`, `mapper`, `repository`, `service` y `service/impl`.

## 2. Stack

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje |
| Spring Boot 4.1.2 | API REST |
| Spring Web MVC | Endpoints |
| Spring Data JPA / Hibernate | Persistencia con `ddl-auto=validate` |
| Flyway | Versionado del esquema |
| Spring Security + JWT HS256 | Autenticación y autorización |
| BCrypt | Hash de contraseñas |
| PostgreSQL 16+ | Base de datos |
| Lombok | Reducción de código repetitivo |
| Bean Validation | Validación de entrada |
| springdoc OpenAPI | Swagger UI |
| JUnit 5 + Testcontainers + MockMvc | Pruebas |

## 3. Arquitectura

Monolito modular con flujo:

```text
Angular web
    │
    ▼
HTTP Request + JWT
    │
    ▼
Controller → Service → Repository → PostgreSQL
```

- Los controllers no contienen lógica de negocio ni acceden a repositorios.
- Los DTO y mappers evitan exponer entidades JPA.
- La seguridad valida el JWT y los roles antes de llegar al controller.
- Ventas, facturación e inventario comparten transacciones atómicas.

## 4. Base de datos y Flyway

### 4.1 Fuente del esquema

La fuente oficial del esquema pasa a ser **Flyway**. Los archivos de `Base_datos/` quedan como referencia histórica del esquema anterior.

Migraciones planificadas en `src/main/resources/db/migration/`:

| Versión | Archivo | Contenido |
|---|---|---|
| `V1` | `V1__baseline_inventory.sql` | Esquema actual de 13 tablas, triggers, índices y datos base |
| `V2` | `V2__customers.sql` | Clientes |
| `V3` | `V3__sales.sql` | Secuencias, ventas, detalle y asignación de lotes |
| `V4` | `V4__invoicing.sql` | Facturas, detalle y pagos |
| `V5` | `V5__product_presentation_images.sql` | `image_url`, `image_alt` |
| `V6` | `V6__product_tax_rate.sql` | `tax_rate` y secuencias iniciales |

Reglas:

- No se usa `DROP` ni `TRUNCATE` en migraciones.
- En una base existente se configura el baseline de Flyway en `V1`.
- En pruebas, Flyway crea el esquema desde cero en el contenedor.
- `spring.jpa.hibernate.ddl-auto=validate` se mantiene siempre.

### 4.2 Reglas del esquema

- Nombres en inglés y `snake_case`; tablas en plural.
- PK `BIGSERIAL`, FK `BIGINT`.
- `is_active BOOLEAN NOT NULL DEFAULT TRUE` en catálogos.
- `created_at` y `updated_at` con trigger `set_audit_timestamps()`.
- `CHECK` para cantidades, precios y estados.
- La existencia no se guarda: se deriva de `SUM(available_quantity)` de lotes activos.
- `product_lots.total_cost` es columna generada `STORED`; la API no la inserta.
- Imágenes por URL; nunca binarios en PostgreSQL.

## 5. Configuración

### 5.1 Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | Conexión JDBC | `jdbc:postgresql://localhost:5432/tamachihua_erp` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña | — |
| `JWT_SECRET` | Clave Base64, mínimo 32 bytes decodificados | — |

No se cargan archivos `.env` automáticamente. Configura las variables en tu shell o en IntelliJ.

```bash
export DB_URL="jdbc:postgresql://localhost:5432/tamachihua_erp"
export DB_USERNAME="postgres"
export DB_PASSWORD="tu_contraseña"
export JWT_SECRET="tu_secreto_base64"
```

Para generar un secreto:

```bash
openssl rand -base64 48
```

### 5.2 `application.properties`

```properties
spring.application.name=tamachihua-erp-api

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=false

app.jwt.secret=${JWT_SECRET}
app.jwt.expiration=3600

# Flyway (se agrega en la implementación)
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

Nunca escribas credenciales reales en el repositorio.

## 6. Ejecutar el backend

Requisitos: JDK 21, PostgreSQL 16+ y Docker para pruebas.

```bash
cd Api/api
./mvnw spring-boot:run
```

Windows:

```powershell
cd Api\api
.\mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080` y la base de rutas es `http://localhost:8080/api`.

Swagger UI: `http://localhost:8080/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 7. Endpoints

### 7.1 Implementados (inventario)

| Grupo | Base |
|---|---|
| Autenticación | `/api/auth` |
| Usuarios y roles | `/api/users`, `/api/roles` |
| Catálogos | `/api/categories`, `/api/product-types`, `/api/brands`, `/api/models`, `/api/measure-units`, `/api/presentations` |
| Productos | `/api/products`, `/api/products/{id}/presentations` |
| Precios | `/api/products/{productId}/presentations/{id}/prices` |
| Proveedores | `/api/suppliers` |
| Lotes | `/api/lots`, `/api/products/{productId}/last-cost` |
| Dashboard | `/api/dashboard/summary`, `/api/dashboard/low-stock`, `/api/dashboard/expiring-lots` |
| Reportes | `/api/reports/stock/export` |

Patrón CRUD: `GET` paginado, `GET /options`, `GET /{id}`, `POST`, `PUT /{id}` y `PATCH /{id}/status`. No hay `DELETE`.

### 7.2 Nuevos (clientes, ventas y facturación)

| Grupo | Base |
|---|---|
| Clientes | `/api/customers` |
| Ventas | `/api/sales` |
| Facturación | `/api/invoices` |
| Pagos | `/api/invoices/{id}/payments` |

Endpoints destacados:

```text
POST /api/sales/{id}/confirm        Confirmar venta y asignar lotes FEFO
POST /api/sales/{id}/cancel         Cancelar y devolver stock
POST /api/sales/{id}/invoice        Emitir factura
POST /api/invoices/{id}/payments    Registrar pago
POST /api/invoices/{id}/credit-note Emitir nota de crédito
```

El contrato completo está en `Documentacion/Diagramas/Endpoints-API.md` (v2.0).

### 7.3 Retirados

- `/api/app/**`
- Encabezado `X-Client-Interface`
- Cualquier DTO o regla exclusiva de la app móvil

## 8. Seguridad

Roles no jerárquicos: `ADMIN`, `WAREHOUSE`, `PURCHASING`, `SELLER`.

| Recurso | ADMIN | WAREHOUSE | PURCHASING | SELLER |
|---|:-:|:-:|:-:|:-:|
| Usuarios y roles | ✔ | – | – | – |
| Catálogos (escritura) | ✔ | – | – | – |
| Productos y presentaciones | ✔ | ✔ | – | – |
| Precios | ✔ | – | – | – |
| Proveedores | ✔ | – | ✔ | – |
| Lotes | ✔ | ✔ | consulta | – |
| Costos | ✔ | – | ✔ | – |
| Clientes | ✔ | – | consulta | ✔ |
| Ventas | ✔ | consulta | consulta | propias |
| Facturación | ✔ | – | consulta | emitir |
| Pagos | ✔ | – | consulta | registrar |

Reglas:

- El `userId` sale siempre del JWT, nunca del body.
- `SELLER` no ve costos ni proveedores.
- `WAREHOUSE` no emite facturas ni registra pagos.
- Sin token válido, toda operación protegida responde 401.
- Token vigente por 1 hora; no hay refresh token ni lista de revocados.

## 9. Reglas de negocio clave

- Venta: `DRAFT → CONFIRMED → CANCELLED`.
- Al confirmar, se consume **stock vendible** (lotes activos no vencidos) en orden **FEFO**.
- Bloqueo pesimista sobre lotes para evitar sobreventa.
- Factura emitida inmutable; se cancela con nota de crédito.
- Numeración por `document_sequences` con `SELECT ... FOR UPDATE`.
- Pagos no pueden superar el total de la factura.
- Crédito: `saldo pendiente + total` no debe superar `credit_limit`.
- Precios incluyen IVA 13 %; `tax_rate` por producto.

## 10. Pruebas

```bash
cd Api/api
./mvnw test
./mvnw -Dtest=ApiApplicationTests test
./mvnw -Dtest=ApiApplicationTests#contextLoads test
./mvnw verify
```

- Testcontainers requiere Docker.
- Flyway prepara el esquema en el contenedor.
- Pruebas unitarias: IVA, totales, FEFO, numeración y crédito.
- Pruebas de integración: repositorios, servicios, MockMvc, seguridad y concurrencia.
- CI: GitHub Actions con JDK 21 y Maven.

## 11. Convenciones

- Código, clases, métodos, tablas y columnas en inglés.
- Mensajes, validaciones, comentarios y documentación en español.
- DTOs como `record`; entidades con `@Getter`, `@Setter` y `@NoArgsConstructor`.
- Inyección por constructor con `@RequiredArgsConstructor`.
- Servicio: interfaz + `impl`; `@Transactional(readOnly = true)` en clase y `@Transactional` en escrituras.
- Dinero con `BigDecimal` y `HALF_UP`; nunca `double` ni `float`.
- Fechas con `LocalDate` y `LocalDateTime`.
- JSON en `camelCase`; base de datos en `snake_case`.
- Filtros con `Specification`; paginación con `PageResponseDto` (máximo 100).
- Errores centralizados en `GlobalExceptionHandler`.

## 12. Solución de problemas

| Error | Causa probable | Solución |
|---|---|---|
| `Connection refused` | PostgreSQL apagado o puerto incorrecto | Verifica el servicio y `DB_URL` |
| `password authentication failed` | Credenciales incorrectas | Revisa `DB_USERNAME` y `DB_PASSWORD` |
| `Schema-validation` | Entidad y esquema no coinciden | Revisa la migración Flyway y los nombres/tipos |
| `FlywayException` | Migración fallida o checksum cambiado | No edites migraciones aplicadas; crea una nueva |
| `Port 8080 already in use` | Otro proceso usa el puerto | Detén el proceso o cambia `server.port` |

## 13. Documentación de referencia

- [`../Implementacion.md`](../Implementacion.md): plan de la nueva etapa.
- [`../MEMORY.md`](../MEMORY.md): decisiones y estado.
- [`../Documentacion/Diagramas/Endpoints-API.md`](../Documentacion/Diagramas/Endpoints-API.md): contrato REST.
- [`../Base_datos/`](../Base_datos/): esquema anterior, referencia del baseline.
- [`../AGENTS.md`](../AGENTS.md): reglas para agentes y desarrolladores.
