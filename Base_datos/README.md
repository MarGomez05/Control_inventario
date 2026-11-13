# Base_datos — Referencia histórica

Esta carpeta contiene el esquema y los datos iniciales del **módulo de inventario original**.

> Desde la nueva etapa, la **fuente oficial del esquema es Flyway**, con las migraciones en `Api/api/src/main/resources/db/migration/`.

## Archivos

| Archivo | Uso |
|---|---|
| `01_create_tables.sql` | Esquema de 13 tablas, triggers, restricciones e índices. Base para `V1__baseline_inventory.sql` |
| `02_seed_data.sql` | Datos iniciales y de prueba. Base para el seed de desarrollo |
| `Diccionario_de_datos_TAMACHIHUA.xlsx` | Diccionario, restricciones e índices del esquema original |
| `MER_TAMACHIHUA_Inventario.png` | Modelo entidad-relación del inventario |

## Reglas

- No ejecutar estos scripts contra una base nueva: usar Flyway.
- No modificar el esquema fuera de una migración.
- Los cambios de la nueva etapa (clientes, ventas, facturación, imágenes e IVA) están en `Implementacion.md`.
- `product_lots.total_cost` es columna generada `STORED`; la API no la inserta.

## Migraciones planificadas

| Versión | Contenido |
|---|---|
| `V1` | Baseline del esquema actual |
| `V2` | Clientes |
| `V3` | Ventas, detalle, asignación de lotes y secuencias |
| `V4` | Facturación, detalle y pagos |
| `V5` | Imágenes de presentación |
| `V6` | `tax_rate` y secuencias iniciales |
