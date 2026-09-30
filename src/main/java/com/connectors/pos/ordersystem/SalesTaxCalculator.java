package com.connectors.pos.ordersystem;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SalesTaxCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private SalesTaxCalculator() {
    }

    public static BigDecimal calculateTax(BigDecimal taxableSubtotal, BigDecimal ratePercent) {
        if (taxableSubtotal == null || ratePercent == null
                || taxableSubtotal.signum() <= 0 || ratePercent.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return taxableSubtotal.multiply(ratePercent)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
