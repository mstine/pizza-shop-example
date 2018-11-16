package com.mattstine.dddworkshop.pizzashop.kitchen;

import lombok.Value;

@Value
final class PizzaBakeFinishedEvent implements PizzaEvent {
    PizzaRef ref;
}
