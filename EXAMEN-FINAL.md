# EXAMEN FINAL — JAVA & SPRING BOOT + MICROSERVICIOS

**Curso:** Desarrollo Backend con Java y Spring Boot
**Puntaje Total:** 210 puntos
**Modalidad:** Individual
**Fecha de entrega:** [Completar por el instructor]

---

## Introducción

**RapidoCourier S.A.C.** es una empresa peruana de mensajería y courier con sucursales en Lima, Arequipa y Cusco. Gestiona envíos de paquetes entre personas naturales y empresas, con tarifas que varían según el peso, el valor declarado y la ruta. Actualmente opera de forma completamente manual y necesita digitalizar su operación.

Dado su plan de expansión nacional, la empresa ha decidido construir su plataforma backend **directamente como un sistema de microservicios**, sin pasar por un monolito previo. La arquitectura debe ser distribuida desde el primer día.

**Tu tarea es diseñar e implementar este sistema de microservicios desde cero.**

### Libertad de diseño

Las siguientes decisiones son tuyas. Justifica cada una brevemente en el `README.md` del repositorio:

- **Microservicios:** tú defines cuántos servicios de negocio creas, qué responsabilidad tiene cada uno y cómo se llaman. Mínimo 3 servicios de negocio.
- **Base de datos por servicio:** cada microservicio puede usar la base de datos que consideres más apropiada (PostgreSQL, MySQL, MongoDB u otra). Está permitida la persistencia políglota. Justifica cada elección.
- **Arquitectura interna de cada servicio:** capas tradicional, hexagonal u otra. Debe ser consistente dentro de cada servicio.
- **Distribución de requerimientos:** tú decides en qué microservicio implementas cada requerimiento funcional, siempre que el sistema en conjunto los cumpla todos.
- **Comunicación inter-servicio:** sincrónica (Feign), asincrónica (eventos) o ambas. Justifica según el caso de uso.

### Convenciones del curso que debes respetar en todo el proyecto

- Spring Boot 3.x, Java 17, Maven.
- UUID como tipo de clave primaria en todas las entidades.
- Clase genérica `ApiResponse<T>` en todas las respuestas HTTP de todos los servicios.
- `application.yaml` (no `application.properties`).
- Inyección de dependencias por constructor (no `@Autowired` en campo).

### Estructura de repositorio esperada

```
rapidocourier/
├── eureka-server/
├── api-gateway/
├── config-server/
├── servicio-[nombre]/        ← al menos 3 servicios de negocio
├── servicio-[nombre]/
├── servicio-[nombre]/
├── RESPUESTAS-CONCEPTUALES.md
└── README.md
```

---

## Requerimientos Funcionales

El sistema en su conjunto debe cumplir todos los siguientes requerimientos. Tú decides en qué microservicio implementas cada uno.

**RF-01 — Registro de clientes**
El sistema debe permitir registrar un cliente usando su número de DNI. Al registrar, debe consultar la API de RENIEC (`https://api.decolecta.com/v1/reniec/dni`) para obtener y almacenar automáticamente el nombre completo del titular. El correo electrónico debe ser único entre todos los clientes.

**RF-02 — Registro de paquetes**
Un operador autenticado debe poder registrar un paquete asociándolo a un remitente y a un destinatario (ambos ya existentes como clientes). El sistema debe generar automáticamente un código de rastreo alfanumérico único. El paquete debe registrar el peso en kg, el valor declarado en soles, la sucursal de origen y la sucursal de destino.

**RF-03 — Cálculo de tarifa**
Al registrar un paquete, el sistema debe calcular y almacenar automáticamente la tarifa en soles. La regla de cálculo la defines tú, pero debe depender de al menos dos variables (peso, valor declarado, distancia entre sucursales o categoría del paquete). Documenta la regla en el `README.md`.

**RF-04 — Gestión de estados del paquete**
El sistema debe permitir actualizar el estado de un paquete. Las transiciones entre estados deben seguir una secuencia definida; intentar una transición inválida debe rechazarse con un error descriptivo. El estudiante define los estados y las transiciones válidas; deben estar documentados en el `README.md`.

**RF-05 — Historial de estados**
Cada cambio de estado de un paquete debe registrarse con el nuevo estado, la fecha y hora del cambio y el usuario que lo realizó. El sistema debe exponer un endpoint para consultar el historial completo de un paquete.

**RF-06 — Consulta por sucursal**
El sistema debe exponer un endpoint que retorne todos los paquetes asociados a una sucursal (como origen o destino), filtrables por estado actual.

**RF-07 — Búsqueda por texto**
El sistema debe exponer un endpoint para buscar paquetes por coincidencia parcial sobre el código de rastreo o sobre el nombre del remitente o destinatario.

**RF-08 — Control de acceso por roles**
El sistema debe tener al menos tres roles: `ADMIN`, `OPERADOR` y `CLIENTE`. Reglas mínimas: solo `ADMIN` puede eliminar registros; solo `ADMIN` y `OPERADOR` pueden crear o actualizar paquetes; `CLIENTE` solo puede consultar sus propios paquetes e historial.

**RF-09 — Categorías de paquetes**
El sistema debe permitir asignar una o más categorías a un paquete (frágil, refrigerado, documentos, sobredimensionado, etc.). Una categoría puede asignarse a múltiples paquetes.

---

## Requerimientos No Funcionales

Aplican a todos los microservicios del sistema y serán verificados durante la corrección.

**RNF-01 — Envoltorio de respuesta**
Toda respuesta HTTP (éxito y error) en todos los servicios debe usar la estructura genérica `ApiResponse<T>` con los campos `success` (boolean), `message` (String) y `data` (T, o null en error).

**RNF-02 — Códigos de estado HTTP**
200 para GET/PUT exitosos, 201 para POST exitoso, 204 para DELETE exitoso, 400 para validación fallida (con `Map<String, List<String>>` por campo), 404 para recurso no encontrado, 409 para conflictos o duplicados, 502 para fallo de API externa, 403 para acceso denegado, 500 para errores inesperados.

**RNF-03 — Validación de entrada**
Todos los DTOs de request deben estar validados con Bean Validation. Se deben usar mínimo 6 tipos de anotación distintos en todo el proyecto.

**RNF-04 — Campos de auditoría**
Al menos dos entidades (en cualquier servicio) deben incluir `createdAt` y `updatedAt` poblados automáticamente.

**RNF-05 — Pruebas unitarias**
Las pruebas de la capa de servicio deben correr sin errores con `mvn test` en cada microservicio que tenga pruebas. Deben cubrir: al menos un happy path por servicio probado, al menos dos rutas de excepción con `assertThrows`, y al menos un caso de resultado vacío.

**RNF-06 — Gestión de secretos**
El token de RENIEC, la clave de firma JWT y las contraseñas de base de datos no deben estar hardcodeados. En los microservicios, la clave JWT debe almacenarse en Vault; el token de RENIEC debe leerse con `@Value` desde el Config Server o desde `application.yaml`.

**RNF-07 — Documentación en README**
El `README.md` raíz debe incluir: mapa de microservicios con sus responsabilidades, modelo de datos por servicio, justificación de cada base de datos elegida, regla de cálculo de tarifa (RF-03), estados y transiciones del paquete (RF-04), e instrucciones para levantar el ecosistema completo localmente.

**RNF-08 — Todo el tráfico pasa por el Gateway**
Los clientes externos solo interactúan con el sistema a través del API Gateway. Los microservicios de negocio no deben ser accesibles directamente desde el exterior en el entorno de producción local (puertos no expuestos o protegidos).

---

## PARTE A — MICROSERVICIOS DE NEGOCIO (120 puntos)

> Estas secciones evalúan las habilidades de Spring Boot aplicadas dentro del contexto de microservicios. Los criterios se evalúan sobre el **conjunto de servicios**, no por servicio individual, salvo indicación contraria.

---

### Sección 1: Diseño e Identificación de Microservicios (10 puntos)

**Qué se evalúa:** Capacidad de identificar bounded contexts coherentes y de definir responsabilidades claras para cada servicio antes de implementar.

**Entregables:**

- Sección en el `README.md` que liste cada microservicio de negocio con:
  - Su nombre y el bounded context que representa.
  - Las entidades que gestiona.
  - Los RF que implementa.
  - La base de datos que usa y por qué.
  - Los otros servicios con los que se comunica y el tipo de comunicación (sincrónica / asincrónica).
- Mínimo **3 microservicios de negocio** con responsabilidades claramente diferenciadas y sin solapamiento de dominio.
- Diagrama o tabla en el `README.md` que muestre las dependencias entre servicios (quién llama a quién).

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| Mínimo 3 microservicios con bounded contexts diferenciados y documentados | 4 |
| Responsabilidades y RF asignados a cada servicio sin solapamientos evidentes | 3 |
| Diagrama o tabla de dependencias inter-servicio incluido en el `README.md` | 3 |

---

### Sección 2: Entidades y Base de Datos (20 puntos)

**Qué se evalúa:** Correcto modelado de datos dentro de cada servicio; cada servicio es dueño exclusivo de sus entidades. Cubre RF-01 al RF-09 en el nivel de modelo, y RNF-04.

**Entregables (evaluados sobre el conjunto de servicios):**

- Mínimo **4 entidades en total** distribuidas entre los servicios, cada una con anotaciones JPA o MongoDB completas.
- Cada microservicio gestiona sus propias entidades; **ninguna entidad JPA es compartida entre servicios** (la comunicación entre servicios usa DTOs o IDs, no entidades).
- Al menos **una relación `@OneToMany` / `@ManyToOne`** en alguno de los servicios, correctamente configurada.
- Al menos **una relación `@ManyToMany`** con tabla intermedia explícita en alguno de los servicios (cubre RF-09).
- Campos `createdAt` y `updatedAt` en al menos 2 entidades con gestión automática (cubre RNF-04).
- UUID como tipo de clave primaria en todas las entidades.
- Modelo de datos de cada servicio documentado en el `README.md`.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| Mínimo 4 entidades totales con anotaciones JPA / MongoDB completas y correctas | 7 |
| Ninguna entidad JPA compartida entre servicios; comunicación inter-servicio solo por DTOs o IDs | 4 |
| Relación `@OneToMany` / `@ManyToOne` correctamente configurada en al menos 1 servicio | 3 |
| Relación `@ManyToMany` con `@JoinTable` explícito en al menos 1 servicio (RF-09) | 3 |
| Campos `createdAt` y `updatedAt` en al menos 2 entidades con gestión automática (RNF-04) | 2 |
| Modelo de datos de cada servicio incluido en el `README.md` | 1 |

---

### Sección 3: DTOs y Validaciones (10 puntos)

**Qué se evalúa:** Separación entre capa de persistencia y API pública en cada servicio; validaciones declarativas. Cubre RNF-01 y RNF-03.

**Entregables:**

- DTOs separados en paquetes `request/` y `response/` en cada servicio.
- Clase genérica `ApiResponse<T>` implementada en cada servicio y usada en todos sus endpoints (cubre RNF-01).
- Mínimo **6 tipos de anotación de validación distintos** usados en los DTOs de request del proyecto. Ejemplos: `@NotBlank`, `@NotNull`, `@Email`, `@Pattern`, `@Positive`, `@Size`, `@Min`, `@Max`, `@Past`, `@Future` (cubre RNF-03).
- Al menos **un DTO de response con datos anidados** de otro servicio: un recurso cuyo response incluye información de una entidad de otro bounded context, obtenida mediante llamada inter-servicio.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| DTOs de request y response diferenciados en todos los servicios | 2 |
| `ApiResponse<T>` en todos los endpoints de todos los servicios (RNF-01) | 3 |
| Mínimo 6 tipos de anotación de validación distintos en los DTOs de request (RNF-03) | 3 |
| Al menos un DTO de response que incluya datos obtenidos de otro servicio mediante llamada inter-servicio | 2 |

---

### Sección 4: Repositorios (10 puntos)

**Qué se evalúa:** Uso correcto de Spring Data en cada servicio; consultas personalizadas para los requerimientos de búsqueda y filtro. Cubre RF-06 y RF-07.

**Entregables:**

- Una interfaz `Repository` por entidad principal en cada servicio, extendiendo `JpaRepository<E, UUID>` o `MongoRepository<D, UUID>`.
- Mínimo **3 consultas personalizadas en total** distribuidas entre los servicios, usando `@Query(nativeQuery = true)` para SQL o `@Aggregation` / `MongoTemplate` para MongoDB. Deben cubrir:
  1. Búsqueda por texto parcial sobre código de rastreo o nombre de cliente (cubre RF-07).
  2. Filtro por sucursal y estado del paquete (cubre RF-06).
  3. Consulta que combine datos de dos o más entidades del mismo servicio (JOIN o `$lookup`).

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| Repositorios correctamente definidos para todas las entidades principales | 4 |
| Consulta de búsqueda por texto parcial implementada (RF-07) | 2 |
| Consulta de filtro por sucursal y estado implementada (RF-06) | 2 |
| Consulta multi-entidad con JOIN o `$lookup` implementada | 2 |

---

### Sección 5: Capa de Servicio y Lógica de Negocio (20 puntos)

**Qué se evalúa:** Lógica de negocio en la capa correcta; excepciones personalizadas; conversión entre capas. Cubre RF-03, RF-04 y RF-05.

**Entregables:**

- Una clase `@Service` por entidad o caso de uso principal, con inyección por constructor en todos los servicios.
- Las siguientes reglas de negocio implementadas (distribuidas donde corresponda según tu diseño):
  - **RF-03:** cálculo automático de la tarifa al registrar un paquete, basado en al menos 2 variables. Documentado en el `README.md`.
  - **RF-04:** validación de transiciones de estado; transición inválida rechazada con excepción descriptiva que indique los estados involucrados. Estados y transiciones documentados en el `README.md`.
  - **RF-05:** registro automático en el historial de estados en cada cambio, incluyendo usuario y timestamp.
- Excepciones personalizadas en cada servicio que las necesite:
  - Recurso no encontrado → 404.
  - Conflicto o duplicado → 409.
  - Transición de estado inválida → 409.
  - Fallo de servicio externo (RENIEC u otro microservicio) → 502.
- Conversión entidad ↔ DTO mediante `ModelMapper` o mapeo manual justificado.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| Servicios con inyección por constructor en todos los microservicios | 3 |
| RF-03: cálculo automático de tarifa implementado y documentado | 5 |
| RF-04: validación de transición de estado con excepción descriptiva | 5 |
| RF-05: registro automático de historial con usuario y timestamp en cada cambio de estado | 4 |
| Excepciones personalizadas para los cuatro casos indicados, lanzadas en el contexto correcto | 3 |

---

### Sección 6: Controladores REST (15 puntos)

**Qué se evalúa:** Exposición correcta de recursos HTTP en cada servicio; verbos, códigos de estado y estructura de respuesta. Cubre RNF-01 y RNF-02.

**Entregables:**

- Un `@RestController` por entidad o recurso principal en cada servicio.
- CRUD completo para al menos **2 recursos** en el proyecto (pueden estar en servicios distintos).
- Los siguientes endpoints deben existir en algún servicio (tú decides dónde):
  - `POST` registrar cliente con validación RENIEC automática (RF-01).
  - `POST` registrar paquete con cálculo de tarifa automático (RF-02, RF-03).
  - `PATCH` o `PUT` actualizar estado de paquete con validación de transición (RF-04).
  - `GET` historial de estados de un paquete (RF-05).
  - `GET` con `@RequestParam` filtro por sucursal y estado (RF-06).
  - `GET` con `@RequestParam` búsqueda por texto parcial (RF-07).
  - `POST` o `PUT` asignar categorías a un paquete (RF-09).
- `@Valid` en todos los endpoints que reciban un body.
- `ResponseEntity<ApiResponse<T>>` con código HTTP correcto según RNF-02 en todos los endpoints.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| CRUD completo para al menos 2 recursos con verbos HTTP correctos | 4 |
| Todos los endpoints específicos de RF-01 al RF-09 presentes y funcionando | 6 |
| `@Valid` aplicado en todos los endpoints con body | 2 |
| `ResponseEntity<ApiResponse<T>>` con códigos HTTP correctos en todos los casos (RNF-02) | 3 |

---

### Sección 7: Manejo Global de Excepciones (10 puntos)

**Qué se evalúa:** Centralización del manejo de errores en cada servicio; respuestas de error consistentes. Cubre RNF-01 y RNF-02.

**Entregables:**

- Clase `GlobalExceptionHandler` con `@RestControllerAdvice` en **cada microservicio de negocio**.
- Handlers en cada servicio para:
  - `MethodArgumentNotValidException` → 400 con `Map<String, List<String>>` agrupado por campo.
  - Excepción de recurso no encontrado → 404.
  - Excepción de conflicto o transición inválida → 409.
  - Excepción de fallo de servicio externo → 502.
  - `Exception` genérica → 500 como fallback.
- Todos los errores retornan `ApiResponse<T>` con `success: false`.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| `@RestControllerAdvice` presente en cada microservicio de negocio | 2 |
| Handler para 400 con mapa de errores agrupados por campo en todos los servicios | 3 |
| Handlers para 404 y 409 en todos los servicios | 3 |
| Handler para 502 y handler genérico 500 en todos los servicios | 2 |

---

### Sección 8: Integración con API Externa — Feign Client (10 puntos)

**Qué se evalúa:** Consumo de RENIEC con Spring Cloud OpenFeign; gestión segura del token. Cubre RF-01 y RNF-06.

**Entregables:**

- Interfaz `@FeignClient` apuntando a `https://api.decolecta.com/v1/reniec/dni` en el servicio que gestiona el registro de clientes.
- Al registrar un cliente, el nombre completo debe obtenerse **exclusivamente de RENIEC**; no puede ser enviado por el cliente en el request.
- El token de RENIEC leído con `@Value` desde `application.yaml` o desde el Config Server. No puede estar hardcodeado (cubre RNF-06).
- Si RENIEC falla, lanzar la excepción de servicio externo y retornar 502.
- `@EnableFeignClients` habilitado en la clase principal del servicio correspondiente.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| `@FeignClient` correctamente definido con nombre y URL; `@EnableFeignClients` habilitado | 3 |
| Nombre completo del cliente obtenido exclusivamente de RENIEC al registrar (RF-01) | 2 |
| Token de RENIEC inyectado con `@Value`, sin hardcodear (RNF-06) | 2 |
| Fallo de RENIEC manejado con excepción personalizada y respuesta 502 | 3 |

---

### Sección 9: Pruebas Unitarias con Mockito (10 puntos)

**Qué se evalúa:** Pruebas aisladas de la capa de servicio en al menos 2 microservicios. Cubre RNF-05.

**Entregables:**

- Clases de prueba en al menos **2 microservicios distintos** usando `@ExtendWith(MockitoExtension.class)`.
- `@Mock` para todas las dependencias; `@InjectMocks` para la clase bajo prueba.
- Mínimo **6 casos de prueba en total** cubriendo obligatoriamente:
  - Al menos 1 happy path por servicio probado (verificado con `assertEquals` / `assertNotNull`).
  - Al menos 2 rutas de excepción con `assertThrows` verificando tipo y mensaje (ejemplos: email duplicado, transición de estado inválida).
  - Al menos 1 caso de resultado vacío (repositorio retorna lista vacía u `Optional.empty()`).
- Si tienes un componente auxiliar compartido entre servicios (por ejemplo, un calculador de tarifa), usa `@Spy` y demuestra el impacto cruzado. Si no aplica, justifícalo con un comentario en el archivo de prueba.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| Estructura correcta: `@ExtendWith`, `@Mock`, `@InjectMocks` en 2 microservicios distintos | 2 |
| Mínimo 2 happy paths verificados con aserciones sobre el DTO retornado | 3 |
| Mínimo 2 rutas de excepción con `assertThrows` y verificación del mensaje | 3 |
| Caso de resultado vacío verificado; uso de `@Spy` o justificación documentada | 2 |

---

### Sección 10: Seguridad con Spring Security y JWT (5 puntos)

**Qué se evalúa:** Autenticación y autorización stateless aplicada en el ecosistema de microservicios. Cubre RF-08 y RNF-06.

**Entregables:**

- La validación de JWT puede realizarse en el API Gateway (filtro de autenticación centralizado) o en cada microservicio individualmente. Documenta la decisión en el `README.md`.
- Tres roles implementados: `ADMIN`, `OPERADOR` y `CLIENTE`, con las restricciones del RF-08.
- Token JWT generado al hacer login, firmado con clave HMAC leída desde Vault (cubre RNF-06). La clave no puede aparecer en ningún archivo de configuración del repositorio.
- `DataInitializer` en el servicio de autenticación que inserte los tres roles y al menos un usuario por rol al iniciar.
- Endpoints de login y registro públicos; todos los demás requieren token válido.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| JWT validado correctamente (en Gateway o en cada servicio); estrategia documentada | 2 |
| Los tres roles con restricciones de acceso diferenciadas según RF-08 | 2 |
| `DataInitializer` que puebla roles y usuarios; clave JWT en Vault, no en archivos del repo (RNF-06) | 1 |

---

## PARTE B — INFRAESTRUCTURA Y PATRONES (80 puntos)

---

### Sección 11: Eureka Server y Registro de Servicios (15 puntos)

**Qué se evalúa:** Descubrimiento de servicios: configuración del servidor y registro correcto de todos los componentes. Cubre RNF-08.

**Entregables:**

- Proyecto `eureka-server` con `@EnableEurekaServer` en el puerto **8761**.
- Todos los microservicios de negocio y el API Gateway registrados como clientes Eureka con `spring.application.name` único y descriptivo en cada uno.
- Captura de pantalla del dashboard de Eureka mostrando todos los servicios en estado `UP`. Incluirla en el `README.md`.
- Heartbeat e intervalo de renovación configurados explícitamente en al menos 1 servicio cliente (`lease-renewal-interval-in-seconds`, `lease-expiration-duration-in-seconds`).

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| `eureka-server` levanta correctamente con `@EnableEurekaServer` en el puerto 8761 | 4 |
| Todos los microservicios de negocio y el Gateway se registran con `spring.application.name` único | 6 |
| Captura del dashboard con todos los servicios en estado `UP` incluida en el `README.md` | 3 |
| Heartbeat e intervalo de renovación configurados en al menos 1 cliente | 2 |

---

### Sección 12: API Gateway — Spring Cloud Gateway (20 puntos)

**Qué se evalúa:** Punto de entrada único; enrutamiento load-balanced vía Eureka; filtros globales y por ruta. Cubre RNF-08.

**Entregables:**

- Proyecto `api-gateway` configurado en `application.yaml`.
- Mínimo **3 rutas** (una por microservicio de negocio) usando `lb://` para resolución vía Eureka.
- Al menos **1 predicado** por ruta (`Path`, `Method`, `Header` u otro).
- Al menos **1 filtro global personalizado** implementando `GlobalFilter` y `Ordered` que agregue un header de trazabilidad a cada request (por ejemplo `X-Request-Id` con UUID o `X-Gateway-Timestamp`).
- Al menos **1 filtro de ruta específico** aplicado a al menos una ruta (`StripPrefix`, `AddRequestHeader`, `CircuitBreaker` u otro de Spring Cloud Gateway).
- Si la validación JWT se realiza en el Gateway, el filtro de autenticación debe estar aquí.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| Gateway levanta y enruta correctamente a los 3+ microservicios mediante `lb://` | 8 |
| Mínimo 3 rutas con predicados correctamente definidos en `application.yaml` | 4 |
| `GlobalFilter` personalizado con header de trazabilidad aplicado a todas las solicitudes | 5 |
| Al menos 1 filtro de ruta específico correctamente configurado | 3 |

---

### Sección 13: Config Server y Config Client (15 puntos)

**Qué se evalúa:** Configuración externalizada y centralizada; recarga de propiedades en caliente.

**Entregables:**

- Proyecto `config-server` con `@EnableConfigServer` apuntando a un repositorio Git local o remoto con archivos de configuración por servicio.
- Al menos **2 microservicios** configurados como Config Client con `spring.config.import=configserver:`.
- Los Config Clients deben obtener al menos una propiedad de negocio o de infraestructura **exclusivamente del Config Server** (no de su `application.yaml` local). Ejemplos: puerto, nombre de base de datos, umbral de negocio configurable.
- Demostración de recarga en caliente: cambiar una propiedad en el repositorio de configuración, llamar a `/actuator/refresh` en el servicio afectado (requiere `@RefreshScope`), y verificar que el valor se actualiza sin reiniciar. Documentar el proceso con el comando `curl` exacto en el `README.md`.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| `config-server` levanta y sirve configuración desde repositorio Git | 5 |
| Mínimo 2 microservicios consumen configuración del Config Server correctamente | 5 |
| Al menos una propiedad de negocio o infraestructura proviene exclusivamente del Config Server | 3 |
| `@RefreshScope` + `/actuator/refresh` demostrado y documentado con `curl` en el `README.md` | 2 |

---

### Sección 14: Gestión de Secretos con HashiCorp Vault (10 puntos)

**Qué se evalúa:** Almacenamiento y lectura de secretos sensibles fuera del código fuente. Cubre RNF-06.

**Entregables:**

- Vault en ejecución en modo desarrollo (`vault server -dev`) o mediante Docker.
- La **clave de firma JWT** (secreto HMAC) almacenada en Vault (backend `kv`) y leída por el servicio de autenticación usando Spring Cloud Vault.
- El secreto **no debe aparecer** en ningún `application.yaml`, `bootstrap.yaml`, archivo de configuración del Config Server ni en código fuente. Su ausencia es verificable.
- Configuración de Spring Cloud Vault en el `bootstrap.yaml` del servicio cliente: dirección de Vault, token de acceso, backend y ruta del secreto.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| Vault levanta correctamente y la clave JWT está almacenada en la ruta configurada | 3 |
| El servicio de autenticación lee la clave desde Vault; el secreto no aparece en ningún archivo del repositorio (RNF-06) | 4 |
| Configuración de Spring Cloud Vault en `bootstrap.yaml` correcta y completa | 3 |

---

### Sección 15: Patrones de Resiliencia — Circuit Breaker (15 puntos)

**Qué se evalúa:** Resiliencia en la comunicación inter-servicio con Resilience4j.

**Entregables:**

- Resilience4j incluido en al menos **1 microservicio** que llame a otro servicio de negocio.
- `@CircuitBreaker` aplicado a al menos **1 método** de comunicación inter-servicio, con un método `fallback` que retorne una respuesta degradada pero funcional. El fallback no puede lanzar una excepción genérica.
- Configuración en `application.yaml`:
  - `failureRateThreshold`: porcentaje de fallos que abre el circuito.
  - `waitDurationInOpenState`: tiempo en estado abierto antes de pasar a Half-Open.
  - `slidingWindowSize`: número de llamadas evaluadas.
- Al menos **1 patrón adicional**: `@Retry` con backoff configurable, `@RateLimiter` o `@TimeLimiter`.
- Endpoint `/actuator/circuitbreakers` accesible en el servicio configurado.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| `@CircuitBreaker` con `fallback` funcional aplicado en al menos 1 llamada inter-servicio | 6 |
| Configuración completa en `application.yaml`: `failureRateThreshold`, `waitDurationInOpenState`, `slidingWindowSize` | 4 |
| Al menos 1 patrón adicional (`@Retry`, `@RateLimiter` o `@TimeLimiter`) implementado y configurado | 3 |
| `/actuator/circuitbreakers` accesible y muestra el estado actual | 2 |

---

### Sección 16: Comunicación Inter-Servicio y Justificación de la Descomposición (5 puntos)

**Qué se evalúa:** Al menos un ejemplo concreto de comunicación entre servicios; razonamiento arquitectónico documentado.

**Entregables:**

- Al menos **1 llamada inter-servicio real** implementada (por ejemplo: el servicio de paquetes consulta al servicio de clientes para verificar que el remitente existe antes de registrar el paquete).
- Sección en el `README.md` (máximo 15 líneas) que responda:
  - Qué tipo de comunicación usaste (sincrónica / asincrónica) y por qué en cada caso.
  - Qué datos replicaste entre servicios (si aplica) y cómo manejas la consistencia.
  - Una decisión difícil de diseño con el razonamiento que usaste para resolverla.

**Rúbrica:**

| Criterio | Puntos |
|---|---|
| Al menos 1 llamada inter-servicio real implementada y funcionando | 2 |
| Tipo de comunicación justificado con argumentos técnicos por caso de uso | 2 |
| Decisión difícil documentada con razonamiento específico y honesto | 1 |

---

## APÉNDICE — CASOS DE PRUEBA

> Todos los `curl` van dirigidos al **API Gateway** (`localhost:8080`), nunca directamente a los microservicios. Adapta las rutas al diseño que hayas implementado; si cambias alguna ruta, documéntala en el `README.md`. Reemplaza `TOKEN` por el JWT obtenido en el caso de login. Reemplaza `{id}` por el UUID real del recurso.

---

### Autenticación

**Caso 1 — Registro de usuario**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Carlos Quispe","email":"carlos@rapidocourier.pe","password":"Segura123!","role":"OPERADOR"}' | jq .
```
Esperado: `201 Created` — `success: true`, JWT en `data`.

**Caso 2 — Login exitoso**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos@rapidocourier.pe","password":"Segura123!"}' | jq .
```
Esperado: `200 OK` — `success: true`, JWT válido en `data`.

**Caso 3 — Login con credenciales incorrectas**
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos@rapidocourier.pe","password":"incorrecta"}' | jq .
```
Esperado: `401 Unauthorized` — `success: false`.

---

### Clientes (RF-01)

**Caso 4 — Registro de cliente con consulta RENIEC**
```bash
curl -s -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"dni":"12345678","email":"ana.flores@correo.com","telefono":"987654321"}' | jq .
```
Esperado: `201 Created` — nombre completo obtenido de RENIEC visible en `data`; el campo nombre no estaba en el request.

**Caso 5 — Email duplicado en registro de cliente**
```bash
curl -s -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"dni":"87654321","email":"ana.flores@correo.com","telefono":"999888777"}' | jq .
```
Esperado: `409 Conflict` — `success: false`, mensaje indica email ya registrado.

**Caso 6 — Datos inválidos en registro de cliente**
```bash
curl -s -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"dni":"123","email":"correo-invalido","telefono":""}' | jq .
```
Esperado: `400 Bad Request` — `success: false`, `data` con mapa de errores agrupado por campo.

---

### Paquetes (RF-02, RF-03, RF-04)

**Caso 7 — Registro de paquete con tarifa calculada**
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "descripcion":"Documentos legales",
    "pesoKg":1.5,
    "valorDeclarado":200.00,
    "sucursalOrigen":"Lima",
    "sucursalDestino":"Arequipa",
    "dniRemitente":"12345678",
    "dniDestinatario":"87654321"
  }' | jq .
```
Esperado: `201 Created` — código de rastreo generado y tarifa calculada visibles en `data`.

**Caso 8 — Datos inválidos en registro de paquete**
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"descripcion":"","pesoKg":-1,"valorDeclarado":0}' | jq .
```
Esperado: `400 Bad Request` — mapa de errores por campo en `data`.

**Caso 9 — Consulta de paquete por ID existente**
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/{id} \
  -H "Authorization: Bearer TOKEN" | jq .
```
Esperado: `200 OK` — datos del paquete incluyendo nombre del remitente y destinatario en `data`.

**Caso 10 — Consulta de paquete por ID inexistente**
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer TOKEN" | jq .
```
Esperado: `404 Not Found` — `success: false`.

**Caso 11 — Actualización de estado: transición válida (RF-04)**
```bash
curl -s -X PATCH http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"EN_TRANSITO"}' | jq .
```
Esperado: `200 OK` — paquete con nuevo estado en `data`.

**Caso 12 — Actualización de estado: transición inválida (RF-04)**
```bash
curl -s -X PATCH http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"REGISTRADO"}' | jq .
```
Esperado: `409 Conflict` — mensaje descriptivo con los estados involucrados.

---

### Historial de Estados (RF-05)

**Caso 13 — Consulta del historial de estados**
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/{id}/historial \
  -H "Authorization: Bearer TOKEN" | jq .
```
Esperado: `200 OK` — lista ordenada de cambios de estado con timestamp y usuario en `data`.

---

### Búsqueda y Filtros (RF-06, RF-07)

**Caso 14 — Búsqueda por texto parcial en código de rastreo (RF-07)**
```bash
curl -s -X GET "http://localhost:8080/api/v1/paquetes?busqueda=RC2024" \
  -H "Authorization: Bearer TOKEN" | jq .
```
Esperado: `200 OK` — lista de paquetes cuyo código de rastreo contenga "RC2024".

**Caso 15 — Búsqueda por nombre de cliente (RF-07)**
```bash
curl -s -X GET "http://localhost:8080/api/v1/paquetes?busqueda=flores" \
  -H "Authorization: Bearer TOKEN" | jq .
```
Esperado: `200 OK` — paquetes cuyo remitente o destinatario contenga "flores" en su nombre.

**Caso 16 — Filtro por sucursal y estado (RF-06)**
```bash
curl -s -X GET "http://localhost:8080/api/v1/paquetes?sucursal=Lima&estado=EN_TRANSITO" \
  -H "Authorization: Bearer TOKEN" | jq .
```
Esperado: `200 OK` — paquetes con origen o destino en Lima y estado EN_TRANSITO.

---

### Categorías (RF-09)

**Caso 17 — Asignación de categoría a un paquete**
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes/{id}/categorias/{categoriaId} \
  -H "Authorization: Bearer TOKEN" | jq .
```
Esperado: `200 OK` — paquete actualizado con la categoría asignada en `data`.

---

### Operaciones de escritura y borrado

**Caso 18 — Actualización de paquete**
```bash
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"descripcion":"Documentos actualizados","pesoKg":2.0}' | jq .
```
Esperado: `200 OK` — datos actualizados en `data`.

**Caso 19 — Eliminación de paquete (solo ADMIN)**
```bash
curl -s -X DELETE http://localhost:8080/api/v1/paquetes/{id} \
  -H "Authorization: Bearer TOKEN_ADMIN" | jq .
```
Esperado: `204 No Content`.

**Caso 20 — Eliminación de recurso inexistente**
```bash
curl -s -X DELETE http://localhost:8080/api/v1/paquetes/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer TOKEN_ADMIN" | jq .
```
Esperado: `404 Not Found` — `success: false`.

---

### Seguridad (RF-08)

**Caso 21 — Acceso sin token a endpoint protegido**
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes | jq .
```
Esperado: `401 Unauthorized`.

**Caso 22 — Acceso con rol insuficiente: CLIENTE intenta eliminar**
```bash
curl -s -X DELETE http://localhost:8080/api/v1/paquetes/{id} \
  -H "Authorization: Bearer TOKEN_CLIENTE" | jq .
```
Esperado: `403 Forbidden`.

---

## Entregables del Proyecto

- Repositorio Git público con todos los proyectos Maven en subdirectorios claramente nombrados.
- `README.md` raíz con (cubre RNF-07):
  - [ ] Mapa de microservicios: nombre, bounded context, RF asignados, base de datos y dependencias.
  - [ ] Modelo de datos por servicio (ERD o esquema de colecciones).
  - [ ] Justificación de la base de datos elegida por servicio.
  - [ ] Justificación de la arquitectura interna de cada servicio.
  - [ ] Regla de cálculo de tarifa (RF-03).
  - [ ] Estados y transiciones válidas del paquete (RF-04).
  - [ ] Instrucciones para levantar el ecosistema completo localmente (orden de arranque recomendado).
  - [ ] Captura del dashboard de Eureka con todos los servicios en estado `UP`.
  - [ ] Comando `curl` exacto para demostrar el refresh de configuración con `/actuator/refresh`.
  - [ ] Justificación de la descomposición y decisiones de comunicación inter-servicio.
- `RESPUESTAS-CONCEPTUALES.md` en la raíz del repositorio.
- `mvn test` sin errores en cada microservicio que contenga pruebas unitarias.

---

## Resumen de Puntaje

| Parte | Sección | Tema | Puntos |
|---|---|---|---|
| A | 1 | Diseño e Identificación de Microservicios | 10 |
| A | 2 | Entidades y Base de Datos | 20 |
| A | 3 | DTOs y Validaciones | 10 |
| A | 4 | Repositorios | 10 |
| A | 5 | Capa de Servicio y Lógica de Negocio | 20 |
| A | 6 | Controladores REST | 15 |
| A | 7 | Manejo Global de Excepciones | 10 |
| A | 8 | Integración RENIEC — Feign Client | 10 |
| A | 9 | Pruebas Unitarias con Mockito | 10 |
| A | 10 | Seguridad con Spring Security y JWT | 5 |
| | | **Subtotal Parte A** | **120** |
| B | 11 | Eureka Server y Registro de Servicios | 15 |
| B | 12 | API Gateway — Spring Cloud Gateway | 20 |
| B | 13 | Config Server y Config Client | 15 |
| B | 14 | Gestión de Secretos con Vault | 10 |
| B | 15 | Circuit Breaker con Resilience4j | 15 |
| B | 16 | Comunicación Inter-Servicio y Justificación | 5 |
| | | **Subtotal Parte B** | **80** |
| C | — | Preguntas Conceptuales (5 × 2 pts) | 10 |
| | | **TOTAL** | **210** |
