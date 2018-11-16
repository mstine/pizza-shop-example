package com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports;

import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
public final class Topic {
    String name;
}
