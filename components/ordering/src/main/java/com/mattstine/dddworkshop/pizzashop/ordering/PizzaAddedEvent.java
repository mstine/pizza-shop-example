package com.mattstine.dddworkshop.pizzashop.ordering;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * @author Matt Stine
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
final class PizzaAddedEvent implements OnlineOrderEvent {
    private final OnlineOrderRef ref;
    private final Pizza pizza;
}
