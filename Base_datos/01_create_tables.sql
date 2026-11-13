-- =====================================================================
-- TAMACHIHUA-ERP · Módulo de Inventario
-- 01_create_tables.sql — Creación de tablas, llaves y restricciones
--
-- Motor ........ PostgreSQL 12 o superior (probado en PostgreSQL 16)
-- Tarea ........ S2-02 (T-08) · Sprint 2 · grupo TDSV221, ITCA-FEPADE
-- Autor ........ Ali Velásquez (base de datos) · apoyo: Marvin Gómez (API)
-- Fecha ........ 02/11/2026
-- Base ......... MER v1.5 (01/11/2026) + contrato Endpoints-API.md v1.1
--
-- REFERENCIA HISTÓRICA. La fuente oficial del esquema pasa a ser Flyway
-- (Api/api/src/main/resources/db/migration). Este script se conserva para
-- construir V1__baseline_inventory.sql. La API mantiene
-- spring.jpa.hibernate.ddl-auto=validate.
--
-- Decisiones aplicadas respecto al MER v1.5 (pasan al MER v1.6):
--   P-01  products_presentations  ->  product_presentations
--   P-07  lote único por proveedor + número de lote + presentación
--   P-11  índice único parcial de precio vigente, CHECK de cantidades y
--         precios, notes VARCHAR(200)
--   ·     product_lots.total_cost es columna generada (STORED)
--   ·     nombres de catálogos únicos sin distinguir mayúsculas (lower)
--   ·     is_active NOT NULL DEFAULT TRUE; created_at/updated_at NOT NULL
--         y mantenidos por el trigger set_audit_timestamps()
--
-- Orden de creación (por dependencias):
--   1 roles  2 users  3 product_categories  4 product_types
--   5 product_brands  6 product_models  7 measure_units  8 presentations
--   9 products  10 product_presentations  11 product_prices
--   12 suppliers  13 product_lots
--
-- Uso:   psql -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 01_create_tables.sql
-- AVISO: el bloque 0 borra las 13 tablas si ya existen (deja la base
--        limpia para volver a ejecutar). No lo ejecute sobre datos reales.
-- =====================================================================
SET client_encoding = 'UTF8';   -- el archivo está en UTF-8: evita que Windows (WIN1252) guarde mal las tildes

SET client_min_messages = warning;   -- oculta los avisos "does not exist, skipping"

BEGIN;

-- ---------------------------------------------------------------------
-- 0. Limpieza (orden inverso) para poder re-ejecutar el script
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS product_lots           CASCADE;
DROP TABLE IF EXISTS suppliers              CASCADE;
DROP TABLE IF EXISTS product_prices         CASCADE;
DROP TABLE IF EXISTS product_presentations  CASCADE;
DROP TABLE IF EXISTS products               CASCADE;
DROP TABLE IF EXISTS presentations          CASCADE;
DROP TABLE IF EXISTS measure_units          CASCADE;
DROP TABLE IF EXISTS product_models         CASCADE;
DROP TABLE IF EXISTS product_brands         CASCADE;
DROP TABLE IF EXISTS product_types          CASCADE;
DROP TABLE IF EXISTS product_categories     CASCADE;
DROP TABLE IF EXISTS users                  CASCADE;
DROP TABLE IF EXISTS roles                  CASCADE;
DROP FUNCTION IF EXISTS set_audit_timestamps() CASCADE;

-- ---------------------------------------------------------------------
-- Función de auditoría: completa created_at/updated_at al insertar
-- (aunque la API envíe NULL) y actualiza updated_at en cada UPDATE.
-- ---------------------------------------------------------------------
CREATE FUNCTION set_audit_timestamps() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    NEW.created_at := COALESCE(NEW.created_at, CURRENT_TIMESTAMP);
    NEW.updated_at := COALESCE(NEW.updated_at, NEW.created_at);
  ELSE
    NEW.created_at := OLD.created_at;          -- la fecha de creación no cambia
    NEW.updated_at := CURRENT_TIMESTAMP;
  END IF;
  RETURN NEW;
END;
$$;

COMMENT ON FUNCTION set_audit_timestamps() IS
  'Mantiene created_at (inmutable) y updated_at (fecha de la última modificación) en las 13 tablas.';

-- =====================================================================
-- SEGURIDAD
-- =====================================================================

-- 1. roles ------------------------------------------------------------
CREATE TABLE roles (
  id          BIGSERIAL     NOT NULL,
  code        VARCHAR(20)   NOT NULL,
  name        VARCHAR(50)   NOT NULL,
  description VARCHAR(255)  NULL,
  is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_roles             PRIMARY KEY (id),
  CONSTRAINT uq_roles_code        UNIQUE (code),
  CONSTRAINT uq_roles_name        UNIQUE (name),
  CONSTRAINT ck_roles_code_format CHECK (code ~ '^[A-Z_]+$')
);

COMMENT ON TABLE  roles             IS 'Roles del sistema (RN-14, D-03, D-07). No son jerárquicos: cada endpoint declara qué roles acepta.';
COMMENT ON COLUMN roles.id          IS 'Identificador del rol.';
COMMENT ON COLUMN roles.code        IS 'Código que usa la API para los permisos: ADMIN, WAREHOUSE, PURCHASING o SELLER (D-07).';
COMMENT ON COLUMN roles.name        IS 'Nombre que se muestra en pantalla (Administrador, Bodega, Compras, Vendedor).';
COMMENT ON COLUMN roles.description IS 'Descripción de lo que puede hacer el rol.';
COMMENT ON COLUMN roles.is_active   IS 'Borrado lógico: FALSE = rol desactivado.';
COMMENT ON COLUMN roles.created_at  IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN roles.updated_at  IS 'Fecha y hora de la última modificación.';

-- 2. users ------------------------------------------------------------
CREATE TABLE users (
  id            BIGSERIAL     NOT NULL,
  role_id       BIGINT        NOT NULL,
  username      VARCHAR(50)   NOT NULL,
  password_hash VARCHAR(255)  NOT NULL,
  full_name     VARCHAR(150)  NOT NULL,
  email         VARCHAR(150)  NULL,
  is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_users          PRIMARY KEY (id),
  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT fk_users_role     FOREIGN KEY (role_id) REFERENCES roles (id)
);
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
CREATE INDEX ix_users_role_id ON users (role_id);

COMMENT ON TABLE  users               IS 'Usuarios del panel y de la app. No hay autorregistro: los crea el administrador (HU-02).';
COMMENT ON COLUMN users.id            IS 'Identificador del usuario.';
COMMENT ON COLUMN users.role_id       IS 'Rol del usuario (FK a roles).';
COMMENT ON COLUMN users.username      IS 'Nombre de usuario para iniciar sesión. Único.';
COMMENT ON COLUMN users.password_hash IS 'Contraseña cifrada con BCrypt (RNF-01). Nunca se devuelve en la API.';
COMMENT ON COLUMN users.full_name     IS 'Nombre completo de la persona.';
COMMENT ON COLUMN users.email         IS 'Correo electrónico. Opcional; único sin distinguir mayúsculas.';
COMMENT ON COLUMN users.is_active     IS 'Borrado lógico: un usuario inactivo no puede iniciar sesión (RN-13).';
COMMENT ON COLUMN users.created_at    IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN users.updated_at    IS 'Fecha y hora de la última modificación.';

-- =====================================================================
-- CATÁLOGOS
-- =====================================================================

-- 3. product_categories -----------------------------------------------
CREATE TABLE product_categories (
  id          BIGSERIAL     NOT NULL,
  name        VARCHAR(100)  NOT NULL,
  description VARCHAR(255)  NULL,
  is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_product_categories PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uq_product_categories_name ON product_categories (lower(name));

COMMENT ON TABLE  product_categories             IS 'Clasificación comercial del producto (ferretería, fontanería, agroquímicos...). HU-03.';
COMMENT ON COLUMN product_categories.id          IS 'Identificador de la categoría.';
COMMENT ON COLUMN product_categories.name        IS 'Nombre de la categoría. Único sin distinguir mayúsculas.';
COMMENT ON COLUMN product_categories.description IS 'Descripción opcional.';
COMMENT ON COLUMN product_categories.is_active   IS 'Borrado lógico.';
COMMENT ON COLUMN product_categories.created_at  IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN product_categories.updated_at  IS 'Fecha y hora de la última modificación.';

-- 4. product_types ----------------------------------------------------
CREATE TABLE product_types (
  id          BIGSERIAL     NOT NULL,
  name        VARCHAR(100)  NOT NULL,
  description VARCHAR(255)  NULL,
  is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_product_types PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uq_product_types_name ON product_types (lower(name));

COMMENT ON TABLE  product_types             IS 'Clasificación por naturaleza del artículo (insumo, herramienta, material, químico). Filtro extra (D-08); se carga con datos iniciales (HU-20 aplazada).';
COMMENT ON COLUMN product_types.id          IS 'Identificador del tipo de producto.';
COMMENT ON COLUMN product_types.name        IS 'Nombre del tipo. Único sin distinguir mayúsculas.';
COMMENT ON COLUMN product_types.description IS 'Descripción opcional.';
COMMENT ON COLUMN product_types.is_active   IS 'Borrado lógico.';
COMMENT ON COLUMN product_types.created_at  IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN product_types.updated_at  IS 'Fecha y hora de la última modificación.';

-- 5. product_brands ---------------------------------------------------
CREATE TABLE product_brands (
  id          BIGSERIAL     NOT NULL,
  name        VARCHAR(100)  NOT NULL,
  is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_product_brands PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uq_product_brands_name ON product_brands (lower(name));

COMMENT ON TABLE  product_brands            IS 'Fabricantes o marcas de los productos (HU-04).';
COMMENT ON COLUMN product_brands.id         IS 'Identificador de la marca.';
COMMENT ON COLUMN product_brands.name       IS 'Nombre de la marca. Único sin distinguir mayúsculas (evita Ferromax / FERROMAX).';
COMMENT ON COLUMN product_brands.is_active  IS 'Borrado lógico.';
COMMENT ON COLUMN product_brands.created_at IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN product_brands.updated_at IS 'Fecha y hora de la última modificación.';

-- 6. product_models ---------------------------------------------------
CREATE TABLE product_models (
  id               BIGSERIAL     NOT NULL,
  product_brand_id BIGINT        NOT NULL,
  name             VARCHAR(100)  NOT NULL,
  is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_product_models       PRIMARY KEY (id),
  CONSTRAINT fk_product_models_brand FOREIGN KEY (product_brand_id) REFERENCES product_brands (id)
);
CREATE UNIQUE INDEX uq_product_models_brand_name ON product_models (product_brand_id, lower(name));

COMMENT ON TABLE  product_models                  IS 'Modelos o referencias de cada marca (HU-04). El producto llega a su marca a través del modelo.';
COMMENT ON COLUMN product_models.id               IS 'Identificador del modelo.';
COMMENT ON COLUMN product_models.product_brand_id IS 'Marca a la que pertenece el modelo (FK a product_brands).';
COMMENT ON COLUMN product_models.name             IS 'Nombre del modelo. Único dentro de la misma marca, sin distinguir mayúsculas.';
COMMENT ON COLUMN product_models.is_active        IS 'Borrado lógico.';
COMMENT ON COLUMN product_models.created_at       IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN product_models.updated_at       IS 'Fecha y hora de la última modificación.';

-- 7. measure_units ----------------------------------------------------
CREATE TABLE measure_units (
  id          BIGSERIAL     NOT NULL,
  name        VARCHAR(50)   NOT NULL,
  code        VARCHAR(10)   NOT NULL,
  is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_measure_units PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uq_measure_units_name ON measure_units (lower(name));
CREATE UNIQUE INDEX uq_measure_units_code ON measure_units (lower(code));

COMMENT ON TABLE  measure_units            IS 'Unidades en que se expresa una presentación: libra, kilogramo, litro, galón, unidad, metro (HU-05).';
COMMENT ON COLUMN measure_units.id         IS 'Identificador de la unidad de medida.';
COMMENT ON COLUMN measure_units.name       IS 'Nombre de la unidad. Único sin distinguir mayúsculas.';
COMMENT ON COLUMN measure_units.code       IS 'Abreviatura de la unidad (LB, KG, L...). Única, máximo 10 caracteres.';
COMMENT ON COLUMN measure_units.is_active  IS 'Borrado lógico.';
COMMENT ON COLUMN measure_units.created_at IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN measure_units.updated_at IS 'Fecha y hora de la última modificación.';

-- 8. presentations ----------------------------------------------------
CREATE TABLE presentations (
  id              BIGSERIAL      NOT NULL,
  measure_unit_id BIGINT         NOT NULL,
  name            VARCHAR(100)   NOT NULL,
  quantity        NUMERIC(10,2)  NOT NULL,
  is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_presentations              PRIMARY KEY (id),
  CONSTRAINT fk_presentations_measure_unit FOREIGN KEY (measure_unit_id) REFERENCES measure_units (id),
  CONSTRAINT ck_presentations_quantity     CHECK (quantity > 0)
);
CREATE UNIQUE INDEX uq_presentations_name ON presentations (lower(name));
CREATE INDEX ix_presentations_measure_unit_id ON presentations (measure_unit_id);

COMMENT ON TABLE  presentations                 IS 'Formas de venta (Libra, Caja 25 lb, Saco 100 lb, Galón...). Cada una se expresa en una unidad y contiene una cantidad (RN-04).';
COMMENT ON COLUMN presentations.id              IS 'Identificador de la presentación.';
COMMENT ON COLUMN presentations.measure_unit_id IS 'Unidad de medida en que se expresa el contenido (FK a measure_units).';
COMMENT ON COLUMN presentations.name            IS 'Nombre de la presentación. Único sin distinguir mayúsculas; incluye el contenido cuando hay empaques del mismo nombre (Caja 25 lb, Caja 1000 u).';
COMMENT ON COLUMN presentations.quantity        IS 'Cantidad contenida en la unidad de medida (p. ej. 25 libras). Mayor que cero.';
COMMENT ON COLUMN presentations.is_active       IS 'Borrado lógico.';
COMMENT ON COLUMN presentations.created_at      IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN presentations.updated_at      IS 'Fecha y hora de la última modificación.';

-- =====================================================================
-- PRODUCTOS Y PRECIOS
-- =====================================================================

-- 9. products ---------------------------------------------------------
CREATE TABLE products (
  id                  BIGSERIAL     NOT NULL,
  product_category_id BIGINT        NOT NULL,
  product_type_id     BIGINT        NOT NULL,
  product_model_id    BIGINT        NULL,
  code                VARCHAR(50)   NOT NULL,
  name                VARCHAR(150)  NOT NULL,
  description         VARCHAR(255)  NULL,
  requires_expiration BOOLEAN       NOT NULL DEFAULT FALSE,
  is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_products          PRIMARY KEY (id),
  CONSTRAINT uq_products_code     UNIQUE (code),
  CONSTRAINT fk_products_category FOREIGN KEY (product_category_id) REFERENCES product_categories (id),
  CONSTRAINT fk_products_type     FOREIGN KEY (product_type_id)     REFERENCES product_types (id),
  CONSTRAINT fk_products_model    FOREIGN KEY (product_model_id)    REFERENCES product_models (id)
);
CREATE INDEX ix_products_product_category_id ON products (product_category_id);
CREATE INDEX ix_products_product_type_id     ON products (product_type_id);
CREATE INDEX ix_products_product_model_id    ON products (product_model_id);

COMMENT ON TABLE  products                     IS 'Artículos que vende el negocio (HU-06). Pertenecen a una categoría y a un tipo; pueden tener modelo (y por él, marca). RN-01, RN-02.';
COMMENT ON COLUMN products.id                  IS 'Identificador del producto.';
COMMENT ON COLUMN products.product_category_id IS 'Categoría del producto (FK a product_categories).';
COMMENT ON COLUMN products.product_type_id     IS 'Tipo de producto (FK a product_types).';
COMMENT ON COLUMN products.product_model_id    IS 'Modelo del producto (FK a product_models). Opcional.';
COMMENT ON COLUMN products.code                IS 'Código interno del producto (p. ej. FER-001). Único (RN-01).';
COMMENT ON COLUMN products.name                IS 'Nombre del producto.';
COMMENT ON COLUMN products.description         IS 'Descripción opcional.';
COMMENT ON COLUMN products.requires_expiration IS 'TRUE si sus lotes exigen fecha de vencimiento (químicos, semillas, pinturas). D-01, RN-08.';
COMMENT ON COLUMN products.is_active           IS 'Borrado lógico (RN-13).';
COMMENT ON COLUMN products.created_at          IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN products.updated_at          IS 'Fecha y hora de la última modificación.';

-- 10. product_presentations -------------------------------------------
CREATE TABLE product_presentations (
  id              BIGSERIAL      NOT NULL,
  product_id      BIGINT         NOT NULL,
  presentation_id BIGINT         NOT NULL,
  code            VARCHAR(50)    NOT NULL,
  min_stock       NUMERIC(12,2)  NOT NULL,
  is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_product_presentations                      PRIMARY KEY (id),
  CONSTRAINT uq_product_presentations_code                 UNIQUE (code),
  CONSTRAINT uq_product_presentations_product_presentation UNIQUE (product_id, presentation_id),
  CONSTRAINT fk_product_presentations_product      FOREIGN KEY (product_id)      REFERENCES products (id),
  CONSTRAINT fk_product_presentations_presentation FOREIGN KEY (presentation_id) REFERENCES presentations (id),
  CONSTRAINT ck_product_presentations_min_stock    CHECK (min_stock > 0)
);
CREATE INDEX ix_product_presentations_presentation_id ON product_presentations (presentation_id);

COMMENT ON TABLE  product_presentations                 IS 'Presentaciones en que se vende cada producto, con su stock mínimo (HU-07, RN-03). La existencia NO se guarda: es SUM(available_quantity) de sus lotes activos (RN-09).';
COMMENT ON COLUMN product_presentations.id              IS 'Identificador de la presentación del producto.';
COMMENT ON COLUMN product_presentations.product_id      IS 'Producto (FK a products).';
COMMENT ON COLUMN product_presentations.presentation_id IS 'Presentación (FK a presentations). La combinación producto + presentación es única.';
COMMENT ON COLUMN product_presentations.code            IS 'Código de la presentación del producto (p. ej. FER-001-CJ25). Único en todo el sistema; se usa en la búsqueda de la app.';
COMMENT ON COLUMN product_presentations.min_stock       IS 'Stock mínimo de esta presentación. Mayor que cero. Bajo stock si existencia <= mínimo (RN-10).';
COMMENT ON COLUMN product_presentations.is_active       IS 'Borrado lógico.';
COMMENT ON COLUMN product_presentations.created_at      IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN product_presentations.updated_at      IS 'Fecha y hora de la última modificación.';

-- 11. product_prices --------------------------------------------------
--     Sin is_active: el historial se controla con end_date.
CREATE TABLE product_prices (
  id                      BIGSERIAL      NOT NULL,
  product_presentation_id BIGINT         NOT NULL,
  user_id                 BIGINT         NOT NULL,
  price                   NUMERIC(10,2)  NOT NULL,
  start_date              TIMESTAMP      NOT NULL,
  end_date                TIMESTAMP      NULL,
  created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_product_prices                      PRIMARY KEY (id),
  CONSTRAINT fk_product_prices_product_presentation FOREIGN KEY (product_presentation_id) REFERENCES product_presentations (id),
  CONSTRAINT fk_product_prices_user                 FOREIGN KEY (user_id)                 REFERENCES users (id),
  CONSTRAINT ck_product_prices_price                CHECK (price > 0),
  CONSTRAINT ck_product_prices_dates                CHECK (end_date IS NULL OR end_date >= start_date)
);
-- Un solo precio vigente (end_date IS NULL) por presentación del producto (RN-05, P-11)
CREATE UNIQUE INDEX uq_product_prices_current
  ON product_prices (product_presentation_id) WHERE end_date IS NULL;
CREATE INDEX ix_product_prices_history ON product_prices (product_presentation_id, start_date DESC);
CREATE INDEX ix_product_prices_user_id ON product_prices (user_id);

COMMENT ON TABLE  product_prices                         IS 'Historial de precios de venta por presentación (HU-08, RN-05). Vigente = end_date IS NULL; solo uno por presentación.';
COMMENT ON COLUMN product_prices.id                      IS 'Identificador del registro de precio.';
COMMENT ON COLUMN product_prices.product_presentation_id IS 'Presentación del producto a la que aplica el precio (FK a product_presentations).';
COMMENT ON COLUMN product_prices.user_id                 IS 'Usuario que registró o autorizó el cambio (FK a users; la API lo toma del token).';
COMMENT ON COLUMN product_prices.price                   IS 'Precio de venta en US$. Mayor que cero.';
COMMENT ON COLUMN product_prices.start_date              IS 'Inicio de vigencia del precio.';
COMMENT ON COLUMN product_prices.end_date                IS 'Fin de vigencia. NULL = precio vigente. No puede ser anterior a start_date.';
COMMENT ON COLUMN product_prices.created_at              IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN product_prices.updated_at              IS 'Fecha y hora de la última modificación.';

-- =====================================================================
-- PROVEEDORES Y LOTES
-- =====================================================================

-- 12. suppliers -------------------------------------------------------
CREATE TABLE suppliers (
  id          BIGSERIAL     NOT NULL,
  trade_name  VARCHAR(150)  NOT NULL,
  legal_name  VARCHAR(150)  NOT NULL,
  nit         VARCHAR(17)   NOT NULL,
  nrc         VARCHAR(10)   NULL,
  address     VARCHAR(255)  NULL,
  phone       VARCHAR(20)   NULL,
  email       VARCHAR(150)  NULL,
  credit_days INTEGER       NOT NULL,
  is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_suppliers             PRIMARY KEY (id),
  CONSTRAINT uq_suppliers_nit         UNIQUE (nit),
  CONSTRAINT uq_suppliers_nrc         UNIQUE (nrc),
  CONSTRAINT ck_suppliers_credit_days CHECK (credit_days >= 0)
);
CREATE UNIQUE INDEX uq_suppliers_email ON suppliers (lower(email));

COMMENT ON TABLE  suppliers             IS 'Proveedores de mercadería (HU-09). Fuera del alcance: contactos, límite de crédito y marcas que vende (RN-15).';
COMMENT ON COLUMN suppliers.id          IS 'Identificador del proveedor.';
COMMENT ON COLUMN suppliers.trade_name  IS 'Nombre comercial.';
COMMENT ON COLUMN suppliers.legal_name  IS 'Razón social.';
COMMENT ON COLUMN suppliers.nit         IS 'NIT de 14 dígitos (0000-000000-000-0) o DUI usado como NIT (00000000-0). Único; formato validado en la API (P-08).';
COMMENT ON COLUMN suppliers.nrc         IS 'Número de registro de contribuyente (p. ej. 245781-3). Opcional y único.';
COMMENT ON COLUMN suppliers.address     IS 'Dirección.';
COMMENT ON COLUMN suppliers.phone       IS 'Teléfono (dígitos, espacios, + y -).';
COMMENT ON COLUMN suppliers.email       IS 'Correo para pedidos. Opcional; único sin distinguir mayúsculas.';
COMMENT ON COLUMN suppliers.credit_days IS 'Días de crédito que otorga el proveedor. Cero o positivo.';
COMMENT ON COLUMN suppliers.is_active   IS 'Borrado lógico (RN-13).';
COMMENT ON COLUMN suppliers.created_at  IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN suppliers.updated_at  IS 'Fecha y hora de la última modificación.';

-- 13. product_lots ----------------------------------------------------
CREATE TABLE product_lots (
  id                      BIGSERIAL      NOT NULL,
  product_presentation_id BIGINT         NOT NULL,
  supplier_id             BIGINT         NOT NULL,
  user_id                 BIGINT         NOT NULL,
  lot_number              VARCHAR(50)    NOT NULL,
  invoice_number          VARCHAR(50)    NOT NULL,
  invoice_date            DATE           NOT NULL,
  entry_date              DATE           NOT NULL DEFAULT CURRENT_DATE,
  expiration_date         DATE           NULL,
  received_quantity       NUMERIC(12,2)  NOT NULL,
  available_quantity      NUMERIC(12,2)  NOT NULL,
  unit_cost               NUMERIC(10,2)  NOT NULL,
  total_cost              NUMERIC(12,2)  GENERATED ALWAYS AS (round(received_quantity * unit_cost, 2)) STORED NOT NULL,
  notes                   VARCHAR(200)   NULL,
  is_active               BOOLEAN        NOT NULL DEFAULT TRUE,
  created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_product_lots                      PRIMARY KEY (id),
  CONSTRAINT uq_product_lots_supplier_lot_pp      UNIQUE (supplier_id, lot_number, product_presentation_id),
  CONSTRAINT fk_product_lots_product_presentation FOREIGN KEY (product_presentation_id) REFERENCES product_presentations (id),
  CONSTRAINT fk_product_lots_supplier             FOREIGN KEY (supplier_id)             REFERENCES suppliers (id),
  CONSTRAINT fk_product_lots_user                 FOREIGN KEY (user_id)                 REFERENCES users (id),
  CONSTRAINT ck_product_lots_received_quantity    CHECK (received_quantity > 0),
  CONSTRAINT ck_product_lots_available_quantity   CHECK (available_quantity >= 0 AND available_quantity <= received_quantity),
  CONSTRAINT ck_product_lots_unit_cost            CHECK (unit_cost >= 0),
  CONSTRAINT ck_product_lots_expiration_date      CHECK (expiration_date IS NULL OR expiration_date > entry_date)
);
CREATE INDEX ix_product_lots_product_presentation_id ON product_lots (product_presentation_id);
CREATE INDEX ix_product_lots_user_id                 ON product_lots (user_id);
CREATE INDEX ix_product_lots_expiration_date         ON product_lots (expiration_date);

COMMENT ON TABLE  product_lots                         IS 'Ingresos de mercadería por lote (HU-10, HU-11, RN-06). Un registro por presentación recibida (D-02). Lote activo: is_active = TRUE y available_quantity > 0.';
COMMENT ON COLUMN product_lots.id                      IS 'Identificador del lote.';
COMMENT ON COLUMN product_lots.product_presentation_id IS 'Presentación del producto que ingresa (FK a product_presentations).';
COMMENT ON COLUMN product_lots.supplier_id             IS 'Proveedor que entregó el lote (FK a suppliers).';
COMMENT ON COLUMN product_lots.user_id                 IS 'Usuario que registró el ingreso (FK a users; la API lo toma del token).';
COMMENT ON COLUMN product_lots.lot_number              IS 'Número de lote del proveedor. Único por proveedor y presentación (P-07). Sin lote del proveedor: SL-<factura>; inventario inicial: INI-<código>.';
COMMENT ON COLUMN product_lots.invoice_number          IS 'Número de la factura o CCF de compra.';
COMMENT ON COLUMN product_lots.invoice_date            IS 'Fecha de emisión de la factura. No puede ser futura (validación de la API).';
COMMENT ON COLUMN product_lots.entry_date              IS 'Fecha de ingreso a bodega. Por defecto, la fecha actual.';
COMMENT ON COLUMN product_lots.expiration_date         IS 'Fecha de vencimiento. Obligatoria si products.requires_expiration = TRUE (validación de la API, D-01); si existe, posterior a entry_date.';
COMMENT ON COLUMN product_lots.received_quantity       IS 'Cantidad recibida según el conteo físico. Mayor que cero.';
COMMENT ON COLUMN product_lots.available_quantity      IS 'Cantidad que queda disponible. Entre 0 y received_quantity. Se corrige con HU-11.';
COMMENT ON COLUMN product_lots.unit_cost               IS 'Costo unitario pagado al proveedor (US$). Cero o positivo. Solo lo ven ADMIN y PURCHASING (RN-12).';
COMMENT ON COLUMN product_lots.total_cost              IS 'Columna generada: round(received_quantity * unit_cost, 2). La API no la envía (insertable = false, updatable = false).';
COMMENT ON COLUMN product_lots.notes                   IS 'Observaciones (p. ej. diferencias entre lo facturado y lo contado). Máximo 200 caracteres.';
COMMENT ON COLUMN product_lots.is_active               IS 'Borrado lógico.';
COMMENT ON COLUMN product_lots.created_at              IS 'Fecha y hora de creación del registro.';
COMMENT ON COLUMN product_lots.updated_at              IS 'Fecha y hora de la última modificación.';

-- ---------------------------------------------------------------------
-- Triggers de auditoría en las 13 tablas
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_roles_audit                 BEFORE INSERT OR UPDATE ON roles                 FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_users_audit                 BEFORE INSERT OR UPDATE ON users                 FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_product_categories_audit    BEFORE INSERT OR UPDATE ON product_categories    FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_product_types_audit         BEFORE INSERT OR UPDATE ON product_types         FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_product_brands_audit        BEFORE INSERT OR UPDATE ON product_brands        FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_product_models_audit        BEFORE INSERT OR UPDATE ON product_models        FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_measure_units_audit         BEFORE INSERT OR UPDATE ON measure_units         FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_presentations_audit         BEFORE INSERT OR UPDATE ON presentations         FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_products_audit              BEFORE INSERT OR UPDATE ON products              FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_product_presentations_audit BEFORE INSERT OR UPDATE ON product_presentations FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_product_prices_audit        BEFORE INSERT OR UPDATE ON product_prices        FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_suppliers_audit             BEFORE INSERT OR UPDATE ON suppliers             FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();
CREATE TRIGGER trg_product_lots_audit          BEFORE INSERT OR UPDATE ON product_lots          FOR EACH ROW EXECUTE FUNCTION set_audit_timestamps();

COMMIT;

-- Fin de 01_create_tables.sql
