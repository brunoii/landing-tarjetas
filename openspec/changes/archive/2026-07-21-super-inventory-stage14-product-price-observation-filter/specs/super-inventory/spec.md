# Delta for super-inventory

## ADDED Requirements

### Requirement: Vista contextual de observaciones de precio por producto

El sistema MUST permitir que la UI existente muestre observaciones de precio filtradas por un producto específico desde el contexto del producto, preservando la vista global reciente como estado recuperable. La vista contextual MUST usar el identificador del producto como filtro, MUST mantener un encabezado o estado vacío que indique el producto seleccionado, y MUST ofrecer un control compacto para volver al historial global reciente. La UI MUST NOT introducir comparación, gráficos, tiendas/comercios, múltiples precios/presentaciones, barcode, OCR, ticket, foto, scraping, automatización ni alcance de Etapa 15.

#### Scenario: Ver observaciones de un producto
- GIVEN la lista de productos está visible y existe un producto con observaciones de precio
- WHEN el usuario solicita ver las observaciones de precio de ese producto
- THEN la UI MUST consultar observaciones filtradas por el identificador del producto con límite reciente seguro
- AND MUST renderizar la lista en contexto del producto seleccionado

#### Scenario: Producto sin observaciones visibles
- GIVEN la lista de productos está visible y un producto no tiene observaciones de precio recientes
- WHEN el usuario solicita ver las observaciones de precio de ese producto
- THEN la UI MUST mostrar un estado vacío contextual para ese producto
- AND MUST NOT ocultar la posibilidad de volver al historial global reciente

#### Scenario: Volver al historial global reciente
- GIVEN la UI está mostrando observaciones filtradas por producto
- WHEN el usuario activa el control de retorno global
- THEN la UI MUST consultar observaciones recientes sin filtro de producto
- AND MUST restaurar el encabezado o estado de historial global reciente

#### Scenario: Frontera de alcance Stage 14
- GIVEN el usuario consulta observaciones desde un producto
- WHEN la UI renderiza acciones, estados o datos del historial
- THEN el sistema MUST NOT exponer comparación, comercios, multimedia, múltiples precios/presentaciones ni capacidades de Etapa 15
- AND MUST NOT requerir cambios backend si el filtro por producto existente satisface el contrato
