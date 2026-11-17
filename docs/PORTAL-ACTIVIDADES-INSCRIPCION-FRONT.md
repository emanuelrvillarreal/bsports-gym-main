# Guía de integración front — Inscripción self-service a actividades (portal del socio)

Guía para integrar `POST /portal/member/actividad/inscripcion`, el endpoint nuevo que permite a un socio titular inscribirse **a sí mismo o a un familiar de su grupo** en una actividad, **sin intervención de staff**. Para el detalle completo del endpoint ver [`docs/API.md` §21](API.md#21-portal-del-socio). Payloads de ejemplo listos para probar: colección `postman/bsports-club.postman_collection.json` → carpeta **"06 · Portal · Socio" → "Actividades"**.

## Qué resuelve

Hasta ahora, desde el portal el socio solo podía **ver** actividades a través del calendario (`POST /portal/calendario`, que muestra solo las actividades con inscripción activa). Para inscribirse en una nueva actividad, dependía de que staff lo hiciera desde backoffice (`POST /actividad/{actividadUuid}/inscripcion`). Este endpoint nuevo habilita esa alta directamente desde el portal, para el propio titular o para cualquier familiar de su grupo.

## 1. Pre-requisitos antes de mostrar el flujo

1. El usuario logueado debe ser **socio titular** (no familiar). El permiso `portal.activities.enroll` solo lo tiene el rol `SOCIO_TITULAR` — un familiar logueado directamente recibe **403** al intentar usar este endpoint. No mostrar la opción de inscribirse (ni a sí mismo ni a otro) si el logueado es familiar.
2. Traer el listado de actividades disponibles (mismo listado que usa backoffice, `POST /actividad/paginate`, o el que ya use el front para mostrar actividades del club) para que el socio elija cuál — este endpoint no lista actividades, solo inscribe.
3. Si el flujo permite elegir **a quién** inscribir, traer el grupo familiar con `GET /portal/member/grupo` (`portal.family.view`, solo titular) para armar el selector de "inscribir a…" con los integrantes del grupo.

## 2. Elegir a quién inscribir

El campo `memberUuid` del body decide el destino:

| Caso | Valor de `memberUuid` |
|---|---|
| Inscribir al titular logueado (uno mismo) | `null` u **omitido** |
| Inscribir a un familiar del grupo | UUID de ese familiar (`memberUuid` de `GET /portal/member/grupo`) |

No hay forma de inscribir a un socio que **no** pertenezca al mismo grupo familiar que el titular logueado — el backend lo valida y devuelve **403** si se intenta.

## 3. Llamar al endpoint

```
POST /portal/member/actividad/inscripcion
Authorization: Bearer <jwt del socio titular>
X-Company-Uuid: <uuid>
Content-Type: application/json
```

**Inscribirme:**

```json
{ "actividadUuid": "8e2f5c3a-…", "memberUuid": null }
```

**Inscribir a un familiar:**

```json
{ "actividadUuid": "8e2f5c3a-…", "memberUuid": "3b7a1d90-…" }
```

| Campo | Tipo | Notas |
|---|---|---|
| `actividadUuid` | string (UUID) | Obligatorio |
| `memberUuid` | string (UUID) o `null` | Opcional. `null`/omitido → titular logueado; si se envía, debe ser un familiar del mismo grupo |

### Respuesta 200 — `InscripcionActividadDTO`

```json
{
  "inscripcionUuid": "…",
  "actividadUuid": "…",
  "actividadNombre": "Fútbol Sub-15",
  "memberUuid": "…",
  "memberNombre": "Juan",
  "memberApellido": "Pérez",
  "fechaInscripcion": "26/08/2026",
  "actividadCompleta": false,
  "cuotaGenerada": true,
  "cuotaUuid": "…",
  "cuotaAdvertencia": null
}
```

| Campo | Notas |
|---|---|
| `memberUuid`/`memberNombre`/`memberApellido` | Del socio que quedó inscripto — el titular o el familiar elegido, según lo enviado |
| `actividadCompleta` | Si pasó a `true`, la actividad llegó al cupo máximo con esta inscripción |
| `cuotaGenerada` | Si es `false`, revisar `cuotaAdvertencia` para saber por qué se omitió la cuota (la inscripción igual queda confirmada) |

La lógica de negocio (duplicados, cupo, estado del socio, edad vs. categoría) es **idéntica** a la del flujo de backoffice — ver [`docs/API.md` §11](API.md#11-actividades-e-inscripciones) para el detalle de cada regla.

## 4. Manejo de errores

| Status | Cuándo | Qué mostrar |
|---|---|---|
| **400** | Socio ya inscripto, actividad completa/sin cupo, socio no `ACTIVO` (Moroso, Inhabilitado, Suspendido, Inactivo), o edad fuera del rango de la categoría de la actividad | El mensaje de negocio viene en `userMessage` — mostrarlo tal cual |
| **403** | El logueado es familiar (no tiene `portal.activities.enroll`), o el `memberUuid` enviado no pertenece al grupo familiar del titular logueado | Si pasa lo primero, no debería llegar a mostrarse el flujo (tratar como sesión vencida / permisos cambiaron); si pasa lo segundo, revisar el selector de familiares — probablemente está mostrando un uuid que ya no corresponde al grupo |
| **404** | `actividadUuid` no existe (o no pertenece al club) | El listado de actividades pudo haber cambiado entre que se cargó la pantalla y que el socio confirmó — refrescar el listado |

## 5. Resumen del flujo (pseudocódigo)

```text
1. Traer actividades disponibles (listado existente del club)
2. (Opcional) GET /portal/member/grupo → armar selector "inscribir a: yo / <familiar>"
3. POST /portal/member/actividad/inscripcion → { actividadUuid, memberUuid }
4. Si 200 → mostrar confirmación con memberNombre/memberApellido + actividadNombre;
   si cuotaGenerada = false, mostrar cuotaAdvertencia como aviso informativo
5. Si error → mostrar el mensaje correspondiente según la tabla de errores
```
