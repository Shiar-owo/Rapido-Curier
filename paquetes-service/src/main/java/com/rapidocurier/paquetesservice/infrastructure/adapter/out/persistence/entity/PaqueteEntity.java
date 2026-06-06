package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "paquetes")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class PaqueteEntity {

    @Id
    private UUID id;

    @Column(name = "codigo_rastreo", nullable = false, unique = true)
    private String codigoRastreo;

    @Column(name = "remitente_id", nullable = false)
    private UUID remitenteId;

    @Column(name = "destinatario_id", nullable = false)
    private UUID destinatarioId;

    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @Column(name = "valor_declarado", nullable = false)
    private Double valorDeclarado;

    @Column(name = "sucursal_origen", nullable = false)
    private String sucursalOrigen;

    @Column(name = "sucursal_destino", nullable = false)
    private String sucursalDestino;

    @Column(name = "tarifa", nullable = false)
    private Double tarifa;

    @Column(name = "estado_actual", nullable = false)
    private String estadoActual;

    @ManyToMany
    @JoinTable(
        name = "paquete_categoria",
        joinColumns = @JoinColumn(name = "paquete_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private Set<CategoriaEntity> categorias = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
