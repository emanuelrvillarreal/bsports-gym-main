---
producto: B Sport Clubes — vertical de clubes deportivos y sociales
version: "1.0 — Complementario definitivo"
fecha: Mayo 2026
estado: CERRADO para MVP — cambios requieren aprobación del product owner
origen: Documento Funcional Integral B Sport v1.1 + Borrador BSports Clubes (2026-05-20)
audiencia: Equipo de desarrollo · Líder técnico · Product owner
uso: Interno — referencia para agentes y desarrollo
---

# B Sport Clubes — Documento Complementario v1.0

> **PRECEDENCIA:** Este documento complementa y en algunos puntos **REEMPLAZA** definiciones del *Documento Funcional Integral v1.1* y del *borrador BSports Clubes*. En caso de conflicto, **prevalece este documento**. Cualquier versión anterior queda obsoleta en los puntos aquí definidos.

## Convenciones de callouts

| Etiqueta | Significado |
|---|---|
| **NUEVO en v2** | Regla o funcionalidad que no existía en el documento integral. Incorporada desde el borrador Clubes o definida ahora. |
| **REEMPLAZA doc anterior** | Esta definición reemplaza explícitamente lo que decía el documento integral o el borrador en ese punto. |
| **DECISIÓN** | Conflicto entre documentos resuelto por el product owner. Versión definitiva. |
| **CERRADO** | Decisión tomada, no se rediscute sin aprobación del product owner. |

---

## 1. Contexto y propósito

El documento funcional integral de B Sport (v1.1) definía **Club**, **Gimnasio** y **Cancha** como tipos dentro de una plataforma unificada. En paralelo, el equipo de producto generó un borrador específico para BSports Clubes que profundiza varios módulos y agrega reglas de negocio que el integral no tenía.

Este documento hace tres cosas:

- Incorpora al documento oficial las **11 reglas/funcionalidades nuevas** del borrador que no existían en el integral.
- Cierra los **8 conflictos** entre documentos con decisiones explícitas del product owner.
- Establece cuál versión de cada definición prevalece a partir de ahora.

> **DECISIÓN:** B Sport Clubes es la primera vertical prioritaria. El documento funcional integral sigue siendo válido para **Gimnasio** y **Cancha**. Para **Club**, prevalece este documento complementario.

---

## 2. Decisiones sobre conflictos entre documentos

Los siguientes 8 puntos estaban definidos de forma diferente en el integral y en el borrador. Se cierra cada uno con una decisión definitiva.

### 2.1 Alta de una nueva organización Club

> **REEMPLAZA doc anterior:** El integral definía un formulario de registro público self-service. Para Club esa definición queda reemplazada por la siguiente.

> **DECISIÓN:** El alta de una organización Club se realiza **exclusivamente desde un panel sysadmin interno de B Sport**. No existe registro público para clubes. El equipo de B Sport crea el club y su usuario Administrador inicial. El cliente recibe sus credenciales por el canal que defina B Sport (email, WhatsApp, etc.).

**Campos del alta:**

| Campo | Obligatorio | Descripción |
|---|---|---|
| Nombre del club | Sí | Nombre comercial o institucional. |
| Razón social | Sí | Denominación legal. |
| CUIT | Sí | Identificación fiscal. Formato validado. |
| Domicilio | Sí | Dirección institucional. |
| Teléfono | Sí | Teléfono institucional. |
| Email institucional | Sí | Correo del club. No es el email del admin. |
| Nombre del admin inicial | Sí | Nombre y apellido del primer Administrador. |
| Email del admin inicial | Sí | Email de acceso del primer Administrador. **Único en el sistema.** |
| Logo del club | Sí | Imagen institucional para branding. |
| Colores institucionales | Sí | Para la app y el carnet digital. |
| Cuota social habilitada | Sí | Booleano. Define si el club cobra cuota social fija mensual. |
| Importe de cuota social | Condicional | Obligatorio si cuota social está habilitada. Monto positivo. |

> **CERRADO:** Sin registro público para clubes. Alta solo desde panel sysadmin. El integral queda obsoleto en este punto para Club.

### 2.2 Portal del socio titular en MVP

> **REEMPLAZA doc anterior:** El borrador Clubes excluía el portal del socio titular del MVP. Esta decisión lo incorpora.

> **DECISIÓN:** El socio titular **TIENE portal propio en el MVP**. Incluye: carnet digital con QR, estado de cuota del mes actual, historial de pagos de los últimos 6 meses, actividades inscriptas y vista del grupo familiar (resumen: nombre y estado de pago de cada integrante).

| Sección del portal | Qué ve el socio titular | Diferencia vs. familiar |
|---|---|---|
| Carnet digital | Foto, nombre, número de socio, DNI, categoría, club, badge Al día/Debe. QR con UUID. | Mismo contenido. El socio tiene número de socio; el familiar no. |
| Mi cuota | Badge del mes actual. Historial últimos 6 meses: mes, monto, concepto, fecha de cobro. | Igual que el familiar. |
| Mis actividades | Listado de actividades inscriptas: nombre, días, horario. | Igual que el familiar. |
| Mi grupo familiar | Integrantes del grupo: nombre, parentesco, estado (Activo/Moroso/etc.). | **El familiar NO ve el grupo.** Solo el titular ve este bloque. |
| Mi categoría | Nombre y descripción de la categoría. | Igual que el familiar. |

> **CERRADO:** El socio titular tiene portal en MVP con vista de grupo familiar. El familiar también tiene su portal, pero **SIN** ver el grupo ni datos del titular.

### 2.3 Nombre del rol operativo

> **REEMPLAZA doc anterior:** El integral usaba "Recepcionista". A partir de aquí se usa el nombre del borrador.

> **DECISIÓN:** El rol operativo se llama **"Administración / Recepción"** en toda la documentación, código y UI del módulo Club. "Recepcionista" queda obsoleto para este módulo. Para Gimnasio y Cancha se mantiene "Operador" o "Recepcionista" según sus documentos.

> **CERRADO:** Rol operativo de Club = **Administración / Recepción**. No negociable para Club.

### 2.4 Generación de cuotas

> **REEMPLAZA doc anterior:** El integral (Futsal MVP) usaba registro manual de pagos sin job automático. Para Club la lógica es diferente y más compleja.

> **DECISIÓN:** Las cuotas en Club se generan **automáticamente** mediante un job el **día 1 de cada mes**. Arquitectura definitiva para Club; no aplica a Futsal (que mantiene su lógica manual).

- El día 1 de cada mes el sistema genera las cuotas de cada persona **activa** (socios titulares y familiares activos).
- Las cuotas vencen el **día 10** del mismo mes.
- El pago de una cuota se registra **manualmente** por Administrador o Administración/Recepción cuando el socio paga.
- Personas en estado **Inhabilitado** NO generan nuevas cuotas (ver §3.5 / §3.2).
- Personas en **baja lógica (Inactivo)** NO generan cuotas.

> **Impacto técnico:** El backend necesita un scheduler que ejecute la generación el día 1 (cron job / tarea programada según el stack). El job debe ser **idempotente**: si corre dos veces el mismo día no debe generar cuotas duplicadas.

> **CERRADO:** Cuotas automáticas. Job el día 1. Vencimiento el día 10. Registro de pago siempre manual.

### 2.5 Acceso de Administración/Recepción a Tesorería

> **REEMPLAZA doc anterior:** Había contradicción entre el integral (configurable) y el borrador (acceso fijo).

> **DECISIÓN:** El acceso a Tesorería es **CONFIGURABLE** por el Administrador mediante un switch en Configuración. Por defecto al crear la organización: acceso a Tesorería **HABILITADO** para Administración/Recepción.

| Permiso de Tesorería | Acceso = ON | Acceso = OFF |
|---|---|---|
| Ver movimientos | Sí (ingresos, egresos, cobros) | No — la sección no aparece en su menú |
| Registrar cobros | Sí | No |
| Registrar egresos | **No** — solo el Administrador | No |
| Ver deuda consolidada | Sí | No |
| Anular movimientos | **No** — solo el Administrador | No |

> **CERRADO:** Switch configurable. Default ON. Solo el Administrador cambia el switch. Solo el Administrador puede registrar egresos y anular movimientos, independientemente del switch.

### 2.6 Notificación de morosos en MVP

> **REEMPLAZA doc anterior:** El borrador excluía notificaciones. El integral tenía un placeholder de WhatsApp.

> **DECISIÓN:** El MVP incluye un botón de notificación por WhatsApp como **placeholder funcional**: construye un link `wa.me` con el número del socio titular y un mensaje prellenado. Sin integración con API ni envío automático.

- El botón **"Notificar"** aparece en el panel de morosos junto a cada socio titular con deuda.
- Al clic, abre WhatsApp Web (o la app en mobile) con el número del titular y el mensaje prellenado:

  > Hola [Nombre del socio], te recordamos que tenés cuotas pendientes en [Nombre del Club] por $[Monto total]. Por favor acercate o contactanos para regularizar. ¡Gracias!

- El teléfono debe estar cargado en el perfil del socio. Si no tiene teléfono: botón **deshabilitado** con tooltip "Sin teléfono registrado".
- El sistema **NO** registra automáticamente que se envió la notificación. El operador puede agregar una observación manual.

> **CERRADO:** WhatsApp placeholder en MVP. Sin API ni envío automático. Notificación siempre al socio **TITULAR**, no al familiar.

### 2.7 Estado Inhabilitado — consecuencias

> **REEMPLAZA doc anterior:** El borrador definía inhabilitación por persona. La decisión del product owner lo modifica.

> **DECISIÓN:** Cuando UNA persona queda Inhabilitada (**4 cuotas vencidas impagas**), **TODO el grupo familiar queda bloqueado**: el titular y todos sus familiares pasan a Inhabilitado simultáneamente.

| Consecuencia | Alcance |
|---|---|
| Se detiene la generación de nuevas cuotas | Todas las personas del grupo. |
| Se dan de baja las inscripciones activas | Todas las personas del grupo. |
| Se bloquea el acceso al portal | Todas las personas del grupo. |
| No se pueden generar nuevas inscripciones | Todas las personas del grupo. |
| La deuda acumulada se mantiene visible | Sí — la deuda no desaparece. Queda en Tesorería. |
| Para regularizar | El titular debe pagar **TODA** la deuda del grupo. Al regularizar, todas vuelven a Activo. Las actividades requieren **reinscripción manual**. |

> ⚠️ **ATENCIÓN:** Es la diferencia más importante respecto al borrador. El borrador decía que la inhabilitación era por persona; la decisión es que es por **grupo familiar completo**.

> **CERRADO:** Inhabilitación = bloqueo del grupo familiar completo. Regularización = pago total de toda la deuda del grupo. Actividades requieren reinscripción manual post-regularización.

---

## 3. Funcionalidades nuevas incorporadas desde el borrador

Las siguientes 11 reglas no existían en el integral. Se incorporan como definiciones oficiales del módulo Club.

### 3.1 Ciclo completo de generación de cuotas

> **NUEVO en v2:** El integral no definía el ciclo de fechas. Se incorpora íntegro.

| Evento | Cuándo ocurre | Vencimiento | Aplica a |
|---|---|---|---|
| Primera cuota social | Al dar de alta la persona, si cuota social está habilitada. | 10 días desde el alta. | Persona recién creada. |
| Primera cuota de actividad | Al inscribir a la persona en una actividad. | 10 días desde la inscripción. | Persona recién inscripta. |
| Cuotas del ciclo mensual | Job automático el día 1 de cada mes. | Día 10 del mismo mes. | Todas las personas activas con cuota social y/o inscripciones activas. |

**Reglas adicionales del ciclo:**

- Cada cuota se asocia a **DOS** entidades: la **persona que la origina** (quien debe pagarla) y el **socio titular responsable** del pago (cabeza del grupo).
- Si el titular es el propio socio (sin grupo), ambas referencias apuntan a la misma persona.
- La vista de deuda en Tesorería muestra el total consolidado por titular con desglose por persona origen.
- **No se permiten pagos parciales en MVP.** Una cuota se paga completa o no se paga.
- La anulación de una cuota solo la hace el Administrador, con **motivo obligatorio**.

> **CERRADO:** Sin pagos parciales en MVP. Cuota: pago completo o anulación manual por Admin con motivo.

### 3.2 Reglas exactas de morosidad e inhabilitación

> **NUEVO en v2:** El integral no tenía umbrales numéricos.

| Estado | Cuándo se aplica | Quién es afectado | Consecuencias operativas |
|---|---|---|---|
| Moroso | **2+** cuotas vencidas impagas de la persona. | Solo la persona con deuda. | Mantiene inscripciones actuales. No puede tomar nuevas. |
| Inhabilitado | **4+** cuotas vencidas impagas de cualquier persona del grupo. | **TODO el grupo** (titular + familiares). | Se detienen cuotas. Se bajan inscripciones. Se bloquea el portal. |
| Activo (recuperado) | El titular paga toda la deuda del grupo. | Todo el grupo vuelve a Activo. | Cuotas se generan desde el próximo ciclo. Inscripciones requieren **reinscripción manual**. |

> **Nota técnica:** El cálculo de estado debe ejecutarse automáticamente cuando: se registra un pago, se vence una cuota (job día 10/11) y se regeneran cuotas (job día 1). No puede depender de actualización manual del operador.

### 3.3 Cuota social fija configurable por club

> **NUEVO en v2:** El integral trataba la cuota social como siempre presente. El borrador la hace opcional.

- Cada club tiene un parámetro **"Cuota social habilitada"** (booleano, configurable por el Administrador).
- **Habilitada:** al dar de alta una persona se genera automáticamente su primera cuota social. El ciclo mensual genera cuotas sociales para todas las personas activas.
- **Deshabilitada:** no se generan cuotas sociales. La cobranza depende exclusivamente de las cuotas de actividades.
- El Administrador puede cambiar el parámetro en cualquier momento. El cambio aplica **desde el próximo ciclo mensual** — no genera ni elimina cuotas del ciclo en curso.
- El importe es un **único valor por club**. No hay importes diferenciados por categoría para la cuota social (la diferenciación por categoría aplica a actividades).

> **CERRADO:** Cuota social: booleano por club. Importe único por club. Cambio aplica desde el próximo ciclo.

### 3.4 Vista de deuda consolidada por titular

> **NUEVO en v2:** No estaba en el integral.

- **Nivel 1 — Vista por titular:** monto total adeudado por el grupo familiar completo (cuotas del titular + de todos sus familiares).
- **Nivel 2 — Desglose por persona:** expandible desde la vista del titular; detalle de cada cuota impaga por persona origen, con concepto, monto, vencimiento y días de mora.
- Al registrar un cobro, el operador imputa el pago a cuotas específicas de una persona específica. El sistema actualiza solo el estado de esa persona — **no** regulariza automáticamente a otras del grupo.
- El botón "Notificar" envía el WhatsApp siempre al titular, incluyendo el **monto total del grupo**.

### 3.5 Conversión de familiar a socio titular

> **NUEVO en v2:** Completamente nueva.

Permite que un familiar existente pase a operar como socio titular sin crear un registro nuevo.

| Paso | Descripción |
|---|---|
| 1. Condición previa | El familiar debe estar **Activo**. No se puede convertir si está Inhabilitado, Moroso, Suspendido o en Baja lógica. |
| 2. Acción del Admin | El Administrador accede al detalle del familiar y selecciona "Convertir en socio titular". |
| 3. Confirmación | Modal con el impacto: el familiar dejará de pertenecer al grupo de su titular actual. |
| 4. Procesamiento | El sistema: (a) desvincula al familiar de su titular actual; (b) convierte el registro en socio titular; (c) asigna **número de socio nuevo**; (d) **conserva UUID, email de acceso, historial de pagos e historial de inscripciones**. |
| 5. Resultado | La persona opera como socio titular. Su acceso al portal sigue sin cambios. Su historial previo como familiar es visible en su nueva ficha. |

> **CERRADO:** Conversión conserva UUID, acceso, historial y continuidad operativa. Solo para familiares en estado **Activo**.

### 3.6 Branding del club

> **NUEVO en v2:** No estaba detallado en el integral.

- Se compone de: **nombre comercial**, **logo** (imagen) y **colores institucionales** (primario y secundario).
- Se aplica a dos superficies: la interfaz del **backoffice** del club (header, sidebar, colores de acción) y el **carnet digital** de socios y familiares.
- La configura el Administrador desde Configuración.
- Se gestiona por **valor actual** — no guarda histórico de cambios en MVP.
- El carnet digital muestra: logo del club, nombre comercial, datos de la persona (nombre, número de socio o ID, DNI, categoría), badge de estado de cuota y **QR con UUID**.

### 3.7 Ficha del Profesor como usuario interno

> **NUEVO en v2:** El integral mencionaba el rol pero no detallaba ficha ni acceso.

- El Profesor es un **usuario interno** del club (no es socio ni familiar).
- Lo da de alta el Administrador desde la sección Usuarios.
- Ficha con los mismos campos base que el socio: nombre, apellido, DNI, teléfono, email.
- El **email** es su identificador de acceso. Único en la plataforma.
- Accede con email y contraseña. Al ingresar ve solo **sus actividades asignadas** y el listado de inscriptos de cada una.
- **No** puede editar actividades, **no** puede inscribir/desinscribir personas y **no** tiene acceso a Tesorería en ningún caso.
- Si pasa a Inactivo, sus actividades se mantienen vigentes. La reasignación a otro profesor la hace el Administrador cuando corresponda.

> **CERRADO:** El Profesor puede VER sus actividades e inscriptos. No puede modificar nada. Sin acceso a Tesorería.

### 3.8 Estados de socios y familiares — tabla completa

> **NUEVO en v2:** El integral no consolidaba los estados con sus transiciones.

| Estado | Descripción | Transición posible | Impacto operativo |
|---|---|---|---|
| Activo | Estado operativo normal. | → Moroso (auto) · → Suspendido (manual) · → Inactivo (manual) | Toma nuevas inscripciones. Genera cuotas. Accede al portal. |
| Moroso | 2+ cuotas vencidas impagas. | → Activo (auto al pagar toda su deuda) · → Inhabilitado (auto al llegar a 4) | Mantiene inscripciones. No toma nuevas. Genera cuotas. Accede al portal. |
| Inhabilitado | 4+ cuotas vencidas en el grupo. | → Activo (auto al pagar TODA la deuda del grupo) | No genera cuotas. Se bajan inscripciones. Sin portal. **Todo el grupo bloqueado.** |
| Suspendido | Asignado manualmente por el Administrador. | → Activo (manual) | No toma nuevas inscripciones. Mantiene actuales. Sin portal. |
| Inactivo (baja lógica) | Baja voluntaria o administrativa. | → Activo (manual, reactivación) | No genera cuotas. No toma inscripciones. Sin portal. Historial conservado. |

> **Nota:** Las transiciones automáticas (Activo → Moroso → Inhabilitado → Activo) las ejecuta el sistema. Las manuales las ejecuta el Administrador o Administración/Recepción desde la ficha.

### 3.9 Reglas de inscripción a actividades por estado

> **NUEVO en v2:** El integral mencionaba restricciones pero no las tabulaba.

| Estado | ¿Toma nuevas inscripciones? | ¿Mantiene inscripciones existentes? |
|---|---|---|
| Activo | Sí | Sí |
| Moroso | No | Sí — mientras no llegue a Inhabilitado |
| Inhabilitado | No | No — se dan de baja automáticamente |
| Suspendido | No | Sí — las conserva mientras esté suspendido |
| Inactivo | No | No — al darse de baja se eliminan las inscripciones activas |

> **CERRADO:** Moroso conserva inscripciones pero no toma nuevas. Inhabilitado pierde todo. Al regularizar, reinscripción **siempre manual**.

### 3.10 Carnet digital del familiar con QR

> **NUEVO en v2:** El integral mencionaba el carnet pero no especificaba el QR ni su contenido.

- El carnet digital del familiar incluye un **código QR** en todas las versiones MVP.
- El QR codifica el **UUID del usuario** del familiar — no el DNI ni ningún dato personal adicional.
- El QR se genera **en el momento de mostrar el carnet**. No se guarda como imagen estática.
- En MVP el QR es base de infraestructura para futuros usos de validación (acceso al predio, control de asistencia). **Sin lógica operativa de validación** en esta etapa.
- El carnet del socio titular también tiene QR con el mismo formato (UUID del usuario).
- El carnet es una **vista en pantalla**. La descarga como PDF es **post-MVP**.

> **CERRADO:** QR en carnet desde MVP. Contiene UUID. Sin lógica de validación en MVP. Sin PDF en MVP.

### 3.11 Familiar con mora independiente del titular

> **NUEVO en v2:** Introducida por el borrador, incorporada con la modificación de la decisión §2.7.

La **morosidad** se acumula por persona; la **inhabilitación** afecta al grupo completo:

- Familiar con 2 cuotas vencidas → **ese familiar** pasa a Moroso. El titular no cambia por eso.
- Familiar moroso que llega a 4 cuotas → **TODO el grupo** (titular + familiares) pasa a Inhabilitado.
- Titular con 2 cuotas vencidas → **el titular** pasa a Moroso. Los familiares no cambian por eso.
- Titular moroso que llega a 4 cuotas → **TODO el grupo** pasa a Inhabilitado.
- Para regularizar el grupo: el titular debe pagar **TODA** la deuda acumulada del grupo (propias + de todos los familiares).

---

## 4. Matriz de roles y permisos — Club (versión definitiva)

> Reemplaza la matriz de roles del documento integral para el módulo Club.

| Módulo / Acción | Administrador | Adm. / Recepción | Profesor | Socio titular (portal) | Familiar (portal) |
|---|---|---|---|---|---|
| Ver dashboard | Sí | Sí | No | No | No |
| Ver listado de socios | Sí | Sí | No | No | No |
| Alta de socio | Sí | Sí | No | No | No |
| Editar datos de socio | Sí | Sí | No | No | No |
| Cambiar estado de socio | Sí | Sí | No | No | No |
| Alta de familiar | Sí | Sí | No | No | No |
| Editar datos de familiar | Sí | Sí | No | No | No |
| Convertir familiar a socio | Sí | No | No | No | No |
| Ver Tesorería | Sí | Configurable (switch) | No | No | No |
| Registrar cobros | Sí | Configurable (switch) | No | No | No |
| Registrar egresos | Sí | No | No | No | No |
| Anular movimientos | Sí | No | No | No | No |
| Ver deuda consolidada | Sí | Configurable (switch) | No | No | No |
| Notificar morosos (WhatsApp) | Sí | Configurable (switch) | No | No | No |
| CRUD de actividades | Sí | Sí | No | No | No |
| Inscribir persona a actividad | Sí | Sí | No | No | No |
| Desinscribir persona de actividad | Sí | Sí | No | No | No |
| Ver inscriptos de actividad | Sí | Sí | Sí (sus actividades) | No | No |
| Configuración del club | Sí | No | No | No | No |
| Gestionar usuarios internos | Sí | No | No | No | No |
| Cambiar switch de Tesorería | Sí | No | No | No | No |
| Portal: ver carnet propio | No | No | No | Sí | Sí |
| Portal: ver mi cuota | No | No | No | Sí | Sí |
| Portal: ver mis actividades | No | No | No | Sí | Sí |
| Portal: ver mi grupo familiar | No | No | No | Sí (ve el grupo) | **No** (no ve el grupo) |

---

## 5. Entidades y campos complementarios

> Incorpora las entidades/campos que el borrador agregó y que no estaban en el modelo del integral.

| Entidad | Campos nuevos o modificados | Versión |
|---|---|---|
| **Club / Organización** | `cuota_social_habilitada` (bool) · `importe_cuota_social` (decimal) · `nombre_comercial` · `logo_url` · `color_primario` · `color_secundario` · `sysadmin_creado` (bool, siempre true para Club) | Nuevo en Clubes |
| **Socio / Familiar (Persona)** | `estado` ampliado: `activo \| moroso \| inhabilitado \| suspendido \| inactivo` · `grupo_inhabilitado_at` (timestamp) · `uuid` (único, para QR del carnet) | Nuevo en Clubes |
| **Cuota** | `id` · `org_id` · `persona_id` (quién genera la deuda) · `titular_id` (responsable del pago) · `tipo` (`social \| actividad`) · `actividad_id` (nullable) · `mes` · `anio` · `monto` · `vencimiento` · `estado` (`pendiente \| pagada \| vencida \| anulada`) · `anulada_por` · `anulada_motivo` | Nuevo en Clubes |
| **Pago de cuota** | `id` · `org_id` · `cuota_id` · `persona_id` · `titular_id` · `monto` · `fecha_cobro` · `medio_pago` · `registrado_por` · `observaciones` | Nuevo en Clubes |
| **Profesor** | `id` · `org_id` · `nombre` · `apellido` · `dni` · `telefono` · `email` · `estado` (`activo \| inactivo`) · `fecha_alta` · `usuario_id` | Nuevo en Clubes |
| **Conversión familiar→socio** | Auditoría: `registro_id` · `fecha` · `usuario_que_ejecuto` · `titular_anterior_id` · `nuevo_numero_socio` | Nuevo en Clubes |

### 5.1 Jobs automáticos requeridos

| Job | Cuándo corre | Qué hace | Requisito técnico |
|---|---|---|---|
| Generación de cuotas mensuales | Día 1 de cada mes (00:00 o primer horario hábil). | Genera cuotas sociales y de actividad para todas las personas activas. Ignora Inhabilitadas e Inactivas. | **Idempotente** (si corre 2 veces no duplica). Log de ejecución. |
| Cálculo de morosidad e inhabilitación | Día 11 de cada mes (post-vencimiento) y al registrar/anular un pago. | Recalcula el estado de todas las personas con cuotas vencidas. Aplica transición a Moroso (2+) o Inhabilitado (4+ → grupo completo). | **Atómico por grupo familiar**: si un grupo se inhabilita, todos cambian de estado en la misma transacción. |
| Baja de inscripciones por inhabilitación | Inmediatamente al detectar inhabilitación del grupo. | Marca como inactivas todas las inscripciones activas de todas las personas del grupo. | Dentro de la misma transacción que la inhabilitación. |

---

## 6. Tabla de alcance definitiva — Club MVP

> Reemplaza la tabla de alcance del integral para el tipo Club. **Fuente de verdad definitiva.**

### En el MVP

| Funcionalidad | Fuente / Decisión |
|---|---|
| Alta de club desde panel sysadmin interno | Decisión 2.1 — reemplaza self-service del integral |
| Branding del club (logo, colores, nombre comercial) | Nuevo — §3.6 |
| Cuota social configurable (habilitada/no) | Nuevo — §3.3 |
| CRUD de socios titulares | Doc integral confirmado |
| CRUD de familiares vinculados a titular | Doc integral confirmado |
| Acceso al portal del familiar (carnet + actividades) | Doc integral + borrador confirmados |
| Acceso al portal del socio titular (carnet + cuota + actividades + grupo familiar) | Decisión 2.2 |
| Carnet digital con QR (UUID) para socio y familiar | Nuevo — §3.10 |
| Conversión de familiar a socio titular | Nuevo — §3.5 |
| Generación automática de cuotas (job día 1) | Decisión 2.4 — reemplaza manual del integral |
| Primera cuota al alta (vencimiento 10 días) | Nuevo — §3.1 |
| Cuota de actividad al inscribirse (vencimiento 10 días) | Nuevo — §3.1 |
| Deuda consolidada por titular con desglose por persona | Nuevo — §3.4 |
| Estados: Activo / Moroso / Inhabilitado / Suspendido / Inactivo | Nuevo — §3.8 |
| Morosidad automática (2 cuotas → Moroso) | Nuevo — §3.2 |
| Inhabilitación automática de grupo (4 cuotas → todo el grupo) | Decisión 2.7 — modificada del borrador |
| Regularización automática al pagar toda la deuda del grupo | Nuevo — §3.2 |
| Baja automática de inscripciones al inhabilitarse | Nuevo — §3.2 |
| Reinscripción manual post-regularización (no automática) | Nuevo — §3.2 |
| Sin pagos parciales en MVP | Nuevo — §3.1 |
| CRUD de actividades (comisiones: días, horario, lugar, cupo, profesores) | Doc integral confirmado |
| Inscripción de socios y familiares a actividades desde backoffice | Doc integral confirmado |
| Estado "Completa" automático cuando cupo = máximo | Doc integral confirmado |
| Sin lista de espera en MVP | Doc integral + borrador confirmados |
| Tesorería: ingresos (cobros) y egresos | Doc integral confirmado |
| Switch configurable de acceso a Tesorería para Adm/Recepción | Decisión 2.5 |
| Notificación de morosos por WhatsApp (placeholder) | Decisión 2.6 |
| Dashboard: socios activos, ingresos, egresos, balance, actividades del día | Doc integral + borrador confirmados |
| Accesos rápidos en dashboard (alta socio, registrar cobro, alta familiar) | Borrador confirmado |
| Ficha del Profesor como usuario interno | Nuevo — §3.7 |
| Panel sysadmin de B Sport (separado del backoffice del club) | Nuevo — decisión 2.1 |

### Fuera del MVP (Post-MVP)

| Funcionalidad | Estado |
|---|---|
| Reportes avanzados | Post-MVP — confirmado en ambos documentos |
| Notificaciones automáticas (email / WhatsApp automático) | Post-MVP — confirmado en ambos documentos |
| Reservas de amenities | Post-MVP — confirmado en ambos documentos |
| Portal de autogestión completa del socio (con pagos online) | Post-MVP — confirmado en ambos documentos |
| Mercado Pago | Post-MVP — confirmado en ambos documentos |
| QR con validación operativa (molinetes, acceso al predio) | Post-MVP — confirmado en ambos documentos |
| Multi-sede | Post-MVP — confirmado en ambos documentos |
| App móvil nativa | Post-MVP — confirmado en ambos documentos |
| Descarga de carnet como PDF | Post-MVP — Nuevo (§3.10) |

---

## 7. Roadmap post-MVP — Club

| Etapa | Funcionalidades | Prioridad |
|---|---|---|
| **MVP 1.1** (mejoras operativas) | Exportación básica a Excel (padrón de socios, morosos, movimientos de caja). Filtros avanzados en listados. Trazabilidad extendida de cambios de estado. | Media |
| **MVP 2** (reservas + permisos) | Módulo de reservas de amenities (parrillas, quinchos, salones) independiente de actividades. Permisos más granulares por usuario interno. Primeras salidas operativas fuera del core. | Media |
| **MVP 3** (portal completo + pagos) | Portal completo del socio con pagos online (Mercado Pago). Descarga de carnet como PDF. QR con validación operativa (registro de presencia en actividades). | Alta |
| **MVP 4** (accesos físicos + analítica) | Integración con hardware de acceso (QR en molinetes). Notificaciones automáticas de vencimiento por WhatsApp/email. Reportes analíticos y dashboards históricos. | Baja |

---

*B Sport Clubes — Documento Complementario v1.0 — Mayo 2026 — Uso interno.*
