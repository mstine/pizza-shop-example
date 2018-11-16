package com.mattstine.dddworkshop.pizzashop.ordering;

import com.mattstine.dddworkshop.pizzashop.infrastructure.domain.valuetypes.Amount;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import com.mattstine.dddworkshop.pizzashop.payments.PaymentRef;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * @author Matt Stine
 */
@SuppressWarnings("DefaultAnnotationParam")
@Value
@EqualsAndHashCode(callSuper = false)
public final class OnlineOrder implements Aggregate {
    Type type;
    EventLog $eventLog;
    OnlineOrderRef ref;
    List<Pizza> pizzas;
    @NonFinal
    State state;
    @NonFinal
    PaymentRef paymentRef;

    @Builder
    private OnlineOrder(@NonNull Type type, @NonNull EventLog eventLog, @NonNull OnlineOrderRef ref) {
        this.type = type;
        this.$eventLog = eventLog;
        this.ref = ref;
        this.pizzas = new ArrayList<>();

        this.state = State.NEW;
    }

    /**
     * Private no-args ctor to support reflection ONLY.
     */
    @SuppressWarnings("unused")
    private OnlineOrder() {
        this.type = null;
        this.ref = null;
        this.pizzas = null;
        this.$eventLog = null;
    }

    boolean isPickupOrder() {
        return this.type == Type.PICKUP;
    }

    boolean isDeliveryOrder() {
        return this.type == Type.DELIVERY;
    }

    public boolean isNew() {
        return state == State.NEW;
    }

    boolean isSubmitted() {
        return this.state == State.SUBMITTED;
    }

    boolean isPaid() {
        return state == State.PAID;
    }

    public void addPizza(Pizza pizza) {
        if (this.state != State.NEW) {
            throw new IllegalStateException("Can only add Pizza to NEW OnlineOrder");
        }

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert this.pizzas != null;
        this.pizzas.add(pizza);

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("ordering"), new PizzaAddedEvent(ref, pizza));
    }

    void submit() {
        if (this.state != State.NEW) {
            throw new IllegalStateException("Can only submit NEW OnlineOrder");
        }

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert this.pizzas != null;
        if (this.pizzas.isEmpty()) {
            throw new IllegalStateException("Cannot submit OnlineOrder without at least one Pizza");
        }

        this.state = State.SUBMITTED;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("ordering"), new OnlineOrderSubmittedEvent(ref));
    }

    void assignPaymentRef(PaymentRef paymentRef) {
        this.paymentRef = paymentRef;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("ordering"), new PaymentRefAssignedEvent(ref, paymentRef));
    }

    Amount calculatePrice() {
        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert this.pizzas != null;
        return this.pizzas.stream()
                .map(Pizza::calculatePrice)
                .reduce(Amount.of(0, 0), Amount::plus);
    }

    void markPaid() {
        if (this.state != State.SUBMITTED) {
            throw new IllegalStateException("Can only mark SUBMITTED OnlineOrder as Paid");
        }

        this.state = State.PAID;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("ordering"), new OnlineOrderPaidEvent(ref));
    }

    @Override
    public OnlineOrder identity() {
        return OnlineOrder.builder()
                .eventLog(EventLog.IDENTITY)
                .ref(OnlineOrderRef.IDENTITY)
                .type(Type.IDENTITY)
                .build();
    }

    @Override
    public BiFunction<OnlineOrder, OnlineOrderEvent, OnlineOrder> accumulatorFunction() {
        return new Accumulator();
    }

    @Override
    public OrderState state() {
        return new OrderState(ref, state, type);
    }

    enum State {
        NEW, SUBMITTED, PAID
    }

    public enum Type {
        IDENTITY, DELIVERY, PICKUP
    }

    private static class Accumulator implements BiFunction<OnlineOrder, OnlineOrderEvent, OnlineOrder> {

        @Override
        public OnlineOrder apply(OnlineOrder onlineOrder, OnlineOrderEvent onlineOrderEvent) {
            if (onlineOrderEvent instanceof OnlineOrderAddedEvent) {
                OnlineOrderAddedEvent oae = (OnlineOrderAddedEvent) onlineOrderEvent;
                OrderState orderState = oae.getOrderState();
                return OnlineOrder.builder()
                        .eventLog(InProcessEventLog.instance())
                        .ref(orderState.getOnlineOrderRef())
                        .type(orderState.getType())
                        .build();
            } else if (onlineOrderEvent instanceof PizzaAddedEvent) {
                PizzaAddedEvent pae = (PizzaAddedEvent) onlineOrderEvent;

                /*
                 * condition only occurs if reflection supporting
                 * private no-args constructor is used
                 */
                assert onlineOrder.pizzas != null;
                onlineOrder.pizzas.add(pae.getPizza());

                return onlineOrder;
            } else if (onlineOrderEvent instanceof OnlineOrderSubmittedEvent) {
                onlineOrder.state = State.SUBMITTED;
                return onlineOrder;
            } else if (onlineOrderEvent instanceof PaymentRefAssignedEvent) {
                @SuppressWarnings("SpellCheckingInspection")
                PaymentRefAssignedEvent prae = (PaymentRefAssignedEvent) onlineOrderEvent;
                onlineOrder.paymentRef = prae.getPaymentRef();
                return onlineOrder;
            } else if (onlineOrderEvent instanceof OnlineOrderPaidEvent) {
                onlineOrder.state = State.PAID;
                return onlineOrder;
            }
            throw new IllegalStateException("Unknown OnlineOrderEvent");
        }
    }

    @Value
    static class OrderState implements AggregateState {
        OnlineOrderRef onlineOrderRef;
        State state;
        Type type;
    }
}
