# Tasks: Etapa 1 — Evolución del modelo actual

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 500-750 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 backend model/API/tests → PR 2 UI/static contracts |
| Delivery strategy | force-chained / auto-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Modelo/API compatible de `super_items` | PR 1 | Tests MockMvc/JPA incluidos; sin UI. |
| 2 | UI mínima y contratos estáticos | PR 2 | Depende de PR 1; mantener lista manual. |

## Phase 1: RED backend contracts

- [x] 1.1 En `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`, agregar tests fallidos para payload viejo pendiente y payload nuevo con `unit`, `habitualObjective`, `configured`.
- [x] 1.2 En `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java`, cubrir objetivo inválido, `checked` preservado, `uncheck-all` sin tocar configuración y delete lógico oculto.

## Phase 2: GREEN backend model/API

- [x] 2.1 En `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java`, agregar `ITEM_UNIT_MAX_LENGTH = 40`.
- [x] 2.2 En `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java`, agregar `unit`, `habitualObjective` nullable y helper derivado; preservar `checked` y `active`.
- [x] 2.3 En `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` y `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemResponse.java`, extender contrato opcional y respuesta `configured`.
- [x] 2.4 En `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java`, trim de `unit`, validación `habitualObjective > 0`, mapeo, preservación de `checked` y baja lógica.
- [x] 2.5 En `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRepository.java`, revisar consultas activas para excluir `active=false` sin tabla paralela.
- [x] 2.6 En `src/main/java/com/gentleia/landingtarjetas/shared/ApiExceptionHandler.java`, agregar etiquetas para `unit` y `habitualObjective`.

## Phase 3: RED/GREEN UI mínima

- [x] 3.1 En `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` y `src/test/resources/static-ui-contract-tests.mjs`, agregar contratos fallidos de campos, payload e indicador.
- [x] 3.2 En `src/main/resources/static/index.html`, agregar inputs opcionales de unidad/objetivo y columna/indicador de configuración.
- [x] 3.3 En `src/main/resources/static/js/supermarket.js`, enviar campos opcionales, renderizar configurado/pendiente y conservar lista manual basada en `checked`.
- [x] 3.4 En `src/main/resources/static/css/styles.css`, ajustar estilos responsive mínimos del indicador.

## Phase 4: Verificación

- [x] 4.1 Ejecutar `mvn -Dtest=SupermarketControllerTests,StaticUiContractTests test`; corregir solo fallos de Etapa 1.
- [x] 4.2 Ejecutar `mvn test`; confirmar categorías/lista manual intactas y ausencia de stock, movimientos, precios, barcode, OCR o sugeridos.
