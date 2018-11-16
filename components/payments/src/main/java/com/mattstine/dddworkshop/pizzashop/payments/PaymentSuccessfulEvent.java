package com.mattstine.dddworkshop.pizzashop.payments;

import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
public final class PaymentSuccessfulEvent implements PaymentEvent {
    PaymentRef ref;
}
