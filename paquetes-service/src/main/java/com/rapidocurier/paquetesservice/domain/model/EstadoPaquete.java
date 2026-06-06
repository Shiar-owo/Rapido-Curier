package com.rapidocurier.paquetesservice.domain.model;

import java.util.Map;
import java.util.Set;

public enum EstadoPaquete {
    REGISTRADO, EN_ALMACEN, EN_TRANSITO, EN_REPARTO, ENTREGADO, NO_ENTREGADO;

    public static final Map<EstadoPaquete, Set<EstadoPaquete>> TRANSICIONES = Map.of(
        REGISTRADO,    Set.of(EN_ALMACEN),
        EN_ALMACEN,    Set.of(EN_TRANSITO),
        EN_TRANSITO,   Set.of(EN_REPARTO),
        EN_REPARTO,    Set.of(ENTREGADO, NO_ENTREGADO),
        NO_ENTREGADO,  Set.of(EN_ALMACEN)
    );

    public boolean esValida(EstadoPaquete siguiente) {
        return TRANSICIONES.getOrDefault(this, Set.of()).contains(siguiente);
    }
}
