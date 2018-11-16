package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.ordering.acl.payments.PaymentRef;
import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
final class PaymentRefAssignedEvent implements OnlineOrderEvent {
    OnlineOrderRef ref;
    PaymentRef paymentRef;
}
