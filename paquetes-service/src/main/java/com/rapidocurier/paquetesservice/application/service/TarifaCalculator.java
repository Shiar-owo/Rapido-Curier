package com.rapidocurier.paquetesservice.application.service;

import com.rapidocurier.paquetesservice.application.config.TarifaProperties;

import org.springframework.stereotype.Service;

@Service
public class TarifaCalculator {

    private final TarifaProperties tarifaProperties;

    public TarifaCalculator(TarifaProperties tarifaProperties) {
        this.tarifaProperties = tarifaProperties;
    }

    public double calcular(double pesoKg, double valorDeclarado,
                           String origen, String destino) {
        String clave = origen.toUpperCase() + "-" + destino.toUpperCase();
        double tarifaRuta = tarifaProperties.getRutas().getOrDefault(clave, tarifaProperties.getTarifaDefault());
        return (pesoKg * tarifaProperties.getCostoPorKg())
             + (valorDeclarado * tarifaProperties.getPorcentajeValorDeclarado())
             + tarifaRuta;
    }
}
