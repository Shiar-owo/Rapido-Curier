package com.rapidocurier.paquetesservice.application.service;

import com.rapidocurier.paquetesservice.application.config.TarifaProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TarifaCalculatorTest {

    private TarifaCalculator calculator;

    @BeforeEach
    void setUp() {
        TarifaProperties props = new TarifaProperties();
        props.setCostoPorKg(8.0);
        props.setPorcentajeValorDeclarado(0.01);
        props.setTarifaDefault(5.0);
        props.setRutas(Map.of(
            "LIMA-AREQUIPA", 15.0, "AREQUIPA-LIMA", 15.0,
            "LIMA-CUSCO", 20.0, "CUSCO-LIMA", 20.0,
            "AREQUIPA-CUSCO", 12.0, "CUSCO-AREQUIPA", 12.0
        ));
        calculator = new TarifaCalculator(props);
    }

    @Test
    void calcular_rutaConocida_LimaArequipa() {
        // peso=10 * 8.0 + valor=500 * 0.01 + tarifaRuta=15.0 = 100.0
        double result = calculator.calcular(10.0, 500.0, "LIMA", "AREQUIPA");
        assertEquals(100.0, result);
    }

    @Test
    void calcular_rutaConocida_ArequipaLima() {
        // 10*8 + 500*0.01 + 15.0 = 100.0
        double result = calculator.calcular(10.0, 500.0, "AREQUIPA", "LIMA");
        assertEquals(100.0, result);
    }

    @Test
    void calcular_rutaConocida_LimaCusco() {
        // 10*8 + 500*0.01 + 20.0 = 105.0
        double result = calculator.calcular(10.0, 500.0, "LIMA", "CUSCO");
        assertEquals(105.0, result);
    }

    @Test
    void calcular_rutaConocida_CuscoLima() {
        // 10*8 + 500*0.01 + 20.0 = 105.0
        double result = calculator.calcular(10.0, 500.0, "CUSCO", "LIMA");
        assertEquals(105.0, result);
    }

    @Test
    void calcular_rutaConocida_ArequipaCusco() {
        // 10*8 + 500*0.01 + 12.0 = 97.0
        double result = calculator.calcular(10.0, 500.0, "AREQUIPA", "CUSCO");
        assertEquals(97.0, result);
    }

    @Test
    void calcular_rutaConocida_CuscoArequipa() {
        // 10*8 + 500*0.01 + 12.0 = 97.0
        double result = calculator.calcular(10.0, 500.0, "CUSCO", "AREQUIPA");
        assertEquals(97.0, result);
    }

    @Test
    void calcular_rutaDesconocida_usaTarifaDefault() {
        // 5*8 + 200*0.01 + 5.0(default) = 47.0
        double result = calculator.calcular(5.0, 200.0, "TRUJILLO", "PIURA");
        assertEquals(47.0, result);
    }

    @Test
    void calcular_caseInsensitive() {
        // lowercase should work same as uppercase
        double resultUpper = calculator.calcular(10.0, 500.0, "LIMA", "AREQUIPA");
        double resultLower = calculator.calcular(10.0, 500.0, "lima", "arequipa");
        assertEquals(resultUpper, resultLower);
    }

    @Test
    void calcular_pesoYValorCero_soloTarifaRuta() {
        // 0*8 + 0*0.01 + 15.0 = 15.0
        double result = calculator.calcular(0.0, 0.0, "LIMA", "AREQUIPA");
        assertEquals(15.0, result);
    }

    @Test
    void calcular_valoresGrandes() {
        // 100*8 + 10000*0.01 + 20.0 = 800+100+20 = 920.0
        double result = calculator.calcular(100.0, 10000.0, "LIMA", "CUSCO");
        assertEquals(920.0, result);
    }
}
