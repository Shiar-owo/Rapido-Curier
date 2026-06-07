package com.rapidocurier.paquetesservice.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "tarifa")
public class TarifaProperties {

    private double costoPorKg = 8.0;
    private double porcentajeValorDeclarado = 0.01;
    private Map<String, Double> rutas = Map.of(
        "LIMA-AREQUIPA", 15.0, "AREQUIPA-LIMA", 15.0,
        "LIMA-CUSCO", 20.0, "CUSCO-LIMA", 20.0,
        "AREQUIPA-CUSCO", 12.0, "CUSCO-AREQUIPA", 12.0
    );
    private double tarifaDefault = 5.0;

    public double getCostoPorKg() { return costoPorKg; }
    public void setCostoPorKg(double costoPorKg) { this.costoPorKg = costoPorKg; }

    public double getPorcentajeValorDeclarado() { return porcentajeValorDeclarado; }
    public void setPorcentajeValorDeclarado(double porcentajeValorDeclarado) { this.porcentajeValorDeclarado = porcentajeValorDeclarado; }

    public Map<String, Double> getRutas() { return rutas; }
    public void setRutas(Map<String, Double> rutas) { this.rutas = rutas; }

    public double getTarifaDefault() { return tarifaDefault; }
    public void setTarifaDefault(double tarifaDefault) { this.tarifaDefault = tarifaDefault; }
}
