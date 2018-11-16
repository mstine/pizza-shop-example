package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Repository;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentRef;

/**
 * @author Matt Stine
 */
interface OnlineOrderRepository extends Repository<OnlineOrderRef, OnlineOrder, OnlineOrder.OrderState, OnlineOrderEvent, OnlineOrderAddedEvent> {
    void add(OnlineOrder onlineOrder);

    OnlineOrderRef nextIdentity();

    OnlineOrder findByRef(OnlineOrderRef ref);

    OnlineOrder findByPaymentRef(PaymentRef paymentRef);
}
