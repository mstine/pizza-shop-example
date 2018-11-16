package com.mattstine.dddworkshop.pizzashop.payments;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DummyPaymentProcessor implements PaymentProcessor {
    private static DummyPaymentProcessor singleton;

    public static DummyPaymentProcessor instance() {
        if (singleton == null) {
            singleton = new DummyPaymentProcessor();
        }
        return singleton;
    }

    @Override
    public void request(Payment payment) {
        // Do nothing
    }
}
