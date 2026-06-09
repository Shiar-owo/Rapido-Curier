#!/bin/bash
# ============================================
#  Script de Pruebas — examples.md
#  Ejecuta los 53 casos de prueba contra el
#  API Gateway (localhost:8080)
# ============================================

set -uo pipefail

BASE_URL="http://localhost:8080"
PASS=0
FAIL=0
TOTAL=0

# ── Colores ─────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ── Funciones auxiliares ────────────────────
check() {
  local expected=$1 actual=$2 case=$3
  TOTAL=$((TOTAL + 1))
  if [ "$expected" = "$actual" ]; then
    echo -e "  ${GREEN}PASS${NC} [$case] — HTTP $actual"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}FAIL${NC} [$case] — expected $expected, got $actual"
    FAIL=$((FAIL + 1))
  fi
}

section() {
  echo ""
  echo -e "${BOLD}${CYAN}── $1 ──${NC}"
}

# ── Verificar que los servicios estén arriba ─
echo -e "${BOLD}Verificando servicios...${NC}"
for i in $(seq 1 60); do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" -H "Content-Type: application/json" -d '{}' 2>/dev/null)
  if [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "401" ]; then
    echo -e "${GREEN}Gateway OK${NC}"
    break
  fi
  if [ $i -eq 60 ]; then
    echo -e "${RED}ERROR: Gateway no disponible en $BASE_URL${NC}"
    exit 1
  fi
  sleep 3
done

# Esperar a que los servicios backend estén listos (Eureka propagation)
for i in $(seq 1 30); do
  TEST_TOKEN="eyJhbGciOiJIUzI1NiJ9.test"
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/v1/clientes" -H "Authorization: Bearer $TEST_TOKEN" 2>/dev/null)
  if [ "$HTTP_CODE" != "502" ] && [ "$HTTP_CODE" != "503" ]; then
    echo -e "${GREEN}Backend services ready${NC}"
    break
  fi
  if [ $i -eq 30 ]; then
    echo -e "${RED}WARNING: Some backend services may not be ready${NC}"
  fi
  sleep 3
done

# ─────────────────────────────────────────────
section "AUTENTICACIÓN"
# ─────────────────────────────────────────────

# Caso 1 — Registro OPERADOR
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Carlos Quispe","email":"carlos@rapidocourier.pe","password":"Segura123!","rol":"OPERADOR"}')
check 201 "$CODE" "Caso 1 — Registro OPERADOR"

# Caso 2 — Login exitoso
RESP=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos@rapidocourier.pe","password":"Segura123!"}')
CODE=$(echo "$RESP" | python3 -c "import sys,json; print(200 if json.load(sys.stdin).get('success') else 0)" 2>/dev/null || echo "0")
check 200 "$CODE" "Caso 2 — Login exitoso"
OP_TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data'])" 2>/dev/null)

# Caso 3 — Login incorrecto
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos@rapidocourier.pe","password":"incorrecta"}')
check 401 "$CODE" "Caso 3 — Login incorrecto"

# Caso 3a — Registro campos faltantes
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com"}')
check 400 "$CODE" "Caso 3a — Registro campos faltantes"

# Caso 3b — Login campos faltantes
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{}')
check 400 "$CODE" "Caso 3b — Login campos faltantes"

# Crear ADMIN y CLIENTE para pruebas posteriores
curl -sf -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Admin Test","email":"admin@rapidocourier.pe","password":"Admin123!","rol":"ADMIN"}' >/dev/null 2>&1
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@rapidocourier.pe","password":"Admin123!"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data'])" 2>/dev/null)

# ─────────────────────────────────────────────
section "CLIENTES (RF-01)"
# ─────────────────────────────────────────────

# Caso 4 — Registro cliente con RENIEC (DNI 10001088)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/clientes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"dni":"10001088","email":"ana.flores@correo.com"}')
CODE=$(echo "$RESP" | tail -1)
check 201 "$CODE" "Caso 4 — Registro cliente RENIEC"
REMI_ID=$(echo "$RESP" | head -1 | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)

# Registrar CLIENTE con el mismo email que el remitente (para mis-paquetes)
curl -sf -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Cliente Test","email":"ana.flores@correo.com","password":"Cliente123!","rol":"CLIENTE"}' >/dev/null 2>&1
CLI_TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"ana.flores@correo.com","password":"Cliente123!"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data'])" 2>/dev/null)

# Caso 5 — Email duplicado
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/clientes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"dni":"40826421","email":"ana.flores@correo.com"}')
check 409 "$CODE" "Caso 5 — Email duplicado"

# Registrar segundo cliente (DNI 40826421)
RESP2=$(curl -s -X POST "$BASE_URL/api/v1/clientes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"dni":"40826421","email":"pedro.garcia@correo.com"}')
DEST_ID=$(echo "$RESP2" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)

# Registrar tercer cliente (DNI 16002918) — para Caso 19c (paquete ajeno)
RESP3=$(curl -s -X POST "$BASE_URL/api/v1/clientes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"dni":"16002918","email":"luis.mendoza@correo.com"}')
THIRD_ID=$(echo "$RESP3" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)

# Caso 6 — Datos inválidos
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/clientes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"dni":"123","email":"correo-invalido"}')
check 400 "$CODE" "Caso 6 — Datos inválidos"

# Caso 6a — Listar clientes
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/clientes" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 6a — Listar clientes"

# Caso 6b — Obtener por ID
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/clientes/$REMI_ID" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 6b — Obtener cliente por ID"

# Caso 6c — Buscar por nombre
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/clientes/buscar?nombre=KEIKO" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 6c — Buscar por nombre"

# Caso 6d — Cliente inexistente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/clientes/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $OP_TOKEN")
check 404 "$CODE" "Caso 6d — Cliente inexistente"

# Caso 6e — CLIENTE no puede listar
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/clientes" \
  -H "Authorization: Bearer $CLI_TOKEN")
check 403 "$CODE" "Caso 6e — CLIENTE no puede listar"

# Caso 6f — ADMIN eliminar cliente
DISP=$(curl -s -X POST "$BASE_URL/api/v1/clientes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"dni":"10001089","email":"disposable@test.com"}')
DISP_ID=$(echo "$DISP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/clientes/$DISP_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check 204 "$CODE" "Caso 6f — ADMIN eliminar cliente"

# Caso 6g — OPERADOR no puede eliminar
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/clientes/$REMI_ID" \
  -H "Authorization: Bearer $OP_TOKEN")
check 403 "$CODE" "Caso 6g — OPERADOR no puede eliminar"

# Caso 6h — Eliminar inexistente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/clientes/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check 404 "$CODE" "Caso 6h — Eliminar inexistente"

# ─────────────────────────────────────────────
section "CATEGORÍAS (RF-09)"
# ─────────────────────────────────────────────

# Caso 17 — Crear categoría (ADMIN)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/categorias" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"nombre":"FRAGIL","descripcion":"Articulo fragil"}')
CODE=$(echo "$RESP" | tail -1)
check 201 "$CODE" "Caso 17 — Crear categoría (ADMIN)"
CAT_ID=$(echo "$RESP" | head -1 | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)

# Caso 17a — OPERADOR no puede crear categoría
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/categorias" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nombre":"PRUEBA","descripcion":"Test"}')
check 403 "$CODE" "Caso 17a — OPERADOR no puede crear"

# Caso 17b — Nombre duplicado
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/categorias" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"nombre":"FRAGIL","descripcion":"Duplicado"}')
check 409 "$CODE" "Caso 17b — Categoría duplicada"

# Caso 19 — Listar categorías
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/categorias" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 19 — Listar categorías"

# ─────────────────────────────────────────────
section "PAQUETES (RF-02, RF-03, RF-04)"
# ─────────────────────────────────────────────

# Caso 7 — Registrar paquete
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/paquetes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d "{
    \"remitenteId\":\"$REMI_ID\",
    \"destinatarioId\":\"$DEST_ID\",
    \"pesoKg\":1.5,
    \"valorDeclarado\":200.00,
    \"sucursalOrigen\":\"LIMA\",
    \"sucursalDestino\":\"AREQUIPA\",
    \"categoriaIds\":[\"$CAT_ID\"]
  }")
CODE=$(echo "$RESP" | tail -1)
check 201 "$CODE" "Caso 7 — Registrar paquete"
PKG_ID=$(echo "$RESP" | head -1 | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
TARIFA=$(echo "$RESP" | head -1 | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['tarifa'])" 2>/dev/null)
echo -e "  → Tarifa: S/. $TARIFA"

# Caso 8 — Datos inválidos
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/paquetes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"pesoKg":-1,"valorDeclarado":0,"sucursalOrigen":"","sucursalDestino":""}')
check 400 "$CODE" "Caso 8 — Paquete datos inválidos"

# Caso 9 — Obtener por ID
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/$PKG_ID" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 9 — Obtener paquete por ID"

# Caso 10 — Paquete inexistente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $OP_TOKEN")
check 404 "$CODE" "Caso 10 — Paquete inexistente"

# Caso 11 — Transición válida REGISTRADO → EN_ALMACEN
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"operador1"}')
check 200 "$CODE" "Caso 11 — REGISTRADO → EN_ALMACEN"

# Caso 12 — Transición inválida EN_ALMACEN → ENTREGADO
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nuevoEstado":"ENTREGADO","usuarioResponsable":"operador1"}')
check 400 "$CODE" "Caso 12 — Transición inválida"

# Caso 12a — Cadena completa (ya en EN_ALMACEN)
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nuevoEstado":"EN_TRANSITO","usuarioResponsable":"op1"}')
check 200 "$CODE" "Caso 12a — EN_ALMACEN → EN_TRANSITO"

CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nuevoEstado":"EN_REPARTO","usuarioResponsable":"op1"}')
check 200 "$CODE" "Caso 12a — EN_TRANSITO → EN_REPARTO"

CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nuevoEstado":"ENTREGADO","usuarioResponsable":"op1"}')
check 200 "$CODE" "Caso 12a — EN_REPARTO → ENTREGADO"

# Caso 12b — ENTREGADO es terminal
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"op1"}')
check 400 "$CODE" "Caso 12b — ENTREGADO es terminal"

# Caso 12c — NO_ENTREGADO → EN_ALMACEN
RESP2=$(curl -s -X POST "$BASE_URL/api/v1/paquetes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d "{
    \"remitenteId\":\"$THIRD_ID\",
    \"destinatarioId\":\"$DEST_ID\",
    \"pesoKg\":2.0,
    \"valorDeclarado\":300.00,
    \"sucursalOrigen\":\"LIMA\",
    \"sucursalDestino\":\"CUSCO\",
    \"categoriaIds\":[\"$CAT_ID\"]
  }")
PKG2_ID=$(echo "$RESP2" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)
# Llevar a EN_REPARTO
curl -sf -X PUT "$BASE_URL/api/v1/paquetes/$PKG2_ID/estado" -H "Content-Type: application/json" -H "Authorization: Bearer $OP_TOKEN" -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"op1"}' >/dev/null
curl -sf -X PUT "$BASE_URL/api/v1/paquetes/$PKG2_ID/estado" -H "Content-Type: application/json" -H "Authorization: Bearer $OP_TOKEN" -d '{"nuevoEstado":"EN_TRANSITO","usuarioResponsable":"op1"}' >/dev/null
curl -sf -X PUT "$BASE_URL/api/v1/paquetes/$PKG2_ID/estado" -H "Content-Type: application/json" -H "Authorization: Bearer $OP_TOKEN" -d '{"nuevoEstado":"EN_REPARTO","usuarioResponsable":"op1"}' >/dev/null
# EN_REPARTO → NO_ENTREGADO
curl -sf -X PUT "$BASE_URL/api/v1/paquetes/$PKG2_ID/estado" -H "Content-Type: application/json" -H "Authorization: Bearer $OP_TOKEN" -d '{"nuevoEstado":"NO_ENTREGADO","usuarioResponsable":"op1"}' >/dev/null
# NO_ENTREGADO → EN_ALMACEN
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG2_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"op1"}')
check 200 "$CODE" "Caso 12c — NO_ENTREGADO → EN_ALMACEN"

# Caso 12d — Estado paquete inexistente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/00000000-0000-0000-0000-000000000000/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"op1"}')
check 404 "$CODE" "Caso 12d — Estado paquete inexistente"

# ─────────────────────────────────────────────
section "HISTORIAL DE ESTADOS (RF-05)"
# ─────────────────────────────────────────────

# Caso 13 — Historial existente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/$PKG_ID/historial" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 13 — Historial existente"

# Caso 13a — Historial inexistente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/00000000-0000-0000-0000-000000000000/historial" \
  -H "Authorization: Bearer $OP_TOKEN")
check 404 "$CODE" "Caso 13a — Historial inexistente"

# ─────────────────────────────────────────────
section "BÚSQUEDAS Y FILTROS (RF-06, RF-07)"
# ─────────────────────────────────────────────

# Caso 14 — Búsqueda por tracking code
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/buscar?texto=RC" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 14 — Búsqueda por tracking code"

# Caso 14a — Sin resultados
RESULT=$(curl -s -X GET "$BASE_URL/api/v1/paquetes/buscar?texto=ZZZZZ" \
  -H "Authorization: Bearer $OP_TOKEN" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('data',[])))" 2>/dev/null)
TOTAL=$((TOTAL + 1))
if [ "$RESULT" = "0" ]; then
  echo -e "  ${GREEN}PASS${NC} [Caso 14a — Sin resultados] — data: []"
  PASS=$((PASS + 1))
else
  echo -e "  ${RED}FAIL${NC} [Caso 14a — Sin resultados] — got $RESULT items"
  FAIL=$((FAIL + 1))
fi

# Caso 15 — Búsqueda por nombre cliente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/cliente?nombre=KEIKO" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 15 — Búsqueda por nombre cliente"

# Caso 16 — Filtro sucursal y estado
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/sucursal/LIMA/estado/EN_ALMACEN" \
  -H "Authorization: Bearer $OP_TOKEN")
check 200 "$CODE" "Caso 16 — Filtro sucursal y estado"

# ─────────────────────────────────────────────
section "ASIGNACIÓN DE CATEGORÍAS"
# ─────────────────────────────────────────────

# Caso 18 — Asignar categoría (PKG2 ya la tiene de registro, verificar 409)
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/paquetes/$PKG2_ID/categorias/$CAT_ID" \
  -H "Authorization: Bearer $OP_TOKEN")
check 409 "$CODE" "Caso 18 — Categoría ya asignada → 409"

# Caso 18a — Paquete inexistente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/paquetes/00000000-0000-0000-0000-000000000000/categorias/$CAT_ID" \
  -H "Authorization: Bearer $OP_TOKEN")
check 404 "$CODE" "Caso 18a — Paquete inexistente"

# Caso 18b — Categoría inexistente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/paquetes/$PKG2_ID/categorias/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $OP_TOKEN")
check 404 "$CODE" "Caso 18b — Categoría inexistente"

# ─────────────────────────────────────────────
section "CLIENTE — PAQUETES PROPIOS (RF-10)"
# ─────────────────────────────────────────────

# Caso 19a — CLIENTE ve sus paquetes
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/mis-paquetes" \
  -H "Authorization: Bearer $CLI_TOKEN")
check 200 "$CODE" "Caso 19a — CLIENTE ve sus paquetes"

# Caso 19b — CLIENTE ve historial de su paquete
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/mis-paquetes/$PKG_ID/historial" \
  -H "Authorization: Bearer $CLI_TOKEN")
check 200 "$CODE" "Caso 19b — CLIENTE historial de su paquete"

# Caso 19c — CLIENTE accede a paquete ajeno (404)
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/mis-paquetes/$PKG2_ID" \
  -H "Authorization: Bearer $CLI_TOKEN")
check 404 "$CODE" "Caso 19c — CLIENTE accede a paquete ajeno → 404"

# Caso 19d — OPERADOR no puede acceder a mis-paquetes
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/mis-paquetes" \
  -H "Authorization: Bearer $OP_TOKEN")
check 403 "$CODE" "Caso 19d — OPERADOR no puede acceder a mis-paquetes"

# ─────────────────────────────────────────────
section "ESCRITURA Y BORRADO"
# ─────────────────────────────────────────────

# Caso 20 — Actualizar paquete
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG2_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d '{"pesoKg":2.0,"valorDeclarado":300.00,"sucursalOrigen":"LIMA","sucursalDestino":"CUSCO"}')
check 200 "$CODE" "Caso 20 — Actualizar paquete"

# Crear paquete para eliminar
RESP3=$(curl -s -X POST "$BASE_URL/api/v1/paquetes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OP_TOKEN" \
  -d "{
    \"remitenteId\":\"$REMI_ID\",
    \"destinatarioId\":\"$DEST_ID\",
    \"pesoKg\":1.0,
    \"valorDeclarado\":100.00,
    \"sucursalOrigen\":\"LIMA\",
    \"sucursalDestino\":\"CUSCO\",
    \"categoriaIds\":[\"$CAT_ID\"]
  }")
PKG3_ID=$(echo "$RESP3" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null)

# Caso 21 — Eliminar paquete (ADMIN)
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/paquetes/$PKG3_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check 204 "$CODE" "Caso 21 — Eliminar paquete (ADMIN)"

# Caso 21a — OPERADOR no puede eliminar
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/paquetes/$PKG2_ID" \
  -H "Authorization: Bearer $OP_TOKEN")
check 403 "$CODE" "Caso 21a — OPERADOR no puede eliminar"

# Caso 22 — Eliminar inexistente
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/paquetes/00000000-0000-0000-0000-000000000000" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check 404 "$CODE" "Caso 22 — Eliminar inexistente"

# ─────────────────────────────────────────────
section "SEGURIDAD (RF-08)"
# ─────────────────────────────────────────────

# Caso 23 — Sin token
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/buscar?texto=test")
check 401 "$CODE" "Caso 23 — Sin token → 401"

# Caso 24 — CLIENTE intenta eliminar
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/paquetes/$PKG_ID" \
  -H "Authorization: Bearer $CLI_TOKEN")
check 403 "$CODE" "Caso 24 — CLIENTE intenta eliminar → 403"

# Caso 24a — CLIENTE no puede registrar paquete
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/paquetes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLI_TOKEN" \
  -d "{\"remitenteId\":\"$REMI_ID\",\"destinatarioId\":\"$DEST_ID\",\"pesoKg\":1.5,\"valorDeclarado\":200.00,\"sucursalOrigen\":\"LIMA\",\"sucursalDestino\":\"AREQUIPA\",\"categoriaIds\":[\"$CAT_ID\"]}")
check 403 "$CODE" "Caso 24a — CLIENTE no puede registrar → 403"

# Caso 24b — CLIENTE no puede cambiar estado
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/paquetes/$PKG_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CLI_TOKEN" \
  -d '{"nuevoEstado":"EN_ALMACEN","usuarioResponsable":"cli1"}')
check 403 "$CODE" "Caso 24b — CLIENTE no puede cambiar estado → 403"

# Caso 24c — CLIENTE no puede buscar por tracking
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/api/v1/paquetes/buscar?texto=RC" \
  -H "Authorization: Bearer $CLI_TOKEN")
check 403 "$CODE" "Caso 24c — CLIENTE no puede buscar por tracking → 403"

# ─────────────────────────────────────────────
echo ""
echo -e "${BOLD}══════════════════════════════════════${NC}"
echo -e "${BOLD}  RESULTADOS: ${GREEN}$PASS PASS${NC} / ${RED}$FAIL FAIL${NC} / $TOTAL TOTAL"
echo -e "${BOLD}══════════════════════════════════════${NC}"

if [ $FAIL -gt 0 ]; then
  exit 1
fi
