package com.mattstine.dddworkshop.pizzashop.payments;

import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
final class PaymentProcessedEvent implements PaymentEvent {
    PaymentRef ref;
    Status status;

    boolean isSuccessful() {
        return status == Status.SUCCESSFUL;
    }

    boolean isFailed() {
        return status == Status.FAILED;
    }

    public enum Status {
        SUCCESSFUL, FAILED
    }
}
