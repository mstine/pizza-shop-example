package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.RepositoryAddEvent;
import lombok.Value;

@Value
final class PizzaAddedEvent implements PizzaEvent, RepositoryAddEvent {
    PizzaRef ref;
    Pizza.PizzaState state;
}
