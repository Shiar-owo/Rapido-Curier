-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(20) NOT NULL UNIQUE
);

-- Usuarios table
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
);

-- Usuario-Roles junction table
CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

-- Insert default roles
INSERT INTO roles (nombre) VALUES ('ADMIN'), ('OPERADOR'), ('CLIENTE') ON CONFLICT (nombre) DO NOTHING;