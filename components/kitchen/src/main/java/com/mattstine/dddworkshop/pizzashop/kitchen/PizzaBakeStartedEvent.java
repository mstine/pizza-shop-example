package com.mattstine.dddworkshop.pizzashop.kitchen;

import lombok.Value;

@Value
final class PizzaBakeStartedEvent implements PizzaEvent {
    PizzaRef ref;
}
