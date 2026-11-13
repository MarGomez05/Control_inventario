# AGENTS.md — TAMACHIHUA-ERP

> Instrucciones para agentes que trabajen en este repositorio.
> Responder siempre en español.
> Leer y actualizar `MEMORY.md` en cada tarea.

## Stack tecnológico

- **Backend:** Java 21, Spring Boot 4.1.2, Maven Wrapper 3.10.0, Spring Data JPA, Hibernate, PostgreSQL 16+, Flyway, Spring Security + JWT HS256, BCrypt, Lombok, Bean Validation y springdoc OpenAPI.
- **Frontend:** Angular, único cliente web. **No se usa Vue. No se usa Ionic ni aplicación móvil.** Se ubicará en `web/`.
- **Pruebas:** JUnit 5, Spring Boot Test, Testcontainers (`postgres:latest`) y MockMvc.
- **CI/CD:** GitHub Actions para backend y, cuando exista, frontend.
- **Documentación:** `Api/README.md`, `Documentacion/Diagramas/Endpoints-API.md`, `Base_datos/`, `Implementacion.md` y `MEMORY.md`.

## Convenciones de código

- Código, clases, métodos, tablas y columnas en inglés. Mensajes de usuario, validaciones, comentarios y documentación en español.
- Paquetes en minúsculas; clases `PascalCase`; métodos y variables `camelCase`; constantes `UPPER_SNAKE_CASE`.
- DTOs como `record`; entidades con Lombok `@Getter`, `@Setter` y `@NoArgsConstructor`. No usar `@Data` en entidades.
- Inyección por constructor con `@RequiredArgsConstructor`; no usar `@Autowired` en campos.
- Servicio: interfaz + `impl`. `@Transactional(readOnly = true)` en la clase y `@Transactional` en métodos de escritura.
- Mapeo manual en `mapper`; no exponer entidades JPA en controllers.
- Dinero con `BigDecimal`: precios `10,2`, totales `12,2`, redondeo `HALF_UP`. Nunca `double` ni `float`.
- Fechas con `LocalDate` y `LocalDateTime`. `isActive` como `Boolean`, no `boolean`.
- JSON en `camelCase`; base de datos en `snake_case`.
- Validaciones de formato en DTOs con Jakarta Validation; reglas de negocio en servicios.
- Filtros de listado con `Specification`; paginación con `PageResponseDto` y máximo 100 registros.
- Errores centralizados en `GlobalExceptionHandler`; mensajes en español.
- Endpoints REST en plural y base `/api`. No hay `DELETE`: la baja lógica es `PATCH /{id}/status`.
- Módulos nuevos por funcionalidad (`customer/`, `sale/`, `billing/`). No agregar código nuevo a los paquetes globales por capa.
- Angular: standalone components, signals, servicios HTTP tipados, formularios reactivos, sin `any` y UI en español.

## Patrones

- Monolito modular: `controller → service → repository → entity`.
- DTO de entrada/salida + mapper manual.
- Baja lógica con `is_active`; historial de precios con vigencia.
- Stock derivado de lotes activos, nunca almacenado como columna.
- Venta transaccional con asignación **FEFO** a lotes y bloqueo pesimista.
- Factura inmutable con snapshot de cliente y líneas.
- Numeración de documentos por tabla de secuencias con bloqueo.
- Imágenes por URL; nunca binarios en PostgreSQL.
- Cambios de esquema únicamente mediante migraciones Flyway y `ddl-auto=validate`.

## Prohibiciones

- No usar `ddl-auto=update` ni `ddl-auto=create`. Solo `validate`.
- No modificar el esquema fuera de una migración Flyway.
- No eliminar registros físicamente ni agregar endpoints `DELETE`.
- No exponer entidades JPA ni costos a roles no autorizados.
- No usar `double` o `float` para dinero.
- No confiar en `userId` enviado por el cliente; siempre sale del JWT.
- No ocultar datos sensibles con `@JsonIgnore`; usar DTOs separados.
- No poner lógica de negocio en controllers ni acceder a repositorios desde ellos.
- No hardcodear secretos, credenciales ni URLs de producción.
- No commitear `.env*`, `target/`, `.idea/` ni archivos generados.
- No subir binarios de imágenes a PostgreSQL.
- No vender lotes vencidos.
- No usar `/api/app/**`, Vue, Ionic ni `X-Client-Interface` en código nuevo: el único cliente es Angular web.
- No hacer commit sin autorización del usuario.

## Estructura del proyecto

```text
Control_inventario/
├── AGENTS.md
├── MEMORY.md
├── Implementacion.md
├── README.md
├── Api/
│   ├── README.md              # Documentación del backend
│   └── api/                   # Proyecto Spring Boot (pom.xml aquí)
├── Base_datos/                # Esquema actual de referencia
├── Documentacion/             # Contrato, diagramas y logo
└── web/                       # Angular (futuro)
```

- Código backend actual: `Api/api/src/main/java/com/tamachihua_erp/api/`.
- Paquetes actuales: `config`, `security`, `controller`, `service`, `repository`, `entity`, `mapper`, `dto`, `exception`, `util`.
- Paquetes objetivo para módulos nuevos: `customer`, `sale`, `billing` e `inventory`.
- No hay POM en la raíz. Todos los comandos Maven se ejecutan desde `Api/api`.
- `Base_datos/01_create_tables.sql` y `02_seed_data.sql` son referencia histórica; la fuente oficial pasa a ser Flyway.

## Flujo de trabajo

1. Leer `AGENTS.md` y `MEMORY.md`.
2. Revisar `Implementacion.md` y `Documentacion/Diagramas/Endpoints-API.md`.
3. Actualizar `MEMORY.md` con decisiones, avances y pendientes.
4. Para cambios de esquema: crear migración Flyway, actualizar entidad, pruebas y documentación.
5. Para cambios de API: actualizar `SecurityConfig`, DTOs, contrato, pruebas y documentación.
6. Ejecutar compilación y pruebas antes de cerrar la tarea.
7. Actualizar `MEMORY.md` y la documentación afectada.
8. No hacer commit sin autorización.

## Testing y CI/CD

- Compilar: `cd Api/api && ./mvnw -q -DskipTests compile`
- Ejecutar: `cd Api/api && ./mvnw spring-boot:run`
- Todos los tests: `cd Api/api && ./mvnw test`
- Un test: `cd Api/api && ./mvnw -Dtest=ApiApplicationTests test`
- Un método: `cd Api/api && ./mvnw -Dtest=ApiApplicationTests#contextLoads test`
- Verificación completa: `cd Api/api && ./mvnw verify`
- Los tests usan Testcontainers y requieren Docker. Flyway prepara el esquema en el contenedor.
- Pruebas unitarias: cálculos de IVA, totales, FEFO, numeración y límite de crédito.
- Pruebas de integración: repositorios, servicios, MockMvc, seguridad y concurrencia.
- Frontend: `npm ci`, lint, tests y build cuando exista `web/`.
- CI: GitHub Actions con JDK 21 y Maven para backend; Node y Angular para frontend.
- Sin umbral de cobertura obligatorio por ahora.

## Memoria del proyecto

- **Leer y actualizar `MEMORY.md` en cada tarea.**
- Registrar decisiones, cambios de esquema, endpoints implementados y pendientes.
- No borrar decisiones anteriores; marcarlas como reemplazadas si cambian.
