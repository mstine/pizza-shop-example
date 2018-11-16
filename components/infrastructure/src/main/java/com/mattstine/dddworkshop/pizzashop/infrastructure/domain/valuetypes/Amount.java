package com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Amount {
    public static Amount IDENTITY = Amount.of(0, 0);
    int dollars;
    int cents;

    public static Amount of(int dollars, int cents) {
        if (dollars < 0) {
            throw new IllegalArgumentException("Cannot build Amount with Dollars < 0");
        }

        if (cents < 0) {
            throw new IllegalArgumentException("Cannot build Amount with Cents < 0");
        }

        if (cents > 99) {
            throw new IllegalArgumentException("Cannot build Amount with Cents > 99");
        }

        return new Amount(dollars, cents);
    }

    public Amount plus(Amount amount) {
        int centsTotal = this.cents + amount.cents;

        if (centsTotal / 100 == 1) {
            return new Amount(this.dollars + amount.dollars + 1, centsTotal % 100);
        }

        return new Amount(this.dollars + amount.dollars,
                centsTotal);
    }
}
