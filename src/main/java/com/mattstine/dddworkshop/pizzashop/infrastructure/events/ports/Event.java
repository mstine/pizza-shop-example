package com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Ref;

/**
 * @author Matt Stine
 */
public interface Event {
    String toString();
    Ref getRef();

    //TODO: Event needs a unique key to support Kafka topics

    //TODO: Need to think about how to serialize/deserialize events
}
