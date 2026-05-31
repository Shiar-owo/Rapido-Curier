# Postman Testing Examples — RapidoCurier API Gateway

Pega cada `curl` directamente en Postman → **Import → Raw text**. Postman lo parsea automáticamente.

---

## 1. Autenticación (públicas — sin token)

### 1.1 Registrar usuario

```bash
curl --location 'http://localhost:8080/api/v1/auth/register' \
--header 'Content-Type: application/json' \
--data-raw '{
  "nombre": "Admin",
  "email": "admin@mail.com",
  "password": "123456",
  "rol": "ADMIN"
}'
```

**Response** `201 CREATED`
```json
{
  "success": true,
  "message": "Recurso creado",
  "data": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### 1.2 Iniciar sesión

```bash
curl --location 'http://localhost:8080/api/v1/auth/login' \
--header 'Content-Type: application/json' \
--data-raw '{
  "email": "admin@mail.com",
  "password": "123456"
}'
```

**Response** `200 OK`
```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> Guarda el token (`data`) de la respuesta. Lo usarás como `Bearer <token>` en las rutas protegidas.

---

## 2. Health / Actuator (públicas — sin token)

```bash
curl --location 'http://localhost:8080/actuator/health'
```

**Response** `200 OK`
```json
{
  "status": "UP"
}
```

---

## 3. Clientes (protegidas — requieren JWT)

Reemplaza `<TOKEN>` con el JWT obtenido en login/register.

### 3.1 Sin token — error esperado

```bash
curl --location 'http://localhost:8080/api/v1/clientes'
```

**Response** `401 UNAUTHORIZED`
```json
{
  "success": false,
  "message": "Token JWT requerido",
  "data": null
}
```

---

### 3.2 Token inválido — error esperado

```bash
curl --location 'http://localhost:8080/api/v1/clientes' \
--header 'Authorization: Bearer token-malo'
```

**Response** `401 UNAUTHORIZED`
```json
{
  "success": false,
  "message": "JWT token inválido o expirado",
  "data": null
}
```

---

### 3.3 Sin Bearer — error esperado

```bash
curl --location 'http://localhost:8080/api/v1/clientes' \
--header 'Authorization: Basic somecreds'
```

**Response** `401 UNAUTHORIZED`
```json
{
  "success": false,
  "message": "Token JWT requerido",
  "data": null
}
```

---

### 3.4 Listar clientes — token válido

```bash
curl --location 'http://localhost:8080/api/v1/clientes' \
--header 'Authorization: Bearer <TOKEN>'
```

**Response** `200 OK` (sin clientes)
```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": []
}
```

---

### 3.5 Crear cliente — token válido

```bash
curl --location 'http://localhost:8080/api/v1/clientes' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <TOKEN>' \
--data-raw '{
  "dni": "12345678",
  "email": "cliente@mail.com"
}'
```

**Response** `201 CREATED`
```json
{
  "success": true,
  "message": "Recurso creado",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "dni": "12345678",
    "email": "cliente@mail.com",
    "nombre": null,
    "apellidoPaterno": null,
    "apellidoMaterno": null,
    "createdAt": "2026-05-27T20:00:00.000Z",
    "updatedAt": null
  }
}
```

---

### 3.6 Obtener cliente por ID — token válido

```bash
curl --location 'http://localhost:8080/api/v1/clientes/550e8400-e29b-41d4-a716-446655440000' \
--header 'Authorization: Bearer <TOKEN>'
```

**Response** `200 OK`
```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "dni": "12345678",
    "email": "cliente@mail.com",
    "nombre": "Juan",
    "apellidoPaterno": "Pérez",
    "apellidoMaterno": "López",
    "createdAt": "2026-05-27T20:00:00.000Z",
    "updatedAt": null
  }
}
```

**Response** — no encontrado `404 NOT FOUND`
```json
{
  "success": false,
  "message": "Cliente no encontrado con ID: 00000000-0000-0000-0000-000000000000",
  "data": null
}
```

---

## Resumen de códigos de respuesta

| Código | Significado |
|--------|-------------|
| `200 OK` | Éxito |
| `201 CREATED` | Recurso creado |
| `401 UNAUTHORIZED` | Token faltante, inválido o expirado |
| `404 NOT FOUND` | Recurso no encontrado |
| `409 CONFLICT` | Conflicto (ej. email duplicado) |
| `503 SERVICE UNAVAILABLE` | Servicio externo no disponible (Reniec) |
