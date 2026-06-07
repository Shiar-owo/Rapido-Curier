# CASOS DE PRUEBA — API Examples

> Todos los `curl` van dirigidos al **API Gateway** (`localhost:8080`).
> Reemplaza `TOKEN` por el JWT obtenido en el caso de login.
> Reemplaza `{id}`, `{categoriaId}`, etc. por UUIDs reales.

---

## Autenticación

### Caso 1 — Registro de usuario (OPERADOR)
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Carlos Quispe","email":"carlos@rapidocourier.pe","password":"Segura123!","rol":"OPERADOR"}' | jq .
```
**Esperado:** `201 Created` — `success: true`, JWT en `data`.

### Caso 2 — Login exitoso
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos@rapidocourier.pe","password":"Segura123!"}' | jq .
```
**Esperado:** `200 OK` — `success: true`, JWT válido en `data`.

### Caso 3 — Login con credenciales incorrectas
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos@rapidocourier.pe","password":"incorrecta"}' | jq .
```
**Esperado:** `401 Unauthorized` — `success: false`.

### Caso 3a — Registro con campos faltantes
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com"}' | jq .
```
**Esperado:** `400 Bad Request` — `data` con errores de validación (nombre, password, rol requeridos).

### Caso 3b — Login con campos faltantes
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```
**Esperado:** `400 Bad Request` — `data` con errores de validación (email, password requeridos).

---

## Clientes (RF-01)

### Caso 4 — Registro de cliente con consulta RENIEC
```bash
curl -s -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"dni":"10001088","email":"ana.flores@correo.com"}' | jq .
```
**Esperado:** `201 Created` — nombre completo obtenido de RENIEC visible en `data`.

### Caso 5 — Email duplicado en registro de cliente
```bash
curl -s -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"dni":"40826421","email":"ana.flores@correo.com"}' | jq .
```
**Esperado:** `409 Conflict` — `success: false`, mensaje indica email ya registrado.

### Caso 6 — Datos inválidos en registro de cliente
```bash
curl -s -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"dni":"123","email":"correo-invalido"}' | jq .
```
**Esperado:** `400 Bad Request` — `success: false`, `data` con mapa de errores agrupado por campo.

### Caso 6a — Listar todos los clientes
```bash
curl -s -X GET http://localhost:8080/api/v1/clientes \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — lista de clientes en `data`.

### Caso 6b — Obtener cliente por ID
```bash
curl -s -X GET http://localhost:8080/api/v1/clientes/{REMOTE_CLIENT_ID} \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — datos del cliente en `data`.

### Caso 6c — Buscar clientes por nombre
```bash
curl -s -X GET "http://localhost:8080/api/v1/clientes/buscar?nombre=KEIKO" \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — lista de clientes cuyo nombre contenga "KEIKO".

### Caso 6d — Obtener cliente inexistente
```bash
curl -s -X GET http://localhost:8080/api/v1/clientes/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `404 Not Found` — `success: false`.

### Caso 6e — CLIENTE no puede listar clientes
```bash
curl -s -X GET http://localhost:8080/api/v1/clientes \
  -H "Authorization: Bearer TOKEN_CLIENTE" | jq .
```
**Esperado:** `403 Forbidden`.

### Caso 6f — Eliminar cliente (solo ADMIN)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/clientes/{CLIENT_ID} \
  -H "Authorization: Bearer TOKEN_ADMIN" | jq .
```
**Esperado:** `204 No Content`.

### Caso 6g — OPERADOR no puede eliminar cliente
```bash
curl -s -X DELETE http://localhost:8080/api/v1/clientes/{CLIENT_ID} \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `403 Forbidden`.

### Caso 6h — Eliminar cliente inexistente
```bash
curl -s -X DELETE http://localhost:8080/api/v1/clientes/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer TOKEN_ADMIN" | jq .
```
**Esperado:** `404 Not Found`.

---

## Paquetes (RF-02, RF-03, RF-04)

### Caso 7 — Registro de paquete con tarifa calculada
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "remitenteId":"REMOTE_CLIENT_ID",
    "destinatarioId":"DEST_CLIENT_ID",
    "pesoKg":1.5,
    "valorDeclarado":200.00,
    "sucursalOrigen":"LIMA",
    "sucursalDestino":"AREQUIPA",
    "categoriaIds":["CATEGORY_ID"]
  }' | jq .
```
**Esperado:** `201 Created` — código de rastreo generado y tarifa calculada visibles en `data`.

### Caso 8 — Datos inválidos en registro de paquete
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"pesoKg":-1,"valorDeclarado":0,"sucursalOrigen":"","sucursalDestino":""}' | jq .
```
**Esperado:** `400 Bad Request` — mapa de errores por campo en `data`.

### Caso 9 — Consulta de paquete por ID existente
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/{id} \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — datos del paquete en `data`.

### Caso 10 — Consulta de paquete por ID inexistente
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `404 Not Found` — `success: false`.

### Caso 11 — Actualización de estado: transición válida (RF-04)
```bash
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"operador1"}' | jq .
```
**Esperado:** `200 OK` — `success: true`.

### Caso 12 — Actualización de estado: transición inválida (RF-04)
```bash
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"ENTREGADO","usuarioResponsable":"operador1"}' | jq .
```
**Esperado:** `400 Bad Request` — mensaje descriptivo con los estados involucrados.

### Caso 12a — Cadena completa de estados: REGISTRADO → ENTREGADO
```bash
# REGISTRADO → EN_ALMACEN
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"op1"}'
# EN_ALMACEN → EN_TRANSITO
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"EN_TRANSITO","usuarioResponsable":"op1"}'
# EN_TRANSITO → EN_REPARTO
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"EN_REPARTO","usuarioResponsable":"op1"}'
# EN_REPARTO → ENTREGADO
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"ENTREGADO","usuarioResponsable":"op1"}'
```
**Esperado:** Todas devuelven `200 OK`.

### Caso 12b — Estado terminal: ENTREGADO no permite cambios
```bash
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"op1"}' | jq .
```
**Esperado:** `400 Bad Request` — ENTREGADO es estado terminal.

### Caso 12c — NO_ENTREGADO permite volver a EN_ALMACEN
```bash
# First: REGISTRADO → EN_ALMACEN → EN_TRANSITO → EN_REPARTO → NO_ENTREGADO
# Then: NO_ENTREGADO → EN_ALMACEN (valid re-entry)
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"op1"}' | jq .
```
**Esperado:** `200 OK` — transición permitida.

### Caso 12d — Cambiar estado de paquete inexistente
```bash
curl -s -X PUT http://localhost:8080/api/v1/paquetes/00000000-0000-0000-0000-000000000000/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"op1"}' | jq .
```
**Esperado:** `404 Not Found`.

---

## Historial de Estados (RF-05)

### Caso 13 — Consulta del historial de estados
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/{id}/historial \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — lista ordenada de cambios de estado con timestamp y usuario en `data`.

### Caso 13a — Historial de paquete inexistente
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/00000000-0000-0000-0000-000000000000/historial \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `404 Not Found`.

---

## Búsqueda y Filtros (RF-06, RF-07)

### Caso 14 — Búsqueda por texto parcial en código de rastreo (RF-07)
```bash
curl -s -X GET "http://localhost:8080/api/v1/paquetes/buscar?texto=RC2026" \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — lista de paquetes cuyo código de rastreo contenga "RC2026".

### Caso 14a — Búsqueda sin resultados
```bash
curl -s -X GET "http://localhost:8080/api/v1/paquetes/buscar?texto=ZZZZZ" \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — `data: []` (lista vacía).

### Caso 15 — Búsqueda por nombre de cliente (RF-07)
```bash
curl -s -X GET "http://localhost:8080/api/v1/paquetes/cliente?nombre=KEIKO" \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — paquetes cuyo remitente o destinatario contenga "KEIKO" en su nombre.

### Caso 16 — Filtro por sucursal y estado (RF-06)
```bash
curl -s -X GET "http://localhost:8080/api/v1/paquetes/sucursal/LIMA/estado/EN_ALMACEN" \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — paquetes con origen o destino en LIMA y estado EN_ALMACEN.

---

## Categorías (RF-09)

### Caso 17 — Crear categoría (ADMIN)
```bash
curl -s -X POST http://localhost:8080/api/v1/categorias \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_ADMIN" \
  -d '{"nombre":"FRAGIL","descripcion":"Artículo frágil"}' | jq .
```
**Esperado:** `201 Created` — categoría creada en `data`.

### Caso 17a — OPERADOR no puede crear categoría
```bash
curl -s -X POST http://localhost:8080/api/v1/categorias \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_OPERADOR" \
  -d '{"nombre":"PRUEBA","descripcion":"Test"}' | jq .
```
**Esperado:** `403 Forbidden`.

### Caso 17b — Nombre de categoría duplicado
```bash
curl -s -X POST http://localhost:8080/api/v1/categorias \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_ADMIN" \
  -d '{"nombre":"FRAGIL","descripcion":"Duplicado"}' | jq .
```
**Esperado:** `409 Conflict` — "La categoría 'FRAGIL' ya existe".

### Caso 18 — Asignar categoría a un paquete
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes/{id}/categorias/{categoriaId} \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `204 No Content` — categoría asignada al paquete.

### Caso 18a — Asignar categoría a paquete inexistente
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes/00000000-0000-0000-0000-000000000000/categorias/{categoriaId} \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `404 Not Found`.

### Caso 18b — Asignar categoría inexistente a paquete
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes/{id}/categorias/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `404 Not Found`.

### Caso 18c — Asignar categoría ya asignada (duplicado)
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes/{id}/categorias/{categoriaId} \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `409 Conflict` — categoría ya está asignada al paquete.

### Caso 19 — Listar categorías
```bash
curl -s -X GET http://localhost:8080/api/v1/categorias \
  -H "Authorization: Bearer TOKEN" | jq .
```
**Esperado:** `200 OK` — lista de categorías en `data`.

---

## CLIENTE — Paquetes propios (RF-10)

### Caso 19a — CLIENTE ve sus paquetes
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/mis-paquetes \
  -H "Authorization: Bearer TOKEN_CLIENTE" | jq .
```
**Esperado:** `200 OK` — lista de paquetes donde el CLIENTE es remitente o destinatario.

### Caso 19b — CLIENTE ve historial de su paquete
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/mis-paquetes/{id}/historial \
  -H "Authorization: Bearer TOKEN_CLIENTE" | jq .
```
**Esperado:** `200 OK` — historial de estados del paquete.

### Caso 19c — CLIENTE accede a paquete ajeno (404)
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/mis-paquetes/{OTHER_PKG_ID}/historial \
  -H "Authorization: Bearer TOKEN_CLIENTE" | jq .
```
**Esperado:** `404 Not Found` — paquete no pertenece al CLIENTE.

### Caso 19d — OPERADOR no puede acceder a mis-paquetes
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/mis-paquetes \
  -H "Authorization: Bearer TOKEN_OPERADOR" | jq .
```
**Esperado:** `403 Forbidden`.

---

## Operaciones de escritura y borrado

### Caso 20 — Actualización de paquete
```bash
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"pesoKg":2.0,"valorDeclarado":300.00,"sucursalOrigen":"LIMA","sucursalDestino":"CUSCO"}' | jq .
```
**Esperado:** `200 OK` — datos actualizados y tarifa recalculada en `data`.

### Caso 21 — Eliminación de paquete (solo ADMIN)
```bash
curl -s -X DELETE http://localhost:8080/api/v1/paquetes/{id} \
  -H "Authorization: Bearer TOKEN_ADMIN" | jq .
```
**Esperado:** `204 No Content`.

### Caso 21a — OPERADOR no puede eliminar paquete
```bash
curl -s -X DELETE http://localhost:8080/api/v1/paquetes/{id} \
  -H "Authorization: Bearer TOKEN_OPERADOR" | jq .
```
**Esperado:** `403 Forbidden`.

### Caso 22 — Eliminación de recurso inexistente
```bash
curl -s -X DELETE http://localhost:8080/api/v1/paquetes/00000000-0000-0000-0000-000000000000 \
  -H "Authorization: Bearer TOKEN_ADMIN" | jq .
```
**Esperado:** `404 Not Found` — `success: false`.

---

## Seguridad (RF-08)

### Caso 23 — Acceso sin token a endpoint protegido
```bash
curl -s -X GET http://localhost:8080/api/v1/paquetes/buscar?texto=test | jq .
```
**Esperado:** `401 Unauthorized`.

### Caso 24 — Acceso con rol insuficiente: CLIENTE intenta eliminar
```bash
curl -s -X DELETE http://localhost:8080/api/v1/paquetes/{id} \
  -H "Authorization: Bearer TOKEN_CLIENTE" | jq .
```
**Esperado:** `403 Forbidden`.

### Caso 24a — CLIENTE no puede registrar paquete
```bash
curl -s -X POST http://localhost:8080/api/v1/paquetes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_CLIENTE" \
  -d '{
    "remitenteId":"REMOTE_CLIENT_ID",
    "destinatarioId":"DEST_CLIENT_ID",
    "pesoKg":1.5,
    "valorDeclarado":200.00,
    "sucursalOrigen":"LIMA",
    "sucursalDestino":"AREQUIPA",
    "categoriaIds":["CATEGORY_ID"]
  }' | jq .
```
**Esperado:** `403 Forbidden`.

### Caso 24b — CLIENTE no puede cambiar estado
```bash
curl -s -X PUT http://localhost:8080/api/v1/paquetes/{id}/estado \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_CLIENTE" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"cli1"}' | jq .
```
**Esperado:** `403 Forbidden`.

### Caso 24c — CLIENTE no puede buscar por código de rastreo
```bash
curl -s -X GET "http://localhost:8080/api/v1/paquetes/buscar?texto=RC" \
  -H "Authorization: Bearer TOKEN_CLIENTE" | jq .
```
**Esperado:** `403 Forbidden`.
