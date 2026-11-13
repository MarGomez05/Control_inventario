# TAMACHIHUA-ERP · Contrato de APIs REST

**Versión:** 2.0 · **Backend:** Java + Spring Boot + PostgreSQL + Flyway · **Cliente único:** Angular web
**Base:** Product Backlog v1.2, MER del inventario y plan aprobado en `Implementacion.md`.

> **Cambio de alcance:** se elimina la app móvil (Ionic) y el panel Vue. El único cliente es **Angular web**. Se retiran `/api/app/**` y el encabezado `X-Client-Interface`.

---

## 1. Convenciones generales

| Tema | Regla |
|---|---|
| Base path | `/api/...` para todos los recursos |
| Formato | JSON (`application/json`, UTF-8). Campos en `camelCase`; tablas y columnas en `snake_case` inglés |
| Fechas | `DATE` → `yyyy-MM-dd`; `TIMESTAMP` → ISO-8601 (`2026-10-30T14:05:00`) |
| Decimales | Números JSON (no strings). Redondeo `HALF_UP` a 2 decimales en precios, costos y totales |
| Autenticación | `Authorization: Bearer <JWT>` (BCrypt + JWT HS256). Token de 1 hora, sin refresh token |
| Borrado | **Lógico** (`is_active`). No existe `DELETE`; se usa `PATCH /{id}/status` |
| Mensajes | En español, comprensibles para el usuario final |
| Auditoría | `userId` se toma **siempre del token**, nunca del body |
| Esquema | Controlado por **Flyway**. La API arranca con `spring.jpa.hibernate.ddl-auto=validate` |
| Paginación | `page` (base 0, default 0), `size` (default 20, máximo 100), `sort` (`campo,asc\|desc`) |

### 1.1 Roles

| Código | Rol |
|---|---|
| `ADMIN` | Administrador (gerencia / soporte) |
| `WAREHOUSE` | Bodega |
| `PURCHASING` | Compras |
| `SELLER` | Vendedor |

Los roles **no son jerárquicos**. Cada endpoint declara explícitamente qué roles acepta (`hasAnyRole`). Los permisos se validan en la API, no solo en el menú.

### 1.2 Códigos HTTP

| Código | Uso |
|---|---|
| 200 / 201 / 204 | Consulta OK / creación OK / sin contenido |
| 400 | Validación fallida. Incluye `errors[]` |
| 401 | Sin token, token vencido o credenciales incorrectas |
| 403 | Autenticado pero sin permiso |
| 404 | Recurso no existe |
| 409 | Conflicto de unicidad, estado o stock insuficiente |
| 422 | Regla de negocio no aplicable sobre datos válidos (opcional) |

### 1.3 Formato de error

```json
{
  "timestamp": "2026-10-30T14:05:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Hay campos inválidos",
  "path": "/api/customers",
  "errors": [
    { "field": "legalName", "message": "La razón social es obligatoria" },
    { "field": "documentNumber", "message": "El documento ya está registrado" }
  ]
}
```

Valores de `error`: `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INSUFFICIENT_STOCK`, `BAD_REQUEST`, `INTERNAL_SERVER_ERROR`, `METHOD_NOT_ALLOWED`. `errors[]` solo aparece en 400.

### 1.4 Respuesta paginada

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 134, "totalPages": 7 }
```

### 1.5 Patrón CRUD de catálogos

| Operación | Método y ruta | Éxito |
|---|---|---|
| Listar (filtros `search`, `isActive`, paginado) | `GET /api/{recurso}` | 200 |
| Opciones para combos (activos, sin paginar) | `GET /api/{recurso}/options` → `[{id, name}]` | 200 |
| Detalle | `GET /api/{recurso}/{id}` | 200 |
| Crear | `POST /api/{recurso}` | 201 + `Location` |
| Editar | `PUT /api/{recurso}/{id}` | 200 |
| Activar / desactivar | `PATCH /api/{recurso}/{id}/status` body `{ "isActive": false }` | 204 |

---

## 2. Matriz de permisos

| Recurso | ADMIN | WAREHOUSE | PURCHASING | SELLER |
|---|:-:|:-:|:-:|:-:|
| Auth (`login`, `me`) | ✔ | ✔ | ✔ | ✔ |
| Usuarios y roles | ✔ | – | – | – |
| Catálogos (escritura) | ✔ | – | – | – |
| Catálogos (lectura / options) | ✔ | ✔ | ✔ | – |
| Productos y presentaciones (escritura) | ✔ | ✔ | – | – |
| Productos (lectura) | ✔ | ✔ | ✔ | ✔ |
| Precios: cambiar y ver historial | ✔ | – | – | – |
| Proveedores | ✔ | – | ✔ | – |
| Lotes: registrar y corregir | ✔ | ✔ | – | – |
| Lotes: consultar | ✔ | ✔ | ✔ | – |
| Lotes: ver costos | ✔ | – | ✔ | – |
| Dashboard | ✔ | ✔ | parcial | – |
| Reporte de existencias | ✔ | – | – | – |
| Clientes: crear y editar | ✔ | – | – | ✔ |
| Clientes: consultar | ✔ | – | ✔ | ✔ |
| Clientes: crédito | ✔ | – | – | – |
| Ventas: crear, confirmar y editar borrador | ✔ | – | – | ✔ |
| Ventas: consultar | ✔ | ✔ | ✔ | propias |
| Ventas: cancelar | ✔ | – | – | – |
| Facturas: emitir | ✔ | – | – | ✔ |
| Facturas: consultar | ✔ | – | ✔ | ✔ |
| Pagos: registrar | ✔ | – | – | ✔ |
| Nota de crédito | ✔ | – | – | – |

---

## 3. Seguridad y autenticación

| Método y ruta | Roles | Descripción |
|---|---|---|
| `POST /api/auth/login` | Público | Login del cliente Angular |
| `GET /api/auth/me` | Autenticado | Datos del usuario en sesión |
| `GET /api/roles` | ADMIN | Roles para combos |
| `GET /api/users` | ADMIN | Filtros: `search`, `roleId`, `isActive` |
| `GET /api/users/{id}` | ADMIN | Detalle |
| `POST /api/users` | ADMIN | Crear usuario (sin autorregistro) |
| `PUT /api/users/{id}` | ADMIN | Editar datos y rol (no cambia contraseña) |
| `PATCH /api/users/{id}/status` | ADMIN | Activar / desactivar |

**LoginRequest:** `username` (obligatorio, ≤ 50; acepta usuario o correo) y `password` (obligatorio).

**LoginResponse (200):**

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": 3, "username": "jperez", "fullName": "Juan Pérez", "role": "SELLER" }
}
```

Reglas:

- Credenciales incorrectas o usuario inactivo → 401 con mensaje genérico `"Credenciales inválidas"`.
- El cierre de sesión lo hace el cliente borrando el token. No existe `POST /api/auth/logout`.
- Un usuario desactivado conserva su token hasta que venza (máximo 1 hora).
- Sin token válido, toda operación protegida responde 401.

**UserCreateRequest:** `username` (único, ≤ 50), `password` (≥ 8, BCrypt), `fullName` (≤ 150), `email` (opcional, ≤ 150), `roleId`.
**UserUpdateRequest:** `fullName`, `email`, `roleId`.
**UserResponse:** `id`, `username`, `fullName`, `email`, `role {id, name}`, `isActive`, `createdAt`, `updatedAt`.

Un administrador no puede desactivarse a sí mismo ni dejar el sistema sin un `ADMIN` activo (409).

---

## 4. Catálogos

Patrón de la sección 1.5. Escritura: `ADMIN`. Lectura y `options`: `ADMIN`, `WAREHOUSE`, `PURCHASING`, `SELLER`.

### 4.1 Categorías · `/api/categories`

| Campo | Tipo | Validación |
|---|---|---|
| `name` | string | Obligatorio, ≤ 100, único sin distinguir mayúsculas |
| `description` | string | Opcional, ≤ 255 |

### 4.2 Tipos de producto · `/api/product-types`

| Campo | Tipo | Validación |
|---|---|---|
| `name` | string | Obligatorio, ≤ 100, único sin distinguir mayúsculas |
| `description` | string | Opcional, ≤ 255 |

### 4.3 Marcas · `/api/brands`

| Campo | Tipo | Validación |
|---|---|---|
| `name` | string | Obligatorio, ≤ 100, único |

### 4.4 Modelos · `/api/models`

Listado filtrable por `brandId`.

| Campo | Tipo | Validación |
|---|---|---|
| `brandId` | long | Obligatorio, marca existente y activa |
| `name` | string | Obligatorio, ≤ 100, único dentro de la marca |

La respuesta incluye `brand {id, name}`.

### 4.5 Unidades de medida · `/api/measure-units`

| Campo | Tipo | Validación |
|---|---|---|
| `name` | string | Obligatorio, ≤ 50, único |
| `code` | string | Obligatorio, ≤ 10, único |

### 4.6 Presentaciones · `/api/presentations`

| Campo | Tipo | Validación |
|---|---|---|
| `name` | string | Obligatorio, ≤ 100, único |
| `measureUnitId` | long | Obligatorio, unidad existente y activa |
| `quantity` | decimal(10,2) | Obligatorio, > 0 |

La respuesta incluye `measureUnit {id, name, code}`.

Regla común: no se puede desactivar un catálogo con hijos activos → 409.

---

## 5. Productos

### 5.1 Productos · `/api/products`

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/products` | Todos | Filtros: `search`, `categoryId`, `productTypeId`, `isActive`; paginado |
| `GET /api/products/{id}` | Todos | Detalle con presentaciones |
| `POST /api/products` | ADMIN, WAREHOUSE | Crear |
| `PUT /api/products/{id}` | ADMIN, WAREHOUSE | Editar |
| `PATCH /api/products/{id}/status` | ADMIN, WAREHOUSE | Activar / desactivar |

**ProductRequest**

| Campo | Tipo | Validación |
|---|---|---|
| `code` | string | Obligatorio, ≤ 50, único (409) |
| `name` | string | Obligatorio, ≤ 150 |
| `description` | string | Opcional, ≤ 255 |
| `categoryId` | long | Obligatorio, categoría activa |
| `productTypeId` | long | Obligatorio, tipo activo |
| `productModelId` | long | Opcional |
| `requiresExpiration` | boolean | Obligatorio. No se cambia a `false` si tiene lotes activos (409) |
| `taxRate` | decimal(5,2) | Obligatorio, entre 0 y 100; default 13.00 |

**ProductDetail (extracto)**

```json
{
  "id": 12, "code": "CLV-001", "name": "Clavo 2 pulgadas",
  "requiresExpiration": false, "taxRate": 13.00, "isActive": true,
  "presentations": [
    {
      "id": 31, "code": "CLV-001-CJ",
      "presentation": { "id": 4, "name": "Caja", "measureUnit": "Libra", "quantity": 25.00 },
      "minStock": 10.00, "currentPrice": 18.50,
      "stock": 40.00, "sellableStock": 35.00, "stockStatus": "OK",
      "imageUrl": "/media/products/clavo-caja.jpg", "imageAlt": "Caja de clavos",
      "isActive": true
    }
  ]
}
```

Reglas de stock:

- `stock` (físico) = suma de `available_quantity` de lotes activos.
- `sellableStock` = suma de lotes activos **no vencidos** (D-08).
- `stockStatus`: `OUT` si `sellableStock = 0`; `LOW` si `sellableStock ≤ minStock`; `OK` en otro caso.
- `currentPrice` es `null` si no hay precio vigente.

### 5.2 Presentaciones del producto · `/api/products/{productId}/presentations`

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/products/{productId}/presentations` | Todos | Lista con stock, mínimo y precio |
| `POST /api/products/{productId}/presentations` | ADMIN, WAREHOUSE | Asignar presentación |
| `PUT /api/products/{productId}/presentations/{id}` | ADMIN, WAREHOUSE | Editar código, mínimo e imagen |
| `PATCH /api/products/{productId}/presentations/{id}/status` | ADMIN, WAREHOUSE | Activar / desactivar |

| Campo | Tipo | Validación |
|---|---|---|
| `presentationId` | long | Obligatorio al crear; combinación única producto + presentación |
| `code` | string | Obligatorio, ≤ 50, único en el sistema |
| `minStock` | decimal(12,2) | Obligatorio, > 0 |
| `imageUrl` | string | Opcional, ≤ 500, URL o ruta relativa |
| `imageAlt` | string | Opcional, ≤ 150 |

Fase posterior: `PUT /api/products/{productId}/presentations/{id}/image` con `multipart/form-data`. No se guardan binarios en PostgreSQL.

### 5.3 Precios

| Método y ruta | Roles | Descripción |
|---|---|---|
| `POST /api/products/{productId}/presentations/{id}/prices` | ADMIN | Cambiar precio (201) |
| `GET /api/products/{productId}/presentations/{id}/prices` | ADMIN | Historial, más reciente primero |

**PriceRequest:** `{ "price": 19.75 }` → obligatorio, > 0, `decimal(10,2)`.

Reglas: cierra el precio vigente (`end_date = now`) y crea el nuevo con `user_id` del token. Solo un precio vigente por presentación (índice único parcial). Conflicto concurrente → 409.

**PriceHistoryItem:** `id`, `price`, `startDate`, `endDate` (null = vigente), `changedBy {id, fullName}`.

---

## 6. Proveedores · `/api/suppliers`

Patrón CRUD. Roles: `ADMIN`, `PURCHASING`. Filtros: `search`, `isActive`.

| Campo | Tipo | Validación |
|---|---|---|
| `tradeName` | string | Obligatorio, ≤ 150 |
| `legalName` | string | Obligatorio, ≤ 150 |
| `nit` | string | Obligatorio, único, ≤ 17. NIT de 14 dígitos o DUI como NIT |
| `nrc` | string | Opcional, único, ≤ 10 |
| `address` | string | Opcional, ≤ 255 |
| `phone` | string | Opcional, ≤ 20 |
| `email` | string | Opcional, ≤ 150, único |
| `creditDays` | integer | Obligatorio, ≥ 0 |

---

## 7. Lotes · `/api/lots`

| Método y ruta | Roles | Descripción |
|---|---|---|
| `POST /api/lots` | ADMIN, WAREHOUSE | Registrar ingreso (201) |
| `GET /api/lots` | ADMIN, WAREHOUSE, PURCHASING | Consulta por vencimiento |
| `GET /api/lots/{id}` | ADMIN, WAREHOUSE, PURCHASING | Detalle |
| `PATCH /api/lots/{id}/available-quantity` | ADMIN, WAREHOUSE | Corregir cantidad |
| `GET /api/products/{productId}/last-cost` | ADMIN, PURCHASING | Último costo por presentación |

**LotRequest**

| Campo | Tipo | Validación |
|---|---|---|
| `productPresentationId` | long | Obligatorio, activo |
| `supplierId` | long | Obligatorio, activo |
| `invoiceNumber` | string | Obligatorio, ≤ 50 |
| `invoiceDate` | date | Obligatorio, no futura |
| `lotNumber` | string | Obligatorio, ≤ 50; único por proveedor + presentación |
| `receivedQuantity` | decimal(12,2) | Obligatorio, > 0 |
| `unitCost` | decimal(10,2) | Obligatorio, ≥ 0 |
| `expirationDate` | date | Obligatoria si `requiresExpiration = true`; posterior al ingreso |
| `notes` | string | Opcional, ≤ 200 |

El servidor calcula `entryDate`, `availableQuantity`, `totalCost` (columna generada), `userId` e `isActive`.

`unitCost` y `totalCost` solo se serializan para `ADMIN` y `PURCHASING` con DTOs separados.

**Corrección:** `PATCH /api/lots/{id}/available-quantity` con `{ "availableQuantity": 38.00 }`. Validación: ≥ 0 y ≤ `receivedQuantity`.

---

## 8. Dashboard

| Método y ruta | Roles | Respuesta |
|---|---|---|
| `GET /api/dashboard/summary` | ADMIN, WAREHOUSE | `{ activeProducts, lowStockProducts, activeLots, expiringLots, expiredLots }` |
| `GET /api/dashboard/low-stock` | ADMIN, WAREHOUSE, PURCHASING | Tabla paginada |
| `GET /api/dashboard/expiring-lots` | ADMIN, WAREHOUSE | Vencidos y por vencer en 30 días |

`LowStockItem`: `productId`, `productCode`, `productName`, `presentationName`, `currentStock`, `sellableStock`, `minStock`, `missing`. Condición: `sellableStock ≤ minStock`.

`ExpiringLotItem`: `lotId`, `productName`, `presentationName`, `lotNumber`, `expirationDate`, `daysRemaining`, `severity`, `label`.

| `severity` | Condición | Etiqueta |
|---|---|---|
| `EXPIRED` | `daysRemaining < 0` | Vencido |
| `RED` | `0` a `15` | Vence pronto |
| `YELLOW` | `16` a `30` | Vence pronto |

---

## 9. Reporte de existencias

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/reports/stock/export?format=PDF\|XLSX&categoryId=` | ADMIN | Descarga el archivo |

Columnas: código, producto, presentación, existencia física, stock vendible, mínimo y precio vigente.

---

## 10. Clientes · `/api/customers`

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/customers` | ADMIN, PURCHASING, SELLER | Filtros: `search`, `isActive`, `documentType`; paginado |
| `GET /api/customers/options` | ADMIN, SELLER | Activos para combos |
| `GET /api/customers/{id}` | ADMIN, PURCHASING, SELLER | Detalle con saldo y crédito |
| `POST /api/customers` | ADMIN, SELLER | Crear |
| `PUT /api/customers/{id}` | ADMIN, SELLER | Editar datos básicos |
| `PATCH /api/customers/{id}/status` | ADMIN | Activar / desactivar |
| `PATCH /api/customers/{id}/credit` | ADMIN | Límite y días de crédito |

**CustomerRequest**

| Campo | Tipo | Validación |
|---|---|---|
| `customerType` | string | `NATURAL` o `BUSINESS` |
| `documentType` | string | `DUI`, `NIT`, `PASSPORT`, `FOREIGN`; opcional |
| `documentNumber` | string | ≤ 20, único si viene |
| `legalName` | string | Obligatorio, ≤ 200 |
| `tradeName` | string | Opcional, ≤ 200 |
| `nit` | string | Opcional, ≤ 17, único |
| `nrc` | string | Opcional, ≤ 10, único |
| `dui` | string | Opcional, ≤ 10, único |
| `address` | string | Opcional, ≤ 255 |
| `phone` | string | Opcional, ≤ 20 |
| `email` | string | Opcional, ≤ 150, único sin distinguir mayúsculas |
| `creditLimit` | decimal(12,2) | ≥ 0, default 0 |
| `creditDays` | integer | ≥ 0, default 0 |

**CustomerResponse (extracto)**

```json
{
  "id": 8, "customerType": "BUSINESS", "documentType": "NIT", "documentNumber": "0614-123456-101-2",
  "legalName": "Agroservicio El Progreso, S.A.", "tradeName": "El Progreso",
  "nit": "0614-123456-101-2", "nrc": "123456-7", "dui": null,
  "address": "Santa Tecla", "phone": "2222-3333", "email": "compras@elprogreso.com",
  "creditLimit": 5000.00, "creditDays": 30, "outstandingBalance": 1250.00, "isActive": true
}
```

`outstandingBalance` = suma de `(total - paid_total)` de facturas emitidas del cliente.

**Crédito:** `PATCH /api/customers/{id}/credit` con `{ "creditLimit": 5000.00, "creditDays": 30 }`. Solo `ADMIN`.

---

## 11. Ventas · `/api/sales`

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/sales` | ADMIN, WAREHOUSE, PURCHASING, SELLER | Filtros: `from`, `to`, `customerId`, `status`, `userId`; paginado |
| `GET /api/sales/{id}` | ADMIN, WAREHOUSE, PURCHASING, SELLER | Detalle con líneas |
| `POST /api/sales` | ADMIN, SELLER | Crear en `DRAFT` (201) |
| `PUT /api/sales/{id}` | ADMIN, SELLER | Editar solo en `DRAFT` |
| `POST /api/sales/{id}/confirm` | ADMIN, SELLER | Confirmar y asignar lotes FEFO |
| `POST /api/sales/{id}/cancel` | ADMIN | Cancelar y devolver stock |
| `POST /api/sales/{id}/invoice` | ADMIN, SELLER | Emitir factura |

Visibilidad: `SELLER` solo ve sus propias ventas; `ADMIN`, `WAREHOUSE` y `PURCHASING` según la matriz.

**SaleRequest**

| Campo | Tipo | Validación |
|---|---|---|
| `customerId` | long | Opcional (consumidor final); si viene, cliente activo |
| `paymentCondition` | string | `CASH` o `CREDIT` |
| `paymentMethod` | string | `CASH`, `CARD`, `TRANSFER`, `OTHER`; opcional |
| `notes` | string | Opcional, ≤ 255 |
| `details` | array | Al menos una línea |

**SaleDetailRequest**

| Campo | Tipo | Validación |
|---|---|---|
| `productPresentationId` | long | Obligatorio, activo |
| `quantity` | decimal(12,2) | > 0 |
| `discount` | decimal(12,2) | ≥ 0; no supera el subtotal de la línea |
| `unitPrice` | decimal(10,2) | Opcional; si no viene, se usa el precio vigente |

El servidor calcula `taxRate`, `taxAmount`, `lineSubtotal`, `lineTotal` y los totales de la venta.

**SaleResponse (extracto)**

```json
{
  "id": 101, "saleNumber": "V-2026-000101", "status": "CONFIRMED",
  "saleDate": "2026-11-13T10:15:00",
  "customer": { "id": 8, "legalName": "Agroservicio El Progreso, S.A." },
  "seller": { "id": 3, "fullName": "Juan Pérez" },
  "paymentCondition": "CREDIT", "paymentMethod": null,
  "subtotal": 100.00, "discountTotal": 0.00, "taxTotal": 13.00,
  "total": 113.00, "paidTotal": 0.00,
  "details": [
    {
      "id": 501, "productPresentationId": 31, "productName": "Clavo 2 pulgadas",
      "presentationName": "Caja", "quantity": 5.00,
      "unitPrice": 20.00, "discount": 0.00,
      "taxRate": 13.00, "taxAmount": 11.50,
      "lineSubtotal": 100.00, "lineTotal": 113.00
    }
  ]
}
```

**Confirmar venta (409 si no hay stock):**

1. Venta en `DRAFT` con líneas.
2. Cliente activo; si es crédito, `outstandingBalance + total ≤ creditLimit`.
3. Por línea: lotes activos, no vencidos, `available_quantity > 0`, orden FEFO (`expiration_date ASC NULLS LAST`, `entry_date ASC`, `id ASC`), bloqueo pesimista.
4. Si el stock vendible no alcanza → `409 INSUFFICIENT_STOCK`.
5. Descuenta lotes, crea `sale_lot_allocations` y pasa a `CONFIRMED`.

**Cancelar:** solo `ADMIN`. Si no hay factura emitida, devuelve cantidades a los lotes. Si hay factura, responde 409 e indica emitir nota de crédito.

---

## 12. Facturación y pagos · `/api/invoices`

| Método y ruta | Roles | Descripción |
|---|---|---|
| `GET /api/invoices` | ADMIN, PURCHASING, SELLER | Filtros: `from`, `to`, `customerId`, `invoiceType`, `status`; paginado |
| `GET /api/invoices/{id}` | ADMIN, PURCHASING, SELLER | Detalle con snapshot |
| `POST /api/invoices` | ADMIN, SELLER | Emitir factura desde una venta |
| `POST /api/invoices/{id}/payments` | ADMIN, SELLER | Registrar pago |
| `GET /api/invoices/{id}/payments` | ADMIN, PURCHASING, SELLER | Historial de pagos |
| `POST /api/invoices/{id}/credit-note` | ADMIN | Emitir nota de crédito |
| `GET /api/invoices/{id}/pdf` | ADMIN, SELLER | Fase posterior |

### 12.1 Emitir factura

**InvoiceRequest**

| Campo | Tipo | Validación |
|---|---|---|
| `saleId` | long | Obligatorio; venta `CONFIRMED` sin factura |
| `invoiceType` | string | `CONSUMER` o `CCF` |
| `controlNumber` | string | Opcional, único; requerido para CCF preimpreso si aplica |
| `paymentCondition` | string | `CASH` o `CREDIT` |
| `notes` | string | Opcional, ≤ 255 |

Reglas:

- Genera `invoiceNumber` con `document_sequences` y `SELECT ... FOR UPDATE`.
- Copia cliente y líneas a `invoices` e `invoice_details` (snapshot inmutable).
- En crédito, `dueDate = issueDate + customer.creditDays` (mínimo 1 día).
- Una venta solo puede tener una factura emitida (índice único parcial en `sale_id`).

**InvoiceResponse (extracto)**

```json
{
  "id": 40, "invoiceNumber": "CCF-2026-000040", "controlNumber": "A-001234",
  "invoiceType": "CCF", "status": "ISSUED",
  "saleId": 101, "customerId": 8,
  "issueDate": "2026-11-13", "dueDate": "2026-12-13",
  "customerName": "Agroservicio El Progreso, S.A.",
  "customerNit": "0614-123456-101-2", "customerNrc": "123456-7",
  "subtotal": 100.00, "discountTotal": 0.00, "taxTotal": 13.00,
  "total": 113.00, "paidTotal": 0.00,
  "paymentCondition": "CREDIT"
}
```

### 12.2 Registrar pago

**PaymentRequest**

| Campo | Tipo | Validación |
|---|---|---|
| `paymentDate` | date | Obligatorio, no futura |
| `amount` | decimal(12,2) | > 0; `paidTotal + amount ≤ total` |
| `paymentMethod` | string | `CASH`, `CARD`, `TRANSFER`, `CHECK`, `OTHER` |
| `reference` | string | Opcional, ≤ 50 |
| `notes` | string | Opcional, ≤ 255 |

La suma de pagos no puede superar el total de la factura (409). Actualiza `paidTotal`.

### 12.3 Nota de crédito

`POST /api/invoices/{id}/credit-note` con `{ "reason": "...", "returnStock": true|false }`.

- Solo `ADMIN`.
- Referencia la factura original en `related_invoice_id`.
- Copia las líneas como snapshot.
- Marca la factura original como `CANCELLED`.
- Devuelve stock a los lotes solo si `returnStock = true` y la venta estaba confirmada.
- Genera número con la secuencia `CREDIT_NOTE`.

### 12.4 Inmutabilidad

Una factura `ISSUED` no se edita ni se elimina. Los cambios se hacen con nota de crédito. Toda cancelación guarda usuario, fecha y motivo.

---

## 13. Decisiones y pendientes

### 13.1 Decisiones cerradas

| ID | Tema | Decisión |
|---|---|---|
| D-01 | Esquema | Flyway con baseline del esquema actual |
| D-02 | IVA | Precios incluyen IVA 13 %; `tax_rate` por producto |
| D-03 | Documentos | Consumidor final, CCF y nota de crédito |
| D-04 | Stock | FEFO y exclusión de lotes vencidos |
| D-05 | Código | Paquetes por funcionalidad, migración incremental |
| D-06 | Imágenes | Por URL; upload externo en fase posterior |
| D-07 | Frontend | Angular standalone + signals |
| D-08 | Stock en pantalla | Stock físico + stock vendible |

### 13.2 Pendientes abiertos

| ID | Tema | Propuesta |
|---|---|---|
| P-01 | Formato exacto de NIT/NRC para clientes | Validar con el PO |
| P-02 | ¿La nota de crédito devuelve stock siempre o solo con confirmación? | `returnStock` explícito |
| P-03 | ¿Se requiere PDF de factura en esta etapa? | Fase posterior |
| P-04 | ¿El cliente "Consumidor Final" es registro o `customer_id` nulo? | Nulo por ahora |
| P-05 | ¿Se requiere retención o percepción? | Fuera de alcance inicial |
| P-06 | Formato de correlativos fiscales | Confirmar con contabilidad |
| P-07 | ¿Se implementa kardex en esta etapa? | Fase posterior |

---

## 14. Cobertura funcional

| Épica | Sección |
|---|---|
| Seguridad | 3 |
| Catálogos | 4 |
| Productos, presentaciones e imágenes | 5 |
| Proveedores | 6 |
| Lotes y stock | 7 |
| Dashboard y reportes | 8, 9 |
| Clientes | 10 |
| Ventas | 11 |
| Facturación y pagos | 12 |

---

## 15. Historial de cambios

**v2.0**
- Se adopta Flyway y se elimina la referencia a scripts manuales como fuente oficial.
- Se retira la app móvil: sin `/api/app/**`, sin `X-Client-Interface`, sin Vue ni Ionic.
- Se agregan clientes, ventas, detalle, asignación de lotes, facturación, pagos y notas de crédito.
- Se agrega `tax_rate` por producto y stock vendible.
- Se agregan `imageUrl` e `imageAlt` en presentaciones.
- Se actualiza la matriz de permisos y el formato de error.

**v1.1**
- Se eliminó `POST /api/auth/logout`.
- Bodega escribe productos y presentaciones.
- Tipos de producto con CRUD completo.
- Dashboard con severidad `EXPIRED`.

**v1.0** · Primera versión del contrato.
