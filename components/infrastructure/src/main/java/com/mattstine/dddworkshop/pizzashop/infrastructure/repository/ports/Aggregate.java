package com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports;

import java.util.function.BiFunction;

/**
 * @author Matt Stine
 */
public interface Aggregate<E extends AggregateEvent> {

    Aggregate identity();

    BiFunction<Aggregate, E, Aggregate> accumulatorFunction();

    Ref getRef();

    AggregateState state();

}
