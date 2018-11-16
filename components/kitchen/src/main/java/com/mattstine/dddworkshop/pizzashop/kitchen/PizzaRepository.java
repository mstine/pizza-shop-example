package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Repository;

import java.util.Set;

interface PizzaRepository extends Repository<PizzaRef, Pizza, Pizza.PizzaState, PizzaEvent, PizzaAddedEvent> {
    Set<Pizza> findPizzasByKitchenOrderRef(KitchenOrderRef kitchenOrderRef);
}
