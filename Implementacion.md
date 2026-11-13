# Implementación — Nueva etapa de TAMACHIHUA-ERP

> Documento de planificación aprobado.
> Fecha: 13/11/2026 · Rama: `feature/Documentacion` · Estado: aprobado, pendiente de ejecución.
>
> Esta etapa **no modifica código todavía**. Define cómo se trabajará: Flyway, nuevas tablas y relaciones, endpoints nuevos y modificados, flujo de clientes/ventas/facturación y frontend Angular web.

## 1. Resumen ejecutivo

El proyecto pasa de un **módulo de inventario** a un **ERP comercial**. Se mantiene el backend Java + Spring Boot en `Api/api/` y se agrega un único cliente web en **Angular**.

Cambios de alcance aprobados:

- Se adopta **Flyway** como único mecanismo de cambios de esquema.
- Se agregan **clientes, ventas, detalle de venta, facturación, pagos y secuencias de documentos**.
- Se agrega **imagen por presentación de producto** (por URL).
- Se retira por completo el alcance **Vue** y **móvil con Ionic**.
- Se eliminan los endpoints `/api/app/**` y el encabezado `X-Client-Interface` en código nuevo.
- El único cliente es **Angular web** en `web/`.

## 2. Estado actual

### 2.1 Backend

- Java 21, Spring Boot 4.1.2, Maven Wrapper 3.10.0.
- Código en `Api/api/src/main/java/com/tamachihua_erp/api/`.
- Organización actual por capas: `config`, `security`, `controller`, `service`, `repository`, `entity`, `mapper`, `dto`, `exception`, `util`.
- `spring.jpa.hibernate.ddl-auto=validate`; sin Flyway.
- Una sola prueba de contexto con Testcontainers.

### 2.2 Base de datos

- `Base_datos/01_create_tables.sql` define 13 tablas.
- `Base_datos/02_seed_data.sql` carga datos iniciales y de prueba.
- `Base_datos/Diccionario_de_datos_TAMACHIHUA.xlsx` documenta tipos, restricciones, índices y triggers.
- Tablas: `roles`, `users`, `product_categories`, `product_types`, `product_brands`, `product_models`, `measure_units`, `presentations`, `products`, `product_presentations`, `product_prices`, `suppliers`, `product_lots`.
- La existencia no se almacena: se deriva de `SUM(available_quantity)` de lotes activos.
- `product_lots.total_cost` es columna generada `STORED`.

### 2.3 Brechas

1. No hay versionado del esquema.
2. No existen clientes, ventas, facturación ni pagos.
3. No hay imagen por presentación.
4. No hay cálculo de IVA ni numeración fiscal.
5. No hay pruebas de integración, concurrencia ni CI/CD.
6. La documentación menciona Vue, Ionic y endpoints móviles que ya no aplican.

## 3. Decisiones aprobadas

| ID | Decisión |
|---|---|
| D-01 | Adoptar **Flyway** con baseline del esquema actual. |
| D-02 | Los precios actuales **incluyen IVA 13 %**; se agrega `tax_rate` por producto. |
| D-03 | Documentos: **factura consumidor final, CCF y nota de crédito**, con numeración por secuencia. |
| D-04 | Consumo de stock **FEFO** y exclusión de lotes vencidos al vender. |
| D-05 | Paquetes por funcionalidad para módulos nuevos; migración incremental del código existente. |
| D-06 | Imagen por presentación mediante `image_url`; carga de archivos con almacenamiento local/S3 en fase posterior. |
| D-07 | Frontend **Angular standalone + signals**; Material o Tailwind se decide antes de iniciar. |
| D-08 | Mantener **stock físico** y agregar **stock vendible** (sin lotes vencidos). |

## 4. Arquitectura objetivo

### 4.1 Estilo

Monolito modular. No microservicios: venta, facturación e inventario deben compartir una transacción atómica.

### 4.2 Estructura de código

```text
com.tamachihua_erp.api
├── config/          # Seguridad, OpenAPI, configuración
├── security/        # JWT, user details, errores de seguridad
├── shared/          # DTO comunes, excepciones, utilidades
├── catalog/         # Catálogos, productos, presentaciones, proveedores
├── inventory/       # Lotes, precios, stock, dashboard, reportes
├── customer/        # Clientes
├── sale/            # Ventas y detalle de venta
└── billing/         # Facturas, notas de crédito, pagos
```

Reglas:

- Los módulos nuevos nacen con estructura por funcionalidad.
- El código existente se migra cuando se toque; no hay refactor masivo.
- No se agrega código nuevo a los paquetes globales por capa.
- Cada módulo replica `controller`, `dto`, `entity`, `mapper`, `repository`, `service` y `service/impl`.

### 4.3 Transacciones

- `@Transactional(readOnly = true)` a nivel de servicio.
- `@Transactional` en confirmar venta, emitir factura, registrar pago y cancelar.
- Bloqueo pesimista (`PESSIMISTIC_WRITE`) sobre lotes al confirmar venta.
- Venta, detalle, asignación de lotes y factura se guardan en una sola transacción cuando corresponde.

## 5. Modelo de datos

### 5.1 Cambios a tablas existentes

#### `product_presentations`

| Columna | Tipo | Nulo | Regla |
|---|---|---|---|
| `image_url` | `VARCHAR(500)` | Sí | URL o ruta relativa de la imagen |
| `image_alt` | `VARCHAR(150)` | Sí | Texto alternativo |

No se guardan binarios en PostgreSQL. La carga de archivos se resuelve con almacenamiento externo en una fase posterior.

#### `products`

| Columna | Tipo | Nulo | Regla |
|---|---|---|---|
| `tax_rate` | `NUMERIC(5,2)` | No | Default `13.00`; `CHECK (tax_rate >= 0 AND tax_rate <= 100)` |

Los productos exentos usan `0.00`.

### 5.2 Nuevas tablas

#### `customers`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `customer_type` | `VARCHAR(20)` | `NATURAL` o `BUSINESS` |
| `document_type` | `VARCHAR(10)` | `DUI`, `NIT`, `PASSPORT`, `FOREIGN`; nullable |
| `document_number` | `VARCHAR(20)` | Único cuando no es nulo |
| `legal_name` | `VARCHAR(200)` | Obligatorio |
| `trade_name` | `VARCHAR(200)` | Opcional |
| `nit` | `VARCHAR(17)` | Único cuando no es nulo |
| `nrc` | `VARCHAR(10)` | Único cuando no es nulo |
| `dui` | `VARCHAR(10)` | Único cuando no es nulo |
| `address` | `VARCHAR(255)` | Opcional |
| `phone` | `VARCHAR(20)` | Opcional |
| `email` | `VARCHAR(150)` | Único sin distinguir mayúsculas cuando no es nulo |
| `credit_limit` | `NUMERIC(12,2)` | Default `0`; `CHECK >= 0` |
| `credit_days` | `INTEGER` | Default `0`; `CHECK >= 0` |
| `is_active` | `BOOLEAN` | Default `TRUE` |
| `created_at`, `updated_at` | `TIMESTAMP` | Auditoría |

Índices: `lower(legal_name)`, `document_number`, `email`, `is_active`.

#### `document_sequences`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `document_type` | `VARCHAR(20)` | `SALE`, `INVOICE`, `CCF`, `CREDIT_NOTE` |
| `prefix` | `VARCHAR(10)` | Ej. `V-`, `F-`, `CCF-`, `NC-` |
| `year` | `INTEGER` | Año del correlativo |
| `current_number` | `BIGINT` | Último número emitido |
| `version` | `BIGINT` | Control de concurrencia |
| `created_at`, `updated_at` | `TIMESTAMP` | Auditoría |

Restricción única: `(document_type, year)`. El número se genera con `SELECT ... FOR UPDATE` dentro de la transacción.

#### `sales`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `sale_number` | `VARCHAR(30)` | Único |
| `customer_id` | `BIGINT` | FK `customers`; nullable para consumidor final |
| `user_id` | `BIGINT` | FK `users`; vendedor tomado del JWT |
| `sale_date` | `TIMESTAMP` | Fecha de la venta |
| `status` | `VARCHAR(20)` | `DRAFT`, `CONFIRMED`, `CANCELLED` |
| `payment_condition` | `VARCHAR(20)` | `CASH` o `CREDIT` |
| `payment_method` | `VARCHAR(20)` | `CASH`, `CARD`, `TRANSFER`, `OTHER`; nullable |
| `subtotal` | `NUMERIC(12,2)` | `CHECK >= 0` |
| `discount_total` | `NUMERIC(12,2)` | Default `0`; `CHECK >= 0` |
| `tax_total` | `NUMERIC(12,2)` | Default `0`; `CHECK >= 0` |
| `total` | `NUMERIC(12,2)` | `CHECK >= 0` |
| `paid_total` | `NUMERIC(12,2)` | Default `0`; `CHECK >= 0` |
| `notes` | `VARCHAR(255)` | Opcional |
| `cancelled_at` | `TIMESTAMP` | Opcional |
| `cancelled_by` | `BIGINT` | FK `users`; opcional |
| `cancel_reason` | `VARCHAR(255)` | Opcional |
| `created_at`, `updated_at` | `TIMESTAMP` | Auditoría |

Índices: `customer_id`, `user_id`, `sale_date`, `status`, `sale_number`.

#### `sale_details`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `sale_id` | `BIGINT` | FK `sales` |
| `product_presentation_id` | `BIGINT` | FK `product_presentations` |
| `quantity` | `NUMERIC(12,2)` | `CHECK > 0` |
| `unit_price` | `NUMERIC(10,2)` | Precio congelado al momento de la venta |
| `discount` | `NUMERIC(12,2)` | Default `0`; `CHECK >= 0` |
| `tax_rate` | `NUMERIC(5,2)` | Default `13.00` |
| `tax_amount` | `NUMERIC(12,2)` | Default `0` |
| `line_subtotal` | `NUMERIC(12,2)` | Base sin IVA |
| `line_total` | `NUMERIC(12,2)` | Total de la línea |
| `created_at`, `updated_at` | `TIMESTAMP` | Auditoría |

Restricción única: `(sale_id, product_presentation_id)`.

#### `sale_lot_allocations`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `sale_detail_id` | `BIGINT` | FK `sale_details` |
| `product_lot_id` | `BIGINT` | FK `product_lots` |
| `quantity` | `NUMERIC(12,2)` | `CHECK > 0` |
| `unit_cost` | `NUMERIC(10,2)` | Costo congelado del lote |
| `created_at` | `TIMESTAMP` | Auditoría |

Restricción única: `(sale_detail_id, product_lot_id)`. Permite devolver stock al lote correcto si la venta se cancela.

#### `invoices`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `invoice_number` | `VARCHAR(30)` | Único |
| `control_number` | `VARCHAR(30)` | Único; nullable, para CCF preimpreso |
| `invoice_type` | `VARCHAR(20)` | `CONSUMER`, `CCF`, `CREDIT_NOTE` |
| `sale_id` | `BIGINT` | FK `sales`; nullable para nota de crédito |
| `customer_id` | `BIGINT` | FK `customers`; nullable |
| `related_invoice_id` | `BIGINT` | FK `invoices`; solo nota de crédito |
| `issue_date` | `DATE` | Fecha de emisión |
| `due_date` | `DATE` | Opcional; obligatoria en crédito |
| `status` | `VARCHAR(20)` | `DRAFT`, `ISSUED`, `CANCELLED` |
| `payment_condition` | `VARCHAR(20)` | `CASH` o `CREDIT` |
| `subtotal`, `discount_total`, `tax_total`, `total`, `paid_total` | `NUMERIC(12,2)` | Totales |
| `customer_name` | `VARCHAR(200)` | Snapshot fiscal |
| `customer_document_type`, `customer_document_number`, `customer_nit`, `customer_nrc`, `customer_address`, `customer_email` | Varios | Snapshot fiscal |
| `notes` | `VARCHAR(255)` | Opcional |
| `cancelled_at`, `cancelled_by`, `cancel_reason` | Varios | Cancelación |
| `created_by` | `BIGINT` | FK `users` |
| `created_at`, `updated_at` | `TIMESTAMP` | Auditoría |

Índices: `customer_id`, `issue_date`, `status`, `sale_id`.
Índice único parcial en `sale_id` cuando no es nulo: una factura por venta.
La factura emitida es **inmutable**: no se edita; se cancela con nota de crédito.

#### `invoice_details`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `invoice_id` | `BIGINT` | FK `invoices` |
| `product_presentation_id` | `BIGINT` | FK opcional |
| `product_code`, `product_name`, `presentation_code`, `presentation_name` | `VARCHAR` | Snapshot para conservar historial |
| `quantity`, `unit_price`, `discount`, `tax_rate`, `tax_amount`, `line_subtotal`, `line_total` | `NUMERIC` | Snapshot de la línea |
| `unit_cost` | `NUMERIC(10,2)` | Opcional; solo uso interno |
| `created_at` | `TIMESTAMP` | Auditoría |

#### `invoice_payments`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `invoice_id` | `BIGINT` | FK `invoices` |
| `payment_date` | `DATE` | No futura |
| `amount` | `NUMERIC(12,2)` | `CHECK > 0` |
| `payment_method` | `VARCHAR(20)` | `CASH`, `CARD`, `TRANSFER`, `CHECK`, `OTHER` |
| `reference` | `VARCHAR(50)` | Opcional |
| `notes` | `VARCHAR(255)` | Opcional |
| `user_id` | `BIGINT` | FK `users`; tomado del JWT |
| `created_at` | `TIMESTAMP` | Auditoría |

Regla: la suma de pagos no puede superar el total de la factura.

### 5.3 Relaciones nuevas

```text
customers 1 ── N sales
users     1 ── N sales
sales     1 ── N sale_details
sale_details 1 ── N sale_lot_allocations
product_lots 1 ── N sale_lot_allocations
sales     1 ── 1 invoices
customers 1 ── N invoices
invoices  1 ── N invoice_details
invoices  1 ── N invoice_payments
invoices  1 ── 1 invoices (related_invoice_id, nota de crédito)
```

### 5.4 Tablas futuras (fuera de esta fase)

- `inventory_movements` para kardex completo.
- `purchases` y `purchase_details`.
- `accounts_payable`.
- `sales_returns` si se separa la devolución de la nota de crédito.
- `media_assets` para carga de archivos con almacenamiento externo.
- `audit_events` para auditoría funcional.

## 6. API

### 6.1 Clientes

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/customers` | ADMIN, PURCHASING, SELLER | Filtros `search`, `isActive`, `documentType`; paginado |
| `GET /api/customers/options` | ADMIN, SELLER | Clientes activos para combos |
| `GET /api/customers/{id}` | ADMIN, PURCHASING, SELLER | Detalle |
| `POST /api/customers` | ADMIN, SELLER | Crear |
| `PUT /api/customers/{id}` | ADMIN, SELLER | Editar datos básicos |
| `PATCH /api/customers/{id}/status` | ADMIN | Activar / desactivar |
| `PATCH /api/customers/{id}/credit` | ADMIN | Actualizar límite y días de crédito |

### 6.2 Ventas

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/sales` | ADMIN, WAREHOUSE, PURCHASING, SELLER | Filtros por fecha, cliente, estado, vendedor; paginado |
| `GET /api/sales/{id}` | ADMIN, WAREHOUSE, PURCHASING, SELLER | Detalle con líneas |
| `POST /api/sales` | ADMIN, SELLER | Crear venta en `DRAFT` |
| `PUT /api/sales/{id}` | ADMIN, SELLER | Editar venta en `DRAFT` |
| `POST /api/sales/{id}/confirm` | ADMIN, SELLER | Validar stock, asignar lotes FEFO y confirmar |
| `POST /api/sales/{id}/cancel` | ADMIN | Cancelar y devolver stock si aplica |
| `POST /api/sales/{id}/invoice` | ADMIN, SELLER | Emitir factura de la venta |

Visibilidad: `SELLER` solo ve sus propias ventas; `ADMIN` ve todas.

### 6.3 Facturación y pagos

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/invoices` | ADMIN, PURCHASING, SELLER | Filtros por fecha, cliente, tipo, estado; paginado |
| `GET /api/invoices/{id}` | ADMIN, PURCHASING, SELLER | Detalle con snapshot |
| `POST /api/invoices` | ADMIN, SELLER | Emitir factura desde una venta |
| `POST /api/invoices/{id}/payments` | ADMIN, SELLER | Registrar pago |
| `GET /api/invoices/{id}/payments` | ADMIN, PURCHASING, SELLER | Historial de pagos |
| `POST /api/invoices/{id}/credit-note` | ADMIN | Emitir nota de crédito y cancelar factura |
| `GET /api/invoices/{id}/pdf` | ADMIN, SELLER | Fase posterior |

### 6.4 Imágenes

- `imageUrl` e `imageAlt` se agregan a los DTO de presentación del producto.
- Se exponen en el detalle y en los listados que incluyan presentaciones.
- Fase posterior: `PUT /api/products/{productId}/presentations/{id}/image` con `multipart/form-data`.
- No se permite subir binarios a PostgreSQL.

### 6.5 Endpoints que se retiran

- Todos los `/api/app/**`.
- El encabezado `X-Client-Interface` y la restricción de interfaz asociada.
- Referencias a panel Vue e app Ionic en documentación y código nuevo.

### 6.6 Seguridad

- Actualizar `SecurityConfig` con la matriz nueva.
- `SELLER` no ve costos ni proveedores.
- `WAREHOUSE` no emite facturas ni registra pagos.
- `PURCHASING` consulta ventas y facturas para reportes; no las crea.
- El `userId` siempre sale del JWT.

## 7. Flujos principales

### 7.1 Crear venta

1. Validar cliente activo y al menos una línea.
2. Obtener precio vigente de cada presentación.
3. Calcular `line_subtotal`, IVA (`tax_rate`), descuento y total.
4. Guardar venta en `DRAFT` con número generado por secuencia.

### 7.2 Confirmar venta

1. Validar que la venta esté en `DRAFT` y tenga líneas.
2. Validar cliente activo; si es crédito, validar días y que `saldo pendiente + total` no supere `credit_limit`. Saldo pendiente = suma de `(total - paid_total)` de facturas emitidas del cliente.
3. Para cada línea:
   - Buscar lotes activos con `available_quantity > 0`.
   - Excluir lotes vencidos (`expiration_date < CURRENT_DATE`). Es el **stock vendible** de D-08.
   - Ordenar por `expiration_date ASC NULLS LAST`, `entry_date ASC`, `id ASC` (FEFO).
   - Bloquear los lotes con `PESSIMISTIC_WRITE`.
   - Si la suma disponible es menor a la cantidad, error 409 `INSUFFICIENT_STOCK`.
   - Descontar `available_quantity` y crear `sale_lot_allocations`.
4. Recalcular totales y guardar la venta como `CONFIRMED`.

### 7.3 Emitir factura

1. Validar venta `CONFIRMED` y sin factura emitida.
2. Generar número con `document_sequences`.
3. Copiar datos del cliente y de las líneas a `invoices` e `invoice_details` (snapshot).
4. Calcular IVA y totales.
5. Guardar como `ISSUED`.
6. Si es crédito, calcular `due_date = issue_date + credit_days`.

### 7.4 Registrar pago

1. Validar factura `ISSUED`.
2. Validar `amount > 0` y que `paid_total + amount <= total`.
3. Guardar pago y actualizar `paid_total`.
4. Si `paid_total == total`, la factura queda pagada; si no, queda pendiente.

### 7.5 Cancelar venta o factura

- Venta sin factura emitida: devolver cantidades a los lotes usando `sale_lot_allocations` y marcar `CANCELLED`.
- Venta con factura emitida: no se cancela directamente; se emite nota de crédito.
- Nota de crédito: referencia la factura original, copia líneas, marca la factura como `CANCELLED` y devuelve stock solo si el usuario indica devolución física.
- Toda cancelación guarda usuario, fecha y motivo.

## 8. Flyway y migraciones

### 8.1 Estrategia

- Agregar `flyway-core` y `flyway-database-postgresql`.
- Crear `Api/api/src/main/resources/db/migration/`.
- Mantener `spring.jpa.hibernate.ddl-auto=validate`.
- `Base_datos/01_create_tables.sql` y `02_seed_data.sql` quedan como **referencia histórica**.

### 8.2 Migraciones planificadas

| Versión | Archivo | Contenido |
|---|---|---|
| `V1` | `V1__baseline_inventory.sql` | Esquema actual de 13 tablas, triggers, índices y datos base mínimos |
| `V2` | `V2__customers.sql` | Tabla `customers` e índices |
| `V3` | `V3__sales.sql` | `document_sequences`, `sales`, `sale_details`, `sale_lot_allocations` |
| `V4` | `V4__invoicing.sql` | `invoices`, `invoice_details`, `invoice_payments` |
| `V5` | `V5__product_presentation_images.sql` | `image_url`, `image_alt` |
| `V6` | `V6__product_tax_rate.sql` | `tax_rate` en `products` y datos iniciales de secuencias |

Reglas:

- Ninguna migración usa `DROP` ni `TRUNCATE`.
- Los datos base (roles, usuario administrador inicial, secuencias) van en migraciones o seeds controlados.
- En una base existente se usa baseline de Flyway en `V1`.
- Los tests con Testcontainers dejan que Flyway cree el esquema desde cero.

### 8.3 Datos iniciales

- Secuencias para `SALE`, `INVOICE`, `CCF` y `CREDIT_NOTE`.
- Cliente genérico "Consumidor Final" si se decide manejarlo como registro.
- Datos de ventas de prueba solo en un seed de desarrollo.

## 9. Frontend Angular (preparación)

### 9.1 Ubicación

```text
web/
├── src/app/
│   ├── core/            # auth, interceptores, guards, configuración
│   ├── shared/          # componentes, pipes, directivas, modelos
│   ├── layout/
│   └── features/
│       ├── auth/
│       ├── catalog/
│       ├── inventory/
│       ├── customers/
│       ├── sales/
│       └── billing/
├── environments/
└── ...
```

### 9.2 Convenciones

- Angular standalone components y signals.
- Servicios `HttpClient` tipados.
- Guards e interceptores funcionales.
- Interceptor con `Authorization: Bearer <token>`; sin `X-Client-Interface`.
- Tipos TypeScript espejo de los DTO del backend; prohibido `any`.
- Rutas protegidas por rol; el backend sigue siendo la autoridad.
- Formularios reactivos con validaciones alineadas al backend.
- UI en español.

### 9.3 Pantallas iniciales

- Login.
- Catálogos y productos.
- Clientes.
- Ventas: listado, nueva venta, detalle, confirmar/cancelar.
- Facturación: listado, detalle, pagos, nota de crédito.
- Inventario: lotes y dashboard.

## 10. Testing y CI/CD

### 10.1 Backend

- Unitarias: IVA, totales, FEFO, numeración y límite de crédito.
- Integración con Testcontainers + Flyway: repositorios y servicios.
- MockMvc: validaciones, roles y errores.
- Concurrencia: dos ventas simultáneas no deben sobrevender.
- Comando: `cd Api/api && ./mvnw -B verify`.

### 10.2 Frontend

- Unitarias de servicios, guards y componentes críticos.
- Lint y formato.
- Build de producción.
- E2E opcional en fase posterior.

### 10.3 CI

- GitHub Actions:
  - Backend: JDK 21, cache Maven, `./mvnw -B verify`.
  - Frontend: Node, `npm ci`, lint, tests y build.
- PostgreSQL vía Testcontainers; no requiere servicio externo.
- Opcional: imagen Docker al publicar.

### 10.4 Entorno local

- `docker-compose.yml` con PostgreSQL 16.
- `.env.example` con `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET`.
- Perfil `local` para desarrollo; nunca secretos reales en el repositorio.

## 11. Fases de entrega

| Fase | Entregable |
|---|---|
| 0 | Aprobación del plan y de las decisiones D-01 a D-08 |
| 1 | Documentación base: `AGENTS.md`, `MEMORY.md`, `Implementacion.md`, README y contrato v2.0 |
| 2 | Flyway baseline y migraciones de clientes, ventas, facturación e imágenes |
| 3 | CRUD de clientes con validaciones y seguridad |
| 4 | Ventas: crear, editar, confirmar, cancelar, FEFO y concurrencia |
| 5 | Facturación: emisión, numeración, snapshot, pagos y notas de crédito |
| 6 | Imagen por presentación: columnas, DTOs y endpoint de carga |
| 7 | Frontend Angular: base, auth, clientes, ventas y facturación |
| 8 | Reportes: ventas por período, cuentas por cobrar y productos más vendidos |
| 9 | Endurecimiento: auditoría, índices, rendimiento y documentación final |

## 12. Criterios de aceptación

- Flyway migra una base vacía y la API arranca con `ddl-auto=validate`.
- Los endpoints actuales siguen funcionando.
- Cliente CRUD con documentos únicos y validaciones.
- Una venta confirmada descuenta stock de lotes no vencidos en orden FEFO.
- No se puede sobrevender en pruebas concurrentes.
- La factura guarda snapshot del cliente y de las líneas.
- El número de factura es único y consecutivo por tipo/año.
- Los pagos no superan el total de la factura.
- El límite de crédito se respeta.
- `SELLER` no ve costos; `WAREHOUSE` no factura.
- `imageUrl` se devuelve en las presentaciones.
- No quedan referencias a Vue, Ionic, `/api/app/**` ni `X-Client-Interface`.
- Tests y CI en verde.
- `MEMORY.md` actualizado.

## 13. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Requisitos fiscales cambiantes | Aislar cálculo y numeración en servicios configurables |
| Sobreventa por concurrencia | Bloqueo pesimista y pruebas concurrentes |
| Deriva del esquema | Flyway + `validate` + CI |
| Refactor masivo riesgoso | Migración incremental por módulo |
| Docker no disponible en local | Documentar requisito; CI con Testcontainers |
| Datos existentes inconsistentes | Script de verificación y migración controlada |
| Vender lotes vencidos | Regla FEFO y exclusión de vencidos |
| Mala definición de precios con IVA | `tax_rate` por producto y pruebas de cálculo |

## 14. Documentos de esta etapa

1. `AGENTS.md`: reglas para agentes.
2. `MEMORY.md`: memoria viva del proyecto.
3. `Implementacion.md`: este documento.
4. `README.md`: presentación y arranque del proyecto.
5. `Api/README.md`: guía del backend, Flyway y endpoints.
6. `Documentacion/Diagramas/Endpoints-API.md`: contrato REST v2.0.
7. `Base_datos/`: referencia histórica del esquema anterior al baseline.
