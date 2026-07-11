package com.example.crossborder.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class ProfitCalculator {
    public Result calculate(BigDecimal revenueCny, BigDecimal purchaseCostCny, BigDecimal shippingCny,
                            BigDecimal platformFeeCny, BigDecimal paymentFeeCny, BigDecimal taxFeeCny, BigDecimal adCostCny) {
        BigDecimal profit = value(revenueCny).subtract(value(purchaseCostCny)).subtract(value(shippingCny))
            .subtract(value(platformFeeCny)).subtract(value(paymentFeeCny)).subtract(value(taxFeeCny)).subtract(value(adCostCny))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal margin = value(revenueCny).signum() == 0 ? BigDecimal.ZERO.setScale(4) : profit.divide(value(revenueCny), 4, RoundingMode.HALF_UP);
        return new Result(profit, margin);
    }

    private BigDecimal value(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    public record Result(BigDecimal profitCny, BigDecimal margin) {}
}
