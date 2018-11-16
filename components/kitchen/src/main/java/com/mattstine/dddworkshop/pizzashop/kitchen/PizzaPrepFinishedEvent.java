package com.mattstine.dddworkshop.pizzashop.kitchen;

import lombok.Value;

@Value
final class PizzaPrepFinishedEvent implements PizzaEvent {
    PizzaRef ref;
}

