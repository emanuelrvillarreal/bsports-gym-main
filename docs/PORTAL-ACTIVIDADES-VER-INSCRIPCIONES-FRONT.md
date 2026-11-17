# Guía de integración front — Ver actividades inscriptas (portal del socio)

Guía para integrar los endpoints nuevos que permiten a un socio ver **sus propias** actividades inscriptas y las de **sus familiares**, y explorar el catálogo completo de actividades del club desde el portal. Para el detalle completo de cada endpoint ver [`docs/API.md` §21](API.md#21-portal-del-socio). Payloads de ejemplo listos para probar: colección `postman/bsports-club.postman_collection.json` → carpeta **"06 · Portal · Socio" → "Actividades"**.

## Qué resuelve

Ya existía el alta/baja self-service de inscripciones (`POST /portal/member/actividad/inscripcion[/baja]`, ver [`docs/PORTAL-ACTIVIDADES-INSCRIPCION-FRONT.md`](PORTAL-ACTIVIDADES-INSCRIPCION-FRONT.md)), pero no había forma de **listar** qué actividades tiene un socio vigentes hoy, ni de que el socio explorara el catálogo completo del club por su cuenta. Estos tres endpoints cierran ese flujo:

1. `GET /portal/member/actividad/inscripcion` — mis inscripciones.
2. `GET /portal/member/actividad/inscripcion/familiar/{memberUuid}` — inscripciones de un familiar.
3. `POST /actividad/paginate` — catálogo completo de actividades del club (endpoint de backoffice, ahora también accesible al titular).

## 1. Pre-requisitos

1. Los dos primeros endpoints (`GET .../inscripcion` y `GET .../inscripcion/familiar/{memberUuid}`) usan el permiso `portal.activities.view`, que tienen **tanto** `SOCIO_TITULAR` **como** `FAMILIAR` — a diferencia del alta/baja (`portal.activities.enroll`/`unenroll`), que son solo del titular. Un familiar logueado **sí** puede ver sus propias inscripciones y las de sus familiares.
2. El tercer endpoint (`POST /actividad/paginate`) usa `activity.view`, que **solo** tiene `SOCIO_TITULAR` — un familiar logueado recibe **403**. No mostrar el botón "ver todas las actividades / inscribirme a una nueva" si el logueado es familiar.
3. Para armar el selector de familiares, traer el grupo con `GET /portal/member/grupo` (`portal.family.view`, solo titular) — de ahí sale el `memberUuid` que se usa en `GET /portal/member/actividad/inscripcion/familiar/{memberUuid}`. Un familiar logueado no tiene este permiso, así que no puede armar ese selector por su cuenta (ver nota al final).

## 2. Ver mis inscripciones

```
GET /portal/member/actividad/inscripcion
Authorization: Bearer <jwt del socio>
X-Company-Uuid: <uuid>
```

Sin body ni query params — el socio sale de la sesión (titular o familiar, cualquiera de los dos).

### Respuesta 200 — `InscripcionActividadDTO[]`

```json
[
  {
    "inscripcionUuid": "…",
    "actividadUuid": "…",
    "actividadNombre": "Fútbol Sub-15",
    "memberUuid": "…",
    "memberNombre": "Juan",
    "memberApellido": "Pérez",
    "fechaInscripcion": "26/08/2026",
    "actividadCompleta": false,
    "cuotaGenerada": false,
    "cuotaUuid": null,
    "cuotaAdvertencia": null
  }
]
```

`cuotaGenerada`/`cuotaUuid`/`cuotaAdvertencia` no aplican a un listado (esos campos solo tienen sentido en el momento del alta) — siempre vienen `false`/`null`. Ignorarlos en esta pantalla. Puede devolver `[]` si el socio no tiene inscripciones activas.

## 3. Ver las inscripciones de un familiar

```
GET /portal/member/actividad/inscripcion/familiar/{memberUuid}
Authorization: Bearer <jwt del socio>
X-Company-Uuid: <uuid>
```

`memberUuid` va en el **path** (no es query param), y debe ser un integrante del mismo grupo familiar que el logueado — sale de `GET /portal/member/grupo`.

Misma respuesta que el endpoint anterior (`InscripcionActividadDTO[]`), pero con las inscripciones de ese familiar.

## 4. Explorar el catálogo completo de actividades (solo titular)

Los dos endpoints anteriores solo muestran lo que **ya está inscripto**. Para que el titular vea qué otras actividades ofrece el club (y elija una para inscribirse con `POST /portal/member/actividad/inscripcion`), usar el mismo listado que usa backoffice:

```
POST /actividad/paginate
Authorization: Bearer <jwt del socio titular>
X-Company-Uuid: <uuid>
Content-Type: application/json

{ "page": 1, "size": 10 }
```

Devuelve `Page<ActividadPaginateDTO>` — paginado estándar (`content[]`, `totalElements`, `totalPages`, etc., ver [`docs/API.md` §1](API.md#1-convenciones-generales)). Es exactamente el mismo endpoint que usa el backoffice para el ABM de actividades (ver [`docs/API.md` §11](API.md#11-actividades-e-inscripciones)) — no hay una versión "liviana" separada para portal.

**Importante:** este endpoint vive bajo `/actividad`, no bajo `/portal/member/actividad`. No es un endpoint nuevo — ya existía para backoffice — lo que cambió es que ahora el rol `SOCIO_TITULAR` también tiene el permiso `activity.view` que pide, así que puede llamarlo directo con su JWT.

## 5. Manejo de errores

| Status | Cuándo | Qué mostrar |
|---|---|---|
| **403** | `GET .../familiar/{memberUuid}` con un `memberUuid` que no pertenece al grupo del logueado | Revisar el selector de familiares — probablemente tiene un uuid que ya no corresponde al grupo |
| **403** | `POST /actividad/paginate` llamado por un familiar (no tiene `activity.view`) | No mostrar la opción de explorar el catálogo si el logueado es familiar |
| **404** | `GET .../familiar/{memberUuid}` con un `memberUuid` inexistente en el club | Refrescar el grupo familiar (`GET /portal/member/grupo`) — pudo haberse dado de baja |

## 6. Resumen del flujo (pseudocódigo)

```text
1. GET /portal/member/actividad/inscripcion → mostrar "Mis actividades"
2. Si el logueado es titular:
   a. GET /portal/member/grupo → armar selector de familiares
   b. Por cada familiar seleccionado: GET /portal/member/actividad/inscripcion/familiar/{memberUuid}
   c. Botón "ver catálogo / inscribirme a algo nuevo" → POST /actividad/paginate
      → elegido uno → POST /portal/member/actividad/inscripcion { actividadUuid, memberUuid }
3. Si el logueado es familiar: solo mostrar su propio listado (paso 1);
   no mostrar botones de explorar catálogo ni inscribirse (son solo del titular)
```
