#!/bin/sh
set -e

VAULT_ADDR="http://vault:8200"
export VAULT_ADDR
export VAULT_TOKEN="dev-only-token"

echo "Waiting for Vault to be ready..."
until vault status -address=$VAULT_ADDR > /dev/null 2>&1; do
    sleep 2
done
echo "Vault is ready!"

# Enable KV v2 at secret/ path
vault secrets enable -address=$VAULT_ADDR -path=secret kv-v2 2>/dev/null || true

# Load secrets for auth-service
vault kv put -address=$VAULT_ADDR secret/auth-service \
    jwt.secret="contrasenia-super-mega-secreta-32-caracteres" \
    jwt.expiration="86400000" \
    db.username="auth_user" \
    db.password="auth_pass"

# Load secrets for clients-service
vault kv put -address=$VAULT_ADDR secret/clients-service \
    reniec.api.token="sk_14107.2R91IK9p8iH3dv0u5D7RJYgwDHgykbli" \
    db.username="clientes_user" \
    db.password="clientes_pass"

# Load secrets for api-gateway
vault kv put -address=$VAULT_ADDR secret/api-gateway \
    jwt.secret="contrasenia-super-mega-secreta-32-caracteres"

# Load secrets for paquetes-service
vault kv put -address=$VAULT_ADDR secret/paquetes-service \
    jwt.secret="contrasenia-super-mega-secreta-32-caracteres" \
    db.username="paquetes_user" \
    db.password="paquetes_pass"

echo "All secrets loaded into Vault!"
