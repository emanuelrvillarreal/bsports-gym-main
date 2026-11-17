# Convenciones Técnicas — Backend

Reglas de cumplimiento obligatorio para agentes de IA que trabajen en este repositorio. Si una situación no está cubierta explícitamente, el agente debe detenerse y preguntar antes de decidir por su cuenta.

## 1. Idioma y convenciones de texto

- Todo el código (entidades, columnas, funciones, endpoints, variables, comentarios) se escribe en español.
- No se usan tildes, eñes ni caracteres especiales del español dentro del código. Si una palabra lleva eñe, se reemplaza por "ne" (ej: `cumpleanos`).
- Tildes, eñes y caracteres especiales solo se permiten en textos visibles por el usuario final (mensajes, labels, etc).
- Se permiten anglicismos como convención de código (ej: `getUsuarios()`).

## 2. Control de versiones (Git)

- Flujo de ramas: `develop` -> `staging` -> `main`.
- Los commits deben ser lo más chicos posibles.
- El mensaje del commit debe incluir el código de la card de Jira de donde nace el cambio.
- El mensaje del commit debe explicar claramente qué se cambió.
- Antes de mergear `develop` -> `staging`:
  - Levantar Docker localmente.
  - Recrear la DB con Liquibase y cargar los seeds.
  - Ejecutar los test unitarios de Java.
  - Ejecutar los test de Bruno (obtención y modificación).
- Antes de pasar de `staging` -> `main`: repetir como mínimo los test de obtención de Bruno.

## 3. Versionado de la aplicación

- El backend debe tener versionado independiente, visible en `/info` (ver sección 6).
- La versión debe cambiar en cada push. Nunca se repite ni retrocede.

## 4. Manejo de fechas

- Todas las fechas se manejan en UTC-3, forzado siempre y validado al iniciar la aplicación.
- Se debe validar que la base de datos y el servidor estén en UTC-3 dentro de los smoke test.
- Comunicación con el frontend en formato ISO 8601 UTC-3. Ejemplo: `2026-08-06T10:46:00.123-03:00`.

## 5. Validación de datos (DTOs)

- Todos los campos esperados en los DTOs de los endpoints deben tener validaciones individuales (`@Valid` + validaciones por campo).
- Ante un dato inválido se debe responder status `400` con un mensaje de error personalizado por variable.

## 6. Integraciones externas

- Toda integración externa que dependa de otro contenedor, servidor o servicio (que pueda estar apagado o fallar) debe:
  - Validarse al iniciar la aplicacion como smoke test.
  - Ejecutarse dentro de un `try/catch`.

## 7. Variables de entorno

- Toda variable de entorno usada debe admitir `null` y tener una validación manual con mensaje de error personalizado.
  - Si la variable es necesaria para el correcto funcionamiento de la aplicacion debe cortar la ejecucion del java.

## 8. Validación de conexiones al iniciar

- Al iniciar la aplicación se debe validar el estado de las conexiones externas (base de datos, microservicios, APIs, etc).
- Ante un error de conexión se debe manejar con logs y alertas.
  - Si es necesario correcto funcionamiento de la aplicacion debe cortar la ejecucion del java.

## 9. Actuator y versión expuesta

- Actuator debe estar habilitado en todos los proyectos backend.
- Se deben exponer como mínimo los endpoints `/health` e `/info`.
- `/info` debe responder la versión actual de la aplicación (ver sección 3).
- Todas las respuestas HTTP deben incluir el header `API-Version` con la versión actual (formato ejemplo: `1.4.3`), coincidente con `/info`.

## 10. Base de datos (Liquibase)

- La base de datos nunca se edita a mano. Todo cambio de esquema se hace únicamente vía Liquibase.
- Cada proyecto debe contar con: schema básico, configuraciones para levantar la base, y seeds de ejemplo para pruebas.

## 11. Entidades

- Toda entidad debe tener un comentario que explique para qué sirve y con qué otras entidades se relaciona.
- Toda columna/variable cuyo nombre no sea explícito o pueda resultar ambiguo debe tener un comentario que especifique su función o valor.

## 12. Comentarios de código

- El estándar de colores de comentarios es la extensión de VSCode `aaron-bond.better-comments`.
- Toda función debe tener un comentario que explique: qué hace, de qué servicios externos depende, qué datos espera, qué datos responde.
- Todo DTO, interfaz y clase debe tener un comentario que explique su función dentro del proyecto.

## 13. Testing unitario

- Los test unitarios se usan solo para funcionalidades que no dependan de datos externos. Ejemplos: exportación de información, queries complejas, procesamiento de imágenes (compresión, cropeo), conversión de archivos (docs a pdf, etc).
- No se deben crear test unitarios de funcionalidades que dependan de información externa o del contexto de ejecución.

## 14. Testing con Bruno

- Los test de Bruno se separan en:
  - Test de obtención: no modifican datos en la DB.
  - Test de modificación: modifican datos en la DB.
- Los test de modificación nunca se ejecutan en producción.
- Los test de obtención se ejecutan antes de subir a staging y antes de pasar de staging a producción.

## 15. Documentación de endpoints (Bruno)

- Cada endpoint documentado en Bruno debe indicar: qué hace, qué espera, qué devuelve y sus casos de uso.
- Los casos de uso aplican cuando un mismo endpoint realiza distintas tareas (ej: crear o editar según lo que reciba) o responde distintos mensajes (ej: mensajes de error personalizados).
