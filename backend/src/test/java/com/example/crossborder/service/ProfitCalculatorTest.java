package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProfitCalculatorTest {
    private final ProfitCalculator calculator = new ProfitCalculator();

    @Test
    void calculate_subtractsAllConfiguredCosts() {
        ProfitCalculator.Result result = calculator.calculate(
            new BigDecimal("100.00"), new BigDecimal("40.00"), new BigDecimal("10.00"),
            new BigDecimal("8.00"), new BigDecimal("2.00"), new BigDecimal("3.00"), new BigDecimal("5.00")
        );

        assertEquals(new BigDecimal("32.00"), result.profitCny());
        assertEquals(new BigDecimal("0.3200"), result.margin());
    }
}
