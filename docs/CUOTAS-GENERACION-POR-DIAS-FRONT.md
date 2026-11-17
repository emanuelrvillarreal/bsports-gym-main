# Guía frontend — Cuotas generadas cada N días

Esta guía cubre el cambio en la generación de cuotas (sociales y de actividad): dejan de ser
**mensuales** (una por `mes`/`anio`) y pasan a generarse **cada N días**. Afecta a los
listados y detalles de cuotas, a la generación manual y al balance. El detalle completo está
en [§16 de `docs/API.md`](API.md#16-tesorería--cuotas).

## Qué cambió en el funcionamiento

Antes, cada cuota pertenecía a un **período calendario** (`mes` + `anio`), y el sistema
generaba una por mes.

Ahora cada inscripción a una actividad y cada socio guardan **la fecha en la que les toca la
próxima cuota**. Todos los días el sistema revisa esas fechas:

- Si la fecha ya llegó, genera la cuota **con esa fecha** y corre la próxima fecha N días.
- Si no llegó, no hace nada.

Cada cuánto se genera:

| Tipo de cuota | Cada cuántos días | Dónde se configura |
|---|---|---|
| Actividad | `periodicidadCuota` de la actividad (default `MENSUAL`) | Alta y edición de actividades |
| Social | `periodicidadCuotaSocial` del club (default `MENSUAL`) | Perfil del club (`PUT /companies/profile`) |

Cuándo vence:

| Tipo de cuota | Vencimiento | Dónde se configura |
|---|---|---|
| Actividad | `fechaGeneracion` + `diasVencimientoCuota` de la actividad (default 30) | Por ahora solo en base de datos |
| Social | `fechaGeneracion` + `diasVencimientoCuota` del club (default 10) | Perfil del club (`PUT /companies/profile`) |

`diasVencimientoMoroso` del club **ya no define el vencimiento** de la cuota social: solo cuenta
los días desde el vencimiento para que el grupo pase a `MOROSO`.

Ejemplo: socio inscripto a una actividad el **01/10/2026**, con 30 días de generación:

| Cuota | `fechaGeneracion` | Cuándo se crea |
|---|---|---|
| 1ª | 01/10/2026 | En el momento de la inscripción |
| 2ª | 31/10/2026 | Job diario del 31/10 |
| 3ª | 30/11/2026 | Job diario del 30/11 |

Al inscribirse (o al darse de alta el socio, para la cuota social) la primera cuota se sigue
generando en el mismo momento, como hasta ahora.

## Cambios en DTOs (breaking)

### Cuotas — `CuotaResumenDTO` y `CuotaDetalleDTO`

Aplica a las cuotas por persona, al grupo del titular y al detalle de una cuota.

- **Se eliminan** `mes` y `anio`.
- **Se agrega** `fechaGeneracion`, formato `dd/MM/yyyy` (igual que `vencimiento`).

El listado paginado (`POST /treasury/cuota/paginate`, `CuotaPaginateRowDTO`) no tenía
`mes`/`anio`, así que no cambia.

Antes:

```json
{
  "cuotaUuid": "…",
  "concepto": "Cuota actividad Natación Octubre 2026",
  "vencimiento": "31/10/2026",
  "mes": 10,
  "anio": 2026
}
```

Ahora:

```json
{
  "cuotaUuid": "…",
  "concepto": "Cuota actividad Natación Octubre 2026",
  "vencimiento": "31/10/2026",
  "fechaGeneracion": "01/10/2026"
}
```

Qué revisar en el front:

- Cualquier pantalla que muestre o agrupe cuotas por mes/año (columna "Período", filtros,
  agrupaciones) tiene que pasar a usar `fechaGeneracion`.
- Con ciclos menores a 30 días puede haber **dos cuotas de la misma actividad en el mismo
  mes**. No hay que asumir una cuota por mes.
- El `concepto` sigue diciendo el mes y año de la fecha de generación
  (ej. "Cuota actividad Natación Octubre 2026"). Sirve para mostrar, no para agrupar.

### Resultado de la generación — `CuotaActividadGeneracionResultDTO` / `CuotaSocialGeneracionResultDTO`

- **Se eliminan** `anio` y `mes`.
- **Se agrega** `hasta` (`dd/MM/yyyy`): la fecha de corte con la que se generó.

```json
{ "hasta": "31/12/2026", "companiesProcessed": 1, "cuotasGeneradas": 42, "cuotasOmitidas": 1 }
```

| Campo | Significado |
|---|---|
| `hasta` | Fecha de corte usada |
| `companiesProcessed` | Siempre `1` en la generación manual (solo el club del header) |
| `cuotasGeneradas` | Cuotas creadas. Puede ser más de una por socio o inscripción si había varios ciclos pendientes |
| `cuotasOmitidas` | Socios o inscripciones que correspondía facturar y no se pudo (actividad sin importe, socio sin titular o sin rango de cuota social). Se reintentan en la próxima corrida |

## Cambios en endpoints (breaking)

Los endpoints de generación manual por período **se eliminan**:

- ~~`POST /treasury/cuota/generacion/periodo/actividad?anio=&mes=`~~
- ~~`POST /treasury/cuota/generacion/periodo/social?anio=&mes=`~~

Y se reemplazan por:

| Método | Ruta | Permiso |
|---|---|---|
| `POST` | `/treasury/cuota/generacion/actividad?hasta=dd/MM/yyyy` | rol `ADMIN` |
| `POST` | `/treasury/cuota/generacion/social?hasta=dd/MM/yyyy` | rol `ADMIN` |

Requieren JWT y header `X-Company-Uuid`, igual que el resto del backoffice.

| Query param | Obligatorio | Formato | Notas |
|---|---|---|---|
| `hasta` | Sí | `dd/MM/yyyy` | Si falta o tiene otro formato: **400** |

Qué hacen: generan, para el club del header, **todas** las cuotas pendientes hasta la fecha
`hasta`. Es lo mismo que hace el job diario con la fecha de hoy.

- **Varios ciclos:** si a una inscripción le tocaban varias cuotas antes de `hasta`, las genera
  todas. Por ejemplo, con `hasta` 60 días adelante y ciclo de 30 días, genera 2 cuotas por
  inscripción.
- **Repetir no duplica:** llamarlo de nuevo con la misma fecha (o una anterior) devuelve
  `cuotasGeneradas: 0`.
- **Fechas futuras:** están permitidas y sirven para probar. Pero ⚠️ las cuotas generadas por
  adelantado ya no se vuelven a generar cuando llega la fecha, y si después cambia el importe no
  se actualizan. Si el front ofrece esta acción, conviene pedir confirmación cuando `hasta` sea
  posterior a hoy.

Ejemplo:

```
POST /treasury/cuota/generacion/actividad?hasta=31/12/2026
```

Las `/` de la fecha pueden ir tal cual en la query string. Si el cliente HTTP las codifica
como `%2F`, el backend las acepta igual.

## Perfil del club — campo nuevo `diasVencimientoCuota`

`CompanyDTO` (respuesta del club) y los requests de perfil y registro del club
(`ClubProfileUpdateRequestDTO`, `ClubRegistrationRequestDTO`) suman **`diasVencimientoCuota`**
(entero, opcional):

| Campo | Default | Significado |
|---|---|---|
| `periodicidadCuotaSocial` | `MENSUAL` | Cada cuánto se genera la cuota social: `SEMANAL`, `MENSUAL`, `BIMESTRAL`, `TRIMESTRAL`, `CUATRIMESTRAL`, `SEMESTRAL` o `ANUAL` |
| `diasVencimientoCuota` | 10 | **Nuevo.** Días desde la generación hasta el vencimiento de la cuota social |
| `diasVencimientoMoroso` | 31 | Días desde el vencimiento para que el grupo pase a `MOROSO` |
| `diasInhabilitacion` | 91 | Días desde el vencimiento para que el grupo pase a `INHABILITADO` |

Si no se envía, el club conserva su valor actual. Los clubes existentes arrancan con 10.

Qué revisar en el front: agregar el campo al formulario del perfil del club, y si hoy el label
de `diasVencimientoMoroso` dice algo como "días de vencimiento", cambiarlo por algo como
"días para moroso".

## Balance — `/treasury/balance`

No cambian las rutas ni los DTOs (`BalanceMesDTO` / `BalanceResumenMesDTO` siguen teniendo
`periodo`, `mes` y `anio`, porque el balance **sí** es mensual). Solo cambia cómo se calcula:

- `porCobrar`: cuotas `PENDIENTE`/`VENCIDA` cuya **`fechaGeneracion` cae dentro del mes**.
- El histórico lista los meses en los que hubo cuotas generadas o movimientos.

## Anulación de cuotas

No cambia el endpoint (`POST /treasury/cuota/uuid/{cuotaUuid}/anular`). Una cuota anulada
**no** se vuelve a generar en las corridas siguientes: la próxima fecha del socio o la
inscripción ya había avanzado cuando se creó.

## Checklist para el front

- [ ] Reemplazar `mes`/`anio` por `fechaGeneracion` en listados y detalle de cuotas.
- [ ] Quitar cualquier supuesto de "una cuota por mes" (agrupaciones, validaciones, UI de período).
- [ ] Reemplazar la generación manual por período (`anio`/`mes`) por la nueva con `hasta` en
      `dd/MM/yyyy`, y la ruta sin `/periodo`.
- [ ] Leer `hasta` (en vez de `anio`/`mes`) en la respuesta de la generación.
- [ ] Agregar `diasVencimientoCuota` al perfil del club y revisar el label de `diasVencimientoMoroso`.
- [ ] (Opcional) Confirmación al generar con una fecha futura.
