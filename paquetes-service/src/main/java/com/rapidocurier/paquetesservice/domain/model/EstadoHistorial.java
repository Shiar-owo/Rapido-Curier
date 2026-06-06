package com.rapidocurier.paquetesservice.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EstadoHistorial {

    private UUID id;
    private UUID paqueteId;
    private EstadoPaquete estado;
    private OffsetDateTime fechaCambio;
    private String usuarioResponsable;

    public EstadoHistorial() {}

    public EstadoHistorial(UUID id, UUID paqueteId, EstadoPaquete estado,
                           OffsetDateTime fechaCambio, String usuarioResponsable) {
        this.id = id;
        this.paqueteId = paqueteId;
        this.estado = estado;
        this.fechaCambio = fechaCambio;
        this.usuarioResponsable = usuarioResponsable;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPaqueteId() { return paqueteId; }
    public void setPaqueteId(UUID paqueteId) { this.paqueteId = paqueteId; }

    public EstadoPaquete getEstado() { return estado; }
    public void setEstado(EstadoPaquete estado) { this.estado = estado; }

    public OffsetDateTime getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(OffsetDateTime fechaCambio) { this.fechaCambio = fechaCambio; }

    public String getUsuarioResponsable() { return usuarioResponsable; }
    public void setUsuarioResponsable(String usuarioResponsable) { this.usuarioResponsable = usuarioResponsable; }
}
