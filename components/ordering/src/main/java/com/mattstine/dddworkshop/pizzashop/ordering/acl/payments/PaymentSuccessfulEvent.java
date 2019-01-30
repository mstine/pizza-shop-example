package com.mattstine.dddworkshop.pizzashop.ordering.acl.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import lombok.Value;
import lombok.experimental.NonFinal;

/**
 * @author Matt Stine
 */
@Value
public class PaymentSuccessfulEvent implements Event {
    @NonFinal
    PaymentRef ref;
}
