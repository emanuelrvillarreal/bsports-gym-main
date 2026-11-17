# Guía frontend — Rangos de cuota social (alta / edición / baja)

Esta guía cubre el ABM de `CuotaSocialRango` (alta, edición, baja lógica, listado paginado).
No incluye la asignación del rango al socio, que hoy es automática (ver [§10.1 de
`docs/API.md`](API.md#101-rangos-de-cuota-social) para el detalle completo del feature).

## Qué es un "rango de cuota social"

Reemplaza el importe único de cuota social que antes vivía en la ficha del club
(`Company.importeCuotaSocial`, ya eliminado). Ahora el club define **tramos por edad**, cada
uno con su propio importe — típicamente algo como:

| Nombre | edadHasta | importe |
|---|---|---|
| Menores | 17 | 5000 |
| Adultos | 64 | 15000 |
| Jubilados | 100 | 0 |

**Importante — el modelo es de "tramo" (bracket), no de rango con piso y techo.** Cada rango
solo define un límite superior, `edadHasta`. El rango que le corresponde a un socio es el que
tiene el `edadHasta` **más chico que sea `>= su edad`**. Con la tabla de ejemplo:

- Socio de 10 años → "Menores" (17 es el `edadHasta` más chico que cubre 10).
- Socio de 40 años → "Adultos" (64 es el más chico que cubre 40; 17 no alcanza).
- Socio de 70 años → "Jubilados".

No hay que enviar ni pedir un `edadMinima` — no existe. `edadHasta` es **único por club**: no
se puede crear/editar un rango a un `edadHasta` que ya use otro rango activo del mismo club.

Todo club sin ningún rango configurado ya tiene, por una migración, un rango catch-all
**"General"** con `edadHasta = 100` e `importe = 0` — así que el listado nunca debería venir
vacío una vez desplegado esto. Ese rango en particular viene marcado `esDefault: true` en el
`CuotaSocialRangoDTO` y **no se puede eliminar** — la UI debería deshabilitar/ocultar el botón
de borrar cuando `esDefault === true`, porque el backend lo va a rechazar con 400 igual.

## Endpoints

Base: `{{baseUrl}}/cuota-social-rango`. Todos requieren JWT + header `X-Company-Uuid`, igual
que el resto del backoffice.

### `POST /cuota-social-rango` — Alta

**Permiso:** `cuota_social_rango.create` (hoy solo lo tiene el rol `ADM_RECEPCION`; a
diferencia de `Categoria`, `PROFESOR` no lo tiene).

**Body — `CuotaSocialRangoAltaCommand`:**

```json
{
  "nombre": "Adultos",
  "descripcion": "Socios hasta 64 años",
  "edadHasta": 64,
  "importe": 15000.00
}
```

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `nombre` | string | Sí | ≤255 caracteres, único por club |
| `descripcion` | string | No | ≤512 caracteres |
| `edadHasta` | integer | Sí | 0–120, único por club |
| `importe` | decimal | Sí | `>= 0` (puede ser **0** — rango exento, ver más abajo) |

**Respuesta 201 — `CuotaSocialRangoDTO`:**

```json
{
  "cuotaSocialRangoUuid": "a1b2c3d4-...",
  "nombre": "Adultos",
  "descripcion": "Socios hasta 64 años",
  "edadHasta": 64,
  "importe": 15000.00,
  "esDefault": false
}
```

`esDefault` es de **solo lectura** — el backend lo asigna, no se manda en el request. En un
alta siempre viene `false` (solo el rango sembrado por migración tiene `true`).

**Errores:**
- **400** — algún campo inválido (mensajes de validación estándar del backend).
- **409** — ya existe otro rango activo del club con el mismo `nombre`, **o** con el mismo
  `edadHasta`. El body de error indica cuál de los dos chocó (mirar el mensaje).

### `PUT /cuota-social-rango/{cuotaSocialRangoUuid}` — Edición

**Permiso:** `cuota_social_rango.edit`.

Mismo body que el alta (`CuotaSocialRangoEdicionCommand`, mismos campos/validaciones). Es
edición completa, no parcial: hay que mandar los 4 campos siempre, aunque no cambien.

**Respuesta 200** — mismo shape que el alta.

**Errores:**
- **404** — el uuid no existe (o pertenece a otro club, o está dado de baja).
- **409** — igual que en el alta (nombre o `edadHasta` duplicado, excluyendo el propio rango
  que se está editando).

### `DELETE /cuota-social-rango/{cuotaSocialRangoUuid}` — Baja lógica

**Permiso:** `cuota_social_rango.delete`.

Sin body.

**Respuesta 204** — sin contenido.

**Errores:**
- **404** — el uuid no existe (o pertenece a otro club, o ya está dado de baja).
- **400** — el rango tiene `esDefault: true` (el catch-all "General" sembrado por migración).
  Mensaje: *"No se puede eliminar el rango por defecto del club"*. La UI debería evitar mostrar
  el botón de borrar en ese caso en vez de dejar que el usuario choque con el 400 (ver nota de
  `esDefault` más arriba).

Efecto secundario (async, ver más abajo): los socios que tenían asignado **justo ese rango**
se recalculan — cada uno pasa al tramo que le corresponda por su edad actual, o queda sin
rango si ninguno lo cubre. **No** afecta a socios de otros rangos.

### `POST /cuota-social-rango/paginate` — Listado paginado

**Permiso:** `cuota_social_rango.view`.

**Body — `CuotaSocialRangoPaginateRequestDTO`** (todo opcional, `body: null`/`{}` también
sirve):

```json
{ "page": 1, "size": 10, "search": "" }
```

- `search`: `LIKE` sobre `nombre` y `descripcion`.
- Ordenado por `edadHasta` ascendente — el listado ya viene en el orden natural de los tramos
  (de menor a mayor edad), útil para pintarlo directo en una tabla o timeline.

**Respuesta 200** — `Page<CuotaSocialRangoDTO>` (shape estándar de paginado del backend: `content`,
`totalElements`, `totalPages`, `number`, `size`, etc.).

### Lo que **no** existe todavía

- **Listado simple sin paginar** (el que `Categoria` sí tiene en `GET /categoria` para llenar
  combos). Para un combo/select, usar `paginate` con `size` grande (ej. 100) — en la práctica
  un club va a tener pocos tramos.
- **Asignación manual del rango a un socio.** No hay un endpoint tipo "asignale este rango a
  este socio". La asignación es 100% automática (ver abajo).

## Cómo se entera el usuario del rango de un socio

`GET`/listados de `Member` devuelven, dentro de `MemberDTO`, el campo:

```json
"cuotaSocialRango": { "uuid": "...", "nombre": "Adultos" }
```

Puede venir **`null`** si el club no tiene la cuota social habilitada, o si el socio no tiene
`fechaNacimiento` cargada. No viaja `edadHasta` ni `importe` en ese contexto — si la UI
necesita esos datos, hay que resolverlos contra la lista de rangos del club (`paginate`) usando
el `uuid`.

## Efectos secundarios a tener en cuenta en la UI

Los 3 endpoints que modifican un rango (`POST`, `PUT`, `DELETE`) disparan en el backend, **de
forma asíncrona**, un recálculo del rango de socios afectados:

| Acción | Alcance del recálculo |
|---|---|
| Alta o edición de un rango | **Todos** los socios activos del club (insertar/mover un tramo puede cambiar cuál es el más ajustado para cualquiera) |
| Baja de un rango | Solo los socios que tenían asignado **ese** rango puntual |

Esto significa:

- La respuesta del `POST`/`PUT`/`DELETE` es inmediata — no hay que esperar nada extra, el
  recálculo corre después y en background.
- Si la UI muestra en algún lado "qué rango tiene cada socio" justo después de crear, editar o
  borrar un rango, ese dato puede tardar unos segundos en reflejar el cambio (es asíncrono, no
  hay un tiempo garantizado). Si hace falta certeza inmediata, conviene refrescar esa vista con
  un pequeño delay o un botón de "actualizar", no asumir que el cambio ya se propagó al
  instante.
- No hay hoy un evento/websocket que avise al front cuando terminó el recálculo — es fire and
  forget del lado del backend.
- Tras borrar un rango, algunos socios pueden quedar **sin ningún rango asignado** (si ninguno
  de los que quedan cubre su edad) — la UI debería contemplar ese estado (`cuotaSocialRango:
  null` en `MemberDTO`) como algo esperable, no un error.

## Importe en 0 — rango exento

Si un rango tiene `importe = 0`, cualquier cuota social que se genere para un socio en ese
rango se crea directamente en estado `PAGADA` (no `PENDIENTE`). Útil para modelar, por ejemplo,
un rango de "Jubilados" exento de cuota social. No requiere nada especial del lado del front
más allá de permitir cargar `0` como importe válido (el campo lo acepta).

## Postman

La colección (`postman/bsports-club.postman_collection.json`) tiene los 4 requests bajo
`04 · Backoffice · Tesorería → Rangos de cuota social` (alta, edición, baja, paginado), con los
bodies ya actualizados al modelo de `edadHasta`. El alta guarda automáticamente
`cuotaSocialRangoUuid` en las variables de colección al correrla, para poder encadenar la
edición o la baja sin copiar el uuid a mano.
