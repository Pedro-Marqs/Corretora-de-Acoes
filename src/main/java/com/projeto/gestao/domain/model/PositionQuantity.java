package com.projeto.gestao.domain.model;

/** Quantidade inteira e não negativa mantida em uma posição. */
public record PositionQuantity(long value) {
    public PositionQuantity {
        if (value < 0) {
            throw new IllegalArgumentException("position quantity must not be negative");
        }
    }

    public static PositionQuantity zero() {
        return new PositionQuantity(0);
    }

    public static PositionQuantity positive(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        return new PositionQuantity(value);
    }

    public boolean isZero() {
        return value == 0;
    }

    public PositionQuantity add(PositionQuantity other) {
        return new PositionQuantity(Math.addExact(value, other.value));
    }

    public PositionQuantity subtract(PositionQuantity other) {
        if (other.value > value) {
            throw new IllegalArgumentException("quantity exceeds available position");
        }
        return new PositionQuantity(value - other.value);
    }
}
