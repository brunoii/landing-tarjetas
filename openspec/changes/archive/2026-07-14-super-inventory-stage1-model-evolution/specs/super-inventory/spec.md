# Especificación super-inventory

## Propósito

Define la Etapa 1 para evolucionar la Lista del Super hacia inventario doméstico configurable, preservando productos, categorías, endpoints y la lista manual actual.

## Requirements

### Requirement: Evolución compatible del producto existente

El sistema MUST tratar los productos existentes del super como la base progresiva del futuro Producto Base, sin crear una base paralela ni exigir reconstrucción funcional.

#### Scenario: Producto existente sigue disponible
- GIVEN un producto existente con categoría, nombre, notas y estado `checked`
- WHEN se consulta la lista de productos
- THEN el producto MUST aparecer con sus datos actuales preservados
- AND su identidad funcional MUST seguir siendo la misma

#### Scenario: Sin Producto Base paralelo
- GIVEN la Etapa 1 está activa
- WHEN se registra o actualiza un producto del super
- THEN el sistema MUST usar el modelo evolutivo existente
- AND MUST NOT requerir una tabla o catálogo paralelo de Producto Base

### Requirement: Estado de configuración de producto

El sistema MUST representar si un producto está configurado para inventario mediante unidad y objetivo opcionales.

#### Scenario: Producto configurado
- GIVEN un producto con unidad y objetivo válidos
- WHEN se consulta el producto
- THEN su estado de configuración MUST indicar que está configurado

#### Scenario: Producto pendiente por datos faltantes
- GIVEN un producto sin unidad o sin objetivo
- WHEN se consulta el producto
- THEN su estado MUST indicar configuración pendiente
- AND el sistema MUST NOT inventar unidad ni objetivo por defecto

#### Scenario: Objetivo inválido
- GIVEN una solicitud con objetivo inválido para configuración
- WHEN se crea o actualiza el producto
- THEN el sistema MUST rechazar la solicitud con error de validación
- AND MUST NOT modificar el producto persistido

### Requirement: Contratos retrocompatibles de productos

El sistema MUST aceptar contratos actuales sin unidad/objetivo y MAY aceptar contratos extendidos con esos datos opcionales.

#### Scenario: Payload anterior sigue funcionando
- GIVEN una solicitud actual con nombre, categoría, notas y `checked`
- WHEN se crea o actualiza un producto
- THEN la operación MUST conservar el comportamiento actual
- AND el producto resultante MUST quedar pendiente si falta unidad u objetivo

#### Scenario: Respuesta extendida compatible
- GIVEN productos con y sin configuración completa
- WHEN se consulta `/api/super/items`
- THEN la respuesta MUST incluir los datos actuales
- AND SHOULD exponer unidad, objetivo y estado cuando existan o apliquen

### Requirement: `checked` como intención manual de compra

El sistema MUST preservar `checked` exclusivamente como intención manual de compra pendiente.

#### Scenario: Configurar producto no cambia intención manual
- GIVEN un producto con `checked=true`
- WHEN se completa o modifica su configuración de unidad/objetivo
- THEN `checked` MUST permanecer sin cambios
- AND MUST NOT convertirse en cantidad, stock ni movimiento

#### Scenario: Desmarcado masivo conserva su alcance
- GIVEN productos marcados manualmente para comprar
- WHEN se ejecuta el desmarcado masivo
- THEN el sistema MUST cambiar solo la intención manual `checked`
- AND MUST NOT alterar unidad, objetivo ni estado de configuración

### Requirement: Lista manual y categorías preservadas

El sistema MUST mantener categorías, productos, generación manual de lista y endpoints actuales de forma compatible.

#### Scenario: Lista manual se genera igual
- GIVEN productos configurados y pendientes con `checked=true`
- WHEN se genera la lista manual
- THEN la lista MUST incluirlos según la regla actual basada en `checked`
- AND MUST NOT usar stock real ni sugerencias automáticas

#### Scenario: Categorías sin cambio funcional
- GIVEN categorías existentes activas
- WHEN se crean, editan, listan o usan en productos
- THEN el comportamiento actual MUST preservarse

### Requirement: Límites explícitos de Etapa 1

El sistema MUST NOT introducir stock real, movimientos, precios, presentaciones, barcode, OCR ni lista sugerida en esta etapa.

#### Scenario: Sin semántica de inventario real
- GIVEN un producto con unidad y objetivo
- WHEN se consulta o genera la lista
- THEN el sistema MUST NOT informar stock real ni movimientos
- AND MUST NOT derivar compras sugeridas desde objetivo

#### Scenario: Campos fuera de alcance
- GIVEN una solicitud que intenta gestionar precio, presentación, barcode, OCR, stock o movimiento
- WHEN se procesa la solicitud de Etapa 1
- THEN el sistema MUST tratar esos datos como fuera de contrato
- AND MUST NOT persistirlos como comportamiento soportado
