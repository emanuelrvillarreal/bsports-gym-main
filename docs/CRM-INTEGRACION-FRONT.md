# Guía frontend — Integración CRM (blue-crm)

Esta guía cubre los endpoints y comportamientos que el front necesita conocer de la integración
con el CRM (`blue-crm`). No cubre el webhook (`POST /webhookCrm`) ni los jobs internos — esos son
backend-a-backend, el front no los llama. Detalle técnico completo en
[`docs/API.md` §26](API.md#26-integración-crm-blue-crm).

## Qué hace el CRM en esta integración

El CRM es quien factura y cobra la suscripción del club. `bsports-club` no genera ni cobra esas
facturas — solo las refleja localmente (vía webhooks) y, si hace falta, corta el acceso a la
plataforma cuando el club no está al día.

## 1. Alta de club — son **dos requests** (solo panel sysadmin)

Poner un club en marcha son dos llamadas seguidas, en este orden:

1. **`POST /sysadmin/clubs`** — crea el club, su admin y el cliente en el CRM. Devuelve **201**
   con `company.uuid`.
2. **`POST /sysadmin/clubs/{companyUuid}/crm/suscribir`** — con ese `uuid`, lo suscribe al plan
   (ver más abajo).

⚠️ **Entre una y otra, el club nace con el acceso bloqueado**: si el CRM no le conoce ningún plan,
el paso 1 le marca el bloqueo y sus usuarios reciben **403** en todo request (el mismo 403 de
facturación vencida, ver [API.md](API.md#bloqueo-de-acceso-por-facturación-vencida)). Tampoco
tendría cupos para dar de alta socios: el chequeo es fail-closed. Conviene encadenar los dos pasos
en el mismo wizard y, si el segundo falla, dejarlo reintentable sin rehacer el alta — el club ya
existe, reintentar el paso 1 va a dar *"CUIT ya registrado"*.

El `SYSADMIN` no queda afuera: el interceptor de bloqueo lo deja pasar siempre, así que el panel
interno puede seguir operando sobre un club bloqueado (es justamente quien lo destraba).

**Excepción:** si ese CUIT **ya era cliente del CRM y tenía un plan vigente**, el paso 1 lo detecta
solo (consulta los planes del cliente al terminar el registro) y deja la suscripción ya espejada.
En ese caso el club arranca con cupos y con acceso, y el paso 2 es opcional — sirve para cambiarle
el plan. Para saber si pasó, mirá `suscripcionCrm` del club en `POST /companies/paginate-club`: si
viene completo, ya tiene plan.

Ojo: que tenga plan no alcanza. El paso 1 lo deja bloqueado igual si esa suscripción está
`ANULADA`, o si está `PENDIENTE_PAGO` y arrancó hace más de 10 días (tabla completa en
[API.md §3](API.md#3-sysadmin--alta-de-clubes)). En esos casos hay que hacer el paso 2 igual.

Para el paso 1, `POST /sysadmin/clubs` exige 2 campos más además de los datos del club:

| Campo | Obligatorio | Notas |
|---|---|---|
| `codigoCondicionImpositiva` | Sí | Código de condición impositiva del catálogo del CRM |
| `codigoProvincia` | Sí | Código de provincia del catálogo del CRM |

El `codigoPlan` **no va acá**: viaja en el paso 2.

**Request — solo los 2 campos nuevos** (van junto al resto de `ClubRegistrationRequestDTO`, ver
[API.md §3](API.md#3-sysadmin--alta-de-clubes) para el resto del body):

```json
{
  "codigoCondicionImpositiva": "RI",
  "codigoProvincia": "BA"
}
```

### `GET /sysadmin/clubs/crm-catalogo/planes`

Sin body, sin params. JWT `SYSADMIN`.

**Response — `List<CrmPlanResponseDTO>`:**

```json
[
  {
    "nombre": "Plan Estándar",
    "descripcion": "Plan estándar del club",
    "codigo": "plan-estandar",
    "precio": 25000.00,
    "precioEsFinal": true,
    "alicuotaIva": 21.00,
    "periodicidad": { "codigo": "mensual" },
    "productos": [
      { "codigo": "bsports-socios", "nombre": "Socios", "unidades": 200 },
      { "codigo": "bsports-admin", "nombre": "Administradores", "unidades": 3 }
    ]
  }
]
```

El campo `codigo` de cada ítem es el valor que va en `codigoPlan` (de `crm/suscribir` y
`crm/cambiar-plan`, ver más abajo). A diferencia de los otros dos catálogos, este
usa `integraciones/obtener-planes` del CRM — no depende del permiso extra que exige el módulo de
clientes, pero el CRM lo cachea y **la cache no distingue empresa**.

### `GET /sysadmin/clubs/crm-catalogo/provincias`

Sin body, sin params. JWT `SYSADMIN`.

**Response — `List<CrmProvinciaResponseDTO>`:**

```json
[
  { "uuid": "a1b2c3d4-...", "nombre": "Buenos Aires", "codigo": "BA", "pais": "Argentina", "paisUuid": "e5f6..." },
  { "uuid": "b2c3d4e5-...", "nombre": "Córdoba", "codigo": "CBA", "pais": "Argentina", "paisUuid": "e5f6..." }
]
```

### `GET /sysadmin/clubs/crm-catalogo/condiciones-impositivas`

Sin body, sin params. JWT `SYSADMIN`.

**Response — `List<CrmCondicionImpositivaResponseDTO>`:**

```json
[
  { "uuid": "c3d4e5f6-...", "nombre": "Responsable Inscripto", "codigo": "RI" },
  { "uuid": "d4e5f6a7-...", "nombre": "Monotributo", "codigo": "MT" }
]
```

El campo `codigo` de cada ítem de los tres catálogos (planes, provincias, condiciones
impositivas) es el valor que hay que mandar en `codigoPlan`/`codigoProvincia`/
`codigoCondicionImpositiva` — pintar el combo con `nombre` y guardar `codigo` como value.

⚠️ Estos 2 endpoints dependen de un permiso otorgado del lado del CRM al usuario de integración
de `bsports-club` (`CLIENTES_PROVINCIAS_VER`/`CLIENTES_CONDICIONES_IMPOSITIVAS_VER`). Si todavía
no está otorgado, van a devolver **502** en vez de la lista — no es un bug del front, hay que
confirmar con el equipo de CRM que ese permiso esté asignado.

Si alguno de los 2 códigos del alta es inválido, el CRM lo rechaza y **el alta completa del club
se revierte** (no queda un club a medio crear) — el error llega como un 400/502 normal del
backend (ver [formato de errores](API.md#formato-de-errores)), sin un formato especial. Lo mismo
si el club ya tenía un plan en el CRM que todavía no está sincronizado en el catálogo local: el
alta responde **409** *"El plan del club no está sincronizado en el catálogo local"* y hay que
reintentarla después de que corra la sincronización del catálogo.

### Suscribir a un plan / cambiar el plan (panel sysadmin)

Hay 3 endpoints — todos `POST`, JWT `SYSADMIN`, resuelven el club por `{companyUuid}` en el path
(**404** si no existe).

#### `POST /sysadmin/clubs/{companyUuid}/crm/suscribir`

**Es el paso 2 del alta**, y también sirve para cualquier club que **todavía no es cliente del
CRM**: lo da de alta allá (o reutiliza el cliente si el CUIT ya existía) y lo suscribe al plan.
Por eso vuelve a pedir `codigoCondicionImpositiva` y `codigoProvincia`: `Company` no los guarda.

Si sale bien y el club estaba bloqueado, **lo desbloquea en el momento**: después de este request
sus usuarios ya pueden entrar, sin esperar a ningún job.

**Request — `SysadminSuscripcionRequestDTO`:**

```json
{
  "codigoPlan": "plan-estandar",
  "codigoCondicionImpositiva": "RI",
  "codigoProvincia": "BA"
}
```

**Response — `SysadminSuscripcionResponseDTO`:**

```json
{
  "companyUuid": "8e2f5c3a-...",
  "codigoPlan": "plan-estandar",
  "nombrePlan": "Plan Estándar",
  "uuidSuscripcionCrm": "f1a2b3c4-...",
  "estado": "ACTIVA",
  "fechaProximaFacturacion": "15/07/2026 00:00"
}
```

#### `POST /sysadmin/clubs/{companyUuid}/crm/cambiar-plan`

Para un club que **ya es cliente del CRM**: cancela sus suscripciones vigentes y lo suscribe al
plan nuevo. No hace falta pasarle de nuevo los códigos de condición impositiva/provincia porque
el cliente ya existe en el CRM.

**Request — `SysadminCambioPlanRequestDTO`:**

```json
{ "codigoPlan": "plan-premium" }
```

**Response — `SysadminSuscripcionResponseDTO`:** mismo shape que en `suscribir`.

#### `POST /sysadmin/clubs/{companyUuid}/crm/desuscribir`

Sin body. Da de baja al club en el CRM y **le corta el acceso a la plataforma en el momento** (no
espera al job nocturno).

**Response — tal cual la devuelve el CRM (`CrmCancelarSuscripcionesResponseDTO`):**

```json
{
  "cuit": "20111111111",
  "suscripcionesAnuladas": 1,
  "facturasCompensadas": 2,
  "montoCompensado": 15000.00,
  "mensaje": "Suscripciones anuladas y deuda compensada correctamente."
}
```

`suscribir` es para el caso "este club nunca tuvo plan" (usa los mismos 3 catálogos de arriba);
`cambiar-plan` es para "este club ya tiene un plan y quiero moverlo a otro" (solo necesita el
catálogo de planes). Los tres actualizan
la tabla interna de la que sale el cupo de socios/administradores del club, así que después de
`cambiar-plan` el cupo nuevo aplica de inmediato (no hay que esperar a ningún job).

## 2. Facturas del club — `/factura`

Todos requieren JWT + header `X-Company-Uuid` (o `companyUuid` en el body para `SYSADMIN`, ver
abajo).

### `POST /factura/paginate` — Listado de facturas

**Permiso:** `factura.view` (solo `ADMIN`/`SYSADMIN` lo tienen).

```json
{ "page": 1, "size": 10, "estados": ["VENCIDA"], "orden": "FECHA_VENCIMIENTO_DESC" }
```

Todo opcional. `estados[]` filtra por `FacturaEstadoEnum` (`PENDIENTE`, `PAGADA`, `VENCIDA`); sin
filtro trae todos. Si el usuario logueado es `SYSADMIN`, hay que mandar `companyUuid` en el body
(no tiene club propio en el header).

**Response — `Page<FacturaPaginateRowDTO>`:**

```json
{
  "facturaUuid": "a1b2c3d4-...",
  "uuidFacturaCrm": "...",
  "codigoPlan": "plan-estandar",
  "codigosProductos": ["bsports-extra-1"],
  "importe": 25000.00,
  "fechaGeneracion": "01/06/2026",
  "fechaVencimiento": "15/06/2026",
  "estado": "VENCIDA"
}
```

`codigoPlan` puede venir `null` (una deuda puede originarse solo en productos, sin plan). El
`facturaUuid` es el identificador **local** — es el que hay que mandar a `/factura/pago`, nunca
`uuidFacturaCrm`.

`importe` es el monto de la factura (suma del precio del plan más el de los productos que
originaron la deuda, **con IVA ya aplicado** cuando el catálogo trae `precioEsFinal = false`/
alícuota informada — el front no tiene que sumar nada más). **Puede venir `null`**: se resuelve
una única vez, al momento en que llega la deuda del CRM, contra el catálogo local de planes y
productos; si en ese momento el plan o los productos todavía no estaban sincronizados, la factura
queda sin importe y no se recalcula después. La UI tiene que contemplar ese `null` como un caso
esperable (ej. mostrar "—" en vez del monto), no como un error.

### `POST /factura/pago` — Generar link de pago

**Permiso:** `factura.pagar` (solo `ADMIN`/`SYSADMIN`). Siempre resuelve el club por
`X-Company-Uuid` (a diferencia de `paginate`, **no** acepta `companyUuid` en el body).

```json
{
  "facturasUuid": ["a1b2c3d4-...", "b2c3d4e5-..."],
  "referencia": "pago-junio-2026",
  "fechaPago": "2026-06-20"
}
```

- `facturasUuid[]`: uuids **locales** (los `facturaUuid` de `paginate`), no los del CRM. Todas
  deben ser del club logueado — si alguna es de otro club o no existe, **404**.
- `referencia` y `fechaPago`: los define el front; el CRM los exige (si faltan, el 400 llega del
  CRM, no hay validación local previa).

**Response — tal cual la devuelve el CRM:**

```json
{ "id": "...", "init_point": "https://..." }
```

Redirigir al usuario a `init_point` (Mercado Pago), igual que en el resto de los flujos de pago
del sistema (cuotas, amenities).

**Adónde vuelve Mercado Pago:** el backend arma las 3 URLs de retorno con la base configurada en
`front.url` + un path fijo — **el front tiene que tener estas 3 rutas montadas** para recibir la
vuelta del checkout:

| Resultado | URL |
|---|---|
| Éxito | `{front.url}/pago-factura/exito` |
| Error/rechazado | `{front.url}/pago-factura/fallo` |
| Pendiente | `{front.url}/pago-factura/pendiente` |

Mismo patrón que el resto de los flujos de Mercado Pago del sistema (cuotas, amenities usan
`/pago/exito`, `/pago/fallo`, `/pago/pendiente`), pero bajo su propio prefijo `pago-factura` en
vez de `pago` — son rutas **distintas**, hay que montarlas aparte. No llevan ningún parámetro de
factura en la URL — si la pantalla necesita mostrar cuál factura se pagó, hay que volver a pedir
`POST /factura/paginate` al llegar ahí, no depender de un query param.

**Importante:** este endpoint solo genera el link — no marca nada como pagado en el momento. La
factura pasa a `PAGADA` recién cuando el CRM confirma el pago y notifica por webhook (asíncrono,
puede tardar). Si la UI necesita reflejar el pago, hay que volver a pedir el listado (`paginate`)
después, no asumir que quedó pagada apenas se vuelve del checkout de Mercado Pago.

## 3. Bloqueo de acceso por facturación vencida

Si el club tiene **alguna** factura en estado `VENCIDA`, el backend corta el acceso a **toda la
plataforma** con un 403 — no solo a `/factura/*`. Esto puede pasar en cualquier pantalla, en
cualquier momento (un job corre a la madrugada y puede bloquear un club que estaba funcionando
bien la noche anterior).

### Cómo detectarlo sin esperar un 403

El login (`UserCompanyRoleDTO`, dentro de la respuesta de `POST /user/login`) ya trae:

```json
{ "companyBlocked": true, ... }
```

Si viene `true`, la UI puede mostrar un aviso/banner apenas el usuario entra, sin esperar a que
algún request falle.

### Shape del 403 cuando ocurre

```json
{
  "status": "FORBIDDEN",
  "userMessage": "El club tiene facturación vencida",
  "errores": [
    { "name": "motivo", "value": "FACTURA_VENCIDA", "message": "Regularice el pago en el CRM para recuperar el acceso a la plataforma." }
  ]
}
```

El front puede chequear `errores[0].value === "FACTURA_VENCIDA"` en su interceptor global de
errores para distinguir este caso de un 403 de permisos normal, y redirigir a una pantalla de
"club bloqueado" en vez de mostrar un error genérico.

### Qué sigue funcionando aunque el club esté bloqueado

Estos endpoints **no** cortan, para que el club pueda ver y resolver la deuda:

- `POST /factura/paginate`
- `POST /factura/pago`
- `POST /user/login`, `POST /user/refresh-token`

Cualquier otro endpoint (socios, actividades, tesorería, etc.) corta con el 403 de arriba. La
pantalla de "club bloqueado" debería, como mínimo, dejar ver las facturas vencidas y el botón de
pago — el resto de la navegación puede quedar deshabilitada.

**El alta pública de socios también corta.** `POST /member/public/titular` (el auto-registro sin
JWT, con solo `X-Company-Uuid`) devuelve el **mismo 403** con `errores[0].value = "FACTURA_VENCIDA"`
si el club está bloqueado. Si el front tiene una landing pública de inscripción, tiene que
contemplar ese caso y mostrar un mensaje del tipo "el club no está recibiendo inscripciones en este
momento" en vez de un error genérico.

### Cuándo se destraba

Automático, en el momento en que el CRM confirma el pago (webhook `PAGO_IMPUTADO`) y ya no quedan
facturas `VENCIDA`. No hay que hacer nada especial del lado del front más que dejar de mostrar el
bloqueo — el próximo request exitoso ya no va a devolver 403 (o, si se quiere confirmar antes de
navegar, volver a pedir `POST /factura/paginate` y revisar que no queden `VENCIDA`).

## 4. Cupos de plan — socios y administradores

El plan del CRM define, además del precio, **cuántos socios y cuántos administradores** puede
tener el club. El backend lo hace cumplir *antes* de crear el registro — no es una validación que
el front deba replicar, pero sí hay que manejar el error.

| Recurso | Dónde se valida | Error si se supera |
|---|---|---|
| Socios (titular, familiar, aprobar solicitud, importación masiva) | Antes de crear el `Member` | `400` — `"El club alcanzó el límite de socios de su plan"` |
| Administradores (`POST /admin`) | Antes de crear el `User`/rol `ADMIN` | `400` — `"El club alcanzó el límite de administradores de su plan"` |

**Request de ejemplo — `POST /admin` (`AdminAltaCommand`), el que puede pegar contra el cupo:**

```json
{
  "nombre": "Carlos",
  "apellido": "Fernández",
  "email": "admin.adicional@ejemplo.com",
  "password": "Cambiar123",
  "telefono": "+5491112345678",
  "fechaNacimiento": "1988-07-20"
}
```

**Response 400 cuando se supera el cupo** (mismo shape para socios, solo cambia `userMessage`):

```json
{
  "localTime": "2026-08-19T10:46:00.123",
  "status": "BAD_REQUEST",
  "userMessage": "El club alcanzó el límite de administradores de su plan",
  "developerMessage": "AdminCupoService | companyId: 10 | actuales: 3 | nuevos: 1 | limite: 3",
  "errores": null
}
```

Ambos casos son **fail-closed**: si el backend no puede determinar el límite del plan (club recién
creado y todavía no sincronizado con el CRM, por ejemplo), también corta con `400` —
`"No se pudo determinar el límite de socios/administradores del plan del club"`. No hay forma de
distinguir "sin cupo" de "límite indeterminado" por código de error, solo por el mensaje — si la
UI necesita diferenciarlos (ej. para sugerir "contactar soporte" vs. "actualizar el plan"), hay
que parsear el `userMessage`.

En la importación masiva de socios (`POST /member/import`), el chequeo se hace contra el
**total de filas del archivo de una sola vez**, antes de empezar a procesar — si el archivo tiene
más filas de las que entran en el cupo, se rechaza entero (no importa nada, ni siquiera las filas
que sí entrarían).

El alta de socio **no recibe** `numeroSocio` (el backend lo asigna: `MAX` de valores solo dígitos
de **todos** los members del club, cualquier status, + 1). El cupo, en cambio, cuenta solo members
`status = ACTIVE`. No hay que mandar ni editar el número desde el front.

No hay hoy un endpoint para que el front consulte "cuántos socios/administradores le quedan al
club" de antemano — el único momento en que se entera es al intentar el alta y recibir (o no) el
400.

## 5. Catálogo de planes/productos del CRM

El catálogo de **planes** ya se puede consultar (`GET /sysadmin/clubs/crm-catalogo/planes`, §1
arriba) — pero es solo `SYSADMIN` y trae el catálogo completo del CRM, no "el plan de este club en
particular". No hay endpoint para el catálogo de **productos** del CRM, ni para que un `ADMIN` de
club consulte cuál es su propio plan o cuánto cupo le queda. Todo lo que existe hoy sobre "el plan
de este club" es interno (`plan_crm`, `productos_crm`, `CompanySuscripcionCrm`) y solo lo usan los
servicios de cupo/facturación descritos arriba. Si el front necesita mostrar "tu plan es X, con Y
socios de cupo" dentro del panel del club (no del panel sysadmin), hoy no hay forma de pedirlo —
coordinar con backend antes de diseñar esa pantalla.

## Postman

La colección (`postman/bsports-club.postman_collection.json`) tiene:

- `Facturas` (dentro del árbol de tesorería/backoffice) — `POST /factura/paginate`,
  `POST /factura/pago`.
- `Webhook CRM` — requests para simular los 4 eventos del CRM a mano en local (`DEUDA_GENERADA`,
  `PAGO_IMPUTADO`, `PLAN_CAMBIO`, `PRODUCTO_CAMBIO`). Sirven para armar datos de prueba (generar
  una factura, marcarla pagada, cargar un plan con cupo) sin depender de que el CRM esté
  levantado.
- `Administradores` (dentro de "Personal interno") — `POST /admin`, con el 400 de cupo
  documentado en la descripción del request.
- Variables relevantes: `crmApiToken`, `crmCodigoPlan`, `crmCodigoCondicionImpositiva`,
  `crmCodigoProvincia`, `facturaUuid`, `adminUserUuid`.
