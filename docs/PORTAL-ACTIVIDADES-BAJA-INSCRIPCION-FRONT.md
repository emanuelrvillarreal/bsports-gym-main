# Guía de integración front — Baja self-service de inscripción a actividades (portal del socio)

Guía para integrar `POST /portal/member/actividad/inscripcion/baja`, el endpoint nuevo que permite a un socio titular cancelar **su propia inscripción o la de un familiar de su grupo** en una actividad, **sin intervención de staff**. Para el detalle completo del endpoint ver [`docs/API.md` §21](API.md#21-portal-del-socio). Payloads de ejemplo listos para probar: colección `postman/bsports-club.postman_collection.json` → carpeta **"06 · Portal · Socio" → "Actividades"**. Complementa el flujo de alta descripto en [`docs/PORTAL-ACTIVIDADES-INSCRIPCION-FRONT.md`](PORTAL-ACTIVIDADES-INSCRIPCION-FRONT.md).

## Qué resuelve

Hasta ahora, cancelar una inscripción a una actividad dependía de que staff lo hiciera desde backoffice (`POST /actividad/{actividadUuid}/inscripcion/baja`). Este endpoint nuevo habilita esa baja directamente desde el portal, para el propio titular o para cualquier familiar de su grupo.

## 1. Pre-requisitos antes de mostrar el flujo

1. El usuario logueado debe ser **socio titular** (no familiar). El permiso `portal.activities.unenroll` solo lo tiene el rol `SOCIO_TITULAR` — un familiar logueado directamente recibe **403**. No mostrar la opción de darse de baja (ni a sí mismo ni a otro) si el logueado es familiar.
2. Mostrar la baja solo sobre actividades en las que el socio (o el familiar elegido) tiene una inscripción **activa** — usar el mismo listado que ya use el front para mostrar "mis actividades" (por ejemplo, el calendario del portal, `POST /portal/calendario`, que ya filtra por inscripción activa).
3. Si el flujo permite elegir **a quién** dar de baja, traer el grupo familiar con `GET /portal/member/grupo` (`portal.family.view`, solo titular) para armar el selector.

## 2. Elegir a quién dar de baja

Igual que en el alta, el campo `memberUuid` del body decide el destino:

| Caso | Valor de `memberUuid` |
|---|---|
| Dar de baja al titular logueado (uno mismo) | `null` u **omitido** |
| Dar de baja a un familiar del grupo | UUID de ese familiar |

No hay forma de dar de baja a un socio que **no** pertenezca al mismo grupo familiar que el titular logueado — el backend lo valida y devuelve **403** si se intenta.

## 3. Llamar al endpoint

```
POST /portal/member/actividad/inscripcion/baja
Authorization: Bearer <jwt del socio titular>
X-Company-Uuid: <uuid>
Content-Type: application/json
```

**Darme de baja:**

```json
{ "actividadUuid": "8e2f5c3a-…", "memberUuid": null }
```

**Dar de baja a un familiar:**

```json
{ "actividadUuid": "8e2f5c3a-…", "memberUuid": "3b7a1d90-…" }
```

| Campo | Tipo | Notas |
|---|---|---|
| `actividadUuid` | string (UUID) | Obligatorio |
| `memberUuid` | string (UUID) o `null` | Opcional. `null`/omitido → titular logueado; si se envía, debe ser un familiar del mismo grupo |

### Respuesta 200 — `InscripcionActividadDTO`

Mismo shape que el alta (ver [`docs/PORTAL-ACTIVIDADES-INSCRIPCION-FRONT.md`](PORTAL-ACTIVIDADES-INSCRIPCION-FRONT.md#3-llamar-al-endpoint)): `inscripcionUuid`, `actividadUuid`, `actividadNombre`, `memberUuid`/`memberNombre`/`memberApellido`, `fechaInscripcion`, `actividadCompleta`. Los campos de cuota (`cuotaGenerada`, `cuotaUuid`, `cuotaAdvertencia`) no aplican a la baja.

⚠️ **La baja no anula cuotas ya generadas.** Si se había generado una cuota de actividad por esta inscripción, sigue en pie — el front no debe dar a entender que la baja implica un reembolso o ajuste de cuenta corriente.

## 4. Manejo de errores

| Status | Cuándo | Qué mostrar |
|---|---|---|
| **403** | El logueado es familiar (no tiene `portal.activities.unenroll`), o el `memberUuid` enviado no pertenece al grupo familiar del titular logueado | Si pasa lo primero, no debería llegar a mostrarse el flujo (tratar como sesión vencida / permisos cambiaron); si pasa lo segundo, revisar el selector de familiares |
| **404** | `actividadUuid` no existe, o el socio resuelto (titular/familiar) **no tiene una inscripción activa** en esa actividad | La actividad pudo haber cambiado (ya se le dio de baja desde otro dispositivo, o staff la dio de baja) — refrescar el listado de "mis actividades" antes de reintentar |

## 5. Resumen del flujo (pseudocódigo)

```text
1. Traer actividades con inscripción activa del socio (o del familiar elegido)
2. (Opcional) GET /portal/member/grupo → armar selector "dar de baja a: yo / <familiar>"
3. Confirmar la acción con el socio (es una baja, no reversible desde el front)
4. POST /portal/member/actividad/inscripcion/baja → { actividadUuid, memberUuid }
5. Si 200 → mostrar confirmación con memberNombre/memberApellido + actividadNombre;
   aclarar que cuotas ya generadas no se anulan
6. Si error → mostrar el mensaje correspondiente según la tabla de errores
```
