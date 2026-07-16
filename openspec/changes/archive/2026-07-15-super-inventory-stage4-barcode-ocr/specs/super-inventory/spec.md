# Delta for super-inventory

## ADDED Requirements

### Requirement: Alias locales de barcode sobre SuperItem existente

El sistema MUST persistir códigos de barcode como texto asociado solo a un `SuperItem` existente. MUST preservar ceros iniciales y MUST NOT crear Producto Base, catálogo paralelo ni identidad de producto alternativa.

#### Scenario: Lookup con alias activo
- GIVEN un alias activo con código `0075012345678` asociado a un `SuperItem`
- WHEN se busca exactamente ese código
- THEN el sistema MUST devolver el `SuperItem` asociado
- AND MUST preservar el código como texto sin perder ceros iniciales

#### Scenario: Lookup sin alias activo
- GIVEN no existe alias activo para un código ingresado
- WHEN se busca ese código
- THEN el sistema MUST responder como no encontrado
- AND MAY permitir asociarlo manualmente a un `SuperItem` existente

#### Scenario: Adjuntar alias a producto existente
- GIVEN un `SuperItem` existente y un código sin alias activo
- WHEN el usuario adjunta el código al producto
- THEN el sistema MUST crear un alias activo para ese `SuperItem`

#### Scenario: Rechazo de duplicado activo
- GIVEN un código ya existe como alias activo
- WHEN se intenta adjuntar el mismo código a cualquier producto
- THEN el sistema MUST rechazar la operación por duplicado
- AND MUST NOT crear otro alias activo

#### Scenario: Remover alias
- GIVEN un alias activo asociado a un producto
- WHEN el usuario lo remueve
- THEN el sistema MUST desactivarlo o removerlo
- AND el lookup futuro MUST ignorar ese alias

### Requirement: Barcode manual-first sin impacto en inventario

Las operaciones de barcode MUST NOT modificar `currentStock`, `checked` ni movimientos. La entrada manual o pegado MUST estar disponible; cámara o `BarcodeDetector` MAY existir solo como mejora progresiva. El comportamiento de Etapas 1, 2 y 3 MUST permanecer intacto.

#### Scenario: Estado de inventario preservado
- GIVEN un producto con `currentStock`, `checked` e historial de movimientos
- WHEN se busca, adjunta o remueve un barcode
- THEN esos valores MUST permanecer sin cambios
- AND MUST NOT registrarse movimiento de stock

#### Scenario: Fallback manual obligatorio
- GIVEN el navegador no soporta cámara o deniega permisos
- WHEN el usuario ingresa o pega un código manualmente
- THEN el lookup y la asociación manual MUST seguir disponibles

#### Scenario: Etapas previas intactas
- GIVEN categorías, lista manual, snapshot, cantidad rápida y movimientos existentes
- WHEN se usa o no se usa barcode
- THEN los contratos de Etapas 1, 2 y 3 MUST conservar su comportamiento

## MODIFIED Requirements

### Requirement: Límites explícitos de Etapa 2

El sistema MUST limitar la primera slice de Etapa 4 a alias locales de barcode manual-first sobre `SuperItem` existente. SHALL NOT introducir OCR, lookup externo, precios, tiendas, presentaciones, automatización de compras ni lista sugerida automática.
(Previously: Etapa 3 prohibía barcode completamente junto con precios, presentaciones, OCR y lista sugerida automática.)

#### Scenario: Sin lista sugerida automática
- GIVEN un producto con unidad, objetivo y stock cargado
- WHEN se consulta o genera la lista
- THEN el sistema MUST NOT sugerir compras automáticamente
- AND MUST NOT derivar compras desde objetivo o stock

#### Scenario: Campos fuera de alcance
- GIVEN solicitud con OCR, lookup externo, precio, tienda, presentación, compra automática o lista sugerida automática
- WHEN se procesa en la primera slice de Etapa 4
- THEN el sistema MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado

#### Scenario: Lista manual separada
- GIVEN un producto con `checked=true`
- WHEN se genera la lista manual o cambia `checked`
- THEN MUST NOT modificar `currentStock`
- AND MUST NOT crear movimientos
