# Guía frontend — Editar el número de socio

Esta guía cubre el nuevo campo `numeroSocio` en la edición de socio/familiar desde backoffice.
Ver también [§6 de `docs/API.md`](API.md#6-socios-members) (`PUT /member/uuid/{memberUuid}`) para la referencia formal.

## Contexto

El `numeroSocio` lo sigue generando el backend en las altas (`MAX + 1` por club; en el registro
público, al aprobar la solicitud). Lo nuevo es que el personal del club ahora **puede
modificarlo** después, usando el endpoint de edición que ya existe.

No hay endpoint nuevo: se agrega un campo opcional al body de la edición.

## Endpoint

| Método | Ruta | Permiso |
|---|---|---|
| `PUT` | `/member/uuid/{memberUuid}` | `member.edit` (titular) / `family.edit` (familiar) |

**Headers:** `Authorization: Bearer <jwt>`, `X-Company-Uuid: <companyUuid>`, `Content-Type: application/json`.

### Request

Todos los campos siguen siendo opcionales: lo que no se envía (o va en `null`) **no se modifica**.

```json
{ "numeroSocio": 1250 }
```

Se puede combinar con el resto de los campos de la edición:

```json
{ "nombre": "Juan", "telefono": "+5491112345678", "numeroSocio": 1250 }
```

| Campo | Tipo | Notas |
|---|---|---|
| `numeroSocio` | **entero** | Mayor a 0. Opcional |

> ⚠️ **Tipos distintos en request y response.** En el request va como **número** (`1250`); en
> `MemberDTO` sigue viniendo como **string** (`"1250"`). Convertir al precargar el formulario y
> al enviarlo.

### Response — `200`, `MemberDTO`

Igual que hoy, con el `numeroSocio` actualizado:

```json
{ "memberUuid": "…", "numeroSocio": "1250", "nombre": "Juan", "…": "…" }
```

### Errores

| Código | Cuándo |
|---|---|
| `400` | `numeroSocio` es 0 o negativo, o no es un entero |
| `403` | Sin permiso `member.edit` (titular) / `family.edit` (familiar) |
| `409` | Otro socio **activo** del mismo club ya tiene ese número — mensaje `"El número de socio ya está en uso"` |

## Comportamiento a tener en cuenta

- **Reenviar el número actual no da error.** Si el formulario manda siempre el `numeroSocio` que
  ya tenía el socio, el backend lo acepta sin conflicto. Igual, conviene enviarlo solo si el
  usuario lo cambió.
- **Socios dados de baja o pendientes.** La validación del 409 solo mira socios activos. Si el
  número pertenece a un socio dado de baja o a una solicitud pendiente, el backend no devuelve 409
  pero el guardado falla con un error genérico (la base no permite números repetidos en el club).
  Mostrar el mensaje de error que venga en la respuesta.
- **Afecta la numeración automática.** Las altas siguientes toman `número más alto + 1`. Si se
  asigna a mano un número alto (ej. `5000`), los próximos socios van a seguir desde `5001`.
  Puede valer la pena advertirlo en la UI.
- **Titulares y familiares** comparten la misma numeración dentro del club.

## Sugerencia de UI

En el formulario de edición del socio (backoffice), habilitar el campo "N° de socio" como input
numérico (enteros positivos), precargado con `Number(member.numeroSocio)`. Ante un `409`, mostrar
el error junto al campo para que el usuario elija otro número.
