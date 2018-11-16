package com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;

/**
 * @author Matt Stine
 */
public interface AggregateEvent extends Event {
    @SuppressWarnings("EmptyMethod")
    Ref getRef();
}
