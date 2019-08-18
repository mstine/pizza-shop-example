package com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Event;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventHandler;

/**
 * @author Matt Stine
 */
abstract class VerifiableEventHandler implements EventHandler {
    @SuppressWarnings("WeakerAccess")
    protected boolean invoked = false;

    static VerifiableEventHandler of(EventHandler eventHandler) {
        return new VerifiableEventHandler() {
            @Override
            public void handleEvent(Event e) {
                this.invoked = true;
                eventHandler.handleEvent(e);
            }
        };
    }

    boolean isInvoked() {
        return invoked;
    }
}
