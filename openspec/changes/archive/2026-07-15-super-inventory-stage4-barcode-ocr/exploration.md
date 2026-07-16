## Exploration: super-inventory-stage4-barcode-ocr

### Current State

`main` está limpio e incluye Etapas 1/2/3 archivadas. La fuente vigente es `openspec/specs/super-inventory/spec.md`: `SuperItem` es la base evolutiva del producto, con categoría, nombre, notas, `checked`, `unit`, `habitualObjective`, `currentStock` nullable y `quickQuantity`. No existe Producto Base paralelo y esa restricción sigue siendo clave.

La Etapa 3 dejó movimientos transaccionales (`ADJUSTMENT`, `PURCHASE`, `CONSUMPTION`, `QUICK_CONSUMPTION`), historial reciente, consumo rápido, confirmación de stock negativo y compatibilidad H2 para `movement_type`. Barcode y OCR siguen fuera de contrato; los tests estáticos todavía bloquean términos `barcode`, `ocr`, `price`, `suggested` en `supermarket.js`.

El stack condiciona la solución: Spring Boot + JPA + H2 local con `ddl-auto=update`, UI estática sin bundler, tests Java/Node contract y sin mecanismo de secretos para APIs externas. La Web Barcode Detection API es experimental/no Baseline y requiere contexto seguro; `getUserMedia` requiere permisos y contexto seguro, aunque `localhost` califica. Open Food Facts permite lookup público de productos por barcode pero tiene red, rate limits y cobertura variable. OCR offline con Tesseract.js es posible, pero por defecto arrastra worker/core/lang desde CDN salvo que se auto-hospeden assets; cloud OCR implicaría secretos, costo y red.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — debe modificar el límite que hoy prohíbe barcode/OCR y mantener fuera precios, presentaciones y lista sugerida automática.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` — producto existente seguirá siendo la entidad funcional; no debe aparecer una base paralela.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/` — posible alias de barcode, repositorio, servicio, DTOs y endpoints de lookup/adjuntar.
- `src/main/resources/static/js/api.js` — helpers para lookup local y adjuntar/quitar barcode; cache token nuevo.
- `src/main/resources/static/js/supermarket.js` — flujo UI manual-first para ingresar/escanear código y asociarlo a un item existente.
- `src/main/resources/static/index.html` / `css/styles.css` — controles mínimos de búsqueda/asociación sin rediseñar la pantalla.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — contratos de alias, lookup, duplicados, preservación de stock/movimientos.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — contratos de UI, fallback sin APIs del navegador y nuevos tokens.
- `openspec/changes/archive/2026-07-14-super-inventory-stage3-movements/` — evidencia base: Stage 3 pasó 15/15 escenarios y confirmó que barcode/OCR no fueron introducidos.

### Approaches

1. **Barcode alias local/manual-first** — Agregar alias de barcode asociados a `SuperItem` y una búsqueda local por código. La UI permite escribir o pegar el código; el escaneo por cámara puede ser mejora progresiva si `BarcodeDetector` existe.
   - Pros: Bajo riesgo, offline, sin secretos, respeta `SuperItem` como producto base, valor inmediato para encontrar/adjuntar productos.
   - Cons: No completa datos desde catálogos externos; requiere que el usuario asocie códigos la primera vez.
   - Effort: Medium

2. **Barcode lookup externo opcional** — Consultar una API pública como Open Food Facts cuando el código no existe localmente y sugerir nombre/categoría.
   - Pros: Puede acelerar altas de productos conocidos.
   - Cons: Red, rate limits, cobertura regional incompleta, datos no confiables para stock doméstico, necesita política de fallback/caché.
   - Effort: Medium/High

3. **OCR de ticket/producto** — Capturar imagen o texto de ticket/producto y generar sugerencias para asociar productos o movimientos.
   - Pros: Potencialmente útil para cargar compras en lote.
   - Cons: Es impreciso, mezcla precios/tiendas/presentaciones, requiere parsing complejo, dependencias pesadas o servicios con secretos.
   - Effort: High

### Recommendation

Avanzar con una primera subetapa **Barcode local alias**. El flujo mínimo útil es: ingresar o escanear código → buscar alias local → si existe, resaltar/abrir el `SuperItem`; si no existe, permitir asociarlo manualmente a un producto existente. Esto debe quedar offline/manual-first; el escáner de cámara debe ser opcional y con fallback a input manual.

Modelo recomendado: crear una relación de alias, por ejemplo `SuperItemBarcodeAlias`, con `code` como `String` (no numérico, para preservar ceros iniciales), `format` opcional, `item`, `active`, `createdAt`. El código debe ser único en alias activos y referenciar `SuperItem`; no debe crear un catálogo externo ni reemplazar la identidad del producto. En la primera slice, barcode no cambia `currentStock`, no crea movimientos y no modifica `checked`.

Barcode debe venir antes que OCR porque es determinístico, testeable, acotado y encaja en el modelo actual. OCR debería esperar hasta que existan alias locales y una política clara de sugerencias; de lo contrario empuja al sistema hacia precios, tiendas, presentaciones y compras automáticas antes de tener fundamentos.

Plan de pruebas recomendado: backend para attach/lookup/remove, duplicados, código no encontrado, item inactivo, preservación de ceros iniciales, no mutación de stock/movimientos; UI estática para helpers, fallback sin `BarcodeDetector`, asociación manual, cache tokens y ausencia de precios/OCR/lista sugerida; Node contract para funciones puras de normalización/render. Forecast inicial: una slice local bien cortada debería quedar cerca del presupuesto de 800 líneas; sumar cámara + lookup externo + OCR probablemente obliga a auto-chain.

### Risks

- `BarcodeDetector` no es Baseline y cámara requiere permisos/contexto seguro; no puede ser el único camino.
- Tratar códigos como números rompería ceros iniciales; deben persistirse como texto normalizado.
- Un solo campo `barcode` en `SuperItem` puede quedarse corto si más adelante hay múltiples envases/códigos; alias one-to-many evita rehacer el modelo sin crear Producto Base paralelo.
- Open Food Facts u otros catálogos agregan red, rate limits, datos incompletos y semántica externa que no debe contaminar el inventario local.
- OCR empuja naturalmente a precios, tiendas, presentaciones y compras automáticas; eso debe seguir fuera de la primera slice.
- H2 con `ddl-auto=update` no da migraciones versionadas; constraints/índices únicos deben diseñarse con cuidado y probarse en H2 persistente.
- Review risk: si se implementa barcode + cámara + lookup externo + OCR en una sola PR, se excede el presupuesto y se vuelve difícil revisar.

### Ready for Proposal

Yes — proponer Stage 4 como **barcode alias local/manual-first**, con OCR y lookup externo explícitamente fuera de la primera slice. El mensaje al usuario: empezar por asociar y buscar códigos locales sobre `SuperItem`; eso construye la base correcta para escaneo real sin comprar deuda técnica ni depender de servicios externos.
