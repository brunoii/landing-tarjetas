# Propuesta: Lista sugerida de compras Etapa 5

## Intención

Permitir que el inventario del super sugiera compras de forma derivada y segura cuando ya conoce objetivo habitual y stock actual, sin reemplazar la lista manual ni convertir sugerencias en compras, movimientos o cambios de stock.

## Alcance

### Incluido
- Endpoint/backend read-only para lista sugerida derivada desde productos activos configurados.
- Regla mínima: `unit` + `habitualObjective`, `currentStock != null`, `currentStock < habitualObjective`; cantidad sugerida = `habitualObjective - currentStock`.
- Render UI separado de la lista manual cuando el endpoint exista.

### Fuera de alcance
- Mutar `checked`, `currentStock` o movimientos desde sugerencias.
- Precios, tiendas, presentaciones comerciales, OCR, lookup externo, compra/consumo automático.
- Lista sugerida persistida, confirmable o mezclada con productos marcados manualmente.

## Capacidades

### Nuevas capacidades
- Ninguna.

### Capacidades modificadas
- `super-inventory`: agrega lista sugerida read-only y separada; modifica el límite vigente que prohibía sugerencias automáticas para permitir solo esta regla derivada mínima.

## Enfoque

Usar backend como fuente de verdad: calcular sugerencias en servicio/controlador desde `SuperItem` activo y exponer DTO dedicado. La UI consume ese endpoint y muestra una sección/card independiente; `generatedSuperListText(items)` sigue dependiendo solo de `checked`.

## Áreas afectadas

| Área | Impacto | Descripción |
|------|---------|-------------|
| `openspec/specs/super-inventory/spec.md` | Modificado | Delta para habilitar sugerencias mínimas y conservar límites. |
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | Nuevo/Modificado | Endpoint, DTO y regla read-only. |
| `src/main/resources/static/js/api.js` | Modificado | Helper de consulta sugerida. |
| `src/main/resources/static/js/supermarket.js` | Modificado | Render separado sin tocar lista manual. |
| `src/main/resources/static/index.html`, `src/main/resources/static/css/styles.css` | Modificado | Sección/card mínima. |
| `src/test/java/com/gentleia/landingtarjetas/`, `src/test/resources/static-ui-contract-tests.mjs` | Modificado | Contratos backend/UI y guards estáticos. |

## Riesgos

| Riesgo | Probabilidad | Mitigación |
|--------|--------------|------------|
| Tratar stock desconocido como cero | Media | Exigir `currentStock != null`. |
| Mezclar sugeridos con manuales | Media | Endpoint/DTO y UI separados; no mutar `checked`. |
| Abrir scope comercial | Baja | Mantener guards contra precios, tiendas, presentaciones, OCR y automatización. |

## Plan de rollback

Eliminar endpoint/DTO/helper/render de sugerencias y revertir el delta de spec; los datos existentes no requieren migración porque no se persiste estado nuevo.

## Dependencias

- Campos existentes de `SuperItem`: `active`, `unit`, `habitualObjective`, `currentStock`, `checked`.

## Criterios de éxito

- [ ] Solo productos activos configurados y con stock conocido bajo objetivo aparecen sugeridos.
- [ ] La cantidad sugerida coincide con `habitualObjective - currentStock`.
- [ ] Consultar/renderizar sugerencias no cambia `checked`, `currentStock` ni movimientos.
- [ ] La lista manual marcada permanece separada y compatible.
