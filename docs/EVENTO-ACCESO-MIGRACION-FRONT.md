# Migración front — Eventos de acceso / entradas

Guía de los cambios en `POST/GET/PUT/DELETE /evento-acceso/**` que el front necesita integrar. Para el detalle completo de cada endpoint, ver [`docs/API.md` §19](API.md#19-eventos-de-acceso--entradas). Payloads de ejemplo listos para probar: colección `postman/bsports-club.postman_collection.json` → carpeta **"Eventos acceso / entradas"**.

## Resumen de lo que cambió

1. **Alta/edición de evento** ahora piden 4 campos nuevos obligatorios: `permiteMorosos`, `permiteInvitados`, `montoMorosos`, `montoInvitados`.
2. **`POST .../scan` ya no lanza 402.** Siempre responde 200 con un `estado` explícito (`ACCESO_PERMITIDO` / `DEBE_PAGAR` / `BLOQUEADO`). Solo informa — no cobra ni decide si dejar pasar, eso lo decide el front con el `estado`.
3. **`/invitado` y `/moroso` desaparecieron.** Se reemplazan por un único endpoint `POST .../cobro`, con `tipo: "SOCIO" | "INVITADO"` + un `payload` que cambia de forma según el tipo.
4. **Nuevo endpoint de historial:** `POST .../historial/paginate` — lista cada scan y cada cobro del evento.

---

## 1. Alta y edición de evento (`POST /evento-acceso`, `PUT /evento-acceso/{uuid}`)

Agregar al formulario:

| Campo | Tipo | Obligatorio | Qué es |
|---|---|---|---|
| `permiteMorosos` | boolean | Sí | Si está en `false`, ningún socio moroso puede acceder ni pagar entrada en este evento |
| `permiteInvitados` | boolean | Sí | Si está en `false`, no se puede cobrar entrada a invitados en este evento |
| `montoMorosos` | decimal > 0 | Sí | Precio de entrada para un socio moroso |
| `montoInvitados` | decimal > 0 | Sí | Precio de entrada para un invitado |

El campo `monto` que ya existía **se mantiene** y ahora tiene un significado más específico: es el precio que se le cobra a un **socio activo** (al día) que igual tiene que pagar entrada — ver punto 3.

⚠️ Si el formulario actual no pedía estos 4 campos, las llamadas a alta/edición van a empezar a fallar con 400 hasta que se agreguen. No hay valores por defecto en el request (si se quiere "sin restricción", enviar `permiteMorosos: true` / `permiteInvitados: true` explícitamente).

`GET`/`POST .../paginate` devuelven estos mismos 4 campos en la respuesta — no hace falta un segundo fetch para mostrarlos en un formulario de edición.

---

## 2. Scan del carnet (`POST .../scan`)

**No cambia el request** (`{ "userUuid": "..." }`), pero sí la respuesta:

```json
{
  "memberUuid": "…",
  "nombre": "Carlos",
  "apellido": "Gómez",
  "numeroSocio": "500",
  "estado": "DEBE_PAGAR",
  "motivo": "Carlos Gómez (socio 500) es moroso y debe pagar para acceder al evento \"Partido amistoso\"",
  "montoAPagar": 3500.00
}
```

| `estado` | Qué hacer en el front |
|---|---|
| `ACCESO_PERMITIDO` | Dejar pasar. `motivo` y `montoAPagar` vienen `null`. |
| `DEBE_PAGAR` | Mostrar `montoAPagar` y ofrecer cobrar → disparar `POST .../cobro` con `tipo: "SOCIO"`, `payload.userUuid` = el mismo `userUuid` escaneado. |
| `BLOQUEADO` | No dejar pasar. Mostrar `motivo` (explica si es por estado del socio o porque el evento no admite morosos). |

**Antes** el front tenía que capturar un error HTTP 402 para saber que había que cobrar. **Ahora** todo viene en una respuesta 200 — sacar cualquier `catch` que estuviera mirando el status code 402 específicamente para este flujo.

Cada scan (sea cual sea el `estado`) queda registrado en el historial del evento — no hace falta que el front lo guarde aparte para armar una planilla de asistencia.

---

## 3. Cobro unificado (`POST .../cobro`)

Reemplaza **tanto** a `POST .../invitado` **como** a `POST .../moroso`. Un solo shape de request:

```json
{
  "tipo": "SOCIO",
  "payload": { "...": "..." }
}
```

`tipo` es `"SOCIO"` o `"INVITADO"`. `payload` cambia de forma según `tipo` — armarlo como un objeto separado en el código del front (ej. dos builders: `buildPayloadSocio(...)` / `buildPayloadInvitado(...)`) y envolverlo en `{ tipo, payload }` al llamar.

### 3.1. `tipo: "SOCIO"` (reemplaza a `/moroso`)

```json
{
  "tipo": "SOCIO",
  "payload": {
    "userUuid": "…",
    "canalCobro": "MANUAL",
    "cantidadEntradas": 1,
    "medioPago": "EFECTIVO",
    "fecha": "2026-08-24",
    "observaciones": "Entrada en puerta"
  }
}
```

Mismos campos que el viejo `EventoAccesoMorosoCobroCommand` (`userUuid`, `canalCobro`, `cantidadEntradas`, `medioPago`, `fecha`, `observaciones`).

**Cambio de comportamiento importante:** antes este endpoint **solo** aceptaba socios morosos (rechazaba `ACTIVO` con 409 siempre). Ahora:

- Socio **moroso** → cobra `montoMorosos` del evento, **si** `permiteMorosos = true` y `montoMorosos > 0`. Si no, **403**.
- Socio **activo (al día)** → si `evento.monto > 0`, también se le puede cobrar entrada (usa el `monto` general, no `montoMorosos`). Si `monto` es `0`/vacío, sigue devolviendo **409** "El socio no está moroso" — no hay nada que cobrarle.
- Socio inhabilitado/suspendido/inactivo → **403**, igual que antes.

En la práctica: si el front solo llama a `/cobro` cuando `scan` devolvió `estado: "DEBE_PAGAR"`, este caso ya está cubierto — pero si en algún flujo se permite forzar el cobro a un socio que scan marcó `ACCESO_PERMITIDO`, ahora puede tener éxito (si el evento tiene `monto > 0`) en vez de fallar siempre.

### 3.2. `tipo: "INVITADO"` (reemplaza a `/invitado`)

```json
{
  "tipo": "INVITADO",
  "payload": {
    "canalCobro": "MANUAL",
    "cantidadEntradas": 1,
    "medioPago": "EFECTIVO",
    "fecha": "2026-08-24",
    "observaciones": "Invitado en puerta",
    "nombre": "Juan Pérez"
  }
}
```

Mismos campos que antes, **más uno nuevo: `nombre`** (opcional pero recomendado). Como el invitado no tiene `Member`, es el único dato legible que va a quedar guardado en el historial del evento — sin él, la fila del historial va a tener `nombre: null`.

Requiere `evento.permiteInvitados = true` y `evento.montoInvitados > 0` — si el evento no admite invitados, **403**.

### 3.3. Respuesta unificada

Ambos casos devuelven el mismo shape, `EventoAccesoCobroResultDTO`:

```json
{
  "eventoUuid": "…",
  "tipo": "SOCIO",
  "resultado": "ACCESO_PERMITIDO",
  "cantidadEntradas": 1,
  "monto": 3500.00,
  "concepto": "Acceso moroso - Partido amistoso - Carlos Gómez (socio 500)",
  "movimientoUuid": "…",
  "qr": null,
  "memberUuid": "…",
  "nombre": "Carlos",
  "apellido": "Gómez",
  "numeroSocio": "500"
}
```

- `resultado: "ACCESO_PERMITIDO"` → `canalCobro` fue `MANUAL`, ya se cobró, `movimientoUuid` viene seteado.
- `resultado: "DEBE_PAGAR"` → `canalCobro` fue `MERCADO_PAGO`, pago pendiente, `qr` viene con el Base64 y `movimientoUuid` es `null` hasta que se apruebe el webhook (sin cambios respecto al flujo viejo de MP).
- Cuando `tipo: "INVITADO"`, los campos `memberUuid`/`nombre`/`apellido`/`numeroSocio` vienen `null`.

### 3.4. Mapeo directo de código viejo → nuevo

| Antes | Ahora |
|---|---|
| `POST .../moroso` con body plano | `POST .../cobro` con `{ tipo: "SOCIO", payload: <mismo body de antes> }` |
| `POST .../invitado` con body plano | `POST .../cobro` con `{ tipo: "INVITADO", payload: <mismo body de antes + nombre opcional> }` |
| Response `EventoAccesoMorosoCobroResultDTO` | Response `EventoAccesoCobroResultDTO` (superset — mismos campos + `tipo`/`resultado`) |
| Response `EventoAccesoInvitadoResultDTO` | Response `EventoAccesoCobroResultDTO` (mismos campos, `memberUuid`/etc. en `null`) |

Si el front tenía dos funciones separadas (`cobrarInvitado()` / `cobrarMoroso()`) apuntando a URLs distintas, alcanza con unificarlas en una sola que arme `{ tipo, payload }` y pegue siempre a `POST .../cobro`.

---

## 4. Historial del evento (`POST .../historial/paginate`) — nuevo

Antes no existía. Sirve para mostrar una planilla de "quién entró/cobró en este evento":

```json
// Request (body opcional)
{ "page": 1, "size": 10, "search": "" }
```

```json
// Response — Page<EventoAccesoHistorialDetalleDTO>
{
  "content": [
    {
      "uuid": "…",
      "tipo": "SOCIO",
      "nombre": "Carlos Gómez",
      "resultado": "DEBE_PAGAR",
      "motivo": "Carlos Gómez (socio 500) es moroso y debe pagar para acceder al evento \"Partido amistoso\"",
      "monto": 3500.00,
      "memberUuid": "…",
      "numeroSocio": "500",
      "fecha": "…"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

`search` filtra por `nombre` (LIKE, case-insensitive). Se genera una fila **por cada** `scan` (los 3 resultados) y **por cada** `cobro` exitoso — incluidos los bloqueos por `permiteMorosos`/`permiteInvitados` en `false`. No requiere `treasury.collect`, solo club resuelto.

⚠️ **Formato de `fecha`:** el campo no tiene `@JsonFormat` configurado en el backend, así que **no** viaja como `dd/MM/yyyy HH:mm:ss` (a diferencia de la mayoría de las fechas de esta API) — viaja en el formato timestamp/array por defecto de Jackson. Confirmar el formato exacto contra un ambiente real antes de parsear (`fecha` en `EventoAccesoDetalleDTO` tiene el mismo problema).

---

## 5. Checklist de migración

- [ ] Agregar `permiteMorosos`, `permiteInvitados`, `montoMorosos`, `montoInvitados` a los formularios de alta/edición de evento.
- [ ] Sacar el manejo de status 402 en el flujo de `scan`; leer `estado`/`motivo`/`montoAPagar` de la respuesta 200.
- [ ] Reemplazar las llamadas a `/invitado` y `/moroso` por `POST .../cobro` con `{ tipo, payload }`.
- [ ] Agregar el campo `nombre` (opcional) al formulario de cobro de invitado.
- [ ] Actualizar los tipos/interfaces del front: `EventoAccesoInvitadoResultDTO` y `EventoAccesoMorosoCobroResultDTO` se unifican en `EventoAccesoCobroResultDTO`.
- [ ] (Opcional) Agregar una vista de historial del evento usando `POST .../historial/paginate`.
- [ ] Verificar en un ambiente real el formato serializado de `fecha` antes de parsearlo en las pantallas de evento/historial.
