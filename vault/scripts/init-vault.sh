#!/bin/sh
set -e

apk add --no-cache jq curl

VAULT_ADDR="http://127.0.0.1:8200"
export VAULT_ADDR

# Start Vault server in background
echo "Starting Vault server..."
vault server -config=/vault/config/dev-server.hcl &
VAULT_PID=$!

# Wait for Vault to be ready
echo "Waiting for Vault to be ready..."
until curl -s $VAULT_ADDR/v1/sys/health > /dev/null 2>&1; do
    sleep 2
done
echo "Vault ready!"

# Check if initialized
STATUS=$(curl -s $VAULT_ADDR/v1/sys/health)
if echo "$STATUS" | grep -q '"initialized":true'; then
    echo "Vault already initialized."
    if echo "$STATUS" | grep -q '"sealed":true'; then
        if [ -f /vault/data/vault-credentials.env ]; then
            . /vault/data/vault-credentials.env
            vault operator unseal $VAULT_UNSEAL_KEY
        fi
    fi
else
    echo "Initializing Vault..."
    INIT_OUTPUT=$(vault operator init -key-shares=1 -key-threshold=1 -format=json)
    UNSEAL_KEY=$(echo $INIT_OUTPUT | jq -r '.unseal_keys_b64[0]')
    ROOT_TOKEN=$(echo $INIT_OUTPUT | jq -r '.root_token')

    echo "export VAULT_TOKEN=$ROOT_TOKEN" > /vault/data/vault-credentials.env
    echo "export VAULT_UNSEAL_KEY=$UNSEAL_KEY" >> /vault/data/vault-credentials.env

    vault operator unseal $UNSEAL_KEY
    echo "Vault initialized!"
fi

# Enable KV and load secrets
vault secrets enable -path=secret kv-v2 2>/dev/null || true

vault kv put secret/rapidocourier/auth \
    db.username="auth_user" db.password="auth_pass" jwt.secret="clave-32-caracteres-minimo-aqui"

vault kv put secret/rapidocourier/clientes \
    db.username="clientes_user" db.password="clientes_pass" reniec.token="Bearer reniec_token"

vault kv put secret/rapidocourier/paquetes \
    db.username="paquetes_user" db.password="paquetes_pass"

echo "Secrets loaded!"

# Wait for Vault process
wait $VAULT_PID