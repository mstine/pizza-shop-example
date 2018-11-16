package com.mattstine.dddworkshop.pizzashop.payments;

import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
final class PaymentFailedEvent implements PaymentEvent {
    PaymentRef ref;
}
