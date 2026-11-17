# Guía frontend — Alta de clubes desde el CRM y limpieza del perfil del club

Esta guía cubre dos cambios que afectan al front:

1. **Se eliminan campos del perfil del club** (pago a proveedores, personas autorizadas Open Data
   y domicilio de entrega de facturas). Es un cambio **breaking**: los requests que los sigan
   mandando responden **400**.
2. **Los clubes también se dan de alta solos desde el CRM** (webhook `ALTA_CLIENTE`), con datos
   mínimos y un usuario administrador creado automáticamente. El front tiene que soportar clubes
   con el perfil incompleto.

El detalle completo está en [§3](API.md#3-sysadmin--alta-de-clubes) y
[§26 de `docs/API.md`](API.md#26-integración-crm-blue-crm).

## 1. Campos eliminados (breaking)

Se eliminan del club, de la base y de todos los DTOs:

| Campo | Tipo |
|---|---|
| `domicilioEntregaFacturas` | `{ direccion, codigoPostal, localidad }` |
| `telefonoPagoProveedores` | string |
| `responsablePagoProveedoresNombre` | string |
| `responsablePagoProveedoresApellido` | string |
| `responsablePagoProveedoresCargo` | string |
| `responsablePagoProveedoresEmail` | string |
| `diasAtencionPagoProveedores` | string |
| `personasAutorizadasOpenData[]` | `[{ nombre, apellido, email }]` |

Los datos que ya estaban cargados se pierden con la migración: no hay forma de recuperarlos
desde la API.

### Requests — hay que dejar de mandarlos

La API rechaza las propiedades que no conoce: **si el body trae alguno de estos campos, responde
400**. Hay que sacarlos de los formularios y de los bodies de:

| Endpoint | DTO |
|---|---|
| `POST /sysadmin/clubs` (alta de club por sysadmin) | `ClubRegistrationRequestDTO` |
| `PUT /companies/profile` (edición del perfil del club) | `ClubProfileUpdateRequestDTO` |

Quedan obligatorios en los dos: `nombreComercial`, `razonSocial`, `cuit`, `domicilio`,
`domicilioFacturacion`, `telefono`, `emailFacturaElectronica`, `emailInstitucional`, `path`,
`colorPrimario`, `colorSecundario`, `cuotaSocialHabilitada`. En el alta, además, `admin`,
`codigoCondicionImpositiva` y `codigoProvincia`.

Antes (`PUT /companies/profile`):

```json
{
  "nombreComercial": "Club Atlético Norte",
  "razonSocial": "Club Atlético Norte S.A.",
  "cuit": "20000000001",
  "domicilio": { "direccion": "Av. Libertador 1200", "codigoPostal": "1425", "localidad": "CABA" },
  "domicilioFacturacion": { "direccion": "Av. Corrientes 500", "codigoPostal": "1043", "localidad": "CABA" },
  "domicilioEntregaFacturas": { "direccion": "Av. Santa Fe 2000", "codigoPostal": "1123", "localidad": "CABA" },
  "telefono": "1140001000",
  "telefonoPagoProveedores": "1140002000",
  "responsablePagoProveedoresNombre": "Juan",
  "responsablePagoProveedoresApellido": "Pérez",
  "responsablePagoProveedoresCargo": "Tesorero",
  "responsablePagoProveedoresEmail": "tesoreria@catnorte.com",
  "diasAtencionPagoProveedores": "Lunes a Viernes 9 a 17 hs",
  "emailFacturaElectronica": "facturas@catnorte.com",
  "personasAutorizadasOpenData": [{ "nombre": "Ana", "apellido": "López", "email": "ana@catnorte.com" }],
  "emailInstitucional": "info@catnorte.com",
  "path": "club-atletico-norte",
  "colorPrimario": "#003366",
  "colorSecundario": "#FFCC00",
  "cuotaSocialHabilitada": true
}
```

Ahora:

```json
{
  "nombreComercial": "Club Atlético Norte",
  "razonSocial": "Club Atlético Norte S.A.",
  "cuit": "20000000001",
  "domicilio": { "direccion": "Av. Libertador 1200", "codigoPostal": "1425", "localidad": "CABA" },
  "domicilioFacturacion": { "direccion": "Av. Corrientes 500", "codigoPostal": "1043", "localidad": "CABA" },
  "telefono": "1140001000",
  "emailFacturaElectronica": "facturas@catnorte.com",
  "emailInstitucional": "info@catnorte.com",
  "path": "club-atletico-norte",
  "colorPrimario": "#003366",
  "colorSecundario": "#FFCC00",
  "cuotaSocialHabilitada": true
}
```

### Responses — dejan de venir

`CompanyDTO` ya no trae ninguno de esos campos. Afecta a todo lo que devuelve un club:

- `GET /companies/current`
- `PUT /companies/profile`
- `POST /companies/paginate-club`
- `POST /sysadmin/clubs` (`company` dentro de la respuesta)

Cualquier pantalla que los muestre (detalle del club, listado de sysadmin, formulario de
edición precargado) tiene que dejar de leerlos.

### Otro cambio en `POST /sysadmin/clubs`

Si el email del `admin` ya pertenece a otro usuario, ahora responde **400** con
`"Email ya registrado"` (antes fallaba con un error genérico).

## 2. Clubes dados de alta desde el CRM

Además del alta por sysadmin, un club ahora puede nacer solo: cuando un cliente paga la factura
inicial de su suscripción en el CRM, el CRM avisa con el webhook `ALTA_CLIENTE` y el backend
crea el club. No hay ningún endpoint nuevo para el front: el club simplemente aparece.

### Con qué datos nace

| Campo de `CompanyDTO` | Valor |
|---|---|
| `razonSocial` | La del CRM |
| `name` | Igual a `razonSocial` |
| `nombreComercial` | Viene igual a `razonSocial` (el backend la completa con `name` si está vacío) |
| `cuit` | El del CRM, 11 dígitos sin guiones |
| `emailFacturaElectronica` | El correo de facturación del CRM |
| `path` | Generado desde la razón social (ver abajo) |
| `adminCreado` | `true` si se pudo crear el admin; `false` si no |
| `domicilio`, `domicilioFacturacion`, `telefono`, `emailInstitucional`, `logoUrl`, `colorPrimario`, `colorSecundario`, `cuotaSocialHabilitada` | **`null`** |
| `periodicidadCuotaSocial`, `diasVencimientoCuota`, `diasVencimientoMoroso`, `diasInhabilitacion` | Defaults: `MENSUAL`, 10, 31, 91 |

El front tiene que tolerar estos `null`: en el header/branding (sin logo ni colores), en el
detalle del club y en el listado de sysadmin.

Para completar el perfil se usa el mismo `PUT /companies/profile` de siempre, que exige todos los
campos obligatorios de la sección 1.

### `path` generado

Se arma desde la razón social:

1. Sin tildes ni diacríticos (`á` → `a`, `ñ` → `n`).
2. Todo en minúsculas.
3. Los `.` se eliminan.
4. Cualquier otro carácter que no sea letra o número pasa a `-`; sin `-` en los extremos.
5. Si ya hay un club con ese `path`, se agrega un sufijo: `-2`, `-3`, …

| Razón social | `path` |
|---|---|
| `Club Atlético Norte S.A.` | `club-atletico-norte-sa` |
| `Club Atlético Norte S.A.` (ya existe) | `club-atletico-norte-sa-2` |
| `Peña & Amigos` | `pena-amigos` |

Es un `path` válido para `GET /companies/public/by-path/{path}` desde el primer momento, y se
puede cambiar después con `PUT /companies/profile`.

### Usuario administrador

Junto con el club se crea su usuario con rol `ADMIN`:

| Dato | Valor |
|---|---|
| Email / usuario | El correo de facturación del CRM (`emailFacturaElectronica` del club) |
| Contraseña | El CUIT del club (11 dígitos, sin guiones) |
| Nombre | La razón social |
| Apellido | `null` |

Ingresa por el login de siempre. Si la creación del admin falla (por ejemplo, porque ese email
ya es de otro usuario), el club se crea igual con `adminCreado = false` y sin administrador.

> La contraseña inicial es el CUIT, que es un dato público. Por ahora no hay cambio de
> contraseña obligatorio en el primer ingreso.

## Checklist

- [ ] Sacar los 8 campos de la sección 1 del formulario de alta de club (sysadmin).
- [ ] Sacar los mismos campos del formulario de edición del perfil del club.
- [ ] Dejar de leerlos de `CompanyDTO` en detalle, listado y precarga del formulario.
- [ ] Manejar el 400 `Email ya registrado` en el alta de club.
- [ ] Tolerar clubes con domicilios, teléfono, email institucional, logo, colores y
      `cuotaSocialHabilitada` en `null`.
