package com.mattstine.dddworkshop.pizzashop.payments;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.function.BiFunction;

/**
 * @author Matt Stine
 */
@SuppressWarnings("DefaultAnnotationParam")
@Value
@EqualsAndHashCode(callSuper = false)
public final class Payment implements Aggregate {
    Amount amount;
    PaymentProcessor $paymentProcessor;
    PaymentRef ref;
    EventLog $eventLog;
    @NonFinal
    State state;

    @Builder
    private Payment(@NonNull Amount amount,
                    @NonNull PaymentProcessor paymentProcessor,
                    @NonNull PaymentRef ref,
                    @NonNull EventLog eventLog) {
        this.amount = amount;
        this.$paymentProcessor = paymentProcessor;
        this.ref = ref;
        this.$eventLog = eventLog;

        this.state = State.NEW;
    }

    /**
     * Private no-args ctor to support reflection ONLY.
     */
    @SuppressWarnings("unused")
    private Payment() {
        this.amount = null;
        this.$eventLog = null;
        this.$paymentProcessor = null;
        this.ref = null;
    }

    public boolean isNew() {
        return state == State.NEW;
    }

    boolean isRequested() {
        return state == State.REQUESTED;
    }

    boolean isSuccessful() {
        return state == State.SUCCESSFUL;
    }

    boolean isFailed() {
        return state == State.FAILED;
    }

    void request() {
        if (state != State.NEW) {
            throw new IllegalStateException("Payment must be NEW to request payment");
        }

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $paymentProcessor != null;
        $paymentProcessor.request(this);

        state = State.REQUESTED;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("payments"), new PaymentRequestedEvent(this.ref));
    }

    void markSuccessful() {
        if (state != State.REQUESTED) {
            throw new IllegalStateException("Payment must be REQUESTED to mark successful");
        }

        state = State.SUCCESSFUL;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("payments"), new PaymentSuccessfulEvent(ref));
    }

    void markFailed() {
        if (state != State.REQUESTED) {
            throw new IllegalStateException("Payment must be REQUESTED to mark failed");
        }

        state = State.FAILED;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("payments"), new PaymentFailedEvent(ref));
    }

    @Override
    public Payment identity() {
        return Payment.builder()
                .amount(Amount.IDENTITY)
                .eventLog(EventLog.IDENTITY)
                .paymentProcessor(PaymentProcessor.IDENTITY)
                .ref(PaymentRef.IDENTITY)
                .build();
    }

    @Override
    public BiFunction<Payment, PaymentEvent, Payment> accumulatorFunction() {
        return new Accumulator();
    }

    @Override
    public PaymentState state() {
        return new PaymentState(state, amount, ref);
    }

    public enum State {
        NEW, REQUESTED, SUCCESSFUL, FAILED
    }

    private static class Accumulator implements BiFunction<Payment, PaymentEvent, Payment> {
        @Override
        public Payment apply(Payment payment, PaymentEvent paymentEvent) {
            if (paymentEvent instanceof PaymentAddedEvent) {
                PaymentAddedEvent pae = (PaymentAddedEvent) paymentEvent;
                PaymentState paymentState = pae.getPaymentState();
                return Payment.builder()
                        .amount(paymentState.getAmount())
                        .paymentProcessor(DummyPaymentProcessor.instance())
                        .ref(paymentState.getRef())
                        .eventLog(InProcessEventLog.instance())
                        .build();
            } else if (paymentEvent instanceof PaymentRequestedEvent) {
                payment.state = State.REQUESTED;
                return payment;
            } else if (paymentEvent instanceof PaymentSuccessfulEvent) {
                payment.state = State.SUCCESSFUL;
                return payment;
            } else if (paymentEvent instanceof PaymentFailedEvent) {
                payment.state = State.FAILED;
                return payment;
            }
            throw new IllegalStateException("Unknown PaymentEvent");
        }
    }

    @Value
    static class PaymentState implements AggregateState {
        State state;
        Amount amount;
        PaymentRef ref;
    }

}
