# MEMORY.md — TAMACHIHUA-ERP

> Memoria viva del proyecto. Leer al inicio de cada tarea y actualizar al finalizar.
> Última actualización: 13/11/2026.
> Estado: documentación de la nueva etapa completa; código y esquema aún sin modificaciones.

## 1. Resumen

TAMACHIHUA-ERP es un sistema de gestión para ferretería y agroservicio. El módulo actual cubre inventario: catálogos, productos, presentaciones, precios, proveedores, lotes, dashboard y una API móvil que será retirada.

La nueva etapa convierte el sistema en un ERP comercial con:
- Clientes.
- Ventas y detalle de venta.
- Facturación, pagos y cuentas por cobrar.
- Imagen por presentación de producto.
- Esquema versionado con Flyway.
- Frontend Angular como único cliente web.

**Cambio de alcance aprobado:** se elimina Vue y se elimina Ionic/móvil. Solo habrá Angular web. Los endpoints `/api/app/**` y el encabezado `X-Client-Interface` se retiran en la migración.

## 2. Stack

- Backend: Java 21, Spring Boot 4.1.2, Maven Wrapper 3.10.0.
- Persistencia: PostgreSQL 16+, Spring Data JPA, Hibernate `validate`, Flyway.
- Seguridad: Spring Security, JWT HS256, BCrypt, roles no jerárquicos.
- Documentación: springdoc OpenAPI.
- Frontend: Angular (standalone, signals, servicios tipados).
- Pruebas: JUnit 5, Spring Boot Test, Testcontainers, MockMvc.
- CI/CD: GitHub Actions.

## 3. Estado actual

- Código backend en `Api/api/`.
- Documentación del backend en `Api/README.md`.
- Plan aprobado en `Implementacion.md`.
- Contrato REST v2.0 en `Documentacion/Diagramas/Endpoints-API.md`.
- Esquema actual en `Base_datos/01_create_tables.sql` y datos de prueba en `02_seed_data.sql` (referencia histórica para el baseline).
- 13 tablas actuales: `roles`, `users`, `product_categories`, `product_types`, `product_brands`, `product_models`, `measure_units`, `presentations`, `products`, `product_presentations`, `product_prices`, `suppliers`, `product_lots`.
- API implementada: auth, usuarios, roles, catálogos, productos, presentaciones, precios, proveedores, lotes, dashboard y `/api/app/**`.
- No existen clientes, ventas, detalle de venta, facturación, pagos ni imágenes.
- No hay Flyway todavía; el esquema se mantiene con scripts manuales.
- Tests: solo contexto de Spring con Testcontainers.
- No hay CI/CD configurado.
- El repositorio está en la rama `feature/Documentacion`. No se debe hacer commit sin autorización.

### Documentación completada en esta rama

- `AGENTS.md` en español con stack, convenciones, patrones, prohibiciones, estructura, flujo, testing y memoria.
- `MEMORY.md` como memoria viva.
- `Implementacion.md` con plan, modelo de datos, endpoints, flujos, Flyway, fases y criterios de aceptación.
- `README.md` raíz actualizado.
- `Api/README.md` reescrito sin Vue/Ionic, con Flyway y flujo Angular.
- `Documentacion/Diagramas/Endpoints-API.md` en v2.0 con clientes, ventas, facturación y pagos.
- `Base_datos/README.md` que marca la carpeta como referencia histórica.
- Encabezado de `Base_datos/01_create_tables.sql` actualizado para indicar que Flyway es la fuente oficial.

## 4. Decisiones aprobadas

| ID | Decisión | Estado |
|---|---|---|
| D-01 | Adoptar Flyway con baseline del esquema actual | Aprobada |
| D-02 | Los precios actuales incluyen IVA 13 %; `tax_rate` por producto | Aprobada |
| D-03 | Factura consumidor final, CCF y nota de crédito | Aprobada |
| D-04 | FEFO y exclusión de lotes vencidos al vender | Aprobada |
| D-05 | Paquetes por funcionalidad para módulos nuevos, migración incremental | Aprobada |
| D-06 | Imagen por URL; upload con almacenamiento local/S3 en fase posterior | Aprobada |
| D-07 | Angular standalone + signals; Material o Tailwind se decide antes de iniciar | Aprobada |
| D-08 | Mantener stock físico y agregar stock vendible sin vencidos | Aprobada |

## 5. Reglas de negocio clave

- La existencia física no se guarda: es `SUM(available_quantity)` de lotes activos (`is_active = true` y `available_quantity > 0`).
- El **stock vendible** excluye lotes vencidos. Las ventas usan stock vendible; las pantallas de inventario pueden mostrar ambos.
- Un solo precio vigente por presentación: `end_date IS NULL`; índice único parcial.
- Cambio de precio: cierra el vigente y crea uno nuevo con `user_id` del JWT.
- Baja lógica con `is_active`; no hay `DELETE`.
- Roles: `ADMIN`, `WAREHOUSE`, `PURCHASING`, `SELLER`; no son jerárquicos.
- `SELLER` no ve costos; `WAREHOUSE` no ve costos ni proveedores; `PURCHASING` ve costos.
- `userId` siempre se toma del JWT, nunca del body.
- Mensajes de usuario, validaciones, comentarios y documentación en español; identificadores de código en inglés.
- Venta: `DRAFT → CONFIRMED → CANCELLED`. Una venta facturada no se cancela directamente: se emite nota de crédito.
- Factura emitida: inmutable; snapshot de cliente y líneas.
- Pago: no puede superar el saldo de la factura.
- Crédito: se valida `saldo pendiente + total` contra `credit_limit`.

## 6. Convenciones de código

### Backend
- Paquetes en minúsculas; clases `PascalCase`; métodos y variables `camelCase`; constantes `UPPER_SNAKE_CASE`.
- DTOs como `record`; entidades con Lombok `@Getter/@Setter/@NoArgsConstructor`; no `@Data`.
- Inyección por constructor con `@RequiredArgsConstructor`; no `@Autowired` en campos.
- Servicio: interfaz + `impl`; `@Transactional(readOnly = true)` en la clase y `@Transactional` en escrituras.
- Mapeo manual en `mapper`; no exponer entidades.
- Dinero con `BigDecimal` y `RoundingMode.HALF_UP`; fechas con `LocalDate`/`LocalDateTime`.
- `isActive` como `Boolean`, no `boolean`.
- Validaciones de formato en DTOs; reglas de negocio en servicios.
- Filtros con `Specification`; paginación con `PageResponseDto` (máximo 100).
- Errores centralizados en `GlobalExceptionHandler`.
- Endpoints REST en plural, base `/api`, sin `DELETE`; baja lógica con `PATCH /{id}/status`.
- Módulos nuevos por funcionalidad: `customer/`, `sale/`, `billing/`; no agregar código nuevo a los paquetes globales por capa.

### Base de datos
- Nombres en inglés y `snake_case`; tablas en plural.
- PK `BIGSERIAL`; FK `BIGINT`; `is_active BOOLEAN NOT NULL DEFAULT TRUE` en catálogos.
- `created_at` y `updated_at` con trigger `set_audit_timestamps()`.
- Restricciones `CHECK` para cantidades, precios y estados.
- Índices en FKs y columnas de búsqueda.
- Nombres de catálogos únicos con `lower(name)`.
- Cambios de esquema solo por migración Flyway.

### Frontend (Angular)
- Standalone components, signals y servicios HTTP tipados.
- Estructura `core/`, `shared/`, `layout/`, `features/`.
- Interceptor JWT con `Authorization`.
- Guards por rol; el backend sigue siendo la autoridad.
- Formularios reactivos; sin `any`; UI en español.

## 7. Patrones

- Monolito modular; `controller → service → repository → entity`.
- DTO de entrada/salida + mapper manual.
- Baja lógica con `is_active`.
- Historial de precios con vigencia.
- Stock derivado de lotes.
- Venta transaccional con asignación FEFO y bloqueo pesimista.
- Factura inmutable con snapshot de cliente y líneas.
- Numeración por tabla de secuencias con bloqueo.
- Imágenes por URL, no binarios en la base.

## 8. Prohibiciones

- No usar `ddl-auto=update` ni `create`; solo `validate`.
- No modificar el esquema fuera de Flyway.
- No eliminar registros físicamente ni agregar `DELETE`.
- No exponer entidades JPA ni costos a roles no autorizados.
- No usar `double` o `float` para dinero.
- No confiar en `userId` enviado por el cliente.
- No ocultar datos sensibles con `@JsonIgnore`; usar DTOs separados.
- No poner lógica de negocio en controllers.
- No acceder a repositorios desde controllers.
- No hardcodear secretos, credenciales ni URLs de producción.
- No usar `System.out`; usar SLF4J.
- No commitear `target/`, `.idea/`, `.env*` ni archivos generados.
- No subir binarios de imágenes a PostgreSQL.
- No vender lotes vencidos.
- No usar `/api/app/**`, Vue, Ionic ni `X-Client-Interface` en código nuevo.
- No hacer commit sin autorización.

## 9. Estructura del proyecto

```text
Control_inventario/
├── AGENTS.md
├── MEMORY.md
├── Implementacion.md
├── README.md
├── Api/
│   ├── README.md
│   └── api/                 # Backend Spring Boot
├── Base_datos/              # Esquema histórico + README de referencia
├── Documentacion/           # Contrato, diagramas y logo
└── web/                     # Angular (futuro)
```

Backend actual: `Api/api/src/main/java/com/tamachihua_erp/api/`.
Módulos objetivo: `config`, `security`, `shared`, `catalog`, `inventory`, `customer`, `sale`, `billing`.

## 10. Comandos

Desde `Api/api`:

```bash
./mvnw -q -DskipTests compile
./mvnw spring-boot:run
./mvnw test
./mvnw -Dtest=ApiApplicationTests test
./mvnw -Dtest=ApiApplicationTests#contextLoads test
./mvnw package
./mvnw verify
```

Variables requeridas: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (Base64, mínimo 32 bytes decodificados). No hay carga automática de `.env`.

## 11. Documentación

- `Implementacion.md`: plan detallado de la nueva etapa.
- `Api/README.md`: guía del backend, configuración, Flyway y endpoints.
- `Documentacion/Diagramas/Endpoints-API.md`: contrato REST v2.0.
- `Base_datos/`: esquema y datos históricos; `README.md` explica su uso como referencia del baseline.
- `AGENTS.md`: reglas para agentes.

## 12. Pendientes técnicos

- Crear migraciones Flyway: baseline y nuevas tablas.
- Implementar clientes, ventas, facturación, pagos e imágenes.
- Retirar `/api/app/**` y `X-Client-Interface`.
- Actualizar `SecurityConfig` con la nueva matriz de permisos.
- Agregar `image_url` e `image_alt` a `product_presentations`.
- Agregar `tax_rate` a `products`.
- Implementar FEFO con bloqueo pesimista y stock vendible.
- Agregar pruebas de integración, concurrencia y seguridad.
- Configurar CI/CD.
- Crear frontend Angular en `web/`.
- Regenerar diagramas de arquitectura y clases.
- Documentación base ya actualizada; falta validarla con el equipo.

## 13. Cómo actualizar esta memoria

1. Actualizar la fecha y el estado en la cabecera.
2. Agregar decisiones nuevas a la tabla de decisiones.
3. Actualizar tablas y endpoints implementados.
4. Registrar pendientes y riesgos nuevos.
5. No borrar decisiones anteriores; marcarlas como reemplazadas si cambian.
