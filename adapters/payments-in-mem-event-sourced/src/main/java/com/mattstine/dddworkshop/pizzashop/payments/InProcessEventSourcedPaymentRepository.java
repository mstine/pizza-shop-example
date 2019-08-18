package com.mattstine.dddworkshop.pizzashop.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.adapters.InProcessEventSourcedRepository;

/**
 * @author Matt Stine
 */
final class InProcessEventSourcedPaymentRepository extends InProcessEventSourcedRepository<PaymentRef, Payment, Payment.PaymentState, PaymentEvent, PaymentAddedEvent> implements PaymentRepository {
    InProcessEventSourcedPaymentRepository(EventLog eventLog,
                                           Topic topic) {
        super(eventLog, PaymentRef.class, Payment.class, Payment.PaymentState.class, PaymentAddedEvent.class, topic);
    }
}
