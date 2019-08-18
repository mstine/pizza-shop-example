package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import com.mattstine.dddworkshop.pizzashop.kitchen.acl.ordering.OnlineOrderRef;
import lombok.*;
import lombok.experimental.NonFinal;

import java.util.List;
import java.util.function.BiFunction;

@Value
public final class KitchenOrder implements Aggregate {
    KitchenOrderRef ref;
    OnlineOrderRef onlineOrderRef;
    List<Pizza> pizzas;
    EventLog $eventLog;
    @NonFinal
    @Setter(AccessLevel.PACKAGE)
    State state;

    @Builder
    private KitchenOrder(@NonNull KitchenOrderRef ref, @NonNull OnlineOrderRef onlineOrderRef, @Singular List<Pizza> pizzas, @NonNull EventLog eventLog) {
        this.ref = ref;
        this.onlineOrderRef = onlineOrderRef;
        this.pizzas = pizzas;
        this.$eventLog = eventLog;

        this.state = State.NEW;
    }

    /**
     * Private no-args ctor to support reflection ONLY.
     */
    @SuppressWarnings("unused")
    private KitchenOrder() {
        this.ref = null;
        this.onlineOrderRef = null;
        this.pizzas = null;
        this.$eventLog = null;
    }

    public boolean isNew() {
        return this.state == State.NEW;
    }

    void startPrep() {
        if (this.state != State.NEW) {
            throw new IllegalStateException("Can only startPrep on NEW OnlineOrder");
        }

        this.state = State.PREPPING;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("kitchen_orders"), new KitchenOrderPrepStartedEvent(ref));
    }

    boolean isPrepping() {
        return this.state == State.PREPPING;
    }

    void startBake() {
        if (this.state != State.PREPPING) {
            throw new IllegalStateException("Can only startBake on PREPPING KitchenOrder");
        }

        this.state = State.BAKING;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("kitchen_orders"), new KitchenOrderBakeStartedEvent(ref));
    }

    boolean isBaking() {
        return this.state == State.BAKING;
    }

    void startAssembly() {
        if (this.state != State.BAKING) {
            throw new IllegalStateException("Can only startAssembly on BAKING KitchenOrder");
        }

        this.state = State.ASSEMBLING;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("kitchen_orders"), new KitchenOrderAssemblyStartedEvent(ref));
    }

    boolean hasStartedAssembly() {
        return this.state == State.ASSEMBLING;
    }

    void finishAssembly() {
        if (this.state != State.ASSEMBLING) {
            throw new IllegalStateException("Can only finishAssembly on ASSEMBLING KitchenOrder");
        }

        this.state = State.ASSEMBLED;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("kitchen_orders"), new KitchenOrderAssemblyFinishedEvent(ref));
    }

    boolean hasFinishedAssembly() {
        return this.state == State.ASSEMBLED;
    }

    @Override
    public KitchenOrder identity() {
        return KitchenOrder.builder()
                .ref(KitchenOrderRef.IDENTITY)
                .onlineOrderRef(OnlineOrderRef.IDENTITY)
                .eventLog(EventLog.IDENTITY)
                .build();
    }

    @Override
    public BiFunction<KitchenOrder, KitchenOrderEvent, KitchenOrder> accumulatorFunction(EventLog eventLog) {
        return new Accumulator(eventLog);
    }

    @Override
    public OrderState state() {
        return new OrderState(ref, onlineOrderRef, pizzas);
    }

    enum State {
        NEW,
        PREPPING,
        BAKING,
        ASSEMBLING,
        ASSEMBLED
    }

    private static class Accumulator implements BiFunction<KitchenOrder, KitchenOrderEvent, KitchenOrder> {

        private final EventLog eventLog;

        Accumulator(EventLog eventLog) {
            this.eventLog = eventLog;
        }

        @Override
        public KitchenOrder apply(KitchenOrder kitchenOrder, KitchenOrderEvent kitchenOrderEvent) {
            if (kitchenOrderEvent instanceof KitchenOrderAddedEvent) {
                KitchenOrderAddedEvent oae = (KitchenOrderAddedEvent) kitchenOrderEvent;
                OrderState orderState = oae.getState();
                return KitchenOrder.builder()
                        .eventLog(eventLog)
                        .ref(orderState.getRef())
                        .onlineOrderRef(orderState.getOnlineOrderRef())
                        .pizzas(orderState.getPizzas())
                        .build();
            } else if (kitchenOrderEvent instanceof KitchenOrderPrepStartedEvent) {
                kitchenOrder.state = State.PREPPING;
                return kitchenOrder;
            } else if (kitchenOrderEvent instanceof KitchenOrderBakeStartedEvent) {
                kitchenOrder.state = State.BAKING;
                return kitchenOrder;
            } else if (kitchenOrderEvent instanceof KitchenOrderAssemblyStartedEvent) {
                kitchenOrder.state = State.ASSEMBLING;
                return kitchenOrder;
            } else if (kitchenOrderEvent instanceof KitchenOrderAssemblyFinishedEvent) {
                kitchenOrder.state = State.ASSEMBLED;
                return kitchenOrder;
            }
            throw new IllegalStateException("Unknown KitchenOrderEvent");
        }
    }

    /*
     * Pizza Value Object for OnlineOrder Details Only
     */
    @Value
    public static final class Pizza {
        Size size;

        @Builder
        private Pizza(@NonNull Size size) {
            this.size = size;
        }

        public enum Size {
            SMALL, MEDIUM, LARGE
        }
    }

    @Value
    static class OrderState implements AggregateState {
        KitchenOrderRef ref;
        OnlineOrderRef onlineOrderRef;
        List<Pizza> pizzas;
    }
}
