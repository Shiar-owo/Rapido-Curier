package com.rapidocurier.paquetesservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "historial_estado")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class HistorialEstadoEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paquete_id", nullable = false)
    private PaqueteEntity paquete;

    @Column(name = "estado", nullable = false)
    private String estado;

    @CreatedDate
    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private OffsetDateTime fechaCambio;

    @Column(name = "usuario_responsable", nullable = false)
    private String usuarioResponsable;
}
