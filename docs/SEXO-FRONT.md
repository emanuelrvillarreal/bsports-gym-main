# Guía frontend — Campo `sexo`

Nuevo dato personal `sexo` del usuario. Se carga en el alta, se muestra en los listados/detalles y se
puede editar desde el socio (portal) y desde el administrador (backoffice).
Ver también [`docs/API.md`](API.md) para la referencia general de endpoints.

## Valores permitidos

Enum de texto, en **mayúsculas**, igual en request y response:

| Valor | Sugerencia de etiqueta |
|---|---|
| `MASCULINO` | Masculino |
| `FEMENINO` | Femenino |
| `OTRO` | Otro |

Cualquier otro valor devuelve `400` (error de deserialización del enum).

## Obligatoriedad

| Contexto | ¿Obligatorio? |
|---|---|
| Registro público de socio (`POST /member/public/titular`) | **No** (opcional) |
| Alta de socio titular / familiar (backoffice y portal) | **No** (opcional) |
| Alta de **profesor**, **recepcionista**, **administrador** | **No** (opcional) |
| Ediciones (todas) | No: si no se envía o va `null`, **no se modifica** |

> Los usuarios existentes tienen `sexo = null`. El front debe tolerar `null` en las respuestas y
> mostrar el select vacío ("Seleccionar").

## Endpoints afectados

Todos con `Authorization: Bearer <jwt>` y `X-Company-Uuid` (el registro público solo requiere
`X-Company-Uuid`). En los requests el campo va como `"sexo": "MASCULINO"`; en las respuestas
viene como string o `null`.

Leyenda: **Req** = acepta `sexo` en el body · **Res** = devuelve `sexo` en la respuesta.

### Socios

| Método | Ruta | Body / DTO | Req | Res | Obligatorio |
|---|---|---|:-:|:-:|:-:|
| `POST` | `/member/public/titular` | `MemberPublicRegistroCommand` → `MemberDTO` (inscripción pública) | ✅ | ✅ | No |
| `POST` | `/member/titular` | `MemberTitularAltaCommand` → `MemberDTO` | ✅ | ✅ | No |
| `POST` | `/member/familiar` | `MemberFamiliarAltaCommand` → `MemberDTO` | ✅ | ✅ | No |
| `POST` | `/member/portal/familiar` | `MemberPortalFamiliarAltaCommand` → `MemberDTO` | ✅ | ✅ | No |
| `PUT` | `/member/uuid/{memberUuid}` | `MemberBackofficeUpdateCommand` → `MemberDTO` (admin edita) | ✅ | ✅ | No |
| `PUT` | `/portal/member/perfil` | `MemberPortalProfileCommand` → `MemberDTO` (socio edita) | ✅ | ✅ | No |
| `GET` | `/portal/member/perfil` | → `MemberDTO` | — | ✅ | — |
| `POST` | `/member/paginate` | → `Page<MemberDTO>` | — | ✅ | — |
| `GET` | `/member/uuid/{memberUuid}/familiares` | → `List<MemberDTO>` | — | ✅ | — |
| `POST` | `/member/solicitudes/paginate` | → `Page<MemberSolicitudRegistroDTO>` | — | ✅ | — |
| `POST` | `/member/uuid/{memberUuid}/aprobar-registro` | → `MemberDTO` | — | ✅ | — |
| `POST` | `/member/uuid/{memberUuid}/rechazar-registro` | → `MemberDTO` | — | ✅ | — |
| `POST` | `/member/uuid/{memberUuid}/baja` | → `MemberDTO` | — | ✅ | — |
| `PUT` | `/member/uuid/{memberUuid}/estado` | → `MemberDTO` | — | ✅ | — |

### Profesores

| Método | Ruta | Body / DTO | Req | Res | Obligatorio |
|---|---|---|:-:|:-:|:-:|
| `POST` | `/profesor` | `ProfesorAltaCommand` → `ProfesorDTO` | ✅ | ✅ | No |
| `PUT` | `/profesor/uuid/{profesorUuid}` | `StaffProfileUpdateCommand` → `ProfesorDTO` | ✅ | ✅ | No |
| `POST` | `/profesor/paginate` | → `Page<ProfesorDTO>` | — | ✅ | — |
| `GET` | `/portal/profesor/perfil` | → `ProfesorDTO` | — | ✅ | — |
| `PUT` | `/portal/profesor/perfil` | `StaffProfileUpdateCommand` → `ProfesorDTO` (el propio profesor) | ✅ | ✅ | No |

### Recepcionistas

| Método | Ruta | Body / DTO | Req | Res | Obligatorio |
|---|---|---|:-:|:-:|:-:|
| `POST` | `/recepcionista` | `RecepcionistaAltaCommand` → `RecepcionistaDTO` | ✅ | ✅ | No |
| `PUT` | `/recepcionista/uuid/{recepcionistaUuid}` | `StaffProfileUpdateCommand` → `RecepcionistaDTO` | ✅ | ✅ | No |
| `POST` | `/recepcionista/paginate` | → `Page<RecepcionistaDTO>` | — | ✅ | — |
| `GET` | `/recepcionista/perfil` | → `RecepcionistaDTO` | — | ✅ | — |
| `PUT` | `/recepcionista/perfil` | `StaffProfileUpdateCommand` → `RecepcionistaDTO` (el propio recepcionista) | ✅ | ✅ | No |

### Administradores

| Método | Ruta | Body / DTO | Req | Res | Obligatorio |
|---|---|---|:-:|:-:|:-:|
| `POST` | `/admin` | `AdminAltaCommand` → `AdminDTO` | ✅ | ✅ | No |
| `PUT` | `/admin/uuid/{userUuid}` | `StaffProfileUpdateCommand` → `AdminDTO` | ✅ | ✅ | No |
| `POST` | `/admin/paginate` | → `Page<AdminDTO>` | — | ✅ | — |

Ejemplos:

```json
// Alta / edición
{ "nombre": "Juan", "apellido": "Pérez", "email": "juan@mail.com", "sexo": "MASCULINO" }

// Respuesta
{ "memberUuid": "…", "nombre": "Juan", "fechaNacimiento": "15/03/1990", "sexo": "MASCULINO" }
```

## DTOs de respuesta con `sexo`

`MemberDTO`, `MemberSolicitudRegistroDTO`, `ProfesorDTO`, `RecepcionistaDTO`, `AdminDTO`.

## Errores

| Código | Cuándo |
|---|---|
| `400` | `sexo` con un valor fuera del enum |

## Cambios a hacer en el front

1. **Formulario de inscripción (registro público):** agregar un select "Sexo" con los 3 valores.
2. **Alta de socio / familiar (backoffice y portal):** agregar el select.
3. **Alta de profesor, recepcionista y admin:** agregar el select (**opcional**, para ser retrocompatible con versiones anteriores del front que no lo envían).
4. **Edición de socio (portal) y de socio / personal (admin):** agregar el select precargado con el
   valor actual (`null` → sin selección). Enviarlo solo si el usuario lo cambió.
5. **Listados y detalles:** mostrar `sexo` donde corresponda, tolerando `null`.
6. **Revisión de solicitudes de registro:** mostrar el `sexo` en el detalle de la solicitud.

## Notas

- No hay filtro por `sexo` en los paginados.
- No se agregó `sexo` a la importación masiva de socios (Excel/CSV).
