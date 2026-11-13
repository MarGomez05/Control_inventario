-- =====================================================================
-- TAMACHIHUA-ERP · Módulo de Inventario
-- 02_seed_data.sql — Datos iniciales y de prueba
--
-- Motor ........ PostgreSQL 12 o superior (probado en PostgreSQL 16)
-- Tarea ........ S2-02 (T-08) · Sprint 2
-- Autor ........ Ali Velásquez · apoyo: Marvin Gómez
-- Fecha ........ 02/11/2026
-- Requiere ..... 01_create_tables.sql ejecutado antes
--
-- Fuente de los datos (01-Documentacion/Insumos de negocio; empresa
-- simulada "Ferretería y Agroservicio El Progreso"):
--   01  Procesos, reglas y glosario ... tipos de producto, unidades
--   02  Control de inventario ......... productos, presentaciones,
--                                        mínimos, existencias, costos,
--                                        cuaderno de vencimientos, proveedores
--   03  Factura CCF-00045821 .......... lotes CF-2510, AV-PK-1301 y bombas
--   04  Lista de precios .............. precios vigentes e historial
--   06  Usuarios y permisos ........... usuarios
--
-- Los nombres se normalizan (Ferreteria/Ferretería, FERROMAX/Ferromax,
-- Tubo Plus/TuboPlus...). Los SUPUESTOS que no vienen en los insumos
-- están marcados con [SUPUESTO] en el comentario de cada bloque.
--
-- Contraseña de prueba de TODOS los usuarios:  Tamachihua2026
--   Hash BCrypt $2a$, costo 10 (compatible con BCryptPasswordEncoder de
--   Spring Security). Generado con Python:
--     import bcrypt
--     bcrypt.hashpw(b"Tamachihua2026", bcrypt.gensalt(rounds=10, prefix=b"2a"))
--   Cada usuario tiene su propia sal, por eso los 5 hashes son distintos.
--   ¡Cambiar estas contraseñas antes de usar el sistema con datos reales!
--
-- Uso:   psql -U <usuario> -d <base> -v ON_ERROR_STOP=1 -f 02_seed_data.sql
-- AVISO: el bloque 0 vacía las 13 tablas y reinicia los id.
-- =====================================================================
SET client_encoding = 'UTF8';   -- el archivo está en UTF-8: evita que Windows (WIN1252) guarde mal las tildes

SET client_min_messages = warning;

BEGIN;

-- ---------------------------------------------------------------------
-- 0. Limpieza: vacía las tablas y reinicia las secuencias de id
-- ---------------------------------------------------------------------
TRUNCATE TABLE product_lots, suppliers, product_prices, product_presentations,
               products, presentations, measure_units, product_models,
               product_brands, product_types, product_categories, users, roles
RESTART IDENTITY CASCADE;

-- =====================================================================
-- 1. roles (D-03, D-07) — la API usa code; name se muestra en pantalla
-- =====================================================================
INSERT INTO roles (code, name, description) VALUES
  ('ADMIN',      'Administrador', 'Gerencia y soporte IT: usuarios, catálogos, productos, precios, proveedores, lotes, dashboard y reportes. Panel y app.'),
  ('WAREHOUSE',  'Bodega',        'Productos y presentaciones, ingreso y corrección de lotes, dashboard y consulta de productos. Panel y app.'),
  ('PURCHASING', 'Compras',       'Proveedores, costos de lotes, productos en bajo stock y consulta de productos y lotes. Solo panel.'),
  ('SELLER',     'Vendedor',      'Consulta de productos, existencias y precio de venta. No ve costos ni proveedores. Solo app.');

-- =====================================================================
-- 2. users — un usuario por rol, tomados del insumo 06
--    username = parte local del correo. Contraseña: Tamachihua2026
--    layala: ex vendedor INACTIVO (renunció en septiembre); se conserva
--    para probar que un usuario inactivo no inicia sesión (HU-01).
--    No se carga a Roberto Flores (soporte IT, también ADMIN) ni a los
--    otros dos vendedores, para dejar un usuario activo por rol.
-- =====================================================================
INSERT INTO users (role_id, username, password_hash, full_name, email, is_active)
SELECT r.id, v.username, v.password_hash, v.full_name, v.email, v.is_active
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('ADMIN',      'mrivas',    '$2a$10$gryRyj9gu8lLGiqvtfJQzuHLHrey4S70rrgGnfPMrxUCDLHNPnRtu', 'Marta Elena Rivas',        'mrivas@elprogreso.example',    TRUE),
  ('WAREHOUSE',  'jmejia',    '$2a$10$w1o27.ELFkHuCujbX7BZaety2AwMDksdBzSdvUe7hsGTplK1ktwD6', 'José Antonio Mejía',       'jmejia@elprogreso.example',    TRUE),
  ('PURCHASING', 'kmenjivar', '$2a$10$6hsaVygG9lno1UPHig31WuYTCT9J2j4..9v9TxIC3ph2HQKQokMpK', 'Karla Beatriz Menjívar',   'kmenjivar@elprogreso.example', TRUE),
  ('SELLER',     'kalvarado', '$2a$10$0EZmDRQoe8kfPrNAxdCVZuNTuJAoitwvT/cNStqx0eJdsScwA/dnu', 'Kevin Alexander Alvarado', 'kalvarado@elprogreso.example', TRUE),
  ('SELLER',     'layala',    '$2a$10$5XG.1SKnvZ.VAqidz7r.UeZnxFVwjo3t//rdQfDjkX98B5wt1Gh8m', 'Luis Fernando Ayala',      'layala@elprogreso.example',    FALSE)
) AS x(role_code, username, password_hash, full_name, email, is_active)) AS v
JOIN roles r ON r.code = v.role_code
ORDER BY v.ord;

-- =====================================================================
-- 3. product_categories — columna "Categoría" del insumo 02
-- =====================================================================
INSERT INTO product_categories (name) VALUES
  ('Ferretería'), ('Construcción'), ('Fontanería'), ('Agroquímicos'),
  ('Semillas'), ('Herramientas'), ('Pinturas');

-- =====================================================================
-- 4. product_types — glosario del insumo 01 (HU-20 aplazada: se cargan aquí)
-- =====================================================================
INSERT INTO product_types (name, description) VALUES
  ('Insumo',      'Artículos que se consumen en la producción agrícola (semillas).'),
  ('Herramienta', 'Herramientas y equipos (motosierras, bombas, machetes).'),
  ('Material',    'Materiales de ferretería, construcción y fontanería.'),
  ('Químico',     'Productos químicos: fertilizantes, plaguicidas, pegamentos y pinturas.');

-- =====================================================================
-- 5. product_brands — columna "Marca / modelo" del insumo 02 (normalizada)
-- =====================================================================
INSERT INTO product_brands (name) VALUES
  ('Ferromax'), ('Cementos del Valle'), ('TuboPlus'), ('CampoFértil'),
  ('AgroVerde'), ('SemiCampo'), ('Herramax'), ('Pintex');

-- =====================================================================
-- 6. product_models
--    Modelos que trae el insumo 02: Protek, Limpia, Mochila 16, H-59,
--    MS-180, Esmalte Pro.
--    [SUPUESTO] "Genérico": el MER solo enlaza el producto a la marca a
--    través del modelo. Para no perder la marca de productos sin modelo
--    (clavos, cemento, PVC, fertilizante, machete) se crea un modelo
--    "Genérico" por marca.
-- =====================================================================
INSERT INTO product_models (product_brand_id, name)
SELECT b.id, v.model
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('Ferromax',           'Genérico'),
  ('Cementos del Valle', 'Genérico'),
  ('TuboPlus',           'Genérico'),
  ('CampoFértil',        'Genérico'),
  ('AgroVerde',          'Protek'),
  ('AgroVerde',          'Limpia'),
  ('AgroVerde',          'Mochila 16'),
  ('SemiCampo',          'H-59'),
  ('Herramax',           'MS-180'),
  ('Herramax',           'Genérico'),
  ('Pintex',             'Esmalte Pro')
) AS x(brand, model)) AS v
JOIN product_brands b ON b.name = v.brand
ORDER BY v.ord;

-- =====================================================================
-- 7. measure_units — glosario del insumo 01 (los códigos son abreviaturas)
-- =====================================================================
INSERT INTO measure_units (name, code) VALUES
  ('Libra',     'LB'),
  ('Kilogramo', 'KG'),
  ('Litro',     'L'),
  ('Galón',     'GAL'),
  ('Unidad',    'UND'),
  ('Metro',     'M');

-- =====================================================================
-- 8. presentations — columnas "Presentación" y "Contenido" del insumo 02
--    con los nombres de la lista de precios (insumo 04). El nombre es
--    único, por eso incluye el contenido cuando dos empaques se llaman
--    igual (Caja 25 lb / Caja 1000 u; Bolsa 100 u / 10 u / 25 lb / 42.5 kg).
--    Galón de insecticida (3.785 L) y galón de pintura (1 gal) son la
--    misma presentación: 1 galón.
-- =====================================================================
INSERT INTO presentations (measure_unit_id, name, quantity)
SELECT u.id, v.name, v.quantity
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('Libra',         'LB',    1.00),
  ('Caja 25 lb',    'LB',   25.00),
  ('Saco 100 lb',   'LB',  100.00),
  ('Bolsa 25 lb',   'LB',   25.00),
  ('Bolsa 42.5 kg', 'KG',   42.50),
  ('Unidad',        'UND',   1.00),
  ('Bolsa 10 u',    'UND',  10.00),
  ('Bolsa 100 u',   'UND', 100.00),
  ('Caja 1000 u',   'UND',1000.00),
  ('Frasco 1 L',    'L',     1.00),
  ('Galón',         'GAL',   1.00),
  ('Cuarto',        'GAL',   0.25),
  ('Lata 1/4 gal',  'GAL',   0.25)
) AS x(name, unit_code, quantity)) AS v
JOIN measure_units u ON u.code = v.unit_code
ORDER BY v.ord;

-- =====================================================================
-- 9. products — 15 códigos del insumo 02 (nombres de la lista de precios)
--    requires_expiration = TRUE en los que el negocio controla con lote y
--    vencimiento (pegamento, agroquímicos, semilla, pintura). RN-08.
--    [SUPUESTO] Tipo de producto: el glosario define los tipos pero los
--    insumos no los asignan a cada producto. Asignación propuesta (a
--    confirmar con el PO): Material = clavos, tornillos, cemento, PVC;
--    Químico = pegamento, fertilizante, insecticida, herbicida, pintura;
--    Insumo = semilla; Herramienta = motosierra, bomba, machete.
-- =====================================================================
INSERT INTO products (product_category_id, product_type_id, product_model_id,
                      code, name, requires_expiration)
SELECT c.id, t.id, m.id, v.code, v.name, v.requires_expiration
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('FER-001', 'Clavo de acero 2 pulg',                'Ferretería',   'Material',    'Ferromax',           'Genérico',    FALSE),
  ('FER-002', 'Clavo de acero 3 pulg',                'Ferretería',   'Material',    'Ferromax',           'Genérico',    FALSE),
  ('FER-010', 'Tornillo para madera 1 1/2 pulg',      'Ferretería',   'Material',    'Ferromax',           'Genérico',    FALSE),
  ('CON-001', 'Cemento gris 42.5 kg',                 'Construcción', 'Material',    'Cementos del Valle', 'Genérico',    FALSE),
  ('FON-001', 'Tubo PVC 1/2 pulg x 6 m',              'Fontanería',   'Material',    'TuboPlus',           'Genérico',    FALSE),
  ('FON-002', 'Codo PVC 1/2 pulg 90°',                'Fontanería',   'Material',    'TuboPlus',           'Genérico',    FALSE),
  ('FON-005', 'Pegamento PVC',                        'Fontanería',   'Químico',     'TuboPlus',           'Genérico',    TRUE),
  ('AGR-001', 'Fertilizante 15-15-15',                'Agroquímicos', 'Químico',     'CampoFértil',        'Genérico',    TRUE),
  ('AGR-010', 'Insecticida líquido Protek',           'Agroquímicos', 'Químico',     'AgroVerde',          'Protek',      TRUE),
  ('AGR-011', 'Herbicida Limpia',                     'Agroquímicos', 'Químico',     'AgroVerde',          'Limpia',      TRUE),
  ('SEM-001', 'Semilla de maíz híbrido H-59',         'Semillas',     'Insumo',      'SemiCampo',          'H-59',        TRUE),
  ('HER-001', 'Motosierra 18 pulg MS-180',            'Herramientas', 'Herramienta', 'Herramax',           'MS-180',      FALSE),
  ('HER-005', 'Bomba de fumigar 16 L',                'Herramientas', 'Herramienta', 'AgroVerde',          'Mochila 16',  FALSE),
  ('HER-010', 'Machete 22 pulg',                      'Herramientas', 'Herramienta', 'Herramax',           'Genérico',    FALSE),
  ('PIN-001', 'Pintura de aceite blanca Esmalte Pro', 'Pinturas',     'Químico',     'Pintex',             'Esmalte Pro', TRUE)
) AS x(code, name, category, type, brand, model, requires_expiration)) AS v
JOIN product_categories c ON c.name = v.category
JOIN product_types      t ON t.name = v.type
JOIN product_brands     b ON b.name = v.brand
JOIN product_models     m ON m.product_brand_id = b.id AND m.name = v.model
ORDER BY v.ord;

-- =====================================================================
-- 10. product_presentations — 22 filas de la lista de precios (insumo 04)
--     min_stock = columna "Mínimo" del insumo 02.
--     [SUPUESTO] code: los insumos no traen código por presentación; se
--     usa la convención <código del producto>-<abreviatura>.
-- =====================================================================
INSERT INTO product_presentations (product_id, presentation_id, code, min_stock)
SELECT p.id, pr.id, v.code, v.min_stock
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('FER-001', 'Libra',         'FER-001-LB',      10.00),
  ('FER-001', 'Caja 25 lb',    'FER-001-CJ25',    10.00),
  ('FER-002', 'Libra',         'FER-002-LB',      10.00),
  ('FER-010', 'Bolsa 100 u',   'FER-010-BL100',    5.00),
  ('FER-010', 'Caja 1000 u',   'FER-010-CJ1000',   2.00),
  ('CON-001', 'Bolsa 42.5 kg', 'CON-001-BL',      40.00),
  ('FON-001', 'Unidad',        'FON-001-UN',      20.00),
  ('FON-002', 'Unidad',        'FON-002-UN',      50.00),
  ('FON-002', 'Bolsa 10 u',    'FON-002-BL10',     5.00),
  ('FON-005', 'Lata 1/4 gal',  'FON-005-LT',       6.00),
  ('AGR-001', 'Saco 100 lb',   'AGR-001-SC100',   10.00),
  ('AGR-001', 'Libra',         'AGR-001-LB',      20.00),
  ('AGR-010', 'Frasco 1 L',    'AGR-010-FR1L',     6.00),
  ('AGR-010', 'Galón',         'AGR-010-GL',       2.00),
  ('AGR-011', 'Frasco 1 L',    'AGR-011-FR1L',     6.00),
  ('SEM-001', 'Bolsa 25 lb',   'SEM-001-BL25',     4.00),
  ('SEM-001', 'Libra',         'SEM-001-LB',       5.00),
  ('HER-001', 'Unidad',        'HER-001-UN',       2.00),
  ('HER-005', 'Unidad',        'HER-005-UN',       2.00),
  ('HER-010', 'Unidad',        'HER-010-UN',      10.00),
  ('PIN-001', 'Galón',         'PIN-001-GL',       4.00),
  ('PIN-001', 'Cuarto',        'PIN-001-CT',       6.00)
) AS x(product_code, presentation, code, min_stock)) AS v
JOIN products      p  ON p.code  = v.product_code
JOIN presentations pr ON pr.name = v.presentation
ORDER BY v.ord;

-- =====================================================================
-- 11. product_prices — insumo 04. Los cambios los autorizó Marta Rivas
--     (columna "Autorizó"), por eso user_id = mrivas.
-- 11a. Precios vigentes (end_date NULL): "Precio de venta" y "Vigente desde"
-- =====================================================================
INSERT INTO product_prices (product_presentation_id, user_id, price, start_date, end_date)
SELECT pp.id, u.id, v.price, v.start_date, NULL
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('FER-001-LB',       1.25, TIMESTAMP '2026-09-01 00:00'),
  ('FER-001-CJ25',    28.50, TIMESTAMP '2026-09-01 00:00'),
  ('FER-002-LB',       1.30, TIMESTAMP '2026-09-01 00:00'),
  ('FER-010-BL100',    3.25, TIMESTAMP '2026-07-15 00:00'),
  ('FER-010-CJ1000',  27.00, TIMESTAMP '2026-07-15 00:00'),
  ('CON-001-BL',       9.75, TIMESTAMP '2026-10-20 00:00'),
  ('FON-001-UN',       3.60, TIMESTAMP '2026-06-01 00:00'),
  ('FON-002-UN',       0.35, TIMESTAMP '2026-06-01 00:00'),
  ('FON-002-BL10',     3.00, TIMESTAMP '2026-06-01 00:00'),
  ('FON-005-LT',       5.50, TIMESTAMP '2026-06-01 00:00'),
  ('AGR-001-SC100',   38.50, TIMESTAMP '2026-10-22 00:00'),
  ('AGR-001-LB',       0.45, TIMESTAMP '2026-10-22 00:00'),
  ('AGR-010-FR1L',     9.90, TIMESTAMP '2026-08-01 00:00'),
  ('AGR-010-GL',      34.00, TIMESTAMP '2026-08-01 00:00'),
  ('AGR-011-FR1L',     7.95, TIMESTAMP '2026-08-01 00:00'),
  ('SEM-001-BL25',    55.00, TIMESTAMP '2026-10-01 00:00'),
  ('SEM-001-LB',       2.60, TIMESTAMP '2026-10-01 00:00'),
  ('HER-001-UN',     199.00, TIMESTAMP '2026-05-01 00:00'),
  ('HER-005-UN',      39.90, TIMESTAMP '2026-05-01 00:00'),
  ('HER-010-UN',       6.50, TIMESTAMP '2026-05-01 00:00'),
  ('PIN-001-GL',      19.75, TIMESTAMP '2026-10-10 00:00'),
  ('PIN-001-CT',       6.25, TIMESTAMP '2026-10-10 00:00')
) AS x(pp_code, price, start_date)) AS v
JOIN product_presentations pp ON pp.code = v.pp_code
JOIN users u ON u.username = 'mrivas'
ORDER BY v.ord;

-- ---------------------------------------------------------------------
-- 11b. Historial: hoja "Historial de cambios" del insumo 04.
--      end_date = fecha del cambio (cuando empezó el precio nuevo).
--      [SUPUESTO] start_date = 2026-01-01: el insumo no dice desde
--      cuándo regía el precio anterior.
-- ---------------------------------------------------------------------
INSERT INTO product_prices (product_presentation_id, user_id, price, start_date, end_date)
SELECT pp.id, u.id, v.price, TIMESTAMP '2026-01-01 00:00', v.end_date
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('FER-001-LB',      1.15, TIMESTAMP '2026-09-01 00:00'),   -- Aumento de costo del proveedor
  ('FER-001-CJ25',   26.75, TIMESTAMP '2026-09-01 00:00'),   -- Aumento de costo del proveedor
  ('CON-001-BL',      9.50, TIMESTAMP '2026-10-20 00:00'),   -- Ajuste de mercado
  ('AGR-001-SC100',  36.75, TIMESTAMP '2026-10-22 00:00'),   -- Nuevo lote con costo mayor
  ('AGR-001-LB',      0.42, TIMESTAMP '2026-10-22 00:00')    -- Nuevo lote con costo mayor
) AS x(pp_code, price, end_date)) AS v
JOIN product_presentations pp ON pp.code = v.pp_code
JOIN users u ON u.username = 'mrivas'
ORDER BY v.ord;

-- =====================================================================
-- 12. suppliers — hoja "Proveedores" del insumo 02 (coincide con la
--     factura 03 y la ficha 05). Fuera del alcance: vendedor que visita,
--     límite de crédito y marcas que vende (RN-15).
-- =====================================================================
INSERT INTO suppliers (trade_name, legal_name, nit, nrc, address, phone, email, credit_days) VALUES
  ('Distribuidora Agrícola La Cosecha', 'Distribuidora Agrícola La Cosecha, S.A. de C.V.',   '0614-150318-102-5', '245781-3', 'Km 12 carretera a Santa Ana, Lourdes, La Libertad', '2525-7788', 'ventas@lacosecha.example',       30),
  ('Agroinsumos Occidente',             'Agroinsumos de Occidente, S.A. de C.V.',            '0210-020615-101-1', '199302-7', 'Av. Independencia 25, Santa Ana',                    '2447-1020', 'pedidos@agroinsumos.example',    15),
  ('Ferretera Central',                 'Ferretera Central, S.A. de C.V.',                   '0614-220909-103-8', '176540-2', 'Calle Rubén Darío 1210, San Salvador',               '2222-1100', 'ventas@ferreteracentral.example', 30),
  ('Materiales Santa Ana',              'Materiales de Construcción Santa Ana, S.A. de C.V.', '0210-111112-102-0', '210045-9', 'Calle Libertad 8, Santa Ana',                        '2440-3300', 'info@matsantaana.example',        0),
  ('Importadora Herramax',              'Importadora Herramax, S.A. de C.V.',                '0614-050520-104-6', '301122-4', 'Bulevar del Ejército km 5, Soyapango',               '2210-5050', 'ventas@herramax.example',        45),
  ('Pinturas y Más',                    'Pinturas y Más, S.A. de C.V.',                      '0614-300417-101-2', '256600-1', 'Av. Las Palmas 44, Santa Tecla',                     '2260-4141', 'pinturasymas@correo.example',    30);

-- =====================================================================
-- 13. product_lots — los registra José Mejía (firma "J.M." del cuaderno)
--     total_cost NO se inserta: es columna generada.
--
-- 13a. Lotes con datos del cuaderno de vencimientos (insumo 02) y de la
--      factura CCF-00045821 (insumo 03).
--      received_quantity = "Cantidad recibida"; available_quantity =
--      "Existencia" del insumo 02; unit_cost = "Último costo" del 02 o
--      "Precio unit." de la factura.
--      [SUPUESTO] invoice_date = fecha de ingreso, salvo CCF-00045821
--      (21/10/2026), porque el cuaderno no trae la fecha de la factura.
--
--      [DEMO] Fechas de vencimiento relativas (pedido del PO, 4/11/2026):
--      4 lotes vencen respecto a la fecha en que se ejecuta el seed, para
--      que el dashboard (HU-12) muestre las tres alertas el día de la
--      presentación. Volver a ejecutar el seed el 14 o el 15/11.
--        L-PV2309    CURRENT_DATE - 5   EXPIRED (vencido con existencia)
--        AV-PK-1123  CURRENT_DATE + 10  RED
--        AV-PK-1201  CURRENT_DATE + 15  RED (límite de la regla 0–15)
--        AV-LM-0905  CURRENT_DATE + 25  YELLOW
--      Fechas del cuaderno: L-PV2309 15/03/2027, AV-PK-1123 20/11/2026,
--      AV-PK-1201 15/01/2027, AV-LM-0905 10/12/2026.
-- =====================================================================
INSERT INTO product_lots (product_presentation_id, supplier_id, user_id, lot_number,
                          invoice_number, invoice_date, entry_date, expiration_date,
                          received_quantity, available_quantity, unit_cost, notes)
SELECT pp.id, s.id, u.id, v.lot_number, v.invoice_number, v.invoice_date, v.entry_date,
       v.expiration_date, v.received, v.available, v.unit_cost, v.notes
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('AGR-001-SC100', '0614-150318-102-5', 'CF-2408',         'CCF-00045110', DATE '2026-08-05', DATE '2026-08-05', DATE '2027-06-30', 40.00, 22.00, 31.00, NULL),
  -- [SUPUESTO] libra suelta: sale de un saco abierto del lote CF-2408 (movimiento fuera del alcance).
  --            Recibido = disponible = existencia del Excel; costo derivado 31.00 / 100 lb.
  ('AGR-001-LB',    '0614-150318-102-5', 'CF-2408',         'CCF-00045110', DATE '2026-08-05', DATE '2026-08-05', DATE '2027-06-30', 45.00, 45.00,  0.31, 'Libra suelta de saco abierto del lote CF-2408. Costo derivado: 31.00 / 100 lb.'),
  ('AGR-010-FR1L',  '0614-150318-102-5', 'AV-PK-1123',      'CCF-00045387', DATE '2026-09-12', DATE '2026-09-12', CURRENT_DATE + 10, 24.00, 14.00,  6.80, NULL),
  ('FON-005-LT',    '0614-220909-103-8', 'L-PV2309',        'F-2231',       DATE '2026-09-12', DATE '2026-09-12', CURRENT_DATE - 5,  24.00, 18.00,  3.75, NULL),
  ('AGR-011-FR1L',  '0210-020615-101-1', 'AV-LM-0905',      'CF-7781',      DATE '2026-10-02', DATE '2026-10-02', CURRENT_DATE + 25, 12.00,  9.00,  5.40, NULL),
  ('AGR-010-GL',    '0210-020615-101-1', 'AV-PK-1201',      'CF-7781',      DATE '2026-10-02', DATE '2026-10-02', CURRENT_DATE + 15,  6.00,  4.00, 24.50, NULL),
  -- Mismo lote PX-2407 en dos presentaciones (galón y cuarto) en la misma factura: caso P-07
  ('PIN-001-GL',    '0614-300417-101-2', 'PX-2407',         'PM-1020',      DATE '2026-10-10', DATE '2026-10-10', DATE '2027-07-31', 12.00, 10.00, 14.20, NULL),
  ('PIN-001-CT',    '0614-300417-101-2', 'PX-2407',         'PM-1020',      DATE '2026-10-10', DATE '2026-10-10', DATE '2027-07-31', 24.00, 16.00,  4.30, NULL),
  ('AGR-001-SC100', '0614-150318-102-5', 'CF-2510',         'CCF-00045821', DATE '2026-10-21', DATE '2026-10-21', DATE '2027-10-31', 15.00, 15.00, 32.40, 'Factura: 16 sacos; recibidos 15 (faltó 1). J.M. 21/10/2026.'),
  -- [SUPUESTO] AV-PK-1301 viene en la factura y la nota de bodega dice "completos", pero no figura
  --            en el Excel ni en el cuaderno: disponible = recibido.
  ('AGR-010-FR1L',  '0614-150318-102-5', 'AV-PK-1301',      'CCF-00045821', DATE '2026-10-21', DATE '2026-10-21', DATE '2027-02-28', 24.00, 24.00,  6.95, 'Recibido completo según nota del CCF-00045821; no figura en el Excel ni en el cuaderno.'),
  -- Sin número de lote del proveedor ("—" en la factura): convención SL-<factura>
  ('HER-005-UN',    '0614-150318-102-5', 'SL-CCF-00045821', 'CCF-00045821', DATE '2026-10-21', DATE '2026-10-21', NULL,               4.00,  4.00, 28.00, 'Factura sin número de lote. Recibidas completas.'),
  ('SEM-001-BL25',  '0210-020615-101-1', 'SC-H59-26A',      'CF-7902',      DATE '2026-10-21', DATE '2026-10-21', DATE '2027-05-01', 10.00,  8.00, 42.00, NULL),
  -- [SUPUESTO] libra de bolsa abierta del lote SC-H59-26A; costo derivado 42.00 / 25 lb.
  ('SEM-001-LB',    '0210-020615-101-1', 'SC-H59-26A',      'CF-7902',      DATE '2026-10-21', DATE '2026-10-21', DATE '2027-05-01', 12.00, 12.00,  1.68, 'Libra de bolsa abierta del lote SC-H59-26A. Costo derivado: 42.00 / 25 lb.')
) AS x(pp_code, supplier_nit, lot_number, invoice_number, invoice_date, entry_date,
       expiration_date, received, available, unit_cost, notes)) AS v
JOIN product_presentations pp ON pp.code = v.pp_code
JOIN suppliers s ON s.nit = v.supplier_nit
JOIN users u ON u.username = 'jmejia'
ORDER BY v.ord;

-- ---------------------------------------------------------------------
-- 13b. [SUPUESTO] Inventario inicial: productos con existencia y costo en
--      el insumo 02 pero sin lote ni factura. Se cargan como saldo inicial
--      para que la existencia (derivada de los lotes) coincida con el Excel.
--      lot_number = INI-<código de la presentación>; invoice_number =
--      INV-INICIAL; fecha = 02/11/2026 (fecha de la carga).
--      received = available = "Existencia"; unit_cost = "Último costo".
-- ---------------------------------------------------------------------
INSERT INTO product_lots (product_presentation_id, supplier_id, user_id, lot_number,
                          invoice_number, invoice_date, entry_date, expiration_date,
                          received_quantity, available_quantity, unit_cost, notes)
SELECT pp.id, s.id, u.id, 'INI-' || v.pp_code, 'INV-INICIAL', DATE '2026-11-02', DATE '2026-11-02',
       NULL, v.quantity, v.quantity, v.unit_cost, v.notes
FROM (SELECT x.*, row_number() OVER () AS ord FROM (VALUES
  ('FER-001-LB',     '0614-220909-103-8',  24.00,   0.85, NULL),
  ('FER-001-CJ25',   '0614-220909-103-8',  10.00,  19.50, NULL),
  ('FER-002-LB',     '0614-220909-103-8',  30.00,   0.85, NULL),
  ('FER-010-BL100',  '0614-220909-103-8',  15.00,   2.10, NULL),
  ('FER-010-CJ1000', '0614-220909-103-8',   3.00,  18.00, NULL),
  ('CON-001-BL',     '0210-111112-102-0',  80.00,   7.90, 'Excel de bodega: 2 bolsas rotas (merma).'),
  ('FON-001-UN',     '0614-220909-103-8',  45.00,   2.40, NULL),
  ('FON-002-UN',     '0614-220909-103-8', 120.00,   0.18, NULL),
  ('FON-002-BL10',   '0614-220909-103-8',  12.00,   1.60, NULL),
  ('HER-001-UN',     '0614-050520-104-6',   2.00, 145.00, 'Series: HX180-5521, HX180-5530.'),
  -- El Excel dice 5 bombas; se asume que incluye las 4 del CCF-00045821 (lote SL-CCF-00045821).
  ('HER-005-UN',     '0614-150318-102-5',   1.00,  28.00, 'Existencia previa: Excel 5 en total, incluye las 4 del CCF-00045821. 1 dañada en garantía.'),
  ('HER-010-UN',     '0614-050520-104-6',  25.00,   4.10, NULL)
) AS x(pp_code, supplier_nit, quantity, unit_cost, notes)) AS v
JOIN product_presentations pp ON pp.code = v.pp_code
JOIN suppliers s ON s.nit = v.supplier_nit
JOIN users u ON u.username = 'jmejia'
ORDER BY v.ord;

-- =====================================================================
-- 14. Verificación: si algún JOIN no encontró su fila, se cancela todo
-- =====================================================================
DO $$
DECLARE
  expected CONSTANT jsonb := '{"roles":4,"users":5,"product_categories":7,"product_types":4,
    "product_brands":8,"product_models":11,"measure_units":6,"presentations":13,"products":15,
    "product_presentations":22,"product_prices":27,"suppliers":6,"product_lots":25}';
  t text;
  n bigint;
BEGIN
  FOR t IN SELECT jsonb_object_keys(expected) LOOP
    EXECUTE format('SELECT count(*) FROM %I', t) INTO n;
    IF n <> (expected ->> t)::bigint THEN
      RAISE EXCEPTION 'Seed incompleto en %: % filas (se esperaban %)', t, n, expected ->> t;
    END IF;
  END LOOP;
END;
$$;

COMMIT;

-- Fin de 02_seed_data.sql
