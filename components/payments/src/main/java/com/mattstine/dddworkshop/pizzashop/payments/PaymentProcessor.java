package com.mattstine.dddworkshop.pizzashop.payments;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author Matt Stine
 */
interface PaymentProcessor {
    PaymentProcessor IDENTITY = payment -> {
        throw new NotImplementedException();
    };

    @SuppressWarnings({"EmptyMethod", "unused"})
    void request(Payment payment);
}
