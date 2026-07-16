## Exploration: super-inventory-stage7-prices

### Current State

`main` está limpio y sincronizado con `origin/main`. La especificación vigente de `super-inventory` ya incluye Stage 6A: una presentación comercial default opcional sobre el `SuperItem` existente. Esa presentación se guarda en `commercialPresentationLabel` y `commercialPresentationQuantity`, se expone por `SuperItemRequest`/`SuperItemResponse`, se valida en `SupermarketService`, y se renderiza/edita desde la UI estática.

Los precios siguen explícitamente fuera de contrato: `openspec/specs/super-inventory/spec.md` prohíbe precios, tiendas, historial de precios y múltiples presentaciones; los contratos estáticos también bloquean `price`, `prices`, `store`, `shop`, `shops` y `presentations` en la superficie del super. No existe entidad, endpoint ni campo de precio actual en el módulo `supermarket`.

La presentación default ya resuelve el prerrequisito principal para precios mínimos: el sistema sabe qué presentación comercial se compra y qué cantidad contiene en la unidad de inventario. Por eso, la siguiente etapa segura puede introducir precio solo si el precio queda asociado a esa presentación default y no a la unidad de stock, tienda, historial ni compra automática.

Nota de entorno SDD: existe `openspec/specs/super-inventory/spec.md`, pero no se encontró `openspec/config.yaml` en el workspace actual.

### Affected Areas

- `openspec/specs/super-inventory/spec.md` — necesita delta para levantar solo precio actual de presentación default y mantener fuera tiendas, historial, múltiples presentaciones y automatizaciones.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItem.java` — lugar más pequeño para un precio nullable asociado al producto/presentación default existente, sin módulo paralelo.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SuperItemRequest.java` / `SuperItemResponse.java` — contratos compatibles para aceptar/exponer precio opcional.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketService.java` — validación transaccional: precio positivo/nullable y sin mutaciones de stock, `checked`, movimientos, sugerencias ni barcode.
- `src/main/java/com/gentleia/landingtarjetas/supermarket/SupermarketLimits.java` — posible límite/precisión explícita de precio si se centraliza la política.
- `src/main/resources/static/index.html` / `src/main/resources/static/js/supermarket.js` / `src/main/resources/static/css/styles.css` — formulario, render informativo y validación cliente; abrir solo semántica de precio permitida.
- `src/test/java/com/gentleia/landingtarjetas/SupermarketControllerTests.java` — regresiones de compatibilidad, validación y no mutación colateral.
- `src/test/java/com/gentleia/landingtarjetas/StaticUiContractTests.java` / `src/test/resources/static-ui-contract-tests.mjs` — permitir quirúrgicamente precio actual y seguir bloqueando tiendas, historial, múltiples presentaciones y automatización.

### Approaches

1. **Precio actual de la presentación default** — Agregar un precio nullable en pesos asociado a la presentación comercial default del `SuperItem` existente; mostrarlo como dato informativo y editable en el formulario/listado.
   - Pros: slice mínima; aprovecha Stage 6A; no requiere tiendas ni tablas nuevas; conserva categorías/productos/checkbox; mantiene stock solo por movimientos.
   - Cons: no conserva historial; no compara comercios; puede quedar desactualizado si no se comunica como precio de referencia.
   - Effort: Medium

2. **Historial de precios sin tiendas** — Crear observaciones de precio para la presentación default con fecha y precio, más vista reciente.
   - Pros: evita sobrescribir datos; prepara análisis de variación; más honesto para precios cambiantes.
   - Cons: agrega entidad/endpoints/UI de historial; aumenta el tamaño de PR; todavía no resuelve comparación por tienda.
   - Effort: High

3. **Precios por tienda y/o múltiples presentaciones** — Modelar tiendas, presentaciones múltiples y precios por combinación.
   - Pros: modelo más completo para compras reales y comparación.
   - Cons: demasiado amplio para el siguiente stage; reabre el diseño de producto/presentación; alto riesgo de romper presupuesto de revisión y constraints de no reconstruir módulo.
   - Effort: High

4. **Otro prerrequisito antes de precios** — Definir tiendas o monedas antes de cualquier precio.
   - Pros: reduce ambigüedad futura si el objetivo principal es comparación entre comercios.
   - Cons: el código ya tiene el prerrequisito mínimo para precio de presentación; introduce catálogo adicional sin valor inmediato de precio.
   - Effort: Medium

### Recommendation

Avanzar con **Approach 1: precio actual de la presentación default** como Stage 7. La propuesta debería definir el precio como un dato comercial opcional, nullable y de referencia, expresado en pesos, aplicable a la presentación default existente del `SuperItem`; no debe interpretarse como precio por unidad de inventario.

Alcance recomendado: persistir/exponer/editar un precio actual positivo o ausente para productos existentes, renderizarlo de forma informativa y mantener bloqueados tiendas, historial, múltiples presentaciones, conversiones automáticas, estimaciones automáticas de compra y mutaciones de stock. Si se quiere mostrar en listas, debe ser solo texto informativo; no debe modificar la lista manual, sugerida, `checked`, movimientos ni stock.

### Risks

- Un precio único puede quedar viejo rápido; mitigación: nombrarlo como precio actual/de referencia y dejar historial para Stage 7B.
- Mezclar precio con sugerencias puede tentar a convertir cantidades o calcular totales prematuros; mitigación: no introducir conversiones ni estimaciones automáticas en esta slice.
- Los contratos estáticos bloquean términos de precio; habrá que abrir `price` de forma quirúrgica y mantener bloqueados `store`, `shop`, `shops`, `presentations`, historial y automatizaciones.
- Si se agrega columna de UI a la tabla ya cargada, puede aumentar el diff; mantener split stacked-to-main: backend/API/tests, UI/static/tests y archivo/spec sync.
- No hay `openspec/config.yaml`; las fases siguientes deberían apoyarse en `openspec/specs/` y reglas pasadas hasta que el config se restaure o regenere.

### Ready for Proposal

Yes — listo para propuesta. El mensaje al usuario/orquestador: después de Stage 6A, la etapa segura de precios no es historial ni tiendas; es un precio actual nullable en pesos asociado a la presentación comercial default existente, preservando el módulo actual y dejando historial, tiendas, múltiples presentaciones y automatizaciones para etapas posteriores.
