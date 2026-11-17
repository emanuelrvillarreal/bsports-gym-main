# Guía frontend — Porcentaje de asistencia en el calendario del profesor

Esta guía cubre el campo nuevo `porcentajeAsistencia` en la respuesta del calendario del
profesor. Ver también [§14 de `docs/API.md`](API.md#14-calendario) para la referencia formal.

## Contexto

El calendario del profesor ahora indica, para cada clase (slot de actividad), qué porcentaje de
los alumnos quedó registrado como **presente**. No hay endpoint nuevo: se agrega un campo a la
respuesta del que ya existe.

## Endpoint

| Método | Ruta | Permiso |
|---|---|---|
| `POST` | `/portal/profesor/calendario` | `activity.view` (profesor habilitado para el portal) |

**Headers:** `Authorization: Bearer <jwt>`, `X-Company-Uuid: <companyUuid>`, `Content-Type: application/json`.

### Request (sin cambios)

```json
{ "desde": "01/11/2026", "hasta": "30/11/2026" }
```

Fechas en `dd/MM/yyyy`. **400** si `hasta` no es posterior a `desde`.

### Response — `200`, `List<CalendarioSlotDTO>`

Igual que antes, con un campo más:

```json
[
  {
    "slotUuid": "c9f3e6c8-56b2-400a-9214-e72174445e67",
    "eventoUuid": null,
    "tipo": "ACTIVIDAD",
    "referenciaUuid": "…",
    "referenciaNombre": "Fútbol Sub-15",
    "inicio": "03/11/2026 18:00:00",
    "fin": "03/11/2026 19:30:00",
    "estado": "CONFIRMADO",
    "porcentajeAsistencia": 66.7
  },
  {
    "slotUuid": null,
    "eventoUuid": "…",
    "titulo": "Torneo interno",
    "ubicacion": "Cancha 1",
    "tipo": null,
    "inicio": "07/11/2026 10:00:00",
    "fin": "07/11/2026 13:00:00",
    "estado": "CONFIRMADO",
    "porcentajeAsistencia": null
  }
]
```

| Campo | Tipo | Notas |
|---|---|---|
| `porcentajeAsistencia` | número (`Double`) o `null` | De 0 a 100, con **un decimal** (`66.7`, `100.0`, `0.0`) |

> Los eventos de actividad vienen con `tipo = null`. Para distinguir un slot de un evento usar
> `slotUuid` / `eventoUuid` (o `tipo`, que solo es `ACTIVIDAD` en los slots).

## Cómo se calcula

`porcentajeAsistencia = presentes / alumnos × 100`

- **Presentes:** alumnos con asistencia registrada como `PRESENTE` en ese slot.
- **Alumnos:** socios distintos inscriptos en la actividad.
  - Cuentan los que tienen la inscripción **activa**.
  - Cuentan también los que se **dieron de baja después** de esa clase (estaban inscriptos cuando
    se dictó). Los que se dieron de baja antes no cuentan.
- Tope en 100.

## Cuándo viene `null`

- **Eventos de actividad** (`eventoUuid` poblado): no tienen asistencia.
- **Slots sin alumnos** que contar: no hay base para calcular, no es lo mismo que 0 %.
- Los otros calendarios (`POST /calendario` y `POST /portal/calendario`) **no** traen el campo
  con valor: siempre `null`.

## Comportamiento a tener en cuenta

- **`0.0` no significa "ausentes".** Un slot con alumnos al que todavía no se le tomó asistencia
  también devuelve `0.0`, porque no hay presentes registrados. Para distinguir "sin tomar" de
  "todos ausentes" hay que consultar la planilla.
- **Puede no coincidir con la planilla.** `GET /portal/profesor/asistencia/slot/{slotUuid}`
  (el detalle por alumno) incluye, en clases pasadas, a **todos** los alumnos dados de baja sin
  mirar cuándo se fueron. Si se cuenta la lista de esa planilla, el total puede ser mayor que la
  base del porcentaje.
- **La fecha de baja es aproximada.** Se toma la última modificación de la inscripción, así que
  si algo la edita después de la baja, la fecha se corre.
- **El porcentaje es de lectura:** se recalcula en cada consulta, no hay nada que guardar.

## Sugerencia de UI

Mostrar el porcentaje como un indicador chico (por ejemplo "67 %") en la tarjeta de cada clase
del calendario del profesor, redondeando para mostrar. Ocultarlo cuando venga `null`. Para el
detalle por alumno, llevar al profesor a la planilla del slot con el `slotUuid` del mismo ítem.
