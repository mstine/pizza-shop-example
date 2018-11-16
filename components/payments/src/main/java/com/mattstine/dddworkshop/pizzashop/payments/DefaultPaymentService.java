package com.mattstine.dddworkshop.pizzashop.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;

/**
 * @author Matt Stine
 */
final class DefaultPaymentService implements PaymentService {
    private final PaymentProcessor processor;
    private final PaymentRepository repository;
    private final EventLog eventLog;

    DefaultPaymentService(PaymentProcessor processor, PaymentRepository repository, EventLog eventLog) {
        this.processor = processor;
        this.repository = repository;
        this.eventLog = eventLog;

        eventLog.subscribe(new Topic("payment_processor"), (e) -> {
            if (e instanceof PaymentProcessedEvent) {
                PaymentProcessedEvent ppe = (PaymentProcessedEvent) e;
                if (ppe.isSuccessful()) {
                    markPaymentSuccessful(ppe.getRef());
                } else if (ppe.isFailed()) {
                    markPaymentFailed(ppe.getRef());
                }
            }
        });
    }

    @Override
    public PaymentRef createPaymentOf(Amount amount) {
        PaymentRef ref = repository.nextIdentity();

        Payment payment = Payment.builder()
                .amount(amount)
                .ref(ref)
                .paymentProcessor(processor)
                .eventLog(eventLog)
                .build();

        repository.add(payment);

        return ref;
    }

    @Override
    public void requestPaymentFor(PaymentRef ref) {
        Payment payment = repository.findByRef(ref);
        payment.request();
    }

    private void markPaymentSuccessful(PaymentRef ref) {
        Payment payment = repository.findByRef(ref);
        payment.markSuccessful();
    }

    private void markPaymentFailed(PaymentRef ref) {
        Payment payment = repository.findByRef(ref);
        payment.markFailed();
    }
}
