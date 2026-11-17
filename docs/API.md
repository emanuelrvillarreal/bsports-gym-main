# bsports-club — Referencia de API

Documento único de la API REST del backend de clubes deportivos. Reemplaza y consolida la documentación que estaba dispersa en `postman/README.md`, `CERTIFICADOS-MEDICOS.md`, `CAMBIOS_IMPORTACION_MASIVA.md`, `docs/*/SPEC.md`, `docs/*/FRONTEND.md` y `static/openapi.md`.

**Fuente de verdad:** este documento se generó leyendo los controllers y services de `src/main/java/com/blue/open`. Donde la documentación previa contradecía al código, **manda el código** (las diferencias están marcadas con ⚠️).

**Documentos que sobreviven aparte:**

| Documento | Qué cubre |
|---|---|
| [`docs/bsport-clubes-complementario-v1.md`](bsport-clubes-complementario-v1.md) | Definición funcional del producto (reglas de negocio, decisiones de product owner, alcance MVP) |
| [`convenciones-backend.md`](../convenciones-backend.md) | Convenciones técnicas obligatorias del equipo (idioma, git, fechas, testing, Liquibase) |
| [`src/main/resources/static/openapi.yaml`](../src/main/resources/static/openapi.yaml) | Contrato OpenAPI que consume Swagger UI |
| [`postman/bsports-club.postman_collection.json`](../postman/bsports-club.postman_collection.json) | Colección ejecutable con payloads de ejemplo reales |
| [`docs/EVENTO-ACCESO-MIGRACION-FRONT.md`](EVENTO-ACCESO-MIGRACION-FRONT.md) | Guía de migración para el front — cambios en eventos de acceso: cobro unificado, historial, campos nuevos |
| [`docs/PORTAL-AMENITIES-RESERVAR-FRONT.md`](PORTAL-AMENITIES-RESERVAR-FRONT.md) | Guía de integración para el front — reserva self-service de amenities desde el portal del socio |
| [`docs/PORTAL-ACTIVIDADES-VER-INSCRIPCIONES-FRONT.md`](PORTAL-ACTIVIDADES-VER-INSCRIPCIONES-FRONT.md) | Guía de integración para el front — ver las actividades inscriptas del socio y de sus familiares, y explorar el catálogo completo de actividades desde el portal |
| [`docs/CUOTA-SOCIAL-RANGOS-FRONT.md`](CUOTA-SOCIAL-RANGOS-FRONT.md) | Guía de integración para el front — ABM de rangos de cuota social por edad (`edadHasta`) |
| [`docs/CUOTAS-GENERACION-POR-DIAS-FRONT.md`](CUOTAS-GENERACION-POR-DIAS-FRONT.md) | Guía de migración para el front — cuotas generadas cada N días: sin `mes`/`anio`, `fechaGeneracion` y generación manual "hasta una fecha" |
| [`docs/LOGIN-SIN-CONTRASENA-FRONT.md`](LOGIN-SIN-CONTRASENA-FRONT.md) | Guía de integración para el front — login sin contraseña con enlace por mail (`start-login` / `finish-login`) |

---

## Índice

1. [Convenciones generales](#1-convenciones-generales)
2. [Autenticación y usuarios](#2-autenticación-y-usuarios)
3. [Sysadmin — alta de clubes](#3-sysadmin--alta-de-clubes)
4. [Companies — perfil y branding del club](#4-companies--perfil-y-branding-del-club)
5. [Configuración](#5-configuración)
6. [Socios (members)](#6-socios-members)
7. [Certificados médicos](#7-certificados-médicos)
8. [Importación masiva de socios](#8-importación-masiva-de-socios)
9. [Personal interno — profesores y recepcionistas](#9-personal-interno--profesores-y-recepcionistas)
10. [Categorías](#10-categorías)
    - 10.1 [Rangos de cuota social](#101-rangos-de-cuota-social)
11. [Actividades e inscripciones](#11-actividades-e-inscripciones)
12. [Eventos de actividad](#12-eventos-de-actividad)
13. [Asistencia a actividades](#13-asistencia-a-actividades)
14. [Calendario](#14-calendario)
15. [Amenities y reservas](#15-amenities-y-reservas)
16. [Tesorería — cuotas](#16-tesorería--cuotas)
    - 16.1 [Descuentos](#161-descuentos)
17. [Tesorería — movimientos y balance](#17-tesorería--movimientos-y-balance)
18. [Accesos al club](#18-accesos-al-club)
19. [Eventos de acceso / entradas](#19-eventos-de-acceso--entradas)
20. [Mercado Pago](#20-mercado-pago)
21. [Portal del socio](#21-portal-del-socio)
22. [Portal del profesor](#22-portal-del-profesor)
23. [Jobs automáticos](#23-jobs-automáticos)
24. [Catálogo de enums](#24-catálogo-de-enums)
25. [Matriz de permisos por rol](#25-matriz-de-permisos-por-rol)
26. [Integración CRM (blue-crm)](#26-integración-crm-blue-crm)

---

## 1. Convenciones generales

### Base URL

| Entorno | URL |
|---|---|
| Local | `http://localhost:7036/api` |
| Staging | `https://stg.blueopendata.com/bsports-club/api` |

El context-path `/api` está configurado en `server.servlet.context-path`. Todas las rutas de este documento son relativas a esa raíz.

Swagger UI: `{baseUrl}/swagger-ui/index.html` (lee `static/openapi.yaml`, no genera desde los controllers).

### Headers

| Header | Cuándo | Descripción |
|---|---|---|
| `Authorization: Bearer <jwt>` | Casi todos | Token obtenido de `POST /user/login`. Alternativa: Basic Auth (usado por Swagger UI). |
| `X-Company-Uuid: <uuid>` | Todo endpoint con scope de club | Resuelve el club (tenant). Sin este header, cualquier endpoint que llame a `getCurrentCompanyOrThrow()` responde **400 "Company requerida"**. |
| `X-Captcha-Token` | Registro público de usuario del Core | Opcional según configuración de captcha. |

**Multi-tenant:** un usuario puede pertenecer a varias companies vía `UserCompanyRole`. El header decide en cuál opera. Si el usuario no pertenece a la company del header → **403 "Empresa no permitida"** (salvo SYSADMIN, que puede operar sobre cualquiera).

### Modelo de permisos

`AuthService.valid(List.of(...))` usa semántica **OR**: basta con tener **cualquiera** de los permisos o roles de la lista.

> ⚠️ La documentación anterior (`postman/README.md`) decía que la importación masiva requería «`member.create` **+** `family.create`». Es incorrecto: el código llama `authService.valid(List.of(MEMBER_CREATE, FAMILY_CREATE))`, que es **OR** — alcanza con uno de los dos.

Las listas pueden mezclar permisos (`PermissionEnum`) y roles (`RoleEnum`, `BaseRoleEnum`) en la misma llamada.

### Formatos de fecha

⚠️ **No hay un criterio único.** El formato depende del DTO concreto:

| Formato | Dónde |
|---|---|
| `dd/MM/yyyy` | Mayoría de las **respuestas** con `LocalDate`: `fechaAlta`, `fechaNacimiento`, `fechaCarga`, `fechaExpiracion`, `vencimiento`, `fechaInscripcion` |
| `dd/MM/yyyy HH:mm:ss` | Respuestas con `LocalDateTime`: `inicio`, `fin`, `slotInicio`, `venceSenaAt`, `anuladaAt`, `fecha` de accesos. También en los **requests** de eventos de actividad (`EventoActividadAltaCommand.inicio/fin`) y filtros de reservas |
| ISO `yyyy-MM-dd` | **Requests** que no declaran `@JsonFormat`: `fechaNacimiento`, `fechaAlta`, `fecha` de cobros/movimientos, `MovimientoPaginateRequestDTO.fechaDesde/fechaHasta`. También query params `desde` / `hasta` de accesos y `fecha` de generación de cuotas |
| ISO `yyyy-MM-ddTHH:mm:ss` | `ReservaAmenityAltaCommand.inicio/fin` |
| `dd/MM/yyyy` | `CalendarioConsultaCommand.desde/hasta` (request) |
| `MM/yyyy` | `BalanceMesDTO.periodo` (respuesta) |
| `dd/MM/yyyy - HH:mm` | `MemberBulkImportResult*.dateCreated` |
| `yyyy-MM-dd` | `CuotaVencimientoResultDTO.fechaCorte` |

**Regla práctica para el front:** las fechas **se envían en ISO** salvo que el DTO de request tenga `@JsonFormat` explícito (calendario, eventos de actividad, filtros de reserva), y **se reciben formateadas** (`dd/MM/yyyy`). Ver cada endpoint.

Zona horaria del sistema: UTC-3 (ver `convenciones-backend.md` §4).

### Paginación

Casi todos los listados usan `POST /{recurso}/paginate` con body opcional.

| Aspecto | Valor |
|---|---|
| `page` | **Base 1**. `Math.max(page, 1)` — enviar `0` o vacío equivale a la página 1 |
| `size` | Default `10`. Tope típico `100`; en eventos de acceso el tope es `20` |
| Respuesta | `Page<T>` de Spring: `content[]`, `totalElements`, `totalPages`, `number` (base 0), `size`, `first`, `last` |
| Body omitido | Permitido en la mayoría (`@RequestBody(required = false)`) → usa defaults |

> ⚠️ **Excepción:** `GET /member/import/results` usa `page` **base 0** (query param, default `0`) y devuelve un DTO propio (`MemberBulkImportResultsPageDTO`), no un `Page<T>` de Spring. Es el único listado con esa convención.

### Formato de errores

Excepciones de negocio (`CustomException`):

```json
{
  "localTime": "2026-08-19T10:46:00.123",
  "status": "BAD_REQUEST",
  "userMessage": "Mensaje para mostrar al usuario",
  "developerMessage": "Contexto técnico | detalle",
  "errores": null
}
```

Errores de validación de bean (`@Valid` fallido) devuelven **400** con un mapa `campo → mensaje`.

Códigos usados en el proyecto:

| Código | Significado habitual |
|---|---|
| 400 | Validación fallida, falta `X-Company-Uuid`, regla de negocio violada, archivo inválido |
| 401 | Sin token o token expirado |
| 403 | Sin permiso, usuario ajeno al club, socio no habilitado para el portal |
| 404 | Recurso inexistente o de otro club (se enmascara como "no encontrado") |
| 409 | Conflicto: duplicado, estado incompatible, dependencias activas |

### Baja lógica

Todas las entidades extienden `AbstractEntity` (`id`, `uuid`, `status`, auditoría). Convención de `status`:

| Valor | Significado |
|---|---|
| `0` | Activo |
| `-1` | Baja lógica |
| `-2` | Pendiente de aprobación (registro público de socios) |

Los endpoints `DELETE` casi siempre son **baja lógica idempotente** que responde `204 No Content`.

---

## 2. Autenticación y usuarios

Base: `/user`. Heredado de `BaseUserController` + `AbstractController` del Core.

### Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `POST` | `/user/login` | Pública | Login JWT. Body `{ "username": "...", "password": "..." }` → `JwtTokenDTO` (`token`, `refreshToken`) |
| `POST` | `/user/refresh-token` | Pública | Renueva el access token. Body `JwtTokenDTO` con el `refreshToken` |
| `GET` | `/user/login` | Basic | Login con Basic Auth; devuelve el `UserDTO` del usuario |
| `POST` | `/user/create-token` | JWT | Crea un access token de larga duración |
| `DELETE` | `/user/delete-token/{uuid}` | JWT | Revoca un access token |
| `GET` | `/user/tokens` | JWT | Lista los access tokens del usuario |
| `POST` | `/user/public/register` | Pública | Registro público de usuario (flujo del Core) |
| `POST` | `/user/public/register-client` | Pública | Registro de usuario cliente vinculado a una company |
| `POST` | `/user/public/link/{userUuid}` | Pública | Vincula un usuario existente a la company del header |
| `POST` | `/user/public/verify` | Pública | Verifica el código enviado por email |
| `POST` | `/user/public/resend-code` | Pública | Reenvía el código de verificación |
| `POST` | `/user/public/forgot-password` | Pública | Dispara el mail de recuperación. Body `{ "email": "...", "path": "..." }` (`path` opcional: path/slug de la company para personalizar el mail con su branding) |
| `POST` | `/user/public/change-password` | Pública | Cambia la contraseña con el token de recuperación |
| `POST` | `/user/public/start-login` | Pública | Login sin contraseña, paso 1: envía por mail un enlace de acceso de un solo uso. Body `StartLoginRequestDTO` → `StartLoginResponseDTO`. Ver [Login sin contraseña](#login-sin-contraseña) |
| `POST` | `/user/public/finish-login` | Pública | Login sin contraseña, paso 2: canjea el código del enlace por los tokens. Body `FinishLoginRequestDTO` → `JwtTokenDTO` |
| `GET` | `/user/uuid/{uuid}` | JWT | Detalle de usuario (CRUD genérico del Core) |
| `POST` | `/user/paginate` | JWT | Paginado genérico del Core (body `PaginateList`) |
| `POST` | `/user/save` | JWT | Alta/edición genérica del Core |
| `DELETE` | `/user/{uuid}` | JWT | Baja del Core |
| `POST` | `/user/photo` | JWT | Sube la foto de perfil. `multipart/form-data`, campo `file` → `UserDTO` |
| `DELETE` | `/user/photo` | JWT | Elimina la foto de perfil → `UserDTO` |

### `UserDTO`

```json
{
  "uuid": "…", "username": "…", "email": "…", "name": "…",
  "nombre": "Juan", "apellido": "Pérez", "dni": "30111222",
  "fechaNacimiento": "15/03/1990",
  "imageUrl": "https://…"
}
```

Los campos heredados de `BaseUserDTO` (uuid, username, email, name, roles, status…) vienen del Core.
Entre ellos, `userCompanyRoles[]` (`UserCompanyRoleDTO`) trae, por cada club al que pertenece el
usuario: `companyUuid`, `companyName`, `companyCuit`, `logoUrl`, `roles[]` y **`companyBlocked`**
— `true` si ese club tiene el acceso bloqueado por facturación vencida (ver
[§26](#26-integración-crm-blue-crm)). El front puede usarlo para avisar del bloqueo apenas
loguea, sin esperar a que algún otro endpoint responda 403.

### Login sin contraseña

Dos pasos, ambos públicos y disponibles solo si el protocolo `jwt` está habilitado (si no, 400 "No está disponible este metodo de login"). Lógica en `LoginSinPasswordService`; códigos en la tabla `codigo_login` (solo se guarda el hash SHA-256). No usa `security_code` ni afecta a `forgot-password`.

**`POST /user/public/start-login`** — `StartLoginRequestDTO`:

```json
{ "email": "socio@mail.com", "path": "club-ejemplo" }
```

| Campo | Regla |
|---|---|
| `email` | Obligatorio, formato email |
| `path` | Opcional. Path del club para los colores y el logo del mail; sin path o sin branding completo usa el de Blue Open Data |

Response **200** `StartLoginResponseDTO` — siempre igual, exista o no el email (no revela qué emails están registrados):

```json
{ "message": "Mail enviado correctamente" }
```

- Solo usuarios activos (`status = 0`); los pendientes de verificación se tratan como inexistentes.
- Genera un código de 32 caracteres que vence a los **15 minutos** e invalida el código anterior del usuario.
- El mail sale **asíncrono, después del commit**, con un botón a `{front.url.mail}/auth/finish-login?code={code}`. Si el envío falla solo se loguea.
- **429** si el usuario ya pidió un enlace hace menos de **5 minutos**.

**`POST /user/public/finish-login`** — `FinishLoginRequestDTO`:

```json
{ "code": "aB3dE5fG7hJ9kL1mN3pQ5rS7tU9vW1xY" }
```

Response **200** `JwtTokenDTO`, el mismo body que `POST /user/login`:

```json
{ "token": "eyJ…", "refresh": "eyJ…" }
```

- **400** "Código inválido o vencido" si el código no existe, ya se usó, venció, fue reemplazado por uno más nuevo o el usuario ya no está activo.
- El código es de un solo uso: queda consumido al canjearlo.

Guía de integración para el front: [`docs/LOGIN-SIN-CONTRASENA-FRONT.md`](LOGIN-SIN-CONTRASENA-FRONT.md).

### Roles del dominio

Definidos en `Code/Auth/RoleEnum`: `ADM_RECEPCION`, `PROFESOR`, `SOCIO_TITULAR`, `FAMILIAR`. Se suman los roles del Core (`BaseRoleEnum`): `SYSADMIN`, `ADMIN`.

Los roles se crean por club al dar de alta la company. `POST /sysadmin/clubs/sync-roles` re-sincroniza los permisos plantilla en todos los clubes activos (necesario tras agregar permisos nuevos en un deploy).

---

## 3. Sysadmin — alta de clubes

Base: `/sysadmin/clubs`. **Todos los endpoints exigen rol `SYSADMIN`.**

> No existe registro público de clubes: el alta es exclusivamente interna (decisión §2.1 del documento funcional).

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/sysadmin/clubs` | Alta de club + administrador inicial (**no lo suscribe a un plan**) → **201** |
| `POST` | `/sysadmin/clubs/sync-roles` | Re-sincroniza roles/permisos plantilla en todos los clubes activos → **204** |
| `GET` | `/sysadmin/clubs/{companyUuid}/mercado-pago` | Configuración MP del club (enmascarada) |
| `PUT` | `/sysadmin/clubs/{companyUuid}/mercado-pago` | Carga/actualiza credenciales MP del club |
| `GET` | `/sysadmin/clubs/crm-catalogo/planes` | Catálogo de planes del CRM, para completar `codigoPlan` |
| `GET` | `/sysadmin/clubs/crm-catalogo/provincias` | Catálogo de provincias del CRM, para completar `codigoProvincia` |
| `GET` | `/sysadmin/clubs/crm-catalogo/condiciones-impositivas` | Catálogo de condiciones impositivas del CRM, para completar `codigoCondicionImpositiva` |
| `POST` | `/sysadmin/clubs/{companyUuid}/crm/suscribir` | Suscribe a un plan un club ya existente (da de alta el cliente en el CRM si hace falta). **Segundo paso obligatorio del alta** |
| `POST` | `/sysadmin/clubs/{companyUuid}/crm/cambiar-plan` | Cambia el plan de un club que ya es cliente del CRM |
| `POST` | `/sysadmin/clubs/{companyUuid}/crm/desuscribir` | Da de baja al club en el CRM y le corta el acceso |

### `POST /sysadmin/clubs`

**Request — `ClubRegistrationRequestDTO`** (extiende `ClubProfileFieldsDTO`):

| Campo | Obligatorio | Notas |
|---|---|---|
| `nombreComercial`, `razonSocial` | Sí | |
| `cuit` | Sí | Validado con `@ValidCuit` (dígito verificador). Ejemplos válidos: `20000000000`, `20123456786` |
| `domicilio`, `domicilioFacturacion` | Sí | Cada uno: `{ direccion, codigoPostal, localidad }` |
| `telefono` | Sí | |
| `emailFacturaElectronica`, `emailInstitucional` | Sí | Formato email |
| `path` | Sí | Identificador de URL del club. Solo `[a-zA-Z0-9_-]+` (sin espacios). **Único** en toda la base — usado por `GET /companies/public/by-path/{path}` |
| `colorPrimario`, `colorSecundario` | Sí | Branding del club |
| `cuotaSocialHabilitada` | Sí | Booleano. El importe **ya no se define acá**: vive por rango etario en [§10.1](#101-rangos-de-cuota-social) |
| `periodicidadCuotaSocial` | No | Default `MENSUAL`. Cada cuánto se genera la cuota social: `SEMANAL`, `MENSUAL`, `BIMESTRAL`, `TRIMESTRAL`, `CUATRIMESTRAL`, `SEMESTRAL` o `ANUAL` |
| `diasVencimientoCuota` | No | Default 10. Días desde la generación hasta el vencimiento de la cuota social |
| `diasVencimientoMoroso` | No | Default 31. Días desde el vencimiento para pasar a `MOROSO` |
| `diasInhabilitacion` | No | Default 91 |
| `admin` | Sí | `{ nombre, apellido, email, password }` — el email es único en todo el sistema |
| `codigoCondicionImpositiva` | Sí | Código de condición impositiva del catálogo del CRM. El CRM valida y responde 400 si no existe |
| `codigoProvincia` | Sí | Código de provincia del catálogo del CRM. El CRM valida y responde 400 si no existe |

**No** acepta `logoUrl`: el logo se sube aparte con `POST /companies/logo`.

**Response 201 — `ClubRegistrationResponseDTO`:** `{ "company": CompanyDTO, "admin": UserDTO }`. El `company.uuid` es el valor que va después en el header `X-Company-Uuid`.

**Integración CRM en el alta:** después de crear el club y el admin, en la **misma transacción**, el backend crea el cliente en el CRM. Si el CRM lo rechaza (código de condición impositiva o provincia inválidos, CUIT mal formado, etc.), la excepción propaga y **revierte el alta completa del club** — no queda un club a medio crear. El `uuidCliente` que devuelve el CRM (tanto en `CLIENTE_CREADO` como en `CLIENTE_EXISTENTE`) se guarda en el club: es la clave con la que llegan sus webhooks. Detalle del mecanismo en [§26](#26-integración-crm-blue-crm).

Si el email del `admin` ya pertenece a otro usuario, responde **400** `Email ya registrado`.

Terminado el registro, y todavía dentro de la misma transacción, el backend le pregunta al CRM qué planes tiene ese CUIT (`GET integraciones/cliente/planes`). **Si ya tenía uno**, lo espeja en `company_suscripcion_crm` y el club arranca con los cupos de ese plan, sin necesidad del segundo request. Es el caso de un CUIT que ya era cliente del CRM (el alta responde `CLIENTE_EXISTENTE`). Si tiene varios, se toma el primero: esta app admite un único plan por club.

Con esa suscripción en mano decide además si el club puede operar:

| Caso | Acceso |
|---|---|
| El CRM no le conoce ninguna suscripción | **Bloqueado** |
| `estadoSuscripcion = ANULADA` | **Bloqueado** |
| `estadoSuscripcion = PENDIENTE_PAGO` y `fechaInicio` hace **más de 10 días** | **Bloqueado** — se pasó la ventana para pagar la primera factura |
| `estadoSuscripcion = PENDIENTE_PAGO` dentro de esos 10 días, o sin `fechaInicio` | Habilitado |
| Cualquier otro estado (`ACTIVA`, `BORRADOR`, o uno nuevo del CRM) | Habilitado |

La suscripción se espeja en `company_suscripcion_crm` en todos los casos en que existe, incluso si deja al club bloqueado: el CRM es la fuente de verdad y de esa fila salen después los cupos. Los estados desconocidos habilitan a propósito — el catálogo del CRM es abierto y un estado nuevo no debería dejar al club afuera. El job diario de vigencia vuelve a evaluar todo esto (con sus propias reglas, ver [§26](#26-integración-crm-blue-crm)).

> ⚠️ **Salvo ese caso, el alta no suscribe al club a ningún plan.** Son **dos requests**: este y después `POST /sysadmin/clubs/{companyUuid}/crm/suscribir` (ver más abajo), con el `companyUuid` que devuelve el 201. El `codigoPlan` va en ese segundo request, no acá.

> ⚠️ **Sin una suscripción que lo habilite, el club nace con el acceso bloqueado** (`CompanyBloqueoAcceso`): sus usuarios reciben **403** en todo request hasta que se lo suscriba — el segundo request lo desbloquea en el momento. Es deliberado: sin plan vigente no tiene cupos y el chequeo es fail-closed, así que tampoco podría dar de alta socios. El `SYSADMIN` sí puede seguir operando sobre ese club (ver [Bloqueo de acceso](#bloqueo-de-acceso-por-facturación-vencida)).

> **409** si el club ya tenía un plan en el CRM pero ese plan todavía no está sincronizado en el catálogo local (`plan_crm`): *"El plan del club no está sincronizado en el catálogo local"*. Revierte el alta. Se destraba con `POST /sysadmin/jobs/catalogo-sync` y reintentando.

### `GET /sysadmin/clubs/crm-catalogo/planes`

Catálogo en vivo del CRM, para completar `codigoPlan` de
`POST /{companyUuid}/crm/suscribir`/`cambiar-plan` (ver más abajo). Sin body, sin params.

**Response — `List<CrmPlanResponseDTO>`:**

```json
[{
  "nombre": "Plan Estándar",
  "descripcion": "...",
  "codigo": "plan-estandar",
  "precio": 25000.00,
  "precioEsFinal": true,
  "alicuotaIva": 21.00,
  "periodicidad": { "codigo": "mensual" },
  "productos": [{ "codigo": "bsports-socios", "nombre": "Socios", "unidades": 200 }]
}]
```

El campo `codigo` de cada ítem es el valor que va en `codigoPlan`. A diferencia de
`/crm-catalogo/provincias`/`condiciones-impositivas`, este llama a
`integraciones/obtener-planes` del CRM — el mismo endpoint que usa
`CrmCatalogoSyncService`/`obtenerPlanes()` — así que no depende del permiso adicional que
exige el módulo de clientes; ojo que **el CRM lo cachea y la cache no distingue empresa**.

### `GET /sysadmin/clubs/crm-catalogo/provincias` y `/crm-catalogo/condiciones-impositivas`

Catálogo en vivo del CRM, para completar `codigoProvincia`/`codigoCondicionImpositiva` del alta de arriba sin tener que pedirlos a mano al equipo de CRM. Sin body, sin params — piden el catálogo completo cada vez (no hay caché local, son listas chicas de uso esporádico).

**Response `GET /crm-catalogo/provincias` — `List<CrmProvinciaResponseDTO>`:**

```json
[{ "uuid": "...", "nombre": "Buenos Aires", "codigo": "BA", "pais": "Argentina", "paisUuid": "..." }]
```

**Response `GET /crm-catalogo/condiciones-impositivas` — `List<CrmCondicionImpositivaResponseDTO>`:**

```json
[{ "uuid": "...", "nombre": "Responsable Inscripto", "codigo": "RI" }]
```

El campo `codigo` de cada ítem es el valor que va en `codigoProvincia`/`codigoCondicionImpositiva`.

**A diferencia del resto de los métodos de `CrmIntegracionService`**, estos dos no llaman a `integraciones/**` del CRM sino a `clientes/provincias/obtener` y `clientes/condicion-impositiva/obtener` — endpoints del módulo de clientes del CRM, protegidos con los permisos `CLIENTES_PROVINCIAS_VER`/`CLIENTES_CONDICIONES_IMPOSITIVAS_VER` en vez de `INTEGRACIONES_EMPRESAS_EXTERNAS`. Las autoridades del token de sistema externo salen del rol asignado al usuario de integración en el CRM: si ese rol no tiene esos dos permisos otorgados, el CRM responde 403 y `bsports-club` lo traduce en **502** (mismo comportamiento que cualquier otra falla del CRM) — es un paso de configuración a coordinar con quien administra el CRM, no algo que se resuelva desde este repo.

### Gestión de la suscripción de un club — `/sysadmin/clubs/{companyUuid}/crm/*`

Los tres flujos resuelven el club por `companyUuid` (404 si no existe o está dado de baja), sacan el CUIT de la `Company` (400 si no lo tiene cargado) y terminan actualizando `company_suscripcion_crm`, que es de donde salen los cupos de socios y administradores.

> **No son transaccionales de punta a punta**: cada paso es una llamada HTTP al CRM y una transacción local no puede revertirlas. El orden está elegido para que, si algo se corta a la mitad, el estado local nunca quede diciendo que el club tiene un plan que en el CRM no tiene.

#### `POST /{companyUuid}/crm/suscribir`

Para un club que todavía no está operando con el CRM — entre otros, **el que acaba de salir de
`POST /sysadmin/clubs`**: el alta no suscribe, así que este es el segundo request del flujo. Secuencia: **crear cliente** → **obtener suscripciones** → **desuscribir de las vigentes** (si tiene) → **suscribir al plan** → persistir.

**Request:** `codigoPlan`, `codigoCondicionImpositiva`, `codigoProvincia` (los tres obligatorios). Los dos códigos de catálogo viajan en el body porque `Company` **no los guarda** — solo se usan para dar de alta el cliente en el CRM, que es justo lo que hace este flujo. Salen de los endpoints de `/crm-catalogo/*` de arriba.

Si todo sale bien y el club estaba bloqueado, **le devuelve el acceso en el momento**. Ojo: desbloquea por el solo hecho de haberse suscripto, sin mirar si tiene facturas `VENCIDA` — un club bloqueado por deuda recupera el acceso al suscribirlo, hasta que el job diario lo vuelva a bloquear.

Si el CUIT ya existía en el CRM no es error (responde `CLIENTE_EXISTENTE` y sigue). Al desuscribir se **filtran las suscripciones ya `ANULADA`**: el CRM responde 404 si ninguno de los códigos informados tiene una suscripción activa, y eso cortaría el alta de un club que solo tenía bajas viejas.

#### `POST /{companyUuid}/crm/cambiar-plan`

**Request:** `codigoPlan`. Cancela **todas** las suscripciones del club (`cancelar-suscripciones`, que además **compensa la deuda pendiente** de esos planes en el CRM: por cada factura pendiente genera un movimiento de compensación y la cancela) y lo suscribe al plan nuevo. No vuelve a crear el cliente: para cambiar de plan el club ya tiene que existir del otro lado.

#### `POST /{companyUuid}/crm/desuscribir`

Sin body. Cancela todas las suscripciones y compensa la deuda (mismo endpoint del CRM que el cambio de plan), marca la fila local con `estado = ANULADA` y **corta el acceso del club en el momento** — sin esto le quedaría acceso libre hasta que corra el job de vigencia a las 03:30.

**Response — `CrmCancelarSuscripcionesResponseDTO`** (passthrough del CRM): `cuit`, `suscripcionesAnuladas`, `facturasCompensadas`, `montoCompensado`, `mensaje`.

**Response de suscribir/cambiar-plan — `SysadminSuscripcionResponseDTO`:** `companyUuid`, `codigoPlan`, `nombrePlan`, `uuidSuscripcionCrm`, `estado`, `fechaProximaFacturacion` (`dd/MM/yyyy HH:mm`).

Los tres devuelven **409** si el plan resultante todavía no está sincronizado en `plan_crm` (*"El plan del club no está sincronizado en el catálogo local"* — se destraba con `POST /sysadmin/jobs/catalogo-sync` y reintentando), y **502** ante cualquier rechazo del CRM.

### `PUT /sysadmin/clubs/{companyUuid}/mercado-pago`

**Request — `SysadminMercadoPagoConfigRequestDTO`:** `accessToken`, `publicKey`, `webhookSecret` (todos ≤512 chars), `habilitado` (boolean).

**Response — `SysadminMercadoPagoConfigResponseDTO`:** `companyUuid`, `habilitado`, `operativo`, `webhookOperativo`, `accessTokenConfigured`, `accessTokenHint` (`****xxxx`), `publicKey`, `webhookSecretConfigured`, `webhookSecretHint`.

Las credenciales se guardan cifradas (migración `v33-company-mercado-pago-credentials-encryption.sql`). El GET nunca devuelve el token completo.

---

## 4. Companies — perfil y branding del club

Base: `/companies`.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/companies/context` | Pública | Modo mono/multi empresa y company actual si hay sesión → `CompanyContextDTO` |
| `GET` | `/companies/public/by-path/{path}` | Pública | Busca un club por su `path` → `CompanyPublicDTO`. **404** si no existe |
| `GET` | `/companies/current` | `ADMIN` o `SYSADMIN` | Datos completos del club actual (header `X-Company-Uuid`) → `CompanyDTO` |
| `POST` | `/companies/paginate-club` | `SYSADMIN` | Busca clubes por CUIT, razón social o nombre comercial |
| `PUT` | `/companies/profile` | `ADMIN` o `SYSADMIN` | Actualiza el perfil institucional del club actual |
| `POST` | `/companies/logo` | `ADMIN` o `SYSADMIN` | Sube el logo. `multipart/form-data`, campo `file` |
| `DELETE` | `/companies/logo` | `ADMIN` o `SYSADMIN` | Elimina el logo |
| `GET` | `/companies/uuid/{uuid}` | JWT | CRUD genérico del Core |
| `POST` | `/companies/paginate` | JWT | CRUD genérico del Core (body `PaginateList`) |
| `POST` | `/companies/save` | JWT | CRUD genérico del Core |
| `DELETE` | `/companies/{uuid}` | JWT | CRUD genérico del Core |

`PUT /companies/profile` recibe `ClubProfileUpdateRequestDTO` — el mismo perfil que el alta sysadmin, **sin** el objeto `admin` y sin logo. Incluye `path` (obligatorio, mismo formato y unicidad que en el alta).

### `GET /companies/public/by-path/{path}`

Endpoint público (sin JWT, sin `X-Company-Uuid`). Busca el club activo por `path` — pensado para resolver el club desde una URL amigable (ej. landing público del club). **404 "Club no encontrado"** si no existe ningún club con ese `path`. Devuelve solo campos públicos (`CompanyPublicDTO`), no el `CompanyDTO` completo.

### `GET /companies/current`

Equivalente privado del anterior: en vez de resolver por `path`, toma el club del header `X-Company-Uuid` y devuelve el `CompanyDTO` completo. Solo `ADMIN` o `SYSADMIN`.

### `POST /companies/paginate-club`

**Request — `CompanyPaginateRequestDTO`:** `page`, `size`, `search` (opcional, `LIKE` sobre `cuit`, `razonSocial` y `nombreComercial`). `body: null`/`{}` también sirve.

**Response — `Page<CompanyDTO>`**, con 4 campos que **solo este endpoint completa** (ver nota más abajo):

```json
{
  "suscripcionCrm": {
    "codigoPlan": "plan-estandar",
    "nombrePlan": "Plan Estándar",
    "uuidSuscripcionCrm": "f1a2b3c4-...",
    "estado": "ACTIVA",
    "fechaProximaFacturacion": "15/07/2026"
  },
  "ultimaFacturaEstado": "PENDIENTE",
  "ultimaFacturaFechaGeneracion": "01/07/2026",
  "ultimaFacturaFechaVencimiento": "11/07/2026"
}
```

- `suscripcionCrm` sale de `CompanySuscripcionCrm` — los mismos campos que expone `SysadminSuscripcionResponseDTO` (ver [§26](#26-integración-crm-blue-crm)), **sin `uuidFacturaInicialCrm`**: es un dato de auditoría del alta, no aporta nada a un listado. `null` si el club nunca se suscribió a un plan.
- `ultimaFacturaEstado`/`ultimaFacturaFechaGeneracion`/`ultimaFacturaFechaVencimiento` son de la `Factura` más reciente del club (por `fechaGeneracion`). Los tres quedan `null` si el club no tiene ninguna factura todavía.
- Ambos se resuelven en lote contra `company_suscripcion_crm`/`facturas` (2 queries extra por página, no una por club), así que no degradan con el tamaño de página.

### `CompanyPublicDTO` — campos públicos del club

Subconjunto de campos expuesto por `GET /companies/public/by-path/{path}`: `uuid`, `name`, `razonSocial`, `nombreComercial`, `logoUrl`, `colorPrimario`, `colorSecundario`.

### `CompanyDTO` — campos propios del club

Además de los del Core (incluye `path`, heredado de `BaseCompanyDTO`): `cuit`, `nombreComercial`, `razonSocial`, `domicilio`, `domicilioFacturacion`, `telefono`, `emailFacturaElectronica`, `emailInstitucional`, `logoUrl`, `colorPrimario`, `colorSecundario`, `cuotaSocialHabilitada`, `periodicidadCuotaSocial`, `diasVencimientoCuota`, `diasVencimientoMoroso`, `diasInhabilitacion`, `adminCreado`.

> **`suscripcionCrm`, `ultimaFacturaEstado`, `ultimaFacturaFechaGeneracion`, `ultimaFacturaFechaVencimiento`** también son parte del DTO, pero **solo vienen completos en `POST /companies/paginate-club`** (ver arriba) — el resto de los endpoints que devuelven `CompanyDTO` (`GET /companies/current`, `PUT /companies/profile`, alta de club, etc.) no los resuelve y quedan `null`.

> El importe de la cuota social **no** es un campo de `Company`: se define por rango etario (`CuotaSocialRango`, [§10.1](#101-rangos-de-cuota-social)) y cada socio toma el importe del rango que tiene asignado (`Member.cuotaSocialRango`).

Los tres parámetros de días controlan el ciclo de cuotas (ver [§16](#16-tesorería--cuotas)).

---

## 5. Configuración

Base: `/configuracion`. Sistema de variables clave/valor para cambiar parámetros sin recompilar (colores de marca, textos, flags).

**Dos ejes independientes:**

- `global` (boolean): `true` = variable **pública**, legible sin autenticación. `false` = **privada**, requiere `configuration.view`.
- `companyUuid` (query param, opcional): asocia la variable a un club. Permite tener `theme.colorPrimario` con distinto valor por club. **No** confundir con el header `X-Company-Uuid` — acá es un filtro explícito, justamente porque estos endpoints se llaman sin sesión.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/configuracion/all?companyUuid=` | `configuration.view` (opcional) | Con permiso: todas. Sin permiso o sin JWT: solo las públicas del sistema |
| `GET` | `/configuracion/get/{key}?companyUuid=` | Ninguno si `global=true` | Una variable por clave |
| `POST` | `/configuracion/get/list?companyUuid=` | Ninguno para públicas | Varias claves de una. Body: `["clave1", "clave2"]` |
| `POST` | `/configuracion/save?companyUuid=` | `configuration.create` | Alta/edición en lote. Body: `ConfigurationDTO[]` |

### `ConfigurationDTO`

```json
{ "name": "theme.colorPrimario", "value": "#1A73E8", "global": true, "companyUuid": "8e2f5c3a-…" }
```

**Comportamientos a tener en cuenta:**

- Si la clave no existe, `GET /get/{key}` responde **200 con `value: null`** (no 404). El front siempre debe manejar `null` y aplicar un default propio.
- `GET /get/{key}` sobre una variable privada sin permiso → error `"La variable es privada"`.
- `POST /get/list` con alguna clave privada y caller no-ADMIN → error `"Las variables X, Y son privadas"`.
- En `POST /save`, el `companyUuid` puede venir como query param (aplica a todo el body) o dentro de cada item.

**Caso de uso típico — colores del club sin login:** el admin guarda `theme.colorPrimario` / `theme.colorSecundario` con `global: true` y `companyUuid={clubUuid}`; el landing público las lee con `POST /configuracion/get/list?companyUuid={clubUuid}`. Lo que habilita el acceso sin autenticación es `global: true`; `companyUuid` solo decide **de qué club** es el valor.

---

## 6. Socios (members)

Base: `/member`.

### Modelo

Un `Member` vincula un `User` (persona física) con una `Company` (club). El **titular** tiene `titular_id` nulo; los **familiares** apuntan al titular (cabeza del grupo familiar). Un usuario puede ser socio de varios clubes.

**`numeroSocio`:** lo genera el backend (`MAX` de valores **solo dígitos** de todos los `members` del club, **cualquier status**, + 1). El club **no** lo envía en alta ni importación; **sí** puede modificarlo después vía `PUT /member/uuid/{memberUuid}` (ver más abajo). En registro público / familiar portal queda `null` hasta `POST .../aprobar-registro`. Al rechazar sigue `null`.

**Estados (`MemberEstadoEnum`):**

| Estado | Cómo se llega | Consecuencias |
|---|---|---|
| `ACTIVO` | Alta o regularización | Opera normalmente |
| `MOROSO` | Automático: cuota social vencida en el grupo | **Informativo** — sin bloqueo de portal ni de inscripciones |
| `INHABILITADO` | Automático: cuota social impaga que superó `diasInhabilitacion` | Bloquea portal, detiene generación de cuotas, da de baja inscripciones. Afecta a **todo el grupo familiar** |
| `SUSPENDIDO` | Manual | Sin portal; conserva inscripciones, no toma nuevas |
| `INACTIVO` | Baja lógica manual | Sin portal, sin cuotas, sin inscripciones |

> ⚠️ El documento funcional §3.2 define morosidad por **cantidad** de cuotas (2 → moroso, 4 → inhabilitado). El código implementa el modelo **rolling por días** del SPEC de cuotas: moroso a los `diasVencimientoMoroso` (default 31) desde el vencimiento de la cuota impaga más antigua del grupo, inhabilitado a los `diasInhabilitacion` (default 91). La cuota social vence a los `diasVencimientoCuota` (default 10) desde su generación. Manda el modelo por días.

### Endpoints — backoffice

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/member/paginate` | `member.view` | Listado paginado de socios y familiares |
| `POST` | `/member/titular` | `member.create` | Alta de socio titular |
| `POST` | `/member/familiar` | `family.create` | Alta de familiar vinculado a un titular |
| `PUT` | `/member/uuid/{memberUuid}` | `member.edit` (titular) / `family.edit` (familiar) | Edición de datos |
| `GET` | `/member/uuid/{memberUuid}/familiares` | `member.view` | Familiares de un titular |
| `POST` | `/member/uuid/{memberUuid}/baja` | `member.edit` / `family.edit` | Baja lógica → `INACTIVO`. Idempotente |
| `PUT` | `/member/uuid/{memberUuid}/estado` | `member.change_status` | Cambio manual de estado |
| `POST` | `/member/solicitudes/paginate` | `member.view` | Solicitudes de registro público pendientes |
| `POST` | `/member/uuid/{memberUuid}/aprobar-registro` | `member.create` | Aprueba una solicitud → asigna `numeroSocio`, socio activo + primera cuota. Body opcional `{ "descuentos": [...] }`, ver [§16.1](#161-descuentos) |
| `POST` | `/member/uuid/{memberUuid}/rechazar-registro` | `member.create` | Rechaza una solicitud. Body opcional `{ "motivo": "…" }` (≤512) |

### Cupo de socios según el plan del CRM

Antes de crear un socio nuevo — `POST /member/titular`, `POST /member/familiar`,
`POST /member/uuid/{memberUuid}/aprobar-registro` y la importación masiva ([§8](#8-importación-masiva-de-socios), contra el total de filas del archivo) —
`MemberCupoService` valida que `socios actuales + nuevos` no supere el límite del producto
`"bsports-socios"` dentro del plan CRM del club (`plan.productos[]`, sincronizado en `plan_crm`,
ver [§26](#26-integración-crm-blue-crm)).

- **Fail-closed:** si no se puede determinar el límite (club sin `CompanySuscripcionCrm`, plan
  sin sincronizar, o sin el producto `"bsports-socios"`), **corta** en vez de dejar pasar — es un
  límite de capacidad paga, no un dato informativo.
- **400** `"El club alcanzó el límite de socios de su plan"` si se supera.
- **400** `"No se pudo determinar el límite de socios del plan del club"` si el límite es
  indeterminable.
- Cuenta como "socio actual" cualquier `Member` con `status = ACTIVE` (dado de baja no cuenta;
  `MOROSO`/`INHABILITADO`/etc. sí ocupan lugar). Las solicitudes `PENDING_APPROVAL` no cuentan
  hasta que se aprueban.

### Endpoints — registro público y portal

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `POST` | `/member/public/titular` | **Ninguna** (solo `X-Company-Uuid`) | Auto-registro de socio titular → queda pendiente (`status -2`). **403** si el club está bloqueado (ver [§26](#26-integración-crm-blue-crm)) |
| `POST` | `/member/portal/familiar` | `portal.family.create` (titular) | El titular da de alta un familiar desde el portal → pendiente |

### `POST /member/titular` — `MemberTitularAltaCommand`

Extiende `MemberAltaUserFields`:

| Campo | Obligatorio | Validación |
|---|---|---|
| `nombre` | Sí | `@NotBlank` |
| `apellido` | Sí | `@NotBlank` |
| `dni` | No | 7 a 11 dígitos numéricos |
| `fechaNacimiento` | No | ISO `yyyy-MM-dd`, debe ser pasada |
| `sexo` | No (opcional en todas las altas) | `MASCULINO` | `FEMENINO` | `OTRO`. Ver [SEXO-FRONT.md](SEXO-FRONT.md) |
| `telefono` | No | |
| `email` | Sí | Formato email, único en el sistema |
| `password` | Sí | |
| `fechaAlta` | No | ISO. Default: hoy |
| `descuentos[]` | No | Asigna descuentos recurrentes de cuota social al socio en el mismo alta. Ver [§16.1](#161-descuentos) |

`numeroSocio` **no se envía**: el backend lo autogenera (`MAX` de valores solo dígitos de todos los members del club, cualquier status, + 1).

**Response — `MemberDTO`:**

```json
{
  "memberUuid": "…", "userUuid": "…",
  "titular": true, "tieneFamiliares": false, "titularMemberUuid": null,
  "parentesco": null, "numeroSocio": "500",
  "estado": "ACTIVO", "fechaAlta": "01/06/2026",
  "cuotaSocialRango": { "uuid": "…", "nombre": "Adultos" },
  "nombre": "Carlos", "apellido": "Gómez", "dni": "20111222",
  "fechaNacimiento": "12/04/1985",
  "sexo": "MASCULINO",
  "telefono": "+5491112345678", "email": "carlos@ejemplo.com",
  "imageUrl": null,
  "certificados": null
}
```

> `certificados` viene **`null`** en todos los endpoints salvo `GET /portal/member/perfil` y `GET /portal/member/grupo`. No se carga en el resto por costo de performance — usar `GET /member/uuid/{memberUuid}/certificado`.

> `cuotaSocialRango` es un DTO reducido (`MemberCuotaSocialRangoDTO`: solo `uuid` y `nombre`) — es **`null`** si el socio no tiene rango asignado. Ver [§10.1](#101-rangos-de-cuota-social).

### `POST /member/familiar` — `MemberFamiliarAltaCommand`

Mismos campos + `titularMemberUuid` (obligatorio) y `parentesco` (obligatorio, `ParentescoEnum`). También acepta `descuentos[]` (opcional), igual que en el alta de titular — ver [§16.1](#161-descuentos). El `numeroSocio` se autogenera igual que en el alta de titular.

### `PUT /member/uuid/{memberUuid}` — `MemberBackofficeUpdateCommand`

Todos los campos opcionales; los `null` **no modifican** el valor existente.

`nombre`, `apellido`, `dni`, `fechaNacimiento` (ISO, pasada), `sexo` (`MASCULINO` | `FEMENINO` | `OTRO`), `telefono`, `email`, `parentesco` (solo familiar), `numeroSocio`.

**`numeroSocio`:** se envía como **número entero** (`"numeroSocio": 1250`), mayor a 0 (**400** si no). En la respuesta sigue viniendo como string (`"1250"`). Si otro socio **activo** (`status = 0`) del mismo club ya tiene ese número → **409** `"El número de socio ya está en uso"`. Enviar el número que el socio ya tiene no da error. Los socios dados de baja o pendientes no se consideran en la validación, pero la base mantiene una clave única `(company_id, numero_socio)` sin distinguir status: reasignar el número de uno de ellos falla al guardar (error distinto del 409). La autogeneración de altas posteriores sigue siendo `MAX + 1`, así que asignar un número alto a mano corre la secuencia. Guía de integración: [`MEMBER-NUMERO-SOCIO-EDICION-FRONT.md`](MEMBER-NUMERO-SOCIO-EDICION-FRONT.md).

No cambia `estado` ni `imageUrl`.

### `POST /member/public/titular` — `MemberPublicRegistroCommand`

Sin JWT. Solo `X-Company-Uuid`.

`nombre`, `apellido`, `email` obligatorios. `password` obligatorio **solo si el email no existe** en el sistema. `dni`, `telefono`, `fechaNacimiento`, `sexo` opcionales. Sin `numeroSocio` ni `fechaAlta`.

Crea `User` y `Member` con `status = -2` (pendiente), `numeroSocio = null`, y notifica al club por mail. El número real se asigna al aprobar. La cuota social se genera recién al aprobar.

**409** si ese usuario ya tiene una solicitud pendiente o ya es socio activo del club.

### `POST /member/portal/familiar` — `MemberPortalFamiliarAltaCommand`

El titular sale de la sesión: **no** se envía `titularMemberUuid`. `parentesco` obligatorio. Si el email es nuevo, `password` obligatorio.

Crea el `Member` familiar con `status = -2` y `numeroSocio = null`. El número se asigna al aprobar.

**403** si el usuario logueado es familiar (no titular). **409** si ya hay solicitud pendiente o member activo.

### `POST /member/paginate` — `MemberPaginateRequestDTO`

```json
{ "page": 1, "size": 10, "search": "gómez", "estados": ["ACTIVO", "MOROSO"], "titular": true }
```

- `search`: nombre, apellido o DNI.
- `estados[]`: opcional.
- `titular`: `true` = solo titulares; `false` u omitido = titulares y familiares.

### `MemberSolicitudRegistroDTO` (solicitudes pendientes)

`memberUuid`, `tipoRegistro` (`SOCIO_TITULAR` | `FAMILIAR`), `titularMemberUuid`, `titularNombre`, `parentesco`, `numeroSocio` (`null` mientras está pendiente), `nombre`, `apellido`, `dni`, `fechaNacimiento` (`dd/MM/yyyy`), `sexo`, `telefono`, `email`, `fechaSolicitud` (`dd/MM/yyyy HH:mm:ss`).

---

## 7. Certificados médicos

Un `CertificadoMedico` pertenece a un `User` y se expone siempre en el contexto de un `Member`. Un socio puede acumular **varios** a lo largo del tiempo (no se pisan).

### Estados

| Estado | Cómo se llega | Notas |
|---|---|---|
| `PENDIENTE_VALIDACION` | El socio lo sube desde el portal | Espera confirmación de recepción |
| `CONFIRMADO` | Recepción lo sube directamente, **o** confirma uno pendiente | **Único estado que cuenta como vigente** |
| `VENCIDO` | Job diario, cuando `fechaExpiracion` quedó en el pasado | **No se puede setear por request.** El job además **borra el archivo del storage** — no confiar en `urlArchivo` una vez `VENCIDO` |

### Endpoints

| Método | Ruta | Permiso | Estado resultante |
|---|---|---|---|
| `POST` | `/member/uuid/{memberUuid}/certificado` | `member.certificado.upload` | `CONFIRMADO` |
| `POST` | `/portal/member/uuid/{memberUuid}/certificado` | `portal.certificado.upload` | `PENDIENTE_VALIDACION` |
| `PUT` | `/member/certificado/uuid/{certificadoUuid}/estado` | `member.certificado.change_status` | El que se envíe |
| `PUT` | `/member/certificado/uuid/{certificadoUuid}` | Rol `ADMIN` o `ADM_RECEPCION` | — (no cambia) |
| `GET` | `/member/uuid/{memberUuid}/certificado` | `member.view` | — (listado) |

**Subida:** `multipart/form-data`, campo `file`. PNG, JPEG, JPG o PDF, **máximo 2 MB**. 400 si no cumple.

**Portal:** `memberUuid` puede ser el propio socio **o** cualquier familiar de su mismo grupo. **403** si pertenece a otro grupo, o si el socio no está habilitado para el portal (`INHABILITADO` / `SUSPENDIDO` / `INACTIVO`).

**Cambio de estado:** body `{ "estado": "CONFIRMADO" }`. Solo acepta `PENDIENTE_VALIDACION` o `CONFIRMADO`. **404** si el certificado no pertenece a un socio del club del header.

**Edición de vencimiento:** body `{ "fechaExpiracion": "31/03/2027" }` (`dd/MM/yyyy`, obligatorio). Es lo **único** editable: archivo, estado y `fechaCarga` no cambian. Autorización **por rol** (`ADMIN` o `ADM_RECEPCION`), no por permiso; `SYSADMIN` no está habilitado. Sin validación de rango: acepta fechas anteriores a `fechaCarga` o pasadas — si queda en el pasado y el certificado está `CONFIRMADO`, el job diario lo pasa a `VENCIDO` y borra el archivo. Extender la fecha de uno `VENCIDO` **no** lo reactiva. **400** si falta la fecha o el formato es inválido, **404** si el certificado no pertenece a un socio del club del header. Responde `CertificadoMedicoDTO`. Guía de integración: [`CERTIFICADO-MEDICO-EDICION-VENCIMIENTO-FRONT.md`](CERTIFICADO-MEDICO-EDICION-VENCIMIENTO-FRONT.md).

**Listado:** devuelve **todos** los certificados del socio en cualquier estado (incluye `VENCIDO`), sin paginar. Puede ser `[]`.

### `CertificadoMedicoDTO`

| Campo | Tipo | Notas |
|---|---|---|
| `certificadoUuid` | string | |
| `memberUuid` | string | Socio dueño |
| `fechaCarga` | `dd/MM/yyyy` | Fecha de subida |
| `fechaExpiracion` | `dd/MM/yyyy` | Al subir se **calcula** = 31/12 del año de `fechaCarga` (no se envía en la subida). Editable luego con `PUT /member/certificado/uuid/{certificadoUuid}` |
| `nombreArchivo` | string | Nombre original |
| `urlArchivo` | string | URL pública (servida por SM3) |
| `estado` | enum | |

### Dónde aparece dentro de `MemberDTO`

| Endpoint | `certificados` |
|---|---|
| `GET /portal/member/perfil` | Certificados propios del socio logueado |
| `GET /portal/member/grupo` | Cada integrante trae los suyos en `integrantes[].member.certificados` |
| `GET /portal/member/credencial` | **`null`** — es el endpoint de QR, no de perfil |
| Cualquier otro (`/member/paginate`, alta, update…) | **`null`** |

---

## 8. Importación masiva de socios

Flujo asíncrono: el POST devuelve **202** casi al instante y el procesamiento sigue en segundo plano. El avance se sigue por SSE; el resultado detallado queda persistido en BD.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/member/import` | `member.create` **o** `family.create` | Dispara la importación → **202** |
| `GET` | `/member/import/{importId}/progress` | **Ninguno** | Stream SSE de avance |
| `GET` | `/member/import/results` | `member.create` **o** `family.create` | Historial paginado de importaciones |
| `GET` | `/member/import/{importId}/result` | `member.create` **o** `family.create` | Detalle completo con todas las filas |

### `POST /member/import`

`multipart/form-data`, campo `file` (`.xls` o `.xlsx`). Máximo **2000 filas de datos**.

La validación de permisos y el **parseo del Excel son síncronos**: si el archivo está mal formado, faltan columnas obligatorias o tiene demasiadas filas, la respuesta es **400 en el momento** — no hace falta abrir el SSE para enterarse.

**Formato del Excel:** columnas en español (`Nombre`, `Apellido`, `Email`, `Password`, `Fecha de nacimiento`, `Parentesco`, `Titular DNI`…). Fechas en `dd/MM/yyyy` como texto. `Titular DNI` acepta DNI **o** email del titular. El `numeroSocio` **no se lee del archivo**: se autogenera al crear cada fila (mismo criterio que el alta de titular). Plantilla de ejemplo: `postman/samples/member-import-ejemplo.xlsx`.

**Response 202:**

```json
{ "message": "Procesando", "importId": "3f1a9c2e-8b7d-4e6a-9c1a-2d4f5b6a7c8d", "filasTotales": 1000 }
```

### `GET /member/import/{importId}/progress` — SSE

**Sin autenticación.** No puede tenerla: la API nativa `EventSource` del navegador no permite headers custom. La única protección es que el `importId` es un UUID aleatorio e impredecible.

`Content-Type: text/event-stream`.

| Evento | Cuándo | ¿Cierra el stream? |
|---|---|---|
| `progress` | Al conectarse (estado actual) y tras cada fila procesada | No |
| `completed` | Terminó de recorrer todas las filas | **Sí** |
| `error` | La importación abortó por una excepción no controlada | **Sí** |

**Payload — `MemberBulkImportProgressDTO`:**

```json
{ "filasProcesadas": 340, "filasTotales": 1000, "porcentajeCompletado": 34, "errorCount": 2, "warningCount": 5 }
```

En `error` se suma `errorMessage`.

**Puntos críticos para el front:**

1. **`completed` ≠ «sin errores».** La importación es **parcial por diseño**: cada fila puede fallar sola (DNI inválido, email duplicado) sin frenar el resto. `completed` solo dice que terminó de recorrer el Excel. Para el detalle fila por fila, usar `GET /member/import/{importId}/result`.
2. El SSE **ya no envía el array `errorRows`** — solo contadores. Antes lo enviaba y el payload crecía sin control.
3. **Usar `EventSource` nativo**, no `fetch`/`axios`. Si el framework no lo soporta (SSR, React Native), usar `@microsoft/fetch-event-source`.
4. **Evento `error` de doble sentido:** `EventSource` dispara su propio evento nativo `error` ante fallas de red, con payload distinto del evento de negocio. Chequear si `e.data` existe y parsea como JSON antes de asumir que es el de negocio.
5. **Retención: 15 minutos** tras terminar. Después, reconectar con ese `importId` da **404**.
6. **Reconexión / F5:** si se guardó el `importId`, se puede reabrir el `EventSource` sin re-subir el archivo — el primer evento da el estado actual.
7. **Timeout:** el servidor mantiene el stream hasta 30 minutos por conexión. Con un proxy delante (nginx), verificar `proxy_read_timeout` para `text/event-stream`.
8. Cerrar con `es.close()` en `completed` / `error` y al desmontar el componente.

### `GET /member/import/results?page=0&size=10`

> ⚠️ **`page` es base 0 acá** (default `0`), a diferencia del resto de la API.

**Response — `MemberBulkImportResultsPageDTO`:** `content[]` (`MemberBulkImportResultItemDTO`), `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`.

Cada ítem: `importId`, `totalRows`, `successCount`, `errorCount`, `warningCount`, `importStatus` (`PROCESSING` | `COMPLETED` | `FAILED`), `errorMessage`, `dateCreated` (`dd/MM/yyyy - HH:mm`).

### `GET /member/import/{importId}/result`

**Response — `MemberBulkImportResultDetailDTO`:** los mismos contadores + `rows[]` con **todas** las filas procesadas.

Cada fila (`MemberBulkImportRowResultDTO`): `rowNumber`, `status`, `tipo`, `memberUuid`, `numeroSocio`, `errorMessage`, `warnings[]`.

Devuelve todas las filas en un solo payload, sin paginar. Usar solo cuando se necesita el detalle completo.

---

## 9. Personal interno — profesores y recepcionistas

Ambos siguen el mismo patrón: una entidad (`Profesor` / `Recepcionista`) que vincula un `User` con una `Company`, más `estado` (`ACTIVO` | `INACTIVO`) y `fechaAlta`.

> El campo `fechaNacimiento` **no** vive en las entidades `Profesor` / `Recepcionista` sino en `User` (`users.fecha_nacimiento`), igual que en `Member`. Los DTOs lo exponen leyéndolo del user.

### Profesores — `/profesor`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/profesor` | `activity.create` | Alta. Crea usuario portal con rol `PROFESOR` + fila en `profesores` (`ACTIVO`) |
| `PUT` | `/profesor/uuid/{profesorUuid}` | `club.internal_user.manage` **o** rol `ADM_RECEPCION` | Edición de datos personales |
| `POST` | `/profesor/paginate` | `activity.view` | Listado paginado |
| `DELETE` | `/profesor/{profesorUuid}` | `activity.delete` | Baja lógica → **204** |

**Alta — `ProfesorAltaCommand`:** `MemberAltaUserFields` (nombre, apellido, dni, fechaNacimiento, sexo, telefono, email, password) + `fechaAlta` opcional. `sexo` opcional.

**Baja lógica:** `status = -1`, `estado = INACTIVO`, y las asignaciones en `actividades_profesores` pasan a `status = -1`. Idempotente.

**Paginado — `ProfesorPaginateRequestDTO`:** `page`, `size`, `search` (nombre/apellido/DNI), `estados[]` (`ACTIVO`, `INACTIVO`). Orden: apellido, nombre.

### Recepcionistas — `/recepcionista`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/recepcionista` | `club.internal_user.manage` | Alta. Rol `ADM_RECEPCION` |
| `PUT` | `/recepcionista/uuid/{recepcionistaUuid}` | `club.internal_user.manage` **o** rol `ADM_RECEPCION` | Edición |
| `POST` | `/recepcionista/paginate` | `club.internal_user.manage` | Listado paginado |
| `DELETE` | `/recepcionista/{recepcionistaUuid}` | `club.internal_user.manage` | Baja lógica → **204** |
| `GET` | `/recepcionista/perfil` | Solo autenticado | Perfil propio del recepcionista logueado |
| `PUT` | `/recepcionista/perfil` | Solo autenticado | Actualiza el perfil propio |

`club.internal_user.manage` **no** está en el set de `ADM_RECEPCION`: en la práctica el ABM de recepcionistas es de `ADMIN`.

### Administradores — `/admin`

A diferencia de `Profesor`/`Recepcionista`, `ADMIN` **no tiene tabla propia** — es sólo un `User` con el rol `ADMIN` en `UserCompanyRole` para la company (mismo mecanismo que usa el alta del club para el admin fundador). El identificador en estos endpoints es el **`uuid` del `User`**.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/admin` | `club.internal_user.manage` | Alta de un admin adicional del club |
| `PUT` | `/admin/uuid/{userUuid}` | `club.internal_user.manage` | Edición de datos personales |
| `POST` | `/admin/paginate` | `club.internal_user.manage` | Listado paginado |
| `DELETE` | `/admin/{userUuid}` | `club.internal_user.manage` | Baja lógica (le remueve el rol `ADMIN`) → **204** |

**Alta — `AdminAltaCommand`:** `MemberAltaUserFields` (nombre, apellido, dni, fechaNacimiento, sexo, telefono, email, password), sin campos extra. `sexo` opcional.

**Cupo:** antes de crear el admin, se valida contra el producto `"bsports-admin"` del plan CRM del club (`plan.productos[]`, mismo mecanismo que usa el alta de socios contra `"bsports-socios"` — fail-closed: si no se puede determinar el límite, corta). Si se supera, `400` con mensaje `"El club alcanzó el límite de administradores de su plan"`.

**Reutilización de usuario:** si el email/dni ya corresponde a un usuario existente (de otra company), se reutiliza y sólo se le agrega el rol `ADMIN` para esta company — mismo comportamiento que el alta de Recepcionista/Profesor.

**Baja lógica:** le remueve el rol `"ADMIN"` de su `UserCompanyRole` en esa company (si no le queda ningún rol, la asignación se marca eliminada). **Nunca deja el club sin administradores**: si es el único admin, corta con `400` (`"No se puede eliminar el último administrador del club"`).

**Paginado — `AdminPaginateRequestDTO`:** `page`, `size`, `search` (nombre/apellido/email). Sin `estados[]` (no hay estado propio). Orden: apellido, nombre.

**`AdminDTO`:** `userUuid`, `nombre`, `apellido`, `dni`, `sexo`, `telefono`, `email`.

**Fuera de alcance:** el admin fundador del club (creado durante el alta del club en `/sysadmin/club`) no pasa por este chequeo de cupo. El endpoint genérico `POST /user/save` sigue pudiendo asignar el rol `ADMIN` sin control de cupo — no fue restringido.

### `StaffProfileUpdateCommand` — request compartido

Usado por los PUT de staff (`/profesor/uuid/{uuid}`, `/portal/profesor/perfil`, `/recepcionista/uuid/{uuid}`, `/recepcionista/perfil`, `/admin/uuid/{userUuid}`).

| Campo | Validación | Nota |
|---|---|---|
| `nombre` | — | |
| `apellido` | — | |
| `dni` | 7–11 dígitos numéricos | |
| `fechaNacimiento` | ISO `yyyy-MM-dd`, `@Past` | |
| `sexo` | `MASCULINO` / `FEMENINO` / `OTRO` | Opcional |
| `telefono` | — | |
| `email` | Formato email | Actualiza también el `username` |

Todos opcionales. Un campo omitido o `null` **no se modifica** — no hay forma de borrar un valor ya cargado.

### `ProfesorDTO` / `RecepcionistaDTO`

```json
{
  "profesorUuid": "…",
  "estado": "ACTIVO",
  "fechaAlta": "01/06/2026",
  "nombre": "Ana", "apellido": "López", "dni": "30222333",
  "fechaNacimiento": "12/04/1985",
  "sexo": "MASCULINO",
  "telefono": "1140002000", "email": "ana.lopez@club.com",
  "imageUrl": null
}
```

`RecepcionistaDTO` es idéntico salvo que la clave es `recepcionistaUuid` y **no tiene `imageUrl`**.

> ⚠️ Asimetría de formato: `fechaNacimiento` **se envía en ISO** (`1985-04-12`) y **se recibe en `dd/MM/yyyy`** (`12/04/1985`). Es la misma convención que socios.

### Alcance funcional del profesor

Ve solo **sus actividades asignadas** y los inscriptos de cada una. **No** puede editar actividades desde backoffice, **no** puede inscribir/desinscribir personas, **no** tiene acceso a tesorería en ningún caso. Sí puede operar eventos de actividad y asistencia **desde el portal**, solo sobre actividades donde está asignado.

Si pasa a `INACTIVO`, sus actividades siguen vigentes; la reasignación la hace el administrador.

---

## 10. Categorías

Base: `/categoria`. Categorías de edad del club (ej. «Sub-15»), asignables a actividades y socios.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/categoria` | `categoria.create` | Alta → **201** |
| `GET` | `/categoria` | Autenticado + company | Listado simple de activas (para combos) |
| `POST` | `/categoria/paginate` | `categoria.view` | Listado paginado |
| `PUT` | `/categoria/{categoriaUuid}` | `categoria.edit` | Edición |
| `DELETE` | `/categoria/{categoriaUuid}` | `categoria.delete` | Baja lógica → **204** |

**Request — `CategoriaAltaCommand` / `CategoriaEdicionCommand`:**

| Campo | Obligatorio | Validación |
|---|---|---|
| `nombre` | Sí | ≤255, único por club |
| `descripcion` | No | ≤512 |
| `edadMinima` | Sí | 0–120 |
| `edadMaxima` | Sí | 0–120, ≥ `edadMinima` |

**Response — `CategoriaDTO`:** `categoriaUuid`, `nombre`, `descripcion`, `edadMinima`, `edadMaxima`.

**Baja:** **409** si hay socios con esa categoría asignada.

**Edad reglamentaria** (`CategoriaEdadUtil`, año calendario actual): edad máxima = `año actual − año de nacimiento`; cumple la mínima si `fechaNacimiento + edadMinima` cayó en el año anterior o antes.

Roles con ABM completo: `ADM_RECEPCION` y `PROFESOR`.

---

## 10.1 Rangos de cuota social

Base: `/cuota-social-rango`. Tramos etarios del club (ej. «Menores», «Adultos», «Jubilados») usados para cobrar **distintos importes de cuota social** según la edad del socio. Reemplazan el importe único que antes vivía en `Company.importeCuotaSocial`.

**Modelo de tramo (bracket), no de rango con piso y techo:** cada `CuotaSocialRango` define un único límite superior, `edadHasta` — **único por club** (no puede haber dos rangos con el mismo `edadHasta` en la misma company). El rango que le corresponde a un socio es el que tiene el `edadHasta` más chico que sea `>= su edad` (ej.: rangos "hasta 17", "hasta 30" y "hasta 100" — un socio de 25 años cae en "hasta 30"). Con `edadHasta` único no puede haber ambigüedad: para cualquier edad hay como mucho un rango que matchea.

Toda company activa sin ningún rango configurado recibe automáticamente, vía migración, un rango catch-all **"General"** con `edadHasta = 100` e `importe = 0`, marcado `esDefault = true` — ese rango en particular **no puede eliminarse** (ver `DELETE` más abajo).

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/cuota-social-rango` | `cuota_social_rango.create` | Alta → **201** |
| `PUT` | `/cuota-social-rango/{cuotaSocialRangoUuid}` | `cuota_social_rango.edit` | Edición |
| `DELETE` | `/cuota-social-rango/{cuotaSocialRangoUuid}` | `cuota_social_rango.delete` | Baja lógica → **204** |
| `POST` | `/cuota-social-rango/paginate` | `cuota_social_rango.view` | Listado paginado |

A diferencia de `Categoria`, la validación de permiso se hace en el **controller** (`authService.valid(...)` antes de invocar el service), no dentro de cada service.

Tanto el alta como la edición disparan, tras el commit, un recálculo asíncrono del rango de todos los socios del club (ver [Asignación al socio](#asignación-al-socio) más abajo) — la respuesta del endpoint no espera a que termine.

**Request — `CuotaSocialRangoAltaCommand` / `CuotaSocialRangoEdicionCommand`:**

| Campo | Obligatorio | Validación |
|---|---|---|
| `nombre` | Sí | ≤255, único por club |
| `descripcion` | No | ≤512 |
| `edadHasta` | Sí | 0–120, único por club (**409** si ya existe otro rango con el mismo `edadHasta`) |
| `importe` | Sí | ≥ 0 (puede ser **0**, ver nota abajo) |

**Response — `CuotaSocialRangoDTO`:** `cuotaSocialRangoUuid`, `nombre`, `descripcion`, `edadHasta`, `importe`, `esDefault`.

**`DELETE /cuota-social-rango/{cuotaSocialRangoUuid}`** — baja lógica (`status = -1`).

- **204** sin body.
- **404** si no existe o ya está dado de baja.
- **400** si `esDefault = true` — el rango catch-all sembrado por migración (ver más arriba) **no puede eliminarse**, sea cual sea el club.
- Dispara, tras el commit, un recálculo asíncrono (`CuotaSocialRangoBajaEvent`) que reasigna a los socios que tenían justo ese rango (ver [Asignación al socio](#asignación-al-socio)).

Todavía no hay listado simple para combos (a diferencia de `Categoria`) — usar `paginate` con un `size` grande.

Roles con ABM: `ADM_RECEPCION` (no se le dio a `PROFESOR`, a diferencia de `categoria.*`).

### Asignación al socio

`Member` tiene un `ManyToOne` opcional a `CuotaSocialRango` (`cuotaSocialRangoId`). Dentro de `MemberDTO` se expone reducido a `cuotaSocialRango: { uuid, nombre }` — no viaja `edadHasta`/`importe` en ese contexto. Hoy no hay un command de alta/edición de socio que permita asignarlo manualmente (igual que pasa con `categoria`); se resuelve automáticamente en dos momentos:

- **Al dar de alta un socio** (titular o familiar): `CuotaSocialRangoResolver` calcula la edad (año actual − año de nacimiento) y le asigna el rango con el `edadHasta` más chico que la cubra. Si el club tiene la cuota social habilitada pero falta `fechaNacimiento`, o ningún rango cubre esa edad, el alta falla con **400**.
- **Recálculo automático al crear/editar un rango:** `POST /cuota-social-rango` y `PUT /cuota-social-rango/{uuid}` publican un evento (`CuotaSocialRangoCambiadoEvent`) que dispara, **de forma asíncrona** (`@Async`, pool `AsyncTaskExecutor`, después del commit), `MemberCuotaSocialRangoRecalculoService.recalcularPorCompany(companyId)`. Ese proceso recorre todos los socios activos del club y, para cada uno, recalcula el rango que le corresponde según su edad actual — así, insertar o mover un tramo reacomoda automáticamente a los socios existentes que quedaron en el tramo equivocado. Solo **asigna** cuando encuentra un rango que matchea la edad actual; si no encuentra ninguno, no toca la asignación existente. Los socios sin `fechaNacimiento` se omiten.
- **Recálculo automático al dar de baja un rango:** `DELETE /cuota-social-rango/{uuid}` publica `CuotaSocialRangoBajaEvent`, que dispara (mismo mecanismo async) `MemberCuotaSocialRangoRecalculoService.recalcularPorRangoBorrado(companyId, rangoId)`. A diferencia del caso anterior, acá el alcance es **solo** los socios que tenían asignado justo el rango borrado: se les limpia la referencia y se les busca el tramo que corresponda a su edad actual — si ninguno cubre esa edad, quedan sin rango asignado (`null`).
- **Migración de backfill** (`v48-members-backfill-cuota-social-rango.sql`): al desplegar el modelo de rangos, asigna el rango correspondiente a todos los socios activos que todavía no tenían uno, con el mismo criterio de edad.

### Impacto en la generación de la cuota social

`Cuota.deSocial(...)` (ver [§16](#16-tesorería--cuotas)) toma el importe de `persona.cuotaSocialRango.importe`, **no** de la company. Si el socio no tiene rango asignado, o el rango no tiene importe, la generación de esa cuota falla con **400**.

**Importe 0:** si el rango asignado tiene `importe = 0`, la cuota social se crea directamente en estado **`PAGADA`** (no `PENDIENTE`) — pensado para rangos exentos (ej. jubilados con cuota social bonificada).

---

## 11. Actividades e inscripciones

Base: `/actividad`.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/actividad` | `activity.create` | Alta |
| `PUT` | `/actividad/{actividadUuid}` | `activity.edit` | Edición |
| `GET` | `/actividad/{actividadUuid}` | `activity.view` | Detalle completo |
| `POST` | `/actividad/paginate` | `activity.view` | Listado paginado (DTO liviano) |
| `POST` | `/actividad/{actividadUuid}/inscripcion` | `activity.enroll` | Inscribe un socio |
| `POST` | `/actividad/{actividadUuid}/inscripcion/baja` | `activity.unenroll` | Da de baja una inscripción |
| `DELETE` | `/actividad/{actividadUuid}` | `activity.delete` | Baja lógica → **204** |

> El rol `SOCIO_TITULAR` también tiene `activity.view` — un titular logueado en el portal puede llamar `POST /actividad/paginate` y `GET /actividad/{actividadUuid}` directamente para explorar el catálogo completo de actividades del club antes de auto-inscribirse (ver [§21](#21-portal-del-socio)). `FAMILIAR` no lo tiene.

### Alta / edición — `ActividadAltaCommand` / `ActividadEdicionCommand`

| Campo | Obligatorio | Validación |
|---|---|---|
| `nombre` | Sí | ≤255 |
| `ubicacion` | Sí | ≤255 |
| `cupoMaximo` | Sí | ≥1 |
| `diasSemana` | Sí | Tokens `LUNES`…`DOMINGO` separados por `;`. Ej: `"LUNES;MIERCOLES;VIERNES"` |
| `horaInicio`, `horaFin` | Sí | `HH:mm` |
| `importeCuota` | Sí | > 0 |
| `categoriaUuid` | No | Categoría activa del club |
| `profesorUuids[]` | No | Ver reglas abajo |

**Efectos del alta:** `completa = false`; genera slots `CONFIRMADO` en una ventana de 3 meses hacia adelante.

**Reglas de `profesorUuids` en la edición:**

- **Omitido o `null`:** no modifica la asignación actual.
- **Enviado (incluido `[]`):** **reemplaza** el conjunto — desasigna a los activos que no estén en la lista y asigna a los de la lista que no estaban.
- Cada uuid debe ser un profesor `ACTIVO` del club → **400** si no.

**`categoriaUuid` en la edición:** omitido o vacío **quita** la categoría.

La edición **no** regenera slots.

### Detalle — `GET /actividad/{actividadUuid}` → `ActividadDTO`

```json
{
  "actividadUuid": "…", "nombre": "Fútbol Sub-15", "ubicacion": "Cancha 1",
  "cupoMaximo": 20, "diasSemana": "LUNES;MIERCOLES", "horaInicio": "18:00", "horaFin": "19:30",
  "importeCuota": 15000.00, "completa": false,
  "categoria": { "categoriaUuid": "…", "nombre": "Sub-15", "edadMinima": 13, "edadMaxima": 15 },
  "profesores": [ { "nombre": "Ana", "apellido": "López", … } ],
  "inscriptos": [ { "userUuid": "…", "titularMemberUuid": "…", "nombre": "…", "apellido": "…", "imageUrl": null, "email": "…", "dni": "…", "fechaNacimiento": "12/04/2011" } ]
}
```

`inscriptos[]` solo se puebla en el detalle por uuid — viene vacío en alta y edición.

**404** si el uuid no existe o no pertenece a la company del header.

### Paginado — `POST /actividad/paginate`

**Request — `ActividadPaginateRequestDTO`:** `page`, `size`, `search` (nombre/ubicación), `completa` (boolean opcional), `profesorUuid` (opcional).

**Response — `Page<ActividadPaginateDTO>`:** DTO liviano, **sin** relaciones: en lugar de los objetos `categoria` y `profesores` trae `categoriaNombre` (string o `null`) y `cantidadInscriptos` (long).

### Inscripción — `POST /actividad/{actividadUuid}/inscripcion`

**Request — `ActividadInscripcionCommand`:**

```json
{ "memberUuid": "…", "descuentos": [ { "descuentoUuid": "…", "fechaDesde": "2026-06-01", "fechaHasta": null } ] }
```

`memberUuid` (titular o familiar del club) es lo único obligatorio. `descuentos[]` es opcional — asigna descuentos recurrentes a esta inscripción en el mismo request, ver [§16.1](#161-descuentos).

**Reglas:**

- Rechaza duplicado (socio ya inscripto).
- Rechaza si la actividad está `completa` o el cupo se agotó.
- Solo socios en estado `ACTIVO` → **400** para `MOROSO` / `INHABILITADO` / `SUSPENDIDO` / `INACTIVO`.
- Si la actividad tiene categoría, valida la edad del socio contra `edadMinima` / `edadMaxima` (requiere `fechaNacimiento` cargada).

**Efectos:** persiste en `inscripciones_actividad`; genera una cuota tipo `ACTIVIDAD` con fecha de generación igual a la de inscripción (vencimiento a los `diasVencimientoCuota` de la actividad) y deja `proximaGeneracion` un período adelante (según `periodicidadCuota`); al llenar el cupo, `actividadCompleta` pasa a `true`.

**Response — `InscripcionActividadDTO`:** `inscripcionUuid`, `actividadUuid`, `actividadNombre`, `memberUuid`, `memberNombre`, `memberApellido`, `fechaInscripcion` (`dd/MM/yyyy`), `actividadCompleta`, `cuotaGenerada`, `cuotaUuid`, `cuotaAdvertencia`.

Revisar `cuotaAdvertencia` cuando `cuotaGenerada = false`: explica por qué se omitió la cuota.

### Baja de inscripción — `POST /actividad/{actividadUuid}/inscripcion/baja`

Mismo body. Soft-delete de la inscripción activa; si queda cupo libre, `actividadCompleta` vuelve a `false`. **No anula cuotas ya generadas.**

**404** si la actividad no existe o el socio no está inscripto.

### Baja lógica de actividad — `DELETE /actividad/{actividadUuid}`

| Aspecto | Comportamiento |
|---|---|
| Bloqueo | **409** si tiene socios inscriptos activos — darlos de baja primero |
| Efectos | Desasigna todos los profesores activos (`actividades_profesores.status = -1`); borra los slots **futuros** (los pasados quedan como historial) |
| Sin efecto sobre | Eventos puntuales (`evento_actividad`), cuotas ya generadas, asistencias registradas |
| Respuesta | **204**, idempotente |

> En el modelo de datos, **desactivar ≡ eliminar**: ambos casos son `Actividad.status = -1`. No hay estado intermedio.

---

## 12. Eventos de actividad

Eventos **puntuales** (una sola ocurrencia) vinculados a una actividad — distintos de los slots recurrentes auto-generados. Se persisten en la tabla propia `evento_actividad`, **no** en `slots`.

Son **informativos**: no impactan cuotas, tesorería, inscripciones ni notificaciones.

### Backoffice — `/evento-actividad`

| Método | Ruta | Permiso |
|---|---|---|
| `POST` | `/evento-actividad` | `activity.create` |
| `PUT` | `/evento-actividad/{eventoUuid}` | `activity.edit` |
| `GET` | `/evento-actividad/{eventoUuid}` | `activity.view` **o** `portal.activities.view` |
| `POST` | `/evento-actividad/paginate` | `activity.view` **o** `portal.activities.view` |
| `POST` | `/evento-actividad/{eventoUuid}/estado` | `activity.delete` |
| `DELETE` | `/evento-actividad/{eventoUuid}` | `activity.delete` |

### Portal profesor — `/portal/profesor/evento-actividad`

Mismas rutas y permisos, pero el profesor solo puede operar sobre actividades **donde está asignado**. El alta responde **201**.

| Método | Ruta |
|---|---|
| `POST` | `/portal/profesor/evento-actividad` |
| `PUT` | `/portal/profesor/evento-actividad/{eventoUuid}` |
| `GET` | `/portal/profesor/evento-actividad/{eventoUuid}` |
| `POST` | `/portal/profesor/evento-actividad/{eventoUuid}/estado` |
| `DELETE` | `/portal/profesor/evento-actividad/{eventoUuid}` |

> El profesor opera eventos **solo desde el portal**. Desde backoffice, la operación no está permitida para ese rol.

### `EventoActividadAltaCommand`

| Campo | Obligatorio | Formato |
|---|---|---|
| `actividadUuid` | Sí | La actividad debe estar activa (`status = 0`) |
| `titulo` | Sí | ≤255 |
| `descripcion` | Sí | |
| `ubicacion` | Sí | ≤255 |
| `inicio` | Sí | **`dd/MM/yyyy HH:mm:ss`** |
| `fin` | Sí | **`dd/MM/yyyy HH:mm:ss`**, posterior a `inicio` |

`EventoActividadEdicionCommand` es igual pero **sin** `actividadUuid` (no se puede mover un evento de actividad).

**Reglas:**

- Única validación de fechas: `fin` > `inicio`. El horario del evento **no** tiene que respetar los días ni el rango horario de la actividad.
- **No** se pueden crear eventos si la actividad está eliminada/desactivada (`status = -1`). Los eventos ya existentes de una actividad desactivada **siguen vivos**.
- **Se permiten solapamientos** con slots recurrentes de la misma actividad, con otros eventos, y con slots de amenities.
- Editar y dar de baja está permitido **sin restricción por antigüedad** — incluidos eventos ya pasados.

### Cambiar estado vs. baja lógica

| Operación | Efecto | Visible en calendario |
|---|---|---|
| `POST .../estado` con `{"estado": "CANCELADO"}` | `estado = CANCELADO`, `status` sigue en `0` | **Sí** |
| `DELETE .../{eventoUuid}` | `status = -1` | **No** |

`POST .../estado` devuelve **409** si el evento ya está en el estado solicitado.

### `EventoActividadDetalleDTO`

`uuid`, `actividadUuid`, `actividadNombre`, `titulo`, `descripcion`, `ubicacion`, `inicio`, `fin` (`dd/MM/yyyy HH:mm:ss`), `estado` (`CONFIRMADO` | `CANCELADO`).

> La `descripcion` **solo** aparece en el detalle. En el calendario se exponen `titulo`, `ubicacion` y `referenciaNombre` (nombre de la actividad).

### Paginado

**Request — `EventoActividadPaginateRequestDTO`:** `page`, `size`, `search` (coincide por título del evento **o** por nombre de la actividad), `actividadUuid` (opcional).

**No filtra por `estado`:** incluye `CONFIRMADO` y `CANCELADO`; solo excluye los de baja lógica.

---

## 13. Asistencia a actividades

Registro informativo de presencia por **slot recurrente** (una clase concreta). **No** aplica a eventos puntuales.

Un registro por combinación **slot + member** (único). Estados persistidos: `PRESENTE` y `AUSENTE`. **Sin registro = sin marcar** (`null` en la planilla): no se persiste fila.

| Método | Ruta | Permiso | Audiencia |
|---|---|---|---|
| `POST` | `/asistencia-actividad/paginate` | `activity.attendance.view` | Backoffice — **solo lectura** |
| `POST` | `/portal/profesor/asistencia/paginate` | `activity.attendance.manage` | Portal profesor |
| `GET` | `/portal/profesor/asistencia/slot/{slotUuid}` | `activity.attendance.manage` | Planilla de un slot |
| `PUT` | `/portal/profesor/asistencia/slot/{slotUuid}` | `activity.attendance.manage` | Guardado en lote |

> Admin y recepción **solo consultan**. Registrar y corregir es exclusivo del profesor asignado, desde el portal. Socios y familiares **no** tienen acceso al historial.

### Elegibilidad del profesor

Debe estar **activo** y tener asignación vigente en `actividades_profesores` para la actividad del slot. Además: el slot no puede estar `CANCELADO` y la actividad debe tener `status = 0`.

### Planilla — `GET /portal/profesor/asistencia/slot/{slotUuid}`

Qué alumnos se listan depende de la fecha del slot:

| Slot | Alumnos listados |
|---|---|
| **Futuro o del día actual** | Solo inscripciones **activas** (`status = 0`) |
| **Pasado** | Inscripciones activas **y** ex-inscriptos (`status = -1`) — histórico |

Un inscripto cuya `fecha_inscripcion` es **posterior** a la fecha del slot **figura igual**, con asistencia `null` (caso atípico permitido).

**Response — `AsistenciaPlanillaDTO`:** `slotUuid`, `actividadUuid`, `actividadNombre`, `fecha` (`dd/MM/yyyy`), `inicio`, `fin` (`dd/MM/yyyy HH:mm:ss`), `alumnos[]`.

Cada alumno (`AsistenciaPlanillaRowDTO`): `memberUuid`, `memberNombre`, `memberApellido`, `fechaInscripcion` (`dd/MM/yyyy`), `estado` (`PRESENTE` | `AUSENTE` | `null`).

### Guardado en lote — `PUT /portal/profesor/asistencia/slot/{slotUuid}`

**Request — `AsistenciaLoteCommand`:**

```json
{ "items": [ { "memberUuid": "…", "estado": "PRESENTE" }, { "memberUuid": "…", "estado": "AUSENTE" } ] }
```

`items` no puede estar vacío. Cada ítem exige `memberUuid` y `estado`.

**Reglas:**

- **Idempotente:** reenviar el mismo lote actualiza sin duplicar.
- **Lote parcial permitido:** no hace falta mandar toda la planilla.
- **Todo o nada:** si algún ítem es inválido (member fuera de la planilla válida, member repetido, ítem sin estado), se **rechaza toda la operación** y no se persiste nada.
- **Sin restricción temporal** para registrar o corregir — incluye slots pasados y futuros.
- **No se puede desmarcar:** solo alternar entre `PRESENTE` y `AUSENTE`. No hay forma de volver a `null` / eliminar el registro.

**Response:** la planilla actualizada (`AsistenciaPlanillaDTO`).

### Consulta paginada

**Request — `AsistenciaPaginateRequestDTO`:**

| Campo | Obligatorio | Formato |
|---|---|---|
| `actividadUuid` | **Sí** | |
| `page`, `size` | No | |
| `fechaDesde`, `fechaHasta` | No | `dd/MM/yyyy` |
| `memberUuid` | No | Acepta también el alias JSON `personaMemberUuid` |

**Response — `Page<AsistenciaActividadConsultaDTO>`:** `uuid`, `slotUuid`, `actividadUuid`, `actividadNombre`, `memberUuid`, `memberNombre`, `memberApellido`, `fecha` (`dd/MM/yyyy`), `slotInicio` (`dd/MM/yyyy HH:mm:ss`), `estado`.

---

## 14. Calendario

Vista unificada que combina **tres fuentes**: slots recurrentes de actividades, slots de amenities y eventos puntuales de actividad.

| Método | Ruta | Permiso | Alcance |
|---|---|---|---|
| `POST` | `/calendario` | `activity.view` **o** `amenity.view` | **Staff** — todo el club |
| `POST` | `/portal/calendario` | `portal.activities.view` | **Socio** — solo actividades con inscripción activa |
| `POST` | `/portal/profesor/calendario` | `activity.view` | **Profesor** — solo actividades donde está asignado |

**Request — `CalendarioConsultaCommand`** (los tres usan el mismo):

```json
{ "desde": "01/06/2026", "hasta": "30/06/2026" }
```

Formato **`dd/MM/yyyy`**. El backend expande a rango inclusivo (`desde` 00:00 — `hasta` 23:59:59.999999999).

**Response — `List<CalendarioSlotDTO>`:**

```json
[{
  "slotUuid": "…", "eventoUuid": null,
  "titulo": "Fútbol Sub-15", "ubicacion": "Cancha 1",
  "tipo": "ACTIVIDAD",
  "referenciaUuid": "…", "referenciaNombre": "Fútbol Sub-15",
  "titularNombre": null, "titularApellido": null,
  "inicio": "03/06/2026 18:00:00", "fin": "03/06/2026 19:30:00",
  "estado": "CONFIRMADO",
  "porcentajeAsistencia": 66.7
}]
```

| Campo | Significado |
|---|---|
| `slotUuid` | Poblado para slots (actividad o amenity); `null` para eventos |
| `eventoUuid` | Poblado **solo** para eventos de actividad |
| `tipo` | `ACTIVIDAD` \| `AMENITY` |
| `referenciaUuid` / `referenciaNombre` | Actividad o amenity de origen |
| `titularNombre` / `titularApellido` | Socio de la reserva (solo amenities) |
| `estado` | `SlotEstadoEnum`: `PENDIENTE_DE_PAGO`, `CONFIRMADO`, `CANCELADO` |
| `porcentajeAsistencia` | `Double` 0–100 con un decimal. **Solo** en `POST /portal/profesor/calendario`, en items de tipo `ACTIVIDAD`; `null` en el resto (ver abajo) |

**`porcentajeAsistencia` (solo calendario del profesor):** `presentes / alumnos × 100`.

- **Presentes:** asistencias del slot con `estado = PRESENTE` y `status = 0`.
- **Alumnos:** socios distintos con inscripción en la actividad. Cuentan las de `status = 0` y las de `status = -1` (baja) **solo si su `date_updated` es posterior al inicio del slot** (estaba inscripto cuando se dictó).
- Tope en 100. **`null`** si no hay alumnos que contar, y en eventos de actividad, amenities y en los calendarios `/calendario` y `/portal/calendario`.
- Una asistencia sin marcar (slot sin planilla guardada) cuenta como 0 presentes, así que un slot sin tomar asistencia devuelve `0.0` si tiene alumnos.
- `date_updated` se actualiza en cualquier edición de la inscripción, no solo al darla de baja.
- La planilla `GET /portal/profesor/asistencia/slot/{slotUuid}` **no** aplica la regla de fecha de baja: para un slot pasado incluye todas las inscripciones en `-1`, así que el porcentaje del calendario y la lista de la planilla pueden diferir.

Guía de integración: [`CALENDARIO-PROFESOR-ASISTENCIA-FRONT.md`](CALENDARIO-PROFESOR-ASISTENCIA-FRONT.md).

**Visibilidad:**

- Items `CONFIRMADO` y `CANCELADO` **sí aparecen** (los cancelados siguen visibles a propósito).
- Items con **baja lógica** (`status = -1`) **no aparecen**.
- Slots **eliminados físicamente** (reserva de amenity vencida a las 48 h sin seña) no aparecen porque ya no existen. Distinto de `CANCELADO`.
- El calendario del socio se evalúa **al momento de consultar**: si se inscribe después de creado un evento, lo ve; si se da de baja, deja de verlo.
- La `descripcion` del evento **no** viaja en el calendario — solo en `GET /evento-actividad/{eventoUuid}`.

Para tomar asistencia, el `slotUuid` de tipo `ACTIVIDAD` sale de acá.

---

## 15. Amenities y reservas

Instalaciones reservables con cobro por uso (ej. parrillas, quinchos). La reserva **crea el slot en un solo paso**; no hay huecos libres previos.

### ABM de amenities — `/amenities`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/amenities` | `amenity.create` | Alta |
| `PUT` | `/amenities/{uuid}` | `amenity.edit` | Edición |
| `GET` | `/amenities` | `amenity.view` | Listado de activos (staff) |
| `DELETE` | `/amenities/{uuid}` | `amenity.delete` | Desactivación → **204** |

**Request — `AmenityAltaCommand` / `AmenityEdicionCommand`:**

| Campo | Obligatorio | Validación |
|---|---|---|
| `nombre` | Sí | ≤255 |
| `montoHora` | Sí | ≥ 0 |
| `porcentajeSena` | Sí | 0–100 |
| `diasSemana` | Sí | Tokens `LUNES`…`DOMINGO` separados por `;` |
| `horaInicio`, `horaFin` | Sí | `HH:mm` |

**Amenity gratuito:** `montoHora = 0` **exige** `porcentajeSena = 0`. Amenity de pago exige ambos > 0.

**Response — `AmenityDTO`:** `amenityUuid`, `nombre`, `montoHora`, `porcentajeSena`, `diasSemana`, `horaInicio`, `horaFin` (`HH:mm`).

**Desactivación (`DELETE /amenities/{uuid}`):** los slots **futuros** pasan a `CANCELADO` (sin borrado físico, siguen visibles en calendario); las reservas vinculadas pasan a `CANCELADA` con `saldoPendiente = 0`; el amenity deja de listarse. **No** se revierten movimientos de tesorería previos — las devoluciones son manuales (egreso aparte).

### Reservas — `/amenities/reservas`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/amenities/reservas` | `amenity.reservation.create` | Crea reserva + slot |
| `POST` | `/amenities/reservas/paginate` | `amenity.view` | Listado paginado (backoffice) |
| `POST` | `/amenities/reservas/{uuid}/confirmar` | `amenity.reservation.collect` | Confirma reserva **gratuita** |
| `POST` | `/amenities/reservas/{uuid}/cobro-sena` | `treasury.collect` **o** `amenity.reservation.collect` | Cobra la seña |
| `POST` | `/amenities/reservas/{uuid}/cobro-saldo` | `treasury.collect` **o** `amenity.reservation.collect` | Cobra el saldo |
| `POST` | `/amenities/reservas/{uuid}/cobro-total` | `treasury.collect` **o** `amenity.reservation.collect` | Cobra el 100 % en un paso |
| `DELETE` | `/amenities/reservas/{uuid}` | `amenity.reservation.cancel` | Cancelación manual |

> **Self-service desde el portal:** además de este flujo de staff, el socio titular puede crear y pagar su propia reserva con `POST /portal/amenities/{amenityUuid}/reservar` (ver [§21](#21-portal-del-socio)). Reutiliza exactamente estas mismas reglas de negocio y el mismo cobro por Mercado Pago — la diferencia es que el permiso se resuelve en el controller del portal y el pago se fuerza siempre a `MERCADO_PAGO` (el socio no puede cobrar en efectivo). Ver también `docs/PORTAL-AMENITIES-RESERVAR-FRONT.md` para la guía de integración del front.

### `POST /amenities/reservas` — `ReservaAmenityAltaCommand`

```json
{ "amenityUuid": "…", "memberUuid": "…", "inicio": "2026-06-07T14:00:00", "fin": "2026-06-07T16:00:00" }
```

Formato **ISO datetime** (sin `@JsonFormat`).

**Reglas de negocio:**

| Regla | Detalle |
|---|---|
| Solo titular | Si `memberUuid` es un familiar no titular → **400** |
| Ventana reservable | De **hoy+2** a **hoy+7** días calendario, ambos inclusive |
| Tope semanal | Máx. **2 reservas de amenities por titular** por semana calendario (lunes a domingo). No aplica a inscripciones de actividades |
| Solapamiento | Prohibido entre slots del **mismo** amenity. Permitido con actividades y con otros amenities |
| Rango horario | `inicio`–`fin` debe respetar `diasSemana` y `horaInicio`/`horaFin` del amenity |
| Monto | Calculado **automática y sincrónicamente** por hora, con facturación mínima de **1 hora** |

**Efectos:** crea slot en `PENDIENTE_DE_PAGO` + reserva en `PENDIENTE_SENA`; `venceSenaAt` = creación + 48 h.

**Response — `ReservaAmenityDTO`:**

```json
{
  "reservaUuid": "…", "amenityUuid": "…", "amenityNombre": "Parrilla 1",
  "memberUuid": "…", "memberNombre": "Carlos", "memberApellido": "Gómez",
  "slotUuid": "…", "slotInicio": "07/06/2026 14:00:00", "slotFin": "07/06/2026 16:00:00",
  "slotEstado": "PENDIENTE_DE_PAGO",
  "montoTotal": 20000.00, "montoSena": 6000.00, "saldoPendiente": 20000.00,
  "estado": "PENDIENTE_SENA",
  "venceSenaAt": "09/06/2026 14:00:00",
  "canceladaPorUuid": null, "canceladaAt": null
}
```

**Semántica de `saldoPendiente`** — es la deuda **total**, no el resto:

| Momento | Valor |
|---|---|
| Al crear | `montoTotal` (seña incluida) |
| Tras cobrar la seña | `montoTotal − montoSena` |
| Tras cobrar el saldo, o cobro total | `0` |

### Cobros — `ReservaAmenityCobroCommand`

| Campo | Obligatorio | Notas |
|---|---|---|
| `canalCobro` | Sí | `MANUAL` \| `MERCADO_PAGO` |
| `monto` | Solo en `MANUAL` | > 0. En `cobro-total` manual debe ser **exactamente** `montoTotal` |
| `medioPago` | Solo en `MANUAL` | `EFECTIVO`, `TRANSFERENCIA`, `OTRO` |
| `fecha` | No | ISO |
| `observaciones` | No | |

En `MERCADO_PAGO` solo se envía `canalCobro`; la respuesta incluye `qr` (PNG Base64) y el movimiento de tesorería se registra recién al aprobarse el webhook.

**Response — `ReservaAmenityCobroResultDTO`:** `reservaUuid`, `tipoCobro` (`SENA` | `SALDO` | `TOTAL`), `monto`, `concepto`, `movimientoUuid` (`null` en MP hasta el webhook), `qr` (solo MP).

**Efectos tras la seña:** slot → `CONFIRMADO`, reserva → `CONFIRMADA`, `saldoPendiente` → `montoTotal − montoSena`. **Basta la seña para usar el amenity**: no hace falta el pago total antes del uso.

**`cobro-total`:** solo desde `PENDIENTE_SENA` **sin seña previa**. Deja la reserva `CONFIRMADA` con `saldoPendiente = 0` y un único movimiento. Si ya hay seña parcial, usar `cobro-saldo`. **Rechaza reservas gratuitas** (400 → usar `/confirmar`).

### `POST /amenities/reservas/{uuid}/confirmar` — reserva gratuita

Sin body. Solo para reservas con `montoTotal = 0` en `PENDIENTE_SENA`. Mismo efecto que la seña pagada, **sin movimiento de tesorería**. Los endpoints de cobro rechazan reservas gratuitas.

Las reservas gratuitas también vencen a las 48 h si no se confirman.

### `DELETE /amenities/reservas/{uuid}` — cancelación manual

Se cancela **por UUID de reserva**, no de slot.

**Reglas:** solo reservas en `PENDIENTE_SENA` o `CONFIRMADA`, con horario de uso **presente o futuro**. Cancelación definitiva, no reversible. No se exige motivo.

**Efectos:** reserva → `CANCELADA`; slot → `CANCELADO` (**sigue visible** en calendario); `saldoPendiente` → `0`; se auditan `canceladaPorUuid` y `canceladaAt`. Libera cupo del tope semanal y el slot deja de bloquear solapamientos.

**Tesorería:** **no** se revierten ni anulan movimientos automáticamente, con o sin seña/saldo cobrado.

**Errores:** **404** no encontrada o de otro club; **409** ya cancelada; **400** vencida o con uso finalizado. Si la reserva sigue activa pero el slot ya está `CANCELADO`, la operación es **idempotente** (alinea reserva a `CANCELADA` + saldo 0).

### Matriz de baja de una reserva

| Camino | Disparador | Reserva | Slot | Tesorería |
|---|---|---|---|---|
| **Cancelación manual** | Staff con `amenity.reservation.cancel` | → `CANCELADA` | → `CANCELADO` (visible) | Sin reversión; saldo → 0 |
| **Vencimiento 48 h sin seña** | Job automático | → `VENCIDA` | **Eliminado físicamente** (no visible) | Sin movimientos (nunca se cobró) |
| **Desactivar amenity** | Staff al desactivar | → `CANCELADA` | → `CANCELADO` (visible) | Sin reversión; saldo → 0 |

En **ningún** camino se revierten movimientos de tesorería automáticamente.

### Paginado de reservas — `ReservaPaginateRequestDTO`

`page`, `size`, `estados[]` (`ReservaEstadoEnum`), `amenityUuid`, `memberUuid`, `slotDesde` / `slotHasta` (`dd/MM/yyyy HH:mm:ss`), `search`.

---

## 16. Tesorería — cuotas

Base: `/treasury/cuota`.

### Modelo de generación

> ⚠️ El documento funcional §2.4 / §3.1 describe un **job mensual el día 1 con vencimiento el día 10**. Eso está **reemplazado** por el modelo **cada N días** con plazos configurables por club y por actividad. Manda este modelo. Las cuotas ya **no tienen `mes`/`anio`**: la única fecha que las ubica en el tiempo es `fechaGeneracion`.

Cada cuota se asocia a **dos** entidades: la **persona que la origina** (quien genera la deuda) y el **titular responsable** del pago. Si el socio es titular sin grupo, ambas referencias apuntan a la misma persona.

**Parámetros por club (`Company`):**

| Parámetro | Default | Significado |
|---|---|---|
| `periodicidadCuotaSocial` | `MENSUAL` | Cada cuánto se genera la **cuota social** de cada socio: `SEMANAL`, `MENSUAL`, `BIMESTRAL`, `TRIMESTRAL`, `CUATRIMESTRAL`, `SEMESTRAL` o `ANUAL` |
| `diasVencimientoCuota` | 10 | Días desde la generación hasta el **vencimiento** de la cuota **social** |
| `diasVencimientoMoroso` | 31 | Días desde el vencimiento de la cuota impaga más antigua del grupo para pasar a **MOROSO**. No define el vencimiento de la cuota |
| `diasInhabilitacion` | 91 | Días desde la generación de la cuota social impaga **más antigua del grupo** para pasar a **INHABILITADO** |

Solo `ADMIN` o `SYSADMIN` pueden modificarlos (vía `PUT /companies/profile`).

**Parámetros por actividad (`Actividad`):**

| Parámetro | Default | Significado |
|---|---|---|
| `periodicidadCuota` | `MENSUAL` | Cada cuánto se genera la cuota de **actividad** de cada inscripción (mismos valores) |
| `diasVencimientoCuota` | 30 | Días desde la generación hasta el vencimiento de la cuota de actividad |

**Puntero de próxima generación:**

Cada inscripción (`InscripcionActividad.proximaGeneracion`) y cada socio (`Member.proximaGeneracionCuota`) guardan la **fecha en la que le toca la próxima cuota**. La regla es una sola:

> Mientras la próxima generación sea menor o igual a la fecha de corte, se genera la cuota **con esa fecha** y el puntero avanza un período (`periodicidadCuota` de la actividad o `periodicidadCuotaSocial` del club). El avance es por calendario: `SEMANAL` suma 7 días y el resto suma meses, así las cuotas mantienen el día de la primera generación (si el mes destino no tiene ese día, se usa el último día del mes y el puntero sigue desde ahí).

- Cuota y puntero se guardan en la **misma transacción**: si falla el commit no queda ninguno de los dos, y volver a correr no duplica.
- El puntero avanza sobre la fecha programada, no sobre hoy: si el job no corre un día, el ciclo no se corre.
- Si hay varios ciclos pendientes (por ejemplo, generando con una fecha futura), se generan **todos** de una vez, una cuota por ciclo.
- Si una cuota no se puede construir (actividad sin importe, socio sin titular o sin rango), el puntero queda en esa fecha y se reintenta en la próxima corrida.
- La inscripción usa además optimistic locking (`@Version`), para que dos procesos no generen la misma cuota a la vez.

**Vencimiento:** actividad → `fechaGeneracion + Actividad.diasVencimientoCuota`; social → `fechaGeneracion + Company.diasVencimientoCuota`.

**Cuota SOCIAL:** el club necesita `cuotaSocialHabilitada = true`; si no, la generación se omite en silencio (sin error). El **importe** sale del `CuotaSocialRango` asignado al socio ([§10.1](#101-rangos-de-cuota-social)). Si el socio no tiene rango asignado, o el rango no tiene importe, la generación de esa cuota puntual falla: en la corrida queda como `cuotasOmitidas`, y en el alta/aprobación de un socio individual propaga el error. Si el importe del rango es **0**, la cuota se crea directamente `PAGADA`.

**Disparadores de la primera cuota:**

| Tipo | Disparador | Condición |
|---|---|---|
| `SOCIAL` | Alta de titular o familiar (no pública), o aprobación de un registro público pendiente | Club con `cuotaSocialHabilitada = true` **y** socio con `cuotaSocialRango` asignado con importe |
| `ACTIVIDAD` | Inscripción en una actividad | Actividad con `importeCuota` configurado |

Se genera **en el mismo paso** del alta/inscripción, con fecha de generación igual a la del alta/inscripción, y el puntero queda un ciclo adelante (`fecha + días del ciclo`). Si el club no tiene cuota social, el puntero del socio queda en la fecha de alta. El registro **público** (`POST /member/public/titular`, `POST /member/public/familiar`) es la excepción: queda `PENDING_APPROVAL` y la cuota social se genera recién al aprobar (`POST /member/uuid/{memberUuid}/aprobar-registro`), no en el alta.

**Elegibilidad para generar cuotas por estado del socio:**

| Estado | ¿Genera? |
|---|---|
| `ACTIVO`, `MOROSO`, `SUSPENDIDO` | Sí |
| `INHABILITADO`, `INACTIVO` | No |

**Descuentos por cuota:**

> ⚠️ Reemplaza el modelo anterior de un único `descuento` porcentual entero por cuota. Ver [§16.1](#161-descuentos) para el modelo completo (catálogo, asignación recurrente, snapshot).

Una cuota puede tener **cero, uno o varios** descuentos aplicados simultáneamente, cada uno como línea independiente (`CuotaDescuento`):

| Campo | Significado |
|---|---|
| `monto` | Importe de lista (de la actividad, o del rango de cuota social del socio). **No** es lo que se cobra |
| `descuentos[]` | Snapshot de los descuentos aplicados en el momento de generar la cuota: `{ titulo, valor }` cada uno |
| `montoFinal` | `monto − Σ(descuentos[].valor)`, con dos decimales y **sin bajar de 0**. **Es el importe cobrable** |

Todo lo que mueve plata usa `montoFinal`: el cobro manual, el movimiento de caja, la preferencia de MercadoPago y el total por cobrar del período. Si la suma de descuentos iguala o supera el `monto`, la cuota se cobra en $0 (nunca queda negativa).

Cada descuento aplicado se calcula **independientemente** sobre el `monto` de lista (no en cascada uno sobre el saldo que deja el anterior) — si hay dos descuentos del 20% y 10% sobre una cuota de $10.000, el resultado es $10.000 − $2.000 − $1.000 = $7.000, no $10.000 × 0,8 × 0,9.

### 16.1 Descuentos

Modelo introducido para permitir convenios, promociones o descuentos individuales sobre cuotas sociales y de actividad.

**`Descuento` — catálogo por club.** Definición reutilizable, sin vigencia propia: un título (se usa como snapshot en la cuota) y un valor expresado como **porcentaje** (0–100) o **monto fijo** — nunca los dos a la vez. El monto fijo queda acotado al `monto` de la cuota al aplicarse (nunca genera un descuento negativo).

**ABM del catálogo — `/descuento`:**

| Método | Ruta | Permiso | Body | Acción |
|---|---|---|---|---|
| `POST` | `/descuento` | `descuento.create` | `DescuentoAltaCommand` | Alta |
| `PUT` | `/descuento/{descuentoUuid}` | `descuento.edit` | `DescuentoEdicionCommand` | Edición |
| `DELETE` | `/descuento/{descuentoUuid}` | `descuento.delete` | — | Baja lógica, ver nota abajo |
| `POST` | `/descuento/catalogo/paginate` | `descuento.view` | `DescuentoPaginateRequestDTO` | Listado paginado |

`DescuentoAltaCommand`/`DescuentoEdicionCommand` — mismo shape en los dos:

```json
{ "titulo": "Convenio Empresa X", "tipoValor": "PORCENTAJE", "porcentaje": 20, "montoFijo": null }
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `titulo` | Sí | No vacío. Sin restricción de unicidad — pueden existir dos descuentos con el mismo título |
| `tipoValor` | Sí | `PORCENTAJE` o `MONTO_FIJO` |
| `porcentaje` | Solo si `tipoValor = PORCENTAJE` | 0–100. **400** fuera de rango |
| `montoFijo` | Solo si `tipoValor = MONTO_FIJO` | `> 0`. **400** si no |

La edición permite cambiar de `PORCENTAJE` a `MONTO_FIJO` (o viceversa) — el campo que no
corresponde al nuevo tipo queda en `null`. **Respuesta 201** (alta) **/ 200** (edición) —
`DescuentoDTO`: `{ descuentoUuid, titulo, tipoValor, porcentaje, montoFijo }`.

`POST /descuento/catalogo/paginate` — request `{ page, size, search }` (`search` sobre
`titulo`), response `Page<DescuentoDTO>`. Distinto de `POST /descuento/paginate` (ver más abajo),
que pagina socios con sus descuentos asignados, no el catálogo.

**`DELETE /descuento/{descuentoUuid}` — baja lógica con cascada física.** Antes de marcar el
`Descuento` como borrado (`status = -1`), el backend **elimina físicamente** todas las filas de
`MemberDescuento` e `InscripcionActividadDescuento` que lo referencien — no queda ninguna
asignación viva apuntando a un descuento dado de baja. Todo ocurre en una sola transacción: si
algo falla, no se borra nada. Las cuotas **ya generadas** no se ven afectadas — guardan su propio
snapshot (`titulo`/`valor` copiados en `CuotaDescuento`), sin referencia viva al catálogo.
**Irreversible.** Respuesta **204**. **404** si el uuid no existe o es de otra company.

No hay validación de "no se puede borrar si tiene asignaciones" — a diferencia de otros ABM de
este sistema (ej. `CuotaSocialRango` bloquea el borrado del rango por defecto), acá el borrado
siempre procede y arrastra sus asignaciones consigo.

**Asignación recurrente — vigencia propia, no del catálogo.** Un mismo `Descuento` del catálogo (ej. "Convenio Empresa X −20%") puede tener vigencias distintas según a quién se le asigne: a un socio desde enero a junio, a otro desde marzo sin fecha de fin. Por eso la vigencia (`fechaDesde`, `fechaHasta` opcional) vive en la asignación, no en el catálogo. No hay exclusividad: un mismo socio/inscripción puede tener más de una asignación vigente al mismo tiempo — todas se aplican, cada una como línea independiente en la cuota.

Hay dos puntos de asignación, independientes entre sí (reflejan la misma separación que ya existe entre cuota social y de actividad):

| Asignación | A quién | Impacta |
|---|---|---|
| `MemberDescuento` | Al `Member` (socio) | Cuota **social** |
| `InscripcionActividadDescuento` | A la `InscripcionActividad` | Cuota de esa **actividad** |

**Cómo se asigna hoy (sin ABM, vía los endpoints de alta):**

| Endpoint | Campo | Asigna a |
|---|---|---|
| `POST /member/titular` | `descuentos[]` (opcional) | `MemberDescuento` del socio recién creado |
| `POST /member/familiar` | `descuentos[]` (opcional) | `MemberDescuento` del familiar recién creado |
| `POST /actividad/{actividadUuid}/inscripcion` | `descuentos[]` (opcional) | `InscripcionActividadDescuento` de la inscripción recién creada |
| `POST /member/uuid/{memberUuid}/aprobar-registro` | `descuentos[]` (opcional) | `MemberDescuento` del socio que se activa al aprobar |

Shape del item, igual en los cuatro:

```json
{ "descuentoUuid": "…", "fechaDesde": "2026-06-01", "fechaHasta": null }
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `descuentoUuid` | Sí | Uuid de un `Descuento` activo del club. **404** si no existe o pertenece a otro club |
| `fechaDesde` | Sí | ISO `yyyy-MM-dd` |
| `fechaHasta` | No | `null`/omitido = sin fecha de fin |

La asignación se persiste **antes** de generar la primera cuota del alta/inscripción, en la misma transacción — así esa primera cuota ya sale con el descuento aplicado si la `fechaDesde` cubre la fecha de generación. Para asignar un descuento a un socio/inscripción que ya existe (sin pasar por un alta nueva), o para quitar/finalizar una asignación existente, ver **Gestión de asignaciones** más abajo.

**Dónde no está disponible:** portal público (`POST /member/public/titular`, `POST /member/portal/familiar`, `POST /portal/member/actividad/inscripcion`) — sus commands no exponen `descuentos[]`, a propósito. Tampoco la importación masiva (`POST /member/import`).

**Consulta por socio — `POST /descuento/paginate`**

Base: `/descuento`. Lista socios con sus descuentos asignados (distinto del catálogo — ver
`POST /descuento/catalogo/paginate` arriba).

**Permiso:** `member.view` (mismo que `/member/paginate`).

**Request — `MemberDescuentoPaginateRequestDTO`:**

```json
{ "page": 1, "size": 10, "search": "gómez" }
```

`search` busca por nombre, apellido, DNI **o número de socio** — a diferencia de `/member/paginate`,
que no busca por número de socio.

**Response — `Page<MemberDescuentosDTO>`:**

```json
{
  "content": [
    {
      "memberUuid": "…",
      "nombre": "Carlos Gómez",
      "fotoUrl": null,
      "cuotaSocial": [
        {
          "asignacionUuid": "…",
          "descuentoUuid": "…",
          "titulo": "Convenio Empresa X",
          "tipoValor": "PORCENTAJE",
          "porcentaje": 20,
          "montoFijo": null,
          "fechaDesde": "2026-06-01",
          "fechaHasta": null,
          "vigente": true
        }
      ],
      "actividades": [
        {
          "actividadUuid": "…",
          "actividadNombre": "Fútbol Sub-15",
          "descuentos": [ { "…": "mismo shape que arriba" } ]
        }
      ]
    }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 10
}
```

Notas:
- Trae **todos** los socios del club, paginados — no solo los que tienen algún descuento.
  `cuotaSocial` y `actividades` vienen como array vacío (`[]`) para un socio sin nada asignado.
- `nombre` combina `nombre + apellido` en un solo string (no vienen separados como en `MemberDTO`).
- `actividades[]` solo lista actividades donde el socio tiene **al menos un** descuento asignado
  — no es la lista de todas sus inscripciones. Si está inscripto en 3 actividades y solo una
  tiene descuento, acá aparece solo esa.
- `vigente` se calcula contra la fecha de hoy en el momento de la consulta — no es un valor
  guardado, así que una asignación puede pasar de `true` a `false` sin que nadie la haya tocado
  (por ejemplo, al llegar su `fechaHasta`).
- `asignacionUuid` es el uuid de la fila `MemberDescuento`/`InscripcionActividadDescuento` (la
  asignación en sí), distinto de `descuentoUuid` (el catálogo). Es el identificador que piden los
  endpoints de baja/finalizar de **Gestión de asignaciones**, más abajo.

**Gestión de asignaciones — asignar / baja física / finalizar**

Además de asignarse de paso en el alta (arriba), un `MemberDescuento`/`InscripcionActividadDescuento`
se puede crear, finalizar o eliminar de forma independiente, sobre un socio o una inscripción que
ya existen. 6 endpoints, todos en `/descuento`:

| Método | Ruta | Permiso | Body | Acción |
|---|---|---|---|---|
| `POST` | `/descuento/member/{memberUuid}` | `member.edit` | `MemberDescuentoCommand` | Asigna un descuento a un socio existente |
| `DELETE` | `/descuento/member-descuento/{uuid}` | `member.edit` | — | Baja **física** de la asignación |
| `PUT` | `/descuento/member-descuento/{uuid}/finalizar` | `member.edit` | `{ "fecha": "…" }` | Cierra la vigencia a partir de esa fecha |
| `POST` | `/descuento/inscripcion-actividad/{inscripcionActividadUuid}` | `activity.enroll` | `InscripcionActividadDescuentoCommand` | Asigna un descuento a una inscripción existente |
| `DELETE` | `/descuento/inscripcion-actividad-descuento/{uuid}` | `activity.enroll` | — | Baja física de la asignación |
| `PUT` | `/descuento/inscripcion-actividad-descuento/{uuid}/finalizar` | `activity.enroll` | `{ "fecha": "…" }` | Cierra la vigencia |

Notar que `{uuid}` significa cosas distintas según el endpoint: en los dos `POST` es el uuid del
**dueño** (`memberUuid`/`inscripcionActividadUuid`); en `DELETE`/`finalizar` es el uuid de la
**asignación misma** (`asignacionUuid`, el mismo que devuelve `/descuento/paginate`). Por eso el
segmento de ruta cambia (`member` vs. `member-descuento`, `inscripcion-actividad` vs.
`inscripcion-actividad-descuento`) — evita la ambigüedad de reusar el mismo path para dos
identificadores distintos.

**`POST /descuento/member/{memberUuid}`** — body `MemberDescuentoCommand`, mismo shape que
`descuentos[]` en el alta (`{descuentoUuid, fechaDesde, fechaHasta}`). Respuesta **201** con el
`DescuentoAsignadoDTO` recién creado (mismo shape que en `cuotaSocial[]`/`actividades[].descuentos[]`
del paginado). **404** si `memberUuid` o `descuentoUuid` no existen o son de otra company.

**`POST /descuento/inscripcion-actividad/{inscripcionActividadUuid}`** — análogo, body
`InscripcionActividadDescuentoCommand`, mismo shape. **404** si la inscripción o el descuento no
existen o son de otra company.

**`DELETE /descuento/member-descuento/{uuid}`** / **`DELETE /descuento/inscripcion-actividad-descuento/{uuid}`** —
**baja física**, no lógica: la fila se borra de la tabla (`member_descuentos`/
`inscripcion_actividad_descuentos`), no se marca `status = -1`. Es la única excepción al patrón
de borrado lógico del resto del dominio — se decidió así porque ninguna otra tabla referencia
estas filas (son hojas del modelo) y las cuotas ya generadas guardan su propio snapshot
(`CuotaDescuento`, con `titulo`/`valor` copiados) sin FK hacia la asignación, así que borrarla no
dejar nada huérfano ni afecta cuotas previas. Respuesta **204**. **404** si el uuid no existe o es
de otra company (ya sea porque nunca existió o porque ya se borró antes).

**`PUT /descuento/member-descuento/{uuid}/finalizar`** / **`PUT /descuento/inscripcion-actividad-descuento/{uuid}/finalizar`** —
body `{ "fecha": "yyyy-MM-dd" }` (obligatorio). Pone `fechaHasta = fecha` en la asignación, sin
borrarla — a diferencia de la baja física, esto conserva el historial de que la asignación
existió y estuvo vigente hasta esa fecha. Respuesta **200** con el `DescuentoAsignadoDTO`
actualizado (`vigente` ya recalculado contra hoy). **400** si `fecha` es anterior a la
`fechaDesde` de la asignación. **404** si el uuid no existe o es de otra company.

Estos 6 endpoints de asignación no usan los permisos `descuento.*` (esos son solo para el ABM del
catálogo, ver arriba) — reutilizan `member.edit` (lado social) y `activity.enroll` (lado
actividad, el mismo que ya gatea `/actividad/{uuid}/inscripcion`) por ser los más cercanos
semánticamente a "modificar la config financiera de un socio/inscripción".

**Morosidad e inhabilitación:**

El estado se recalcula (`MemberEstadoRecalculoService`) al cobrar, sea cual sea el método
(cobro manual, Mercado Pago), y en el **cron diario de vencimiento** (`CuotaVencimientoService`
marca `PENDIENTE → VENCIDA` y dispara el recálculo vía evento). **La generación de cuotas
(job, generación manual o alta puntual) no dispara el recálculo por sí misma** — si se generan
cuotas atrasadas que ya nacen vencidas, el socio no queda MOROSO/INHABILITADO hasta la corrida
del cron. El algoritmo toma, por titular, la cuota en estado `VENCIDA` con `vencimiento` más
antiguo del grupo — **de cualquier tipo, SOCIAL o ACTIVIDAD** — y compara los días
transcurridos desde esa fecha contra los parámetros del club:

| Condición | Estado resultante |
|---|---|
| Sin ninguna cuota `VENCIDA` en el grupo | `ACTIVO` |
| Días desde el `vencimiento` más antiguo `>= diasInhabilitacion` | `INHABILITADO` |
| Días desde el `vencimiento` más antiguo `>= diasVencimientoMoroso` (y no inhabilitado) | `MOROSO` |
| En cualquier otro caso (hay deuda vencida pero aún no cruza el umbral de moroso) | `ACTIVO` |

- **MOROSO** es **informativo**: sin bloqueo de portal, sin restricción de inscripciones, sin detener la generación. Aplica a **todo el grupo familiar**.
- **INHABILITADO** aplica a **todo el grupo**: detiene la generación, da de baja las inscripciones activas, bloquea el portal. La deuda sigue visible. Tras regularizar, la reinscripción a actividades es **manual**.
- ⚠️ Las cuotas de **actividad** vencidas **cuentan** para morosidad/inhabilitación igual que las sociales (antes quedaban totalmente excluidas).
- **No se permiten pagos parciales.** Al pagar toda la deuda del grupo (social y de actividad), vuelve a `ACTIVO` de inmediato.

### Endpoints

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/treasury/cuota/generacion/actividad?hasta=dd/MM/yyyy` | rol `ADMIN` | Genera las cuotas de **actividad** pendientes hasta la fecha para el club del header |
| `POST` | `/treasury/cuota/generacion/social?hasta=dd/MM/yyyy` | rol `ADMIN` | Genera las cuotas **sociales** pendientes hasta la fecha para el club del header |
| `POST` | `/treasury/cuota/cobro` | `treasury.collect` **o** `portal.fee.pay_mercado_pago` | Registra el cobro de una o varias cuotas |
| `POST` | `/treasury/cuota/paginate` | `treasury.view` | Listado paginado con filtros |
| `GET` | `/treasury/cuota/persona/{personaMemberUuid}` | `treasury.view` | Cuotas de una persona |
| `GET` | `/treasury/cuota/titular/{titularMemberUuid}/grupo` | `treasury.view` **o** `treasury.debt_consolidated` | Deuda consolidada del grupo familiar |
| `GET` | `/treasury/cuota/uuid/{cuotaUuid}` | `treasury.view` | Detalle de una cuota |
| `POST` | `/treasury/cuota/uuid/{cuotaUuid}/anular` | `treasury.void` | Anula (perdona) una cuota |

**`POST /treasury/cuota/generacion/actividad`** y **`POST /treasury/cuota/generacion/social`**

Mismo contrato en los dos; la única diferencia es qué tipo de cuota generan. Reemplazan a los anteriores `/generacion/periodo/actividad` y `/generacion/periodo/social` (con `anio`/`mes`), que **ya no existen**.

| Query param | Obligatorio | Notas |
|---|---|---|
| `hasta` | Sí | Fecha de corte, formato **`dd/MM/yyyy`** (ej. `15/12/2026`). Falta o formato inválido: **400** |

Genera, para el club del header, **todas** las cuotas cuyo puntero de próxima generación sea `<= hasta`, una por ciclo pendiente (ver [Modelo de generación](#modelo-de-generación)). Se permiten fechas **futuras**: sirve para probar los ciclos o facturar por adelantado.

Es idempotente: repetirlo con la misma fecha (o una anterior) devuelve `cuotasGeneradas = 0`, porque los punteros ya quedaron después de `hasta`.

**Response — `CuotaActividadGeneracionResultDTO`** (actividad) **/ `CuotaSocialGeneracionResultDTO`** (social), mismo shape en los dos:

```json
{ "hasta": "15/12/2026", "companiesProcessed": 1, "cuotasGeneradas": 42, "cuotasOmitidas": 1 }
```

`cuotasOmitidas` cuenta socios/inscripciones que **correspondía** facturar y no se pudo: actividad sin importe o sin días de generación, socio sin rango de cuota social asignado (o rango sin importe), socio sin titular. No cuenta los que no tenían nada pendiente.

> ⚠️ Generar con una fecha **futura** mueve los punteros hacia adelante: esas cuotas ya no las vuelve a generar el job cuando llegue la fecha. Si después cambia el importe de la actividad o de la cuota social, las cuotas ya generadas **no se actualizan**. En producción, usar una fecha futura solo para facturar por adelantado a propósito.

Los jobs automáticos corren a diario con `hasta = hoy` y hacen lo mismo para todos los clubes activos: actividad con `tesoreria.cuota-actividad.cron` (default `0 30 0 * * *`), social con `tesoreria.cuota-social.cron` (default `0 0 0 * * *`).

### `POST /treasury/cuota/cobro` — `CobroCuotaCommand`

| Campo | Obligatorio | Notas |
|---|---|---|
| `cuotaUuids[]` | Sí | Cuotas a cobrar |
| `canalCobro` | Sí | `MANUAL` \| `MERCADO_PAGO` |
| `medioPago` | Sí | `EFECTIVO`, `TRANSFERENCIA`, `MERCADO_PAGO`, `OTRO` |
| `fecha` | No | ISO, no futura |
| `observaciones` | No | |

**Backoffice:** `canalCobro = MANUAL` con `treasury.collect`.
**Portal titular:** `canalCobro = MERCADO_PAGO` con `portal.fee.pay_mercado_pago`; requiere credenciales MP cargadas por SYSADMIN.

**Response — `CobroCuotaResultDTO`:**

```json
{
  "items": [ { "cuotaUuid": "…", "exitoso": true, "movimientoUuid": "…", "mensajeError": null } ],
  "exitosas": 3, "fallidas": 0,
  "mercadoPagoPreferenceId": "…",
  "mercadoPagoInitPoint": "https://…",
  "mercadoPagoSandboxInitPoint": "https://…"
}
```

En MP, redirigir a `mercadoPagoInitPoint`. La liquidación ocurre **asincrónicamente vía webhook** tras el pago aprobado.

Registrar un cobro de cuota social dispara el **recálculo de estados del grupo**.

### `POST /treasury/cuota/uuid/{cuotaUuid}/anular` — `CuotaAnularCommand`

`{ "motivo": "Condonación por antigüedad del socio" }` — obligatorio, ≤512 caracteres.

Perdona la cuota directamente, **sin** generar movimiento de cobro. Solo si está `PENDIENTE` o `VENCIDA` → **409** si está `PAGADA` (hay que anular primero el movimiento de cobro) o ya `ANULADA`. Recalcula la morosidad del grupo del titular.

Ningún job regenera una cuota anulada: al generarla, el puntero de la inscripción o del socio ya avanzó al ciclo siguiente, así que la anulación es definitiva para las corridas automáticas.

Una nueva inscripción a la misma actividad es una inscripción distinta, con su propio puntero, y genera su cuota al inscribirse. Las unique keys por fecha de generación ignoran las anuladas, así que esa cuota nueva puede tener la misma fecha que una anulada.

### Filtros y listados

**`CuotaPaginateRequestDTO`:** `page`, `size`, `estados[]`, `tipos[]`, `search`, `titularMemberUuid`, `personaMemberUuid`, `orden` (default `VENCIMIENTO_DESC`).

Los GET por persona/grupo aceptan `estado` y `tipo` como query params repetibles.

**`CuotaResumenDTO`** (base): `cuotaUuid`, `concepto`, `tipo`, `monto`, `descuento`, `montoFinal`, `vencimiento` (`dd/MM/yyyy`), `estado`, `diasMora`, `fechaGeneracion` (`dd/MM/yyyy`), `actividadId`.

> **Breaking change:** se eliminaron `mes` y `anio` de todas las respuestas de cuota. Para ubicar la cuota en el tiempo usar **`fechaGeneracion`**. Ver [`CUOTAS-GENERACION-POR-DIAS-FRONT.md`](CUOTAS-GENERACION-POR-DIAS-FRONT.md).

> **Breaking change:** se eliminó el campo `origen` (`ALTA` / `INSCRIPCION` / `CICLO_MENSUAL`) de todas las respuestas de cuota; ya no se distingue el disparador. En su lugar aparecen `descuento` y `montoFinal`. Para mostrar importes usar **`montoFinal`**, no `monto`.

**`CuotaPaginateRowDTO`:** lo anterior + datos de persona (`personaMemberUuid`, `personaNombre`, `personaApellido`, `personaEstado`) y titular (`titularMemberUuid`, `titularNombre`, `titularApellido`, `titularEmail`, `titularTelefono`).

**`CuotaDetalleDTO`:** `CuotaResumenDTO` + persona/titular + `parentesco` + `anuladaPorUuid`, `anuladaMotivo`, `anuladaAt` (poblados solo si `estado = ANULADA`).

**`CuotaPersonaListDTO`** (por persona): `personaMemberUuid`, `nombre`, `apellido`, `parentesco`, `estadoMember`, `titularMemberUuid`, `cuotas[]`, `subtotalPersona`, `subtotalDeudaPersona`, `totalPorPagar`.

**`CuotaGrupoTitularDTO`** (deuda consolidada): `titularMemberUuid`, `titularNombre`, `titularApellido`, `montoTotalGrupo`, `montoDeudaGrupo`, `totalPorPagarGrupo`, `personas[]` (cada una un `CuotaPersonaGrupoDTO` con sus cuotas y subtotales).

Los subtotales de deuda (`subtotalDeudaPersona`, `montoDeudaGrupo`) suman las cuotas con `diasMora > 0`. **No existe** un campo `impaga` ni un filtro `soloImpagas`.

---

## 17. Tesorería — movimientos y balance

### Movimientos — `/treasury/movimiento`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/treasury/movimiento/paginate` | `treasury.view` | Listado paginado |
| `GET` | `/treasury/movimiento/uuid/{movimientoUuid}` | `treasury.view` | Detalle |
| `POST` | `/treasury/movimiento` | `treasury.collect` (ingreso) / `treasury.expense` (egreso) | Alta manual → **201** |
| `POST` | `/treasury/movimiento/uuid/{movimientoUuid}/anular` | `treasury.void` | Anulación |

**No hay edición (`PUT`):** las correcciones se hacen anulando y creando un movimiento nuevo.

**Request — `MovimientoManualCommand`:**

| Campo | Obligatorio | Validación |
|---|---|---|
| `tipo` | Sí | `INGRESO` \| `EGRESO` |
| `monto` | Sí | > 0 |
| `concepto` | Sí | ≤255 |
| `fecha` | No | ISO, no futura |
| `medioPago` | Condicional | Obligatorio en ingresos manuales |
| `observaciones` | No | ≤65535 |

El permiso requerido depende del `tipo`: los egresos exigen `treasury.expense` (ADMIN y `ADM_RECEPCION`).

**Request — `MovimientoAnularCommand`:** `{ "motivo": "…" }` obligatorio, ≤512. Solo ADMIN (`treasury.void`). Si el movimiento era el cobro de una cuota, **revierte la cuota a pendiente/vencida**.

**Request — `MovimientoPaginateRequestDTO`:** `page`, `size`, `tipo`, `fechaDesde`/`fechaHasta` (ISO `yyyy-MM-dd`, opcionales — si se omiten ambos trae el histórico completo, orden descendente, sin límite de rango), `estados[]` (default `ACTIVO`).

**Response — `MovimientoRowDTO`:** `movimientoUuid`, `tipo`, `monto`, `fecha` (`dd/MM/yyyy`), `concepto`, `medioPago`, `observaciones`, `estado`, `origen` (`COBRO_CUOTA` | `COBRO_RESERVA` | `MANUAL`), `cuotaUuid`, `reservaUuid`, datos de persona y titular, y de quién lo registró.

**`MovimientoDetalleDTO`** suma `anuladaPorUuid`, `anuladaMotivo`, `anuladaAt`.

### Balance — `/treasury/balance`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/treasury/balance` | `treasury.view` | Balance del mes en curso |
| `POST` | `/treasury/balance/paginate` | `treasury.view` | Histórico mensual (**excluye** el mes actual) |

**Response — `BalanceMesDTO` / `BalanceResumenMesDTO`:**

| Campo | Significado |
|---|---|
| `periodo` | `MM/yyyy` |
| `mes`, `anio` | Numéricos |
| `ingresosTotales` | Movimientos de ingreso activos del período |
| `egresosTotales` | Movimientos de egreso activos del período |
| `porCobrar` | Cuotas `PENDIENTE` / `VENCIDA` **con `fechaGeneracion` dentro del mes** del período — no toda la deuda histórica |
| `ingresosNetos` | `ingresosTotales − egresosTotales` |
| `ingresosEstimados` | `ingresosTotales + porCobrar` |

---

## 18. Accesos al club

Base: `/acceso-club`. Registra quién entra y sale del predio y quién está adentro en tiempo real.

**Modelo genérico por `User`**, no por `Member`: cubre socios (titular y familiar), profesores y personal interno. Dos tablas:

| Tabla | Rol |
|---|---|
| `accesos_club` | Log **append-only** de eventos `INGRESO` / `EGRESO` |
| `club_presencia` | Snapshot «¿está adentro ahora?» por `(company_id, user_id)` — unique |

Módulo **informativo**: no impacta cuotas, tesorería, inscripciones ni el estado del socio.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/acceso-club` | `club.access.register` | Registra acceso (toggle) → **201** |
| `GET` | `/acceso-club?desde=&hasta=` | `club.access.view` | Historial por rango |
| `GET` | `/acceso-club/presentes` | `club.access.view` | Quiénes están adentro ahora |
| `GET` | `/acceso-club/live` | `club.access.view` | Stream SSE de actualizaciones |

### `POST /acceso-club` — toggle automático

**Request — `AccesoClubRegistroCommand`:** `{ "userUuid": "…" }`.

> Recepción **no envía el tipo**: el backend lo infiere según la presencia actual. Sin fila o `presente = false` → `INGRESO`; `presente = true` → `EGRESO`.

Cada llamada persiste **exactamente un** registro en `accesos_club` (append-only) y sincroniza `club_presencia` en la **misma transacción**.

El `userUuid` es el contenido del QR de la credencial del portal.

**Validación por estado del socio** (si el `User` tiene un `Member` activo en el club):

| Estado | Comportamiento |
|---|---|
| `ACTIVO` | Permitido, sin advertencia |
| `MOROSO` | Permitido; la respuesta incluye `advertencia` textual (no se persiste) |
| `INHABILITADO`, `SUSPENDIDO`, `INACTIVO` | **403** — no registra evento ni modifica presencia. Aplica **tanto a ingreso como a egreso** |

Si el usuario **no** tiene `Member` (staff, profesor), el acceso se permite y `memberId` / `memberEstado` quedan `null`.

`member_estado` en el log es **histórico**: cambios posteriores del socio no alteran registros pasados.

**Response 201 — `AccesoClubRegistroDTO`:** `accesoUuid`, `userUuid`, `nombre`, `apellido`, `dni`, `tipo` (`INGRESO` | `EGRESO`), `memberEstado`, `advertencia`, `presente` (estado **posterior** al evento), `fecha` (`dd/MM/yyyy HH:mm:ss`).

**Errores:** **404** usuario inexistente o inactivo; **400** usuario de otro club o body inválido; **403** socio en estado bloqueado.

### `GET /acceso-club?desde=&hasta=`

Query params **obligatorios**, ISO `yyyy-MM-dd`. El backend expande a rango inclusivo (`desde` 00:00:00 — `hasta` 23:59:59.999999999). **400** si `desde` > `hasta`.

Orden descendente por fecha. Solo registros con `status = 0` del club actual.

**Response — `List<AccesoClubItemDTO>`:** `accesoUuid`, `userUuid`, `nombre`, `apellido`, `dni`, `tipo`, `memberEstado`, `fecha`.

### `GET /acceso-club/presentes`

Filas de `club_presencia` con `presente = true`, ordenadas por `ultimoAccesoAt` descendente.

**Response — `ClubPresenciaListDTO`:** `{ "presentes": [ClubPresenciaItemDTO], "count": 12 }`.

`ClubPresenciaItemDTO`: `userUuid`, `nombre`, `apellido`, `dni`, `presente`, `imageUrl`, `ultimoAccesoAt` (`dd/MM/yyyy HH:mm:ss`).

Los usuarios que nunca pasaron por recepción **no tienen fila** en `club_presencia` y no aparecen.

### `GET /acceso-club/live` — SSE

Stream `text/event-stream` scopeado a la company del header. **Sí requiere** `club.access.view` (a diferencia del SSE de importación).

Cada `POST /acceso-club` de esa company dispara un evento `update` a todos los suscriptos:

```json
{ "item": { …AccesoClubItemDTO… }, "presenteCount": 13 }
```

El servidor envía además un comentario `heartbeat` cada 30 s para mantener viva la conexión (no genera evento en el cliente).

---

## 19. Eventos de acceso / entradas

Base: `/evento-acceso`. Eventos con entrada paga (ej. una fiesta del club) donde se escanea el carnet en la puerta.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/evento-acceso` | Solo club resuelto | Alta → **201** |
| `POST` | `/evento-acceso/paginate` | Solo club resuelto | Listado paginado |
| `GET` | `/evento-acceso/{eventoUuid}` | Solo club resuelto | Detalle |
| `PUT` | `/evento-acceso/{eventoUuid}` | Solo club resuelto | Edición |
| `DELETE` | `/evento-acceso/{eventoUuid}` | Solo club resuelto | Baja lógica → **204** |
| `POST` | `/evento-acceso/{eventoUuid}/scan` | `treasury.collect` | Escaneo del QR del carnet — solo informa, no cobra |
| `POST` | `/evento-acceso/{eventoUuid}/cobro` | `treasury.collect` | **Endpoint unificado de cobro** (reemplaza a los antiguos `/invitado` y `/moroso`) → **201** |
| `POST` | `/evento-acceso/{eventoUuid}/historial/paginate` | Solo club resuelto | Historial paginado de scans/cobros del evento |

> ⚠️ **El ABM y las consultas no validan ningún permiso** — solo exigen JWT válido y un club resuelto por `X-Company-Uuid` (`getCurrentCompanyOrThrow()`). Cualquier usuario del club puede crear, editar y borrar eventos de acceso, y consultar el historial. Solo `scan` / `cobro` exigen `treasury.collect`.

> ⚠️ **Breaking change:** los endpoints `POST /evento-acceso/{eventoUuid}/invitado` y `POST /evento-acceso/{eventoUuid}/moroso` **ya no existen**. Ver [guía de migración para el front](EVENTO-ACCESO-MIGRACION-FRONT.md).

### Alta / edición — `EventoAccesoAltaCommand` / `EventoAccesoEdicionCommand`

| Campo | Obligatorio | Notas |
|---|---|---|
| `titulo` | Sí | ≤255 |
| `descripcion` | Sí | |
| `monto` | Sí | ≥ 0. Precio general del evento — se usa para cobrar a un socio `ACTIVO` (ver `/cobro`). `0` = gratuito |
| `fecha` | No | `LocalDateTime`. **Sin `@JsonFormat`** → viaja en el formato timestamp/array por defecto de Jackson, no `dd/MM/yyyy`. Ver nota de fechas más abajo |
| `permiteMorosos` | Sí | Boolean. Si `false`, ningún socio moroso puede acceder/pagar en este evento |
| `permiteInvitados` | Sí | Boolean. Si `false`, no se admiten invitados en este evento |
| `montoMorosos` | Sí | ≥ 0. Precio a cobrar a un socio moroso. `0` = gratuito |
| `montoInvitados` | Sí | ≥ 0. Precio a cobrar a un invitado. `0` = gratuito |

> ⚠️ **Fechas sin formatear:** a diferencia del resto de la API, `fecha` en `EventoAcceso`/`EventoAccesoDetalleDTO`/`EventoAccesoHistorialDetalleDTO` **no tiene `@JsonFormat`**. Con la configuración actual de Jackson (`JacksonConfig`, sin `WRITE_DATES_AS_TIMESTAMPS` deshabilitado), un `LocalDateTime`/`Timestamp` sin anotación serializa como **array numérico** (`[2026,9,15,20,0,0]`) o timestamp epoch, no como string. Confirmar el formato real contra un ambiente antes de parsear en el front.

**Response — `EventoAccesoDetalleDTO`:** `uuid`, `titulo`, `descripcion`, `monto`, `fecha`, `permiteMorosos`, `permiteInvitados`, `montoMorosos`, `montoInvitados`.

**Paginado:** `page` (default 1), `size` (default 10, **máximo 20**), `search` (LIKE case-insensitive sobre `titulo`). Orden por `dateCreated` descendente.

### `POST .../scan` — `EventoAccesoScanCommand`

`{ "userUuid": "…" }`. Escaneo del carnet en la puerta. **Ya no lanza 402** — siempre responde **200** con un resultado explícito, y **cada scan queda registrado en el historial** (ver más abajo), incluidos los accesos permitidos.

**Response — `EventoAccesoScanResultDTO`:**

| Campo | Notas |
|---|---|
| `memberUuid`, `nombre`, `apellido`, `numeroSocio` | Datos del socio escaneado |
| `estado` | `EventoAccesoScanEstadoEnum`: `ACCESO_PERMITIDO` \| `DEBE_PAGAR` \| `BLOQUEADO` |
| `motivo` | Texto explicativo. `null` cuando `estado = ACCESO_PERMITIDO` |
| `montoAPagar` | Solo seteado cuando `estado = DEBE_PAGAR` (`monto` si `ACTIVO`, `montoMorosos` si `MOROSO`) |

| `estado` | Cuándo | Acción del front |
|---|---|---|
| `ACCESO_PERMITIDO` | Socio `ACTIVO` con `monto = 0`, o `MOROSO` con `permiteMorosos = true` y `montoMorosos = 0` | Dejar pasar, no hay nada más que hacer |
| `DEBE_PAGAR` | Socio `ACTIVO` con `monto > 0`, o `MOROSO` con `permiteMorosos = true` y `montoMorosos > 0` | Mostrar `montoAPagar` y disparar `POST .../cobro` con `tipo: "SOCIO"` |
| `BLOQUEADO` | Socio `MOROSO` con `permiteMorosos = false`, o `INHABILITADO`/`SUSPENDIDO`/`INACTIVO` | No dejar pasar; mostrar `motivo` |

### `POST .../cobro` — endpoint unificado (Strategy)

Reemplaza a los antiguos `/invitado` y `/moroso`. **Request — `EventoAccesoCobroCommand`:**

```json
{
  "tipo": "SOCIO",
  "payload": { "...": "..." }
}
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `tipo` | Sí | `EventoAccesoHistorialTipoEnum`: `SOCIO` \| `INVITADO` |
| `payload` | Sí | Objeto libre — su forma depende de `tipo` (ver abajo). El backend lo parsea y valida según corresponda; si no matchea el shape esperado responde **400** |

**`payload` cuando `tipo = "SOCIO"`** (equivale al viejo `EventoAccesoMorosoCobroCommand`, ahora `EventoAccesoSocioCobroCommand`):

| Campo | Obligatorio | Notas |
|---|---|---|
| `userUuid` | Sí | Socio a cobrar (viene del QR/scan) |
| `canalCobro` | Sí | `MANUAL` \| `MERCADO_PAGO` |
| `cantidadEntradas` | No | ≥1, default 1 |
| `medioPago` | Solo en `MANUAL` | |
| `fecha`, `observaciones` | No | |

Lógica según el estado del socio en el momento del cobro:

- **`MOROSO`** con `evento.permiteMorosos = true` **y** `evento.montoMorosos > 0` → cobra `montoMorosos × cantidadEntradas`.
- **`MOROSO`** con `permiteMorosos = false` o `montoMorosos ≤ 0` → **403**, y queda un registro `BLOQUEADO` en el historial.
- **`ACTIVO`** con `evento.monto > 0` → cobra `monto × cantidadEntradas` (un socio al día también puede pagar entrada si el evento tiene precio general).
- **`ACTIVO`** con `monto` en `0`/`null` → **409** "El socio no está moroso" (no hay nada que cobrarle; el acceso gratuito se resuelve en `/scan`).
- **`INHABILITADO` / `SUSPENDIDO` / `INACTIVO`** → **403**.

**`payload` cuando `tipo = "INVITADO"`** (`EventoAccesoInvitadoCommand`):

| Campo | Obligatorio | Notas |
|---|---|---|
| `canalCobro` | Sí | `MANUAL` \| `MERCADO_PAGO` |
| `cantidadEntradas` | No | ≥1, default 1 |
| `medioPago` | Solo en `MANUAL` | |
| `fecha`, `observaciones` | No | |
| `nombre` | No | Nombre libre del invitado — se guarda en el historial (no hay `Member`) |

Requiere `evento.permiteInvitados = true` **y** `evento.montoInvitados > 0`; si no, **403** + registro `BLOQUEADO` en el historial. Si pasa, cobra `montoInvitados × cantidadEntradas`.

**Response — `EventoAccesoCobroResultDTO`** (unificado, reemplaza a `EventoAccesoInvitadoResultDTO`/`EventoAccesoMorosoCobroResultDTO`):

| Campo | Notas |
|---|---|
| `eventoUuid`, `cantidadEntradas`, `monto`, `concepto`, `movimientoUuid`, `qr` | Igual que antes |
| `tipo` | `SOCIO` \| `INVITADO` |
| `resultado` | `EventoAccesoScanEstadoEnum`: **`ACCESO_PERMITIDO`** si `canalCobro = MANUAL` (ya cobrado), **`DEBE_PAGAR`** si `canalCobro = MERCADO_PAGO` (pago pendiente vía QR) |
| `memberUuid`, `nombre`, `apellido`, `numeroSocio` | Solo cuando `tipo = SOCIO` |

En `MERCADO_PAGO`, `qr` trae el Base64 y `movimientoUuid` viene `null` hasta que se apruebe el webhook (igual que antes).

Todo cobro exitoso — y todo bloqueo por `permiteMorosos`/`permiteInvitados` en `false` — queda registrado en el historial del evento.

### `POST .../historial/paginate` — `EventoAccesoHistorialPaginateRequestDTO`

Nuevo. Historial de todos los scans/cobros de un evento (uno por cada llamada a `/scan` o `/cobro` exitosa).

**Body opcional:** `page` (default 1), `size` (default 10, máx 20), `search` (LIKE sobre `nombre`), `resultado` (`EventoAccesoScanEstadoEnum`: `ACCESO_PERMITIDO` \| `DEBE_PAGAR` \| `BLOQUEADO` — filtro exacto).

**Response — `Page<EventoAccesoHistorialDetalleDTO>`:**

| Campo | Notas |
|---|---|
| `uuid` | |
| `tipo` | `EventoAccesoHistorialTipoEnum`: `SOCIO` \| `INVITADO` |
| `nombre` | Nombre del socio o del invitado en el momento del evento (snapshot, no se actualiza si el socio cambia de nombre después) |
| `resultado` | `EventoAccesoScanEstadoEnum`: `ACCESO_PERMITIDO` \| `DEBE_PAGAR` \| `BLOQUEADO` |
| `motivo` | Texto explicativo, `null` en accesos permitidos sin contexto adicional |
| `monto` | `null` cuando `resultado = BLOQUEADO` |
| `memberUuid`, `numeroSocio` | Solo cuando `tipo = SOCIO` |
| `fecha` | Timestamp del registro (`dateCreated`) — mismo caveat de formato sin `@JsonFormat` que arriba |

Ordenado por `dateCreated` descendente.

---

## 20. Mercado Pago

### Configuración

Las credenciales (`accessToken`, `publicKey`, `webhookSecret`) se cargan **por club** desde `PUT /sysadmin/clubs/{companyUuid}/mercado-pago` (SYSADMIN). Se guardan cifradas.

### Flujo end-to-end

1. SYSADMIN configura las credenciales del club.
2. El staff o el titular inicia un cobro con `canalCobro = MERCADO_PAGO` (cuotas, reserva de amenity o entrada a evento).
3. El backend crea una preferencia y devuelve `mercadoPagoInitPoint` (cuotas) o `qr` Base64 (reservas / entradas), más un `MercadoPagoCobroIntent` en estado `PENDIENTE`.
4. El usuario paga en Checkout Pro.
5. MP notifica a `POST /mercado-pago-webhook/{companyUuid}`.
6. El backend valida la firma (`x-signature`), consulta el pago y **liquida**: marca las cuotas como `PAGADA` / confirma la reserva y registra el movimiento de tesorería.

**Hasta el webhook aprobado, `movimientoUuid` viene `null`** y la reserva no queda confirmada.

### Webhook — `POST /mercado-pago-webhook/{companyUuid}`

**Pública** (la llama Mercado Pago). Validada por firma.

| Entrada | Origen |
|---|---|
| `companyUuid` | Path — identifica el club |
| `x-signature`, `x-request-id` | Headers |
| `data.id` o `data_id`, `type` | Query params |
| Body | `MercadoPagoWebhookNotificationDTO` (opcional) |

Responde siempre **200** para que MP no reintente indefinidamente.

Toda la interacción queda trazada en `mercado_pago_traza` (`MercadoPagoTrazaEtapaEnum`: desde `COBRO_INICIADO` hasta `LIQUIDACION_*`).

### Consulta de un pago — `GET /pago/{externalReferenceMp}`

JWT + `X-Company-Uuid`. Pensado para la pantalla de retorno de MP (`/pago/exito|fallo|pendiente`), que recibe el `external_reference` por query param.

**Response — `PagoDetalleDTO`:** `uuid`, `externalReference`, `titulo`, `detalle`, `montoTotal`, `tipo` (`CUOTA` | `OTRO` | `ACCESO_CLUB` | `RESERVA_AMENITY`), `estado` (`PENDIENTE` | `APROBADO` | `RECHAZADO` | `CANCELADO`), `paymentId`, `items[]`.

Cada ítem (`PagoItemDTO`): `uuid`, `titulo`, `detalle`, `monto`, `cuotaUuid` (si aplica).

> En cobros tipo `CUOTA`, `titulo` y `detalle` del cobro y de los ítems pueden venir `null` (solo se completa `detalle` del ítem con el concepto de la cuota). En `ACCESO_CLUB` y `RESERVA_AMENITY` vienen siempre poblados.

**404** si la external reference no existe o pertenece a otro club.

---

## 21. Portal del socio

Rutas bajo `/portal/member`, `/portal/cuota`, `/portal/amenities`, `/portal/calendario`.

Requiere JWT de un usuario vinculado a un `Member` del club (`SOCIO_TITULAR` o `FAMILIAR`) + `X-Company-Uuid`. **El socio se resuelve por sesión** — no se pasa su UUID en la URL.

Un socio en `INHABILITADO`, `SUSPENDIDO` o `INACTIVO` **no puede usar el portal** (403).

### Perfil y credencial — `/portal/member`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/portal/member/credencial` | `portal.card.view` | Carnet digital con QR |
| `GET` | `/portal/member/grupo` | `portal.family.view` | Grupo familiar con credenciales — **solo titular** |
| `GET` | `/portal/member/perfil` | `portal.card.view` | Perfil propio |
| `PUT` | `/portal/member/perfil?memberUuid=` | `portal.profile.edit` | Actualiza el perfil propio o el de un familiar |
| `POST` | `/portal/member/uuid/{memberUuid}/certificado` | `portal.certificado.upload` | Sube certificado médico |

**`PortalMemberCredencialDTO`:** `userUuid`, `qr` (PNG Base64; el payload del QR es el `userUuid`), `member` (`MemberDTO`, con `certificados = null` acá).

Mostrar en el front como `data:image/png;base64,{qr}`.

El QR se genera **en el momento de mostrar el carnet**, no se guarda como imagen. En esta etapa es infraestructura para usos futuros de validación — el único consumo operativo hoy es el escaneo en `/acceso-club` y `/evento-acceso/{uuid}/scan`.

**`GET /portal/member/grupo` → `PortalMemberGrupoDTO`:** `titularMemberUuid` + `integrantes[]` (cada uno un `PortalMemberCredencialDTO` completo, con QR y con `member.certificados[]` poblado).

**No lista integrantes con `estado = INACTIVO`**; el resto de los estados sí aparece. Es distinto de `GET /portal/cuota/grupo`: este devuelve credenciales, el otro cuotas.

**`PUT /portal/member/perfil` — `MemberPortalProfileCommand`:** solo `nombre`, `apellido`, `fechaNacimiento` (ISO, pasada), `sexo` y `telefono`. **Sin** email, DNI, número de socio ni foto.

Acepta `memberUuid` como **query param opcional**: si se envía, actualiza a ese socio en lugar del logueado. **403** si el logueado no es titular o si el `memberUuid` no pertenece a su grupo familiar; **404** si no existe en el club.

### Cuotas — `/portal/cuota`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/portal/cuota/mias` | `portal.fee.view` | Cuotas del socio logueado |
| `GET` | `/portal/cuota/grupo` | `portal.fee.view` **y** `portal.family.view` | Desglose del grupo — **solo titular** |

Query params opcionales: `estado` (repetible, `CuotaEstadoEnum`), `tipo` (repetible, `CuotaTipoEnum`). `/mias` acepta además `ultimosMeses` (ej. `6` para el historial).

`/grupo` valida **ambos** permisos en llamadas separadas (por lo tanto es un AND real) y exige que el socio sea titular → **403** para un familiar.

**Cobro Mercado Pago desde el portal:** el titular usa `POST /treasury/cuota/cobro` con `canalCobro = MERCADO_PAGO` (`portal.fee.pay_mercado_pago`).

### Amenities — `/portal/amenities`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/portal/amenities` | `portal.amenities.view` | Amenities activos del club |
| `POST` | `/portal/amenities/reservas/paginate` | `portal.amenities.view` | Mis reservas |
| `POST` | `/portal/amenities/{amenityUuid}/reservar` | `portal.amenities.reserve` | Crea la reserva **y** cobra la seña o el total, en un solo paso |
| `POST` | `/portal/amenities/reservas/{reservaUuid}/pago` | `portal.amenities.reserve` | Genera un **nuevo** intento de pago Mercado Pago para una reserva propia ya existente |

Los cuatro exigen ser **titular** y estar habilitado para el portal → **403** para un familiar.

El paginado devuelve **solo las reservas del socio de la sesión**: el member sale del token, no del body, así que no hay forma de pedir las de otro socio (ni las de familiares del grupo). Body opcional: `page` (default 1), `size` (default 10, máx. 100). Sin filtros por estado/amenity — para eso está el paginado de backoffice.

### `POST /portal/amenities/{amenityUuid}/reservar` — self-service

Único endpoint de alta de reserva accesible al socio (para el flujo operado por staff ver [§15](#15-amenities-y-reservas)). Orquesta en una sola llamada transaccional lo que en backoffice son dos pasos (`POST /amenities/reservas` + `.../cobro-sena` o `.../cobro-total`).

**Request — `PortalReservaAmenityAltaCommand`:**

```json
{ "fechaDesde": "2026-06-09T14:00:00", "fechaHasta": "2026-06-09T16:00:00", "tipoPago": "SENA" }
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `fechaDesde` | Sí | ISO datetime, sin `@JsonFormat` |
| `fechaHasta` | Sí | ISO datetime |
| `tipoPago` | Sí | `ReservaAmenityTipoPagoInicialEnum`: `SENA` \| `TOTAL` |

No lleva `memberUuid` ni datos de cobro: el titular se resuelve por sesión y el canal de cobro se fuerza siempre a `MERCADO_PAGO` — el socio **no puede** pagar en efectivo desde el portal.

**Reglas de negocio:** las mismas que `POST /amenities/reservas` de backoffice (solo titular, tope semanal, solapamiento, horario/día del amenity — ver [§15](#15-amenities-y-reservas)).

**Efectos:** idénticos al flujo de staff — `tipoPago: SENA` equivale a crear la reserva + `cobro-sena`; `tipoPago: TOTAL` equivale a crear la reserva + `cobro-total`. Igual que en backoffice, `cobro-total` solo aplica si la reserva no tenía seña previa (acá siempre es así, porque se crea en el mismo paso), y como el pago es Mercado Pago, el estado real (`CONFIRMADA`, slot `CONFIRMADO`) recién se aplica cuando llega el webhook — la respuesta de este endpoint no lo espera.

**Response — `ReservaAmenityCobroResultDTO`:** igual que el cobro de backoffice — `reservaUuid`, `tipoCobro` (`SENA` | `TOTAL`), `monto`, `concepto`, `movimientoUuid` (`null` hasta el webhook), `qr` (checkout Mercado Pago en Base64).

**Errores:** 400 si `fechaDesde`/`fechaHasta` violan alguna regla de negocio (tope semanal, solapamiento, horario); 403 si el socio no es titular o no está habilitado para el portal; 404 si el `amenityUuid` no existe o no está activo.

Guía de integración para el front: [`docs/PORTAL-AMENITIES-RESERVAR-FRONT.md`](PORTAL-AMENITIES-RESERVAR-FRONT.md).

### `POST /portal/amenities/reservas/{reservaUuid}/pago` — reintentar/regenerar pago

Genera un **nuevo** intento de pago Mercado Pago para una reserva del socio logueado que ya existe (por ejemplo, si el checkout de `/reservar` se abandonó, expiró o simplemente se quiere reintentar). No crea una reserva nueva — opera sobre el `reservaUuid` recibido en el path.

**Ownership:** a diferencia de `/reservar` (que nunca recibe un `reservaUuid` externo), este endpoint sí lo recibe del cliente — el backend valida que la reserva pertenezca al titular logueado; **403** si es de otro socio.

**Request — `PortalReservaAmenityPagoCommand`:**

```json
{ "tipoPago": "SENA" }
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `tipoPago` | Depende del estado | `ReservaAmenityTipoPagoInicialEnum`: `SENA` \| `TOTAL`. **Obligatorio** mientras la reserva sigue `PENDIENTE_SENA` sin seña pagada (el sistema no recuerda qué eligió el socio la vez anterior). **Se ignora** si la reserva ya está `CONFIRMADA` con saldo pendiente — en ese caso el único pago posible es el saldo, y el body puede omitirse por completo |

**Qué genera según el estado actual de la reserva:**

| Estado de la reserva | Qué cobra | Requiere `tipoPago` |
|---|---|---|
| `PENDIENTE_SENA`, sin seña pagada | Seña o total, según `tipoPago` | Sí |
| `CONFIRMADA`, `saldoPendiente > 0` | El saldo | No (se ignora si se envía) |
| `CONFIRMADA`, `saldoPendiente = 0` | — | **409** — no hay nada pendiente |
| `CANCELADA` / `VENCIDA` | — | **409** — la reserva no admite un nuevo pago |

Reutiliza exactamente `ReservaAmenityCobroService.registrarSena` / `.registrarSaldo` / `.registrarTotal` — mismas reglas de negocio y misma respuesta que el cobro de backoffice.

**Response — `ReservaAmenityCobroResultDTO`:** igual que `/reservar` — nuevo `qr` en Base64, `movimientoUuid: null` hasta que el webhook confirme el pago.

**Errores:** 400 si falta `tipoPago` estando `PENDIENTE_SENA`; 403 si la reserva no pertenece al titular logueado o si no está habilitado para el portal; 404 si el `reservaUuid` no existe; 409 si el estado de la reserva no admite un nuevo pago.

⚠️ No hay limpieza automática de intentos de pago viejos: si se generan varios pagos pendientes para la misma reserva (por ejemplo, reintentando varias veces), la liquidación del webhook es idempotente — el primero que se apruebe confirma la reserva; los siguientes que lleguen aprobados no duplican el movimiento ni rompen el estado (ver `MercadoPagoLiquidacionReservaAmenityStrategy`). Igualmente, evitar mostrarle al socio más de un checkout abierto a la vez para no generar pagos duplicados del lado de Mercado Pago.

### Actividades — `/portal/member/actividad`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `POST` | `/portal/member/actividad/inscripcion` | `portal.activities.enroll` | Inscribe al socio logueado o a un familiar de su grupo en una actividad |
| `POST` | `/portal/member/actividad/inscripcion/baja` | `portal.activities.unenroll` | Da de baja la inscripción del socio logueado o de un familiar de su grupo |
| `GET` | `/portal/member/actividad/inscripcion` | `portal.activities.view` | Actividades activas en las que está inscripto el socio logueado |
| `GET` | `/portal/member/actividad/inscripcion/familiar/{memberUuid}` | `portal.activities.view` | Actividades activas en las que está inscripto un familiar del grupo |

Solo el rol `SOCIO_TITULAR` tiene los permisos `portal.activities.enroll`/`portal.activities.unenroll` (no se otorgan a `FAMILIAR`) → **403** si el logueado es familiar. Los dos `GET` de listado, en cambio, usan `portal.activities.view`, que **sí** tienen ambos roles (`SOCIO_TITULAR` y `FAMILIAR`).

### `POST /portal/member/actividad/inscripcion` — self-service

Único endpoint de alta de inscripción accesible al socio desde el portal (para el flujo operado por staff ver [§11](#11-actividades-e-inscripciones)). Resuelve el socio destino y delega en la misma lógica de negocio que `POST /actividad/{actividadUuid}/inscripcion`.

**Request — `PortalActividadInscripcionCommand`:**

```json
{ "actividadUuid": "…", "memberUuid": null }
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `actividadUuid` | Sí | UUID de la actividad |
| `memberUuid` | No | `null`/omitido → inscribe al titular logueado. Si se envía, debe pertenecer al **mismo grupo familiar** que el titular logueado — **403** si no |

El titular logueado se resuelve por sesión (JWT); no hay forma de inscribir a un socio de otro grupo familiar.

**Reglas de negocio:** las mismas que el flujo de backoffice (ver [§11](#11-actividades-e-inscripciones)) — rechaza duplicado, actividad `completa`/sin cupo, socio no `ACTIVO`, edad fuera del rango de la categoría.

**Response — `InscripcionActividadDTO`:** igual que el flujo de backoffice (`inscripcionUuid`, datos de actividad/socio, `fechaInscripcion`, `actividadCompleta`, `cuotaGenerada`, `cuotaUuid`, `cuotaAdvertencia`).

**Errores:** 400 reglas de negocio (duplicado, sin cupo, socio no `ACTIVO`, edad); 403 si el `memberUuid` enviado no pertenece al grupo del titular logueado, o si el logueado es un familiar (no tiene el permiso); 404 si `actividadUuid` no existe o el socio resuelto no existe en el club.

Guía de integración para el front: [`docs/PORTAL-ACTIVIDADES-INSCRIPCION-FRONT.md`](PORTAL-ACTIVIDADES-INSCRIPCION-FRONT.md).

### `POST /portal/member/actividad/inscripcion/baja` — self-service

Cancela una inscripción vigente, para el titular logueado o un familiar de su grupo. Mismo body que el alta (`PortalActividadInscripcionCommand`); reutiliza la lógica de negocio de `POST /actividad/{actividadUuid}/inscripcion/baja` de backoffice (ver [§11](#11-actividades-e-inscripciones)).

**Request:**

```json
{ "actividadUuid": "…", "memberUuid": null }
```

Mismas reglas de resolución de `memberUuid` que el alta: `null`/omitido → titular logueado; si se envía, debe pertenecer al mismo grupo familiar — **403** si no.

**Efectos:** soft-delete de la inscripción activa (`status = -1`); si queda cupo libre, `actividadCompleta` vuelve a `false`. **No anula cuotas ya generadas.**

**Response — `InscripcionActividadDTO`:** igual que el alta.

**Errores:** 403 si el `memberUuid` no pertenece al grupo del titular logueado o si el logueado es familiar (no tiene el permiso); 404 si `actividadUuid` no existe o el socio resuelto no tiene una inscripción activa en esa actividad.

Guía de integración para el front: [`docs/PORTAL-ACTIVIDADES-BAJA-INSCRIPCION-FRONT.md`](PORTAL-ACTIVIDADES-BAJA-INSCRIPCION-FRONT.md).

### `GET /portal/member/actividad/inscripcion` — mis inscripciones

Lista las inscripciones **activas** (`status = 0`, actividad no dada de baja) del socio logueado — titular o familiar, cualquiera de los dos puede llamarlo con su propio JWT.

Sin body ni query params. El socio sale de la sesión.

**Response 200 — `InscripcionActividadDTO[]`:** mismo DTO que el alta/baja (`inscripcionUuid`, `actividadUuid`, `actividadNombre`, `memberUuid`, `memberNombre`, `memberApellido`, `fechaInscripcion`, `actividadCompleta`). Como es un listado, no hay cobro de por medio: `cuotaGenerada`, `cuotaUuid` y `cuotaAdvertencia` siempre viajan `null`/`false`. Puede devolver `[]`.

### `GET /portal/member/actividad/inscripcion/familiar/{memberUuid}` — inscripciones de un familiar

Mismo listado que el anterior, pero para un integrante del grupo familiar distinto del logueado. `memberUuid` va en el **path**, no en query.

**403** si `memberUuid` no pertenece al mismo grupo familiar que el socio logueado (mismo criterio que en el alta/baja de inscripción — se resuelve por `titular_id` compartido, no importa si quien llama es el titular o un familiar). **404** si el `memberUuid` no existe en el club.

Guía de integración para el front: [`docs/PORTAL-ACTIVIDADES-VER-INSCRIPCIONES-FRONT.md`](PORTAL-ACTIVIDADES-VER-INSCRIPCIONES-FRONT.md).

### Calendario — `POST /portal/calendario`

Ver [§14](#14-calendario). Permiso `portal.activities.view`; devuelve solo las actividades con inscripción activa del socio.

---

## 22. Portal del profesor

Rutas bajo `/portal/profesor`. Requiere JWT del profesor + `X-Company-Uuid`, y que el profesor esté `ACTIVO`.

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| `GET` | `/portal/profesor/credencial` | `portal.card.view` | Carnet con QR |
| `GET` | `/portal/profesor/perfil` | Solo autenticado | Perfil propio |
| `PUT` | `/portal/profesor/perfil` | Solo autenticado | Actualiza el perfil propio (`StaffProfileUpdateCommand`) |
| `POST` | `/portal/profesor/calendario` | `activity.view` | Calendario de sus actividades asignadas |
| — | `/portal/profesor/evento-actividad/**` | Ver [§12](#12-eventos-de-actividad) | ABM de eventos de sus actividades |
| — | `/portal/profesor/asistencia/**` | Ver [§13](#13-asistencia-a-actividades) | Planilla y registro de asistencia |

**`PortalProfesorCredencialDTO`:** `userUuid`, `qr`, `profesor` (`ProfesorDTO`).

---

## 23. Jobs automáticos

Todos los cron son configurables por property. Los defaults están en el código; `application.properties` puede sobrescribirlos.

| Job | Property | Default | Qué hace |
|---|---|---|---|
| Generación de cuotas de actividad | `tesoreria.cuota-actividad.cron` | `0 30 0 * * *` (00:30 diario) | Genera las cuotas de actividad pendientes **hasta hoy** según el puntero `proximaGeneracion` de cada inscripción activa |
| Generación de cuotas sociales | `tesoreria.cuota-social.cron` | `0 0 0 * * *` (00:00 diario) | Genera las cuotas sociales pendientes **hasta hoy** según el puntero `proximaGeneracionCuota` de cada socio elegible, en clubes con cuota social configurada |
| Vencimiento de cuotas | `tesoreria.cuota-vencimiento.cron` | `0 0 1 * * *` (01:00 diario) | Marca `PENDIENTE → VENCIDA` las cuotas cuyo `vencimiento` ya llegó, y recalcula morosidad/inhabilitación |
| Vencimiento de reservas de amenity | `amenities.reserva-vencimiento.cron` | `0 0 * * * *` (cada hora) | Reserva sin seña a las 48 h → `VENCIDA` y **borra el slot físicamente** |
| Extensión de slots de actividad | `actividades.slot-extension.cron` | `0 0 1 * * *` (01:00 diario) | Mantiene la ventana de slots recurrentes hacia adelante |
| Vencimiento de certificados médicos | `certificados.certificado-medico-vencimiento.cron` | `0 0 2 * * *` (02:00 diario) | `CONFIRMADO → VENCIDO` cuando `fechaExpiracion` quedó atrás; **borra el archivo del storage**. No toca `PENDIENTE_VALIDACION` |
| Heartbeat de presencia en vivo | — | Cada 10 s | Mantiene abiertos los `SseEmitter` de `/acceso-club/live` |

Ninguno de los dos jobs de cuotas es "del día 1": corren a diario y generan lo que el puntero de cada inscripción o socio marque como pendiente hasta hoy. Si un puntero quedó atrasado, se generan todos los ciclos pendientes de una vez. Los días en que no hay nada pendiente, no hacen nada.

**Trigger manual:** `POST /treasury/cuota/generacion/actividad?hasta=dd/MM/yyyy` y `POST /treasury/cuota/generacion/social?hasta=dd/MM/yyyy` (rol `ADMIN`) hacen lo mismo que el job, acotado al club del header y con la fecha de corte que se indique (ver [§16](#16-tesorería--cuotas)).

Todos los jobs son **idempotentes**: re-ejecutarlos no duplica cuotas ni corrompe estados. Lo garantiza el puntero, que avanza en la misma transacción que crea la cuota. Como red de seguridad, las unique keys `uk_cuotas_actividad_fecha_gen` sobre `(company_id, persona_id, actividad_id, fecha_generacion)` y `uk_cuotas_social_fecha_gen` sobre `(company_id, persona_id, fecha_generacion)` impiden dos cuotas **no anuladas** con la misma fecha.

---

## 24. Catálogo de enums

En JSON viajan como **string** con el nombre exacto del enum (mayúsculas, guion bajo).

| Enum | Valores |
|---|---|
| `AccesoClubTipoEnum` | `INGRESO`, `EGRESO` |
| `AsistenciaActividadEstadoEnum` | `PRESENTE`, `AUSENTE` |
| `CalendarioSlotTipoEnum` | `ACTIVIDAD`, `AMENITY` |
| `CanalCobroEnum` | `MANUAL`, `MERCADO_PAGO` |
| `CertificadoMedicoEstadoEnum` | `PENDIENTE_VALIDACION`, `CONFIRMADO`, `VENCIDO` |
| `CuotaEstadoEnum` | `PENDIENTE`, `PAGADA`, `VENCIDA`, `ANULADA` |
| `CuotaOrdenEnum` | `VENCIMIENTO_ASC`, `VENCIMIENTO_DESC`, `PERSONA_APELLIDO` |
| `CuotaOrigenEnum` | `ALTA`, `INSCRIPCION`, `CICLO_MENSUAL` |
| `CuotaTipoEnum` | `SOCIAL`, `ACTIVIDAD` |
| `DiaSemanaEnum` | `LUNES`, `MARTES`, `MIERCOLES`, `JUEVES`, `VIERNES`, `SABADO`, `DOMINGO` (acepta también los nombres en inglés) |
| `EventoActividadEstadoEnum` | `CONFIRMADO`, `CANCELADO` |
| `EventoAccesoHistorialTipoEnum` | `SOCIO`, `INVITADO` |
| `EventoAccesoScanEstadoEnum` | `ACCESO_PERMITIDO`, `DEBE_PAGAR`, `BLOQUEADO` |
| `FacturaEstadoEnum` | `PENDIENTE`, `PAGADA`, `VENCIDA` |
| `FacturaOrdenEnum` | `FECHA_VENCIMIENTO_ASC`, `FECHA_VENCIMIENTO_DESC` |
| `MedioPagoEnum` | `EFECTIVO`, `TRANSFERENCIA`, `MERCADO_PAGO`, `OTRO` |
| `MemberEstadoEnum` | `ACTIVO`, `MOROSO`, `INHABILITADO`, `SUSPENDIDO`, `INACTIVO` |
| `MemberRegistroTipoEnum` | `SOCIO_TITULAR`, `FAMILIAR` |
| `MercadoPagoCobroIntentEstadoEnum` | `PENDIENTE`, `APROBADO`, `RECHAZADO`, `CANCELADO` |
| `MercadoPagoCobroTipoEnum` | `CUOTA`, `OTRO`, `ACCESO_CLUB`, `RESERVA_AMENITY` |
| `MovimientoEstadoEnum` | `ACTIVO`, `ANULADO` |
| `MovimientoOrigenEnum` | `COBRO_CUOTA`, `COBRO_RESERVA`, `MANUAL` |
| `MovimientoTipoEnum` | `INGRESO`, `EGRESO` |
| `ParentescoEnum` | `CONYUGE`, `HIJO`, `HIJA`, `PADRE`, `MADRE`, `OTRO` |
| `ProfesorEstadoEnum` | `ACTIVO`, `INACTIVO` |
| `RecepcionistaEstadoEnum` | `ACTIVO`, `INACTIVO` |
| `SexoEnum` | `MASCULINO`, `FEMENINO`, `OTRO` (campo `sexo` de socios, profesores, recepcionistas y admins) |
| `ReservaAmenityCobroTipoEnum` | `SENA`, `SALDO`, `TOTAL` |
| `ReservaAmenityTipoPagoInicialEnum` | `SENA`, `TOTAL` (solo `PortalReservaAmenityAltaCommand`) |
| `ReservaEstadoEnum` | `PENDIENTE_SENA`, `CONFIRMADA`, `CANCELADA`, `VENCIDA` |
| `SlotEstadoEnum` | `PENDIENTE_DE_PAGO`, `CONFIRMADO`, `CANCELADO` |

**`diasSemana`** no es un enum en el DTO sino un **string** con tokens separados por `;`. Ej.: `"LUNES;MIERCOLES;VIERNES"`.

---

## 25. Matriz de permisos por rol

Permisos definidos en `Code/Auth/PermissionEnum`; asignación a roles en `Code/Auth/RoleEnum`. `ADMIN` y `SYSADMIN` vienen del Core y tienen acceso amplio vía el registry de permisos.

| Permiso | `ADM_RECEPCION` | `PROFESOR` | `SOCIO_TITULAR` | `FAMILIAR` |
|---|:--:|:--:|:--:|:--:|
| `dashboard.view` | ✅ | — | — | — |
| `member.view` | ✅ | — | — | — |
| `member.create` | ✅ | — | — | — |
| `member.edit` | ✅ | — | — | — |
| `member.change_status` | ✅ | — | — | — |
| `member.certificado.upload` | ✅ | — | — | — |
| `member.certificado.change_status` | ✅ | — | — | — |
| `family.create` | ✅ | — | — | — |
| `family.edit` | ✅ | — | — | — |
| `family.convert_to_member` | — | — | — | — |
| `treasury.view` | ✅ | — | — | — |
| `treasury.collect` | ✅ | — | — | — |
| `treasury.expense` | ✅ | — | — | — |
| `treasury.void` | — | — | — | — |
| `treasury.debt_consolidated` | ✅ | — | — | — |
| `treasury.notify_delinquent` | ✅ | — | — | — |
| `treasury.access_switch` | — | — | — | — |
| `activity.view` | ✅ | ✅ | ✅ | — |
| `activity.create` | ✅ | ✅ | — | — |
| `activity.edit` | ✅ | ✅ | — | — |
| `activity.delete` | ✅ | ✅ | — | — |
| `activity.enroll` | ✅ | — | — | — |
| `activity.unenroll` | ✅ | — | — | — |
| `activity.view_enrolled` | — | ✅ | — | — |
| `activity.attendance.manage` | — | ✅ | — | — |
| `activity.attendance.view` | ✅ | — | — | — |
| `categoria.view` / `create` / `edit` / `delete` | ✅ | ✅ | — | — |
| `cuota_social_rango.view` / `create` / `edit` / `delete` | ✅ | — | — | — |
| `amenity.view` / `create` / `edit` / `delete` | ✅ | — | — | — |
| `amenity.reservation.create` | ✅ | — | — | — |
| `amenity.reservation.collect` | ✅ | — | — | — |
| `amenity.reservation.cancel` | ✅ | — | — | — |
| `club.configuration` | — | — | — | — |
| `club.internal_user.manage` | — | — | — | — |
| `club.access.register` | ✅ | — | — | — |
| `club.access.view` | ✅ | — | — | — |
| `configuration.view` / `configuration.create` | — | — | — | — |
| `portal.card.view` | — | ✅ | ✅ | ✅ |
| `portal.fee.view` | — | — | ✅ | ✅ |
| `portal.fee.pay_mercado_pago` | — | — | ✅ | — |
| `portal.activities.view` | — | — | ✅ | ✅ |
| `portal.activities.enroll` | — | — | ✅ | — |
| `portal.activities.unenroll` | — | — | ✅ | — |
| `portal.amenities.view` | — | — | ✅ | — |
| `portal.amenities.reserve` | — | — | ✅ | — |
| `portal.family.view` | — | — | ✅ | — |
| `portal.family.create` | — | — | ✅ | — |
| `portal.profile.edit` | — | — | ✅ | ✅ |
| `portal.certificado.upload` | — | — | ✅ | ✅ |

Los permisos sin ✅ en ninguna columna (`treasury.void`, `club.configuration`, `club.internal_user.manage`, `configuration.*`, `family.convert_to_member`, `treasury.access_switch`, `factura.view`, `factura.pagar`) quedan **exclusivamente para `ADMIN` / `SYSADMIN`** vía el registry del Core.

`cuota_social_rango.delete` no puede eliminar el rango marcado `esDefault = true` (el catch-all sembrado por migración) — ver [§10.1](#101-rangos-de-cuota-social).

### Observaciones sobre la matriz

- **`PROFESOR` tiene ABM completo de actividades y categorías** (`activity.create/edit/delete`, `categoria.*`). El documento funcional §3.7 dice que el profesor «no puede editar actividades». La restricción real es **por controller**, no por permiso: los endpoints de backoffice de actividades no filtran por rol, así que un profesor con esos permisos puede operarlos. ⚠️ Divergencia entre el diseño funcional y la implementación.
- **`family.convert_to_member`** está declarado pero **no hay endpoint** que lo use: la conversión de familiar a titular (§3.5 del documento funcional) **no está implementada**.
- **`treasury.access_switch`** y **`club.configuration`** están declarados sin endpoint que los valide: el switch configurable de acceso a tesorería (§2.5) **no está implementado**.
- **`treasury.notify_delinquent`** está declarado pero no hay endpoint de notificación por WhatsApp (§2.6) — es un placeholder de front.

---

## 26. Integración CRM (blue-crm)

`bsports-club` es un "sistema externo" dado de alta en el CRM (`blue-crm`), con integración en
dos direcciones:

- **Saliente:** al dar de alta un club (`POST /sysadmin/clubs`, [§3](#3-sysadmin--alta-de-clubes))
  se crea el cliente en el CRM dentro de la misma transacción, y se espeja el plan que ese CUIT ya
  tuviera. Suscribir a un plan nuevo es un request aparte
  (`POST /sysadmin/clubs/{companyUuid}/crm/suscribir`).
- **Entrante:** el CRM le notifica eventos a `bsports-club` vía `POST /webhookCrm`. Esos eventos
  son la única forma en la que se crean y actualizan las `Factura` que expone
  `POST /factura/paginate`.
- **Saliente (pago):** `POST /factura/pago` le pide al CRM que genere el link de pago (Mercado
  Pago) de una o más facturas del club.

Además, un club con alguna `Factura` en `VENCIDA` puede tener el acceso a la plataforma cortado
— ver [Bloqueo de acceso por facturación vencida](#bloqueo-de-acceso-por-facturación-vencida) al
final de esta sección.

### `POST /webhookCrm`

Receptor de los webhooks del CRM. Url completa: `{host}/api/webhookCrm` — es el valor que hay que
cargar como `webhook_url` del sistema externo `bsports-club` dentro del CRM (que exige `https`).

**No usa JWT de usuario.** El header `Authorization: Bearer <token>` se valida comparando el
token **byte a byte** contra el que el CRM emitió para este sistema externo — ver
[Apéndice](#apéndice--endpoints-sin-validación-de-permiso).

**Body** — sobre común de todos los eventos:

```json
{ "eventId": "...", "tipo": "DEUDA_GENERADA", "data": { ... } }
```

| Campo | Notas |
|---|---|
| `eventId` | Identificador del evento en el CRM. Es la clave de idempotencia: un mismo `eventId` reintentado no se reprocesa si ya quedó `PROCESADO` (si quedó `ERROR`, sí se reintenta) |
| `tipo` | `ALTA_CLIENTE`, `PAGO_IMPUTADO`, `DEUDA_GENERADA`, `PLAN_CAMBIO` o `PRODUCTO_CAMBIO` |
| `data` | Contenido según `tipo`, ver tabla abajo |

**Respuesta:** `200` apenas valida el token y encola el evento para procesarlo de forma
asincrónica — no espera a que termine de procesarse. `401` si el token es inválido. Toda entrega,
aceptada o rechazada, queda registrada en `crm_integracion_logs` (incluidos los `401` repetidos).

El resultado del procesamiento se le confirma al CRM aparte, con un ACK que sale de
`bsports-club` hacia el CRM (tráfico saliente, no forma parte de esta API).

**`data` por tipo de evento:**

| Tipo | Campos de `data` | Efecto en `bsports-club` |
|---|---|---|
| `ALTA_CLIENTE` | `uuidCliente`, `razonSocial`, `cuit`, `correoFacturacion` (obligatorios); `codigoCondicionImpositiva` y `estadoRegistro` se ignoran | Crea el club parcial: `onboardingFinalizado = false`, `emailFacturaElectronica` = `correoFacturacion` y `path` generado desde la razón social (sin tildes, minúsculas, sin `.`, el resto de caracteres no alfanuméricos → `-`; sufijo `-2`, `-3`, … si ya existe). Le provisiona los roles y le asocia las facturas que hayan llegado sin club. Después, en una transacción aparte, crea el usuario `ADMIN` con email/username = `correoFacturacion` y contraseña = `cuit`; si eso falla, el club queda con `adminCreado = false` y el evento se confirma igual. Si ya existe un club con ese `uuidCliente` o `cuit`, o falta un campo obligatorio, el evento queda en `ERROR` |
| `DEUDA_GENERADA` | `uuidCliente` (eventos viejos: `cuit`), `uuidFactura`, `codigosPlanes[]`, `codigosProductos[]`, `importeTotal` | Crea una `Factura` en `PENDIENTE` para el club de ese `uuidCliente` (si no viene o no hay club con ese uuid, por `cuit`). Si el club no existe todavía, la factura se guarda sin club con `uuidCliente`/`cuit` y se le asocia al darse el alta. El `importe` es el `importeTotal` del CRM; si no viene, se resuelve sumando el precio (con IVA aplicado si corresponde, ver más abajo) del plan + el de cada producto (`FacturaService.resolverImporte`, contra `plan_crm`/`productos_crm`). Si ya existe una factura con ese `uuidFactura`, se omite sin error (idempotente) |
| `PAGO_IMPUTADO` | `numeroFactura`, `uuidFactura`, `uuidCliente` (eventos viejos: `cuit`), `codigosPlanes[]`, `codigosProductos[]` | Busca la `Factura` por `uuidFactura` y la marca `PAGADA`, aunque el club no exista. Si encuentra el club (por `uuidCliente`, o por `cuit` como respaldo) genera además un `Movimiento` `EGRESO` local por el `importe` de la factura, atribuido al `ADMIN` del club. Si no existe la factura (ej. llegó antes que el `DEUDA_GENERADA` correspondiente), el evento queda en `ERROR` y el CRM reintenta con backoff |
| `PLAN_CAMBIO` | Mismo formato que un ítem de `obtenerPlanes` del CRM | Upsert por `codigo` en la tabla cache local `plan_crm` (`CrmCatalogoSyncService.actualizarPlan`) — mantiene el catálogo local al día sin esperar al cron |
| `PRODUCTO_CAMBIO` | Mismo formato que un ítem de `obtenerProductos` del CRM | Upsert por `codigo` en `productos_crm` (`CrmCatalogoSyncService.actualizarProducto`) |

`numeroFactura` sirve solo para el ACK: puede caer al `uuid` de la factura si el CRM no le asignó
número todavía, así que no es apto para buscarla — la búsqueda siempre es por `uuidFactura`.

### `POST /factura/paginate`

Listado paginado de las facturas del club, generadas y actualizadas por los webhooks de arriba.

- **Permiso:** `factura.view` (solo `ADMIN` / `SYSADMIN` — ver [§25](#25-matriz-de-permisos-por-rol))

**Resolución de la company** — a diferencia del resto de los endpoints de backoffice, no siempre
sale del header:

| Rol | Cómo se resuelve |
|---|---|
| `ADMIN` | `X-Company-Uuid` (igual que el resto de los endpoints, vía `getCurrentCompanyOrThrow()`) |
| `SYSADMIN` | `companyUuid` del **body** — no tiene una company propia asociada al header. **400** si falta, **404** si el uuid no corresponde a un club activo |

**Request — `FacturaPaginateRequestDTO`:**

| Campo | Obligatorio | Notas |
|---|---|---|
| `page`, `size` | No | Default `1` / `10`, tope `100` |
| `estados[]` | No | `FacturaEstadoEnum` — sin filtro trae todos los estados |
| `orden` | No | `FacturaOrdenEnum`, default `FECHA_VENCIMIENTO_DESC` |
| `companyUuid` | Condicional | Obligatorio solo para `SYSADMIN` |

**Response — `Page<FacturaPaginateRowDTO>`:** `facturaUuid`, `uuidFacturaCrm`, `codigoPlan`,
`codigosProductos[]`, `importe`, `fechaGeneracion` (`dd/MM/yyyy`), `fechaVencimiento`
(`dd/MM/yyyy`), `estado`.

`codigoPlan` puede ser `null`: una deuda puede originarse solo en productos, sin plan asociado.

`importe` también puede ser `null`: es la suma del precio del plan y de los productos, resuelta
contra `plan_crm`/`productos_crm` **al momento de crear la factura**; si ninguno de esos códigos
estaba sincronizado todavía, queda sin importe y no se recalcula después.

Cada precio se suma **con IVA aplicado cuando corresponde**
(`PlanCrm.precioFinal()`/`ProductoCrm.precioFinal()`): si `precioEsFinal = true` (solo existe para
planes) o no hay `alicuotaIva` informada, se usa el precio tal cual sincroniza el catálogo; si no,
se le suma `precio * alicuotaIva / 100` (redondeado a 2 decimales). Replica exactamente el cálculo
que hace el propio CRM al facturar (`PlanPrecioService.calcularPrecioEfectivo`), porque
`plan_crm`/`productos_crm` guardan el precio **crudo** que informa `obtener-planes`/
`obtener-productos`, no el final.

### `POST /factura/pago`

Le pide al CRM que cree el pago (Mercado Pago) de una o más facturas del club y devuelve la
preferencia creada, **tal cual** la responde el CRM.

- **Permiso:** `factura.pagar` (solo `ADMIN` / `SYSADMIN`)
- **Company:** siempre sale del header `X-Company-Uuid` (`getCurrentCompanyOrThrow()`) — a
  diferencia de `/factura/paginate`, este endpoint **no** acepta `companyUuid` en el body.

**Request — `FacturaPagoRequestDTO`:**

| Campo | Obligatorio | Notas |
|---|---|---|
| `facturasUuid[]` | Sí, ≥1 | Uuids **locales** de `Factura` — los mismos que devuelve `POST /factura/paginate` como `facturaUuid`. Todas deben pertenecer al club del header |
| `referencia` | Sí* | Referencia externa del pago para el CRM. La define el front |
| `fechaPago` | Sí* | Fecha del pago (`yyyy-MM-dd`) |

\* El CRM las exige; `FacturaController` no las valida localmente, el 400 llega desde el CRM si
faltan.

**Response — `MercadoPagoPreferenceResponseDTO`:**

```json
{ "id": "...", "init_point": "https://..." }
```

Sin envolver: es el mismo shape que devuelve el CRM. Redirigir a `init_point`, igual que con
`mercadoPagoInitPoint` en el resto de los flujos de Mercado Pago (ver [§20](#20-mercado-pago)).

**Lo que hace `FacturaService.crearPago` antes de llamar al CRM:**
1. Resuelve las `facturasUuid` locales contra la company del header — **404** si alguna no existe
   o es de otro club (la búsqueda ya filtra por `company`, así que nunca se filtra por accidente
   una factura ajena).
2. Traduce cada uuid local al uuid de factura **del CRM** (`Factura.uuidFacturaCrm`) — el CRM no
   conoce los uuids locales.
3. Arma las tres URLs de retorno de Mercado Pago con el mismo dominio que ya usan los pagos de
   cuotas (`front.url`) pero un path propio: `{front.url}/pago-factura/exito`, `/fallo`,
   `/pendiente`.
4. Llama a `POST integraciones/pagos/crear` del CRM con `facturasUuid` (del CRM), `cuit`,
   `referencia`, `fechaPago` y las tres URLs, y devuelve la respuesta sin transformar.

**Nota:** el pago en sí no actualiza nada localmente en el momento — el cierre del círculo
(marcar la factura `PAGADA`) sigue pasando por el webhook `PAGO_IMPUTADO` de siempre.

### Catálogo local de planes y productos

`CrmCatalogoSyncService` sincroniza `CrmIntegracionService.obtenerPlanes()` /
`obtenerProductos()` contra dos tablas cache, `plan_crm` y `productos_crm`, por dos vías:

- **Al arrancar la aplicación** (`ApplicationReadyEvent`, `crm.catalogo-sync.on-startup`, default
  `true`): `plan_crm` nace vacía y sin el plan sincronizado la suscripción y el cambio de plan
  cortan con 409, así que un entorno recién levantado quedaría sin poder poner en marcha clubes
  hasta la corrida del cron. Corre con el servidor ya
  aceptando requests y nunca propaga: si el CRM no responde, se loguea y siguen el cron y los
  webhooks como red de contención.
- **Cron diario** (`CrmCatalogoSyncScheduler`, 02:00 — `crm.catalogo-sync.cron`): recorre todo el
  catálogo.
- **Webhooks `PLAN_CAMBIO`/`PRODUCTO_CAMBIO`** (ver tabla arriba): actualizan un plan/producto
  puntual en el momento en que cambia en el CRM, sin esperar al cron.

Es upsert por `codigo` (la clave natural del CRM): actualiza los que ya existen, crea los nuevos,
y **no** da de baja los que dejaron de venir en la respuesta del CRM. `PlanCrm.productosJson`
guarda como JSON la lista de productos incluidos en el plan (`codigo`, `nombre`, `unidades`) — es
solo cache de lectura, no se modela como tabla hija. Es de ahí de donde salen los cupos de socios
(`"bsports-socios"`, [§6](#6-socios-members)) y de administradores (`"bsports-admin"`,
[§9](#9-personal-interno--profesores-y-recepcionistas)). No hay endpoint para consultar estas
tablas todavía.

**Suscripción del club al plan:** al dar de alta un club, la respuesta de la suscripción del CRM
(uuid de suscripción, uuid de la factura inicial, estado, `fechaProximaFacturacion`) se guarda en
`CompanySuscripcionCrm` (1:1 con `Company`, FK a `PlanCrm`) — es contra esta tabla que
`MemberCupoService`/`AdminCupoService` resuelven qué plan tiene cada club. No hay endpoint para
consultarla directamente todavía.

`SuscripcionVerificacionService` la mantiene **espejada con el CRM**: en su corrida diaria ya pide
`obtenerPlanesCliente`, así que además de decidir el acceso refresca `plan_crm_id`, `estado` y
`fechaProximaFacturacion` con lo que el CRM informa (incluida una suscripción `ANULADA`). Sin
esto, un club que cambia de plan en el CRM conservaría el cupo del plan viejo para siempre. Dos
salvedades:

- **No crea la fila si falta** — un club sin `CompanySuscripcionCrm` no se autogenera una; y como
  el chequeo de cupo es fail-closed, ese club no puede dar de alta socios ni administradores.
- **No refresca si el plan no está en `plan_crm`** — deja la fila como estaba y loguea, para no
  apuntarla a un plan incompleto (un `PlanCrm` sin `productosJson` bloquearía el alta de socios).
  El catálogo se sincroniza solo, o a mano con `POST /sysadmin/jobs/catalogo-sync`.

Por el mismo motivo, la suscripción de un club **corta con 409** si el `codigoPlan` que devolvió
el CRM todavía no está sincronizado en `plan_crm`: antes se creaba un `PlanCrm` mínimo (solo
código y nombre) que dejaba al club sin poder dar de alta ningún socio hasta la siguiente
sincronización.

### Bloqueo de acceso por facturación vencida

Un club con al menos una `Factura` en `VENCIDA` puede tener bloqueado el acceso a la plataforma.
El estado vive en una tabla 1:1 con `Company` (`CompanyBloqueoAcceso`) y lo hace cumplir
`AccesoBloqueadoInterceptor`, que corre sobre **todos** los requests.

Un club **recién dado de alta también nace bloqueado** si el CRM no le conoce ningún plan (lo
normal: el alta no suscribe), si la suscripción que le conoce está `ANULADA`, o si está
`PENDIENTE_PAGO` y arrancó hace más de 10 días. Se destraba **en el momento** al suscribirlo con
`POST /sysadmin/clubs/{companyUuid}/crm/suscribir`, que si el club estaba bloqueado lo desbloquea
al terminar — no hay que esperar al job diario de vigencia. La tabla completa de casos está en
[§3](#3-sysadmin--alta-de-clubes).

**Una sola excepción de rol — `SYSADMIN`:** el interceptor lo deja pasar siempre, antes incluso de
resolver la company. Es quien administra los clubes desde el panel interno, incluido el de
suscribir a uno que justamente está bloqueado por no tener plan; si lo cortara, no habría forma de
destrabarlo. Para el resto corta con **403**: `ADMIN`, roles de club (recepción, etc.) y cualquier
usuario del club. Para ellos la única forma de destrabarlo es del lado del CRM — pagar la deuda
(ver más abajo) o que un sysadmin lo suscriba a un plan.

**Respuesta 403:**
```json
{
  "status": "FORBIDDEN",
  "userMessage": "El club tiene facturación vencida",
  "errores": [
    { "name": "motivo", "value": "FACTURA_VENCIDA", "message": "Regularice el pago en el CRM para recuperar el acceso a la plataforma." }
  ]
}
```
El front puede leer `errores[0].value === "FACTURA_VENCIDA"` para distinguir este 403 de uno de
permisos normal. El `value` es el mismo cualquiera sea el motivo del bloqueo (deuda vencida,
suscripción anulada o club sin plan): el flag es uno solo y no distingue causa.

**El alta pública de socios corta igual, pero no la corta el interceptor.** `POST
/member/public/titular` no tiene JWT, así que el interceptor no puede resolver la company y lo deja
pasar. El chequeo lo hace `MemberPublicRegistroService` antes de crear nada, contra el mismo flag y
devolviendo **el mismo 403** de arriba: para el front es indistinguible.

**Endpoints exceptuados** (siguen funcionando aunque el club esté bloqueado):

| Endpoint | Por qué |
|---|---|
| `POST /webhookCrm` | Es la única vía por la que puede llegar el `PAGO_IMPUTADO` que destraba el club |
| `POST /factura/paginate` | El club tiene que poder ver qué factura lo está bloqueando |
| `POST /factura/pago` | El club tiene que poder generar el link de pago para destrabarse |
| `POST /user/login`, `POST /user/refresh-token` | Sin esto, a un usuario bloqueado se le podría vencer la sesión sin forma de renovarla para llegar a los endpoints de arriba |

**Desbloqueo inmediato:** cuando llega un `PAGO_IMPUTADO` y ya no quedan facturas `VENCIDA` para
el club, se desbloquea en el momento (no hay que esperar a ningún job).

**Bloqueo — un solo job (`SuscripcionVerificacionService` / `SuscripcionVerificacionScheduler`),
diario 03:00** (`crm.suscripcion-verificacion.cron`). Dos fases por corrida:

1. Marca `Factura` `PENDIENTE → VENCIDA` por fecha, por lotes.
2. Por cada club gestionado por el CRM (tiene facturas **o** tiene fila en
   `company_suscripcion_crm`), resuelve el acceso.

La decisión de acceso, en orden — **el primer motivo que aplica corta**:

| # | Condición | Resultado |
|---|---|---|
| 1 | Tiene alguna `Factura` `VENCIDA` | **Bloquea** (no consulta al CRM: el motivo local alcanza) |
| 2 | El CRM no le conoce ninguna suscripción para este sistema externo | **Bloquea** |
| 3 | `estadoSuscripcion == ANULADA` | **Bloquea** |
| 4 | `fechaProximaFacturacion` + 2 días de gracia ya pasó | **Bloquea** |
| — | Ninguna de las anteriores | **Desbloquea** |

Antes esto eran **dos jobs separados** (03:00 facturas, 03:30 vigencia) sobre el mismo booleano
`bloqueado`, y se pisaban: el de facturas desbloqueaba a cualquier club sin `VENCIDA`, deshaciendo
los bloqueos por suscripción anulada del otro. Con una única decisión por club eso ya no puede
pasar: **para desbloquear tienen que cumplirse las dos condiciones** (sin deuda vencida *y*
suscripción vigente), y si el CRM falla para un club se lo deja como está — nunca se desbloquea
sobre información incompleta.

El caso que cubre la fase 2 y `Factura` no puede detectar por sí sola: el cliente que **se
desuscribe estando al día**. Sin deuda, ninguna factura vence.

### Disparo manual de los jobs — `POST /sysadmin/jobs/*`

Solo `SYSADMIN`. Cada endpoint ejecuta el mismo service que su scheduler, sin esperar al cron —
útil para soporte o para probar sin esperar al horario programado.

| Método | Ruta | Ejecuta | Response |
|---|---|---|---|
| `POST` | `/sysadmin/jobs/verificar-suscripciones` | `SuscripcionVerificacionService.verificarTodas(LocalDate.now())` | **200** — `SuscripcionVerificacionResumenDTO`: `facturasVencidas`, `clubesEvaluados`, `clubesBloqueados`, `clubesDesbloqueados`, `erroresCrm` |
| `POST` | `/sysadmin/jobs/catalogo-sync` | `CrmCatalogoSyncService.sincronizar()` | **204** |
| `POST` | `/sysadmin/jobs/verificar-cuotas-vencidas` | `CuotaVencimientoService.marcarVencidasGlobal(LocalDate.now())` | **200** — `CuotaVencimientoResultDTO`: `fechaCorte`, `companiesProcessed`, `cuotasMarcadasVencidas` |

---

## Apéndice — Endpoints sin validación de permiso

Endpoints que solo exigen JWT válido y club resuelto (`getCurrentCompanyOrThrow()`), sin `authService.valid(permiso)`:

| Endpoint | Nota |
|---|---|
| `POST /evento-acceso` | Alta de evento de acceso |
| `PUT /evento-acceso/{eventoUuid}` | Edición |
| `DELETE /evento-acceso/{eventoUuid}` | Baja lógica |
| `GET /evento-acceso/{eventoUuid}` | Detalle |
| `POST /evento-acceso/paginate` | Listado |
| `POST /evento-acceso/{eventoUuid}/historial/paginate` | Historial de scans/cobros |
| `GET /categoria` | Listado simple para combos |
| `GET /recepcionista/perfil` | Perfil propio — se resuelve por sesión |
| `PUT /recepcionista/perfil` | Ídem |
| `GET /portal/profesor/perfil` | Ídem |
| `PUT /portal/profesor/perfil` | Ídem |

Los de perfil propio son razonables (el recurso sale de la sesión). Los de `evento-acceso` **probablemente sean un hueco de autorización** — vale revisarlos.

Endpoints **completamente públicos** (sin JWT):

| Endpoint | Protección |
|---|---|
| `POST /user/login`, `/user/refresh-token`, `/user/public/**` | Flujos de auth del Core |
| `GET /companies/context` | Solo devuelve configuración de tenancy |
| `GET /companies/public/by-path/{path}` | Resuelve un club por su `path` público |
| `POST /member/public/titular` | Solo `X-Company-Uuid`; crea una solicitud pendiente |
| `GET /configuracion/get/{key}`, `POST /configuracion/get/list`, `GET /configuracion/all` | Solo variables con `global = true` |
| `GET /member/import/{importId}/progress` | SSE; protegido solo por lo impredecible del `importId` |
| `POST /mercado-pago-webhook/{companyUuid}` | Validado por firma `x-signature` |
| `POST /webhookCrm` | Sin JWT: validado por comparación byte a byte del token Bearer contra el del sistema externo en el CRM. Ver [§26](#26-integración-crm-blue-crm) |
