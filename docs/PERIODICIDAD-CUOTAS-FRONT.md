# Guía frontend — Periodicidad de cuotas

Esta guía cubre el cambio en cómo se configura cada cuánto se generan las cuotas (sociales y de
actividad): dejan de configurarse en **días** y pasan a una **periodicidad de calendario**
(semanal, mensual, bimestral, etc.). Reemplaza la parte de "cada N días" de
[CUOTAS-GENERACION-POR-DIAS-FRONT.md](CUOTAS-GENERACION-POR-DIAS-FRONT.md); el resto de esa
guía (`fechaGeneracion`, generación manual con `hasta`, balance) sigue vigente.

## Qué cambió en el funcionamiento

Antes, la próxima cuota se calculaba sumando N días a la anterior. Con 30 días, una cuota del
15/01 tenía la siguiente el 14/02, después el 16/03, y la fecha se iba corriendo.

Ahora se elige una periodicidad y la cuota se genera **el mismo día del período** que la
primera: si la primera cuota se generó el 15, las siguientes se generan el 15 que corresponda.

| Valor | Cada cuánto |
|---|---|
| `SEMANAL` | 1 semana (mismo día de la semana) |
| `MENSUAL` | 1 mes |
| `BIMESTRAL` | 2 meses |
| `TRIMESTRAL` | 3 meses |
| `CUATRIMESTRAL` | 4 meses |
| `SEMESTRAL` | 6 meses |
| `ANUAL` | 12 meses |

Ejemplo: socio inscripto a una actividad `MENSUAL` el **15/10/2026**:

| Cuota | `fechaGeneracion` | Cuándo se crea |
|---|---|---|
| 1ª | 15/10/2026 | En el momento de la inscripción |
| 2ª | 15/11/2026 | Job diario del 15/11 |
| 3ª | 15/12/2026 | Job diario del 15/12 |

La primera cuota se sigue generando al inscribirse (actividad) o al darse de alta el socio
(cuota social), como hasta ahora. Esa fecha es la que fija el día de las siguientes.

**Días 29, 30 y 31:** si el mes siguiente no tiene ese día, la cuota se genera el último día
del mes, y **las siguientes quedan en ese día**. Ejemplo `MENSUAL` desde el 31/01:
31/01 → 28/02 → 28/03 → 28/04…

**Cambiar la periodicidad** de un club o de una actividad no mueve la próxima cuota que ya está
calculada para cada socio o inscripción: esa se genera en la fecha prevista, y desde ahí las
siguientes usan la periodicidad nueva.

**Datos existentes:** todos los clubes y actividades quedan en `MENSUAL`. La próxima cuota de
cada socio e inscripción no se mueve; a partir de ella avanzan mes a mes.

El vencimiento **no cambia**: sigue siendo `fechaGeneracion` + `diasVencimientoCuota` (en días).

## Cambios en DTOs (breaking)

En el JSON la periodicidad viaja como **string** con el nombre del valor (`"MENSUAL"`). Un valor
que no sea uno de los siete responde **400**.

### Perfil del club

Aplica a `CompanyDTO` (respuesta de `GET /companies/current`, `PUT /companies/profile`, alta de
club, `POST /companies/paginate-club`) y a los requests `ClubProfileUpdateRequestDTO`
(`PUT /companies/profile`) y `ClubRegistrationRequestDTO` (`POST /sysadmin/clubs`).

- **Se elimina** `diasEntreGeneraciones` (entero).
- **Se agrega** `periodicidadCuotaSocial` (string, opcional en los requests).

| Campo | Default | Significado |
|---|---|---|
| `periodicidadCuotaSocial` | `MENSUAL` | Cada cuánto se genera la cuota social |
| `diasVencimientoCuota` | 10 | Sin cambios. Días desde la generación hasta el vencimiento de la cuota social |
| `diasVencimientoMoroso` | 31 | Sin cambios |
| `diasInhabilitacion` | 91 | Sin cambios |

En `PUT /companies/profile`, si no se envía, el club conserva su valor actual. En el alta, si no
se envía, queda `MENSUAL`.

Antes:

```json
{ "cuotaSocialHabilitada": true, "diasEntreGeneraciones": 30, "diasVencimientoCuota": 10 }
```

Ahora:

```json
{ "cuotaSocialHabilitada": true, "periodicidadCuotaSocial": "MENSUAL", "diasVencimientoCuota": 10 }
```

### Actividades

Aplica a `ActividadAltaCommand` (`POST /actividad`), `ActividadEdicionCommand`
(`PUT /actividad/{actividadUuid}`) y `ActividadDTO` (respuesta de alta, edición y
`GET /actividad/{actividadUuid}`).

- **Se elimina** `diasGeneracionCuota` (entero).
- **Se agrega** `periodicidadCuota` (string, opcional en los commands).

| Campo | Default | Significado |
|---|---|---|
| `periodicidadCuota` | `MENSUAL` | Cada cuánto se genera la cuota de la actividad |
| `diasVencimientoCuota` | 30 | Sin cambios. Días desde la generación hasta el vencimiento (≥ 1) |

En el alta, si no se envía, la actividad queda `MENSUAL`. En la edición, si no se envía, se
conserva el valor actual.

Antes:

```json
{ "nombre": "Natación", "importeCuota": 15000.00, "diasGeneracionCuota": 30, "diasVencimientoCuota": 30 }
```

Ahora:

```json
{ "nombre": "Natación", "importeCuota": 15000.00, "periodicidadCuota": "MENSUAL", "diasVencimientoCuota": 30 }
```

## Sin cambios

- Rutas: no se agregan ni se quitan endpoints.
- Cuotas (`CuotaResumenDTO`, `CuotaDetalleDTO`, paginado): mismos campos. `fechaGeneracion`
  ahora cae siempre en el mismo día del período.
- Generación manual (`POST /treasury/cuota/generacion/actividad|social?hasta=dd/MM/yyyy`): mismo
  contrato. Con `hasta` varios períodos adelante genera una cuota por período (ej. `MENSUAL` y
  `hasta` 3 meses adelante → 3 cuotas por inscripción).

## Sugerencias de UI

- Mostrar la periodicidad como un `select` con los siete valores y labels en castellano
  (Semanal, Mensual, Bimestral, Trimestral, Cuatrimestral, Semestral, Anual), preseleccionando
  `MENSUAL` en el alta.
- Al editar la periodicidad, conviene aclarar que el cambio aplica a partir de la próxima cuota
  de cada socio o inscripción, no a las ya generadas.
- Con `SEMANAL`, un `diasVencimientoCuota` de 7 o más hace que una cuota venza después de que
  se generó la siguiente. El backend no lo valida; si se quiere, se puede advertir en el form.

## Checklist para el front

- [ ] Perfil del club (alta sysadmin y edición): reemplazar el input numérico
      `diasEntreGeneraciones` por un select `periodicidadCuotaSocial`.
- [ ] Alta y edición de actividad: reemplazar `diasGeneracionCuota` por un select
      `periodicidadCuota`.
- [ ] Detalle de actividad y del club: leer `periodicidadCuota` / `periodicidadCuotaSocial` en
      lugar de los días.
- [ ] Quitar cualquier texto tipo "cada N días" referido a la generación de cuotas.
- [ ] (Opcional) Aviso al editar la periodicidad y al combinar `SEMANAL` con vencimientos largos.
