package com.rapidocurier.paquetesservice.domain.exception;

import com.rapidocurier.paquetesservice.domain.model.EstadoPaquete;

public class TransicionInvalidaException extends RuntimeException {
    public TransicionInvalidaException(EstadoPaquete actual, EstadoPaquete siguiente) {
        super("Transición inválida: " + actual + " → " + siguiente);
    }
}
