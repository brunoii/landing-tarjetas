# Propuesta: Etapa 4 — Alias local de barcode

## Problema / resultado esperado

La lista del súper ya funciona como inventario doméstico manual, pero todavía no permite encontrar un producto por código de barras. El usuario debe poder ingresar o pegar un barcode, ubicar rápidamente un `SuperItem` existente y asociar códigos nuevos sin crear catálogos paralelos ni tocar stock.

## Alcance

### Incluido
- Alias locales de barcode asociados a `SuperItem` existente.
- Código persistido como texto normalizado para preservar ceros iniciales.
- Búsqueda local: si existe alias, abrir/resaltar el item; si no existe, permitir asociación manual a un item existente.
- Entrada manual/pegado obligatoria; cámara solo como mejora progresiva si el navegador lo soporta.
- Operaciones de barcode sin cambios en `currentStock`, `checked` ni movimientos.

### Fuera de alcance
- OCR, lectura de tickets, precios, tiendas, presentaciones, compras automáticas y lista sugerida.
- Lookup externo, incluido Open Food Facts.
- Nuevo Producto Base, catálogo paralelo o reemplazo de identidad de `SuperItem`.

## Capacidades

### Nuevas capacidades
- Ninguna.

### Capacidades modificadas
- `super-inventory`: habilitar alias locales de barcode como identificación auxiliar de productos existentes y ajustar el límite que antes prohibía barcode, manteniendo OCR y automatización fuera.

## Enfoque propuesto

Crear una relación tipo `SuperItemBarcodeAlias` con `code: String`, `format` opcional, `item`, `active` y `createdAt`, con unicidad para alias activos. Exponer endpoints de lookup/asociación/remoción y sumar UI mínima manual-first. OCR se difiere intencionalmente hasta tener la base determinística de alias locales.

## User stories / criterios de aceptación

- Como usuario, ingreso o pego un barcode existente y el sistema muestra/resalta el producto asociado.
- Como usuario, ingreso un barcode no encontrado y puedo asociarlo manualmente a un producto existente.
- Como sistema, rechazo duplicados activos y preservo códigos con ceros iniciales.
- Como sistema, lookup/asociación/remoción de barcode no modifica stock, `checked` ni crea movimientos.
- Como UI, si no hay `BarcodeDetector` o permisos de cámara, el flujo manual sigue disponible.

## Áreas afectadas

| Área | Impacto | Descripción |
|------|---------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modificado | Reglas de barcode local y límites de OCR. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | Modificado | Alias, servicio, repositorio, DTOs y endpoints. |
| `src/main/resources/static/` | Modificado | UI y helpers manual-first. |
| `src/test/` | Modificado | Contratos backend y UI estática. |

## Riesgos y rollback

| Riesgo | Mitigación |
|--------|------------|
| Cámara no soportada o sin permisos | Mantener input manual obligatorio. |
| Barcode numérico pierde ceros | Persistir y validar como texto. |
| Constraints H2 con `ddl-auto=update` | Probar duplicados y H2 persistente. |

Rollback: revertir entidades/endpoints/UI/tests del cambio; los alias son datos auxiliares y no alteran productos, stock ni movimientos.

## Forecast de revisión / PR slicing

Slice local/manual-first: riesgo medio, debería entrar en una PR bajo 800 líneas. Si se suma cámara completa, lookup externo u OCR, aplicar auto-chain por work units.

## Criterios de éxito

- [ ] Barcode local encuentra/asocia/remueve alias sobre `SuperItem`.
- [ ] Ceros iniciales y duplicados quedan cubiertos por tests.
- [ ] Stock, `checked` y movimientos quedan invariantes.
