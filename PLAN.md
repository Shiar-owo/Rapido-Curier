# Plan de Implementación - RapidoCourier S.A.C.

## Resumen Ejecutivo

Este documento detalla el plan de implementación para el backend completo de **RapidoCourier S.A.C.**, una empresa peruana de mensajería con sucursales en Lima, Arequipa y Cusco. La arquitectura será de microservicios distribuidos con **Hexagonal Architecture** y **HashiCorp Vault** como única fuente de secretos.

---

## Stack Tecnológico Obligatorio

- **Java 17**
- **Spring Boot 3.x**
- **Maven** (cada microservicio es un módulo independiente)
- **Docker + Docker Compose**
- **application.yaml** (nunca application.properties)
- **UUID** como clave primaria
- **Inyección de dependencias por constructor** (nunca @Autowired)
- **ApiResponse<T>** en todos los endpoints
- **Resilience4j** para Circuit Breaker
- **Spring Cloud Vault** para secrets management

---

## Dependencias Maven por Servicio

### servicio-auth
- `spring-boot-starter-security`
- `jjwt`
- `spring-cloud-starter-vault-config`
- `spring-cloud-starter-netflix-eureka-client`

### servicio-clientes
- `spring-cloud-openfeign`
- `spring-cloud-starter-netflix-eureka-client`
- `spring-cloud-starter-config`
- `resilience4j`

### servicio-paquetes
- `spring-cloud-openfeign`
- `spring-cloud-starter-netflix-eureka-client`
- `spring-cloud-starter-config`
- `resilience4j`

---

## Estructura del Repositorio

```
rapidocourier/
├── eureka-server/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── api-gateway/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── config-server/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── servicio-auth/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── servicio-clientes/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── servicio-paquetes/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── config-repo/
├── docker-compose.yml
└── .env (NO commitear)
```

---

## Fase 1: Estructura del Proyecto y Configuración Inicial

### 1.1 Crear Estructura de Directorios

Crear la jerarquía completa de carpetas para los 6 proyectos Maven.

### 1.2 Archivo .env

Archivo único (en .gitignore) con variables de entorno:

```dotenv
VAULT_TOKEN=
CONFIG_GIT_URI=
```

### 1.3 Repositorio GitHub config-repo

Crear repositorio `rapidocourier-config` con archivos YAML:

**application.yaml** (configuración común):
```yaml
spring:
  application:
    name: rapidocourier
  cloud:
    vault:
      host: ${SPRING_CLOUD_VAULT_HOST:localhost}
      port: 8200
      token: ${SPRING_CLOUD_VAULT_TOKEN:}
      kv:
        enabled: true
        backend: secret
        default-context: rapidocourier

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
```

**servicio-clientes.yaml** (incluye Resilience4j y Eureka heartbeat):
```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/rapidocourier_clientes
    username: ${db.username}
    password: ${db.password}

reniec:
  api:
    url: https://api.decolecta.com/v1/reniec/dni
    token: ${reniec.token}

resilience4j:
  circuitbreaker:
    instances:
      reniec:
        sliding-window-size: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s

eureka:
  instance:
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
```

**servicio-paquetes.yaml** (incluye Circuit Breaker para servicio-clientes):
```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/rapidocourier_paquetes
    username: ${db.username}
    password: ${db.password}

resilience4j:
  circuitbreaker:
    instances:
      servicio-clientes:
        sliding-window-size: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s

eureka:
  instance:
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
```

**servicio-auth.yaml**:
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/rapidocourier_auth
    username: ${db.username}
    password: ${db.password}

jwt:
  secret: ${jwt.secret}

eureka:
  instance:
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
```

---

## Fase 2: Servicios de Infraestructura

### 2.1 HashiCorp Vault

| Atributo | Valor |
|---------|-------|
| Imagen | hashicorp/vault:1.15 |
| Puerto | 8200 |
| Modo | Desarrollo |
| Secrets Engine | KV en secret/ |

**Propósito:** Única fuente de verdad para todos los secretos.

### 2.2 Instancias PostgreSQL

| Contenedor | Base de Datos | Usuario | Path Vault |
|------------|---------------|---------|------------|
| postgres-auth | rapidocourier_auth | auth_user | secret/rapidocourier/auth |
| postgres-clientes | rapidocourier_clientes | clientes_user | secret/rapidocourier/clientes |
| postgres-paquetes | rapidocourier_paquetes | paquetes_user | secret/rapidocourier/paquetes |

### 2.3 Config Server

- Puerto: 8888
- Dependencia: spring-cloud-config-server
- Apunta al repositorio Git config-repo

### 2.4 Eureka Server

- Puerto: 8761
- Dependencia: spring-cloud-starter-netflix-eureka-server

### 2.5 API Gateway

- Puerto: 8080 (único expuesto al host)
- Rutas configuradas hacia auth, clientes, paquetes
- Filtro JWT centralizado que propaga X-User-Id y X-User-Roles

---

## Fase 3: Configuración de Secrets en Vault

### 3.1 Secrets por Servicio

```bash
# servicio-auth
vault kv put secret/rapidocourier/auth \
  db.username="auth_user" \
  db.password="secure_password" \
  jwt.secret="clave-hmac-minimo-32-caracteres-aqui"

# servicio-clientes
vault kv put secret/rapidocourier/clientes \
  db.username="clientes_user" \
  db.password="secure_password" \
  reniec.token="Bearer token_reniec"

# servicio-paquetes
vault kv put secret/rapidocourier/paquetes \
  db.username="paquetes_user" \
  db.password="secure_password"
```

### 3.2 Patrón de Lectura en Servicios

Cada servicio configura en su application.yaml:

```yaml
spring:
  cloud:
    vault:
      kv:
        default-context: rapidocourier/<nombre-servicio>
  datasource:
    username: ${db.username}
    password: ${db.password}
```

---

## Fase 4: servicio-auth

**Puerto:** 8081  
**Base de datos:** rapidocourier_auth  
**Arquitectura:** Hexagonal (Ports & Adapters)

### 4.1 Estructura de Paquetes

```
servicio-auth/
└── src/main/java/com/rapidocourier/auth/
    ├── domain/
    │   ├── model/
    │   │   └── Usuario.java
    │   ├── port/
    │   │   ├── in/
    │   │   │   ├── LoginUseCase.java
    │   │   │   └── RegisterUseCase.java
    │   │   └── out/
    │   │       ├── UsuarioRepositoryPort.java
    │   │       └── JwtPort.java
    │   └── exception/
    │       └── CredencialesInvalidasException.java
    ├── application/
    │   └── service/
    │       └── AuthService.java
    └── infrastructure/
        ├── adapter/
        │   ├── in/
        │   │   └── rest/
        │   │       ├── controller/
        │   │       │   └── AuthController.java
        │   │       └── dto/
        │   │           ├── request/
        │   │           │   ├── LoginRequest.java
        │   │           │   └── RegisterRequest.java
        │   └── out/
        │       ├── persistence/
        │       │   ├── entity/
        │       │   │   ├── UsuarioEntity.java
        │       │   │   └── RolEntity.java
        │       │   ├── repository/
        │       │   │   ├── UsuarioJpaRepository.java
        │       │   │   └── RolJpaRepository.java
        │       │   └── mapper/
        │       │       └── UsuarioMapper.java
        │       └── jwt/
        │           └── JwtAdapter.java
        └── config/
            ├── SecurityConfig.java
            └── BeanConfig.java
```

### 4.2 Modelo de Dominio

```java
// domain/model/Usuario.java - POJO puro
public class Usuario {
    private UUID id;
    private String nombre;
    private String email;
    private String password;
    private Set<String> roles;
}

// domain/model/RolNombre.java
public enum RolNombre {
    ADMIN, OPERADOR, CLIENTE
}
```

### 4.3 Puertos de Entrada (In)

```java
// domain/port/in/LoginUseCase.java
public interface LoginUseCase {
    String login(String email, String password);
}

// domain/port/in/RegisterUseCase.java
public interface RegisterUseCase {
    Usuario registrar(String nombre, String email, String password, String rol);
}
```

### 4.4 Puertos de Salida (Out)

```java
// domain/port/out/UsuarioRepositoryPort.java
public interface UsuarioRepositoryPort {
    Optional<Usuario> buscarPorEmail(String email);
    boolean existePorEmail(String email);
    Usuario guardar(Usuario usuario);
}

// domain/port/out/JwtPort.java
public interface JwtPort {
    String generarToken(Usuario usuario);
    Claims parsearToken(String token);
}
```

### 4.5 Servicio de Aplicación

```java
// application/service/AuthService.java
@Service
public class AuthService implements LoginUseCase, RegisterUseCase {
    private final UsuarioRepositoryPort usuarios;
    private final JwtPort jwt;
    private final PasswordEncoder encoder;

    public AuthService(UsuarioRepositoryPort usuarios, JwtPort jwt, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.jwt = jwt;
        this.encoder = encoder;
    }

    @Override
    public String login(String email, String password) {
        Usuario u = usuarios.buscarPorEmail(email)
            .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));
        if (!encoder.matches(password, u.getPassword()))
            throw new CredencialesInvalidasException("Contraseña incorrecta");
        return jwt.generarToken(u);
    }

    @Override
    public Usuario registrar(String nombre, String email, String password, String rol) {
        if (usuarios.existePorEmail(email))
            throw new ConflictException("El email ya está registrado");
        Usuario nuevo = new Usuario(nombre, encoder.encode(password), email, Set.of(rol));
        return usuarios.guardar(nuevo);
    }
}
```

### 4.6 Controlador REST

```java
// infrastructure/adapter/in/rest/controller/AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final JwtPort jwtPort;

    public AuthController(LoginUseCase loginUseCase, RegisterUseCase registerUseCase, JwtPort jwtPort) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.jwtPort = jwtPort;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest request) {
        String token = loginUseCase.login(request.email(), request.password());
        return ApiResponse.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        Usuario usuario = registerUseCase.registrar(request.nombre(), request.email(), request.password(), request.rol());
        String token = jwtPort.generarToken(usuario);
        return ApiResponse.created(token);
    }
}
```

### 4.7 DataInitializer

Al arrancar, insertar:
- Roles: ADMIN, OPERADOR, CLIENTE
- Usuarios: admin/admin123, operador/op123, cliente/cl123

### 4.8 Pruebas Unitarias (4 tests)

```java
// test/java/com/rapidocourier/auth/application/service/AuthServiceTest.java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepositoryPort usuarios;
    @Mock JwtPort jwt;
    @InjectMocks AuthService service;

    @Test
    void login_exitoso_retornaToken() {
        when(usuarios.buscarPorEmail("admin@test.com")).thenReturn(Optional.of(
            new Usuario(UUID.randomUUID(), "Admin", "hash", "admin@test.com", Set.of("ADMIN"))));
        when(jwt.generarToken(any())).thenReturn("jwt.token");

        String result = service.login("admin@test.com", "admin123");

        assertEquals("jwt.token", result);
    }

    @Test
    void login_usuarioNoExiste_lanzaExcepcion() {
        when(usuarios.buscarPorEmail("admin@test.com")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
            () -> service.login("admin@test.com", "admin123"));
    }

    @Test
    void login_contrasenaIncorrecta_lanzaExcepcion() {
        when(usuarios.buscarPorEmail("admin@test.com")).thenReturn(Optional.of(
            new Usuario(UUID.randomUUID(), "Admin", "hash", "admin@test.com", Set.of("ADMIN"))));

        assertThrows(CredencialesInvalidasException.class,
            () -> service.login("admin@test.com", "wrong"));
    }

    @Test
    void register_exitoso_creaUsuario() {
        when(usuarios.existePorEmail("nuevo@test.com")).thenReturn(false);
        when(usuarios.guardar(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = service.registrar("Nuevo", "nuevo@test.com", "password", "CLIENTE");

        assertEquals("nuevo@test.com", result.getEmail());
        assertTrue(result.getRoles().contains("CLIENTE"));
    }
}
```

---

## Fase 5: servicio-clientes

**Puerto:** 8082  
**Base de datos:** rapidocourier_clientes  
**Arquitectura:** Hexagonal (Ports & Adapters)

### 5.1 Estructura de Paquetes

```
servicio-clientes/
└── src/main/java/com/rapidocourier/clientes/
    ├── domain/
    │   ├── model/
    │   │   └── Cliente.java
    │   ├── port/
    │   │   ├── in/
    │   │   │   ├── RegistrarClienteUseCase.java
    │   │   │   └── ConsultarClienteUseCase.java
    │   │   └── out/
    │   │       ├── ClienteRepositoryPort.java
    │   │       └── ReniecPort.java
    │   └── exception/
    │       ├── ConflictException.java
    │       └── ExternalServiceException.java
    ├── application/
    │   └── service/
    │       └── ClienteService.java
    └── infrastructure/
        ├── adapter/
        │   ├── in/
        │   │   └── rest/
        │   │       ├── controller/
        │   │       │   └── ClienteController.java
        │   │       └── dto/
        │   │           ├── request/
        │   │           │   └── ClienteRequest.java
        │   │           └── response/
        │   │               └── ClienteResponse.java
        │   └── out/
        │       ├── persistence/
        │       │   ├── entity/
        │       │   │   └── ClienteEntity.java
        │       │   ├── repository/
        │       │   │   └── ClienteJpaRepository.java
        │       │   └── mapper/
        │       │       └── ClienteMapper.java
        │       └── reniec/
        │           ├── client/
        │           │   └── ReniecFeignClient.java
        │           └── adapter/
        │               └── ReniecAdapter.java
        └── config/
            ├── JpaConfig.java (EnableJpaAuditing)
            └── FeignConfig.java
```

### 5.2 Modelo de Dominio

```java
// domain/model/Cliente.java
public class Cliente {
    private UUID id;
    private String dni;
    private String nombreCompleto;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 5.3 Puertos de Entrada (In)

```java
// domain/port/in/RegistrarClienteUseCase.java
public interface RegistrarClienteUseCase {
    Cliente registrar(String dni, String email);
}

// domain/port/in/ConsultarClienteUseCase.java
public interface ConsultarClienteUseCase {
    Cliente buscarPorId(UUID id);
    List<Cliente> listarTodos();
    void eliminar(UUID id);
}
```

### 5.4 Puertos de Salida (Out)

```java
// domain/port/out/ClienteRepositoryPort.java
public interface ClienteRepositoryPort {
    Cliente guardar(Cliente cliente);
    Optional<Cliente> buscarPorId(UUID id);
    Optional<Cliente> buscarPorDni(String dni);
    Optional<Cliente> buscarPorEmail(String email);
    List<Cliente> listarTodos();
    void eliminar(UUID id);
}

// domain/port/out/ReniecPort.java
public interface ReniecPort {
    String obtenerNombreCompleto(String dni);
}
```

### 5.5 Servicio de Aplicación

```java
// application/service/ClienteService.java
@Service
public class ClienteService implements RegistrarClienteUseCase, ConsultarClienteUseCase {
    private final ClienteRepositoryPort repo;
    private final ReniecPort reniec;

    public ClienteService(ClienteRepositoryPort repo, ReniecPort reniec) {
        this.repo = repo;
        this.reniec = reniec;
    }

    @Override
    public Cliente registrar(String dni, String email) {
        if (repo.buscarPorEmail(email).isPresent())
            throw new ConflictException("El email ya está registrado");
        if (repo.buscarPorDni(dni).isPresent())
            throw new ConflictException("El DNI ya está registrado");

        String nombreCompleto = reniec.obtenerNombreCompleto(dni);
        Cliente cliente = new Cliente(null, dni, nombreCompleto, email, null, null);
        return repo.guardar(cliente);
    }

    @Override
    public Cliente buscarPorId(UUID id) {
        return repo.buscarPorId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    @Override
    public List<Cliente> listarTodos() {
        return repo.listarTodos();
    }

    @Override
    public void eliminar(UUID id) {
        if (repo.buscarPorId(id).isEmpty())
            throw new ResourceNotFoundException("Cliente no encontrado");
        repo.eliminar(id);
    }
}
```

### 5.6 Adaptador RENIEC con Circuit Breaker

```java
// infrastructure/adapter/out/reniec/adapter/ReniecAdapter.java
@Component
public class ReniecAdapter implements ReniecPort {
    private final ReniecFeignClient client;

    @Value("${reniec.api.token}")
    private String token;

    public ReniecAdapter(ReniecFeignClient client) {
        this.client = client;
    }

    @Override
    @CircuitBreaker(name = "reniec", fallbackMethod = "fallbackReniec")
    public String obtenerNombreCompleto(String dni) {
        return client.consultarDni("Bearer " + token, dni).getNombreCompleto();
    }

    public String fallbackReniec(String dni, Exception ex) {
        throw new ExternalServiceException("RENIEC no disponible (circuit breaker abierto)");
    }
}
```

### 5.7 Controlador REST

```java
// infrastructure/adapter/in/rest/controller/ClienteController.java
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final RegistrarClienteUseCase registrarUseCase;
    private final ConsultarClienteUseCase consultarUseCase;

    public ClienteController(RegistrarClienteUseCase registrarUseCase,
                            ConsultarClienteUseCase consultarUseCase) {
        this.registrarUseCase = registrarUseCase;
        this.consultarUseCase = consultarUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<ClienteResponse>> crear(
            @Valid @RequestBody ClienteRequest request) {
        Cliente cliente = registrarUseCase.registrar(request.dni(), request.email());
        return ApiResponse.created(toResponse(cliente));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> listar() {
        return ApiResponse.ok(consultarUseCase.listarTodos().stream()
            .map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtener(@PathVariable UUID id) {
        return ApiResponse.ok(toResponse(consultarUseCase.buscarPorId(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        consultarUseCase.eliminar(id);
        return ApiResponse.noContent();
    }
}
```

### 5.8 Pruebas Unitarias (4 tests)

```java
// test/java/com/rapidocourier/clientes/application/service/ClienteServiceTest.java
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock ClienteRepositoryPort repo;
    @Mock ReniecPort reniec;
    @InjectMocks ClienteService service;

    @Test
    void registrar_happyPath_retornaCliente() {
        when(repo.buscarPorEmail("a@b.com")).thenReturn(Optional.empty());
        when(repo.buscarPorDni("12345678")).thenReturn(Optional.empty());
        when(reniec.obtenerNombreCompleto("12345678")).thenReturn("Juan Pérez");
        when(repo.guardar(any())).thenAnswer(i -> i.getArgument(0));

        Cliente resultado = service.registrar("12345678", "a@b.com");

        assertNotNull(resultado);
        assertEquals("Juan Pérez", resultado.getNombreCompleto());
    }

    @Test
    void registrar_emailDuplicado_lanzaConflictException() {
        when(repo.buscarPorEmail("a@b.com")).thenReturn(Optional.of(new Cliente()));

        ConflictException ex = assertThrows(ConflictException.class,
            () -> service.registrar("12345678", "a@b.com"));

        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    void registrar_reniecFalla_lanzaExternalServiceException() {
        when(repo.buscarPorEmail(any())).thenReturn(Optional.empty());
        when(reniec.obtenerNombreCompleto(any()))
            .thenThrow(new ExternalServiceException("RENIEC caído"));

        assertThrows(ExternalServiceException.class,
            () -> service.registrar("12345678", "a@b.com"));
    }

    @Test
    void listar_repositorioVacio_retornaListaVacia() {
        when(repo.listarTodos()).thenReturn(List.of());
        assertTrue(service.listarTodos().isEmpty());
    }
}
```

---

## Fase 6: servicio-paquetes

**Puerto:** 8083  
**Base de datos:** rapidocourier_paquetes  
**Arquitectura:** Hexagonal (Ports & Adapters)

### 6.1 Estructura de Paquetes

```
servicio-paquetes/
└── src/main/java/com/rapidocourier/paquetes/
    ├── domain/
    │   ├── model/
    │   │   ├── Paquete.java
    │   │   ├── Categoria.java
    │   │   └── EstadoHistorial.java
    │   ├── port/
    │   │   ├── in/
    │   │   │   ├── RegistrarPaqueteUseCase.java
    │   │   │   ├── ConsultarPaqueteUseCase.java
    │   │   │   └── GestionarEstadoUseCase.java
    │   │   └── out/
    │   │       ├── PaqueteRepositoryPort.java
    │   │       ├── HistorialRepositoryPort.java
    │   │       └── ClienteFeignPort.java
    │   ├── exception/
    │   │   ├── ResourceNotFoundException.java
    │   │   └── InvalidStateTransitionException.java
    │   └── service/
    │       └── TarifaCalculator.java
    ├── application/
    │   └── service/
    │       └── PaqueteService.java
    └── infrastructure/
        ├── adapter/
        │   ├── in/
        │   │   └── rest/
        │   │       ├── controller/
        │   │       │   ├── PaqueteController.java
        │   │       │   └── CategoriaController.java
        │   │       └── dto/
        │   │           ├── request/
        │   │           │   ├── PaqueteRequest.java
        │   │           │   └── CambioEstadoRequest.java
        │   │           └── response/
        │   │               └── PaqueteResponse.java
        │   └── out/
        │       ├── persistence/
        │       │   ├── entity/
        │       │   │   ├── PaqueteEntity.java
        │       │   │   ├── CategoriaEntity.java
        │       │   │   └── EstadoHistorialEntity.java
        │       │   ├── repository/
        │       │   │   ├── PaqueteJpaRepository.java
        │       │   │   ├── CategoriaJpaRepository.java
        │       │   │   └── HistorialJpaRepository.java
        │       │   └── mapper/
        │       │       └── PaqueteMapper.java
        │       └── cliente/
        │           └── ClienteFeignClient.java
        └── config/
            └── FeignConfig.java
```

### 6.2 Modelo de Dominio

```java
// domain/model/Paquete.java
public class Paquete {
    private UUID id;
    private String codigoRastreo;
    private UUID remitenteId;
    private UUID destinatarioId;
    private Double pesoKg;
    private Double valorDeclarado;
    private String sucursalOrigen;
    private String sucursalDestino;
    private Double tarifa;
    private EstadoPaquete estadoActual;
    private Set<String> categorias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// domain/model/EstadoPaquete.java
public enum EstadoPaquete {
    REGISTRADO, EN_ALMACEN, EN_TRANSITO, EN_REPARTO, ENTREGADO, NO_ENTREGADO;

    public static final Map<EstadoPaquete, Set<EstadoPaquete>> TRANSICIONES = Map.of(
        REGISTRADO,    Set.of(EN_ALMACEN),
        EN_ALMACEN,    Set.of(EN_TRANSITO),
        EN_TRANSITO,   Set.of(EN_REPARTO),
        EN_REPARTO,    Set.of(ENTREGADO, NO_ENTREGADO),
        NO_ENTREGADO,  Set.of(EN_ALMACEN)
    );
}
```

### 6.3 Calculadora de Tarifa

```java
// domain/service/TarifaCalculator.java
@Component
public class TarifaCalculator {
    private static final Map<String, Double> TARIFAS_RUTA = Map.of(
        "LIMA-AREQUIPA", 15.0, "AREQUIPA-LIMA", 15.0,
        "LIMA-CUSCO", 20.0, "CUSCO-LIMA", 20.0,
        "AREQUIPA-CUSCO", 12.0, "CUSCO-AREQUIPA", 12.0
    );

    public double calcular(double pesoKg, double valorDeclarado,
                          String origen, String destino) {
        String clave = origen.toUpperCase() + "-" + destino.toUpperCase();
        double tarifaRuta = TARIFAS_RUTA.getOrDefault(clave, 5.0);
        return (pesoKg * 8.0) + (valorDeclarado * 0.01) + tarifaRuta;
    }
}
```

### 6.4 Puertos de Entrada (In)

```java
// domain/port/in/RegistrarPaqueteUseCase.java
public interface RegistrarPaqueteUseCase {
    Paquete registrar(PaqueteRequest request);
}

// domain/port/in/ConsultarPaqueteUseCase.java
public interface ConsultarPaqueteUseCase {
    Paquete buscarPorId(UUID id);
    List<Paquete> buscarPorCodigoRastreo(String texto);
    List<Paquete> buscarPorSucursalYEstado(String sucursal, EstadoPaquete estado);
}

// domain/port/in/GestionarEstadoUseCase.java
public interface GestionarEstadoUseCase {
    void cambiarEstado(UUID paqueteId, EstadoPaquete nuevoEstado, String usuarioResponsable);
    List<EstadoHistorial> obtenerHistorial(UUID paqueteId);
}
```

### 6.5 Puertos de Salida (Out)

```java
// domain/port/out/PaqueteRepositoryPort.java
public interface PaqueteRepositoryPort {
    Paquete guardar(Paquete paquete);
    Optional<Paquete> buscarPorId(UUID id);
    List<Paquete> buscarPorCodigoRastreo(String texto);
    List<Paquete> buscarPorSucursalYEstado(String sucursal, EstadoPaquete estado);
    void eliminar(UUID id);
}

// domain/port/out/HistorialRepositoryPort.java
public interface HistorialRepositoryPort {
    void guardar(EstadoHistorial historial);
    List<EstadoHistorial> obtenerPorPaqueteId(UUID paqueteId);
}

// domain/port/out/ClienteFeignPort.java
public interface ClienteFeignPort {
    ClienteDto obtenerCliente(UUID id);
}
```

### 6.6 Servicio de Aplicación

```java
// application/service/PaqueteService.java
@Service
public class PaqueteService implements RegistrarPaqueteUseCase, 
                                         ConsultarPaqueteUseCase,
                                         GestionarEstadoUseCase {
    private final PaqueteRepositoryPort repo;
    private final HistorialRepositoryPort historial;
    private final ClienteFeignPort clienteFeign;
    private final TarifaCalculator tarifaCalculator;

    public PaqueteService(PaqueteRepositoryPort repo,
                          HistorialRepositoryPort historial,
                          ClienteFeignPort clienteFeign,
                          TarifaCalculator tarifaCalculator) {
        this.repo = repo;
        this.historial = historial;
        this.clienteFeign = clienteFeign;
        this.tarifaCalculator = tarifaCalculator;
    }

    @Override
    public Paquete registrar(PaqueteRequest request) {
        double tarifa = tarifaCalculator.calcular(
            request.pesoKg(), request.valorDeclarado(),
            request.sucursalOrigen(), request.sucursalDestino());

        String codigo = generarCodigoRastreo();
        Paquete paquete = new Paquete(null, codigo, request.remitenteId(),
            request.destinatarioId(), request.pesoKg(), request.valorDeclarado(),
            request.sucursalOrigen(), request.sucursalDestino(), tarifa,
            EstadoPaquete.REGISTRADO, Set.of(), null, null);
        
        Paquete guardado = repo.guardar(paquete);
        
        historial.guardar(new EstadoHistorial(null, guardado.getId(), 
            EstadoPaquete.REGISTRADO, LocalDateTime.now(), "sistema"));
        
        return guardado;
    }

    @Override
    public void cambiarEstado(UUID paqueteId, EstadoPaquete nuevoEstado, 
                             String usuarioResponsable) {
        Paquete paquete = repo.buscarPorId(paqueteId)
            .orElseThrow(() -> new ResourceNotFoundException("Paquete no encontrado"));

        EstadoPaquete estadoActual = paquete.getEstadoActual();
        Set<EstadoPaquete> permitidas = EstadoPaquete.TRANSICIONES.get(estadoActual);

        if (permitidas == null || !permitidas.contains(nuevoEstado)) {
            throw new InvalidStateTransitionException(
                "Transición inválida: " + estadoActual + " → " + nuevoEstado);
        }

        paquete.setEstadoActual(nuevoEstado);
        repo.guardar(paquete);

        historial.guardar(new EstadoHistorial(null, paqueteId, nuevoEstado,
            LocalDateTime.now(), usuarioResponsable));
    }

    private String generarCodigoRastreo() {
        return "RC" + LocalDate.now().getYear()
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
```

### 6.7 Consultas Personalizadas (RF-06, RF-07)

```java
// infrastructure/adapter/out/persistence/repository/PaqueteJpaRepository.java
public interface PaqueteJpaRepository extends JpaRepository<PaqueteEntity, UUID> {

    // RF-07: búsqueda por texto parcial en código de rastreo
    @Query(value = """
        SELECT p.* FROM paquetes p
        WHERE LOWER(p.codigo_rastreo) LIKE LOWER(CONCAT('%', :texto, '%'))
        """, nativeQuery = true)
    List<PaqueteEntity> buscarPorCodigoRastreo(@Param("texto") String texto);

    // RF-06: filtro por sucursal y estado
    @Query(value = """
        SELECT p.* FROM paquetes p
        WHERE (LOWER(p.sucursal_origen) = LOWER(:sucursal)
            OR LOWER(p.sucursal_destino) = LOWER(:sucursal))
          AND (:estado IS NULL OR p.estado_actual = :estado)
        """, nativeQuery = true)
    List<PaqueteEntity> buscarPorSucursalYEstado(
        @Param("sucursal") String sucursal,
        @Param("estado") String estado
    );
}
```

### 6.8 Controlador REST

```java
// infrastructure/adapter/in/rest/controller/PaqueteController.java
@RestController
@RequestMapping("/api/v1/paquetes")
public class PaqueteController {
    private final RegistrarPaqueteUseCase registrarUseCase;
    private final ConsultarPaqueteUseCase consultarUseCase;
    private final GestionarEstadoUseCase estadoUseCase;

    public PaqueteController(RegistrarPaqueteUseCase registrarUseCase,
                            ConsultarPaqueteUseCase consultarUseCase,
                            GestionarEstadoUseCase estadoUseCase) {
        this.registrarUseCase = registrarUseCase;
        this.consultarUseCase = consultarUseCase;
        this.estadoUseCase = estadoUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<PaqueteResponse>> crear(
            @Valid @RequestBody PaqueteRequest request) {
        Paquete paquete = registrarUseCase.registrar(request);
        return ApiResponse.created(toResponse(paquete));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaqueteResponse>> obtener(@PathVariable UUID id) {
        return ApiResponse.ok(toResponse(consultarUseCase.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<PaqueteResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PaqueteRequest request) {
        // Actualizar datos del paquete
        return ApiResponse.ok(toResponse(paquete));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambioEstadoRequest request) {
        String usuario = "operador"; // obtener de header X-User-Id
        estadoUseCase.cambiarEstado(id, request.nuevoEstado(), usuario);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<ApiResponse<List<HistorialResponse>>> historial(@PathVariable UUID id) {
        return ApiResponse.ok(estadoUseCase.obtenerHistorial(id).stream()
            .map(this::toHistorialResponse).toList());
    }

    @PostMapping("/{id}/categorias/{catId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<Void>> asignarCategoria(
            @PathVariable UUID id,
            @PathVariable UUID catId) {
        // Asignar categoría a paquete
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaqueteResponse>>> buscar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String sucursal,
            @RequestParam(required = false) String estado) {
        List<Paquete> resultados;
        if (busqueda != null) {
            resultados = consultarUseCase.buscarPorCodigoRastreo(busqueda);
        } else if (sucursal != null) {
            resultados = consultarUseCase.buscarPorSucursalYEstado(
                sucursal, estado != null ? EstadoPaquete.valueOf(estado) : null);
        } else {
            throw new IllegalArgumentException("Debe proporcionar busqueda o sucursal");
        }
        return ApiResponse.ok(resultados.stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        // eliminar lógica
        return ApiResponse.noContent();
    }
}
```

### 6.9 CategoriaController

```java
// infrastructure/adapter/in/rest/controller/CategoriaController.java
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> crear(
            @Valid @RequestBody CategoriaRequest request) {
        // Crear categoría
        return ApiResponse.created(categoriaResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> listar() {
        // Listar categorías
        return ApiResponse.ok(lista);
    }
}
```

### 6.10 ClienteFeignClient con Circuit Breaker

```java
// infrastructure/adapter/out/cliente/ClienteFeignClient.java
@FeignClient(name = "servicio-clientes")
public interface ClienteFeignClient {
    @GetMapping("/api/v1/clientes/{id}")
    ApiResponse<ClienteDto> buscarCliente(@PathVariable UUID id);
}

// En PaqueteService o en un adaptador separado:
@CircuitBreaker(name = "servicio-clientes", fallbackMethod = "fallbackCliente")
public ClienteDto obtenerCliente(UUID id) {
    return clienteFeignClient.buscarCliente(id).getData();
}

public ClienteDto fallbackCliente(UUID id, Exception ex) {
    throw new ExternalServiceException("servicio-clientes no disponible (circuit breaker abierto)");
}
```

### 6.9 Pruebas Unitarias (3 tests)

```java
// test/java/com/rapidocourier/paquetes/application/service/PaqueteServiceTest.java
@ExtendWith(MockitoExtension.class)
class PaqueteServiceTest {

    @Mock PaqueteRepositoryPort repo;
    @Mock HistorialRepositoryPort historial;
    @Mock ClienteFeignPort clienteFeign;
    @Spy TarifaCalculator tarifaCalculator = new TarifaCalculator();
    @InjectMocks PaqueteService service;

    @Test
    void cambiarEstado_transicionValida_guardaHistorial() {
        Paquete paquete = new Paquete(UUID.randomUUID(), "RC2025ABC", 
            UUID.randomUUID(), UUID.randomUUID(), 5.0, 100.0, 
            "LIMA", "AREQUIPA", 50.0, EstadoPaquete.REGISTRADO, 
            Set.of(), null, null);
        
        when(repo.buscarPorId(any())).thenReturn(Optional.of(paquete));
        when(repo.guardar(any())).thenAnswer(i -> i.getArgument(0));

        service.cambiarEstado(paquete.getId(), EstadoPaquete.EN_ALMACEN, "operador1");

        verify(historial).guardar(argThat(h -> 
            h.getEstado() == EstadoPaquete.EN_ALMACEN && 
            "operador1".equals(h.getUsuarioResponsable())));
    }

    @Test
    void cambiarEstado_transicionInvalida_lanzaException() {
        Paquete paquete = new Paquete(UUID.randomUUID(), "RC2025ABC",
            UUID.randomUUID(), UUID.randomUUID(), 5.0, 100.0,
            "LIMA", "AREQUIPA", 50.0, EstadoPaquete.REGISTRADO,
            Set.of(), null, null);
        
        when(repo.buscarPorId(any())).thenReturn(Optional.of(paquete));

        InvalidStateTransitionException ex = assertThrows(
            InvalidStateTransitionException.class,
            () -> service.cambiarEstado(paquete.getId(), EstadoPaquete.ENTREGADO, "operador1"));

        assertTrue(ex.getMessage().contains("REGISTRADO"));
        assertTrue(ex.getMessage().contains("ENTREGADO"));
    }

    @Test
    void buscarPorSucursal_sinResultados_retornaListaVacia() {
        when(repo.buscarPorSucursalYEstado("MOQUEGUA", null))
            .thenReturn(List.of());
        
        assertTrue(service.buscarPorSucursalYEstado("MOQUEGUA", null).isEmpty());
    }
}
```

---

## Fase 7: Orquestación Docker

### 7.1 docker-compose.yml

```yaml
version: "3.9"

networks:
  rapidocourier-net:
    driver: bridge

volumes:
  postgres_auth_data:
  postgres_clientes_data:
  postgres_paquetes_data:

services:

  vault:
    image: hashicorp/vault:1.15
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: ${VAULT_TOKEN}
      VAULT_DEV_LISTEN_ADDRESS: 0.0.0.0:8200
    ports:
      - "8200:8200"
    cap_add:
      - IPC_LOCK
    networks:
      - rapidocourier-net

  postgres-auth:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: rapidocourier_auth
      POSTGRES_USER: auth_user
      POSTGRES_PASSWORD_FILE: /run/secrets/auth_db_password
    volumes:
      - postgres_auth_data:/var/lib/postgresql/data
    networks:
      - rapidocourier-net

  postgres-clientes:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: rapidocourier_clientes
      POSTGRES_USER: clientes_user
      POSTGRES_PASSWORD_FILE: /run/secrets/clientes_db_password
    volumes:
      - postgres_clientes_data:/var/lib/postgresql/data
    networks:
      - rapidocourier-net

  postgres-paquetes:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: rapidocourier_paquetes
      POSTGRES_USER: paquetes_user
      POSTGRES_PASSWORD_FILE: /run/secrets/paquetes_db_password
    volumes:
      - postgres_paquetes_data:/var/lib/postgresql/data
    networks:
      - rapidocourier-net

  config-server:
    build: ./config-server
    environment:
      SPRING_CLOUD_CONFIG_SERVER_GIT_URI: ${CONFIG_GIT_URI}
      SPRING_CLOUD_VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_CLOUD_VAULT_HOST: vault
    depends_on:
      vault:
        condition: service_healthy
    networks:
      - rapidocourier-net

  eureka-server:
    build: ./eureka-server
    depends_on:
      config-server:
        condition: service_healthy
    networks:
      - rapidocourier-net

  servicio-auth:
    build: ./servicio-auth
    environment:
      SPRING_CLOUD_VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_CLOUD_VAULT_HOST: vault
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      DB_HOST: postgres-auth
    depends_on:
      postgres-auth:
        condition: service_healthy
      vault:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - rapidocourier-net

  servicio-clientes:
    build: ./servicio-clientes
    environment:
      SPRING_CLOUD_VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_CLOUD_VAULT_HOST: vault
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      DB_HOST: postgres-clientes
    depends_on:
      postgres-clientes:
        condition: service_healthy
      vault:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - rapidocourier-net

  servicio-paquetes:
    build: ./servicio-paquetes
    environment:
      SPRING_CLOUD_VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_CLOUD_VAULT_HOST: vault
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      DB_HOST: postgres-paquetes
    depends_on:
      postgres-paquetes:
        condition: service_healthy
      vault:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      servicio-clientes:
        condition: service_started
    networks:
      - rapidocourier-net

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    environment:
      SPRING_CLOUD_VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_CLOUD_VAULT_HOST: vault
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      eureka-server:
        condition: service_healthy
    networks:
      - rapidocourier-net
```

### 7.2 Secuencia de Arranque

```bash
# 1. Crear .env con VAULT_TOKEN y CONFIG_GIT_URI

# 2. Levantar solo Vault
docker compose up vault -d

# 3. Cargar secretos en Vault
docker exec vault vault kv put secret/rapidocourier/auth \
  db.username="auth_user" db.password="secure_pass" jwt.secret="clave-32-caracteres-minimo"

docker exec vault vault kv put secret/rapidocourier/clientes \
  db.username="clientes_user" db.password="secure_pass" reniec.token="Bearer token"

docker exec vault vault kv put secret/rapidocourier/paquetes \
  db.username="paquetes_user" db.password="secure_pass"

# 4. Levantar todo el ecosistema
docker compose up --build -d
```

---

## Validaciones Bean Validation (mínimo 6 tipos)

Usar obligatoriamente en los DTOs de request:

| Anotación | Ejemplo de uso |
|-----------|----------------|
| `@NotBlank` | `String dni` |
| `@NotNull` | `UUID remitenteId` |
| `@Email` | `String email` |
| `@Pattern` | `@Pattern(regexp = "\\d{8}") String dni` |
| `@Positive` | `Double pesoKg` |
| `@Size` | `@Size(max = 500) String descripcion` |
| `@PositiveOrZero` | `Double valorDeclarado` |

---

## GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, List<String>> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
            errores.computeIfAbsent(e.getField(), k -> new ArrayList<>())
                   .add(e.getDefaultMessage()));
        return ResponseEntity.badRequest()
            .body(new ApiResponse<>(false, "Validación fallida", errores));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ConflictException.class, InvalidStateTransitionException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(RuntimeException ex) {
        return ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternal(ExternalServiceException ex) {
        return ApiResponse.error(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
            "Error inesperado: " + ex.getMessage());
    }
}
```

---

## Códigos HTTP Requeridos

| Situación | Código |
|-----------|--------|
| GET / PUT exitoso | 200 |
| POST exitoso | 201 |
| DELETE exitoso | 204 |
| Validación fallida | 400 con `Map<String, List<String>>` por campo |
| No encontrado | 404 |
| Conflicto / duplicado / transición inválida | 409 |
| Acceso denegado | 403 |
| Fallo de API externa (RENIEC / Feign) | 502 |
| Error inesperado | 500 |

---

## Resumen de Entregables

| Fase | Servicios | Pruebas |
|------|-----------|---------|
| 1 | Estructura, .env | - |
| 2 | Vault, PostgreSQL ×3, Eureka, Gateway, Config | - |
| 3 | Secrets en Vault por servicio | - |
| 4 | servicio-auth | 4 tests |
| 5 | servicio-clientes | 4 tests |
| 6 | servicio-paquetes | 3 tests |
| 7 | docker-compose.yml | - |

---

## Endpoints de la API

| Método | Path | Descripción | Roles |
|--------|------|-------------|-------|
| POST | /api/v1/auth/login | Login, retorna JWT | Público |
| POST | /api/v1/auth/register | Registro de usuario | Público |
| POST | /api/v1/clientes | Registrar cliente (RENIEC) | ADMIN, OPERADOR |
| GET | /api/v1/clientes | Listar clientes | ADMIN, OPERADOR |
| GET | /api/v1/clientes/{id} | Obtener cliente por ID | ADMIN, OPERADOR |
| DELETE | /api/v1/clientes/{id} | Eliminar cliente | ADMIN |
| POST | /api/v1/paquetes | Registrar paquete (calcula tarifa) | ADMIN, OPERADOR |
| GET | /api/v1/paquetes/{id} | Obtener paquete con datos de clientes | Autenticado |
| PUT | /api/v1/paquetes/{id} | Actualizar datos del paquete | ADMIN, OPERADOR |
| DELETE | /api/v1/paquetes/{id} | Eliminar paquete | ADMIN |
| PATCH | /api/v1/paquetes/{id}/estado | Cambiar estado (valida transición) | ADMIN, OPERADOR |
| GET | /api/v1/paquetes/{id}/historial | Historial de estados | Autenticado |
| GET | /api/v1/paquetes?busqueda=texto | Búsqueda por código o nombre de cliente | Autenticado |
| GET | /api/v1/paquetes?sucursal=...&estado=... | Filtrar por sucursal y estado | Autenticado |
| POST | /api/v1/paquetes/{id}/categorias/{catId} | Asignar categoría a paquete | ADMIN, OPERADOR |
| POST | /api/v1/categorias | Crear categoría | ADMIN |
| GET | /api/v1/categorias | Listar categorías | ADMIN, OPERADOR |