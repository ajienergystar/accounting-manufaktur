package com.aji.accounting.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helper aritmetika uang. Semua nilai uang memakai BigDecimal, tidak pernah float/double.
 * AMOUNT_SCALE = 2 untuk nilai rupiah, COST_SCALE = 4 untuk harga/biaya per unit & kuantitas.
 */
public final class Money {
    public static final int AMOUNT_SCALE = 2;
    public static final int COST_SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public static BigDecimal amount(BigDecimal v) {
        return nz(v).setScale(AMOUNT_SCALE, ROUNDING);
    }

    public static BigDecimal cost(BigDecimal v) {
        return nz(v).setScale(COST_SCALE, ROUNDING);
    }

    /** qty x harga, dibulatkan ke 2 desimal. */
    public static BigDecimal multiply(BigDecimal qty, BigDecimal price) {
        return nz(qty).multiply(nz(price)).setScale(AMOUNT_SCALE, ROUNDING);
    }

    /** Pembagian aman untuk biaya per unit (4 desimal). Pembagi nol menghasilkan nol. */
    public static BigDecimal divideCost(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() == 0) {
            return BigDecimal.ZERO.setScale(COST_SCALE);
        }
        return nz(numerator).divide(denominator, COST_SCALE, ROUNDING);
    }

    public static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return nz(base).multiply(nz(percent)).divide(BigDecimal.valueOf(100), AMOUNT_SCALE, ROUNDING);
    }

    public static boolean isZero(BigDecimal v) {
        return v == null || v.signum() == 0;
    }

    public static boolean eq(BigDecimal a, BigDecimal b) {
        return nz(a).compareTo(nz(b)) == 0;
    }
}
