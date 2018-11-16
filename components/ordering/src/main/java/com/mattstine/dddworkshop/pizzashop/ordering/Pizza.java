package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
public final class Pizza {

    Size size;

    @SuppressWarnings("unused")
    @Builder
    private Pizza(@NonNull Size size) {
        this.size = size;
    }

    Amount calculatePrice() {
        return size.getPrice();
    }

    public enum Size {
        MEDIUM(Amount.of(6, 0));

        final Amount price;

        Size(Amount price) {
            this.price = price;
        }

        public Amount getPrice() {
            return price;
        }
    }
}
