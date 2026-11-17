# Guía frontend — Descuentos en cuotas

Esta guía cubre el modelo de descuentos sobre cuotas (sociales y de actividad) y cómo
asignarlos hoy desde los endpoints de alta. Ver también [§16.1 de `docs/API.md`](API.md#161-descuentos)
para la referencia formal.

> ✅ **El feature está completo de punta a punta.** Ya existe el ABM del catálogo (`POST`/`PUT`/
> `DELETE`/listado de `Descuento`, ver "ABM del catálogo" más abajo), así que el frontend puede
> crear un `Descuento` propio y usar su `descuentoUuid` real en cualquiera de los endpoints de
> asignación — ya no hace falta insertar nada a mano en la base para probar el flujo completo.

## Qué es un "descuento" acá

Un `Descuento` es una definición reutilizable por club: un **título** (el texto que
eventualmente se va a mostrar al socio, por ejemplo en el checkout de Mercado Pago) y un
**valor**, que puede ser:

- **Porcentaje** (0–100) sobre el importe de lista de la cuota.
- **Monto fijo** en pesos — se acota al importe de la cuota, nunca da un descuento negativo.

Nunca los dos a la vez: un `Descuento` es de un tipo o del otro.

## ABM del catálogo — crear, editar, borrar y listar un `Descuento`

Base `/descuento`. Permisos dedicados (`descuento.view`/`create`/`edit`/`delete`), separados de
los que usan los endpoints de asignación (`member.edit`/`activity.enroll` — ver más abajo).

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/descuento` | Alta |
| `PUT` | `/descuento/{descuentoUuid}` | Edición |
| `DELETE` | `/descuento/{descuentoUuid}` | Baja lógica — con cascada, ver abajo |
| `POST` | `/descuento/catalogo/paginate` | Listado paginado del catálogo |

**Alta y edición** usan el mismo body:

```json
{ "titulo": "Convenio Empresa X", "tipoValor": "PORCENTAJE", "porcentaje": 20, "montoFijo": null }
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `titulo` | Sí | Texto libre, sin restricción de unicidad |
| `tipoValor` | Sí | `"PORCENTAJE"` o `"MONTO_FIJO"` |
| `porcentaje` | Solo si `tipoValor = "PORCENTAJE"` | 0–100. **400** si está fuera de rango |
| `montoFijo` | Solo si `tipoValor = "MONTO_FIJO"` | Mayor a 0. **400** si no |

Al editar se puede cambiar de tipo (de `PORCENTAJE` a `MONTO_FIJO` o al revés) — el campo que ya
no corresponde queda en `null` en la respuesta.

**Respuesta** — `201` (alta) / `200` (edición), `DescuentoDTO`:

```json
{ "descuentoUuid": "…", "titulo": "Convenio Empresa X", "tipoValor": "PORCENTAJE", "porcentaje": 20, "montoFijo": null }
```

**Listado** — `POST /descuento/catalogo/paginate`, body `{ "page": 1, "size": 10, "search": "convenio" }`
(`search` sobre `titulo`), response `Page<DescuentoDTO>`. **No confundir con
`POST /descuento/paginate`** (sin body de listado propio, ver más abajo) — ese pagina *socios con
sus descuentos*, no el catálogo. Nombres parecidos, cosas distintas.

**Baja lógica — `DELETE /descuento/{descuentoUuid}`.** Sin body, respuesta `204`. Importante: al
borrar un `Descuento`, el backend **también borra físicamente** (no lógicamente) todas las
asignaciones (`MemberDescuento`/`InscripcionActividadDescuento`) que lo estuvieran usando — si
un socio tenía ese descuento asignado, esa asignación desaparece del todo, no queda ni como
"vencida". Es **irreversible**: pedile confirmación al usuario antes de llamarlo, sobre todo si
el descuento tiene socios activos usándolo (podés chequearlo antes con `POST /descuento/paginate`
o navegando el listado por socio). Las cuotas **ya generadas** con ese descuento no cambian — ya
tienen su propio snapshot copiado, sin depender del catálogo.

## Cómo se asigna en el alta

Además de la vía independiente ("Gestionar una asignación existente", más abajo), la asignación
se puede hacer **en el mismo momento del alta (o de la aprobación, para el registro público)**,
agregando un campo opcional a cuatro requests que ya existen:

| Request | Qué asigna |
|---|---|
| `POST /member/titular` | Descuento recurrente de **cuota social** al socio titular recién creado |
| `POST /member/familiar` | Descuento recurrente de **cuota social** al familiar recién creado |
| `POST /actividad/{actividadUuid}/inscripcion` | Descuento recurrente de **cuota de actividad** a esa inscripción |
| `POST /member/uuid/{memberUuid}/aprobar-registro` | Descuento recurrente de **cuota social** al socio que se activa al aprobar una solicitud de registro público |

En los cuatro, se agrega el mismo campo:

```json
{
  "...": "...campos habituales del alta/inscripción...",
  "descuentos": [
    { "descuentoUuid": "a1b2c3d4-...", "fechaDesde": "2026-06-01", "fechaHasta": null }
  ]
}
```

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `descuentoUuid` | string | Sí (dentro de cada item) | Uuid de un `Descuento` activo del club actual |
| `fechaDesde` | date (`yyyy-MM-dd`) | Sí (dentro de cada item) | Desde cuándo empieza a aplicar |
| `fechaHasta` | date (`yyyy-MM-dd`) | No | `null`/omitido = sin fecha de fin, sigue aplicando indefinidamente |

**El campo `descuentos` completo es opcional.** Si se omite o se manda `null`, el alta/inscripción/
aprobación funciona exactamente igual que hoy, sin ningún descuento — **no rompe nada de lo que
ya está integrado**. Se puede seguir mandando los cuatro requests sin tocarlos y agregar el
campo más adelante cuando haga falta.

**Caso particular — registro público:** los commands de alta pública (`POST /member/public/titular`,
`POST /member/portal/familiar`) **no** tienen `descuentos[]` — un socio no puede autoasignarse
un descuento. Pero cuando alguien del backoffice aprueba esa solicitud
(`POST /member/uuid/{memberUuid}/aprobar-registro`), ahí sí puede asignarle uno en el mismo
momento de aprobar — es la única forma de que un socio que se registró solo desde el portal
termine con un descuento.

**Se puede mandar más de uno.** Un socio o una inscripción pueden tener varios descuentos
vigentes al mismo tiempo (por ejemplo, un convenio de empresa + un descuento familiar) — cada
uno se aplica como una línea independiente y se **suman** sobre el importe de lista, no se
combinan como porcentajes compuestos. Ejemplo: cuota de $10.000 con un descuento del 20% y uno
de $1.000 fijo → final $10.000 − $2.000 − $1.000 = $7.000.

## Qué pasa después de asignarlo

- La asignación queda guardada contra el socio (o la inscripción), **no** contra la cuota.
- Cada vez que se genera una cuota nueva para ese socio/inscripción (alta, corrida diaria,
  generación manual por período) — no solo en el momento del alta — el backend revisa qué
  descuentos están **vigentes en la fecha de esa cuota** (`fechaDesde` ≤ fecha ≤ `fechaHasta`, o
  sin `fechaHasta`) y los aplica automáticamente. No hace falta volver a mandarlos cada mes.
- La cuota generada en el **mismo** request del alta/inscripción ya sale con el descuento
  aplicado, porque la asignación se guarda antes de generar esa primera cuota.
- Si más adelante se edita el `Descuento` en el catálogo (cambia el título, el porcentaje, etc.),
  las cuotas **ya generadas** no se alteran retroactivamente — cada cuota guarda una copia
  (`titulo` + `valor` ya calculado) tomada en el momento de generarse. Solo van a reflejar el
  cambio las cuotas que se generen **después** de la edición.

## Cómo se ve en la cuota generada (estado actual — limitado)

- **`montoFinal`** en `CuotaResumenDTO`/`CuotaDetalleDTO`/`CuotaPaginateRowDTO` **ya viene
  descontado correctamente** — es el mismo cálculo que usa el cobro real, así que la UI puede
  confiar en ese número tal cual para mostrar "lo que hay que pagar".
- **No hay, todavía, un desglose de qué descuentos se aplicaron a una cuota puntual.** El campo
  `descuento` (Integer) que existía en esos DTOs quedó como resto del modelo anterior — sigue
  presente en el JSON por compatibilidad, pero **siempre viene `null`/sin usar**, no lo
  interpretes. No hay un `descuentos[]` (con `titulo`/`valor` por línea) expuesto en estos DTOs
  todavía — si la UI necesita mostrar "por qué esta cuota tiene descuento" o el título del
  convenio aplicado, hoy no hay de dónde sacarlo vía API (es un pendiente aparte, no depende del
  ABM del catálogo).

## Ver los descuentos asignados — `POST /descuento/paginate`

Nuevo endpoint de solo lectura para armar una pantalla de "gestión de descuentos": lista todos
los socios del club, paginado, con los descuentos que tiene cada uno. Permiso `member.view`
(mismo que `/member/paginate`).

**Request:**

```json
{ "page": 1, "size": 10, "search": "gómez" }
```

`search` busca por nombre, apellido, DNI **o número de socio** — más amplio que la búsqueda de
`/member/paginate`, que no incluye número de socio.

**Response** — `Page` de filas con este shape:

```json
{
  "memberUuid": "…",
  "nombre": "Carlos Gómez",
  "fotoUrl": null,
  "cuotaSocial": [
    {
      "asignacionUuid": "…",
      "descuentoUuid": "…",
      "titulo": "Convenio Empresa X",
      "tipoValor": "PORCENTAJE",
      "porcentaje": 20,
      "montoFijo": null,
      "fechaDesde": "2026-06-01",
      "fechaHasta": null,
      "vigente": true
    }
  ],
  "actividades": [
    {
      "actividadUuid": "…",
      "actividadNombre": "Fútbol Sub-15",
      "descuentos": [ { "...": "mismo shape que en cuotaSocial" } ]
    }
  ]
}
```

Puntos a tener en cuenta al integrarlo:

- **Trae todos los socios del club**, no solo los que tienen descuento — para la mayoría,
  `cuotaSocial` y `actividades` van a venir como `[]`. Si la UI quiere mostrar "solo socios con
  descuento", el filtro hay que hacerlo del lado del cliente por ahora (no hay un parámetro para
  eso en el request).
- `cuotaSocial` y cada `descuentos` dentro de `actividades[]` son **arrays**, no un objeto único
  — un socio puede tener más de un descuento social o de una misma actividad al mismo tiempo (no
  hay exclusividad, es una decisión de diseño ya tomada). No asumas que siempre hay como máximo
  un elemento.
- `actividades[]` solo incluye actividades donde el socio **tiene** al menos un descuento
  asignado — no es la lista de todas sus inscripciones. Un socio inscripto en varias actividades
  pero con descuento en una sola va a mostrar solo esa.
- `vigente` es un booleano calculado **en el momento de la consulta**, contra la fecha de hoy —
  no es algo que se guarde. Una fila puede pasar de `vigente: true` a `false` de un día para el
  otro sin que nadie haya tocado nada (por ejemplo, al llegar su `fechaHasta`). Útil para pintar
  un badge tipo "vigente" / "vencido" sin tener que calcular fechas en el front.
- `asignacionUuid` (distinto de `descuentoUuid`) identifica la asignación puntual — es el dato
  que un futuro endpoint de "editar/quitar descuento" va a pedir. Guardalo si vas a necesitar
  referenciar esa fila más adelante, aunque hoy no haya nada que hacer con él todavía.
- `nombre` viene combinado (`"Nombre Apellido"`), no como `nombre`/`apellido` separados como en
  `MemberDTO` — si la UI necesita separarlos, hay que resolverlo del lado del cliente o pedir el
  cambio.

## Gestionar una asignación existente — asignar / baja física / finalizar

Antes, la única forma de crear una asignación era en el mismo alta del socio o la inscripción.
Ahora hay 6 endpoints nuevos para manejar el ciclo de vida de una asignación en cualquier
momento, sobre un socio o una inscripción que **ya existen**:

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/descuento/member/{memberUuid}` | Asigna un descuento de cuota social a un socio |
| `DELETE` | `/descuento/member-descuento/{uuid}` | Borra la asignación (**física**, no lógica) |
| `PUT` | `/descuento/member-descuento/{uuid}/finalizar` | Cierra la vigencia sin borrarla |
| `POST` | `/descuento/inscripcion-actividad/{inscripcionActividadUuid}` | Asigna un descuento de cuota de actividad a una inscripción |
| `DELETE` | `/descuento/inscripcion-actividad-descuento/{uuid}` | Borra la asignación (física) |
| `PUT` | `/descuento/inscripcion-actividad-descuento/{uuid}/finalizar` | Cierra la vigencia sin borrarla |

**Ojo con qué `{uuid}` va en cada uno** — no es el mismo tipo de uuid en todos:
- En los dos `POST`, el uuid del path es el del **dueño**: el `memberUuid` del socio, o el
  `inscripcionActividadUuid` de la inscripción (el mismo que ya usás en otros endpoints de
  socios/actividades).
- En `DELETE` y en `finalizar`, el uuid del path es el `asignacionUuid` — el mismo campo que
  devuelve `POST /descuento/paginate` en cada item de `cuotaSocial[]`/`descuentos[]`. **No** es
  el `descuentoUuid` del catálogo ni el uuid del socio/inscripción.

### Asignar — `POST /descuento/member/{memberUuid}` y `POST /descuento/inscripcion-actividad/{inscripcionActividadUuid}`

Mismo body en los dos, mismo shape que `descuentos[]` en el alta:

```json
{ "descuentoUuid": "a1b2c3d4-...", "fechaDesde": "2026-06-01", "fechaHasta": null }
```

Permiso: `member.edit` para el de socio, `activity.enroll` para el de inscripción (el mismo que
ya gatea inscribir a alguien en una actividad).

**Respuesta 201** — el `DescuentoAsignadoDTO` recién creado (mismo shape que ya conocés de
`/descuento/paginate`):

```json
{
  "asignacionUuid": "…",
  "descuentoUuid": "…",
  "titulo": "Convenio Empresa X",
  "tipoValor": "PORCENTAJE",
  "porcentaje": 20,
  "montoFijo": null,
  "fechaDesde": "2026-06-01",
  "fechaHasta": null,
  "vigente": true
}
```

Guardá el `asignacionUuid` de la respuesta si la UI va a permitir dar de baja o finalizar esa
misma asignación después de crearla, sin recargar el listado.

Igual que en el alta: si el socio/inscripción ya tenía otra asignación vigente, esta se **suma**,
no la reemplaza — no hay exclusividad.

### Baja física — `DELETE /descuento/member-descuento/{uuid}` y `DELETE /descuento/inscripcion-actividad-descuento/{uuid}`

Sin body. **Borra la fila de la base**, no la marca como inactiva — a diferencia de casi todo lo
demás en este sistema (que usa baja lógica), acá es intencional: nada más referencia esa fila
(las cuotas ya generadas guardan su propio snapshot del título/valor, no un puntero a la
asignación), así que no queda nada huérfano. **Es irreversible** — no hay forma de deshacer un
`DELETE` una vez confirmado, así que si la UI ofrece esta acción conviene pedir confirmación
antes de dispararla (piensen en un modal de "¿estás seguro?", no un botón directo).

**Respuesta 204** sin contenido. Si se llama dos veces sobre el mismo `asignacionUuid`, la
segunda da **404** (ya no existe).

### Finalizar — `PUT /descuento/member-descuento/{uuid}/finalizar` y `PUT /descuento/inscripcion-actividad-descuento/{uuid}/finalizar`

Body:

```json
{ "fecha": "2026-12-31" }
```

A diferencia de la baja física, esto **no borra nada** — le pone una fecha de fin a la
asignación (`fechaHasta = fecha`) y la deja en la base como historial de que existió y estuvo
vigente hasta esa fecha. Es la opción a preferir cuando la intención es "este descuento deja de
aplicar a partir de tal fecha", en vez de "esto nunca debió haber existido".

**Respuesta 200** con el `DescuentoAsignadoDTO` actualizado — mirá el campo `vigente` en la
respuesta para saber si el cambio ya tiene efecto inmediato (`fecha` de hoy o pasada) o recién
va a dejar de aplicar más adelante (`fecha` futura).

**Cuándo usar cada uno:**

| Situación | Usar |
|---|---|
| Se cargó un descuento por error, nunca debió existir | Baja física (`DELETE`) |
| El convenio/descuento termina en una fecha conocida (hoy o a futuro) | Finalizar (`PUT .../finalizar`) |
| Se quiere conservar el historial de qué tuvo cada socio | Finalizar, nunca `DELETE` |

## Errores a manejar

| Situación | Respuesta |
|---|---|
| `descuentoUuid` no existe, está dado de baja, o pertenece a otro club | **404** "Descuento no encontrado" |
| `memberUuid` / `inscripcionActividadUuid` no existe o es de otro club | **404** |
| `asignacionUuid` (baja o finalizar) no existe, es de otro club, o ya fue borrado | **404** "Asignación no encontrada" |
| `finalizar` con `fecha` anterior a la `fechaDesde` de la asignación | **400** |
| `finalizar` sin `fecha` en el body | **400** por validación del DTO |
| Item de `descuentos` sin `fechaDesde` | **400** por validación del DTO |
| Item de `descuentos` sin `descuentoUuid` (vacío/blank) | **400** por validación del DTO |

No hay una validación de "descuento duplicado" — se puede asignar el mismo `Descuento` dos
veces a la misma persona/inscripción (con vigencias distintas o superpuestas) sin que el
backend lo rechace; ambas asignaciones se sumarían igual en la cuota. Si la UI quiere evitar
esto, tiene que controlarlo del lado del cliente por ahora.

## Dónde **no** está disponible

- Portal público: `POST /member/public/titular`, `POST /member/portal/familiar`,
  `POST /portal/member/actividad/inscripcion` — sus commands no tienen el campo `descuentos`, a
  propósito. Un socio no puede autoasignarse un descuento.
- Importación masiva de socios (`POST /member/import`).
- **Editar** una asignación existente (cambiarle el `descuentoUuid` o la `fechaDesde`) — no hay
  endpoint para eso. Sí se puede finalizarla (cerrar su vigencia) o borrarla y crear una nueva —
  ver "Gestionar una asignación existente" más arriba.
- Un "descuento único" para una sola cuota puntual (sin quedar asignado de forma recurrente) —
  el modelo lo contempla a nivel de diseño, pero no hay ningún endpoint que lo dispare todavía.

## Postman

Todo vive en la carpeta `Descuentos` (dentro de `01 · Backoffice · Socios`), en la colección
`postman/bsports-club.postman_collection.json`:

- **ABM del catálogo** (`POST`/`PUT`/`DELETE`/paginado de `Descuento`) — corré primero el alta:
  guarda automáticamente el `descuentoUuid` creado en la variable de colección, así las requests
  de asignación de más abajo ya lo tienen precargado y son ejecutables sin tocar nada a mano.
- **Consulta por socio** (`POST /descuento/paginate`) — ejecutable siempre, sin depender de nada.
- **Gestión de asignaciones** (asignar/baja/finalizar, para socio y para inscripción) — los de
  asignar (`POST`) usan la variable `descuentoUuid` que dejó el alta del catálogo; ya no hace
  falta pegarlo a mano. Los de baja y finalizar usan `memberDescuentoUuid`/
  `inscripcionActividadDescuentoUuid`, que a su vez quedan seteados al correr los de asignar.

Orden sugerido para probar el flujo completo desde cero: alta de `Descuento` → asignar a un
socio o inscripción → ver que aparece en la consulta por socio → finalizar o dar de baja.

Los 4 requests que asignan descuentos **en el alta** (`POST /member/titular`,
`POST /member/familiar`, `POST /actividad/{actividadUuid}/inscripcion` y
`POST /member/uuid/{memberUuid}/aprobar-registro`) siguen con su nota en la descripción
explicando el campo `descuentos[]` — sus bodies de ejemplo no lo incluyen, para no depender de
que el `Descuento` del catálogo ya exista al correrlas; agregalo a mano usando la variable
`descuentoUuid` una vez que hayas dado de alta uno.
