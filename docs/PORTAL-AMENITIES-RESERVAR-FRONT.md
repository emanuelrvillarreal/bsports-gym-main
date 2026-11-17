# Guía de integración front — Reserva self-service de amenities (portal del socio)

Guía para integrar los endpoints de reserva y pago self-service de amenities: `POST /portal/amenities/{amenityUuid}/reservar` (crea la reserva y cobra) y `POST /portal/amenities/reservas/{reservaUuid}/pago` (regenera un pago para una reserva ya existente). Ambos permiten a un socio titular reservar y pagar un amenity (parrilla, quincho, etc.) **sin intervención de staff**. Para el detalle completo de cada endpoint ver [`docs/API.md` §21](API.md#21-portal-del-socio). Payloads de ejemplo listos para probar: colección `postman/bsports-club.postman_collection.json` → carpeta **"06 · Portal · Socio" → "Amenities"**.

## Qué resuelve

Hasta ahora, desde el portal el socio solo podía **ver** amenities (`GET /portal/amenities`) y sus propias reservas (`POST /portal/amenities/reservas/paginate`); la reserva la creaba y cobraba siempre el staff en backoffice. Este endpoint nuevo colapsa en **un solo llamado** lo que en backoffice son dos pasos (crear reserva + cobrar seña o total), y fuerza el pago por Mercado Pago porque el socio no tiene forma de cobrar en efectivo.

## 1. Pre-requisitos antes de mostrar el flujo

1. El usuario logueado debe ser **socio titular** (no familiar) y estar habilitado para el portal (no `INHABILITADO` / `SUSPENDIDO` / `INACTIVO`). Si no lo es, el backend responde **403** — no mostrar la opción de reservar a un familiar.
2. Traer la lista de amenities con `GET /portal/amenities` (permiso `portal.amenities.view`) para que el socio elija cuál y vea `montoHora`, `porcentajeSena`, `diasSemana`, `horaInicio`/`horaFin` — esos datos determinan qué horarios son válidos para el paso 2.

## 2. Armar el horario a reservar

El front debe construir `fechaDesde` / `fechaHasta` respetando las mismas reglas que ya aplica el flujo de backoffice (documentadas en [`docs/API.md` §15](API.md#15-amenities-y-reservas)):

| Regla | Detalle |
|---|---|
| Solo titular | El endpoint ya fuerza esto — no hace falta que el front lo valide, pero si el usuario es familiar el backend responde 403 antes de llegar a esta regla |
| Tope semanal | Máx. **2 reservas de amenities por semana calendario** (lunes a domingo) por titular, sumando todas sus reservas activas (`PENDIENTE_SENA` o `CONFIRMADA`) |
| Solapamiento | No puede haber otro slot del **mismo amenity** que se superponga con el horario elegido |
| Rango horario | `fechaDesde`–`fechaHasta` debe caer dentro de `diasSemana` y `horaInicio`/`horaFin` del amenity elegido |
| Duración mínima facturable | 1 hora (si el rango es menor, igual se cobra 1 hora) |

Ninguna de estas reglas tiene un endpoint de "preview" — el front debe validar lo que pueda en el cliente (día/horario dentro del amenity, `fechaHasta > fechaDesde`) para dar feedback inmediato, pero la validación real ocurre en el servidor al llamar `/reservar`. Mostrar los errores 400 que devuelva el backend (ver tabla de errores más abajo).

## 3. Llamar al endpoint

```
POST /portal/amenities/{amenityUuid}/reservar
Authorization: Bearer <jwt del socio>
X-Company-Uuid: <uuid>
Content-Type: application/json
```

```json
{
  "fechaDesde": "2026-06-09T14:00:00",
  "fechaHasta": "2026-06-09T16:00:00",
  "tipoPago": "SENA"
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `fechaDesde` | ISO datetime (`yyyy-MM-ddTHH:mm:ss`) | Sin zona horaria — el sistema opera en UTC-3 |
| `fechaHasta` | ISO datetime | Debe ser posterior a `fechaDesde` |
| `tipoPago` | `"SENA"` \| `"TOTAL"` | `SENA` cobra el anticipo (`montoHora × porcentajeSena`); `TOTAL` cobra el 100% en el mismo paso |

**No se envía** `memberUuid` (el titular sale del JWT) ni datos de cobro (`canalCobro`, `medioPago`): el backend los fuerza siempre a Mercado Pago.

### Respuesta 200 — `ReservaAmenityCobroResultDTO`

```json
{
  "reservaUuid": "8e2f5c3a-…",
  "tipoCobro": "SENA",
  "monto": 6000.00,
  "concepto": "Seña reserva amenity - Parrilla 1",
  "movimientoUuid": null,
  "qr": "iVBORw0KGgoAAAANSUhEUgAA…"
}
```

| Campo | Notas |
|---|---|
| `reservaUuid` | Guardarlo — es lo que después se usa para trackear el estado de la reserva |
| `tipoCobro` | Eco de `tipoPago` enviado (`SENA` o `TOTAL`) |
| `monto` | Importe cobrado (la seña o el total, según `tipoPago`) |
| `movimientoUuid` | **Siempre `null`** en este flujo — el pago es Mercado Pago, y el movimiento de tesorería recién se registra cuando MP confirma el pago por webhook (no es síncrono) |
| `qr` | PNG en **Base64** — mostrarlo como `<img src="data:image/png;base64,{qr}" />`. Es la representación en QR del link de checkout (`init_point`) de Mercado Pago |

⚠️ **A diferencia de `POST /treasury/cuota/cobro`** (que devuelve `mercadoPagoInitPoint` como URL de texto para redirigir el navegador), este endpoint solo devuelve la imagen QR — no hay un campo con la URL en texto plano. Dos formas de resolverlo en el front:

- **Mostrar el QR tal cual** y que el socio lo escanee con la cámara de otro dispositivo (funciona siempre, sin dependencias extra).
- **Decodificar el QR en el cliente** con una librería estándar (ej. `jsQR`) para extraer la URL y redirigir el navegador directamente — recomendado en mobile/desktop web, donde pedirle al usuario que escanee un QR de su propia pantalla no tiene sentido.

## 4. Confirmar que el pago se acreditó

El pago **no es síncrono**: la respuesta 200 solo confirma que se creó la reserva y se inició el checkout, no que ya se cobró. La reserva queda en `PENDIENTE_SENA` hasta que Mercado Pago liquide el pago vía webhook (server-to-server); recién ahí pasa a `CONFIRMADA` (con `saldoPendiente` recalculado) y el slot a `CONFIRMADO`.

Para saber cuándo se acreditó, el front tiene que **volver a consultar el estado de la reserva** después de que el socio complete el pago en Mercado Pago. Opciones:

1. **Polling simple (recomendado):** después de mostrar el QR, hacer polling a `POST /portal/amenities/reservas/paginate` (o filtrar la respuesta por el `reservaUuid` guardado) cada pocos segundos hasta ver `estado: "CONFIRMADA"` o hasta que el usuario cierre la pantalla.
2. **Pantalla de retorno de Mercado Pago:** si el front abre el checkout en una pestaña/redirect (no solo como QR), Mercado Pago vuelve a la app con `external_reference` en la URL de retorno; ese valor se puede usar contra `GET /pago/{externalReferenceMp}` para el estado del pago puntual (ver [`docs/API.md` §20](API.md#20-mercado-pago)). Este endpoint responde por pago, no por reserva — para reflejar el estado final de la reserva (slot, `saldoPendiente`) siempre hay que volver a mirar la reserva.

No hay webhook ni SSE hacia el front — es responsabilidad del cliente re-consultar.

## 5. Vencimiento de la seña

Igual que en el flujo de backoffice: si la reserva queda en `PENDIENTE_SENA` y no se confirma el pago, **vence a las 48 horas** de creada (`venceSenaAt` en la respuesta original, si se necesita mostrar countdown — hay que guardarlo aparte, ya que `ReservaAmenityCobroResultDTO` no lo trae; para tenerlo actualizado, consultarlo vía el paginado de reservas). Al vencer, un job automático cancela la reserva (`VENCIDA`) y **elimina el slot** — no queda visible en el calendario. Si esto pasa, el socio tiene que volver a llamar `/reservar` desde cero; **no hay forma de "reactivar" una reserva vencida** — el endpoint de reintento de pago del punto siguiente solo funciona mientras la reserva sigue viva (`PENDIENTE_SENA` o `CONFIRMADA` con saldo).

## 6. Reintentar el pago de una reserva existente — `POST /portal/amenities/reservas/{reservaUuid}/pago`

Si el socio cierra la pestaña de Mercado Pago antes de pagar, el checkout expira, o simplemente quiere reintentar, **no hace falta reservar de nuevo** (eso arriesgaría perder el horario si otro socio lo toma primero): mientras la reserva siga viva, este endpoint genera un **nuevo** intento de pago sobre la misma reserva.

```
POST /portal/amenities/reservas/{reservaUuid}/pago
Authorization: Bearer <jwt del socio>
X-Company-Uuid: <uuid>
Content-Type: application/json
```

```json
{ "tipoPago": "SENA" }
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `tipoPago` | Depende del estado — ver tabla abajo | `"SENA"` \| `"TOTAL"` |

El body puede omitirse por completo cuando la reserva ya está `CONFIRMADA` con saldo pendiente (ver tabla).

**El sistema no recuerda qué eligió el socio la primera vez** (seña o total): mientras la reserva sigue `PENDIENTE_SENA` sin seña pagada, el front tiene que volver a preguntarle (o recordar localmente qué había elegido en el primer intento y reenviarlo).

| Estado actual de la reserva | Qué hacer en el front |
|---|---|
| `PENDIENTE_SENA`, sin seña pagada | Enviar `tipoPago` (`SENA` o `TOTAL`) — es obligatorio, **400** si falta |
| `CONFIRMADA`, `saldoPendiente > 0` | Llamar sin `tipoPago` (o con cualquier valor: se ignora) — siempre cobra el saldo |
| `CONFIRMADA`, `saldoPendiente = 0` | No llamar — no hay nada que pagar. Si se llama igual: **409** |
| `CANCELADA` / `VENCIDA` | No llamar — no se puede reactivar. Si se llama igual: **409** |

**Ownership:** a diferencia de `/reservar`, acá el `reservaUuid` sí viaja en la URL — el backend valida que sea una reserva del socio logueado (**403** si es de otro socio). El front solo debería ofrecer este botón sobre reservas que ya vinieron en la respuesta de `POST /portal/amenities/reservas/paginate` del propio socio.

**Respuesta 200:** mismo `ReservaAmenityCobroResultDTO` que `/reservar` — nuevo `qr`, `movimientoUuid: null` hasta el webhook. Aplican los mismos puntos 3 (mostrar el QR) y 4 (polling para confirmar) de arriba.

⚠️ **No mostrar dos checkouts abiertos a la vez** para la misma reserva (por ejemplo, no dejar que el socio reintente mientras todavía tiene otra pestaña de MP abierta de un intento anterior): el backend no bloquea generar varios pagos pendientes para la misma reserva, y aunque la liquidación es idempotente del lado del club (no duplica el movimiento ni rompe el estado), **si el socio efectivamente paga ambos, Mercado Pago le cobra dos veces**. Ante un reintento, tratar el intento anterior como descartado en la UI (ocultar o invalidar su QR).

## 7. Manejo de errores

| Status | Cuándo | Qué mostrar |
|---|---|---|
| **400** | Fecha inválida (`fechaHasta <= fechaDesde`), fuera del horario/días del amenity, tope semanal alcanzado (2 reservas/semana) | El mensaje de negocio viene en `userMessage` — mostrarlo tal cual, son mensajes pensados para el usuario final |
| **403** | El logueado es familiar, o el socio no está habilitado para el portal (`INHABILITADO`/`SUSPENDIDO`/`INACTIVO`) | No debería llegar a mostrarse el flujo si el paso 1 ya filtró esto — tratarlo como error inesperado (sesión vencida / permisos cambiaron) |
| **404** | `amenityUuid` (en `/reservar`) o `reservaUuid` (en `/pago`) no existe / no está activo | La lista de amenities o de reservas pudo haber cambiado — refrescar `GET /portal/amenities` o el paginado de reservas |
| **409** | `/reservar`: ya existe un slot solapado en el mismo amenity para ese horario (otro socio lo reservó mientras se completaba el formulario). `/pago`: la reserva ya no admite un nuevo pago (`CONFIRMADA` sin saldo, `CANCELADA` o `VENCIDA`) | `/reservar`: pedir otro horario. `/pago`: refrescar el estado de la reserva y ocultar la opción de pagar si ya no corresponde |

Si la llamada a `/reservar` falla (cualquier status), **no queda ninguna reserva a medio crear**: la creación y el cobro corren en una única transacción del backend, así que un error en el paso de cobro revierte también la reserva. El front puede reintentar `/reservar` directamente sin necesidad de limpiar nada.

## 8. Resumen del flujo (pseudocódigo)

```text
1. GET /portal/amenities                            → elegir amenity
2. Front valida localmente día/horario/duración
3. POST /portal/amenities/{amenityUuid}/reservar     → { fechaDesde, fechaHasta, tipoPago }
4. Si 200 → mostrar QR (o decodificarlo y redirigir) + guardar reservaUuid
5. Poll POST /portal/amenities/reservas/paginate hasta ver estado = CONFIRMADA (o que el socio cierre)
6a. Si nunca se confirma y pasan 48h → la reserva desaparece (VENCIDA); ofrecer reservar de nuevo desde el paso 1
6b. Si el socio quiere reintentar antes de las 48h (cerró el checkout, expiró, etc.) →
    POST /portal/amenities/reservas/{reservaUuid}/pago  → { tipoPago } (u omitido si ya está CONFIRMADA con saldo)
    → volver al paso 4 con el nuevo qr
```
