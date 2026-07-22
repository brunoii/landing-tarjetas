# Proposal: Super Inventory Stage 14 Product Price Observation Filter

## Intent

Completar el loop de usabilidad de Etapa 13: después de registrar o sincronizar observaciones de precio, el usuario debe poder ver rápidamente las observaciones de un producto concreto sin revisar la lista global reciente.

## Scope

### In Scope
- Agregar en la UI existente una vista contextual de observaciones de precio filtradas por producto.
- Reusar `GET /api/super/price-observations?itemId=&limit=50` y `superPriceObservations(filters)` si siguen siendo suficientes.
- Mantener la lista global reciente y un control compacto para volver a ella.
- Cubrir el contrato UI/static con comportamiento product-scoped y exclusiones de Etapa 15.
- Planificar entrega encadenada bajo presupuesto de revisión de 400 líneas.

### Out of Scope
- Backend nuevo, salvo brecha real detectada en spec/design.
- Comparación, gráficos, tiendas/comercios, múltiples precios/presentaciones.
- Barcode, OCR, ticket, foto, scraping, automatización o alcance de Etapa 15.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `super-inventory`: la UI debe exponer explícitamente observaciones de precio filtradas por producto, preservando el historial global reciente y el filtrado backend existente.

## Approach

Implementar un drilldown compacto en la tabla/listado existente: una acción por fila de producto solicita `superPriceObservations({ itemId, limit: 50 })`, actualiza título/estado vacío con contexto del producto y permite volver a observaciones recientes globales. Evitar paneles nuevos amplios porque la celda de acciones ya está cargada. Mantener Strict TDD con `mvn test`; si el diff crece, separar en PRs stacked-to-main: spec/contract, UI behavior, archivo/publicación.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modified | Requisito vivo deberá exigir vista UI por producto. |
| `src/main/resources/static/index.html` | Modified | Título/estado contextual y control de retorno compacto. |
| `src/main/resources/static/js/supermarket.js` | Modified | Acción por producto, llamada filtrada y render contextual. |
| `src/main/resources/static/js/api.js` | Unchanged expected | Ya soporta query params mediante filtros. |
| `src/test/resources/static-ui-contract-tests.mjs` | Modified | Prueba behavior-level del filtro por `itemId` y retorno global. |
| `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` | Modified | Guard contractual UI/static. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Celda de acciones saturada | Med | Copy corto/accesible y reutilizar patrón de historial. |
| Deriva a Etapa 15 | Med | Guards explícitos contra comparación, tiendas, OCR/foto/ticket y múltiples precios. |
| Cambio backend innecesario | Low | Reusar cobertura existente de `itemId`; agregar backend solo ante brecha probada. |

## Rollback Plan

Revertir la acción UI, estados contextuales y tests/static del cambio. El backend y datos existentes permanecen intactos porque el flujo solo lee observaciones append-only ya soportadas.

## Dependencies

- Etapa 13 archivada y endpoint `price-observations` con `itemId` disponible.
- Strict TDD activo; comando principal: `mvn test`.

## Success Criteria

- [ ] Desde un producto se muestran observaciones de precio filtradas por `itemId`.
- [ ] El usuario puede volver a observaciones recientes globales.
- [ ] No se agregan capacidades de comparación, comercio, multimedia ni Etapa 15.
- [ ] `mvn test` pasa.
