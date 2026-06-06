package com.rapidocurier.paquetesservice.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Paquete {

    private UUID id;
    private String codigoRastreo;
    private UUID remitenteId;
    private UUID destinatarioId;
    private Double pesoKg;
    private Double valorDeclarado;
    private String sucursalOrigen;
    private String sucursalDestino;
    private Double tarifa;
    private EstadoPaquete estadoActual;
    private Set<Categoria> categorias;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Paquete(UUID id, String codigoRastreo, UUID remitenteId, UUID destinatarioId,
                    Double pesoKg, Double valorDeclarado, String sucursalOrigen,
                    String sucursalDestino, Double tarifa, EstadoPaquete estadoActual,
                    Set<Categoria> categorias, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.codigoRastreo = codigoRastreo;
        this.remitenteId = remitenteId;
        this.destinatarioId = destinatarioId;
        this.pesoKg = pesoKg;
        this.valorDeclarado = valorDeclarado;
        this.sucursalOrigen = sucursalOrigen;
        this.sucursalDestino = sucursalDestino;
        this.tarifa = tarifa;
        this.estadoActual = estadoActual;
        this.categorias = categorias;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Paquete create(UUID remitenteId, UUID destinatarioId,
                                 Double pesoKg, Double valorDeclarado,
                                 String sucursalOrigen, String sucursalDestino,
                                 Double tarifa, Set<Categoria> categorias) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Paquete(
            UUID.randomUUID(),
            generarCodigo(),
            remitenteId, destinatarioId,
            pesoKg, valorDeclarado,
            sucursalOrigen, sucursalDestino,
            tarifa,
            EstadoPaquete.REGISTRADO,
            categorias,
            now, now
        );
    }

    public static Paquete rehydrate(UUID id, String codigoRastreo, UUID remitenteId,
                                    UUID destinatarioId, Double pesoKg, Double valorDeclarado,
                                    String sucursalOrigen, String sucursalDestino,
                                    Double tarifa, EstadoPaquete estadoActual,
                                    Set<Categoria> categorias,
                                    OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new Paquete(id, codigoRastreo, remitenteId, destinatarioId,
            pesoKg, valorDeclarado, sucursalOrigen, sucursalDestino,
            tarifa, estadoActual, categorias, createdAt, updatedAt);
    }

    private static String generarCodigo() {
        LocalDate now = LocalDate.now();
        return "RC" + now.getYear()
            + String.format("%02d", now.getMonthValue())
            + String.format("%04d", new Random().nextInt(10000));
    }

    public UUID getId() { return id; }

    public String getCodigoRastreo() { return codigoRastreo; }

    public UUID getRemitenteId() { return remitenteId; }
    public void setRemitenteId(UUID remitenteId) { this.remitenteId = remitenteId; }

    public UUID getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(UUID destinatarioId) { this.destinatarioId = destinatarioId; }

    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }

    public Double getValorDeclarado() { return valorDeclarado; }
    public void setValorDeclarado(Double valorDeclarado) { this.valorDeclarado = valorDeclarado; }

    public String getSucursalOrigen() { return sucursalOrigen; }
    public void setSucursalOrigen(String sucursalOrigen) { this.sucursalOrigen = sucursalOrigen; }

    public String getSucursalDestino() { return sucursalDestino; }
    public void setSucursalDestino(String sucursalDestino) { this.sucursalDestino = sucursalDestino; }

    public Double getTarifa() { return tarifa; }
    public void setTarifa(Double tarifa) { this.tarifa = tarifa; }

    public EstadoPaquete getEstadoActual() { return estadoActual; }
    public void setEstadoActual(EstadoPaquete estadoActual) { this.estadoActual = estadoActual; }

    public Set<Categoria> getCategorias() { return categorias; }
    public void setCategorias(Set<Categoria> categorias) { this.categorias = categorias; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
