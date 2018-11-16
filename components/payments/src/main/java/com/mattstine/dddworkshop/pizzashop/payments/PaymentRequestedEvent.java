package com.mattstine.dddworkshop.pizzashop.payments;

import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
final class PaymentRequestedEvent implements PaymentEvent {
    PaymentRef ref;
}
