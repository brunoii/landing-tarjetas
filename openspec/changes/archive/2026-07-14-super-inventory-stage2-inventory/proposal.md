# Propuesta: Etapa 2 Inventario del Super

## Intención

Incorporar un snapshot de inventario inicial para productos del super sin romper la lista manual actual ni adelantar el Historial/Movimientos visible de Etapa 3.

## Alcance

### Incluido
- Agregar `currentStock` nullable: productos existentes quedan con stock desconocido, no `0`.
- Reutilizar `unit` y `habitualObjective` de Etapa 1; no crear campos duplicados.
- Agregar `quickQuantity` como cantidad rápida/default para lista y consumo futuro, sin implementar consumo.
- Mantener `checked` como intención manual/excepcional de compra; no convertirlo en stock ni movimiento.
- Agregar un comando enfocado de ajuste de stock con semántica interna auditable, sin historial visible.

### Excluido
- UI/API de historial completo de movimientos.
- Flujos de compra, consumo, precios, presentaciones comerciales, barcode, OCR.
- Lista sugerida automática o compras derivadas desde objetivo/stock.

## Capacidades

### Nuevas Capacidades
None

### Capacidades Modificadas
- `super-inventory`: agrega comportamiento de inventario Etapa 2 sobre el Producto Base evolutivo existente.

## Enfoque

Usar un slice incremental seguro: `currentStock` vive como snapshot de lectura rápida, pero todo cambio de stock pasa por un comando específico de ajuste que registre internamente un hecho auditable y actualice el snapshot. Esto respeta el principio movement-first sin exponer todavía pantallas ni endpoints de historial. Los updates genéricos de producto gestionan datos base/configuración, no mutaciones arbitrarias de stock.

## Áreas Afectadas

| Área | Impacto | Descripción |
|------|---------|-------------|
| `src/main/java/com/gentleia/landingtarjetas/supermarket/` | Modificado | Campos, DTOs, validación y comando de ajuste. |
| `src/main/resources/static/js/supermarket.js` | Modificado | Render de stock/cantidad rápida y lista manual compatible. |
| `src/main/resources/static/index.html` / `css/styles.css` | Modificado | Controles mínimos de inventario sin rediseño. |
| `src/test/java/...` / `src/test/resources/...` | Modificado | Contratos backend y UI estática. |
| `openspec/specs/super-inventory/spec.md` | Modificado | Delta de requisitos de Etapa 2. |

## Riesgos

| Riesgo | Probabilidad | Mitigación |
|--------|--------------|------------|
| Confundir stock desconocido con cero | Media | `currentStock=null` para datos migrados. |
| Debilitar movement-first | Media | Stock solo por comando enfocado auditable. |
| Sobrecrecer el alcance | Media | Diferir historial visible y automatizaciones. |

## Plan de Reversión

Revertir los cambios de modelo/API/UI/tests y dejar ignorados los campos nullable agregados por `ddl-auto=update`; al no reemplazar `checked`, la lista manual vuelve al comportamiento de Etapa 1.

## Dependencias

- Etapa 1 validada en `main` (`4a582fe`).
- Persistencia actual con Hibernate `ddl-auto=update`.

## Criterios de Éxito

- [ ] Productos existentes muestran stock desconocido y preservan unidad, objetivo y `checked`.
- [ ] Stock cambia solo mediante ajuste enfocado auditable.
- [ ] `quickQuantity` se persiste/expone sin activar consumo ni lista sugerida.
