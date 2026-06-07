package com.rapidocurier.paquetesservice.application.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TarifaCalculator {

    private static final Map<String, Double> TARIFAS_RUTA = Map.of(
        "LIMA-AREQUIPA", 15.0, "AREQUIPA-LIMA", 15.0,
        "LIMA-CUSCO", 20.0, "CUSCO-LIMA", 20.0,
        "AREQUIPA-CUSCO", 12.0, "CUSCO-AREQUIPA", 12.0
    );

    public double calcular(double pesoKg, double valorDeclarado,
                           String origen, String destino) {
        String clave = origen.toUpperCase() + "-" + destino.toUpperCase();
        double tarifaRuta = TARIFAS_RUTA.getOrDefault(clave, 5.0);
        return (pesoKg * 8.0) + (valorDeclarado * 0.01) + tarifaRuta;
    }
}
