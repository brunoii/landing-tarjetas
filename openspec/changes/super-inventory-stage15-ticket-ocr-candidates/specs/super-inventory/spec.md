# Delta for super-inventory

## ADDED Requirements

### Requirement: Candidatos OCR transitorios de ticket

El sistema MUST aceptar una única imagen de ticket permitida para extraer candidatos revisables en memoria. La respuesta MUST incluir solo metadatos seguros, candidatos de fecha/fuente, líneas candidatas, precio, confianza y advertencias. El sistema MUST NOT persistir automáticamente imagen, texto OCR, candidatos, observaciones, precio actual, producto, movimiento de stock ni alias de barcode.

#### Scenario: Imagen válida devuelve candidatos transitorios
- GIVEN una imagen de ticket válida y dentro del límite permitido
- WHEN se solicita `POST /api/super/ticket-ocr/candidates`
- THEN el sistema MUST procesarla en memoria y devolver candidatos revisables
- AND MUST incluir confianza o advertencias cuando la extracción sea incierta

#### Scenario: Carga inválida no persiste datos
- GIVEN un archivo con tipo, tamaño o cantidad no permitida
- WHEN se solicita extracción OCR de ticket
- THEN el sistema MUST rechazar la solicitud con error explícito
- AND MUST NOT persistir imagen, texto, candidatos ni cambios de inventario/precio

#### Scenario: OCR pobre o línea no parseable
- GIVEN una imagen válida con baja confianza OCR o líneas ambiguas
- WHEN se devuelven candidatos
- THEN el sistema MUST marcar advertencias explícitas por baja confianza o línea no parseable
- AND MUST mantener esas filas como solo revisión hasta confirmación humana posterior

### Requirement: Confirmación humana antes de persistir precio

El sistema MUST requerir revisión humana antes de que cualquier candidato derivado de ticket pueda convertirse en observación de precio o actualización de precio actual/de referencia. La confirmación MUST usar el flujo explícito existente de creación de observaciones de precio para un producto activo, incluyendo su validación y el comportamiento opcional de `syncCurrentReferencePrice`.

#### Scenario: Candidato confirmado crea observación por flujo existente
- GIVEN un candidato revisado con producto activo, precio válido, fecha/fuente válidas o ausentes
- WHEN el usuario confirma explícitamente la creación de observación
- THEN el sistema MUST usar el contrato existente de creación de observaciones de precio
- AND MAY sincronizar el precio actual/de referencia solo si se selecciona la opción explícita

#### Scenario: Candidatos sin confirmar no mutan estado
- GIVEN candidatos OCR visibles en la UI de revisión
- WHEN el usuario no confirma, refresca o abandona la vista
- THEN el sistema MUST descartar los candidatos transitorios
- AND MUST NOT crear observaciones, actualizar productos, stock, barcodes, imágenes ni texto

### Requirement: Frontera de alcance Stage 15 OCR

El sistema MUST limitar Stage 15 a candidatos OCR de imagen de ticket que alimentan revisión manual. MUST NOT introducir lectura de barcode, compras/consumos de stock, administración de fuentes, comparaciones/gráficos, totales de ticket, matching automático de productos más allá de candidatos, auto-creación de productos ni persistencia final automática.

#### Scenario: Funciones fuera de alcance permanecen ausentes
- GIVEN el usuario usa el flujo OCR de ticket
- WHEN el sistema muestra acciones o procesa candidatos
- THEN el sistema MUST NOT exponer lectura de barcode, movimiento de stock, totales, comparación ni administración de fuentes
- AND cualquier match de producto MUST permanecer como candidato que requiere selección/confirmación humana
