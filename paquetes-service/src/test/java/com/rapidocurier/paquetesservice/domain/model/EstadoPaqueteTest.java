package com.rapidocurier.paquetesservice.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoPaqueteTest {

    @Test
    void registrado_haciaEnAlmacen_esValida() {
        assertTrue(EstadoPaquete.REGISTRADO.esValida(EstadoPaquete.EN_ALMACEN));
    }

    @Test
    void enAlmacen_haciaEnTransito_esValida() {
        assertTrue(EstadoPaquete.EN_ALMACEN.esValida(EstadoPaquete.EN_TRANSITO));
    }

    @Test
    void enTransito_haciaEnReparto_esValida() {
        assertTrue(EstadoPaquete.EN_TRANSITO.esValida(EstadoPaquete.EN_REPARTO));
    }

    @Test
    void enReparto_haciaEntregado_esValida() {
        assertTrue(EstadoPaquete.EN_REPARTO.esValida(EstadoPaquete.ENTREGADO));
    }

    @Test
    void enReparto_haciaNoEntregado_esValida() {
        assertTrue(EstadoPaquete.EN_REPARTO.esValida(EstadoPaquete.NO_ENTREGADO));
    }

    @Test
    void noEntregado_haciaEnAlmacen_esValida() {
        assertTrue(EstadoPaquete.NO_ENTREGADO.esValida(EstadoPaquete.EN_ALMACEN));
    }

    @Test
    void registrado_haciaEnTransito_esInvalida() {
        assertFalse(EstadoPaquete.REGISTRADO.esValida(EstadoPaquete.EN_TRANSITO));
    }

    @Test
    void registrado_haciaEntregado_esInvalida() {
        assertFalse(EstadoPaquete.REGISTRADO.esValida(EstadoPaquete.ENTREGADO));
    }

    @Test
    void enAlmacen_haciaEnReparto_esInvalida() {
        assertFalse(EstadoPaquete.EN_ALMACEN.esValida(EstadoPaquete.EN_REPARTO));
    }

    @Test
    void entregado_haciaCualquierEstado_esInvalida() {
        for (EstadoPaquete estado : EstadoPaquete.values()) {
            assertFalse(EstadoPaquete.ENTREGADO.esValida(estado),
                "ENTREGADO → " + estado + " debería ser inválida");
        }
    }

    @Test
    void estadoFinal_noTieneTransiciones() {
        assertTrue(EstadoPaquete.TRANSICIONES.get(EstadoPaquete.ENTREGADO) == null);
    }

    @Test
    void todasTransiciones_sonIrreversibles() {
        assertFalse(EstadoPaquete.EN_ALMACEN.esValida(EstadoPaquete.REGISTRADO));
        assertFalse(EstadoPaquete.EN_TRANSITO.esValida(EstadoPaquete.EN_ALMACEN));
        assertFalse(EstadoPaquete.EN_REPARTO.esValida(EstadoPaquete.EN_TRANSITO));
    }
}
