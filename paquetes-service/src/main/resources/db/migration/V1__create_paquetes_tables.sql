CREATE TABLE IF NOT EXISTS categorias (
    id UUID PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS paquetes (
    id UUID PRIMARY KEY,
    codigo_rastreo VARCHAR(20) NOT NULL UNIQUE,
    remitente_id UUID NOT NULL,
    destinatario_id UUID NOT NULL,
    peso_kg DOUBLE PRECISION NOT NULL,
    valor_declarado DOUBLE PRECISION NOT NULL,
    sucursal_origen VARCHAR(100) NOT NULL,
    sucursal_destino VARCHAR(100) NOT NULL,
    tarifa DOUBLE PRECISION NOT NULL,
    estado_actual VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS paquete_categoria (
    paquete_id UUID NOT NULL,
    categoria_id UUID NOT NULL,
    PRIMARY KEY (paquete_id, categoria_id),
    CONSTRAINT fk_paquete_categoria_paquete FOREIGN KEY (paquete_id) REFERENCES paquetes(id) ON DELETE CASCADE,
    CONSTRAINT fk_paquete_categoria_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS historial_estado (
    id UUID PRIMARY KEY,
    paquete_id UUID NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_cambio TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    usuario_responsable VARCHAR(100) NOT NULL,
    CONSTRAINT fk_historial_estado_paquete FOREIGN KEY (paquete_id) REFERENCES paquetes(id) ON DELETE CASCADE
);

CREATE INDEX idx_paquetes_codigo_rastreo ON paquetes(codigo_rastreo);
CREATE INDEX idx_paquetes_remitente_id ON paquetes(remitente_id);
CREATE INDEX idx_paquetes_destinatario_id ON paquetes(destinatario_id);
CREATE INDEX idx_paquetes_estado_actual ON paquetes(estado_actual);
CREATE INDEX idx_historial_estado_paquete_id ON historial_estado(paquete_id);
