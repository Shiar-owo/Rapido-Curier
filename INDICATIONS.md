# Prompt de implementación — RapidoCourier S.A.C.

Eres un experto en Java 17, Spring Boot 3.x y arquitectura de microservicios. Tu tarea es implementar **desde cero** el backend completo de **RapidoCourier S.A.C.**, una empresa peruana de mensajería con sucursales en Lima, Arequipa y Cusco.

---

## Contexto del negocio

RapidoCourier gestiona envíos de paquetes entre personas naturales y empresas. Las tarifas varían según el peso, el valor declarado y la ruta entre sucursales. El sistema debe estar digitalizado como una arquitectura de microservicios distribuida desde el primer día.

---

## Stack tecnológico obligatorio

- **Java 17**
- **Spring Boot 3.x**
- **Maven** (cada microservicio es un módulo Maven independiente)
- **Docker + Docker Compose** — todo el ecosistema corre en contenedores; no se acepta arranque local sin Docker
- **`application.yaml`** — nunca `application.properties`
- **UUID** como tipo de clave primaria en todas las entidades
- **Inyección de dependencias por constructor** — nunca `@Autowired` en campo
- **Clase genérica `ApiResponse<T>`** en todos los endpoints de todos los servicios

---

## Arquitectura interna de cada microservicio: Hexagonal (Ports & Adapters)

Aplica esta estructura de paquetes **en todos los microservicios de negocio**:

```
com.rapidocourier.<servicio>/
├── domain/
│   ├── model/           ← POJOs puros, sin anotaciones de frameworks
│   ├── port/
│   │   ├── in/          ← interfaces de casos de uso (lo que el exterior pide al dominio)
│   │   └── out/         ← interfaces de puertos de salida (lo que el dominio pide al exterior)
│   └── exception/       ← excepciones de dominio personalizadas
├── application/
│   └── service/         ← implementa los casos de uso, usa solo los ports out
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/     ← @RestController, DTOs request/response
    │   └── out/
    │       ├── persistence/  ← @Entity JPA, JpaRepository, mapper entidad↔dominio
    │       └── <externo>/    ← adaptadores Feign u otros clientes externos
    └── config/          ← @Configuration, SecurityConfig, BeanConfig
```

**Reglas de la arquitectura hexagonal:**
- El modelo de dominio (`domain/model/`) no importa nada de Spring, JPA ni Feign.
- La capa `application/service/` solo depende de `domain/`.
- Los adaptadores (`infrastructure/`) dependen del dominio pero no entre sí.
- Los `@RestController` reciben e inyectan los casos de uso (`port/in/`), no los servicios directamente.
- Las entidades JPA (`@Entity`) viven en `infrastructure/adapter/out/persistence/` y nunca se comparten entre servicios.
- La comunicación entre servicios usa DTOs o IDs, nunca entidades JPA.

---

## Estructura del repositorio

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
├── config-repo/                  ← repositorio Git con los YAMLs de configuración
│   ├── application.yaml
│   ├── servicio-auth.yaml
│   ├── servicio-clientes.yaml
│   └── servicio-paquetes.yaml
├── docker-compose.yml            ← orquesta todo el ecosistema
├── .env                          ← variables de entorno (NO commitear)
├── .env.example                  ← plantilla de variables (SÍ commitear)
├── RESPUESTAS-CONCEPTUALES.md
└── README.md
```

---

## Docker: reglas generales obligatorias

- **Cada microservicio tiene su propio `Dockerfile`** en la raíz de su carpeta.
- **`docker-compose.yml`** en la raíz del repositorio orquesta todos los servicios.
- Los microservicios de negocio **no exponen sus puertos al host** — solo el Gateway (`8080`) es accesible desde el exterior.
- **HashiCorp Vault es la única fuente de verdad para todos los secretos**: credenciales de BD por servicio, token de RENIEC y clave JWT. Ningún secreto va en `.env` ni en `docker-compose.yml`.
- El `.env` contiene **únicamente** el `VAULT_TOKEN` (bootstrap inevitable) y la URL del repo Git del Config Server (no sensible). Está en `.gitignore`.
- Se entrega un `.env.example` con las claves pero sin valores. Sí se commitea.
- Cada base de datos tiene su **propio usuario y contraseña** en su path de Vault. Ninguna BD comparte credenciales con otra.
- Todos los servicios pertenecen a la misma red Docker: `rapidocourier-net`.
- Las bases de datos persisten datos en volúmenes nombrados Docker.

### Dockerfile estándar (igual para todos los microservicios)

```dockerfile
# Etapa 1: build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Etapa 2: runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### .env.example

```dotenv
# Bootstrap token de Vault — único secreto que no puede venir de Vault mismo
VAULT_TOKEN=

# URL del repositorio Git con los YAMLs de configuración (no sensible)
CONFIG_GIT_URI=https://github.com/tu-usuario/rapidocourier-config
```

> **Por qué solo estas dos variables:**
> - `VAULT_TOKEN` es el bootstrap inevitable: necesitas la llave para abrir la caja fuerte, y esa llave no puede estar dentro de la caja fuerte.
> - `CONFIG_GIT_URI` no es sensible (es una URL) pero varía entre entornos, así que va en el entorno.
> - Todo lo demás (passwords de BD, token RENIEC, JWT secret) vive en Vault.

### docker-compose.yml completo

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

  # ── Vault (fuente de verdad de todos los secretos) ───────────────────────────

  vault:
    image: hashicorp/vault:1.15
    container_name: vault
    restart: unless-stopped
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: ${VAULT_TOKEN}   # mismo token para simplificar el setup
      VAULT_DEV_LISTEN_ADDRESS: 0.0.0.0:8200
    ports:
      - "8200:8200"   # expuesto solo para el setup inicial; cerrar en producción real
    cap_add:
      - IPC_LOCK
    networks:
      - rapidocourier-net
    healthcheck:
      test: ["CMD", "vault", "status"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ── Bases de datos (credenciales iniciales también vienen de Vault vía init script) ──

  postgres-auth:
    image: postgres:16-alpine
    container_name: postgres-auth
    restart: unless-stopped
    environment:
      POSTGRES_DB: rapidocourier_auth
      # usuario y contraseña se setean vía script de inicialización que lee de Vault
      # para el arranque inicial Docker necesita al menos POSTGRES_PASSWORD
      POSTGRES_USER: auth_user
      POSTGRES_PASSWORD_FILE: /run/secrets/auth_db_password
    volumes:
      - postgres_auth_data:/var/lib/postgresql/data
    networks:
      - rapidocourier-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U auth_user -d rapidocourier_auth"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres-clientes:
    image: postgres:16-alpine
    container_name: postgres-clientes
    restart: unless-stopped
    environment:
      POSTGRES_DB: rapidocourier_clientes
      POSTGRES_USER: clientes_user
      POSTGRES_PASSWORD_FILE: /run/secrets/clientes_db_password
    volumes:
      - postgres_clientes_data:/var/lib/postgresql/data
    networks:
      - rapidocourier-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U clientes_user -d rapidocourier_clientes"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres-paquetes:
    image: postgres:16-alpine
    container_name: postgres-paquetes
    restart: unless-stopped
    environment:
      POSTGRES_DB: rapidocourier_paquetes
      POSTGRES_USER: paquetes_user
      POSTGRES_PASSWORD_FILE: /run/secrets/paquetes_db_password
    volumes:
      - postgres_paquetes_data:/var/lib/postgresql/data
    networks:
      - rapidocourier-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U paquetes_user -d rapidocourier_paquetes"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ── Spring Cloud Infrastructure ─────────────────────────────────────────────

  config-server:
    build: ./config-server
    container_name: config-server
    restart: unless-stopped
    environment:
      SPRING_CLOUD_CONFIG_SERVER_GIT_URI: ${CONFIG_GIT_URI}
      SPRING_CLOUD_VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_CLOUD_VAULT_HOST: vault
    depends_on:
      vault:
        condition: service_healthy
    networks:
      - rapidocourier-net
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8888/actuator/health | grep UP"]
      interval: 15s
      timeout: 5s
      retries: 10

  eureka-server:
    build: ./eureka-server
    container_name: eureka-server
    restart: unless-stopped
    depends_on:
      config-server:
        condition: service_healthy
    networks:
      - rapidocourier-net
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8761/actuator/health | grep UP"]
      interval: 15s
      timeout: 5s
      retries: 10

  # ── Microservicios de negocio ────────────────────────────────────────────────
  # Solo reciben VAULT_TOKEN y hosts de infraestructura.
  # Todas las credenciales las leen de Vault al arrancar.

  servicio-auth:
    build: ./servicio-auth
    container_name: servicio-auth
    restart: unless-stopped
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
    container_name: servicio-clientes
    restart: unless-stopped
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
    container_name: servicio-paquetes
    restart: unless-stopped
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

  # ── API Gateway (único punto de entrada) ────────────────────────────────────

  api-gateway:
    build: ./api-gateway
    container_name: api-gateway
    restart: unless-stopped
    ports:
      - "8080:8080"   # ÚNICO puerto expuesto al host
    environment:
      SPRING_CLOUD_VAULT_TOKEN: ${VAULT_TOKEN}
      SPRING_CLOUD_VAULT_HOST: vault
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      eureka-server:
        condition: service_healthy
      servicio-auth:
        condition: service_started
      servicio-clientes:
        condition: service_started
      servicio-paquetes:
        condition: service_started
    networks:
      - rapidocourier-net
```

### Configuración de los servicios Spring dentro de Docker

Cuando un servicio corre en Docker, los hostnames cambian. Actualizar los `application.yaml` de cada servicio para que lean del entorno:

```yaml
# Patrón: leer de variable de entorno con fallback para desarrollo local
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/rapidocourier_clientes}
    username: ${SPRING_DATASOURCE_USERNAME:rapidocourier}
    password: ${SPRING_DATASOURCE_PASSWORD:}
  config:
    import: ${SPRING_CONFIG_IMPORT:configserver:http://localhost:8888}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}

spring:
  cloud:
    vault:
      host: ${SPRING_CLOUD_VAULT_HOST:localhost}
      port: 8200
      token: ${SPRING_CLOUD_VAULT_TOKEN:}
```

### Setup inicial de Vault (ejecutar una sola vez tras `docker compose up vault`)

Cada microservicio lee sus secretos de su propio path en Vault. Ningún servicio puede leer el path de otro.

```bash
# 1. Levantar solo Vault primero
docker compose up vault -d

# 2. Cargar secretos de servicio-auth
docker exec vault vault kv put secret/rapidocourier/auth \
  db.username="auth_user" \
  db.password="auth_s3cr3t_!" \
  jwt.secret="clave-hmac-minimo-32-caracteres-aqui"

# 3. Cargar secretos de servicio-clientes
docker exec vault vault kv put secret/rapidocourier/clientes \
  db.username="clientes_user" \
  db.password="clientes_s3cr3t_!" \
  reniec.token="Bearer eyJ..."

# 4. Cargar secretos de servicio-paquetes
docker exec vault vault kv put secret/rapidocourier/paquetes \
  db.username="paquetes_user" \
  db.password="paquetes_s3cr3t_!"

# 5. Verificar
docker exec vault vault kv get secret/rapidocourier/auth
docker exec vault vault kv get secret/rapidocourier/clientes
docker exec vault vault kv get secret/rapidocourier/paquetes

# 6. Levantar el resto del ecosistema
docker compose up --build -d
```

### Cómo Spring Boot lee los secretos de Vault

Cada servicio tiene en su `application.yaml`:

```yaml
spring:
  cloud:
    vault:
      host: ${SPRING_CLOUD_VAULT_HOST:localhost}
      port: 8200
      token: ${SPRING_CLOUD_VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        default-context: rapidocourier/<nombre-servicio>  # cada uno lee solo su path
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/rapidocourier_<nombre-servicio>
    username: ${db.username}       # viene de Vault
    password: ${db.password}       # viene de Vault
```

`jwt.secret` y `reniec.token` también se inyectan automáticamente desde Vault con `@Value("${jwt.secret}")` y `@Value("${reniec.token}")`.

---

## Servicios de infraestructura

### eureka-server
- Puerto: **8761**
- Dependencia: `spring-cloud-starter-netflix-eureka-server`
- Clase principal con `@EnableEurekaServer`
- `application.yaml`:
  ```yaml
  server:
    port: 8761
  eureka:
    client:
      register-with-eureka: false
      fetch-registry: false
  ```

### config-server
- Puerto: **8888**
- Dependencia: `spring-cloud-config-server`
- Clase principal con `@EnableConfigServer`
- Apunta a un repositorio Git que contiene los YAMLs de cada servicio:
  - `application.yaml` (configuración común)
  - `servicio-auth.yaml`
  - `servicio-clientes.yaml`
  - `servicio-paquetes.yaml`
- Expone `/actuator/refresh` con `management.endpoints.web.exposure.include: refresh`

### api-gateway
- Puerto: **8080**
- Dependencia: `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client`
- Registrado en Eureka con `spring.application.name: api-gateway`
- Rutas en `application.yaml`:
  ```yaml
  spring:
    cloud:
      gateway:
        routes:
          - id: auth
            uri: lb://servicio-auth
            predicates:
              - Path=/api/v1/auth/**
          - id: clientes
            uri: lb://servicio-clientes
            predicates:
              - Path=/api/v1/clientes/**
          - id: paquetes
            uri: lb://servicio-paquetes
            predicates:
              - Path=/api/v1/paquetes/**,/api/v1/categorias/**
  ```
- Filtro JWT centralizado: valida el token en el Gateway antes de reenviar la petición. Propaga el usuario y sus roles como headers (`X-User-Id`, `X-User-Roles`) hacia los microservicios downstream.
- Los microservicios de negocio **no están expuestos directamente al exterior** (sus puertos no se publican en producción local).

---

## Microservicios de negocio

### servicio-auth

**Bounded context:** Autenticación y autorización  
**Puerto:** 8081  
**Base de datos:** PostgreSQL (`rapidocourier_auth`)  
**Dependencias clave:** `spring-boot-starter-security`, `jjwt`, `spring-cloud-starter-vault-config`, `spring-cloud-starter-netflix-eureka-client`

**Entidades JPA:**

```java
// infrastructure/adapter/out/persistence/UsuarioEntity.java
@Entity @Table(name = "usuarios")
public class UsuarioEntity {
    @Id UUID id;
    @Column(unique = true) String username;
    String password; // BCrypt
    @ManyToMany(fetch = FetchType.EAGER) Set<RolEntity> roles;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

// infrastructure/adapter/out/persistence/RolEntity.java
@Entity @Table(name = "roles")
public class RolEntity {
    @Id UUID id;
    @Enumerated(EnumType.STRING) RolNombre nombre; // ADMIN, OPERADOR, CLIENTE
}
```

**Modelo de dominio:**
```java
// domain/model/Usuario.java — POJO puro
public class Usuario {
    private UUID id;
    private String username;
    private String passwordHash;
    private Set<String> roles;
}
```

**Puertos:**
```java
// domain/port/in/LoginUseCase.java
public interface LoginUseCase {
    String login(String username, String password); // retorna JWT
}

// domain/port/out/UsuarioRepositoryPort.java
public interface UsuarioRepositoryPort {
    Optional<Usuario> buscarPorUsername(String username);
    boolean existePorUsername(String username);
    Usuario guardar(Usuario usuario);
}

// domain/port/out/JwtPort.java
public interface JwtPort {
    String generarToken(Usuario usuario);
    Claims parsearToken(String token);
}
```

**Servicio de aplicación:**
```java
// application/service/AuthService.java
@Service
public class AuthService implements LoginUseCase {
    private final UsuarioRepositoryPort usuarios;
    private final JwtPort jwt;
    private final PasswordEncoder encoder;

    public AuthService(UsuarioRepositoryPort usuarios, JwtPort jwt, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.jwt = jwt;
        this.encoder = encoder;
    }

    @Override
    public String login(String username, String password) {
        Usuario u = usuarios.buscarPorUsername(username)
            .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));
        if (!encoder.matches(password, u.getPasswordHash()))
            throw new CredencialesInvalidasException("Contraseña incorrecta");
        return jwt.generarToken(u);
    }
}
```

**JWT:**
- Firmado con clave HMAC leída desde **HashiCorp Vault** (nunca en archivos de configuración del repositorio).
- Configuración Vault en `application.yaml`:
  ```yaml
  spring:
    cloud:
      vault:
        host: localhost
        port: 8200
        token: ${VAULT_TOKEN}
        kv:
          enabled: true
          backend: secret
          default-context: rapidocourier
  ```
- En Vault, guardar: `secret/rapidocourier` → `jwt.secret=<clave-hmac>`
- Leer con `@Value("${jwt.secret}")` en el adaptador `JwtAdapter`.

**DataInitializer:** Al arrancar, insertar:
- Roles: `ADMIN`, `OPERADOR`, `CLIENTE`
- Usuarios: `admin/admin123`, `operador/op123`, `cliente/cl123`

**Endpoints públicos:**
- `POST /api/v1/auth/login` → `{ "username": "...", "password": "..." }` → retorna `ApiResponse<String>` con el JWT
- `POST /api/v1/auth/register` → registro de nuevos usuarios (rol CLIENTE por defecto)

---

### servicio-clientes

**Bounded context:** Registro y gestión de clientes  
**Puerto:** 8082  
**Base de datos:** PostgreSQL (`rapidocourier_clientes`)  
**Dependencias clave:** `spring-cloud-openfeign`, `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-config`, `resilience4j`

**Entidades JPA:**

```java
// infrastructure/adapter/out/persistence/ClienteEntity.java
@Entity @Table(name = "clientes")
public class ClienteEntity {
    @Id UUID id;
    @Column(unique = true) String dni;
    String nombreCompleto; // obtenido de RENIEC, nunca del request
    @Column(unique = true) String email;
    LocalDateTime createdAt;   // @CreatedDate
    LocalDateTime updatedAt;   // @LastModifiedDate
}
```

**Modelo de dominio:**
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

**Puertos:**
```java
// domain/port/in/RegistrarClienteUseCase.java
public interface RegistrarClienteUseCase {
    Cliente registrar(String dni, String email);
}

// domain/port/in/ConsultarClienteUseCase.java
public interface ConsultarClienteUseCase {
    Cliente buscarPorId(UUID id);
    List<Cliente> listarTodos();
    void eliminar(UUID id); // solo ADMIN
}

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
    String obtenerNombreCompleto(String dni); // lanza ExternalServiceException si falla
}
```

**Feign Client (adaptador):**
```java
// infrastructure/adapter/out/reniec/ReniecFeignClient.java
@FeignClient(name = "reniec", url = "${reniec.api.url}")
public interface ReniecFeignClient {
    @GetMapping
    ReniecResponseDto consultarDni(
        @RequestHeader("Authorization") String bearer,
        @RequestParam("dni") String dni
    );
}

// infrastructure/adapter/out/reniec/ReniecAdapter.java
@Component
public class ReniecAdapter implements ReniecPort {
    private final ReniecFeignClient client;

    @Value("${reniec.api.token}")
    private String token;

    public ReniecAdapter(ReniecFeignClient client) { this.client = client; }

    @Override
    public String obtenerNombreCompleto(String dni) {
        try {
            return client.consultarDni("Bearer " + token, dni).getNombreCompleto();
        } catch (Exception e) {
            throw new ExternalServiceException("RENIEC no disponible: " + e.getMessage());
        }
    }
}
```

**Configuración en `servicio-clientes.yaml` (Config Server):**
```yaml
reniec:
  api:
    url: https://api.decolecta.com/v1/reniec/dni
    token: ${RENIEC_TOKEN}   # variable de entorno, no hardcodeado
```

**Circuit Breaker en la llamada Feign:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      reniec:
        sliding-window-size: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

**Clase principal:**
```java
@SpringBootApplication
@EnableFeignClients
public class ServicioClientesApplication { ... }
```

**DTO de request (nunca incluye `nombreCompleto`):**
```java
// infrastructure/adapter/in/rest/dto/request/ClienteRequest.java
public record ClienteRequest(
    @NotBlank @Pattern(regexp = "\\d{8}") String dni,
    @NotBlank @Email String email
) {}
```

**DTO de response:**
```java
public record ClienteResponse(UUID id, String dni, String nombreCompleto, String email) {}
```

**Consultas personalizadas:**
```java
// infrastructure/adapter/out/persistence/ClienteJpaRepository.java
public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, UUID> {
    Optional<ClienteEntity> findByEmail(String email);
    Optional<ClienteEntity> findByDni(String dni);
}
```

---

### servicio-paquetes

**Bounded context:** Gestión de paquetes, estados, historial y categorías  
**Puerto:** 8083  
**Base de datos:** PostgreSQL (`rapidocourier_paquetes`)  
**Dependencias clave:** `spring-cloud-openfeign` (para llamar a `servicio-clientes`), `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-config`, `resilience4j`

**Entidades JPA:**

```java
// PaqueteEntity.java
@Entity @Table(name = "paquetes")
public class PaqueteEntity {
    @Id UUID id;
    @Column(unique = true) String codigoRastreo;
    UUID remitenteId;   // ID del cliente en servicio-clientes
    UUID destinatarioId;
    Double pesoKg;
    Double valorDeclarado;
    String sucursalOrigen;   // LIMA | AREQUIPA | CUSCO
    String sucursalDestino;
    Double tarifa;           // calculada automáticamente
    @Enumerated(EnumType.STRING) EstadoPaquete estadoActual;
    @ManyToMany @JoinTable(name = "paquete_categoria",
        joinColumns = @JoinColumn(name = "paquete_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    Set<CategoriaEntity> categorias = new HashSet<>();
    LocalDateTime createdAt;  // @CreatedDate
    LocalDateTime updatedAt;  // @LastModifiedDate
}

// EstadoHistorialEntity.java
@Entity @Table(name = "estado_historial")
public class EstadoHistorialEntity {
    @Id UUID id;
    @ManyToOne @JoinColumn(name = "paquete_id") PaqueteEntity paquete;
    @Enumerated(EnumType.STRING) EstadoPaquete estado;
    LocalDateTime fechaCambio;
    String usuarioResponsable; // username del operador/admin
}

// CategoriaEntity.java
@Entity @Table(name = "categorias")
public class CategoriaEntity {
    @Id UUID id;
    @Column(unique = true) String nombre; // FRAGIL, REFRIGERADO, DOCUMENTOS, SOBREDIMENSIONADO
    String descripcion;
}
```

**Estados y transiciones válidas:**
```java
public enum EstadoPaquete {
    REGISTRADO, EN_ALMACEN, EN_TRANSITO, EN_REPARTO, ENTREGADO, NO_ENTREGADO
}

// Transiciones permitidas:
// REGISTRADO    → EN_ALMACEN
// EN_ALMACEN    → EN_TRANSITO
// EN_TRANSITO   → EN_REPARTO
// EN_REPARTO    → ENTREGADO
// EN_REPARTO    → NO_ENTREGADO
// NO_ENTREGADO  → EN_ALMACEN  (reintento)

public static final Map<EstadoPaquete, Set<EstadoPaquete>> TRANSICIONES = Map.of(
    REGISTRADO,    Set.of(EN_ALMACEN),
    EN_ALMACEN,    Set.of(EN_TRANSITO),
    EN_TRANSITO,   Set.of(EN_REPARTO),
    EN_REPARTO,    Set.of(ENTREGADO, NO_ENTREGADO),
    NO_ENTREGADO,  Set.of(EN_ALMACEN)
);
```

**Regla de cálculo de tarifa (RF-03):**
```
tarifa = (pesoKg × 8.0) + (valorDeclarado × 0.01) + tarifaRuta

tarifaRuta según ruta:
  LIMA ↔ AREQUIPA   = S/ 15.00
  LIMA ↔ CUSCO      = S/ 20.00
  AREQUIPA ↔ CUSCO  = S/ 12.00
  misma sucursal    = S/  5.00
```

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

**Feign Client hacia servicio-clientes:**
```java
@FeignClient(name = "servicio-clientes")
public interface ClienteFeignClient {
    @GetMapping("/api/v1/clientes/{id}")
    ApiResponse<ClienteDto> buscarCliente(@PathVariable UUID id);
}
```

**Consultas personalizadas (RF-06, RF-07):**
```java
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

    // Consulta multi-entidad: paquete + historial
    @Query(value = """
        SELECT p.*, COUNT(h.id) as total_cambios
        FROM paquetes p
        LEFT JOIN estado_historial h ON h.paquete_id = p.id
        WHERE p.id = :paqueteId
        GROUP BY p.id
        """, nativeQuery = true)
    Optional<Object[]> buscarConResumenHistorial(@Param("paqueteId") UUID paqueteId);
}
```

**DTOs de request:**
```java
// PaqueteRequest.java
public record PaqueteRequest(
    @NotNull UUID remitenteId,
    @NotNull UUID destinatarioId,
    @Positive Double pesoKg,
    @PositiveOrZero Double valorDeclarado,
    @NotBlank String sucursalOrigen,
    @NotBlank String sucursalDestino,
    @Size(max = 500) String descripcion
) {}

// CambioEstadoRequest.java
public record CambioEstadoRequest(
    @NotNull EstadoPaquete nuevoEstado
) {}
```

**DTO de response con datos anidados (de servicio-clientes):**
```java
// PaqueteResponse.java
public record PaqueteResponse(
    UUID id,
    String codigoRastreo,
    ClienteDto remitente,      // datos obtenidos de servicio-clientes vía Feign
    ClienteDto destinatario,
    Double pesoKg,
    Double valorDeclarado,
    String sucursalOrigen,
    String sucursalDestino,
    Double tarifa,
    EstadoPaquete estadoActual,
    List<String> categorias,
    LocalDateTime createdAt
) {}
```

**Generación del código de rastreo:**
```java
public String generarCodigo() {
    return "RC" + LocalDate.now().getYear()
        + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
}
```

---

## Clase genérica ApiResponse (copiar en cada servicio)

```java
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Creado exitosamente", data));
    }

    public static ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.noContent().build();
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
            .body(new ApiResponse<>(false, message, null));
    }
}
```

---

## Códigos HTTP requeridos (RNF-02)

| Situación | Código |
|---|---|
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

## GlobalExceptionHandler (replicar en cada microservicio de negocio)

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

## Validaciones Bean Validation requeridas (mínimo 6 tipos distintos)

Usar obligatoriamente en los DTOs de request:

| Anotación | Ejemplo de uso |
|---|---|
| `@NotBlank` | `String dni` |
| `@NotNull` | `UUID remitenteId` |
| `@Email` | `String email` |
| `@Pattern` | `@Pattern(regexp = "\\d{8}") String dni` |
| `@Positive` | `Double pesoKg` |
| `@Size` | `@Size(max = 500) String descripcion` |
| `@PositiveOrZero` | `Double valorDeclarado` |

---

## Seguridad JWT (RF-08)

**Estrategia:** validación centralizada en el API Gateway.

El Gateway valida el JWT en un `GlobalFilter`, extrae el username y roles, y los propaga como headers:
```
X-User-Id: <uuid>
X-User-Roles: ADMIN,OPERADOR
```

Los microservicios de negocio leen estos headers para aplicar autorización. No ejecutan validación JWT propia.

**Roles y restricciones:**
- `ADMIN`: puede crear, actualizar y eliminar cualquier recurso
- `OPERADOR`: puede crear y actualizar paquetes; no puede eliminar
- `CLIENTE`: solo puede consultar sus propios paquetes e historial

**Anotaciones de autorización en controladores:**
```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) { ... }

@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
public ResponseEntity<ApiResponse<PaqueteResponse>> crear(...) { ... }
```

---

## Pruebas unitarias (Sección 9 — mínimo para aprobar)

Crear en **servicio-clientes** y **servicio-paquetes**. Usar `@ExtendWith(MockitoExtension.class)`.

```java
// servicio-clientes: ClienteServiceTest.java
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock ClienteRepositoryPort repositorio;
    @Mock ReniecPort reniec;
    @InjectMocks ClienteService service;

    @Test
    void registrar_happyPath_retornaCliente() {
        when(repositorio.buscarPorEmail("a@b.com")).thenReturn(Optional.empty());
        when(repositorio.buscarPorDni("12345678")).thenReturn(Optional.empty());
        when(reniec.obtenerNombreCompleto("12345678")).thenReturn("Juan Pérez");
        when(repositorio.guardar(any())).thenAnswer(i -> i.getArgument(0));

        Cliente resultado = service.registrar("12345678", "a@b.com");

        assertNotNull(resultado);
        assertEquals("Juan Pérez", resultado.getNombreCompleto());
    }

    @Test
    void registrar_emailDuplicado_lanzaConflictException() {
        when(repositorio.buscarPorEmail("a@b.com"))
            .thenReturn(Optional.of(new Cliente()));

        ConflictException ex = assertThrows(ConflictException.class,
            () -> service.registrar("12345678", "a@b.com"));

        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    void registrar_reniecFalla_lanzaExternalServiceException() {
        when(repositorio.buscarPorEmail(any())).thenReturn(Optional.empty());
        when(reniec.obtenerNombreCompleto(any()))
            .thenThrow(new ExternalServiceException("RENIEC caído"));

        assertThrows(ExternalServiceException.class,
            () -> service.registrar("12345678", "a@b.com"));
    }

    @Test
    void listar_repositorioVacio_retornaListaVacia() {
        when(repositorio.listarTodos()).thenReturn(List.of());
        assertTrue(service.listarTodos().isEmpty());
    }
}
```

```java
// servicio-paquetes: PaqueteServiceTest.java
@ExtendWith(MockitoExtension.class)
class PaqueteServiceTest {

    @Mock PaqueteRepositoryPort repositorio;
    @Mock HistorialRepositoryPort historial;
    @Mock ClienteFeignClient clienteFeign;
    @Spy  TarifaCalculator tarifaCalculator;
    @InjectMocks PaqueteService service;

    @Test
    void cambiarEstado_transicionValida_guardaHistorial() {
        PaqueteEntity p = paqueteConEstado(REGISTRADO);
        when(repositorio.buscarPorId(any())).thenReturn(Optional.of(p));
        when(repositorio.guardar(any())).thenAnswer(i -> i.getArgument(0));

        service.cambiarEstado(p.getId(), EN_ALMACEN, "operador1");

        verify(historial).guardar(argThat(h ->
            h.getEstado() == EN_ALMACEN && "operador1".equals(h.getUsuarioResponsable())));
    }

    @Test
    void cambiarEstado_transicionInvalida_lanzaException() {
        PaqueteEntity p = paqueteConEstado(REGISTRADO);
        when(repositorio.buscarPorId(any())).thenReturn(Optional.of(p));

        InvalidStateTransitionException ex = assertThrows(
            InvalidStateTransitionException.class,
            () -> service.cambiarEstado(p.getId(), ENTREGADO, "operador1"));

        assertTrue(ex.getMessage().contains("REGISTRADO"));
        assertTrue(ex.getMessage().contains("ENTREGADO"));
    }

    @Test
    void buscarPorSucursal_sinResultados_retornaListaVacia() {
        when(repositorio.buscarPorSucursalYEstado("MOQUEGUA", null))
            .thenReturn(List.of());
        assertTrue(service.buscarPorSucursalYEstado("MOQUEGUA", null).isEmpty());
    }
}
```

---

## Eureka: configuración de heartbeat (al menos 1 cliente)

En `servicio-clientes.yaml` (Config Server repo):
```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
```

---

## Circuit Breaker con Resilience4j (Sección 15)

En `servicio-clientes`, proteger la llamada a RENIEC:
```java
@CircuitBreaker(name = "reniec", fallbackMethod = "fallbackReniec")
public String obtenerNombreCompleto(String dni) {
    return feignClient.consultarDni("Bearer " + token, dni).getNombreCompleto();
}

public String fallbackReniec(String dni, Exception ex) {
    throw new ExternalServiceException("RENIEC no disponible (circuit breaker abierto)");
}
```

En `servicio-paquetes`, proteger la llamada a `servicio-clientes`:
```java
@CircuitBreaker(name = "servicio-clientes", fallbackMethod = "fallbackCliente")
public ClienteDto obtenerCliente(UUID id) {
    return clienteFeign.buscarCliente(id).getData();
}
```

---

## Campos de auditoría automática (RNF-04)

En al menos 2 entidades, usar Spring Data Auditing:

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {}

// En la entidad:
@EntityListeners(AuditingEntityListener.class)
public class ClienteEntity {
    @CreatedDate
    @Column(updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    LocalDateTime updatedAt;
}
```

---

## Arranque del ecosistema completo con Docker

### Primer arranque (setup inicial)

```bash
# 1. Copiar variables de entorno y completarlas
cp .env.example .env
# editar .env con las credenciales reales

# 2. Levantar solo Vault primero para cargarlo
docker compose up vault -d
sleep 5

# 3. Cargar el JWT secret en Vault
docker exec vault vault kv put secret/rapidocourier \
  jwt.secret="clave-super-secreta-minimo-32-caracteres"

# 4. Levantar todo el ecosistema
docker compose up --build -d

# 5. Verificar que todos los servicios están UP
docker compose ps
```

### Comandos cotidianos

```bash
# Levantar todo (sin rebuild)
docker compose up -d

# Levantar y forzar rebuild de imágenes
docker compose up --build -d

# Ver logs de un servicio específico
docker compose logs -f servicio-clientes

# Ver logs de todos los servicios
docker compose logs -f

# Detener todo (conserva volúmenes y datos)
docker compose down

# Detener y eliminar volúmenes (borra las BDs)
docker compose down -v

# Reiniciar un solo servicio tras cambios en el código
docker compose up --build -d servicio-paquetes

# Refresh de configuración (Config Server)
curl -X POST http://localhost:8080/api/v1/clientes/actuator/refresh
```

### Verificaciones post-arranque

```bash
# Dashboard de Eureka (abrir en el navegador)
open http://localhost:8761

# Health del Gateway
curl http://localhost:8080/actuator/health

# Login de prueba
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq .
```

---

## Eureka: configuración de heartbeat (al menos 1 cliente)

Todos expuestos a través del Gateway (`localhost:8080`). Requieren `Authorization: Bearer <JWT>` salvo login/register.

| Método | Path | Descripción | Roles |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Login, retorna JWT | Público |
| POST | `/api/v1/auth/register` | Registro de usuario | Público |
| POST | `/api/v1/clientes` | Registrar cliente (consulta RENIEC) | ADMIN, OPERADOR |
| GET | `/api/v1/clientes` | Listar clientes | ADMIN, OPERADOR |
| GET | `/api/v1/clientes/{id}` | Obtener cliente por ID | ADMIN, OPERADOR |
| DELETE | `/api/v1/clientes/{id}` | Eliminar cliente | ADMIN |
| POST | `/api/v1/paquetes` | Registrar paquete (calcula tarifa) | ADMIN, OPERADOR |
| GET | `/api/v1/paquetes/{id}` | Obtener paquete con datos de clientes | Autenticado |
| PUT | `/api/v1/paquetes/{id}` | Actualizar datos del paquete | ADMIN, OPERADOR |
| DELETE | `/api/v1/paquetes/{id}` | Eliminar paquete | ADMIN |
| PATCH | `/api/v1/paquetes/{id}/estado` | Cambiar estado (valida transición) | ADMIN, OPERADOR |
| GET | `/api/v1/paquetes/{id}/historial` | Historial de estados | Autenticado |
| GET | `/api/v1/paquetes?busqueda=texto` | Búsqueda por código o nombre de cliente | Autenticado |
| GET | `/api/v1/paquetes?sucursal=Lima&estado=EN_TRANSITO` | Filtro por sucursal y estado | Autenticado |
| POST | `/api/v1/categorias` | Crear categoría | ADMIN |
| POST | `/api/v1/paquetes/{id}/categorias/{catId}` | Asignar categoría a paquete | ADMIN, OPERADOR |

---

## Restricciones absolutas

- Nunca hardcodear el token de RENIEC, la clave JWT ni contraseñas de BD en archivos del repositorio.
- Nunca hardcodear credenciales en `docker-compose.yml`; siempre usar variables del archivo `.env`.
- El archivo `.env` va en `.gitignore`. Solo se commitea `.env.example`.
- Nunca compartir entidades JPA entre microservicios. Usar DTOs o IDs.
- Nunca exponer los puertos de los microservicios de negocio al host; solo el Gateway en `8080`.
- Nunca usar `@Autowired` en campo; siempre inyección por constructor.
- Nunca usar `application.properties`; siempre `application.yaml`.
- El `nombreCompleto` del cliente nunca se recibe del request; siempre se obtiene de RENIEC.
- Toda respuesta HTTP (éxito y error) debe usar `ApiResponse<T>`.
- Toda transición de estado inválida debe retornar 409 con un mensaje que incluya el estado actual y el estado intentado.
- Cada microservicio debe tener su propio `Dockerfile` con build multietapa (build + runtime).
- Los servicios Spring deben leer su configuración de conexión (BD, Eureka, Vault) desde variables de entorno con fallback a `localhost` para desarrollo sin Docker.
