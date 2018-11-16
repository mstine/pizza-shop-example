package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.adapters.InProcessEventSourcedRepository;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentRef;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Matt Stine
 */
final class InProcessEventSourcedOnlineOrderRepository extends InProcessEventSourcedRepository<OnlineOrderRef, OnlineOrder, OnlineOrder.OrderState, OnlineOrderEvent, OnlineOrderAddedEvent> implements OnlineOrderRepository {

    private final Map<PaymentRef, OnlineOrderRef> paymentRefToOrderRef;

    InProcessEventSourcedOnlineOrderRepository(EventLog eventLog,
                                               Topic topic) {
        super(eventLog, OnlineOrderRef.class, OnlineOrder.class, OnlineOrder.OrderState.class, OnlineOrderAddedEvent.class, topic);

        paymentRefToOrderRef = new HashMap<>();

        eventLog.subscribe(topic, (e) -> {
            if (e instanceof PaymentRefAssignedEvent) {
                @SuppressWarnings("SpellCheckingInspection")
                PaymentRefAssignedEvent prae = (PaymentRefAssignedEvent) e;
                this.paymentRefToOrderRef.put(prae.getPaymentRef(), prae.getRef());
            }
        });
    }

    @Override
    public OnlineOrder findByPaymentRef(PaymentRef paymentRef) {
        OnlineOrderRef ref = paymentRefToOrderRef.get(paymentRef);
        if (ref != null) {
            return this.findByRef(ref);
        }
        return null;
    }
}
