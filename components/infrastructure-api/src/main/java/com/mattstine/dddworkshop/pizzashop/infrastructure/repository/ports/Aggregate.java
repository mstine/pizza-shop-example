package com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;

import java.util.function.BiFunction;

/**
 * @author Matt Stine
 */
public interface Aggregate<E extends AggregateEvent> {

    Aggregate identity();

    BiFunction<Aggregate, E, Aggregate> accumulatorFunction(EventLog eventLog);

    Ref getRef();

    AggregateState state();

}
