# RapidoCourier - Microservicios

Plataforma de mensajería y paquetería basada en microservicios con Spring Boot, arquitectura hexagonal y despliegue Docker.

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Cloud | Spring Cloud 2025.1.1 |
| BD | PostgreSQL 16 |
| Migraciones | Flyway |
| Mapeo | MapStruct |
| Seguridad | JWT (jjwt 0.12.5), BCrypt |
| Documentación | OpenAPI 3.x |
| Testing | JUnit 5, Mockito, Testcontainers |
| Infraestructura | Docker, Eureka, Config Server, Vault |

## Arquitectura General

```
                         ┌─────────────┐
                         │   Vault     │ (secretos)
                         │   :8200     │
                         └──────┬──────┘
                                │
┌──────────────┐     ┌──────────▼─────────┐     ┌────────────────┐
│   Git Repo   │────▶│   Config Server    │────▶│  Eureka Server │
│   (configs)  │     │      :8888         │     │    :8761       │
└──────────────┘     └──────────┬──────────┘     └────────────────┘
                                │
         ┌──────────────────────┼──────────────────────┐
         │                      │                      │
         ▼                      ▼                      ▼
┌──────────────────┐  ┌──────────────────┐  ┌─────────────────────┐
│   auth-service   │  │ clients-service  │  │   paquetes-service  │
│     :8081        │  │     :8082        │  │     :8083           │
│  PostgreSQL      │  │  PostgreSQL      │  │  PostgreSQL         │
│  rapidocourier_  │  │ rapidocourier_   │  │ rapidocourier_      │
│  auth            │  │ clientes         │  │ paquetes            │
└──────────────────┘  └────────┬─────────┘  └─────────────────────┘
                               │
                               ▼
                      ┌──────────────────┐
                      │   RENIEC API     │
                      │ (Feign + CB)     │
                      └──────────────────┘
```

## Estructura del Proyecto

```
RapidoCurier/
├── docker-compose.yml           # Orquestación completa
├── .env                         # Variables de entorno
├── config-server/               # Config Server (Git-backed)
├── eureka-server/               # Service Discovery
├── api-gateway/                 # API Gateway (en desarrollo)
├── auth-service/                # Autenticación y usuarios
├── clients-service/             # Gestión de clientes
├── vault/                       # Configuración de Vault
│   ├── config/dev-server.hcl
│   └── scripts/init-vault.sh
└── .github/workflows/           # CI/CD pipelines
    ├── auth-service-ci-cd.yml
    └── clients-service-ci-cd.yml
```

---

## 1. config-server

Servidor de configuración centralizada que lee desde un repositorio Git.

| Propiedad | Valor |
|-----------|-------|
| Puerto | `8888` |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |
| Backend | Git (`https://github.com/Shiar-owo/Rapido-Curier-Configs`) |

### Endpoints
- `GET /{application}/{profile}` — Obtener configuración
- `GET /actuator/health` — Health check

### Configuración destacada
```yaml
# config-server/src/main/resources/application.yaml
spring.cloud.config.server.git.uri: ${CONFIG_GIT_URI}
spring.cloud.config.server.git.default-label: main
```

### Seguridad
Acceso público (permitAll).

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache git
COPY target/*.jar app.jar
EXPOSE 8888
```

---

## 2. eureka-server

Servicio de descubrimiento (Service Discovery).

| Propiedad | Valor |
|-----------|-------|
| Puerto | `8761` |
| Spring Boot | 3.5.14 |
| Spring Cloud | 2025.0.2 |

### Endpoints
- `GET /` — Dashboard Eureka
- `GET /eureka/apps` — Listar instancias registradas
- `GET /actuator/health` — Health check

### Configuración destacada
```yaml
# eureka-server/src/main/resources/application.yaml
eureka.client.register-with-eureka: false
eureka.client.fetch-registry: false
eureka.server.enable-self-preservation: false
```

### Seguridad
Recursos estáticos y Eureka dashboard públicos; demás endpoints autenticados.

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY target/*.jar app.jar
EXPOSE 8761
```

---

## 3. api-gateway

API Gateway (en desarrollo inicial).

| Propiedad | Valor |
|-----------|-------|
| Puerto host | `8080` |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |

### Estado actual
- Esqueleto del proyecto creado
- Sin rutas configuradas
- Sin filtro JWT implementado
- Sin Dockerfile

### Pendiente
- [ ] Configurar rutas hacia auth-service, clients-service, paquetes-service
- [ ] Implementar filtro JWT centralizado
- [ ] Propagar cabeceras `X-User-Id` y `X-User-Roles`

---

## 4. auth-service

Servicio de autenticación y gestión de usuarios.

| Propiedad | Valor |
|-----------|-------|
| Puerto | `8081` |
| Base de datos | `rapidocourier_auth` |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |
| Arquitectura | Hexagonal (Ports & Adapters) |

### Estructura de paquetes

```
auth-service/
└── src/main/java/com/rapidocourier/authservice/
    ├── domain/
    │   ├── model/
    │   │   ├── Usuario.java
    │   │   └── RolNombre.java          # ADMIN, OPERADOR, CLIENTE
    │   ├── port/
    │   │   ├── in/
    │   │   │   ├── LoginUseCase.java
    │   │   │   └── RegisterUseCase.java
    │   │   └── out/
    │   │       ├── UsuarioRepositoryPort.java
    │   │       └── JwtPort.java
    │   └── exception/
    │       ├── ConflictException.java
    │       └── CredencialesInvalidasException.java
    ├── application/
    │   └── service/
    │       └── AuthService.java
    └── infrastructure/
        ├── adapter/
        │   ├── in/rest/
        │   │   ├── controller/AuthController.java
        │   │   └── dto/request/
        │   │       ├── LoginRequest.java
        │   │       └── RegisterRequest.java
        │   └── out/
        │       ├── jwt/JwtAdapter.java
        │       └── persistence/
        │           ├── entity/ (UsuarioEntity, RolEntity)
        │           ├── repository/ (UsuarioJpaRepository, RolJpaRepository)
        │           ├── mapper/UsuarioMapper.java
        │           └── UsuarioRepositoryAdapter.java
        ├── common/ApiResponse.java
        └── config/
            ├── SecurityConfig.java
            ├── GlobalExceptionHandler.java
            └── DataInitializer.java
```

### Endpoints

| Método | Ruta | Auth | Descripción |
|--------|------|------|-------------|
| `POST` | `/api/v1/auth/register` | Público | Registrar nuevo usuario |
| `POST` | `/api/v1/auth/login` | Público | Iniciar sesión, devuelve JWT |

### DTOs

**RegisterRequest:**
```json
{
  "nombre": "Juan Pérez",
  "email": "juan@email.com",
  "password": "secreta123",
  "rol": "CLIENTE"
}
```

**LoginRequest:**
```json
{
  "email": "juan@email.com",
  "password": "secreta123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": "<JWT_TOKEN>"
}
```

### JWT

| Claim | Contenido |
|-------|-----------|
| `sub` | UUID del usuario |
| `nombre` | Nombre completo |
| `email` | Correo electrónico |
| `roles` | Roles separados por coma (ej: `"ADMIN,OPERADOR"`) |
| `iat` | Fecha de emisión |
| `exp` | Fecha de expiración (24h) |

- **Algoritmo:** HS256
- **Secreto:** `${JWT_SECRET}` (de Vault o Config Server)
- **Expiración:** 86400000 ms (24h)

### Roles

| Rol | Permisos |
|-----|----------|
| `ADMIN` | Acceso total a todos los endpoints |
| `OPERADOR` | Gestión de clientes y paquetes (sin eliminar) |
| `CLIENTE` | Solo consulta de sus propios recursos |

### Seguridad
- Endpoints `/api/v1/auth/**` y `/actuator/**` públicos
- Demás endpoints requieren autenticación
- Contraseñas con BCrypt
- **Sin filtro JWT local** (depende del API Gateway)

### Base de Datos

```sql
-- Tabla roles
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(20) NOT NULL UNIQUE
);
-- Valores: ADMIN, OPERADOR, CLIENTE

-- Tabla usuarios
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

-- Tabla usuario_roles (N:M)
CREATE TABLE usuario_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    rol_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (usuario_id, rol_id)
);
```

### Seed Data (DataInitializer)
Al iniciar, crea 3 usuarios por defecto:
| Email | Password | Rol |
|-------|----------|-----|
| `admin@rapidocourier.com` | `admin123` | ADMIN |
| `operador@rapidocourier.com` | `operador123` | OPERADOR |
| `cliente@rapidocourier.com` | `cliente123` | CLIENTE |

### Pruebas
- `AuthServiceTest.java` — 5 tests unitarios (Mockito)
- `JwtAdapterTest.java` — 3 tests unitarios
- `AuthControllerTest.java` — 6 tests de integración web
- `AuthServiceIntegrationTest.java` — 4 tests con Testcontainers

---

## 5. clients-service

Servicio de gestión de clientes con consulta a RENIEC.

| Propiedad | Valor |
|-----------|-------|
| Puerto | `8082` |
| Base de datos | `rapidocourier_clientes` |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.1 |
| Arquitectura | Hexagonal (Ports & Adapters) |
| Circuit Breaker | Resilience4j (`reniec`) |
| JWT | Validación local (filtro propio) |

### Estructura de paquetes

```
clients-service/
└── src/main/java/com/rapidocourier/clientsservice/
    ├── domain/
    │   ├── model/
    │   │   ├── Cliente.java
    │   │   └── ReniecDataClient.java
    │   ├── port/
    │   │   ├── in/
    │   │   │   ├── RegistrarClienteUseCase.java
    │   │   │   └── ConsultarClienteUseCase.java
    │   │   └── out/
    │   │       ├── ClienteRepositoryPort.java
    │   │       └── ReniecPort.java
    │   └── exception/
    │       ├── ConflictException.java
    │       ├── ExternalServiceException.java
    │       └── ResourceNotFoundException.java
    ├── application/
    │   └── service/
    │       └── ClienteService.java
    └── infrastructure/
        ├── adapter/
        │   ├── in/rest/
        │   │   ├── controller/ClienteController.java
        │   │   └── dto/
        │   │       ├── request/ClienteRequest.java
        │   │       └── response/ClienteResponse.java
        │   └── out/
        │       ├── persistence/
        │       │   ├── entity/ClienteEntity.java
        │       │   ├── repository/ClienteJpaRepository.java
        │       │   ├── mapper/ClienteMapper.java
        │       │   └── ClienteRepositoryAdapter.java
        │       └── reniec/
        │           ├── client/ReniecFeignClient.java
        │           └── adapter/ReniecAdapter.java
        ├── common/ApiResponse.java
        └── config/
            ├── SecurityConfig.java
            ├── JwtService.java
            ├── JwtAuthenticationFilter.java
            ├── JpaConfig.java
            └── GlobalExceptionHandler.java
```

### Endpoints

| Método | Ruta | Roles | Descripción |
|--------|------|-------|-------------|
| `POST` | `/api/v1/clientes` | ADMIN, OPERADOR | Registrar cliente (consulta RENIEC) |
| `GET` | `/api/v1/clientes` | ADMIN, OPERADOR | Listar todos los clientes |
| `GET` | `/api/v1/clientes/{id}` | ADMIN, OPERADOR | Obtener cliente por ID |
| `DELETE` | `/api/v1/clientes/{id}` | ADMIN | Eliminar cliente |

### DTOs

**ClienteRequest:**
```json
{
  "dni": "46027897",
  "email": "cliente@email.com"
}
```

**ClienteResponse:**
```json
{
  "id": "a1b2c3d4-...",
  "dni": "46027897",
  "nombre": "ROXANA KARINA",
  "apellidoPaterno": "DELGADO",
  "apellidoMaterno": "HUAMANI",
  "email": "cliente@email.com",
  "createdAt": "2026-05-24T15:30:00-05:00",
  "updatedAt": "2026-05-24T15:30:00-05:00"
}
```

### Integración RENIEC

**API externa:** `https://api.decolecta.com/v1/reniec/dni`

**Request:**
```
GET https://api.decolecta.com/v1/reniec/dni?numero=46027897
Headers:
  Authorization: Bearer <token>
  Content-Type: application/json
```

**Response:**
```json
{
  "first_name": "ROXANA KARINA",
  "first_last_name": "DELGADO",
  "second_last_name": "HUAMANI",
  "full_name": "DELGADO HUAMANI ROXANA KARINA",
  "document_number": "46027897"
}
```

**Circuit Breaker (`reniec`):**
| Parámetro | Valor |
|-----------|-------|
| sliding-window-size | 5 |
| failure-rate-threshold | 50% |
| wait-duration-in-open-state | 10s |
| permitted-number-of-calls-in-half-open-state | 3 |
| slow-call-duration-threshold | 2s |
| slow-call-rate-threshold | 50% |

### Seguridad
- **JWT validation local** (no depende del Gateway)
- Filtro `JwtAuthenticationFilter` extrae token Bearer, valida firma HS256 con mismo secreto que `auth-service`
- Extrae `roles` del JWT y los convierte a `GrantedAuthority` (prefijo `ROLE_`)
- `@EnableMethodSecurity` para `@PreAuthorize` en los endpoints

### Base de Datos

```sql
CREATE TABLE clientes (
    id UUID PRIMARY KEY,
    dni VARCHAR(8) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(100) NOT NULL,
    apellido_materno VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_clientes_dni ON clientes(dni);
CREATE INDEX idx_clientes_email ON clientes(email);
```

### JPA Auditing
- `@CreatedDate` y `@LastModifiedDate` en `createdAt`/`updatedAt`
- `DateTimeProvider` personalizado que retorna `OffsetDateTime` (no `LocalDateTime`)

### Pruebas (44 tests)

| Test | Tipo | Tests |
|------|------|-------|
| `ClienteServiceTest` | Unitario (Mockito) | 5 |
| `ClienteMapperTest` | Unitario | 3 |
| `ReniecAdapterTest` | Unitario (Mockito) | 3 |
| `JwtServiceTest` | Unitario | 3 |
| `JwtAuthenticationFilterTest` | Unitario (Mockito) | 7 |
| `ClienteControllerTest` | Integración web (WebMvcTest) | 11 |
| `GlobalExceptionHandlerTest` | Integración web (WebMvcTest) | 6 |
| `ClienteRepositoryAdapterTest` | Integración (Testcontainers) | 5 |
| `ClientsServiceApplicationTests` | Contexto (Testcontainers) | 1 |

---

## 6. paquetes-service (no implementado)

Servicio de gestión de paquetes, tracking y estados. Pendiente de desarrollo.

### Planificado
- Registro de paquetes con categorías
- Historial de estados con máquina de estados
- Cálculo de tarifas por ruta y peso
- Integración con clients-service vía Feign

---

## Infraestructura Compartida

### Docker Compose

```yaml
servicios:
  vault:        # hashcorp/vault:1.15 → :8200
  postgres-auth:      # postgres:16-alpine
  postgres-clientes:  # postgres:16-alpine
  postgres-paquetes:  # postgres:16-alpine
  config-server:      # build ./config-server → :8888
  eureka-server:      # build ./eureka-server → :8761
  auth-service:       # build ./auth-service → :8081
  clients-service:    # build ./clients-service → :8082
```

### Vault (Hashicorp Vault 1.15)

Secretos almacenados:

| Path | Secretos |
|------|----------|
| `secret/rapidocourier/auth` | `db.username`, `db.password`, `jwt.secret` |
| `secret/rapidocourier/clientes` | `db.username`, `db.password`, `reniec.token` |
| `secret/rapidocourier/paquetes` | `db.username`, `db.password` |

### Red
- **Nombre:** `rapidocourier-net` (bridge)
- Los servicios se comunican por nombre de contenedor (ej: `http://config-server:8888`)

### Configuración Remota (Git)
**Repositorio:** `https://github.com/Shiar-owo/Rapido-Curier-Configs`

Archivos por servicio:
- `application.yaml` — Configuración global
- `auth-service.yaml` — Puerto 8081, datasource, JWT, Eureka
- `clients-service.yaml` — Puerto 8082, datasource, RENIEC, CB, Eureka

### CI/CD (GitHub Actions)

| Workflow | Disparador | Acciones |
|----------|------------|----------|
| `auth-service-ci-cd.yml` | push/PR a main/develop/Auth-service + path `auth-service/**` | Test, Build, Docker image |
| `clients-service-ci-cd.yml` | push/PR a main/develop/clients-service + path `clients-service/**` | Test, Build, Docker image |

---

## Patrones y Convenciones

### Arquitectura Hexagonal
```
Controller (in) → UseCase (in port) → Service → RepositoryPort (out port) → RepositoryAdapter
                                                      → ReniecPort (out port) → ReniecAdapter (Feign)
```

### Reglas del proyecto
- `OffsetDateTime` siempre, nunca `LocalDateTime`
- `application.yaml` nunca `application.properties`
- UUID como primary key
- Inyección por constructor, nunca `@Autowired`
- MapStruct para mapeos, con `toDomain()` como default method
- Testcontainers para tests de integración, nunca H2
- Flyway para migraciones
- Resiliencia con Resilience4j
- Secretos en Vault

### Flujo de Autenticación
```
1. POST /api/v1/auth/login → JWT
2. Cliente envía JWT en header: Authorization: Bearer <token>
3. clients-service valida JWT localmente (JwtAuthenticationFilter)
4. Extrae roles del JWT → @PreAuthorize verifica permisos
```

---

## Cómo ejecutar

```bash
# Requisitos: Docker, Docker Compose, Java 17+, Maven

# 1. Construir todos los servicios
cd config-server && mvn clean package -DskipTests
cd ../eureka-server && mvn clean package -DskipTests
cd ../auth-service && mvn clean package -DskipTests
cd ../clients-service && mvn clean package -DskipTests

# 2. Iniciar toda la infraestructura
cd .. && docker compose up -d

# 3. Verificar estado
curl http://localhost:8761              # Eureka Dashboard
curl http://localhost:8888/actuator/health  # Config Server
curl http://localhost:8081/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Clients Service

# 4. Obtener token JWT
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@rapidocourier.com", "password": "admin123"}'

# 5. Crear cliente
curl -X POST http://localhost:8082/api/v1/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"dni": "46027897", "email": "cliente@email.com"}'
```

---

## Próximos Pasos

- [ ] Implementar `paquetes-service` (Fase 6 del plan)
- [ ] Configurar rutas y filtro JWT en `api-gateway`
- [ ] Integrar Spring Cloud Vault en los servicios
- [ ] Agregar monitoreo con Prometheus + Grafana
- [ ] Implementar pruebas de contrato con Pact o Spring Cloud Contract
