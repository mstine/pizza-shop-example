package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import lombok.*;
import lombok.experimental.NonFinal;

import java.util.function.BiFunction;

@Value
public final class Pizza implements Aggregate {
    PizzaRef ref;
    KitchenOrderRef kitchenOrderRef;
    Size size;
    EventLog $eventLog;
    @NonFinal
    @Setter(AccessLevel.PACKAGE)
    State state;

    @Builder
    private Pizza(@NonNull PizzaRef ref,
                  @NonNull KitchenOrderRef kitchenOrderRef,
                  @NonNull Size size,
                  @NonNull EventLog eventLog) {
        this.ref = ref;
        this.kitchenOrderRef = kitchenOrderRef;
        this.size = size;
        this.$eventLog = eventLog;

        this.state = State.NEW;
    }

    /**
     * Private no-args ctor to support reflection ONLY.
     */
    @SuppressWarnings("unused")
    private Pizza() {
        this.ref = null;
        this.kitchenOrderRef = null;
        this.size = null;
        this.$eventLog = null;
    }

    public boolean isNew() {
        return this.state == State.NEW;
    }

    void startPrep() {
        if (this.state != State.NEW) {
            throw new IllegalStateException("only NEW Pizza can startPrep");
        }

        this.state = State.PREPPING;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("pizzas"), new PizzaPrepStartedEvent(ref));
    }

    boolean isPrepping() {
        return this.state == State.PREPPING;
    }

    void finishPrep() {
        if (this.state != State.PREPPING) {
            throw new IllegalStateException("only PREPPING Pizza can finishPrep");
        }

        this.state = State.PREPPED;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("pizzas"), new PizzaPrepFinishedEvent(ref));
    }

    boolean hasFinishedPrep() {
        return this.state == State.PREPPED;
    }

    void startBake() {
        if (this.state != State.PREPPED) {
            throw new IllegalStateException("only PREPPED Pizza can startBake");
        }

        this.state = State.BAKING;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("pizzas"), new PizzaBakeStartedEvent(ref));
    }

    boolean isBaking() {
        return this.state == State.BAKING;
    }

    void finishBake() {
        if (this.state != State.BAKING) {
            throw new IllegalStateException("only BAKING pizza can finishBake");
        }

        this.state = State.BAKED;

        /*
         * condition only occurs if reflection supporting
         * private no-args constructor is used
         */
        assert $eventLog != null;
        $eventLog.publish(new Topic("pizzas"), new PizzaBakeFinishedEvent(ref));
    }

    boolean hasFinishedBaking() {
        return this.state == State.BAKED;
    }

    @Override
    public Pizza identity() {
        return Pizza.builder()
                .ref(PizzaRef.IDENTITY)
                .eventLog(EventLog.IDENTITY)
                .kitchenOrderRef(KitchenOrderRef.IDENTITY)
                .size(Size.IDENTITY)
                .build();
    }

    @Override
    public BiFunction<Pizza, PizzaEvent, Pizza> accumulatorFunction(EventLog eventLog) {
        return new Accumulator(eventLog);
    }

    @Override
    public PizzaRef getRef() {
        return ref;
    }

    @Override
    public PizzaState state() {
        return new PizzaState(ref, kitchenOrderRef, size);
    }

    enum Size {
        IDENTITY, SMALL, MEDIUM, LARGE
    }

    enum State {
        NEW,
        PREPPING,
        PREPPED,
        BAKING,
        BAKED
    }

    private static class Accumulator implements BiFunction<Pizza, PizzaEvent, Pizza> {

        private final EventLog eventLog;

        Accumulator(EventLog eventLog) {
            this.eventLog = eventLog;
        }

        @Override
        public Pizza apply(Pizza pizza, PizzaEvent pizzaEvent) {
            if (pizzaEvent instanceof PizzaAddedEvent) {
                PizzaAddedEvent pae = (PizzaAddedEvent) pizzaEvent;
                PizzaState pizzaState = pae.getState();
                return Pizza.builder()
                        .size(pizzaState.getSize())
                        .ref(pizzaState.getRef())
                        .kitchenOrderRef(pizzaState.getKitchenOrderRef())
                        .eventLog(eventLog)
                        .build();
            } else if (pizzaEvent instanceof PizzaPrepStartedEvent) {
                pizza.state = State.PREPPING;
                return pizza;
            } else if (pizzaEvent instanceof PizzaPrepFinishedEvent) {
                pizza.state = State.PREPPED;
                return pizza;
            } else if (pizzaEvent instanceof PizzaBakeStartedEvent) {
                pizza.state = State.BAKING;
                return pizza;
            } else if (pizzaEvent instanceof PizzaBakeFinishedEvent) {
                pizza.state = State.BAKED;
                return pizza;
            }
            throw new IllegalStateException("Unknown PizzaEvent");
        }
    }

    @Value
    static class PizzaState implements AggregateState {
        PizzaRef ref;
        KitchenOrderRef kitchenOrderRef;
        Size size;
    }
}
