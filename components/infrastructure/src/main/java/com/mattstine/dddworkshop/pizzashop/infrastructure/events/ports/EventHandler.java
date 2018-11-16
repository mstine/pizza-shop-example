package com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports;

/**
 * @author Matt Stine
 */
public interface EventHandler {
    void handleEvent(Event e);
}
