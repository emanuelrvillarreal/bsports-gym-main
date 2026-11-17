# Guía frontend — Editar la fecha de expiración de un certificado médico

Esta guía cubre el endpoint de backoffice que permite modificar la **fecha de expiración** de un
certificado médico ya cargado. Ver también [§7 de `docs/API.md`](API.md#7-certificados-médicos)
para la referencia formal de certificados.

## Contexto

Cuando se sube un certificado (desde backoffice o portal), el backend calcula la
`fechaExpiracion` automáticamente: **31/12 del año de carga**. Por ejemplo, un certificado cargado
el 27/10/2026 vence el 31/12/2026.

Este endpoint permite que el personal del club corrija esa fecha (por ejemplo, si el apto médico
real tiene otra vigencia). **Solo se puede cambiar la fecha de expiración**: archivo, estado y
fecha de carga no se tocan.

## Endpoint

| Método | Ruta | Quién puede |
|---|---|---|
| `PUT` | `/member/certificado/uuid/{certificadoUuid}` | Roles `ADMIN` y `ADM_RECEPCION` |

> La autorización es **por rol**, no por permiso: no hay un permiso nuevo que asignar. `SYSADMIN`
> **no** está habilitado.

**Headers:** `Authorization: Bearer <jwt>`, `X-Company-Uuid: <companyUuid>`, `Content-Type: application/json`.

**Path param:** `certificadoUuid` — el `certificadoUuid` que viene en `CertificadoMedicoDTO`
(por ejemplo, del listado `GET /member/uuid/{memberUuid}/certificado`).

### Request

```json
{ "fechaExpiracion": "31/03/2027" }
```

| Campo | Obligatorio | Formato | Notas |
|---|---|---|---|
| `fechaExpiracion` | Sí | `dd/MM/yyyy` | Mismo formato con el que viene en la respuesta |

Cualquier otro campo del body se ignora.

### Response — `200`, `CertificadoMedicoDTO`

```json
{
  "certificadoUuid": "0b7c…",
  "memberUuid": "a91e…",
  "fechaCarga": "27/10/2026",
  "fechaExpiracion": "31/03/2027",
  "nombreArchivo": "apto-medico.pdf",
  "urlArchivo": "https://…",
  "estado": "CONFIRMADO"
}
```

### Errores

| Código | Cuándo |
|---|---|
| `400` | Falta `fechaExpiracion` o no respeta `dd/MM/yyyy` |
| `403` | El usuario no tiene rol `ADMIN` ni `ADM_RECEPCION` en el club del header |
| `404` | El certificado no existe, o no pertenece a un socio del club del header `X-Company-Uuid` |

## Comportamiento a tener en cuenta

- **No hay validación de rango.** El backend acepta cualquier fecha, incluso una anterior a
  `fechaCarga` o ya pasada. Si la UI quiere evitarlo, validarlo del lado del front.
- **Fecha pasada en un certificado `CONFIRMADO`:** el job diario de vencimiento (02:00) lo va a
  pasar a `VENCIDO` en la próxima corrida y **borra el archivo del storage**. Conviene pedir
  confirmación al usuario si elige una fecha anterior a hoy.
- **El estado no cambia.** Extender la fecha de un certificado ya `VENCIDO` **no** lo reactiva
  (y su archivo ya fue borrado). Para eso hay que subir un certificado nuevo.
- Se puede editar un certificado en cualquier estado (`PENDIENTE_VALIDACION`, `CONFIRMADO`,
  `VENCIDO`).

## Sugerencia de UI

En el listado de certificados del socio (backoffice), agregar una acción "Editar vencimiento"
visible solo para `ADMIN` / `ADM_RECEPCION`, con un date picker precargado con la
`fechaExpiracion` actual. Al guardar, reemplazar el ítem del listado con el
`CertificadoMedicoDTO` que devuelve el endpoint.
